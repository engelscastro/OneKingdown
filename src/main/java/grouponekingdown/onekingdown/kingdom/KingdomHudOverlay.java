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

        int x = 8;
        int y = 8;
        int width = 148;
        int height = 86;

        // Fundo escuro semitransparente + borda dourada
        gui.fill(x, y, x + width, y + height, 0xB0121622);
        gui.outline(x, y, width, height, 0xFFD4AF37);

        // Cabecalho do Reino
        gui.text(font, "Reino Nv." + data.level() + " (Tier " + data.tier() + ")", x + 6, y + 6, 0xFFFFD700);
        gui.horizontalLine(x + 4, x + width - 5, y + 17, 0x80D4AF37);

        // Status de recursos e entidades do territorio
        gui.text(font, "Minerios: " + data.ores(), x + 6, y + 21, 0xFF55FFFF);
        gui.text(font, "Madeiras: " + data.wood(), x + 6, y + 31, 0xFFDEB887);
        gui.text(font, "Comida:   " + data.food(), x + 6, y + 41, 0xFF7CFC00);
        gui.text(font, "Guardas:  " + data.guards(), x + 6, y + 51, 0xFF6495ED);
        gui.text(font, "Animais:  " + data.animals(), x + 6, y + 61, 0xFFFFE4B5);

        int monsterColor = data.monsters() > 0 ? 0xFFFF5555 : 0xFFAAAAAA;
        gui.text(font, "Monstros: " + data.monsters(), x + 6, y + 71, monsterColor);
    }
}