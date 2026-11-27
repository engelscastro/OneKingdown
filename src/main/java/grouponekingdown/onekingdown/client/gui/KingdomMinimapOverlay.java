package grouponekingdown.onekingdown.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import grouponekingdown.onekingdown.client.map.WorldMapCache;
import grouponekingdown.onekingdown.network.OpenMapPayload;
import grouponekingdown.onekingdown.network.RequestMapPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class KingdomMinimapOverlay {

    public static final KeyMapping TOGGLE_MAP_KEY = new KeyMapping(
            "key.onekingdown.open_map",
            InputConstants.KEY_M,
            KeyMapping.Category.MISC
    );

    private static final int MARGIN = 6;
    private static final int GRID = 42;            // 42x42 celulas
    private static final int CELL_PX = 2;          // cada celula = 2x2 px na tela
    private static final int BLOCKS_PER_CELL = 2;  // cada celula = 2x2 blocos do mundo (alcance 84x84 blocos)
    private static final int MAP_SIZE = GRID * CELL_PX; // 84x84 px
    private static final int SYNC_INTERVAL_TICKS = 100; // atualiza marcadores a cada 5s

    private static boolean visible = true;
    private static int syncTicks = 0;
    private static volatile OpenMapPayload mapData = null;

    private KingdomMinimapOverlay() {}

    public static void updateData(OpenMapPayload data) {
        mapData = data;
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MAP_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            mapData = null;
            syncTicks = 0;
            return;
        }

        while (TOGGLE_MAP_KEY.consumeClick()) {
            visible = !visible;
            if (visible) {
                ClientPacketDistributor.sendToServer(new RequestMapPayload());
                syncTicks = 0;
            }
        }

        if (visible) {
            if (mapData == null || ++syncTicks >= SYNC_INTERVAL_TICKS) {
                syncTicks = 0;
                ClientPacketDistributor.sendToServer(new RequestMapPayload());
            }
        }
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphicsExtractor gui = event.getGuiGraphics();
        Font font = mc.font;

        int mapX = MARGIN;
        int mapY = MARGIN;
        int playerX = mc.player.getBlockX();
        int playerZ = mc.player.getBlockZ();

        // Fundo discreto semitransparente e borda dourada fina (mesmo padrao do HUD)
        gui.fill(mapX - 1, mapY - 1, mapX + MAP_SIZE + 1, mapY + MAP_SIZE + 13, 0x7710141D);
        gui.outline(mapX - 1, mapY - 1, MAP_SIZE + 2, MAP_SIZE + 14, 0x88D4AF37);

        // Desenha terreno agrupando pixels adjacentes da mesma cor (bem mais leve)
        int startWorldX = playerX - (GRID / 2) * BLOCKS_PER_CELL;
        int startWorldZ = playerZ - (GRID / 2) * BLOCKS_PER_CELL;

        for (int gz = 0; gz < GRID; gz++) {
            int wz = startWorldZ + gz * BLOCKS_PER_CELL;
            int cz = wz >> 4;
            int lz = wz & 15;
            int sy = mapY + gz * CELL_PX;

            int runColor = 0;
            int runStartGx = 0;
            boolean hasRun = false;

            for (int gx = 0; gx < GRID; gx++) {
                int wx = startWorldX + gx * BLOCKS_PER_CELL;
                int cx = wx >> 4;
                int lx = wx & 15;

                int[] pixels = WorldMapCache.getChunkPixels(cx, cz);
                int color = (pixels != null) ? pixels[(lz << 4) | lx] : 0;

                if (color == 0) {
                    if (hasRun) {
                        gui.fill(mapX + runStartGx * CELL_PX, sy, mapX + gx * CELL_PX, sy + CELL_PX, runColor);
                        hasRun = false;
                    }
                } else if (!hasRun) {
                    runColor = color;
                    runStartGx = gx;
                    hasRun = true;
                } else if (color != runColor) {
                    gui.fill(mapX + runStartGx * CELL_PX, sy, mapX + gx * CELL_PX, sy + CELL_PX, runColor);
                    runColor = color;
                    runStartGx = gx;
                }
            }
            if (hasRun) {
                gui.fill(mapX + runStartGx * CELL_PX, sy, mapX + GRID * CELL_PX, sy + CELL_PX, runColor);
            }
        }

        // Marcadores do Reino e Invasao
        OpenMapPayload data = mapData;
        if (data != null) {
            drawMarker(gui, mapX, mapY, playerX, playerZ, data.castleX(), data.castleZ(), 0xFFDAA520, true);
            drawMarker(gui, mapX, mapY, playerX, playerZ, data.barracksX(), data.barracksZ(), 0xFF3B82F6, false);
            drawMarker(gui, mapX, mapY, playerX, playerZ, data.mineX(), data.mineZ(), 0xFF06B6D4, false);
            drawMarker(gui, mapX, mapY, playerX, playerZ, data.farmX(), data.farmZ(), 0xFF22C55E, false);

            if (data.hasInvasion()) {
                boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
                int color = blink ? 0xFFEF4444 : 0xFFF97316;
                drawMarker(gui, mapX, mapY, playerX, playerZ, data.citadelX(), data.citadelZ(), color, true);
            }
        }

        // Jogador no centro (ponto branco)
        int centerX = mapX + MAP_SIZE / 2;
        int centerY = mapY + MAP_SIZE / 2;
        gui.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFF000000);
        gui.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, 0xFFFFFFFF);

        // Indicador "N" (Norte) discreto + Coordenadas e tecla [M] no rodape
        gui.pose().pushMatrix();
        gui.pose().translate(mapX + MAP_SIZE / 2 - 2, mapY + 2);
        gui.pose().scale(0.65f, 0.65f);
        gui.text(font, "N", 0, 0, 0xFFFFD700);
        gui.pose().popMatrix();

        String coords = playerX + ", " + playerZ;
        String keyLabel = "[" + TOGGLE_MAP_KEY.getTranslatedKeyMessage().getString().toUpperCase() + "]";

        gui.pose().pushMatrix();
        gui.pose().translate(mapX + 3, mapY + MAP_SIZE + 3);
        gui.pose().scale(0.65f, 0.65f);
        gui.text(font, coords, 0, 0, 0xFFE5E5E5);
        gui.text(font, keyLabel, Math.round((MAP_SIZE - 18) / 0.65f), 0, 0xFFCCCCCC);
        gui.pose().popMatrix();
    }

    private static void drawMarker(GuiGraphicsExtractor gui, int mapX, int mapY,
                                   int playerX, int playerZ, int worldX, int worldZ,
                                   int color, boolean clampToEdge) {
        int dx = Math.round((worldX - playerX) * ((float) CELL_PX / BLOCKS_PER_CELL));
        int dz = Math.round((worldZ - playerZ) * ((float) CELL_PX / BLOCKS_PER_CELL));

        int half = MAP_SIZE / 2;
        int maxOffset = half - 3;

        if (!clampToEdge && (Math.abs(dx) > maxOffset || Math.abs(dz) > maxOffset)) {
            return;
        }

        int px = mapX + half + Math.max(-maxOffset, Math.min(maxOffset, dx));
        int py = mapY + half + Math.max(-maxOffset, Math.min(maxOffset, dz));

        gui.fill(px - 2, py - 2, px + 2, py + 2, 0xFF000000);
        gui.fill(px - 1, py - 1, px + 1, py + 1, color);
    }
}