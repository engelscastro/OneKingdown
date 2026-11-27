package grouponekingdown.onekingdown.client.gui;

import grouponekingdown.onekingdown.kingdom.KingdomTheme;
import grouponekingdown.onekingdown.network.ConfirmRoomSetupPayload;
import grouponekingdown.onekingdown.network.JoinRoomCodePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class RoomSetupScreen extends Screen {
    private static final int MAX_SLOTS = 5;

    private final String roomCode;
    private final boolean roomAlreadyConfigured;
    private boolean joinTabActive;

    private int selectedPlayers;
    private final KingdomTheme[] slotThemes = new KingdomTheme[MAX_SLOTS];
    private final KingdomTheme.BannerColor[] slotBanners = new KingdomTheme.BannerColor[MAX_SLOTS];

    private EditBox codeInput;
    private String typedCodeDraft = "";
    private String feedbackMessage = "";

    // Metricas responsivas recalculadas a cada resize/init
    private int panelW;
    private int panelH;
    private int left;
    private int topY;
    private int padding;
    private int innerLeft;
    private int innerRight;
    private int innerW;
    private int tabsOffsetY;
    private int rowH;
    private int rowStep;
    private int rowsStartY;
    private int swatchW;
    private int labelW;
    private int terrainBtnX;
    private int terrainBtnW;
    private int bannerBtnX;
    private int bannerBtnW;
    private int swatchX;
    private int countControlsX;
    private boolean compactMode;

    public RoomSetupScreen(String roomCode, boolean roomAlreadyConfigured, int maxPlayers) {
        super(Component.literal("Configuracao da Sala - OneKingdown"));
        this.roomCode = roomCode;
        this.roomAlreadyConfigured = roomAlreadyConfigured;
        this.joinTabActive = roomAlreadyConfigured;
        this.selectedPlayers = Math.max(1, Math.min(MAX_SLOTS, maxPlayers > 0 ? maxPlayers : 2));

        KingdomTheme[] defaults = {
                KingdomTheme.PLAINS,
                KingdomTheme.DESERT,
                KingdomTheme.ICE,
                KingdomTheme.TAIGA,
                KingdomTheme.VOLCANIC
        };
        KingdomTheme.BannerColor[] defaultColors = {
                KingdomTheme.BannerColor.RED,
                KingdomTheme.BannerColor.BLUE,
                KingdomTheme.BannerColor.GREEN,
                KingdomTheme.BannerColor.YELLOW,
                KingdomTheme.BannerColor.PURPLE
        };
        for (int i = 0; i < MAX_SLOTS; i++) {
            slotThemes[i] = defaults[i];
            slotBanners[i] = defaultColors[i];
        }
    }

    private void computeLayout() {
        int marginX = 12;
        int marginY = 10;

        this.panelW = Math.min(360, Math.max(220, this.width - (marginX * 2)));
        this.compactMode = this.panelW < 300;
        this.padding = this.compactMode ? 8 : 14;
        this.left = (this.width - this.panelW) / 2;
        this.innerLeft = this.left + this.padding;
        this.innerRight = this.left + this.panelW - this.padding;
        this.innerW = this.innerRight - this.innerLeft;
        this.tabsOffsetY = this.roomAlreadyConfigured ? 0 : 24;

        if (this.joinTabActive) {
            int desiredH = 144 + this.tabsOffsetY;
            this.panelH = Math.min(desiredH, Math.max(126, this.height - (marginY * 2)));
            this.topY = Math.max(marginY, (this.height - this.panelH) / 2);
            return;
        }

        int availH = this.height - (marginY * 2);
        int headerH = 72 + this.tabsOffsetY;
        int footerH = 44;
        int idealRowH = 20;
        int idealGap = 4;
        int neededH = headerH + (selectedPlayers * (idealRowH + idealGap)) + footerH;

        if (neededH <= availH) {
            this.rowH = idealRowH;
            this.rowStep = idealRowH + idealGap;
            this.panelH = neededH;
        } else {
            int maxRowsSpace = Math.max(selectedPlayers * 16, availH - headerH - footerH);
            this.rowStep = Math.max(16, maxRowsSpace / Math.max(1, selectedPlayers));
            this.rowH = Math.max(14, this.rowStep - 2);
            this.panelH = Math.min(availH, headerH + (selectedPlayers * this.rowStep) + footerH);
        }

        this.topY = Math.max(marginY, (this.height - this.panelH) / 2);
        this.rowsStartY = this.topY + headerH;

        int gap = 4;
        this.labelW = this.compactMode ? 22 : 48;
        this.swatchW = 12;
        int usableForButtons = Math.max(100, this.innerW - this.labelW - this.swatchW - (gap * 3));

        this.terrainBtnW = (int) Math.round(usableForButtons * 0.56);
        this.bannerBtnW = usableForButtons - this.terrainBtnW;

        this.terrainBtnX = this.innerLeft + this.labelW + gap;
        this.bannerBtnX = this.terrainBtnX + this.terrainBtnW + gap;
        this.swatchX = this.bannerBtnX + this.bannerBtnW + gap;

        this.countControlsX = this.innerRight - 72;
    }

    @Override
    protected void init() {
        if (this.codeInput != null) {
            this.typedCodeDraft = this.codeInput.getValue();
        }
        this.clearWidgets();
        this.computeLayout();

        int centerX = this.width / 2;

        // Abas no topo (quando a sala ainda nao foi configurada)
        if (!this.roomAlreadyConfigured) {
            int tabGap = 6;
            int tabW = (this.innerW - tabGap) / 2;
            int tabY = this.topY + 22;

            Button hostTabBtn = Button.builder(
                    Component.literal(this.compactMode ? "Criar Sala" : "Criar / Configurar Sala"),
                    btn -> {
                        this.joinTabActive = false;
                        this.feedbackMessage = "";
                        this.init();
                    }
            ).bounds(this.innerLeft, tabY, tabW, 18).build();
            hostTabBtn.active = this.joinTabActive;
            this.addRenderableWidget(hostTabBtn);

            Button joinTabBtn = Button.builder(
                    Component.literal(this.compactMode ? "Usar Codigo" : "Entrar com Codigo"),
                    btn -> {
                        this.joinTabActive = true;
                        this.feedbackMessage = "";
                        this.init();
                    }
            ).bounds(this.innerLeft + tabW + tabGap, tabY, tabW, 18).build();
            joinTabBtn.active = !this.joinTabActive;
            this.addRenderableWidget(joinTabBtn);
        }

        if (this.joinTabActive) {
            int inputW = Math.min(200, this.innerW - 12);
            int inputY = this.topY + 50 + this.tabsOffsetY;
            this.codeInput = new EditBox(this.font, centerX - (inputW / 2), inputY, inputW, 20, Component.literal("Codigo da Sala"));
            this.codeInput.setMaxLength(16);
            this.codeInput.setValue(this.typedCodeDraft);
            this.addRenderableWidget(this.codeInput);

            int btnW = Math.min(160, this.innerW - 20);
            this.addRenderableWidget(Button.builder(Component.literal("Entrar na Sala"), btn -> {
                String typed = this.codeInput.getValue().trim().toUpperCase();
                if (typed.isEmpty()) {
                    this.feedbackMessage = "Digite o codigo da sala!";
                    return;
                }
                ClientPacketDistributor.sendToServer(new JoinRoomCodePayload(typed));
                this.onClose();
            }).bounds(centerX - (btnW / 2), inputY + 28, btnW, 20).build());
            return;
        }

        int codeRowY = this.topY + 22 + this.tabsOffsetY;
        int playersRowY = this.topY + 46 + this.tabsOffsetY;

        // Botao Copiar Codigo ancorado a direita
        int copyBtnW = this.compactMode ? 64 : 92;
        this.addRenderableWidget(Button.builder(
                Component.literal(this.compactMode ? "Copiar" : "Copiar Codigo"),
                btn -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.roomCode);
                    this.feedbackMessage = "Codigo copiado: " + this.roomCode;
                }
        ).bounds(this.innerRight - copyBtnW, codeRowY, copyBtnW, 18).build());

        // Seletor de quantidade de jogadores (1 a 5)
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            if (selectedPlayers > 1) {
                selectedPlayers--;
                this.init();
            }
        }).bounds(this.countControlsX, playersRowY, 20, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            if (selectedPlayers < MAX_SLOTS) {
                selectedPlayers++;
                this.init();
            }
        }).bounds(this.countControlsX + 52, playersRowY, 20, 18).build());

        // Linhas responsivas para cada jogador ativo
        for (int i = 0; i < selectedPlayers; i++) {
            final int slotIdx = i;
            int rowY = this.rowsStartY + (i * this.rowStep);

            String terrainLabel = (this.compactMode ? "" : "Terreno: ") + slotThemes[slotIdx].displayName;
            this.addRenderableWidget(Button.builder(
                    Component.literal(terrainLabel),
                    btn -> {
                        slotThemes[slotIdx] = slotThemes[slotIdx].next();
                        String updated = (this.compactMode ? "" : "Terreno: ") + slotThemes[slotIdx].displayName;
                        btn.setMessage(Component.literal(updated));
                    }
            ).bounds(this.terrainBtnX, rowY, this.terrainBtnW, this.rowH).build());

            String bannerLabel = (this.compactMode ? "" : "Cor: ") + slotBanners[slotIdx].displayName;
            this.addRenderableWidget(Button.builder(
                    Component.literal(bannerLabel),
                    btn -> {
                        slotBanners[slotIdx] = slotBanners[slotIdx].next();
                        String updated = (this.compactMode ? "" : "Cor: ") + slotBanners[slotIdx].displayName;
                        btn.setMessage(Component.literal(updated));
                    }
            ).bounds(this.bannerBtnX, rowY, this.bannerBtnW, this.rowH).build());
        }

        // Botao Iniciar Partida centralizado e proporcional
        int startBtnW = Math.min(210, this.innerW);
        int startBtnY = this.rowsStartY + (selectedPlayers * this.rowStep) + 6;
        this.addRenderableWidget(Button.builder(Component.literal("Iniciar Jogo e Gerar Reinos"), btn -> {
            int[] themes = new int[selectedPlayers];
            int[] banners = new int[selectedPlayers];
            for (int i = 0; i < selectedPlayers; i++) {
                themes[i] = slotThemes[i].ordinal();
                banners[i] = slotBanners[i].ordinal();
            }
            ClientPacketDistributor.sendToServer(new ConfirmRoomSetupPayload(selectedPlayers, themes, banners));
            this.onClose();
        }).bounds(centerX - (startBtnW / 2), startBtnY, startBtnW, 20).build());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        this.computeLayout();
        int centerX = this.width / 2;

        // Fundo escurecido + painel central responsivo
        gui.fill(0, 0, this.width, this.height, 0xCC0D0D12);
        gui.fill(this.left, this.topY, this.left + this.panelW, this.topY + this.panelH, 0xEE1B1E26);
        gui.outline(this.left, this.topY, this.left + this.panelW, this.topY + this.panelH, 0xFFD4AF37);

        if (this.joinTabActive) {
            String title = this.compactMode ? "OneKingdown - Entrar" : "OneKingdown - Entrar na Sala";
            gui.centeredText(this.font, title, centerX, this.topY + 8, 0xFFFFD700);
            String prompt = this.compactMode ? "Digite o codigo da sala:" : "Digite o codigo fornecido pelo dono da sala:";
            gui.centeredText(this.font, prompt, centerX, this.topY + 32 + this.tabsOffsetY, 0xFFE0E0E0);
        } else {
            String title = this.compactMode ? "OneKingdown - Sala" : "OneKingdown - Configuracao do Mundo";
            gui.centeredText(this.font, title, centerX, this.topY + 8, 0xFFFFD700);

            String codeText = (this.compactMode ? "Codigo: " : "Codigo da Sala: ") + this.roomCode;
            gui.text(this.font, codeText, this.innerLeft, this.topY + 27 + this.tabsOffsetY, 0xFF55FF55);

            String playersLabel = this.compactMode ? "Jogadores (Max 5):" : "Jogadores na Sala (Max 5):";
            gui.text(this.font, playersLabel, this.innerLeft, this.topY + 51 + this.tabsOffsetY, 0xFFFFFFFF);
            gui.centeredText(this.font, String.valueOf(selectedPlayers), this.countControlsX + 36, this.topY + 51 + this.tabsOffsetY, 0xFFFFD700);

            for (int i = 0; i < selectedPlayers; i++) {
                int rowY = this.rowsStartY + (i * this.rowStep);
                int textY = rowY + Math.max(1, (this.rowH - 8) / 2);
                String playerTag = (this.compactMode ? "P" : "Player ") + (i + 1);
                gui.text(this.font, playerTag, this.innerLeft, textY, 0xFFFFFFFF);

                int swatchTop = rowY + 2;
                int swatchBottom = rowY + this.rowH - 2;
                gui.fill(this.swatchX, swatchTop, this.swatchX + this.swatchW, swatchBottom, slotBanners[i].argbColor);
                gui.outline(this.swatchX, swatchTop, this.swatchX + this.swatchW, swatchBottom, 0xFFFFFFFF);
            }
        }

        if (!feedbackMessage.isEmpty()) {
            gui.centeredText(this.font, feedbackMessage, centerX, this.topY + this.panelH - 12, 0xFF55FF55);
        }

        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }
}