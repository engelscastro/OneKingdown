package grouponekingdown.onekingdown.kingdom;

import com.mojang.logging.LogUtils;
import grouponekingdown.onekingdown.Onekingdown;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CastleStructureBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void buildOrUpgradeCastle(ServerLevel level, KingdomData.Kingdom kingdom) {
        int tier = kingdom.currentTier;

        // 1. Preservar itens separados por setor antes da reconstrucao
        List<ItemStack> savedFarmItems = collectAndClearArea(level, kingdom.getFarmPos(), 18);
        List<ItemStack> savedMineItems = collectAndClearArea(level, kingdom.getMinePos(), 18);
        List<ItemStack> savedBarracksItems = collectAndClearArea(level, kingdom.getBarracksPos(), 18);
        List<ItemStack> savedCastleItems = collectAndClearContainers(level, kingdom);

        // 2. Nivelar e limpar a plataforma completa do reino
        prepareTerritoryPlatform(level, kingdom);

        // 3. Construir Castelo no centro + Quartel (Leste) + Mina (Oeste) + Plantacao (Norte)
        if (!tryPlaceNbt(level, "castle_tier_" + tier, kingdom.getCenterPos())) {
            buildProceduralCastle(level, kingdom, tier);
        }

        if (!tryPlaceNbt(level, "barracks_tier_" + tier, kingdom.getBarracksPos())) {
            buildProceduralBarracks(level, kingdom.getBarracksPos(), tier);
        }

        if (!tryPlaceNbt(level, "mine_tier_" + tier, kingdom.getMinePos())) {
            buildProceduralMine(level, kingdom.getMinePos(), tier);
        }

        BlockPos farmChestPos = null;
        String farmNbt = getFarmNbtForTier(tier);
        Rotation farmRotation = (tier >= 4) ? Rotation.CLOCKWISE_180 : Rotation.NONE;
        Vec3i farmSize = (farmNbt != null) ? tryPlaceNbtWithSize(level, farmNbt, kingdom.getFarmPos(), farmRotation) : null;
        if (farmSize == null) {
            farmSize = tryPlaceNbtWithSize(level, "farm_tier_" + tier, kingdom.getFarmPos(), Rotation.NONE);
        }
        if (farmSize == null) {
            buildProceduralFarm(level, kingdom.getFarmPos(), tier);
        } else {
            // Limpa itens nativos que venham dentro do proprio NBT para nao duplicar com os salvos
            clearContainersInArea(level, kingdom.getFarmPos(), 18);
            farmChestPos = placeChestNextToNbt(level, kingdom.getFarmPos(), farmSize);
        }

        // 4. Caminhos de pedra ligando o Castelo as construcoes
        buildConnectingPaths(level, kingdom);

        // 5. Restaurar cada setor apenas nos seus proprios baus
        if (farmChestPos != null && !savedFarmItems.isEmpty()) {
            BlockEntity be = level.getBlockEntity(farmChestPos);
            if (be instanceof Container container) {
                container.clearContent();
                insertIntoContainer(container, savedFarmItems);
                be.setChanged();
            }
        } else {
            restoreItemsToArea(level, kingdom.getFarmPos(), 18, savedFarmItems);
        }

        restoreItemsToArea(level, kingdom.getMinePos(), 18, savedMineItems);
        restoreItemsToArea(level, kingdom.getBarracksPos(), 18, savedBarracksItems);

        // Apenas itens que nao couberam na fazenda vao para o castelo (sem duplicar)
        savedCastleItems.addAll(savedFarmItems);
        savedCastleItems.addAll(savedMineItems);
        savedCastleItems.addAll(savedBarracksItems);
        restoreItemsToArea(level, kingdom.getCenterPos(), 20, savedCastleItems);

        // Fallback caso os baus do castelo tenham enchido
        restoreFallbackChests(level, kingdom, savedCastleItems);
    }

    private static String getFarmNbtForTier(int tier) {
        if (tier >= 4) {
            return "farm_house_nv4_by_norcik1";
        } else if (tier >= 2) {
            return "farm_house_nv2_by_potatoplaysgames";
        }
        return null; // Tier 1 usa a plantacao basica procedural
    }

    private static boolean tryPlaceNbt(ServerLevel level, String name, BlockPos center) {
        return tryPlaceNbtWithSize(level, name, center, Rotation.NONE) != null;
    }

    private static Vec3i tryPlaceNbtWithSize(ServerLevel level, String name, BlockPos center, Rotation rotation) {
        Identifier structureId = Identifier.fromNamespaceAndPath(Onekingdown.MODID, name);
        Optional<StructureTemplate> templateOpt = level.getStructureTemplateManager().get(structureId);
        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            Vec3i size = template.getSize();
            BlockPos placeOrigin = new BlockPos(
                    center.getX() - (size.getX() / 2),
                    center.getY(),
                    center.getZ() - (size.getZ() / 2)
            );
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setRotation(rotation)
                    .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2));

            template.placeInWorld(level, placeOrigin, placeOrigin, settings, level.getRandom(), Block.UPDATE_ALL);
            LOGGER.info("Schematic NBT {} colocado em {} (rotacao {})", structureId, center, rotation);
            return size;
        }
        return null;
    }

    private static BlockPos placeChestNextToNbt(ServerLevel level, BlockPos center, Vec3i size) {
        int southEdgeZ = center.getZ() - (size.getZ() / 2) + size.getZ();
        BlockPos chestPos = new BlockPos(center.getX() + 2, center.getY() + 1, southEdgeZ);

        level.setBlock(chestPos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(chestPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        return chestPos;
    }

    private static void clearContainersInArea(ServerLevel level, BlockPos center, int radius) {
        BlockPos min = center.offset(-radius, -2, -radius);
        BlockPos max = center.offset(radius, 25, radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                container.clearContent();
                be.setChanged();
            }
        }
    }

    private static List<ItemStack> collectAndClearArea(ServerLevel level, BlockPos center, int radius) {
        List<ItemStack> items = new ArrayList<>();
        BlockPos min = center.offset(-radius, -2, -radius);
        BlockPos max = center.offset(radius, 25, radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        items.add(stack.copy());
                    }
                }
                container.clearContent();
                be.setChanged();
            }
        }
        return items;
    }

    private static List<ItemStack> collectAndClearContainers(ServerLevel level, KingdomData.Kingdom kingdom) {
        List<ItemStack> items = new ArrayList<>();
        BlockPos min = new BlockPos(kingdom.getMinX(), kingdom.centerY - 2, kingdom.getMinZ());
        BlockPos max = new BlockPos(kingdom.getMaxX(), kingdom.centerY + 35, kingdom.getMaxZ());

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        items.add(stack.copy());
                    }
                }
                container.clearContent();
                be.setChanged();
            }
        }
        return items;
    }

    private static void restoreItemsToArea(ServerLevel level, BlockPos center, int radius, List<ItemStack> remaining) {
        if (remaining.isEmpty()) return;
        BlockPos min = center.offset(-radius, -1, -radius);
        BlockPos max = center.offset(radius, 25, radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (remaining.isEmpty()) break;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity container) {
                insertIntoContainer(container, remaining);
                be.setChanged();
            }
        }
    }

    private static void restoreFallbackChests(ServerLevel level, KingdomData.Kingdom kingdom, List<ItemStack> remaining) {
        int offset = 0;
        while (!remaining.isEmpty() && offset < 10) {
            BlockPos fallbackPos = new BlockPos(kingdom.centerX + 2 + offset, kingdom.centerY + 1, kingdom.centerZ);
            level.setBlock(fallbackPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            BlockEntity be = level.getBlockEntity(fallbackPos);
            if (be instanceof Container container) {
                insertIntoContainer(container, remaining);
                be.setChanged();
            }
            offset++;
        }
    }

    private static void prepareTerritoryPlatform(ServerLevel level, KingdomData.Kingdom kingdom) {
        int minX = kingdom.getMinX();
        int maxX = kingdom.getMaxX();
        int minZ = kingdom.getMinZ();
        int maxZ = kingdom.getMaxZ();
        int baseY = kingdom.centerY;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = baseY - 3; y < baseY; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                }

                boolean isBorder = (x == minX || x == maxX || z == minZ || z == maxZ);
                BlockState surface = isBorder
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState();
                level.setBlock(new BlockPos(x, baseY, z), surface, Block.UPDATE_ALL);

                for (int y = baseY + 1; y <= baseY + 26; y++) {
                    BlockPos airPos = new BlockPos(x, y, z);
                    if (!level.getBlockState(airPos).isAir()) {
                        level.setBlock(airPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static void buildConnectingPaths(ServerLevel level, KingdomData.Kingdom kingdom) {
        int cx = kingdom.centerX;
        int cy = kingdom.centerY;
        int cz = kingdom.centerZ;

        int minPathX = Math.min(kingdom.getMinePos().getX(), cx);
        int maxPathX = Math.max(kingdom.getBarracksPos().getX(), cx);
        int minPathZ = Math.min(kingdom.getFarmPos().getZ(), cz);

        // Caminho Leste-Oeste (Mina <-> Castelo <-> Quartel)
        for (int x = minPathX; x <= maxPathX; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = new BlockPos(x, cy, cz + dz);
                if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        // Caminho Norte (Castelo <-> Plantacao)
        for (int z = minPathZ; z <= cz; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos pos = new BlockPos(cx + dx, cy, z);
                if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static void buildProceduralCastle(ServerLevel level, KingdomData.Kingdom kingdom, int tier) {
        int clampedTier = Math.min(tier, 4);
        int radius = 5 + (clampedTier * 2); // Tier 1: 15x15 ... Tier 4: 27x27
        int wallHeight = 3 + (clampedTier * 2);

        BlockState wallBlock = switch (clampedTier) {
            case 1 -> Blocks.OAK_PLANKS.defaultBlockState();
            case 2 -> Blocks.COBBLESTONE.defaultBlockState();
            case 3 -> Blocks.STONE_BRICKS.defaultBlockState();
            default -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        };

        BlockState pillarBlock = switch (clampedTier) {
            case 1 -> Blocks.OAK_LOG.defaultBlockState();
            case 2 -> Blocks.STONE_BRICKS.defaultBlockState();
            case 3 -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            default -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        };

        BlockState floorBlock = switch (clampedTier) {
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            case 2 -> Blocks.STONE_BRICKS.defaultBlockState();
            default -> Blocks.POLISHED_ANDESITE.defaultBlockState();
        };

        int cx = kingdom.centerX;
        int cy = kingdom.centerY;
        int cz = kingdom.centerZ;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                level.setBlock(new BlockPos(x, cy, z), floorBlock, Block.UPDATE_ALL);
            }
        }

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                boolean isEdgeX = (x == cx - radius || x == cx + radius);
                boolean isEdgeZ = (z == cz - radius || z == cz + radius);
                if (!isEdgeX && !isEdgeZ) continue;

                boolean isCorner = isEdgeX && isEdgeZ;
                boolean isGate = ((z == cz + radius || z == cz - radius) && Math.abs(x - cx) <= 1)
                        || ((x == cx + radius || x == cx - radius) && Math.abs(z - cz) <= 1);

                for (int dy = 1; dy <= wallHeight; dy++) {
                    if (isGate && dy <= 3) continue;
                    level.setBlock(new BlockPos(x, cy + dy, z), isCorner ? pillarBlock : wallBlock, Block.UPDATE_ALL);
                }

                if (!isCorner && ((x + z) % 2 == 0)) {
                    level.setBlock(new BlockPos(x, cy + wallHeight + 1, z), wallBlock, Block.UPDATE_ALL);
                }
            }
        }

        // Trono do Rei e bau real
        level.setBlock(new BlockPos(cx, cy, cz), Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx, cy + 1, cz - 2), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx - 2, cy + 1, cz - 2), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 2, cy + 1, cz - 2), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Quartel (Leste): Estrutura militar com muralha baixa, beliches, bigorna e bau de suprimentos.
     */
    private static void buildProceduralBarracks(ServerLevel level, BlockPos center, int tier) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int r = 4;

        BlockState wall = tier >= 2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState roof = Blocks.DARK_OAK_SLAB.defaultBlockState();

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                level.setBlock(new BlockPos(x, cy, z), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
                boolean edge = (x == cx - r || x == cx + r || z == cz - r || z == cz + r);
                boolean entrance = (x == cx - r && Math.abs(z - cz) <= 1);
                if (edge && !entrance) {
                    for (int dy = 1; dy <= 3; dy++) {
                        level.setBlock(new BlockPos(x, cy + dy, z), wall, Block.UPDATE_ALL);
                    }
                }
                level.setBlock(new BlockPos(x, cy + 4, z), roof, Block.UPDATE_ALL);
            }
        }

        // Equipamentos internos do Quartel
        level.setBlock(new BlockPos(cx + 2, cy + 1, cz - 2), Blocks.ANVIL.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 2, cy + 1, cz + 2), Blocks.SMITHING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx, cy + 1, cz - 3), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx, cy + 3, cz), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Mina (Oeste): Entrada de galeria escorada com madeira, trilhos, minerios expostos e bau de coleta.
     */
    private static void buildProceduralMine(ServerLevel level, BlockPos center, int tier) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int r = 4;

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                level.setBlock(new BlockPos(x, cy, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // Arco de entrada da mina e monte de pedra/minerios
        for (int z = cz - 2; z <= cz + 2; z++) {
            level.setBlock(new BlockPos(cx - 2, cy + 1, z), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(new BlockPos(cx - 2, cy + 2, z), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(new BlockPos(cx - 2, cy + 3, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
        }

        // Bloco de minerios decorativos + trilho + bau de producao da mina
        level.setBlock(new BlockPos(cx - 3, cy + 1, cz - 1), Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx - 3, cy + 1, cz + 1), Blocks.GOLD_ORE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx - 3, cy + 2, cz), Blocks.COAL_ORE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx - 1, cy + 1, cz), Blocks.RAIL.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 1, cy + 1, cz - 2), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 1, cy + 1, cz + 2), Blocks.BLAST_FURNACE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx - 2, cy + 4, cz), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Plantacao (Norte): Horta irrigada cercada com trigo maduro, feno, defumador e bau de alimentos.
     */
    private static void buildProceduralFarm(ServerLevel level, BlockPos center, int tier) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int r = 4;

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean border = (x == cx - r || x == cx + r || z == cz - r || z == cz + r);
                boolean gate = (z == cz + r && Math.abs(x - cx) <= 1);
                if (border) {
                    level.setBlock(new BlockPos(x, cy, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    if (!gate) {
                        level.setBlock(new BlockPos(x, cy + 1, z), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
                    }
                } else if (x == cx && z == cz) {
                    level.setBlock(new BlockPos(x, cy, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                } else {
                    level.setBlock(new BlockPos(x, cy, z), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(new BlockPos(x, cy + 1, z), Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        // Area de preparo de pao e carne na entrada da plantacao
        level.setBlock(new BlockPos(cx - 2, cy + 1, cz + 3), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 2, cy + 1, cz + 3), Blocks.SMOKER.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 3, cy + 1, cz + 3), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void insertIntoContainer(Container container, List<ItemStack> remaining) {
        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, remaining.remove(0));
            }
        }
    }
}