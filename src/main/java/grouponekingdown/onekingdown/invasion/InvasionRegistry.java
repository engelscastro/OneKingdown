package grouponekingdown.onekingdown.invasion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class InvasionRegistry {
    private static final Map<String, InvasionDefinition> INVASIONS = new LinkedHashMap<>();

    public static final InvasionDefinition INFERNAL_CITADEL = register(new InvasionDefinition(
            "infernal_citadel",
            "Invasão da Cidadela Infernal",
            Identifier.fromNamespaceAndPath("onekingdown", "infernal_blackstone_citadel"),
            5, // Dispara automaticamente no Tier 5 do castelo
            3, // 3 ondas
            6, // 6 inimigos na onda 1 (escala nas próximas)
            BossEvent.BossBarColor.RED,
            List.of(
                    // TODO: Substitua pelos seus EntityTypes de demônios registrados
                    vanillaMob("skeleton"),
                    vanillaMob("blaze")
            )
    ));

    private InvasionRegistry() {}

    @SuppressWarnings("unchecked")
    private static Supplier<? extends EntityType<? extends Mob>> vanillaMob(String id) {
        return () -> (EntityType<? extends Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", id)
        );
    }

    public static InvasionDefinition register(InvasionDefinition definition) {
        INVASIONS.put(definition.id(), definition);
        return definition;
    }

    public static Optional<InvasionDefinition> get(String id) {
        return Optional.ofNullable(INVASIONS.get(id));
    }

    public static Optional<InvasionDefinition> getByCastleTier(int tier) {
        return INVASIONS.values().stream()
                .filter(def -> def.triggerCastleTier() == tier)
                .findFirst();
    }

    public static Collection<InvasionDefinition> all() {
        return INVASIONS.values();
    }
}