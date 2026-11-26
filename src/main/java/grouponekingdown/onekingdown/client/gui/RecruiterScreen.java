package grouponekingdown.onekingdown.client.gui;

import grouponekingdown.onekingdown.Onekingdown;
import grouponekingdown.onekingdown.network.HireUnitPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class RecruiterScreen extends Screen {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Onekingdown.MODID, "textures/gui/recruiter_gui.png");

    private static final int GUI_SIZE = 220;

    private final String recruiterRole;
    private final int ores;
    private final int wood;
    private final int food;

    private String selectedUnitType;
    private int leftPos;
    private int topPos;

    public RecruiterScreen(String recruiterRole, int ores, int wood, int food) {
        super(Component.literal(getTitleForRole(recruiterRole)));
        this.recruiterRole = recruiterRole;
        this.ores = ores;
        this.wood = wood;
        this.food = food;
        this.selectedUnitType = defaultUnitForRole(recruiterRole);
    }

    private static String getTitleForRole(String role) {
        return switch (role) {
            case "GUARD" -> "Lider da Guarda";
            case "MINE" -> "Mestre da Mina";
            case "FARM" -> "Mestre das Fazendas";
            default -> "Contratacao";
        };
    }

    private static String defaultUnitForRole(String role) {
        return switch (role) {
            case "GUARD" -> "WARRIOR";
            case "MINE" -> "MINER";
            case "FARM" -> "FARMER";
            default -> "WARRIOR";
        };
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_SIZE) / 2;
        this.topPos = (this.height - GUI_SIZE) / 2;
        rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();

        int contentX = this.leftPos + 20;
        int contentWidth = GUI_SIZE - 40; // 180px dentro do pergaminho

        // Abas Guerreiro / Arqueiro apenas para o Lider da Guarda
        if ("GUARD".equals(this.recruiterRole)) {
            int tabW = (contentWidth - 4) / 2;
            this.addRenderableWidget(Button.builder(
                    Component.literal("WARRIOR".equals(selectedUnitType) ? "[Guerreiro]" : "Guerreiro"),
                    btn -> {
                        this.selectedUnitType = "WARRIOR";
                        rebuildButtons();
                    }
            ).bounds(contentX, this.topPos + 74, tabW, 16).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("ARCHER".equals(selectedUnitType) ? "[Arqueiro]" : "Arqueiro"),
                    btn -> {
                        this.selectedUnitType = "ARCHER";
                        rebuildButtons();
                    }
            ).bounds(contentX + tabW + 4, this.topPos + 74, tabW, 16).build());
        }

        // 5 botoes de nivel (Nv. 1 a Nv. 5)
        int startY = "GUARD".equals(this.recruiterRole) ? this.topPos + 94 : this.topPos + 82;
        for (int level = 1; level <= 5; level++) {
            final int lv = level;
            String label = getLevelLabel(this.selectedUnitType, lv);
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> {
                        ClientPacketDistributor.sendToServer(
                                new HireUnitPayload(this.recruiterRole, this.selectedUnitType, lv)
                        );
                        this.onClose();
                    }
            ).bounds(contentX, startY + (lv - 1) * 16, contentWidth, 15).build());
        }
    }

    private String getLevelLabel(String unitType, int level) {
        String tier = switch (level) {
            case 1 -> "Madeira";
            case 2 -> "Pedra";
            case 3 -> "Ferro";
            case 4 -> "Diamante";
            case 5 -> "Netherita";
            default -> "Madeira";
        };
        int cost = level * 15;
        return switch (unitType) {
            case "WARRIOR" -> "Nv." + level + " Esp." + tier + " (" + cost + " Min)";
            case "ARCHER" -> "Nv." + level + " Arco/" + tier + " (" + cost + " Min)";
            case "MINER" -> "Nv." + level + " Pic." + tier + " (" + cost + " Mad)";
            case "FARMER" -> "Nv." + level + " Enx." + tier + " (" + cost + " Mad)";
            default -> "Nv." + level;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        // Fundo do pergaminho com espadas (220x220)
        gui.blit(TEXTURE, this.leftPos, this.topPos, this.leftPos + GUI_SIZE, this.topPos + GUI_SIZE, 0f, 1f, 0f, 1f);

        // Titulo e saldo de recursos em marrom escuro sobre o pergaminho
        int centerX = this.leftPos + GUI_SIZE / 2;
        gui.centeredText(this.font, this.title, centerX, this.topPos + 51, 0xFF3B2410);

        String resources = "Min:" + ores + "  Mad:" + wood + "  Com:" + food;
        gui.centeredText(this.font, resources, centerX, this.topPos + 62, 0xFF5A391A);

        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}