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
import com.waylays.arcanes.entity.boss.AnnihilationBeam;
import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.entity.boss.RuneWall;
import com.waylays.arcanes.entity.boss.VoidOrb;
import com.waylays.arcanes.entity.errethys.ManaCamel;
import com.waylays.arcanes.entity.errethys.ManaCrow;
import com.waylays.arcanes.entity.errethys.ManaRat;
import com.waylays.arcanes.entity.errethys.ManaPiranha;
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
import com.waylays.arcanes.entity.glyph.GlyphSign;
import com.waylays.arcanes.entity.glyph.GlyphBolt;
import com.waylays.arcanes.entity.glyph.GlyphWave;
import com.waylays.arcanes.entity.glyph.GlyphArc;
import com.waylays.arcanes.entity.glyph.GlyphMarkVisual;
import com.waylays.arcanes.entity.glyph.GlyphZone;
import com.waylays.arcanes.entity.glyph.GlyphSeal;
import com.waylays.arcanes.entity.glyph.GlyphStorm;
import com.waylays.arcanes.entity.token.TokenAegis;
import com.waylays.arcanes.entity.token.TokenBolt;
import com.waylays.arcanes.entity.token.TokenFlip;
import com.waylays.arcanes.entity.token.TokenHand;
import com.waylays.arcanes.entity.token.TokenMarker;
import com.waylays.arcanes.entity.token.TokenMine;
import com.waylays.arcanes.entity.token.TokenMote;
import com.waylays.arcanes.entity.token.TokenStorm;
import com.waylays.arcanes.entity.token.TokenWave;
import com.waylays.arcanes.entity.verdigris.AncientBell;
import com.waylays.arcanes.entity.verdigris.AncientPillar;
import com.waylays.arcanes.entity.verdigris.CopperSpikes;
import com.waylays.arcanes.entity.verdigris.RustBolt;
import com.waylays.arcanes.entity.verdigris.SporePatch;
import com.waylays.arcanes.entity.verdigris.SunkenTemple;
import com.waylays.arcanes.entity.verdigris.ToppleWall;
import com.waylays.arcanes.entity.verdigris.VineSnare;
import com.waylays.arcanes.entity.ritual.Meteor;
import com.waylays.arcanes.entity.ritual.RitualDome;
import com.waylays.arcanes.entity.ritual.RitualPortal;
import com.waylays.arcanes.entity.ritual.StormCloud;
import com.waylays.arcanes.entity.gem.GemSigil;
import com.waylays.arcanes.entity.gem.GemShatter;
import com.waylays.arcanes.entity.gem.RubyRay;
import com.waylays.arcanes.entity.gem.RubyHeart;
import com.waylays.arcanes.entity.gem.GemShard;
import com.waylays.arcanes.entity.gem.DiamondPrism;
import com.waylays.arcanes.entity.gem.DiamondShell;
import com.waylays.arcanes.entity.gem.LapisSeal;
import com.waylays.arcanes.entity.gem.LapisConstellation;
import com.waylays.arcanes.entity.gem.AmethystResonance;
import com.waylays.arcanes.entity.gem.AmethystGeode;
import com.waylays.arcanes.entity.gem.EmeraldBloom;
import com.waylays.arcanes.entity.gem.GemCrown;
import com.waylays.arcanes.entity.wall.BrickBolt;
import com.waylays.arcanes.entity.wall.BrickStorm;
import com.waylays.arcanes.ritual.RitualCircle;
import com.waylays.arcanes.entity.wall.Citadel;
import com.waylays.arcanes.entity.wall.Rampart;
import com.waylays.arcanes.entity.wall.WallImpact;
import com.waylays.arcanes.entity.wall.WallPiece;
import com.waylays.arcanes.entity.wall.WallSigil;
import com.waylays.arcanes.entity.gold.AurumBladeEntity;
import com.waylays.arcanes.entity.gold.EldoradoSun;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import com.waylays.arcanes.entity.gold.GoldSpikeEntity;
import com.waylays.arcanes.entity.gold.GoldenSentinel;
import com.waylays.arcanes.entity.gold.GoldfallIngot;
import com.waylays.arcanes.entity.gold.MoltenGoldBlob;
import com.waylays.arcanes.entity.gold.MoltenPool;
import com.waylays.arcanes.entity.physical.Missile;
import com.waylays.arcanes.entity.physical.StrikeTrail;
import com.waylays.arcanes.entity.rune.EdgeGlyphs;
import com.waylays.arcanes.entity.rune.ElderCircle;
import com.waylays.arcanes.entity.rune.PetrifyBind;
import com.waylays.arcanes.entity.rune.RuneBulwark;
import com.waylays.arcanes.entity.rune.RuneShard;
import com.waylays.arcanes.entity.rune.RuneSigil;
import com.waylays.arcanes.entity.rune.Runestone;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ArcanesOublies.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GoldBoltProjectile>> GOLD_BOLT = register("gold_bolt",
            EntityType.Builder.<GoldBoltProjectile>of(GoldBoltProjectile::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<GildedChainsEntity>> GILDED_CHAINS = register("gilded_chains",
            EntityType.Builder.<GildedChainsEntity>of(GildedChainsEntity::new, MobCategory.MISC).sized(1f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<GoldfallIngot>> GOLDFALL_INGOT = register("goldfall_ingot",
            EntityType.Builder.<GoldfallIngot>of(GoldfallIngot::new, MobCategory.MISC).sized(.9f, .9f).clientTrackingRange(96).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<GoldSpikeEntity>> GOLD_SPIKE = register("gold_spike",
            EntityType.Builder.<GoldSpikeEntity>of(GoldSpikeEntity::new, MobCategory.MISC).sized(1f, 1.8f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<AurumBladeEntity>> AURUM_BLADE = register("aurum_blade",
            EntityType.Builder.<AurumBladeEntity>of(AurumBladeEntity::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(96));

    public static final DeferredHolder<EntityType<?>, EntityType<MoltenGoldBlob>> MOLTEN_GOLD_BLOB = register("molten_gold_blob",
            EntityType.Builder.<MoltenGoldBlob>of(MoltenGoldBlob::new, MobCategory.MISC).sized(.3f, .3f).clientTrackingRange(64).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<MoltenPool>> MOLTEN_POOL = register("molten_pool",
            EntityType.Builder.<MoltenPool>of(MoltenPool::new, MobCategory.MISC).sized(2.4f, .2f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<GoldenSentinel>> GOLDEN_SENTINEL = register("golden_sentinel",
            EntityType.Builder.<GoldenSentinel>of(GoldenSentinel::new, MobCategory.MISC).sized(.6f, 1.95f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<EldoradoSun>> ELDORADO_SUN = register("eldorado_sun",
            EntityType.Builder.<EldoradoSun>of(EldoradoSun::new, MobCategory.MISC).sized(1.5f, 1.5f).clientTrackingRange(128).fireImmune());

    // Explosion school
    public static final DeferredHolder<EntityType<?>, EntityType<BlastEffect>> BLAST_EFFECT = register("blast_effect",
            EntityType.Builder.<BlastEffect>of(BlastEffect::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(160).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<ExplosiveOrb>> EXPLOSIVE_ORB = register("explosive_orb",
            EntityType.Builder.<ExplosiveOrb>of(ExplosiveOrb::new, MobCategory.MISC).sized(.6f, .6f).clientTrackingRange(64).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<ChainDetonation>> CHAIN_DETONATION = register("chain_detonation",
            EntityType.Builder.<ChainDetonation>of(ChainDetonation::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<RuinSigil>> RUIN_SIGIL = register("ruin_sigil",
            EntityType.Builder.<RuinSigil>of(RuinSigil::new, MobCategory.MISC).sized(1.6f, .2f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<DetonationWave>> DETONATION_WAVE = register("detonation_wave",
            EntityType.Builder.<DetonationWave>of(DetonationWave::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<ImplosionCore>> IMPLOSION_CORE = register("implosion_core",
            EntityType.Builder.<ImplosionCore>of(ImplosionCore::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(96).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<ClusterShell>> CLUSTER_SHELL = register("cluster_shell",
            EntityType.Builder.<ClusterShell>of(ClusterShell::new, MobCategory.MISC).sized(.8f, .8f).clientTrackingRange(96).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<ClusterBomblet>> CLUSTER_BOMBLET = register("cluster_bomblet",
            EntityType.Builder.<ClusterBomblet>of(ClusterBomblet::new, MobCategory.MISC).sized(.35f, .35f).clientTrackingRange(96).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<ShieldBubble>> SHIELD_BUBBLE = register("shield_bubble",
            EntityType.Builder.<ShieldBubble>of(ShieldBubble::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<CataclysmCore>> CATACLYSM_CORE = register("cataclysm_core",
            EntityType.Builder.<CataclysmCore>of(CataclysmCore::new, MobCategory.MISC).sized(2f, 2f).clientTrackingRange(160).fireImmune());

    // Book school
    public static final DeferredHolder<EntityType<?>, EntityType<PageStorm>> PAGE_STORM = register("page_storm",
            EntityType.Builder.<PageStorm>of(PageStorm::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<InkWave>> INK_WAVE = register("ink_wave",
            EntityType.Builder.<InkWave>of(InkWave::new, MobCategory.MISC).sized(.5f, .2f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<InkHands>> INK_HANDS = register("ink_hands",
            EntityType.Builder.<InkHands>of(InkHands::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<FinalChapter>> FINAL_CHAPTER = register("final_chapter",
            EntityType.Builder.<FinalChapter>of(FinalChapter::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(160).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<ChapterRune>> CHAPTER_RUNE = register("chapter_rune",
            EntityType.Builder.<ChapterRune>of(ChapterRune::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(96));

    public static final DeferredHolder<EntityType<?>, EntityType<RazorPage>> RAZOR_PAGE = register("razor_page",
            EntityType.Builder.<RazorPage>of(RazorPage::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(64).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<TomeOfJudgment>> TOME_OF_JUDGMENT = register("tome_of_judgment",
            EntityType.Builder.<TomeOfJudgment>of(TomeOfJudgment::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(128).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<BookmarkMarker>> BOOKMARK_MARKER = register("bookmark_marker",
            EntityType.Builder.<BookmarkMarker>of(BookmarkMarker::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<SilenceSeal>> SILENCE_SEAL = register("silence_seal",
            EntityType.Builder.<SilenceSeal>of(SilenceSeal::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingGrimoire>> FLYING_GRIMOIRE = register("flying_grimoire",
            EntityType.Builder.<FlyingGrimoire>of(FlyingGrimoire::new, MobCategory.MISC).sized(.6f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<GrimoireBolt>> GRIMOIRE_BOLT = register("grimoire_bolt",
            EntityType.Builder.<GrimoireBolt>of(GrimoireBolt::new, MobCategory.MISC).sized(.3f, .3f).clientTrackingRange(64).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<WordOfPower>> WORD_OF_POWER = register("word_of_power",
            EntityType.Builder.<WordOfPower>of(WordOfPower::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<RewriteAura>> REWRITE_AURA = register("rewrite_aura",
            EntityType.Builder.<RewriteAura>of(RewriteAura::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<PaperCrane>> PAPER_CRANE = register("paper_crane",
            EntityType.Builder.<PaperCrane>of(PaperCrane::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(64).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<QuillStroke>> QUILL_STROKE = register("quill_stroke",
            EntityType.Builder.<QuillStroke>of(QuillStroke::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(96));

    public static final DeferredHolder<EntityType<?>, EntityType<AegisPages>> AEGIS_PAGES = register("aegis_pages",
            EntityType.Builder.<AegisPages>of(AegisPages::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(64));

    public static final DeferredHolder<EntityType<?>, EntityType<PaperTempest>> PAPER_TEMPEST = register("paper_tempest",
            EntityType.Builder.<PaperTempest>of(PaperTempest::new, MobCategory.MISC).sized(1f, 2f).clientTrackingRange(96).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingStack>> FLYING_STACK = register("flying_stack",
            EntityType.Builder.<FlyingStack>of(FlyingStack::new, MobCategory.MISC).sized(1.3f, .5f).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<OrigamiDragon>> ORIGAMI_DRAGON = register("origami_dragon",
            EntityType.Builder.<OrigamiDragon>of(OrigamiDragon::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(16).updateInterval(1).fireImmune());

    // Errethys school
    public static final DeferredHolder<EntityType<?>, EntityType<ManaRat>> MANA_RAT = register("mana_rat",
            EntityType.Builder.<ManaRat>of(ManaRat::new, MobCategory.MISC).sized(.5f, .4f).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<ManaPiranha>> MANA_PIRANHA = register("mana_piranha",
            EntityType.Builder.<ManaPiranha>of(ManaPiranha::new, MobCategory.MISC).sized(.6f, .6f).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<ManaSerpent>> MANA_SERPENT = register("mana_serpent",
            EntityType.Builder.<ManaSerpent>of(ManaSerpent::new, MobCategory.MISC).sized(.7f, .4f).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<ManaCrow>> MANA_CROW = register("mana_crow",
            EntityType.Builder.<ManaCrow>of(ManaCrow::new, MobCategory.MISC).sized(.5f, .6f).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<ManaCamel>> MANA_DROMEDARY = register("mana_dromedary",
            EntityType.Builder.<ManaCamel>of(ManaCamel::new, MobCategory.MISC).sized(1.7f, 2.375f).clientTrackingRange(10));

    // Physical Prowess
    public static final DeferredHolder<EntityType<?>, EntityType<StrikeTrail>> STRIKE_TRAIL = register("strike_trail",
            EntityType.Builder.<StrikeTrail>of(StrikeTrail::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(20));
    // Updated every tick, unlike vanilla arrows: guided arrows turn in flight
    public static final DeferredHolder<EntityType<?>, EntityType<Missile>> MISSILE = register("missile",
            EntityType.Builder.<Missile>of(Missile::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(1));

    // Boss Magic: warnings, scenery and the attacks that outlive their cast (tracked far, drawn big)
    public static final DeferredHolder<EntityType<?>, EntityType<BossTelegraph>> BOSS_TELEGRAPH = register("boss_telegraph",
            EntityType.Builder.<BossTelegraph>of(BossTelegraph::new, MobCategory.MISC).sized(1f, .2f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<BossProp>> BOSS_PROP = register("boss_prop",
            EntityType.Builder.<BossProp>of(BossProp::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<AnnihilationBeam>> ANNIHILATION_BEAM = register("annihilation_beam",
            EntityType.Builder.<AnnihilationBeam>of(AnnihilationBeam::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<VoidOrb>> VOID_ORB = register("void_orb",
            EntityType.Builder.<VoidOrb>of(VoidOrb::new, MobCategory.MISC).sized(2.8f, 2.8f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<RuneWall>> RUNE_WALL = register("rune_wall",
            EntityType.Builder.<RuneWall>of(RuneWall::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());

    // Rune Magic: carved runes, standing stones and the shards
    public static final DeferredHolder<EntityType<?>, EntityType<RuneSigil>> RUNE_SIGIL = register("rune_sigil",
            EntityType.Builder.<RuneSigil>of(RuneSigil::new, MobCategory.MISC).sized(1f, .2f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Runestone>> RUNESTONE = register("runestone",
            EntityType.Builder.<Runestone>of(Runestone::new, MobCategory.MISC).sized(1f, 2.4f).clientTrackingRange(10).updateInterval(20).fireImmune());
    // Solid: as wide as it is drawn, so slabs side by side leave no gap
    public static final DeferredHolder<EntityType<?>, EntityType<RuneBulwark>> RUNE_BULWARK = register("rune_bulwark",
            EntityType.Builder.<RuneBulwark>of(RuneBulwark::new, MobCategory.MISC).sized(1.2f, 2.6f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<RuneShard>> RUNE_SHARD = register("rune_shard",
            EntityType.Builder.<RuneShard>of(RuneShard::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(8).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<PetrifyBind>> PETRIFY_BIND = register("petrify_bind",
            EntityType.Builder.<PetrifyBind>of(PetrifyBind::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(2).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<EdgeGlyphs>> EDGE_GLYPHS = register("edge_glyphs",
            EntityType.Builder.<EdgeGlyphs>of(EdgeGlyphs::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(2).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<ElderCircle>> ELDER_CIRCLE = register("elder_circle",
            EntityType.Builder.<ElderCircle>of(ElderCircle::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());

    // Glyph Magic: glyphs written in light, their bolts, waves and arcs, the marks and the reactions' zones
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphSign>> GLYPH_SIGN = register("glyph_sign",
            EntityType.Builder.<GlyphSign>of(GlyphSign::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphBolt>> GLYPH_BOLT = register("glyph_bolt",
            EntityType.Builder.<GlyphBolt>of(GlyphBolt::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphWave>> GLYPH_WAVE = register("glyph_wave",
            EntityType.Builder.<GlyphWave>of(GlyphWave::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphArc>> GLYPH_ARC = register("glyph_arc",
            EntityType.Builder.<GlyphArc>of(GlyphArc::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphMarkVisual>> GLYPH_MARK = register("glyph_mark",
            EntityType.Builder.<GlyphMarkVisual>of(GlyphMarkVisual::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(5).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphZone>> GLYPH_ZONE = register("glyph_zone",
            EntityType.Builder.<GlyphZone>of(GlyphZone::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphSeal>> GLYPH_SEAL = register("glyph_seal",
            EntityType.Builder.<GlyphSeal>of(GlyphSeal::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(5).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GlyphStorm>> GLYPH_STORM = register("glyph_storm",
            EntityType.Builder.<GlyphStorm>of(GlyphStorm::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());

    // Token Magic: soul tokens flung, laid, whirled and gathered into a hand
    public static final DeferredHolder<EntityType<?>, EntityType<TokenBolt>> TOKEN_BOLT = register("token_bolt",
            EntityType.Builder.<TokenBolt>of(TokenBolt::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(8).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<TokenMarker>> TOKEN_MARKER = register("token_marker",
            EntityType.Builder.<TokenMarker>of(TokenMarker::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<TokenMine>> TOKEN_MINE = register("token_mine",
            EntityType.Builder.<TokenMine>of(TokenMine::new, MobCategory.MISC).sized(.5f, .2f).clientTrackingRange(8).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<TokenMote>> TOKEN_MOTE = register("token_mote",
            EntityType.Builder.<TokenMote>of(TokenMote::new, MobCategory.MISC).sized(.3f, .3f).clientTrackingRange(8).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<TokenFlip>> TOKEN_FLIP = register("token_flip",
            EntityType.Builder.<TokenFlip>of(TokenFlip::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<TokenAegis>> TOKEN_AEGIS = register("token_aegis",
            EntityType.Builder.<TokenAegis>of(TokenAegis::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<TokenStorm>> TOKEN_STORM = register("token_storm",
            EntityType.Builder.<TokenStorm>of(TokenStorm::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<TokenWave>> TOKEN_WAVE = register("token_wave",
            EntityType.Builder.<TokenWave>of(TokenWave::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<TokenHand>> TOKEN_HAND = register("token_hand",
            EntityType.Builder.<TokenHand>of(TokenHand::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(1).fireImmune());

    // Moss, Copper and Rust: ancient copper risen from the ground, rust and moss
    public static final DeferredHolder<EntityType<?>, EntityType<RustBolt>> RUST_BOLT = register("rust_bolt",
            EntityType.Builder.<RustBolt>of(RustBolt::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<AncientPillar>> ANCIENT_PILLAR = register("ancient_pillar",
            EntityType.Builder.<AncientPillar>of(AncientPillar::new, MobCategory.MISC).sized(1.2f, 3.6f).clientTrackingRange(12).updateInterval(1).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<CopperSpikes>> COPPER_SPIKES = register("copper_spikes",
            EntityType.Builder.<CopperSpikes>of(CopperSpikes::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<ToppleWall>> TOPPLE_WALL = register("toppling_wall",
            EntityType.Builder.<ToppleWall>of(ToppleWall::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<AncientBell>> ANCIENT_BELL = register("ancient_bell",
            EntityType.Builder.<AncientBell>of(AncientBell::new, MobCategory.MISC).sized(2.4f, 2.4f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<VineSnare>> VINE_SNARE = register("vine_snare",
            EntityType.Builder.<VineSnare>of(VineSnare::new, MobCategory.MISC).sized(.6f, .6f).clientTrackingRange(8).updateInterval(5).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<SporePatch>> SPORE_PATCH = register("spore_patch",
            EntityType.Builder.<SporePatch>of(SporePatch::new, MobCategory.MISC).sized(1f, .2f).clientTrackingRange(8).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<SunkenTemple>> SUNKEN_TEMPLE = register("sunken_temple",
            EntityType.Builder.<SunkenTemple>of(SunkenTemple::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(14).updateInterval(20).fireImmune());

    // Wall Magic: walls and bricks of stone bricks
    public static final DeferredHolder<EntityType<?>, EntityType<BrickBolt>> BRICK_BOLT = register("brick_bolt",
            EntityType.Builder.<BrickBolt>of(BrickBolt::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<WallPiece>> WALL_PIECE = register("wall_piece",
            EntityType.Builder.<WallPiece>of(WallPiece::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Citadel>> CITADEL = register("citadel",
            EntityType.Builder.<Citadel>of(Citadel::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(16).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GemSigil>> GEM_SIGIL = register("gem_sigil",
            EntityType.Builder.<GemSigil>of(GemSigil::new, MobCategory.MISC).sized(.5f, .1f).clientTrackingRange(10).updateInterval(40).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GemShatter>> GEM_SHATTER = register("gem_shatter",
            EntityType.Builder.<GemShatter>of(GemShatter::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(10).updateInterval(40).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<RubyRay>> RUBY_RAY = register("ruby_ray",
            EntityType.Builder.<RubyRay>of(RubyRay::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(12).updateInterval(1).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<RubyHeart>> RUBY_HEART = register("ruby_heart",
            EntityType.Builder.<RubyHeart>of(RubyHeart::new, MobCategory.MISC).sized(.4f, .4f).clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<GemShard>> GEM_SHARD = register("gem_shard",
            EntityType.Builder.<GemShard>of(GemShard::new, MobCategory.MISC).sized(.3f, .3f).clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<DiamondPrism>> DIAMOND_PRISM = register("diamond_prism",
            EntityType.Builder.<DiamondPrism>of(DiamondPrism::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<DiamondShell>> DIAMOND_SHELL = register("diamond_shell",
            EntityType.Builder.<DiamondShell>of(DiamondShell::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(2).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<LapisSeal>> LAPIS_SEAL = register("lapis_seal",
            EntityType.Builder.<LapisSeal>of(LapisSeal::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<LapisConstellation>> LAPIS_CONSTELLATION = register("lapis_constellation",
            EntityType.Builder.<LapisConstellation>of(LapisConstellation::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(2).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<AmethystResonance>> AMETHYST_RESONANCE = register("amethyst_resonance",
            EntityType.Builder.<AmethystResonance>of(AmethystResonance::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<AmethystGeode>> AMETHYST_GEODE = register("amethyst_geode",
            EntityType.Builder.<AmethystGeode>of(AmethystGeode::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<EmeraldBloom>> EMERALD_BLOOM = register("emerald_bloom",
            EntityType.Builder.<EmeraldBloom>of(EmeraldBloom::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(10).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GemCrown>> GEM_CROWN = register("gem_crown",
            EntityType.Builder.<GemCrown>of(GemCrown::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(16).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Rampart>> RAMPART = register("rampart",
            EntityType.Builder.<Rampart>of(Rampart::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(16).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<BrickStorm>> BRICK_STORM = register("brick_storm",
            EntityType.Builder.<BrickStorm>of(BrickStorm::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(12).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<WallSigil>> WALL_SIGIL = register("wall_sigil",
            EntityType.Builder.<WallSigil>of(WallSigil::new, MobCategory.MISC).sized(.5f, .1f).clientTrackingRange(10).updateInterval(40).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<WallImpact>> WALL_IMPACT = register("wall_impact",
            EntityType.Builder.<WallImpact>of(WallImpact::new, MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(10).updateInterval(40).fireImmune());

    // Rituals: the chalk circle and what the rituals leave in the world
    public static final DeferredHolder<EntityType<?>, EntityType<RitualCircle>> RITUAL_CIRCLE = register("ritual_circle",
            EntityType.Builder.<RitualCircle>of(RitualCircle::new, MobCategory.MISC).sized(1f, .1f).clientTrackingRange(16).updateInterval(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Meteor>> METEOR = register("meteor",
            EntityType.Builder.<Meteor>of(Meteor::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(20).updateInterval(20).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<RitualDome>> RITUAL_DOME = register("ritual_dome",
            EntityType.Builder.<RitualDome>of(RitualDome::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(20).updateInterval(40).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<StormCloud>> STORM_CLOUD = register("storm_cloud",
            EntityType.Builder.<StormCloud>of(StormCloud::new, MobCategory.MISC).sized(1f, 1f).clientTrackingRange(20).updateInterval(40).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<RitualPortal>> RITUAL_PORTAL = register("ritual_portal",
            EntityType.Builder.<RitualPortal>of(RitualPortal::new, MobCategory.MISC).sized(1.4f, 2.8f).clientTrackingRange(12).updateInterval(40).fireImmune());

    private ModEntities() {}

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        return ENTITIES.register(name, () -> builder.build(ArcanesOublies.id(name).toString()));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOLDEN_SENTINEL.get(), GoldenSentinel.createAttributes().build());
        event.put(FLYING_GRIMOIRE.get(), FlyingGrimoire.createAttributes().build());
        event.put(FLYING_STACK.get(), FlyingStack.createAttributes().build());
        event.put(MANA_RAT.get(), ManaRat.createAttributes().build());
        event.put(MANA_PIRANHA.get(), ManaPiranha.createAttributes().build());
        event.put(MANA_SERPENT.get(), ManaSerpent.createAttributes().build());
        event.put(MANA_CROW.get(), ManaCrow.createAttributes().build());
        event.put(MANA_DROMEDARY.get(), ManaCamel.createAttributes().build());
    }
}
