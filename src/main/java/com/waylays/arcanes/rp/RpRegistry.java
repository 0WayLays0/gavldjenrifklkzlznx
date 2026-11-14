package com.waylays.arcanes.rp;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.horror.WallWriting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What the role play and the game master's tools register: the words written on a wall, the heartbeat of fear, and
 * the chance effects the weapons' workshop puts on an item.
 */
public final class RpRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ArcanesOublies.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ArcanesOublies.MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ArcanesOublies.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WallWriting>> WRITING = ENTITIES.register("ecriture",
            () -> EntityType.Builder.<WallWriting>of(WallWriting::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8)
                    .updateInterval(20).noSummon().build("ecriture"));
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = SOUNDS.register("coeur_peur",
            () -> SoundEvent.createVariableRangeEvent(ArcanesOublies.id("coeur_peur")));

    private RpRegistry() {}

    public static void register(IEventBus bus) {
        com.waylays.arcanes.rp.arms.Arms.init();
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        COMPONENTS.register(bus);
    }
}
