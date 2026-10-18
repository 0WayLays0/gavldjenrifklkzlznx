package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.BlastEffect;
import com.waylays.arcanes.entity.explosion.CataclysmCore;
import com.waylays.arcanes.entity.explosion.ChainDetonation;
import com.waylays.arcanes.entity.explosion.ClusterBomblet;
import com.waylays.arcanes.entity.explosion.ClusterShell;
import com.waylays.arcanes.entity.explosion.DetonationWave;
import com.waylays.arcanes.entity.explosion.ExplosiveOrb;
import com.waylays.arcanes.entity.explosion.ImplosionCore;
import com.waylays.arcanes.entity.explosion.RuinSigil;
import com.waylays.arcanes.entity.explosion.ShieldBubble;
import com.waylays.arcanes.entity.gold.AurumBladeEntity;
import com.waylays.arcanes.entity.gold.EldoradoSun;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import com.waylays.arcanes.entity.gold.GoldSpikeEntity;
import com.waylays.arcanes.entity.gold.GoldenSentinel;
import com.waylays.arcanes.entity.gold.GoldfallIngot;
import com.waylays.arcanes.entity.gold.MoltenGoldBlob;
import com.waylays.arcanes.entity.gold.MoltenPool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ArcanesOublies.MODID);

    public static final RegistryObject<EntityType<GoldBoltProjectile>> GOLD_BOLT = register("gold_bolt",
            EntityType.Builder.<GoldBoltProjectile>of(GoldBoltProjectile::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64).updateInterval(1));

    public static final RegistryObject<EntityType<GildedChainsEntity>> GILDED_CHAINS = register("gilded_chains",
            EntityType.Builder.<GildedChainsEntity>of(GildedChainsEntity::new, MobCategory.MISC).sized(1f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<GoldfallIngot>> GOLDFALL_INGOT = register("goldfall_ingot",
            EntityType.Builder.<GoldfallIngot>of(GoldfallIngot::new, MobCategory.MISC).sized(.9f, .9f).clientTrackingRange(96).updateInterval(1));

    public static final RegistryObject<EntityType<GoldSpikeEntity>> GOLD_SPIKE = register("gold_spike",
            EntityType.Builder.<GoldSpikeEntity>of(GoldSpikeEntity::new, MobCategory.MISC).sized(1f, 1.8f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<AurumBladeEntity>> AURUM_BLADE = register("aurum_blade",
            EntityType.Builder.<AurumBladeEntity>of(AurumBladeEntity::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(96));

    public static final RegistryObject<EntityType<MoltenGoldBlob>> MOLTEN_GOLD_BLOB = register("molten_gold_blob",
            EntityType.Builder.<MoltenGoldBlob>of(MoltenGoldBlob::new, MobCategory.MISC).sized(.3f, .3f).clientTrackingRange(64).updateInterval(1));

    public static final RegistryObject<EntityType<MoltenPool>> MOLTEN_POOL = register("molten_pool",
            EntityType.Builder.<MoltenPool>of(MoltenPool::new, MobCategory.MISC).sized(2.4f, .2f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<GoldenSentinel>> GOLDEN_SENTINEL = register("golden_sentinel",
            EntityType.Builder.<GoldenSentinel>of(GoldenSentinel::new, MobCategory.MISC).sized(.6f, 1.95f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<EldoradoSun>> ELDORADO_SUN = register("eldorado_sun",
            EntityType.Builder.<EldoradoSun>of(EldoradoSun::new, MobCategory.MISC).sized(1.5f, 1.5f).clientTrackingRange(128).fireImmune());

    // Explosion school
    public static final RegistryObject<EntityType<BlastEffect>> BLAST_EFFECT = register("blast_effect",
            EntityType.Builder.<BlastEffect>of(BlastEffect::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(160).fireImmune());

    public static final RegistryObject<EntityType<ExplosiveOrb>> EXPLOSIVE_ORB = register("explosive_orb",
            EntityType.Builder.<ExplosiveOrb>of(ExplosiveOrb::new, MobCategory.MISC).sized(.6f, .6f).clientTrackingRange(64).updateInterval(1));

    public static final RegistryObject<EntityType<ChainDetonation>> CHAIN_DETONATION = register("chain_detonation",
            EntityType.Builder.<ChainDetonation>of(ChainDetonation::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<RuinSigil>> RUIN_SIGIL = register("ruin_sigil",
            EntityType.Builder.<RuinSigil>of(RuinSigil::new, MobCategory.MISC).sized(1.6f, .2f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<DetonationWave>> DETONATION_WAVE = register("detonation_wave",
            EntityType.Builder.<DetonationWave>of(DetonationWave::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<ImplosionCore>> IMPLOSION_CORE = register("implosion_core",
            EntityType.Builder.<ImplosionCore>of(ImplosionCore::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(96).fireImmune());

    public static final RegistryObject<EntityType<ClusterShell>> CLUSTER_SHELL = register("cluster_shell",
            EntityType.Builder.<ClusterShell>of(ClusterShell::new, MobCategory.MISC).sized(.8f, .8f).clientTrackingRange(96).updateInterval(1));

    public static final RegistryObject<EntityType<ClusterBomblet>> CLUSTER_BOMBLET = register("cluster_bomblet",
            EntityType.Builder.<ClusterBomblet>of(ClusterBomblet::new, MobCategory.MISC).sized(.35f, .35f).clientTrackingRange(96).updateInterval(1));

    public static final RegistryObject<EntityType<ShieldBubble>> SHIELD_BUBBLE = register("shield_bubble",
            EntityType.Builder.<ShieldBubble>of(ShieldBubble::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<CataclysmCore>> CATACLYSM_CORE = register("cataclysm_core",
            EntityType.Builder.<CataclysmCore>of(CataclysmCore::new, MobCategory.MISC).sized(2f, 2f).clientTrackingRange(160).fireImmune());

    private ModEntities() {}

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        return ENTITIES.register(name, () -> builder.build(ArcanesOublies.id(name).toString()));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOLDEN_SENTINEL.get(), GoldenSentinel.createAttributes().build());
    }
}
