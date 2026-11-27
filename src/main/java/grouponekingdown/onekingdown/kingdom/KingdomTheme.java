package grouponekingdown.onekingdown.kingdom;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum KingdomTheme {
    PLAINS("Planicie", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE_BRICKS, Blocks.DIRT_PATH),
    DESERT("Deserto (Areia)", Blocks.SAND, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE),
    ICE("Cidade do Gelo", Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.PACKED_ICE),
    TAIGA("Floresta Taiga", Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MOSSY_STONE_BRICKS, Blocks.SPRUCE_PLANKS),
    VOLCANIC("Terras Vulcanicas", Blocks.BLACKSTONE, Blocks.BASALT, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.MAGMA_BLOCK);

    public final String displayName;
    public final Block surfaceBlock;
    public final Block subSurfaceBlock;
    public final Block borderBlock;
    public final Block pathBlock;

    KingdomTheme(String displayName, Block surfaceBlock, Block subSurfaceBlock, Block borderBlock, Block pathBlock) {
        this.displayName = displayName;
        this.surfaceBlock = surfaceBlock;
        this.subSurfaceBlock = subSurfaceBlock;
        this.borderBlock = borderBlock;
        this.pathBlock = pathBlock;
    }

    public KingdomTheme next() {
        KingdomTheme[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static KingdomTheme byOrdinal(int idx) {
        KingdomTheme[] values = values();
        if (idx < 0 || idx >= values.length) return PLAINS;
        return values[idx];
    }

    public enum BannerColor {
        RED("Vermelho", 0xFFD93838, "red_banner"),
        BLUE("Azul", 0xFF3875D9, "blue_banner"),
        GREEN("Verde", 0xFF38B04A, "green_banner"),
        YELLOW("Amarelo", 0xFFE5C838, "yellow_banner"),
        PURPLE("Roxo", 0xFF9B42D9, "purple_banner"),
        WHITE("Branco", 0xFFF0F0F0, "white_banner"),
        BLACK("Preto", 0xFF2B2B2B, "black_banner"),
        ORANGE("Laranja", 0xFFE67E22, "orange_banner");

        public final String displayName;
        public final int argbColor;
        public final String bannerBlockId;

        BannerColor(String displayName, int argbColor, String bannerBlockId) {
            this.displayName = displayName;
            this.argbColor = argbColor;
            this.bannerBlockId = bannerBlockId;
        }

        public Block getBannerBlock() {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", bannerBlockId));
            return (block != null && block != Blocks.AIR) ? block : Blocks.OAK_FENCE;
        }

        public BannerColor next() {
            BannerColor[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static BannerColor byOrdinal(int idx) {
            BannerColor[] values = values();
            if (idx < 0 || idx >= values.length) return RED;
            return values[idx];
        }
    }
}