# OneKingdown — Arquitetura, Estrutura e Manual do Desenvolvedor

> **Propósito deste documento:** Servir como fonte única de verdade (contexto completo) sobre o mod **OneKingdown**. Ao iniciar uma nova sessão de chat ou configurar o projeto em uma nova máquina, forneça este arquivo para que o assistente ou desenvolvedor conheça exatamente o estado atual do código, as versões exatas das APIs e o roadmap.

## 1. Visão Geral do Projeto

O **OneKingdown** é um mod de gerenciamento procedural de reinos para **Minecraft 26.3 (NeoForge 26.3.0.117)**. Ele transforma a experiência multiplayer/singleplayer em um sistema de construção, economia, recrutamento e defesa de reinos.

### Funcionalidades Já Implementadas

* **Sistema de Reinos e Salas:** Criação de reinos individuais (território de 1.000 blocos quadrados) com suporte planejado para até 5 jogadores simultâneos por sala dinâmica.
* **Progressão em Tiers (Tier 1 a Tier 5+):** Evolução de nível do reino e melhoria das estruturas.
* **Estruturas Principais e Líderes NPC:**
  * **Castelo:** Abriga o **Rei** (núcleo do reino e ponto de respawn).
  * **Quartel (Barracks):** Abriga o **Líder da Guarda** (recrutamento de Guerreiros e Arqueiros).
  * **Mina (Mine):** Abriga o **Mestre da Mina** (recrutamento de Mineiros e produção de minérios).
  * **Fazenda (Farm):** Abriga o **Mestre da Fazenda** (recrutamento de Fazendeiros e produção de alimentos).
* **GUI de Contratação com Níveis (1 a 5):** Tela reutilizável (`RecruiterScreen`) com textura própria de pergaminho e espadas (`recruiter_gui.png`, 220×220). Permite escolher o tipo de unidade (incluindo abas de Guerreiro/Arqueiro no Quartel), exibe o saldo real do inventário (`Ferro`, `Mad`, `Pao`) e mostra os preços de cada nível (Nv. 1 ao 5) com feedback dinâmico de cor (**verde** quando disponível ou em Criativo; **vermelho** quando faltam recursos).
* **Entidade Customizada de Guarda (`KingdomGuardEntity`):** Entidade humanoide própria (`onekingdown:kingdom_guard`) que substituiu os Golems de Ferro. Suporta combate corpo a corpo (Guerreiro) e à distância com arco (Arqueiro), ataca apenas monstros (`Monster`) e renderiza armaduras e armas equipadas nativamente.
* **Sistema de Invasões PvE (`invasion/`):** Sistema modular de invasões por ondas com estrutura `.nbt` gerada e validada dinamicamente (`data/onekingdown/structure/infernal_blackstone_citadel.nbt`), barra de chefe (`ServerBossEvent`), disparo automático conectado ao `KingdomManager` ao atingir o Tier 5 do castelo, carregamento garantido de chunks e comandos administrativos (`/invasao start|stop|status`).
* **Economia e Ciclos de Produção:** Produção periódica de recursos nos baús/containers do território e contagem em tempo real de minérios, madeiras, comida, animais e monstros.
* **Sistema de NPCs:** Recrutamento, interação, sincronização de inventário no consumo de itens e proteção gerenciados via tags persistentes (`TAG_ROLE`, `TAG_OWNER`, `TAG_PROTECTED`, `TAG_LEVEL`), com separação independente de `ROLE_MINER_UNIT` e `ROLE_FARMER_UNIT` (incluindo migração automática do legado `ROLE_WORKER_UNIT`).
* **Respawn Customizado:** Ao morrer, o jogador é teleportado diretamente para o trono do seu Castelo.
* **HUD Reativa e Recolhível:** Painel discreto no canto superior direito sincronizado via rede (`CustomPacketPayload`), exibindo nível, tier, recursos, trabalhadores, tropas e ameaças. Recolhe e expande a qualquer momento com a tecla **`H`** (configurável nos controles do jogo).
* **Minimapa em Tempo Real (`KingdomMinimapOverlay`):** Minimapa leve e discreto no canto superior esquerdo (sem pausar ou fechar o jogo), exibindo terreno explorado em cache (`WorldMapCache`), coordenadas, indicador de Norte (`N`), marcadores do Reino (Castelo, Quartel, Mina, Fazenda), posição do jogador e alerta piscante de Invasão ativa (fixado na borda quando distante). Exibe/oculta a qualquer momento com a tecla **`M`**.

## 2. Stack Tecnológica e Requisitos de Ambiente

| Componente | Versão / Especificação | Observação Crítica |
| ----- | ----- | ----- |
| **Minecraft** | `26.3` (`26.3.0.117`) | API moderna com mudanças significativas em relação à 1.20/1.21 |
| **Mod Loader** | `NeoForge 26.3.0.117` | Utiliza `net.neoforged.devlaunch.Main` |
| **Java / JDK** | **Java 25 (Obrigatório)** | Bytecode `class file version 69.0`. **Não roda em Java 21 (`65.0`)** |
| **Build Tool** | `Gradle 8.14.3` | Wrapper incluído no repositório |
| **IDE** | `IntelliJ IDEA 2025.2` | Windows |
| **Repositório Git** | `https://github.com/engelscastro/OneKingdown` | Branch principal: `main` |

## 3. Peculiaridades da API (Minecraft 26.3 / NeoForge 26.3.0.117)

**ATENÇÃO PARA NOVOS CHATS / IA:** O Minecraft 26.3 alterou diversas classes e assinaturas em comparação com versões antigas (1.20.x / 1.21.x). Sempre respeite as assinaturas abaixo, já validadas diretamente no `.jar` do projeto:

1. **Identificadores (`Identifier`):**
   * Usa `net.minecraft.resources.Identifier.fromNamespaceAndPath(Onekingdown.MODID, "...")` em vez de `ResourceLocation`.
2. **Abertura de Telas (`Minecraft`):**
   * O método `setScreen(...)` foi substituído por **`Minecraft.getInstance().setScreenAndShow(Screen)`**.
   * Campos como `mc.screen` e `mc.options.hideGui` não são acessíveis diretamente como variáveis públicas (`RenderGuiEvent.Post` já respeita o F1 automaticamente).
3. **Renderização de HUD e Telas (`GuiGraphicsExtractor`):**
   * `RenderGuiEvent.Post` e `Screen.extractRenderState(...)` utilizam `GuiGraphicsExtractor`, e não `GuiGraphics`.
   * Métodos confirmados:
     * `gui.pose().pushMatrix()` / `gui.pose().translate(x, y)` / `gui.pose().scale(scale, scale)` / `gui.pose().popMatrix()`
     * `gui.fill(x1, y1, x2, y2, color)` / `gui.outline(x1, y1, x2, y2, color)` / `gui.horizontalLine(x1, x2, y, color)`
     * `gui.text(font, String, x, y, color)` / `gui.centeredText(font, Component/String, centerX, y, color)`
     * `gui.blit(Identifier, x1, y1, x2, y2, u0, u1, v0, v1)` (para desenhar texturas `.png` escaladas no retângulo destino).
4. **Entidades, Renderers e Armaduras:**
   * `EntityType.Builder.build(...)` exige `ResourceKey<EntityType<?>>`.
   * `HumanoidMobRenderer` utiliza `HumanoidRenderState` e `HumanoidModel<HumanoidRenderState>`.
   * `ModelLayers.PLAYER_INNER_ARMOR` / `PLAYER_OUTER_ARMOR` foram substituídos por **`ModelLayers.PLAYER_ARMOR`** com **`ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new)`** no `HumanoidArmorLayer`.
   * `AbstractArrow` foi movida de pacote (`net.minecraft.world.entity.projectile.arrow.AbstractArrow`); ao usar `ProjectileUtil.getMobArrow(...)`, pode-se usar `var`.
   * Constantes como `EntityType.SKELETON` mudaram ou não existem com o mesmo nome; para mobs vanilla, prefira buscar via `BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", id))`.
5. **Invasões, Comandos e Estruturas:**
   * Permissões de comando usam `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`.
   * `ServerBossEvent` exige `UUID` no construtor: `new ServerBossEvent(UUID.randomUUID(), Component, BossBarColor, BossBarOverlay)`.
   * Carregamento de templates `.nbt` usa `level.getStructureTemplateManager().get(Identifier)` e **exige** que o arquivo esteja em `src/main/resources/data/<modid>/structure/<nome>.nbt` (pasta `structure` no singular).
6. **Teclas de Atalho (`KeyMapping`):**
   * Construtor validado: `new KeyMapping(String name, int keyCode, KeyMapping.Category category)`.
   * Códigos de tecla devem vir de `com.mojang.blaze3d.platform.InputConstants` (ex.: `InputConstants.KEY_H`, `InputConstants.KEY_M`). **Não importe `org.lwjgl.glfw.GLFW`**.
   * Registro: `RegisterKeyMappingsEvent` no `modEventBus`; leitura por tick em `ClientTickEvent.Post` usando `while (KEY.consumeClick())`.
7. **Envio de Pacotes (Rede):**
   * Servidor → Cliente: `PacketDistributor.sendToPlayer(player, payload)`.
   * Cliente → Servidor: `ClientPacketDistributor.sendToServer(payload)`.

### Como validar assinaturas desconhecidas no 26.3

Sempre que precisar conferir um método novo antes de codificar, execute no terminal PowerShell do IntelliJ (`Alt + F12`):

```powershell
$javap = Get-ChildItem "$env:USERPROFILE\.jdks","$env:USERPROFILE\.gradle","C:\Program Files" -Filter "javap.exe" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match "25" } | Select-Object -First 1 -ExpandProperty FullName
$mc = "build\moddev\artifacts\minecraft-patched-26.3.0.117.jar"
$nf = Get-ChildItem "$env:USERPROFILE\.gradle\caches" -Filter "neoforge-26.3.0.117-universal.jar" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
& $javap -cp "$mc;$nf" "pacote.completo.NomeDaClasse"
```

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
        │   ├── Onekingdown.java                      # Entrypoint do mod e registro de eventos
        │   ├── Config.java                           # Configurações do mod
        │   ├── client/
        │   │   ├── gui/
        │   │   │   ├── KingdomMinimapOverlay.java    # Minimapa em tempo real e KeyMapping [M]
        │   │   │   └── RecruiterScreen.java          # GUI padrão de contratação (Nv. 1 a 5 + preços)
        │   │   ├── map/
        │   │   │   └── WorldMapCache.java            # Cache leve de terreno (16x16 px por chunk)
        │   │   └── renderer/
        │   │       └── KingdomGuardRenderer.java     # Renderer humanoide com ArmorModelSet
        │   ├── network/
        │   │   ├── OpenRecruiterPayload.java         # Pacote Server -> Client (abre RecruiterScreen)
        │   │   ├── HireUnitPayload.java              # Pacote Client -> Server (solicita contratação)
        │   │   ├── RequestMapPayload.java            # Pacote Client -> Server (solicita dados do mapa)
        │   │   └── OpenMapPayload.java               # Pacote Server -> Client (atualiza marcadores do minimapa)
        │   ├── kingdom/
        │   │   ├── KingdomHudOverlay.java            # HUD recolhível e KeyMapping [H]
        │   │   ├── KingdomHudPayload.java            # Pacote Server -> Client (HUD) + registro de payloads
        │   │   ├── KingdomData.java                  # Modelo de dados e persistência dos reinos
        │   │   ├── KingdomManager.java               # Regras de negócio, geração, upgrades, produção, HUD e gatilho de invasão
        │   │   ├── KingdomEvents.java                # Listeners de eventos de servidor, comandos e proteção
        │   │   └── KingdomNpcManager.java            # Spawn, tags (MINER_UNIT/FARMER_UNIT), contratação com níveis e equipamentos
        │   ├── npc/
        │   │   ├── KingdomGuardEntity.java           # Entidade humanoide de guarda (Guerreiro/Arqueiro)
        │   │   └── ModEntities.java                  # DeferredRegister e atributos das entidades do mod
        │   ├── invasion/
        │   │   ├── InvasionDefinition.java           # Record com definição de cada tipo de invasão
        │   │   ├── InvasionRegistry.java             # Registro de invasões (ex.: INFERNAL_CITADEL)
        │   │   ├── InvasionManager.java              # Controle de ondas, estrutura .nbt, BossBar e getters de mapa
        │   │   └── InvasionCommand.java              # Comando /invasao <start|stop|status>
        │   └── structure/
        │       └── CastleStructureBuilder.java       # Construção procedural do castelo e prédios
        └── resources/
            ├── META-INF/
            │   └── neoforge.mods.toml
            ├── data/onekingdown/structure/
            │   └── infernal_blackstone_citadel.nbt   # Estrutura da Cidadela Infernal (validada)
            └── assets/onekingdown/textures/gui/
                └── recruiter_gui.png                 # Textura de fundo da GUI (pergaminho com espadas)
```

## 5. Arquitetura Detalhada

### 5.1. Entrypoint (`Onekingdown.java`)

* **`modEventBus`:** Registra `commonSetup`, `KingdomHudPayload::registerPayloads`, `ModEntities::registerAttributes`, `BLOCKS`, `ITEMS`, `CREATIVE_MODE_TABS`, `ModEntities.ENTITY_TYPES` e `addCreative`.
* **`NeoForge.EVENT_BUS` (Servidor/Comum):** Registra todos os handlers de `KingdomEvents`, além de `InvasionCommand.register` (em `RegisterCommandsEvent`) e `InvasionManager.tick` (em `ServerTickEvent.Post`).
* **`ClientModEvents` (`@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)`):**
  * `onRegisterKeys(RegisterKeyMappingsEvent)` → registra `KingdomHudOverlay.TOGGLE_HUD_KEY` (`H`) e `KingdomMinimapOverlay.TOGGLE_MAP_KEY` (`M`).
  * `onRegisterRenderers(EntityRenderersEvent.RegisterRenderers)` → registra `KingdomGuardRenderer` para `ModEntities.KINGDOM_GUARD`.
  * `onClientSetup(FMLClientSetupEvent)` → registra `KingdomHudOverlay::onRenderGui`, `KingdomHudOverlay::onClientTick`, `KingdomMinimapOverlay::onRenderGui`, `KingdomMinimapOverlay::onClientTick` e `WorldMapCache::onClientTick` no `NeoForge.EVENT_BUS`.

### 5.2. Fluxo de Sincronização da HUD (Server → Client)

1. **Coleta no Servidor (`KingdomManager.sendHudSnapshot`):** Varre containers e entidades dentro dos limites do reino e envia `KingdomHudPayload` via `PacketDistributor.sendToPlayer`.
2. **Transporte (`KingdomHudPayload`):** ID `onekingdown:kingdom_hud`, armazena snapshot em `clientState` e centraliza o registro de todos os payloads do mod (`registerPayloads`).
3. **Renderização (`KingdomHudOverlay`):** Canto superior direito (`SCALE = 0.70f`), alternado pela tecla **`H`**.

### 5.3. Sistema de Contratação via GUI, Níveis (1 a 5) e Guarda Humanoide

1. **Abertura (Server → Client):** Clique direito no `ROLE_GUARD_LEADER`, `ROLE_MINE_MASTER` ou `ROLE_FARM_MASTER` envia `OpenRecruiterPayload` com o saldo de `IRON_INGOT`, `OAK_LOG` e `BREAD` do jogador e abre `RecruiterScreen` via `setScreenAndShow`.
2. **Seleção e Envio (Client → Server):** `RecruiterScreen` exibe cada nível com preço formatado e colorido dinamicamente (verde/vermelho) e envia `HireUnitPayload(recruiterRole, unitType, level)` via `ClientPacketDistributor.sendToServer`.
3. **Validação e Spawn Equipado (Server):** `KingdomNpcManager.hireUnitWithLevel` desconta o custo (`level * 15`), sincroniza o inventário (`setChanged()` + `broadcastChanges()`), spawna `KingdomGuardEntity` (`ROLE_GUARD_UNIT`) ou `villager` (`ROLE_MINER_UNIT` / `ROLE_FARMER_UNIT`) com `TAG_LEVEL` e equipa arma/ferramenta e armadura:

| Nível | Guerreiro (Guarda) | Arqueiro (Guarda) | Mineiro (Mina) | Fazendeiro (Fazenda) | Custo |
| ----- | ----- | ----- | ----- | ----- | ----- |
| **Nv. 1** | Espada de Madeira + Couro | Arco + Couro | Picareta de Madeira | Enxada de Madeira | 15 |
| **Nv. 2** | Espada de Pedra + Malha | Arco + Malha | Picareta de Pedra | Enxada de Pedra | 30 |
| **Nv. 3** | Espada de Ferro + Ferro | Arco + Ferro | Picareta de Ferro | Enxada de Ferro | 45 |
| **Nv. 4** | Espada de Diamante + Diamante | Arco + Diamante | Picareta de Diamante | Enxada de Diamante | 60 |
| **Nv. 5** | Espada de Netherita + Netherita | Arco + Netherita | Picareta de Netherita | Enxada de Netherita | 75 |

### 5.4. Sistema de Invasões PvE (`invasion/`)

* **`InvasionDefinition` & `InvasionRegistry`:** Define cada invasão com ID, nome, estrutura `.nbt`, tier de gatilho, número de ondas, inimigos base por onda, cor da `BossBar` e lista de mobs (`enemyPool`). A primeira invasão registrada é `INFERNAL_CITADEL` (`onekingdown:infernal_blackstone_citadel`, Tier 5, 3 ondas, atualmente usando `skeleton` e `blaze` como placeholders até a criação dos demônios customizados).
* **`InvasionManager`:**
  * `onCastleTierReached(level, castlePos, newTier)`: Chamado por `KingdomManager.addXpAndCheckUpgrade` quando o castelo evolui de tier.
  * `startInvasion(...)`: Carrega o chunk a 48 blocos, posiciona o `.nbt` da cidadela (reportando erro explícito no chat/log caso não encontre), cria o `ServerBossEvent` e inicia a Onda 1.
  * `tick(level)`: Atualiza jogadores na barra, monitora `aliveEnemies` e avança as ondas até a derrota total.
  * `getCitadelPos()` / `getActiveInvasionName()`: Expõem a localização e o nome da invasão ativa para o minimapa.
* **`InvasionCommand`:** Comandos `/invasao start <id>`, `/invasao stop` e `/invasao status`.

### 5.5. Minimapa em Tempo Real (`KingdomMinimapOverlay` & `WorldMapCache`)

1. **Cache de Terreno (`WorldMapCache`):** Escaneia gradualmente até 4 chunks por ciclo (a cada 20 ticks, raio de 3 chunks ao redor do jogador) armazenando cores de mapa com sombreamento de relevo em um `ConcurrentHashMap<Long, int[]>`. Limpa automaticamente ao trocar de dimensão.
2. **Sincronização de Marcadores (`RequestMapPayload` → `OpenMapPayload`):** A cada 5 segundos (ou ao reexibir o mapa com `M`), o cliente solicita os marcadores ao servidor, que retorna coordenadas do Castelo, Quartel, Mina, Fazenda e da Cidadela de Invasão (se ativa).
3. **Renderização Otimizada (`KingdomMinimapOverlay`):** Desenha um quadrado discreto de 84×84 px no canto superior esquerdo (alcance de 84×84 blocos), agrupando pixels horizontais da mesma cor para minimizar chamadas `gui.fill` e manter o FPS alto.

## 6. Manual do Desenvolvedor

### 6.1. Configurando o Projeto em uma Nova Máquina

Ao clonar o repositório em outro PC, o erro mais comum é o `UnsupportedClassVersionError: class file version 69.0, this version only recognizes up to 65.0`. Siga estes 3 passos:

1. **Project SDK:** `Ctrl + Alt + Shift + S` → **Project** → selecione **JDK 25** → Language level: **SDK default**.
2. **Gradle JVM:** `Ctrl + Alt + S` → **Build, Execution, Deployment > Build Tools > Gradle** → **Gradle JVM**: selecione **JDK 25**.
3. **Run Configuration:** Confirme que o JRE selecionado é o **JDK 25** e recarregue o Gradle (`Ctrl + Shift + O`).

### 6.2. Desativar o Tutorial Nativo do Minecraft no Ambiente de Testes

1. Com o jogo **fechado**, abra `run/options.txt`.
2. Altere `tutorialStep:...` para `tutorialStep:none` e salve.

### 6.3. Fluxo de Trabalho com Git (Multi-PC)

```powershell
# Baixar atualizações antes de codar
git pull origin main

# Enviar alterações após testar
git add .
git commit -m "feat: descricao clara da alteracao"
git push origin main
```

## 7. Roadmap e Próximos Passos

1. **Demônios Customizados e Regras da Invasão:**
   * Registrar entidades customizadas de demônios no `ModEntities` e substituir os placeholders (`skeleton` / `blaze`) no `InvasionRegistry`.
   * Garantir que o disparo de invasão cubra pulos de múltiplos tiers de uma vez (`getByTierRange`).
   * Recompensa de XP e loot ao derrotar todas as ondas da invasão; condição de derrota caso o Rei seja morto.
2. **Balanceamento por Nível e Proteção de NPCs:**
   * Escalar a produção dos Mineiros e Fazendeiros conforme o `TAG_LEVEL` de cada trabalhador.
   * Reforçar proteção/imunidade dos trabalhadores (`TAG_PROTECTED`) contra dano letal de monstros noturnos e sincronizar a contagem de guardas caso algum morra em combate.
   * Adicionar skin customizada em `assets/onekingdown/textures/entity/kingdom_guard.png` para o `KingdomGuardRenderer`.
3. **Evolução Visual de Estruturas (Schematics NBT):**
   * Carregamento de arquivos `.nbt` de castelos que evoluem automaticamente a cada 10 níveis.
4. **Invasões PvP e Salas Dinâmicas (até 5 jogadores):**
   * Mecânica de ataque entre reinos vizinhos na mesma sala.
5. **Polimento e Localização:**
   * Criar `src/main/resources/assets/onekingdown/lang/pt_br.json` para traduzir `key.onekingdown.toggle_hud` (`"Recolher/Expandir HUD do Reino"`) e `key.onekingdown.open_map` (`"Mostrar/Ocultar Minimapa"`).