package grouponekingdown.onekingdown.kingdom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class KingdomHudOverlay {

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        KingdomHudPayload data = KingdomHudPayload.clientState;
        if (data == null) return;

        GuiGraphicsExtractor gui = event.getGuiGraphics();
        Font font = mc.font;

        float scale = 0.70f;
        int boxWidth = 178;
        int boxHeight = 64;
        int margin = 5;

        int x = gui.guiWidth() - Math.round(boxWidth * scale) - margin;
        int y = margin;

        gui.pose().pushMatrix();
        gui.pose().translate(x, y);
        gui.pose().scale(scale, scale);

        // Fundo discreto semitransparente e borda dourada fina
        gui.fill(0, 0, boxWidth, boxHeight, 0x6610141D);
        gui.outline(0, 0, boxWidth, boxHeight, 0x77D4AF37);

        // Cabecalho
        gui.text(font, "Reino Nv." + data.level() + " (Tier " + data.tier() + ")", 6, 5, 0xFFFFD700);
        gui.horizontalLine(5, boxWidth - 6, 15, 0x55D4AF37);

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

        gui.pose().popMatrix();
    }
}