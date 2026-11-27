package grouponekingdown.onekingdown.kingdom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import grouponekingdown.onekingdown.Config;
import grouponekingdown.onekingdown.invasion.InvasionManager;
import grouponekingdown.onekingdown.network.OpenRoomSetupPayload;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class KingdomManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random RANDOM = new Random();

    // Distancia entre centros dos reinos na mesma sala (120x120 -> 30 blocos livres entre muralhas)
    private static final int COMPACT_KINGDOM_STEP = 150;

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

    private static String generateRoomCode() {
        int num = 1000 + RANDOM.nextInt(9000);
        return "OKD-" + num;
    }

    public KingdomData.Room getOrCreatePrimaryRoom() {
        if (!state.rooms.isEmpty()) {
            KingdomData.Room first = state.rooms.get(0);
            if (first.roomCode == null || first.roomCode.isEmpty()) {
                first.roomCode = generateRoomCode();
                saveToDisk();
            }
            return first;
        }
        KingdomData.Room newRoom = new KingdomData.Room(
                state.nextRoomId++,
                Math.min(5, Math.max(1, Config.maxPlayersPerRoom)),
                generateRoomCode()
        );
        state.rooms.add(newRoom);
        saveToDisk();
        return newRoom;
    }

    public void openRoomSetupForPlayer(ServerPlayer player) {
        KingdomData.Room room = getOrCreatePrimaryRoom();
        boolean alreadyConfigured = room.configured;
        String codeToSend = alreadyConfigured ? "" : room.roomCode;
        PacketDistributor.sendToPlayer(player, new OpenRoomSetupPayload(codeToSend, alreadyConfigured, room.maxPlayers));
    }

    public static void applyRoomSetup(ServerPlayer host, int maxPlayers, int[] themeOrdinals, int[] bannerOrdinals) {
        KingdomManager manager = get();
        if (manager == null) return;

        int clampedMax = Math.max(1, Math.min(5, maxPlayers));
        KingdomData.Room room = manager.getOrCreatePrimaryRoom();
        room.maxPlayers = clampedMax;
        room.slotThemes.clear();
        room.slotBanners.clear();
        for (int i = 0; i < clampedMax; i++) {
            int theme = (themeOrdinals != null && i < themeOrdinals.length) ? themeOrdinals[i] : i;
            int banner = (bannerOrdinals != null && i < bannerOrdinals.length) ? bannerOrdinals[i] : i;
            room.slotThemes.add(theme);
            room.slotBanners.add(banner);
        }
        room.configured = true;
        manager.saveToDisk();

        ServerLevel overworld = host.level().getServer().overworld();
        KingdomData.Kingdom kingdom = manager.createKingdomInRoom(overworld, host, room);
        manager.teleportToKingdom(overworld, host, kingdom);

        host.sendSystemMessage(Component.literal(
                "§6§l[OneKingdown] Sala configurada para " + room.maxPlayers + " jogador(es)!"
        ));
        host.sendSystemMessage(Component.literal(
                "§aCodigo da Sala (envie aos convidados): §e§l" + room.roomCode
        ));
        host.sendSystemMessage(Component.literal(
                "§7Seu reino: §f" + kingdom.getTheme().displayName
                        + " §7| Bandeira: §f" + kingdom.getBannerColor().displayName
        ));
    }

    public static void joinRoomWithCode(ServerPlayer guest, String enteredCode) {
        KingdomManager manager = get();
        if (manager == null) return;

        String normalized = enteredCode == null ? "" : enteredCode.trim().toUpperCase();
        Optional<KingdomData.Room> roomOpt = manager.state.rooms.stream()
                .filter(r -> r.configured && r.roomCode != null && r.roomCode.equalsIgnoreCase(normalized))
                .findFirst();

        if (roomOpt.isEmpty()) {
            guest.sendSystemMessage(Component.literal("§c[OneKingdown] Codigo invalido! Tente novamente."));
            manager.openRoomSetupForPlayer(guest);
            return;
        }

        KingdomData.Room room = roomOpt.get();
        if (!room.hasSpace()) {
            guest.sendSystemMessage(Component.literal("§c[OneKingdown] Esta sala ja esta cheia (" + room.maxPlayers + "/" + room.maxPlayers + ")!"));
            return;
        }

        ServerLevel overworld = guest.level().getServer().overworld();
        KingdomData.Kingdom kingdom = manager.createKingdomInRoom(overworld, guest, room);
        manager.teleportToKingdom(overworld, guest, kingdom);

        guest.sendSystemMessage(Component.literal(
                "§a[OneKingdown] Voce entrou na Sala #" + room.roomId + " (" + room.roomCode + ")!"
        ));
        guest.sendSystemMessage(Component.literal(
                "§7Seu reino: §f" + kingdom.getTheme().displayName
                        + " §7| Bandeira: §f" + kingdom.getBannerColor().displayName
        ));
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

    private static int[] getCompactSlotOffset(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> new int[]{0, 0};
            case 1 -> new int[]{COMPACT_KINGDOM_STEP, 0};
            case 2 -> new int[]{-COMPACT_KINGDOM_STEP, 0};
            case 3 -> new int[]{0, COMPACT_KINGDOM_STEP};
            case 4 -> new int[]{0, -COMPACT_KINGDOM_STEP};
            default -> new int[]{COMPACT_KINGDOM_STEP * slotIndex, 0};
        };
    }

    private KingdomData.Kingdom createKingdomInRoom(ServerLevel overworld, ServerPlayer player, KingdomData.Room targetRoom) {
        Optional<KingdomData.Kingdom> existing = getKingdomByPlayer(player.getUUID());
        if (existing.isPresent()) {
            KingdomNpcManager.syncKingdomNpcs(overworld, existing.get());
            return existing.get();
        }

        int slotIndex = targetRoom.kingdoms.size();
        int roomOriginX = (targetRoom.roomId - 1) * Config.roomSpacing;
        int roomOriginZ = 0;

        int[] offset = getCompactSlotOffset(slotIndex);
        int centerX = roomOriginX + offset[0];
        int centerZ = roomOriginZ + offset[1];

        overworld.getChunk(centerX >> 4, centerZ >> 4);
        int surfaceY = Math.max(overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ), 64);

        int themeOrdinal = targetRoom.getThemeForSlot(slotIndex);
        int bannerOrdinal = targetRoom.getBannerForSlot(slotIndex);

        KingdomData.Kingdom kingdom = new KingdomData.Kingdom(
                player.getUUID(),
                player.getName().getString(),
                targetRoom.roomId,
                slotIndex,
                centerX,
                surfaceY,
                centerZ,
                Config.kingdomSideSize,
                themeOrdinal,
                bannerOrdinal
        );

        targetRoom.kingdoms.add(kingdom);
        CastleStructureBuilder.buildOrUpgradeCastle(overworld, kingdom);
        KingdomNpcManager.syncKingdomNpcs(overworld, kingdom);
        saveToDisk();

        return kingdom;
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
                    KingdomData.Room newRoom = new KingdomData.Room(
                            state.nextRoomId++,
                            Math.min(5, Math.max(1, Config.maxPlayersPerRoom)),
                            generateRoomCode()
                    );
                    state.rooms.add(newRoom);
                    LOGGER.info("Nova sala criada: Sala #{} ({})", newRoom.roomId, newRoom.roomCode);
                    return newRoom;
                });

        return createKingdomInRoom(overworld, player, targetRoom);
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
            player.sendSystemMessage(Component.literal(
                    "§6§l[OneKingdown] EVOLUCAO! Castelo e construcoes atualizados para o Tier " + kingdom.currentTier + "!"
            ));
            InvasionManager.onCastleTierReached(
                    overworld,
                    new BlockPos(kingdom.centerX, kingdom.centerY, kingdom.centerZ),
                    kingdom.currentTier
            );
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

        boolean runHudUpdate = hudTickCounter >= 20;
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
                food,
                kingdom.guardsCount,
                kingdom.minersCount,
                kingdom.farmersCount,
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