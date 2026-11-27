package grouponekingdown.onekingdown.invasion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InvasionManager {
    private static InvasionDefinition activeInvasion;
    private static BlockPos citadelPos;
    private static int currentWave = 0;
    private static final Set<UUID> aliveEnemies = new HashSet<>();
    private static ServerBossEvent bossBar;

    private InvasionManager() {}

    /**
     * Chame este método no código onde o seu castelo sobe de tier.
     */
    public static void onCastleTierReached(ServerLevel level, BlockPos castlePos, int newTier) {
        if (isRunning()) return;
        InvasionRegistry.getByCastleTier(newTier)
                .ifPresent(def -> startInvasion(level, castlePos, def));
    }

    public static boolean startInvasion(ServerLevel level, BlockPos originPos, InvasionDefinition definition) {
        if (isRunning()) {
            return false;
        }

        BlockPos spawnPos = findSafeCitadelPos(level, originPos, 48);
        if (!placeStructure(level, spawnPos, definition)) {
            return false;
        }

        activeInvasion = definition;
        citadelPos = spawnPos;
        currentWave = 0;
        aliveEnemies.clear();

        bossBar = new ServerBossEvent(
                UUID.randomUUID(),
                Component.literal(definition.displayName()),
                definition.barColor(),
                BossEvent.BossBarOverlay.PROGRESS
        );
        for (ServerPlayer player : level.players()) {
            bossBar.addPlayer(player);
            player.sendSystemMessage(Component.literal(
                    "§c[INVASÃO] " + definition.displayName() + " surgiu em X:" + spawnPos.getX() + " Z:" + spawnPos.getZ() + "!"
            ));
        }

        spawnNextWave(level);
        return true;
    }

    private static BlockPos findSafeCitadelPos(ServerLevel level, BlockPos origin, int distance) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        int x = origin.getX() + (int) (Math.cos(angle) * distance);
        int z = origin.getZ() + (int) (Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static boolean placeStructure(ServerLevel level, BlockPos pos, InvasionDefinition definition) {
        Optional<StructureTemplate> templateOpt = level.getStructureTemplateManager().get(definition.structureId());
        if (templateOpt.isEmpty()) {
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);

        templateOpt.get().placeInWorld(level, pos, pos, settings, level.getRandom(), 2);
        return true;
    }

    private static void spawnNextWave(ServerLevel level) {
        currentWave++;
        aliveEnemies.clear();

        int count = activeInvasion.enemiesPerWaveBase() + (currentWave - 1) * 3;
        RandomSource random = level.getRandom();
        var pool = activeInvasion.enemyPool();

        for (int i = 0; i < count; i++) {
            EntityType<? extends Mob> type = pool.get(random.nextInt(pool.size())).get();
            int offsetX = random.nextInt(17) - 8;
            int offsetZ = random.nextInt(17) - 8;
            int x = citadelPos.getX() + offsetX;
            int z = citadelPos.getZ() + offsetZ;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            Mob mob = type.create(level, EntitySpawnReason.EVENT);
            if (mob != null) {
                mob.setPos(x + 0.5, y + 1.0, z + 0.5);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
                aliveEnemies.add(mob.getUUID());
            }
        }

        updateBossBar();
    }

    public static void tick(ServerLevel level) {
        if (!isRunning()) return;

        // Mantém jogadores sincronizados na barra
        for (ServerPlayer player : level.players()) {
            bossBar.addPlayer(player);
        }

        // Remove inimigos mortos ou removidos
        Iterator<UUID> it = aliveEnemies.iterator();
        while (it.hasNext()) {
            Entity entity = level.getEntity(it.next());
            if (entity == null || !entity.isAlive()) {
                it.remove();
            }
        }

        updateBossBar();

        if (aliveEnemies.isEmpty()) {
            if (currentWave < activeInvasion.totalWaves()) {
                spawnNextWave(level);
            } else {
                finishInvasion(level, "§a[INVASÃO] " + activeInvasion.displayName() + " foi derrotada!");
            }
        }
    }

    private static void updateBossBar() {
        if (bossBar == null || activeInvasion == null) return;
        int totalForWave = activeInvasion.enemiesPerWaveBase() + (currentWave - 1) * 3;
        float progress = Math.max(0f, Math.min(1f, (float) aliveEnemies.size() / Math.max(1, totalForWave)));
        bossBar.setProgress(progress);
        bossBar.setName(Component.literal(
                activeInvasion.displayName() + " - Onda " + currentWave + "/" + activeInvasion.totalWaves()
        ));
    }

    public static void stopInvasion(ServerLevel level) {
        if (!isRunning()) return;
        for (UUID uuid : aliveEnemies) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) entity.discard();
        }
        finishInvasion(level, "§e[INVASÃO] Invasão encerrada manualmente.");
    }

    private static void finishInvasion(ServerLevel level, String message) {
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(message));
        }
        activeInvasion = null;
        citadelPos = null;
        currentWave = 0;
        aliveEnemies.clear();
    }

    public static boolean isRunning() {
        return activeInvasion != null;
    }

    public static BlockPos getCitadelPos() {
        return citadelPos;
    }

    public static String getActiveInvasionName() {
        return activeInvasion != null ? activeInvasion.displayName() : "";
    }

    public static String getStatusText() {
        if (!isRunning()) return "Nenhuma invasão ativa.";
        return activeInvasion.displayName() + " | Onda " + currentWave + "/" + activeInvasion.totalWaves()
                + " | Inimigos vivos: " + aliveEnemies.size();
    }
}