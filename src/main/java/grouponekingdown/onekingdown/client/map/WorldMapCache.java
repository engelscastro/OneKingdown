package grouponekingdown.onekingdown.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldMapCache {

    private static final int SCAN_RADIUS_CHUNKS = 3;
    private static final int SCAN_INTERVAL_TICKS = 20;

    // Guarda 16x16 pixels (ARGB) por chunk descoberto
    private static final Map<Long, int[]> CHUNK_PIXELS = new ConcurrentHashMap<>();
    private static ResourceKey<Level> lastDimension = null;
    private static int tickCounter = 0;

    private WorldMapCache() {}

    private static long chunkKey(int cx, int cz) {
        return (((long) cx) & 0xFFFFFFFFL) | ((((long) cz) & 0xFFFFFFFFL) << 32);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!CHUNK_PIXELS.isEmpty()) {
                CHUNK_PIXELS.clear();
                lastDimension = null;
            }
            return;
        }

        // Limpa o cache ao trocar de dimensao
        ResourceKey<Level> dim = mc.level.dimension();
        if (!dim.equals(lastDimension)) {
            CHUNK_PIXELS.clear();
            lastDimension = dim;
        }

        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        int playerChunkX = mc.player.getBlockX() >> 4;
        int playerChunkZ = mc.player.getBlockZ() >> 4;

        int scanned = 0;
        for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS && scanned < 4; dx++) {
            for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS && scanned < 4; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                long key = chunkKey(cx, cz);

                if (CHUNK_PIXELS.containsKey(key) && (dx != 0 || dz != 0)) {
                    continue;
                }

                if (mc.level.hasChunk(cx, cz)) {
                    LevelChunk chunk = mc.level.getChunk(cx, cz);
                    CHUNK_PIXELS.put(key, sampleChunk(mc.level, chunk, cx, cz));
                    scanned++;
                }
            }
        }
    }

    private static int[] sampleChunk(Level level, LevelChunk chunk, int chunkX, int chunkZ) {
        int[] pixels = new int[256];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int worldX = baseX + lx;
                int worldZ = baseZ + lz;
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);

                pos.set(worldX, y, worldZ);
                BlockState state = chunk.getBlockState(pos);
                while (y > level.getMinY() && state.getMapColor(level, pos) == MapColor.NONE) {
                    y--;
                    pos.set(worldX, y, worldZ);
                    state = chunk.getBlockState(pos);
                }

                MapColor mapColor = state.getMapColor(level, pos);
                int rgb = mapColor.col;

                // Sombreamento de relevo (compara altura com o bloco ao norte)
                int northY = (lz > 0)
                        ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz - 1)
                        : y;
                float shade = 1.0f;
                if (y > northY) shade = 1.15f;
                else if (y < northY) shade = 0.85f;

                int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * shade));
                int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * shade));
                int b = Math.min(255, (int) ((rgb & 0xFF) * shade));

                pixels[lz * 16 + lx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        return pixels;
    }

    public static int[] getChunkPixels(int chunkX, int chunkZ) {
        return CHUNK_PIXELS.get(chunkKey(chunkX, chunkZ));
    }

    public static boolean isExplored(int worldX, int worldZ) {
        return CHUNK_PIXELS.containsKey(chunkKey(worldX >> 4, worldZ >> 4));
    }
}