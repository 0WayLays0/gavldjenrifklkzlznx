package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.AegisPages;
import com.waylays.arcanes.entity.book.BookmarkMarker;
import com.waylays.arcanes.entity.book.ChapterRune;
import com.waylays.arcanes.entity.book.FinalChapter;
import com.waylays.arcanes.entity.book.FlyingGrimoire;
import com.waylays.arcanes.entity.book.FlyingStack;
import com.waylays.arcanes.entity.book.GrimoireBolt;
import com.waylays.arcanes.entity.book.InkHands;
import com.waylays.arcanes.entity.book.InkWave;
import com.waylays.arcanes.entity.book.OrigamiDragon;
import com.waylays.arcanes.entity.book.PageStorm;
import com.waylays.arcanes.entity.book.PaperCrane;
import com.waylays.arcanes.entity.book.PaperTempest;
import com.waylays.arcanes.entity.book.QuillStroke;
import com.waylays.arcanes.entity.book.RazorPage;
import com.waylays.arcanes.entity.book.RewriteAura;
import com.waylays.arcanes.entity.book.SilenceSeal;
import com.waylays.arcanes.entity.book.TomeOfJudgment;
import com.waylays.arcanes.entity.book.WordOfPower;
import com.waylays.arcanes.entity.errethys.ManaCamel;
import com.waylays.arcanes.entity.errethys.ManaCrow;
import com.waylays.arcanes.entity.errethys.ManaRat;
import com.waylays.arcanes.entity.errethys.ManaSalmon;
import com.waylays.arcanes.entity.errethys.ManaSerpent;
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

    // Book school
    public static final RegistryObject<EntityType<PageStorm>> PAGE_STORM = register("page_storm",
            EntityType.Builder.<PageStorm>of(PageStorm::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<InkWave>> INK_WAVE = register("ink_wave",
            EntityType.Builder.<InkWave>of(InkWave::new, MobCategory.MISC).sized(.5f, .2f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<InkHands>> INK_HANDS = register("ink_hands",
            EntityType.Builder.<InkHands>of(InkHands::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<FinalChapter>> FINAL_CHAPTER = register("final_chapter",
            EntityType.Builder.<FinalChapter>of(FinalChapter::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(160).fireImmune());

    public static final RegistryObject<EntityType<ChapterRune>> CHAPTER_RUNE = register("chapter_rune",
            EntityType.Builder.<ChapterRune>of(ChapterRune::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(96));

    public static final RegistryObject<EntityType<RazorPage>> RAZOR_PAGE = register("razor_page",
            EntityType.Builder.<RazorPage>of(RazorPage::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(64).updateInterval(1));

    public static final RegistryObject<EntityType<TomeOfJudgment>> TOME_OF_JUDGMENT = register("tome_of_judgment",
            EntityType.Builder.<TomeOfJudgment>of(TomeOfJudgment::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(128).fireImmune());

    public static final RegistryObject<EntityType<BookmarkMarker>> BOOKMARK_MARKER = register("bookmark_marker",
            EntityType.Builder.<BookmarkMarker>of(BookmarkMarker::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<SilenceSeal>> SILENCE_SEAL = register("silence_seal",
            EntityType.Builder.<SilenceSeal>of(SilenceSeal::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<FlyingGrimoire>> FLYING_GRIMOIRE = register("flying_grimoire",
            EntityType.Builder.<FlyingGrimoire>of(FlyingGrimoire::new, MobCategory.MISC).sized(.6f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<GrimoireBolt>> GRIMOIRE_BOLT = register("grimoire_bolt",
            EntityType.Builder.<GrimoireBolt>of(GrimoireBolt::new, MobCategory.MISC).sized(.3f, .3f).clientTrackingRange(64).updateInterval(1));

    public static final RegistryObject<EntityType<WordOfPower>> WORD_OF_POWER = register("word_of_power",
            EntityType.Builder.<WordOfPower>of(WordOfPower::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64).fireImmune());

    public static final RegistryObject<EntityType<RewriteAura>> REWRITE_AURA = register("rewrite_aura",
            EntityType.Builder.<RewriteAura>of(RewriteAura::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<PaperCrane>> PAPER_CRANE = register("paper_crane",
            EntityType.Builder.<PaperCrane>of(PaperCrane::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(64).updateInterval(1));

    public static final RegistryObject<EntityType<QuillStroke>> QUILL_STROKE = register("quill_stroke",
            EntityType.Builder.<QuillStroke>of(QuillStroke::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(96));

    public static final RegistryObject<EntityType<AegisPages>> AEGIS_PAGES = register("aegis_pages",
            EntityType.Builder.<AegisPages>of(AegisPages::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final RegistryObject<EntityType<PaperTempest>> PAPER_TEMPEST = register("paper_tempest",
            EntityType.Builder.<PaperTempest>of(PaperTempest::new, MobCategory.MISC).sized(1f, 2f).clientTrackingRange(96).updateInterval(2));

    public static final RegistryObject<EntityType<FlyingStack>> FLYING_STACK = register("flying_stack",
            EntityType.Builder.<FlyingStack>of(FlyingStack::new, MobCategory.MISC).sized(1.3f, .5f).clientTrackingRange(10));

    public static final RegistryObject<EntityType<OrigamiDragon>> ORIGAMI_DRAGON = register("origami_dragon",
            EntityType.Builder.<OrigamiDragon>of(OrigamiDragon::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(16).updateInterval(1).fireImmune());

    // Errethys school
    public static final RegistryObject<EntityType<ManaRat>> MANA_RAT = register("mana_rat",
            EntityType.Builder.<ManaRat>of(ManaRat::new, MobCategory.MISC).sized(.5f, .4f).clientTrackingRange(10));

    public static final RegistryObject<EntityType<ManaSalmon>> MANA_SALMON = register("mana_salmon",
            EntityType.Builder.<ManaSalmon>of(ManaSalmon::new, MobCategory.MISC).sized(.7f, .4f).clientTrackingRange(10));

    public static final RegistryObject<EntityType<ManaSerpent>> MANA_SERPENT = register("mana_serpent",
            EntityType.Builder.<ManaSerpent>of(ManaSerpent::new, MobCategory.MISC).sized(.7f, .4f).clientTrackingRange(10));

    public static final RegistryObject<EntityType<ManaCrow>> MANA_CROW = register("mana_crow",
            EntityType.Builder.<ManaCrow>of(ManaCrow::new, MobCategory.MISC).sized(.5f, .6f).clientTrackingRange(10));

    public static final RegistryObject<EntityType<ManaCamel>> MANA_DROMEDARY = register("mana_dromedary",
            EntityType.Builder.<ManaCamel>of(ManaCamel::new, MobCategory.MISC).sized(1.7f, 2.375f).clientTrackingRange(10));

    private ModEntities() {}

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        return ENTITIES.register(name, () -> builder.build(ArcanesOublies.id(name).toString()));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOLDEN_SENTINEL.get(), GoldenSentinel.createAttributes().build());
        event.put(FLYING_GRIMOIRE.get(), FlyingGrimoire.createAttributes().build());
        event.put(FLYING_STACK.get(), FlyingStack.createAttributes().build());
        event.put(MANA_RAT.get(), ManaRat.createAttributes().build());
        event.put(MANA_SALMON.get(), ManaSalmon.createAttributes().build());
        event.put(MANA_SERPENT.get(), ManaSerpent.createAttributes().build());
        event.put(MANA_CROW.get(), ManaCrow.createAttributes().build());
        event.put(MANA_DROMEDARY.get(), ManaCamel.createAttributes().build());
    }
}
