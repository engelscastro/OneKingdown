package grouponekingdown.onekingdown.kingdom;

import grouponekingdown.onekingdown.network.OpenRecruiterPayload;
import grouponekingdown.onekingdown.npc.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class KingdomNpcManager {
    public static final String TAG_ROLE = "onekingdown_role";
    public static final String TAG_OWNER = "onekingdown_owner";
    public static final String TAG_PROTECTED = "onekingdown_protected";
    public static final String TAG_LEVEL = "onekingdown_level";

    public static final String ROLE_KING = "KING";
    public static final String ROLE_GUARD_LEADER = "GUARD_LEADER";
    public static final String ROLE_MINE_MASTER = "MINE_MASTER";
    public static final String ROLE_FARM_MASTER = "FARM_MASTER";
    public static final String ROLE_GUARD_UNIT = "GUARD_UNIT";
    public static final String ROLE_MINER_UNIT = "MINER_UNIT";
    public static final String ROLE_FARMER_UNIT = "FARMER_UNIT";
    public static final String ROLE_WORKER_UNIT = "WORKER_UNIT"; // Legado

    private static Mob createMob(ServerLevel level, String entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", entityId));
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        return entity instanceof Mob mob ? mob : null;
    }

    /**
     * Garante que o Rei, os 3 gerenciadores, trabalhadores visuais e guardas ativos existam no reino.
     */
    public static void syncKingdomNpcs(ServerLevel level, KingdomData.Kingdom kingdom) {
        AABB searchBox = new AABB(
                kingdom.getMinX() - 4, kingdom.centerY - 10, kingdom.getMinZ() - 4,
                kingdom.getMaxX() + 4, kingdom.centerY + 35, kingdom.getMaxZ() + 4
        );
        List<Entity> existing = level.getEntities((Entity) null, searchBox,
                e -> kingdom.ownerUuid.equals(e.getPersistentData().getString(TAG_OWNER).orElse("")));

        boolean hasKing = false;
        boolean hasGuardLeader = false;
        boolean hasMineMaster = false;
        boolean hasFarmMaster = false;
        int currentGuards = 0;
        int currentMiners = 0;
        int currentFarmers = 0;

        for (Entity e : existing) {
            if (!e.isAlive()) continue;

            if (e instanceof LivingEntity living && living.getPersistentData().getBoolean(TAG_PROTECTED).orElse(false)) {
                living.setHealth(living.getMaxHealth());
            }

            String role = e.getPersistentData().getString(TAG_ROLE).orElse("");
            switch (role) {
                case ROLE_KING -> {
                    hasKing = true;
                    e.teleportTo(kingdom.centerX + 0.5, kingdom.centerY + 1.0, kingdom.centerZ - 1.0);
                }
                case ROLE_GUARD_LEADER -> {
                    hasGuardLeader = true;
                    BlockPos p = kingdom.getBarracksPos();
                    e.teleportTo(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5);
                }
                case ROLE_MINE_MASTER -> {
                    hasMineMaster = true;
                    BlockPos p = kingdom.getMinePos();
                    e.teleportTo(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5);
                }
                case ROLE_FARM_MASTER -> {
                    hasFarmMaster = true;
                    BlockPos p = kingdom.getFarmPos();
                    e.teleportTo(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 2.0);
                }
                case ROLE_GUARD_UNIT -> currentGuards++;
                case ROLE_MINER_UNIT -> currentMiners++;
                case ROLE_FARMER_UNIT -> currentFarmers++;
                case ROLE_WORKER_UNIT -> {
                    // Migra trabalhadores antigos automaticamente pela tag do nome
                    String customName = e.hasCustomName() ? e.getCustomName().getString() : "";
                    if (customName.contains("Mineiro")) {
                        e.getPersistentData().putString(TAG_ROLE, ROLE_MINER_UNIT);
                        currentMiners++;
                    } else {
                        e.getPersistentData().putString(TAG_ROLE, ROLE_FARMER_UNIT);
                        currentFarmers++;
                    }
                }
            }
        }

        // 1. O Rei (vulneravel a monstros nas invasoes, protegido pelos guardas)
        if (!hasKing) {
            spawnStationaryNpc(level, kingdom, ROLE_KING, "§6§lRei de " + kingdom.ownerName,
                    kingdom.centerX + 0.5, kingdom.centerY + 1.0, kingdom.centerZ - 1.0, false);
        }

        // 2. Lider da Guarda (Quartel)
        if (!hasGuardLeader) {
            BlockPos p = kingdom.getBarracksPos();
            spawnStationaryNpc(level, kingdom, ROLE_GUARD_LEADER, "§c§lLíder da Guarda",
                    p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, true);
        }

        // 3. Mestre da Mina (Mina)
        if (!hasMineMaster) {
            BlockPos p = kingdom.getMinePos();
            spawnStationaryNpc(level, kingdom, ROLE_MINE_MASTER, "§b§lMestre da Mina",
                    p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, true);
        }

        // 4. Mestre da Fazenda (Plantacao)
        if (!hasFarmMaster) {
            BlockPos p = kingdom.getFarmPos();
            spawnStationaryNpc(level, kingdom, ROLE_FARM_MASTER, "§a§lMestre da Fazenda",
                    p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 2.0, true);
        }

        // 5. Guardas ativos patrulhando o castelo e atacando monstros
        for (int i = currentGuards; i < kingdom.guardsCount; i++) {
            spawnGuardUnit(level, kingdom, i);
        }

        // 6. Mineiros na Mina
        for (int i = currentMiners; i < kingdom.minersCount; i++) {
            spawnLeveledWorker(level, kingdom, "MINER", 1);
        }

        // 7. Fazendeiros na Plantacao
        for (int i = currentFarmers; i < kingdom.farmersCount; i++) {
            spawnLeveledWorker(level, kingdom, "FARMER", 1);
        }
    }

    private static void spawnStationaryNpc(ServerLevel level, KingdomData.Kingdom kingdom,
                                           String role, String name, double x, double y, double z, boolean protectedNpc) {
        Mob npc = createMob(level, "villager");
        if (npc == null) return;

        npc.setPos(x, y, z);
        npc.setCustomName(Component.literal(name));
        npc.setCustomNameVisible(true);
        npc.setNoAi(true);
        npc.setPersistenceRequired();

        CompoundTag tag = npc.getPersistentData();
        tag.putString(TAG_ROLE, role);
        tag.putString(TAG_OWNER, kingdom.ownerUuid);
        tag.putBoolean(TAG_PROTECTED, protectedNpc);

        level.addFreshEntity(npc);
    }

    private static void spawnGuardUnit(ServerLevel level, KingdomData.Kingdom kingdom, int index) {
        Mob guard = ModEntities.KINGDOM_GUARD.get().create(level, EntitySpawnReason.COMMAND);
        if (guard == null) return;

        double offsetX = ((index % 3) - 1) * 3.0;
        guard.setPos(kingdom.centerX + offsetX, kingdom.centerY + 1.0, kingdom.centerZ + 4.0);
        guard.setCustomName(Component.literal("§9Guarda Real"));
        guard.setCustomNameVisible(true);
        guard.setPersistenceRequired();
        equipUnitByLevel(guard, "WARRIOR", 1);

        CompoundTag tag = guard.getPersistentData();
        tag.putString(TAG_ROLE, ROLE_GUARD_UNIT);
        tag.putString(TAG_OWNER, kingdom.ownerUuid);

        level.addFreshEntity(guard);
    }

    /**
     * Trata clique direito do General nos NPCs gerenciadores ou no Rei.
     */
    public static boolean handleNpcInteract(ServerLevel overworld, ServerPlayer player, Entity target) {
        String role = target.getPersistentData().getString(TAG_ROLE).orElse("");
        if (role.isEmpty()) return false;

        String ownerUuid = target.getPersistentData().getString(TAG_OWNER).orElse("");
        if (!ownerUuid.equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.literal("§c[OneKingdown] Este NPC responde apenas ao General do proprio reino!"));
            return true;
        }

        KingdomManager manager = KingdomManager.get();
        Optional<KingdomData.Kingdom> kingdomOpt = manager.getKingdomByPlayer(player.getUUID());
        if (kingdomOpt.isEmpty()) return true;
        KingdomData.Kingdom kingdom = kingdomOpt.get();

        ItemStack held = player.getMainHandItem();

        switch (role) {
            case ROLE_KING -> {
                if (held.is(Items.GOLD_INGOT) || held.is(Items.IRON_INGOT) || held.is(Items.EMERALD)) {
                    int xpGain = held.is(Items.EMERALD) ? 50 : (held.is(Items.GOLD_INGOT) ? 30 : 15);
                    held.shrink(1);
                    manager.addXpAndCheckUpgrade(overworld, player, xpGain);
                } else {
                    player.sendSystemMessage(Component.literal(
                            "§6[Rei] §fGeneral " + player.getName().getString()
                                    + ", proteja nosso trono! Nosso reino esta no §eNivel " + kingdom.level
                                    + " (Tier " + kingdom.currentTier + ")§f.\n"
                                    + "§7Dica: Clique em mim segurando §eOuro§7, §fFerro§7 ou §aEsmeralda§7 para investir no reino e subir de nivel!"
                    ));
                }
            }
            case ROLE_GUARD_LEADER -> openRecruiterGui(player, "GUARD");
            case ROLE_MINE_MASTER -> openRecruiterGui(player, "MINE");
            case ROLE_FARM_MASTER -> openRecruiterGui(player, "FARM");
        }
        return true;
    }

    private static void openRecruiterGui(ServerPlayer player, String recruiterRole) {
        int ores = countItem(player, Items.IRON_INGOT);
        int wood = countItem(player, Items.OAK_LOG);
        int food = countItem(player, Items.BREAD);
        PacketDistributor.sendToPlayer(player, new OpenRecruiterPayload(recruiterRole, ores, wood, food));
    }

    public static void hireUnitWithLevel(ServerLevel overworld, ServerPlayer player, String unitType, int level) {
        int clampedLevel = Math.max(1, Math.min(5, level));
        KingdomManager manager = KingdomManager.get();
        Optional<KingdomData.Kingdom> kingdomOpt = manager.getKingdomByPlayer(player.getUUID());
        if (kingdomOpt.isEmpty()) return;
        KingdomData.Kingdom kingdom = kingdomOpt.get();

        int cost = clampedLevel * 15;

        switch (unitType) {
            case "WARRIOR", "ARCHER" -> {
                if (kingdom.guardsCount >= kingdom.getMaxGuards()) {
                    player.sendSystemMessage(Component.literal("§c[Quartel] Limite maximo de guardas atingido (" + kingdom.getMaxGuards() + ")."));
                    return;
                }
                if (!player.isCreative()) {
                    if (countItem(player, Items.IRON_INGOT) < cost) {
                        player.sendSystemMessage(Component.literal("§c[Quartel] Voce precisa de §f" + cost + " Barras de Ferro§c no inventario!"));
                        return;
                    }
                    consumeItem(player, Items.IRON_INGOT, cost);
                }
                kingdom.guardsCount++;
                spawnLeveledGuard(overworld, kingdom, unitType, clampedLevel, kingdom.guardsCount);
                manager.saveToDisk();
                manager.sendHudSnapshot(overworld, player, kingdom);
                String nome = "ARCHER".equals(unitType) ? "Arqueiro" : "Guerreiro";
                player.sendSystemMessage(Component.literal("§9[Quartel] " + nome + " Nv." + clampedLevel + " contratado!"));
            }
            case "MINER" -> {
                if (kingdom.minersCount >= kingdom.getMaxMiners()) {
                    player.sendSystemMessage(Component.literal("§c[Mina] Limite maximo de mineiros atingido (" + kingdom.getMaxMiners() + ")."));
                    return;
                }
                if (!player.isCreative()) {
                    if (countItem(player, Items.OAK_LOG) < cost) {
                        player.sendSystemMessage(Component.literal("§c[Mina] Voce precisa de §f" + cost + " Madeiras de Carvalho§c no inventario!"));
                        return;
                    }
                    consumeItem(player, Items.OAK_LOG, cost);
                }
                kingdom.minersCount++;
                spawnLeveledWorker(overworld, kingdom, "MINER", clampedLevel);
                manager.saveToDisk();
                manager.sendHudSnapshot(overworld, player, kingdom);
                player.sendSystemMessage(Component.literal("§b[Mina] Mineiro Nv." + clampedLevel + " contratado!"));
            }
            case "FARMER" -> {
                if (kingdom.farmersCount >= kingdom.getMaxFarmers()) {
                    player.sendSystemMessage(Component.literal("§c[Plantacao] Limite maximo de fazendeiros atingido (" + kingdom.getMaxFarmers() + ")."));
                    return;
                }
                if (!player.isCreative()) {
                    if (countItem(player, Items.OAK_LOG) < cost) {
                        player.sendSystemMessage(Component.literal("§c[Plantacao] Voce precisa de §f" + cost + " Madeiras de Carvalho§c no inventario!"));
                        return;
                    }
                    consumeItem(player, Items.OAK_LOG, cost);
                }
                kingdom.farmersCount++;
                spawnLeveledWorker(overworld, kingdom, "FARMER", clampedLevel);
                manager.saveToDisk();
                manager.sendHudSnapshot(overworld, player, kingdom);
                player.sendSystemMessage(Component.literal("§a[Plantacao] Fazendeiro Nv." + clampedLevel + " contratado!"));
            }
        }
    }

    private static void spawnLeveledGuard(ServerLevel level, KingdomData.Kingdom kingdom, String unitType, int unitLevel, int index) {
        Mob guard = ModEntities.KINGDOM_GUARD.get().create(level, EntitySpawnReason.COMMAND);
        if (guard == null) return;

        double offsetX = ((index % 3) - 1) * 3.0;
        guard.setPos(kingdom.centerX + offsetX, kingdom.centerY + 1.0, kingdom.centerZ + 4.0);
        String label = ("ARCHER".equals(unitType) ? "§9Arqueiro" : "§9Guerreiro") + " Nv." + unitLevel;
        guard.setCustomName(Component.literal(label));
        guard.setCustomNameVisible(true);
        guard.setPersistenceRequired();

        equipUnitByLevel(guard, unitType, unitLevel);

        CompoundTag tag = guard.getPersistentData();
        tag.putString(TAG_ROLE, ROLE_GUARD_UNIT);
        tag.putString(TAG_OWNER, kingdom.ownerUuid);
        tag.putInt(TAG_LEVEL, unitLevel);

        level.addFreshEntity(guard);
    }

    private static void spawnLeveledWorker(ServerLevel level, KingdomData.Kingdom kingdom, String unitType, int unitLevel) {
        Mob worker = createMob(level, "villager");
        if (worker == null) return;

        boolean isMiner = "MINER".equals(unitType);
        BlockPos base = isMiner ? kingdom.getMinePos() : kingdom.getFarmPos();
        worker.setPos(base.getX() + 1.5, base.getY() + 1.0, base.getZ() + 1.5);
        String label = (isMiner ? "§7Mineiro" : "§2Fazendeiro") + " Nv." + unitLevel;
        worker.setCustomName(Component.literal(label));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();

        equipUnitByLevel(worker, unitType, unitLevel);

        CompoundTag tag = worker.getPersistentData();
        tag.putString(TAG_ROLE, isMiner ? ROLE_MINER_UNIT : ROLE_FARMER_UNIT);
        tag.putString(TAG_OWNER, kingdom.ownerUuid);
        tag.putBoolean(TAG_PROTECTED, true);
        tag.putInt(TAG_LEVEL, unitLevel);

        level.addFreshEntity(worker);
    }

    private static void equipUnitByLevel(Mob mob, String unitType, int level) {
        Item mainHand = switch (unitType) {
            case "WARRIOR" -> switch (level) {
                case 1 -> Items.WOODEN_SWORD;
                case 2 -> Items.STONE_SWORD;
                case 3 -> Items.IRON_SWORD;
                case 4 -> Items.DIAMOND_SWORD;
                default -> Items.NETHERITE_SWORD;
            };
            case "ARCHER" -> Items.BOW;
            case "MINER" -> switch (level) {
                case 1 -> Items.WOODEN_PICKAXE;
                case 2 -> Items.STONE_PICKAXE;
                case 3 -> Items.IRON_PICKAXE;
                case 4 -> Items.DIAMOND_PICKAXE;
                default -> Items.NETHERITE_PICKAXE;
            };
            case "FARMER" -> switch (level) {
                case 1 -> Items.WOODEN_HOE;
                case 2 -> Items.STONE_HOE;
                case 3 -> Items.IRON_HOE;
                case 4 -> Items.DIAMOND_HOE;
                default -> Items.NETHERITE_HOE;
            };
            default -> Items.WOODEN_SWORD;
        };
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(mainHand));

        if ("WARRIOR".equals(unitType) || "ARCHER".equals(unitType)) {
            switch (level) {
                case 1 -> equipArmor(mob, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
                case 2 -> equipArmor(mob, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
                case 3 -> equipArmor(mob, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
                case 4 -> equipArmor(mob, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
                default -> equipArmor(mob, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
            }
        }
    }

    private static void equipArmor(Mob mob, Item head, Item chest, Item legs, Item feet) {
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
    }

    public static void hireUnit(ServerLevel overworld, ServerPlayer player, KingdomData.Kingdom kingdom, String tipo) {
        KingdomManager manager = KingdomManager.get();
        switch (tipo.toLowerCase()) {
            case "guarda" -> {
                if (kingdom.guardsCount >= kingdom.getMaxGuards()) {
                    player.sendSystemMessage(Component.literal("§c[Quartel] Limite maximo de guardas atingido para o Tier atual (" + kingdom.getMaxGuards() + ")."));
                    return;
                }
                if (!player.isCreative()) {
                    if (countItem(player, Items.IRON_INGOT) < 5 || countItem(player, Items.BREAD) < 3) {
                        player.sendSystemMessage(Component.literal("§c[Quartel] Voce precisa de §f5 Barras de Ferro§c e §f3 Paes§c no inventario!"));
                        return;
                    }
                    consumeItem(player, Items.IRON_INGOT, 5);
                    consumeItem(player, Items.BREAD, 3);
                }
                kingdom.guardsCount++;
                syncKingdomNpcs(overworld, kingdom);
                manager.saveToDisk();
                manager.sendHudSnapshot(overworld, player, kingdom);
                player.sendSystemMessage(Component.literal("§9[Quartel] Novo Guarda Real recrutado! Total: " + kingdom.guardsCount + "/" + kingdom.getMaxGuards()));
            }
            case "mineiro" -> {
                if (kingdom.minersCount >= kingdom.getMaxMiners()) {
                    player.sendSystemMessage(Component.literal("§c[Mina] Limite maximo de mineiros atingido para o Tier atual (" + kingdom.getMaxMiners() + ")."));
                    return;
                }
                if (!player.isCreative()) {
                    if (countItem(player, Items.BREAD) < 3) {
                        player.sendSystemMessage(Component.literal("§c[Mina] Voce precisa de §f3 Paes§c para alimentar e contratar um mineiro!"));
                        return;
                    }
                    consumeItem(player, Items.BREAD, 3);
                }
                kingdom.minersCount++;
                syncKingdomNpcs(overworld, kingdom);
                manager.saveToDisk();
                manager.sendHudSnapshot(overworld, player, kingdom);
                player.sendSystemMessage(Component.literal("§b[Mina] Novo Mineiro contratado! Total: " + kingdom.minersCount + "/" + kingdom.getMaxMiners()));
            }
            case "fazendeiro" -> {
                if (kingdom.farmersCount >= kingdom.getMaxFarmers()) {
                    player.sendSystemMessage(Component.literal("§c[Plantacao] Limite maximo de fazendeiros atingido para o Tier atual (" + kingdom.getMaxFarmers() + ")."));
                    return;
                }
                if (!player.isCreative()) {
                    if (countItem(player, Items.IRON_INGOT) < 3) {
                        player.sendSystemMessage(Component.literal("§c[Plantacao] Voce precisa de §f3 Barras de Ferro§c para ferramentas do fazendeiro!"));
                        return;
                    }
                    consumeItem(player, Items.IRON_INGOT, 3);
                }
                kingdom.farmersCount++;
                syncKingdomNpcs(overworld, kingdom);
                manager.saveToDisk();
                manager.sendHudSnapshot(overworld, player, kingdom);
                player.sendSystemMessage(Component.literal("§a[Plantacao] Novo Fazendeiro/Cacador contratado! Total: " + kingdom.farmersCount + "/" + kingdom.getMaxFarmers()));
            }
            default -> player.sendSystemMessage(Component.literal("§cUse: /reino contratar <guarda|mineiro|fazendeiro>"));
        }
    }

    /**
     * Executado periodicamente: deposita os recursos minerados, colhidos e madeira nos baus de cada construcao.
     */
    public static void produceKingdomResources(ServerLevel overworld, KingdomData.Kingdom kingdom) {
        if (kingdom.minersCount > 0) {
            depositNear(overworld, kingdom.getMinePos(), new ItemStack(Items.COBBLESTONE, kingdom.minersCount * 2));
            depositNear(overworld, kingdom.getMinePos(), new ItemStack(Items.COAL, kingdom.minersCount));
            if (overworld.getRandom().nextFloat() < 0.65f) {
                depositNear(overworld, kingdom.getMinePos(), new ItemStack(Items.IRON_INGOT, kingdom.minersCount));
            }
            if (kingdom.currentTier >= 2 && overworld.getRandom().nextFloat() < 0.35f) {
                depositNear(overworld, kingdom.getMinePos(), new ItemStack(Items.GOLD_INGOT, 1));
            }
        }

        if (kingdom.farmersCount > 0) {
            depositNear(overworld, kingdom.getFarmPos(), new ItemStack(Items.WHEAT, kingdom.farmersCount * 2));
            depositNear(overworld, kingdom.getFarmPos(), new ItemStack(Items.BREAD, kingdom.farmersCount));
            depositNear(overworld, kingdom.getFarmPos(), new ItemStack(Items.OAK_LOG, kingdom.farmersCount));
            if (overworld.getRandom().nextFloat() < 0.60f) {
                depositNear(overworld, kingdom.getFarmPos(), new ItemStack(Items.COOKED_BEEF, kingdom.farmersCount));
            }
        }
    }

    private static void depositNear(ServerLevel level, BlockPos center, ItemStack toInsert) {
        if (toInsert.isEmpty()) return;
        BlockPos min = center.offset(-5, -1, -5);
        BlockPos max = center.offset(5, 4, 5);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack slot = container.getItem(i);
                    if (slot.isEmpty()) {
                        container.setItem(i, toInsert.copy());
                        be.setChanged();
                        return;
                    } else if (ItemStack.isSameItemSameComponents(slot, toInsert) && slot.getCount() < slot.getMaxStackSize()) {
                        int canAdd = Math.min(toInsert.getCount(), slot.getMaxStackSize() - slot.getCount());
                        slot.grow(canAdd);
                        toInsert.shrink(canAdd);
                        be.setChanged();
                        if (toInsert.isEmpty()) return;
                    }
                }
            }
        }
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}