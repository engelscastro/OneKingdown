package grouponekingdown.onekingdown.kingdom;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class KingdomHudOverlay {

    private static final float SCALE = 0.70f;
    private static final int BOX_WIDTH = 178;
    private static final int EXPANDED_HEIGHT = 64;
    private static final int COLLAPSED_HEIGHT = 18;
    private static final int MARGIN = 5;

    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.onekingdown.toggle_hud",
            InputConstants.KEY_H,
            KeyMapping.Category.MISC
    );

    private static boolean collapsed = false;

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        while (TOGGLE_HUD_KEY.consumeClick()) {
            collapsed = !collapsed;
        }
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        KingdomHudPayload data = KingdomHudPayload.clientState;
        if (data == null) return;

        GuiGraphicsExtractor gui = event.getGuiGraphics();
        Font font = mc.font;

        int boxHeight = collapsed ? COLLAPSED_HEIGHT : EXPANDED_HEIGHT;
        int x = gui.guiWidth() - Math.round(BOX_WIDTH * SCALE) - MARGIN;
        int y = MARGIN;

        gui.pose().pushMatrix();
        gui.pose().translate(x, y);
        gui.pose().scale(SCALE, SCALE);

        // Fundo discreto semitransparente e borda dourada fina
        gui.fill(0, 0, BOX_WIDTH, boxHeight, 0x6610141D);
        gui.outline(0, 0, BOX_WIDTH, boxHeight, 0x77D4AF37);

        // Cabecalho + indicador da tecla de atalho [H]
        String keyLabel = "[" + TOGGLE_HUD_KEY.getTranslatedKeyMessage().getString().toUpperCase() + "]";
        gui.text(font, "Reino Nv." + data.level() + " (Tier " + data.tier() + ")", 6, 5, 0xFFFFD700);
        gui.text(font, keyLabel, BOX_WIDTH - 24, 5, 0xFFCCCCCC);

        if (!collapsed) {
            gui.horizontalLine(5, BOX_WIDTH - 6, 15, 0x55D4AF37);

            // Coluna esquerda: Recursos e Mundo
            gui.text(font, "Minerios: " + data.ores(), 6, 19, 0xFF55FFFF);
            gui.text(font, "Madeiras: " + data.wood(), 6, 30, 0xFFDEB887);
            gui.text(font, "Comida:   " + data.food(), 6, 41, 0xFF7CFC00);
            gui.text(font, "Animais:  " + data.animals(), 6, 52, 0xFFFFE4B5);

            // Coluna direita: Tropas, Trabalhadores e Ameacas
            gui.text(font, "Guardas:     " + data.guards(), 92, 19, 0xFF6495ED);
            gui.text(font, "Mineiros:    " + data.miners(), 92, 30, 0xFF87CEFA);
            gui.text(font, "Fazendeiros: " + data.farmers(), 92, 41, 0xFF98FB98);

            int monsterColor = data.monsters() > 0 ? 0xFFFF5555 : 0xFFAAAAAA;
            gui.text(font, "Monstros:    " + data.monsters(), 92, 52, monsterColor);
        }

        gui.pose().popMatrix();
    }
}