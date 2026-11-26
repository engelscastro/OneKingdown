# OneKingdown — Arquitetura, Estrutura e Manual do Desenvolvedor

> **Propósito deste documento:** Servir como fonte única de verdade (contexto completo) sobre o mod **OneKingdown**. Ao iniciar uma nova sessão de chat ou configurar o projeto em uma nova máquina, forneça este arquivo para que o assistente ou desenvolvedor conheça exatamente o estado atual do código, as versões exatas das APIs e o roadmap.

---

## 1. Visão Geral do Projeto

O **OneKingdown** é um mod de gerenciamento procedural de reinos para **Minecraft 26.3 (NeoForge 26.3.0.117)**. Ele transforma a experiência multiplayer/singleplayer em um sistema de construção, economia, recrutamento e defesa de reinos.

### Funcionalidades Já Implementadas
- **Sistema de Reinos e Salas:** Criação de reinos individuais (território de 1.000 blocos quadrados) com suporte planejado para até 5 jogadores simultâneos por sala dinâmica.
- **Progressão em 4 Tiers (Tier 1 a Tier 4):** Evolução de nível do reino e melhoria das estruturas.
- **Estruturas Principais e Líderes NPC:**
  - **Castelo:** Abriga o **Rei** (núcleo do reino e ponto de respawn).
  - **Quartel (Barracks):** Abriga o **Líder dos Guardas** (recrutamento de Guardas).
  - **Mina (Mine):** Abriga o **Mestre da Mina** (recrutamento de Mineiros e produção de minérios).
  - **Fazenda (Farm):** Abriga o **Mestre da Fazenda** (recrutamento de Fazendeiros e produção de alimentos).
- **Economia e Ciclos de Produção:** Produção periódica de recursos nos baús/containers do território e contagem em tempo real de minérios, madeiras, comida, animais e monstros.
- **Sistema de NPCs:** Recrutamento, interação e proteção gerenciados via tags persistentes (`TAG_ROLE`, `TAG_OWNER`, `TAG_PROTECTED`).
- **Respawn Customizado:** Ao morrer, o jogador é teleportado diretamente para o trono do seu Castelo.
- **HUD Reativa e Recolhível:** Painel discreto no canto superior direito sincronizado via rede (`CustomPacketPayload`), exibindo nível, tier, recursos, trabalhadores, tropas e ameaças. Recolhe e expande a qualquer momento com a tecla **`H`** (configurável nos controles do jogo).

---

## 2. Stack Tecnológica e Requisitos de Ambiente

| Componente | Versão / Especificação | Observação Crítica |
| :--- | :--- | :--- |
| **Minecraft** | `26.3` (`26.3.0.117`) | API moderna com mudanças significativas em relação à 1.20/1.21 |
| **Mod Loader** | `NeoForge 26.3.0.117` | Utiliza `net.neoforged.devlaunch.Main` |
| **Java / JDK** | **Java 25 (Obrigatório)** | Bytecode `class file version 69.0`. **Não roda em Java 21 (`65.0`)** |
| **Build Tool** | `Gradle 8.14.3` | Wrapper incluído no repositório |
| **IDE** | `IntelliJ IDEA 2025.2` | Windows |
| **Repositório Git** | `https://github.com/engelscastro/OneKingdown` | Branch principal: `main` |

---

## 3. Peculiaridades da API (Minecraft 26.3 / NeoForge 26.3.0.117)

**ATENÇÃO PARA NOVOS CHATS / IA:** O Minecraft 26.3 alterou diversas classes e assinaturas em comparação com versões antigas (1.20.x / 1.21.x). Sempre respeite as assinaturas abaixo, já validadas diretamente no `.jar` do projeto:

1. **Identificadores (`Identifier`):**
   - Usa `net.minecraft.resources.Identifier.fromNamespaceAndPath(Onekingdown.MODID, "...")` em vez de `ResourceLocation`.
2. **Renderização de HUD (`GuiGraphicsExtractor`):**
   - O evento `RenderGuiEvent.Post` retorna `GuiGraphicsExtractor` (via `event.getGuiGraphics()`), e não `GuiGraphics`.
   - Métodos confirmados:
     - `gui.pose().pushMatrix()` / `gui.pose().translate(x, y)` / `gui.pose().scale(scale, scale)` / `gui.pose().popMatrix()`
     - `gui.fill(x1, y1, x2, y2, color)`
     - `gui.outline(x1, y1, x2, y2, color)`
     - `gui.horizontalLine(x1, x2, y, color)`
     - `gui.text(font, String, x, y, color)`
3. **Teclas de Atalho (`KeyMapping`):**
   - Construtor validado: `new KeyMapping(String name, int keyCode, KeyMapping.Category category)`.
   - Categorias ficam em `KeyMapping.Category` (ex.: `KeyMapping.Category.MISC`).
   - Códigos de tecla devem vir de `com.mojang.blaze3d.platform.InputConstants` (ex.: `InputConstants.KEY_H`). **Não importe `org.lwjgl.glfw.GLFW`**, pois o pacote não fica exposto no classpath de compilação.
   - Registro: `RegisterKeyMappingsEvent` no `modEventBus` (via `@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)`).
   - Leitura por tick: `ClientTickEvent.Post` no `NeoForge.EVENT_BUS` usando `while (KEY.consumeClick())`.
4. **Encapsulamento da classe `Minecraft`:**
   - Campos como `mc.screen` não são mais acessíveis diretamente como variáveis públicas.

### Como validar assinaturas desconhecidas no 26.3
Sempre que precisar conferir um método novo antes de codificar, execute no terminal PowerShell do IntelliJ (`Alt + F12`):

```powershell
$javap = Get-ChildItem "$env:USERPROFILE\.jdks","$env:USERPROFILE\.gradle","C:\Program Files" -Filter "javap.exe" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match "25" } | Select-Object -First 1 -ExpandProperty FullName
$mc = "build\moddev\artifacts\minecraft-patched-26.3.0.117.jar"
$nf = Get-ChildItem "$env:USERPROFILE\.gradle\caches" -Filter "neoforge-26.3.0.117-universal.jar" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
& $javap -cp "$mc;$nf" "pacote.completo.NomeDaClasse"
```

---

## 4. Estrutura do Projeto

```text
OneKingdown/
├── .gitignore                  # Ignora .gradle, build, run, out/, .idea, *.iml
├── build.gradle                # Configuração NeoForge ModDev e dependências
├── settings.gradle
├── gradlew / gradlew.bat
├── DEV_GUIDE.md                # Este documento
└── src/
    └── main/
        ├── java/grouponekingdown/onekingdown/
        │   ├── Onekingdown.java                # Entrypoint do mod e registro de eventos
        │   ├── Config.java                     # Configurações do mod
        │   └── kingdom/
        │       ├── KingdomData.java            # Modelo de dados e persistência dos reinos
        │       ├── KingdomManager.java         # Regras de negócio, geração, upgrades, produção e snapshot HUD
        │       ├── KingdomEvents.java          # Listeners de eventos de servidor, comandos, respawn e proteção
        │       ├── KingdomHudPayload.java      # Pacote de rede (Server -> Client) com estado do reino
        │       └── KingdomHudOverlay.java      # Renderização da HUD recolhível e KeyMapping [H]
        └── resources/
            └── META-INF/
                └── neoforge.mods.toml
```

---

## 5. Arquitetura Detalhada

### 5.1. Entrypoint (`Onekingdown.java`)
- **`modEventBus`:** Registra `commonSetup`, `KingdomHudPayload::registerPayloads`, `BLOCKS`, `ITEMS`, `CREATIVE_MODE_TABS` e `addCreative`.
- **`NeoForge.EVENT_BUS` (Servidor/Comum):** Registra todos os handlers de `KingdomEvents`:
  - `onServerStarting`, `onServerStopping`, `onServerTick`
  - `onEntityInteract`, `onPlayerLoggedIn`, `onPlayerRespawn`
  - `onBlockBreak`, `onBlockPlace`, `onRegisterCommands`
- **`ClientModEvents` (`@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)`):**
  - `onRegisterKeys(RegisterKeyMappingsEvent)` → registra `KingdomHudOverlay.TOGGLE_HUD_KEY`.
  - `onClientSetup(FMLClientSetupEvent)` → registra `KingdomHudOverlay::onRenderGui` e `KingdomHudOverlay::onClientTick` no `NeoForge.EVENT_BUS`.

### 5.2. Fluxo de Sincronização da HUD (Server → Client)
1. **Coleta no Servidor (`KingdomManager.sendHudSnapshot`):**
   - Varre todos os `BlockEntity` que implementam `Container` dentro dos limites do reino (`getMinX..getMaxX`, `centerY - 2..centerY + 30`, `getMinZ..getMaxZ`) contabilizando `ores`, `wood` e `food`.
   - Varre entidades `Animal` e `Monster` dentro da `AABB` do território (`centerY - 10` até `centerY + 35`).
   - Lê `guardsCount`, `minersCount`, `farmersCount`, `level` e `currentTier` do objeto `KingdomData.Kingdom`.
   - Envia via `PacketDistributor.sendToPlayer(player, payload)`.
2. **Transporte (`KingdomHudPayload`):**
   - `record` implementando `CustomPacketPayload` com ID `onekingdown:kingdom_hud`.
   - Serializa 10 inteiros em ordem: `ores, wood, food, guards, miners, farmers, animals, monsters, level, tier`.
   - No cliente, armazena o último snapshot em `public static volatile KingdomHudPayload clientState`.
3. **Renderização (`KingdomHudOverlay`):**
   - Posicionado no canto superior direito com `SCALE = 0.70f`, `BOX_WIDTH = 178`, `EXPANDED_HEIGHT = 64`, `COLLAPSED_HEIGHT = 18`.
   - Exibe cabeçalho `Reino Nv.X (Tier Y) [H]`.
   - Quando expandido, divide em duas colunas:
     - **Esquerda:** `Minerios`, `Madeiras`, `Comida`, `Animais`.
     - **Direita:** `Guardas`, `Mineiros`, `Fazendeiros`, `Monstros` (vermelho se `> 0`, cinza se `0`).
   - Tecla **`H`** alterna a flag `collapsed`.

### 5.3. Sistema de NPCs e Proteção
- NPCs pertencentes ao reino recebem tags de identificação (`TAG_ROLE`, `TAG_OWNER`, `TAG_PROTECTED`).
- Interações com NPCs (`onEntityInteract`) disparam menus/ações de recrutamento e gestão conforme o papel (Rei, Líder dos Guardas, Mestre da Mina, Mestre da Fazenda).
- Quebra e colocação de blocos (`onBlockBreak`, `onBlockPlace`) respeitam as regras de território do reino.

---

## 6. Manual do Desenvolvedor

### 6.1. Configurando o Projeto em uma Nova Máquina
Ao clonar o repositório em outro PC, o erro mais comum é o `UnsupportedClassVersionError: class file version 69.0, this version only recognizes up to 65.0` (causado por o IntelliJ usar Java 21 por padrão). Siga estes 3 passos:

1. **Project SDK:** `Ctrl + Alt + Shift + S` → **Project** → selecione ou baixe **JDK 25** → Language level: **SDK default**.
2. **Gradle JVM:** `Ctrl + Alt + S` → **Build, Execution, Deployment > Build Tools > Gradle** → **Gradle JVM**: selecione **JDK 25**.
3. **Run Configuration:** Edite a configuração de execução (`runClient` / `Main`) e confirme que o JRE selecionado é o **JDK 25**. Recarregue o Gradle (`Ctrl + Shift + O`).

### 6.2. Fluxo de Trabalho com Git (Multi-PC)
Sempre execute os comandos na pasta raiz do projeto (`.../IdeaProjects/OneKingdown`):

- **Antes de começar a codar (baixar atualizações):**
  ```powershell
  git pull origin main
  ```
- **Após testar e validar alterações (enviar para o GitHub):**
  ```powershell
  git add .
  git commit -m "feat: descricao clara da alteracao"
  git push origin main
  ```
- **Descartar alterações locais e igualar exatamente ao GitHub:**
  ```powershell
  git fetch origin
  git reset --hard origin/main
  ```

---

## 7. Roadmap e Próximos Passos

Estes são os módulos planejados para a continuidade do desenvolvimento:

1. **Sistema de Invasões PvE (Próxima prioridade):**
   - Ondas periódicas de monstros que surgem nas bordas do território e marcham em direção ao **Rei** no Castelo.
   - Combate ativo dos **Guardas** recrutados defendendo o perímetro.
   - Recompensa de XP para o reino ao sobreviver e derrotar a onda; penalidade/derrota caso o Rei seja morto.
2. **Evolução Visual de Estruturas (Schematics NBT):**
   - Carregamento de arquivos `.nbt` de castelos que evoluem automaticamente a cada 10 níveis (começando de um pequeno forte).
3. **Invasões PvP e Salas Dinâmicas (até 5 jogadores):**
   - Mecânica de ataque entre reinos vizinhos na mesma sala.
4. **Polimento e Localização:**
   - Criar `src/main/resources/assets/onekingdown/lang/pt_br.json` para traduzir `key.onekingdown.toggle_hud` como `"Recolher/Expandir HUD do Reino"` no menu de Controles do Minecraft.