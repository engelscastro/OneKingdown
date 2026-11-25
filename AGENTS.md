# Regras do Projeto — OneKingdown

- **Jogo e Loader:** Minecraft 26.3 | NeoForge 26.3.0.117 | Java 25
- **Pacote base:** `grouponekingdown.onekingdown`
- **Mod ID:** `onekingdown`
- **Classe principal:** `src/main/java/grouponekingdown/onekingdown/Onekingdown.java`

## Padrões Obrigatórios da API (NeoForge 26.3)
- **Identificadores:** Use `net.minecraft.resources.Identifier` (ex.: `Identifier.fromNamespaceAndPath(MODID, "nome")` ou `Identifier.parse(...)`). A classe `ResourceLocation` não existe nesta versão.
- **Eventos:** Use `@EventBusSubscriber(modid = MODID)` sem o parâmetro `bus` (os barramentos foram unificados).
- **Registro de Blocos e Itens:** Sempre passe lambdas nas propriedades para injetar o ID automaticamente:
    - Blocos: `BLOCKS.registerSimpleBlock("nome", props -> props.mapColor(MapColor.STONE))`
    - Itens: `ITEMS.registerSimpleItem("nome", props -> props...)`
- **Assets e Modelos (1.21.4+ / 26.3):**
    - Definição do item no cliente: `src/main/resources/assets/onekingdown/items/<nome>.json`
    - Modelo 3D/2D: `src/main/resources/assets/onekingdown/models/item/<nome>.json`
    - Texturas: `src/main/resources/assets/onekingdown/textures/item/<nome>.png`
    - Tradução PT-BR: `src/main/resources/assets/onekingdown/lang/pt_br.json`

## Validação
- Após criar ou alterar qualquer arquivo, execute `.\gradlew.bat build` no terminal para garantir compilação limpa antes de concluir.