package grouponekingdown.onekingdown.invasion;

import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.function.Supplier;

public record InvasionDefinition(
        String id,
        String displayName,
        Identifier structureId,
        int triggerCastleTier,
        int totalWaves,
        int enemiesPerWaveBase,
        BossEvent.BossBarColor barColor,
        List<Supplier<? extends EntityType<? extends Mob>>> enemyPool
) {}