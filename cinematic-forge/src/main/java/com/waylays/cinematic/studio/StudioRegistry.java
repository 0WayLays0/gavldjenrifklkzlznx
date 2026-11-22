package com.waylays.cinematic.studio;

import com.waylays.cinematic.CinematicMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class StudioRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CinematicMod.MOD_ID);
    public static final RegistryObject<EntityType<ActorEntity>> ACTOR = ENTITIES.register("actor",
            () -> EntityType.Builder.<ActorEntity>of(ActorEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).clientTrackingRange(16).updateInterval(1).build("actor"));

    private StudioRegistry() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(StudioRegistry::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ACTOR.get(), LivingEntity.createLivingAttributes().build());
    }
}
