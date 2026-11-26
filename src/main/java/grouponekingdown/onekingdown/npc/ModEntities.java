package grouponekingdown.onekingdown.npc;

import grouponekingdown.onekingdown.Onekingdown;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Onekingdown.MODID);

    public static final ResourceKey<EntityType<?>> KINGDOM_GUARD_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Onekingdown.MODID, "kingdom_guard")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<KingdomGuardEntity>> KINGDOM_GUARD =
            ENTITY_TYPES.register("kingdom_guard", () -> EntityType.Builder
                    .of(KingdomGuardEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(8)
                    .build(KINGDOM_GUARD_KEY));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(KINGDOM_GUARD.get(), KingdomGuardEntity.createAttributes().build());
    }
}