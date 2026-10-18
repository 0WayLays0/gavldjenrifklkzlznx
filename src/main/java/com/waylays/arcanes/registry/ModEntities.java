package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ArcanesOublies.MODID);

    public static final RegistryObject<EntityType<GoldBoltProjectile>> GOLD_BOLT = ENTITIES.register("gold_bolt",
            () -> EntityType.Builder.<GoldBoltProjectile>of(GoldBoltProjectile::new, MobCategory.MISC)
                    .sized(.5f, .5f)
                    .clientTrackingRange(64)
                    .build(ArcanesOublies.id("gold_bolt").toString()));

    public static final RegistryObject<EntityType<GildedChainsEntity>> GILDED_CHAINS = ENTITIES.register("gilded_chains",
            () -> EntityType.Builder.<GildedChainsEntity>of(GildedChainsEntity::new, MobCategory.MISC)
                    .sized(1f, .5f)
                    .clientTrackingRange(64)
                    .build(ArcanesOublies.id("gilded_chains").toString()));

    private ModEntities() {}
}
