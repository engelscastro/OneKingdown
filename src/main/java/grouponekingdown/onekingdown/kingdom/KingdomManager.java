package grouponekingdown.onekingdown.kingdom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import grouponekingdown.onekingdown.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class KingdomManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static KingdomManager instance;

    private final Path saveFile;
    private KingdomData.WorldState state;
    private int productionTickCounter = 0;
    private int hudTickCounter = 0;

    private KingdomManager(Path saveFile) {
        this.saveFile = saveFile;
        this.state = loadFromDisk();
    }

    public static void init(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("onekingdown_kingdoms.json");
        instance = new KingdomManager(path);
        LOGGER.info("OneKingdown carregado com {} sala(s).", instance.state.rooms.size());
    }

    public static void shutdown() {
        if (instance != null) {
            instance.saveToDisk();
            instance = null;
        }
    }

    public static KingdomManager get() {
        return instance;
    }

    private KingdomData.WorldState loadFromDisk() {
        if (Files.exists(saveFile)) {
            try {
                String json = Files.readString(saveFile);
                KingdomData.WorldState loaded = GSON.fromJson(json, KingdomData.WorldState.class);
                if (loaded != null) return loaded;
            } catch (IOException e) {
                LOGGER.error("Erro ao carregar dados de reinos do OneKingdown", e);
            }
        }
        return new KingdomData.WorldState();
    }

    public void saveToDisk() {
        try {
            Files.writeString(saveFile, GSON.toJson(state));
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar dados de reinos do OneKingdown", e);
        }
    }

    public Optional<KingdomData.Kingdom> getKingdomByPlayer(UUID uuid) {
        String id = uuid.toString();
        for (KingdomData.Room room : state.rooms) {
            for (KingdomData.Kingdom k : room.kingdoms) {
                if (k.ownerUuid.equals(id)) {
                    return Optional.of(k);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<KingdomData.Room> getRoom(int roomId) {
        return state.rooms.stream().filter(r -> r.roomId == roomId).findFirst();
    }

    public Optional<KingdomData.Kingdom> findKingdomAt(BlockPos pos) {
        for (KingdomData.Room room : state.rooms) {
            for (KingdomData.Kingdom k : room.kingdoms) {
                if (k.contains(pos)) {
                    return Optional.of(k);
                }
            }
        }
        return Optional.empty();
    }

    public KingdomData.Kingdom getOrCreateKingdom(ServerLevel overworld, ServerPlayer player) {
        Optional<KingdomData.Kingdom> existing = getKingdomByPlayer(player.getUUID());
        if (existing.isPresent()) {
            KingdomNpcManager.syncKingdomNpcs(overworld, existing.get());
            return existing.get();
        }

        KingdomData.Room targetRoom = state.rooms.stream()
                .filter(KingdomData.Room::hasSpace)
                .findFirst()
                .orElseGet(() -> {
                    KingdomData.Room newRoom = new KingdomData.Room(state.nextRoomId++, Config.maxPlayersPerRoom);
                    state.rooms.add(newRoom);
                    LOGGER.info("Nova sala criada: Sala #{} (limite: {} jogadores)", newRoom.roomId, newRoom.maxPlayers);
                    return newRoom;
                });

        int slotIndex = targetRoom.kingdoms.size();
        int roomOriginX = (targetRoom.roomId - 1) * Config.roomSpacing;
        int roomOriginZ = 0;

        int centerX = roomOriginX;
        int centerZ = roomOriginZ;
        if (targetRoom.maxPlayers > 1) {
            double angle = (2.0 * Math.PI * slotIndex) / targetRoom.maxPlayers;
            centerX = roomOriginX + (int) Math.round(Math.cos(angle) * Config.kingdomSpacing);
            centerZ = roomOriginZ + (int) Math.round(Math.sin(angle) * Config.kingdomSpacing);
        }

        overworld.getChunk(centerX >> 4, centerZ >> 4);
        int surfaceY = Math.max(overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ), 64);

        KingdomData.Kingdom kingdom = new KingdomData.Kingdom(
                player.getUUID(),
                player.getName().getString(),
                targetRoom.roomId,
                slotIndex,
                centerX,
                surfaceY,
                centerZ,
                Config.kingdomSideSize
        );

        targetRoom.kingdoms.add(kingdom);
        CastleStructureBuilder.buildOrUpgradeCastle(overworld, kingdom);
        KingdomNpcManager.syncKingdomNpcs(overworld, kingdom);
        saveToDisk();

        return kingdom;
    }

    public void addXpAndCheckUpgrade(ServerLevel overworld, ServerPlayer player, int amount) {
        KingdomData.Kingdom kingdom = getOrCreateKingdom(overworld, player);
        int previousLevel = kingdom.level;
        boolean upgraded = kingdom.addXp(amount, Config.xpPerLevel);
        saveToDisk();

        player.sendSystemMessage(Component.literal(
                "§a[OneKingdown] +" + amount + " XP! Nivel atual: " + kingdom.level
                        + " (" + kingdom.xp + "/" + Config.xpPerLevel + " XP)"
        ));

        if (kingdom.level > previousLevel && !upgraded) {
            player.sendSystemMessage(Component.literal("§e[OneKingdown] Seu reino subiu para o Nivel " + kingdom.level + "!"));
        }

        if (upgraded) {
            CastleStructureBuilder.buildOrUpgradeCastle(overworld, kingdom);
            KingdomNpcManager.syncKingdomNpcs(overworld, kingdom);
            teleportToKingdom(overworld, player, kingdom);
            player.sendSystemMessage(Component.literal(
                    "§6§l[OneKingdown] EVOLUCAO! Castelo e construcoes atualizados para o Tier " + kingdom.currentTier + "!"
            ));
        }

        sendHudSnapshot(overworld, player, kingdom);
    }

    public void onServerTick(MinecraftServer server) {
        productionTickCounter++;
        hudTickCounter++;

        int intervalTicks = Config.productionIntervalSeconds * 20;
        boolean runProduction = productionTickCounter >= intervalTicks;
        if (runProduction) {
            productionTickCounter = 0;
        }

        boolean runHudUpdate = hudTickCounter >= 20; // Atualiza HUD a cada 1 segundo
        if (runHudUpdate) {
            hudTickCounter = 0;
        }

        if (!runProduction && !runHudUpdate) return;

        ServerLevel overworld = server.overworld();
        for (KingdomData.Room room : state.rooms) {
            for (KingdomData.Kingdom kingdom : room.kingdoms) {
                ServerPlayer owner = server.getPlayerList().getPlayer(UUID.fromString(kingdom.ownerUuid));
                if (owner != null) {
                    if (runProduction) {
                        KingdomNpcManager.produceKingdomResources(overworld, kingdom);
                    }
                    if (runHudUpdate) {
                        sendHudSnapshot(overworld, owner, kingdom);
                    }
                }
            }
        }
    }

    public void sendHudSnapshot(ServerLevel overworld, ServerPlayer player, KingdomData.Kingdom kingdom) {
        int ores = 0;
        int wood = 0;
        int food = 0;

        BlockPos min = new BlockPos(kingdom.getMinX(), kingdom.centerY - 2, kingdom.getMinZ());
        BlockPos max = new BlockPos(kingdom.getMaxX(), kingdom.centerY + 30, kingdom.getMaxZ());

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = overworld.getBlockEntity(pos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    Item item = stack.getItem();
                    String path = BuiltInRegistries.ITEM.getKey(item).getPath();

                    if (isOreOrMetal(item, path)) {
                        ores += stack.getCount();
                    } else if (isWoodItem(item, path)) {
                        wood += stack.getCount();
                    } else if (isFoodItem(item, path)) {
                        food += stack.getCount();
                    }
                }
            }
        }

        AABB territoryBox = new AABB(
                kingdom.getMinX(), kingdom.centerY - 10, kingdom.getMinZ(),
                kingdom.getMaxX() + 1, kingdom.centerY + 35, kingdom.getMaxZ() + 1
        );
        int animals = overworld.getEntitiesOfClass(Animal.class, territoryBox).size();
        int monsters = overworld.getEntitiesOfClass(Monster.class, territoryBox).size();

        KingdomHudPayload payload = new KingdomHudPayload(
                ores,
                wood,
                kingdom.guardsCount,
                food,
                animals,
                monsters,
                kingdom.level,
                kingdom.currentTier
        );
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static boolean isOreOrMetal(Item item, String path) {
        return item == Items.COAL || item == Items.CHARCOAL
                || item == Items.IRON_INGOT || item == Items.GOLD_INGOT || item == Items.COPPER_INGOT
                || item == Items.DIAMOND || item == Items.EMERALD || item == Items.REDSTONE || item == Items.LAPIS_LAZULI
                || path.contains("_ore") || path.startsWith("raw_") || path.endsWith("_ingot");
    }

    private static boolean isWoodItem(Item item, String path) {
        return item == Items.STICK
                || path.endsWith("_log") || path.endsWith("_wood")
                || path.endsWith("_planks") || path.endsWith("_stem");
    }

    private static boolean isFoodItem(Item item, String path) {
        return item == Items.BREAD || item == Items.WHEAT || item == Items.APPLE || item == Items.GOLDEN_APPLE
                || item == Items.CARROT || item == Items.POTATO || item == Items.BAKED_POTATO || item == Items.MELON_SLICE
                || path.startsWith("cooked_") || item == Items.BEEF || item == Items.PORKCHOP
                || item == Items.CHICKEN || item == Items.MUTTON || item == Items.COD || item == Items.SALMON;
    }

    public void teleportToKingdom(ServerLevel overworld, ServerPlayer player, KingdomData.Kingdom kingdom) {
        KingdomNpcManager.syncKingdomNpcs(overworld, kingdom);
        player.teleportTo(
                overworld,
                kingdom.centerX + 0.5,
                kingdom.centerY + 1.0,
                kingdom.centerZ + 2.5,
                Set.of(),
                180.0f,
                0.0f,
                true
        );
        sendHudSnapshot(overworld, player, kingdom);
    }
}