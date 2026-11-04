package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.book.BookmarkSpell;
import com.waylays.arcanes.spells.book.FinalChapterSpell;
import com.waylays.arcanes.spells.book.FlyingStackSpell;
import com.waylays.arcanes.spells.book.ForbiddenLibrarySpell;
import com.waylays.arcanes.spells.book.LivingInkSpell;
import com.waylays.arcanes.spells.book.OrigamiDragonSpell;
import com.waylays.arcanes.spells.book.PageStormSpell;
import com.waylays.arcanes.spells.book.PaperCranesSpell;
import com.waylays.arcanes.spells.book.PaperTempestSpell;
import com.waylays.arcanes.spells.book.ParchmentAegisSpell;
import com.waylays.arcanes.spells.book.QuillOfFateSpell;
import com.waylays.arcanes.spells.book.RazorPagesSpell;
import com.waylays.arcanes.spells.book.RewriteSpell;
import com.waylays.arcanes.spells.book.SealOfSilenceSpell;
import com.waylays.arcanes.spells.book.TomeOfJudgmentSpell;
import com.waylays.arcanes.spells.book.WordOfPowerSpell;
import com.waylays.arcanes.spells.boss.AnnihilationBeamSpell;
import com.waylays.arcanes.spells.boss.ThousandBladesSpell;
import com.waylays.arcanes.spells.boss.ExecutionersWheelSpell;
import com.waylays.arcanes.spells.boss.AbyssalMawSpell;
import com.waylays.arcanes.spells.boss.DivinePrismSpell;
import com.waylays.arcanes.spells.boss.ChainsOfTheDamnedSpell;
import com.waylays.arcanes.spells.boss.PentacleOfThornsSpell;
import com.waylays.arcanes.spells.boss.SeraphWingsSpell;
import com.waylays.arcanes.spells.boss.BellOfDoomSpell;
import com.waylays.arcanes.spells.boss.InfernalCheckerboardSpell;
import com.waylays.arcanes.spells.boss.TitansFistSpell;
import com.waylays.arcanes.spells.boss.TopplingColumnsSpell;
import com.waylays.arcanes.spells.boss.ReapersScytheSpell;
import com.waylays.arcanes.spells.boss.IronMaidensSpell;
import com.waylays.arcanes.spells.boss.SpectralKingsSpell;
import com.waylays.arcanes.spells.boss.ClockOfFateSpell;
import com.waylays.arcanes.spells.boss.CrimsonLotusSpell;
import com.waylays.arcanes.spells.boss.CollapseSpell;
import com.waylays.arcanes.spells.boss.WrathOfTheHeavensSpell;
import com.waylays.arcanes.spells.boss.EclipseSpell;
import com.waylays.arcanes.spells.boss.FallenMoonSpell;
import com.waylays.arcanes.spells.boss.CrossOfLightSpell;
import com.waylays.arcanes.spells.boss.CrushingMeteorSpell;
import com.waylays.arcanes.spells.boss.JudgmentBladeSpell;
import com.waylays.arcanes.spells.boss.LastJudgmentSpell;
import com.waylays.arcanes.spells.boss.RunePrisonSpell;
import com.waylays.arcanes.spells.boss.SeismicRiftSpell;
import com.waylays.arcanes.spells.boss.PendulumSpell;
import com.waylays.arcanes.spells.boss.TripleNovaSpell;
import com.waylays.arcanes.spells.boss.VoidOrbSpell;
import com.waylays.arcanes.spells.errethys.ManaCrowsSpell;
import com.waylays.arcanes.spells.errethys.ManaWolfSpell;
import com.waylays.arcanes.spells.errethys.ManaKangarooSpell;
import com.waylays.arcanes.spells.errethys.ManaFoxSpell;
import com.waylays.arcanes.spells.errethys.ManaFalconSpell;
import com.waylays.arcanes.spells.errethys.ManaButterflySpell;
import com.waylays.arcanes.spells.errethys.ManaDromedarySpell;
import com.waylays.arcanes.spells.errethys.ManaRatsSpell;
import com.waylays.arcanes.spells.errethys.ManaPiranhaSpell;
import com.waylays.arcanes.spells.errethys.ManaSerpentSpell;
import com.waylays.arcanes.spells.explosion.BlastLeapSpell;
import com.waylays.arcanes.spells.explosion.BlastOrbSpell;
import com.waylays.arcanes.spells.explosion.CataclysmSpell;
import com.waylays.arcanes.spells.explosion.ChainDetonationSpell;
import com.waylays.arcanes.spells.explosion.ClusterBarrageSpell;
import com.waylays.arcanes.spells.explosion.DetonationWaveSpell;
import com.waylays.arcanes.spells.explosion.ImplosionSpell;
import com.waylays.arcanes.spells.explosion.ReactiveShieldSpell;
import com.waylays.arcanes.spells.explosion.SigilsOfRuinSpell;
import com.waylays.arcanes.spells.explosion.VolatileBrandSpell;
import com.waylays.arcanes.spells.gold.AurumBladeSpell;
import com.waylays.arcanes.spells.gold.GildedArmorSpell;
import com.waylays.arcanes.spells.gold.GildedChainsSpell;
import com.waylays.arcanes.spells.gold.GoldBoltSpell;
import com.waylays.arcanes.spells.gold.GoldenDragonSpell;
import com.waylays.arcanes.spells.gold.GoldenSentinelsSpell;
import com.waylays.arcanes.spells.gold.GoldenSpikesSpell;
import com.waylays.arcanes.spells.gold.GoldfallSpell;
import com.waylays.arcanes.spells.gold.MidasCurseSpell;
import com.waylays.arcanes.spells.gold.MoltenTorrentSpell;
import com.waylays.arcanes.spells.gold.SunOfEldoradoSpell;
import com.waylays.arcanes.spells.physical.AxeCleaveSkill;
import com.waylays.arcanes.spells.physical.AxeSunderSkill;
import com.waylays.arcanes.spells.physical.AxeThrowSkill;
import com.waylays.arcanes.spells.physical.BowGuidedSkill;
import com.waylays.arcanes.spells.physical.BowRainSkill;
import com.waylays.arcanes.spells.physical.BowVolleySkill;
import com.waylays.arcanes.spells.physical.CrossbowBurstSkill;
import com.waylays.arcanes.spells.physical.CrossbowHarpoonSkill;
import com.waylays.arcanes.spells.physical.CrossbowPierceSkill;
import com.waylays.arcanes.spells.physical.DaggerFinisherSkill;
import com.waylays.arcanes.spells.physical.DaggerFlankSkill;
import com.waylays.arcanes.spells.physical.DaggerStabsSkill;
import com.waylays.arcanes.spells.physical.DashSkill;
import com.waylays.arcanes.spells.physical.EvadeSkill;
import com.waylays.arcanes.spells.physical.HammerConcussionSkill;
import com.waylays.arcanes.spells.physical.HammerQuakeSkill;
import com.waylays.arcanes.spells.physical.HammerRamSkill;
import com.waylays.arcanes.spells.physical.KnifeFanSkill;
import com.waylays.arcanes.spells.physical.KnifePinSkill;
import com.waylays.arcanes.spells.physical.KnifeThrowSkill;
import com.waylays.arcanes.spells.physical.LeapSkill;
import com.waylays.arcanes.spells.physical.LongswordCutSkill;
import com.waylays.arcanes.spells.physical.LongswordParrySkill;
import com.waylays.arcanes.spells.physical.LongswordSweepSkill;
import com.waylays.arcanes.spells.physical.PunchFlurrySkill;
import com.waylays.arcanes.spells.physical.PunchSkill;
import com.waylays.arcanes.spells.physical.RushSkill;
import com.waylays.arcanes.spells.physical.ScytheDanceSkill;
import com.waylays.arcanes.spells.physical.ScytheHookSkill;
import com.waylays.arcanes.spells.physical.ScytheReapSkill;
import com.waylays.arcanes.spells.physical.SlashFlurrySkill;
import com.waylays.arcanes.spells.physical.SlashSkill;
import com.waylays.arcanes.spells.physical.SpearChargeSkill;
import com.waylays.arcanes.spells.physical.SpearJavelinSkill;
import com.waylays.arcanes.spells.physical.SpearThrustSkill;
import com.waylays.arcanes.spells.physical.SwordDisarmSkill;
import com.waylays.arcanes.spells.physical.SwordLungeSkill;
import com.waylays.arcanes.spells.physical.SwordRisingCutSkill;
import com.waylays.arcanes.spells.glyph.FireGlyphSpell;
import com.waylays.arcanes.spells.glyph.FrostGlyphSpell;
import com.waylays.arcanes.spells.glyph.StormGlyphSpell;
import com.waylays.arcanes.spells.glyph.TideGlyphSpell;
import com.waylays.arcanes.spells.glyph.GaleGlyphSpell;
import com.waylays.arcanes.spells.glyph.StoneGlyphSpell;
import com.waylays.arcanes.spells.glyph.ElementalSealSpell;
import com.waylays.arcanes.spells.glyph.ElementalScriptSpell;
import com.waylays.arcanes.spells.glyph.GlyphResonanceSpell;
import com.waylays.arcanes.spells.glyph.PrimordialGlyphSpell;
import com.waylays.arcanes.spells.token.CoinFlipSpell;
import com.waylays.arcanes.spells.token.SoulTributeSpell;
import com.waylays.arcanes.spells.token.TokenAegisSpell;
import com.waylays.arcanes.spells.token.TokenHandSpell;
import com.waylays.arcanes.spells.token.TokenMinesSpell;
import com.waylays.arcanes.spells.token.TokenRainSpell;
import com.waylays.arcanes.spells.token.TokenRicochetSpell;
import com.waylays.arcanes.spells.token.TokenStormSpell;
import com.waylays.arcanes.spells.token.TokenThrowSpell;
import com.waylays.arcanes.spells.token.TokenWaveSpell;
import com.waylays.arcanes.spells.verdigris.AncientBellSpell;
import com.waylays.arcanes.spells.verdigris.AncientPillarSpell;
import com.waylays.arcanes.spells.verdigris.ColonnadeSpell;
import com.waylays.arcanes.spells.verdigris.CopperSpikesSpell;
import com.waylays.arcanes.spells.verdigris.LightningRodSpell;
import com.waylays.arcanes.spells.verdigris.MossSporesSpell;
import com.waylays.arcanes.spells.verdigris.RustSpell;
import com.waylays.arcanes.spells.verdigris.StranglingVinesSpell;
import com.waylays.arcanes.spells.verdigris.SunkenTempleSpell;
import com.waylays.arcanes.spells.verdigris.TopplingWallSpell;
import com.waylays.arcanes.spells.gem.RubyRaySpell;
import com.waylays.arcanes.spells.gem.RubyHeartSpell;
import com.waylays.arcanes.spells.gem.LapisSealSpell;
import com.waylays.arcanes.spells.gem.LapisConstellationSpell;
import com.waylays.arcanes.spells.gem.DiamondPrismSpell;
import com.waylays.arcanes.spells.gem.DiamondShellSpell;
import com.waylays.arcanes.spells.gem.AmethystResonanceSpell;
import com.waylays.arcanes.spells.gem.AmethystGeodeSpell;
import com.waylays.arcanes.spells.gem.EmeraldBloomSpell;
import com.waylays.arcanes.spells.gem.CrownOfGemsSpell;
import com.waylays.arcanes.spells.gem.RubyShardsSpell;
import com.waylays.arcanes.spells.gem.RubyNovaSpell;
import com.waylays.arcanes.spells.gem.RubyMeteorSpell;
import com.waylays.arcanes.spells.gem.ShootingStarSpell;
import com.waylays.arcanes.spells.gem.CelestialVaultSpell;
import com.waylays.arcanes.spells.gem.LapisMirrorSpell;
import com.waylays.arcanes.spells.gem.DiamondLanceSpell;
import com.waylays.arcanes.spells.gem.DiamondDustSpell;
import com.waylays.arcanes.spells.gem.DiamondBastionSpell;
import com.waylays.arcanes.spells.gem.AmethystSpikesSpell;
import com.waylays.arcanes.spells.gem.BuddingAmethystSpell;
import com.waylays.arcanes.spells.gem.AmethystChimesSpell;
import com.waylays.arcanes.spells.gem.EmeraldLifelineSpell;
import com.waylays.arcanes.spells.gem.EmeraldAegisSpell;
import com.waylays.arcanes.spells.gem.EmeraldTreeSpell;
import com.waylays.arcanes.spells.gem.EmeraldThornsSpell;
import com.waylays.arcanes.spells.bone.BoneLanceSpell;
import com.waylays.arcanes.spells.bone.ColossalSpikeSpell;
import com.waylays.arcanes.spells.bone.ElbowBladeSpell;
import com.waylays.arcanes.spells.bone.HowlingSkullSpell;
import com.waylays.arcanes.spells.bone.BoneNovaSpell;
import com.waylays.arcanes.spells.bone.BoneTempestSpell;
import com.waylays.arcanes.spells.bone.BoneSpikesSpell;
import com.waylays.arcanes.spells.bone.SpineWhipSpell;
import com.waylays.arcanes.spells.bone.BoneVolleySpell;
import com.waylays.arcanes.spells.bone.BoneWardSpell;
import com.waylays.arcanes.spells.bone.RibCageSpell;
import com.waylays.arcanes.spells.bone.OssuarySpell;
import com.waylays.arcanes.spells.bone.SkeletalGraspSpell;
import com.waylays.arcanes.spells.bone.BoneWyrmSpell;
import com.waylays.arcanes.spells.bone.GashadokuroSpell;
import com.waylays.arcanes.spells.wood.LogHurlSpell;
import com.waylays.arcanes.spells.wood.GraspingRootsSpell;
import com.waylays.arcanes.spells.wood.SeedPodsSpell;
import com.waylays.arcanes.spells.wood.BrambleHedgeSpell;
import com.waylays.arcanes.spells.wood.VineLashSpell;
import com.waylays.arcanes.spells.wood.RingSawSpell;
import com.waylays.arcanes.spells.wood.SplinterStormSpell;
import com.waylays.arcanes.spells.wood.AmberPrisonSpell;
import com.waylays.arcanes.spells.wood.WoodGuardianSpell;
import com.waylays.arcanes.spells.wood.WorldTreeSpell;
import com.waylays.arcanes.spells.cosmos.AsteroidBeltSpell;
import com.waylays.arcanes.spells.cosmos.BlackHoleSpell;
import com.waylays.arcanes.spells.cosmos.CaptiveOrbitSpell;
import com.waylays.arcanes.spells.cosmos.CometRideSpell;
import com.waylays.arcanes.spells.cosmos.LivingConstellationSpell;
import com.waylays.arcanes.spells.cosmos.PocketMoonSpell;
import com.waylays.arcanes.spells.cosmos.ResetChargeSpell;
import com.waylays.arcanes.spells.cosmos.TimeLoopSpell;
import com.waylays.arcanes.spells.cosmos.ZeroGravitySpell;
import com.waylays.arcanes.spells.wall.BastionSpell;
import com.waylays.arcanes.spells.wall.BrickBoltSpell;
import com.waylays.arcanes.spells.wall.BrickCageSpell;
import com.waylays.arcanes.spells.wall.BrickQuakeSpell;
import com.waylays.arcanes.spells.wall.BrickRainSpell;
import com.waylays.arcanes.spells.wall.CitadelSpell;
import com.waylays.arcanes.spells.wall.ClosingKeepSpell;
import com.waylays.arcanes.spells.wall.CrushingWallsSpell;
import com.waylays.arcanes.spells.wall.GreatRampartSpell;
import com.waylays.arcanes.spells.wall.MortarSpell;
import com.waylays.arcanes.spells.wall.RampartChargeSpell;
import com.waylays.arcanes.spells.wall.RisingWallSpell;
import com.waylays.arcanes.spells.wall.SiegeSpell;
import com.waylays.arcanes.spells.wall.WallBurstSpell;
import com.waylays.arcanes.spells.rune.CircleOfEldersSpell;
import com.waylays.arcanes.spells.rune.EdgeRuneSpell;
import com.waylays.arcanes.spells.rune.MendingRunestoneSpell;
import com.waylays.arcanes.spells.rune.MenhirSurgeSpell;
import com.waylays.arcanes.spells.rune.PetrifyingRuneSpell;
import com.waylays.arcanes.spells.rune.RuneShardsSpell;
import com.waylays.arcanes.spells.rune.RunicBulwarkSpell;
import com.waylays.arcanes.spells.rune.RunicPathSpell;
import com.waylays.arcanes.spells.rune.SnareRuneSpell;
import com.waylays.arcanes.spells.rune.WardRuneSpell;
import com.waylays.arcanes.spells.fire.EmberBoltSpell;
import com.waylays.arcanes.spells.fire.StellarBreathSpell;
import com.waylays.arcanes.spells.fire.CoronaSpell;
import com.waylays.arcanes.spells.fire.FirePillarsSpell;
import com.waylays.arcanes.spells.fire.WillOWispsSpell;
import com.waylays.arcanes.spells.fire.FlameCrescentSpell;
import com.waylays.arcanes.spells.fire.SearingBrandSpell;
import com.waylays.arcanes.spells.fire.PhoenixSpell;
import com.waylays.arcanes.spells.fire.SupernovaSpell;
import com.waylays.arcanes.spells.fire.StellarPyreSpell;
import com.waylays.arcanes.spells.water.TideLanceSpell;
import com.waylays.arcanes.spells.wind.GustBladeSpell;
import com.waylays.arcanes.spells.wind.GaleSpell;
import com.waylays.arcanes.spells.wind.UpdraftSpell;
import com.waylays.arcanes.spells.wind.TailwindSpell;
import com.waylays.arcanes.spells.wind.CycloneSpell;
import com.waylays.arcanes.spells.wind.VacuumSpell;
import com.waylays.arcanes.spells.wind.WindWallSpell;
import com.waylays.arcanes.spells.wind.BladeStormSpell;
import com.waylays.arcanes.spells.sand.SandLanceSpell;
import com.waylays.arcanes.spells.sand.ScarabSwarmSpell;
import com.waylays.arcanes.spells.sand.DesertGraspSpell;
import com.waylays.arcanes.spells.sand.SandVeilSpell;
import com.waylays.arcanes.spells.sand.HourglassSpell;
import com.waylays.arcanes.spells.sand.DuneSerpentSpell;
import com.waylays.arcanes.spells.sand.SunLensSpell;
import com.waylays.arcanes.spells.sand.SandfallSpell;
import com.waylays.arcanes.spells.sand.SandDjinnSpell;
import com.waylays.arcanes.spells.sand.SimoomSpell;
import com.waylays.arcanes.spells.lotus.RazorPetalsSpell;
import com.waylays.arcanes.spells.lotus.LotusBudSpell;
import com.waylays.arcanes.spells.lotus.SacredPondSpell;
import com.waylays.arcanes.spells.lotus.LotusThroneSpell;
import com.waylays.arcanes.spells.lotus.MireRootsSpell;
import com.waylays.arcanes.spells.lotus.SeedPodSpell;
import com.waylays.arcanes.spells.lotus.LotusMandalaSpell;
import com.waylays.arcanes.spells.lotus.LotusLanternsSpell;
import com.waylays.arcanes.spells.lotus.PetalStormSpell;
import com.waylays.arcanes.spells.lotus.ThousandPetalLotusSpell;
import com.waylays.arcanes.spells.echo.SoundWaveSpell;
import com.waylays.arcanes.spells.echo.ShriekSpell;
import com.waylays.arcanes.spells.echo.EchoStepSpell;
import com.waylays.arcanes.spells.echo.SculkTendrilsSpell;
import com.waylays.arcanes.spells.echo.SculkSensorSpell;
import com.waylays.arcanes.spells.echo.ResonantBurstSpell;
import com.waylays.arcanes.spells.echo.SonicBoomSpell;
import com.waylays.arcanes.spells.echo.SilenceDomeSpell;
import com.waylays.arcanes.spells.echo.DeepChoirSpell;
import com.waylays.arcanes.spells.echo.RequiemSpell;
import com.waylays.arcanes.spells.wind.SkyfallSpell;
import com.waylays.arcanes.spells.wind.HurricaneSpell;
import com.waylays.arcanes.spells.water.PressureJetSpell;
import com.waylays.arcanes.spells.water.GeyserSpell;
import com.waylays.arcanes.spells.water.BubblePrisonSpell;
import com.waylays.arcanes.spells.water.CleansingRainSpell;
import com.waylays.arcanes.spells.water.MaelstromSpell;
import com.waylays.arcanes.spells.water.JellyfishSwarmSpell;
import com.waylays.arcanes.spells.water.TyphoonSpell;
import com.waylays.arcanes.spells.water.TidalWaveSpell;
import com.waylays.arcanes.spells.water.AbyssalDomeSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Spells added to Iron's spell registry. Every spell class also needs {@code @AutoSpellConfig} and a no-arg
 * constructor, a spell icon at textures/gui/spell_icons/{@code <spell>}.png and its lang keys.
 */
public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ArcanesOublies.MODID);

    // Gold school
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GOLD_BOLT = register(new GoldBoltSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GILDED_CHAINS = register(new GildedChainsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GOLDFALL = register(new GoldfallSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GOLDEN_SPIKES = register(new GoldenSpikesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MIDAS_CURSE = register(new MidasCurseSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AURUM_BLADE = register(new AurumBladeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MOLTEN_TORRENT = register(new MoltenTorrentSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GILDED_ARMOR = register(new GildedArmorSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GOLDEN_SENTINELS = register(new GoldenSentinelsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUN_OF_ELDORADO = register(new SunOfEldoradoSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GOLDEN_DRAGON = register(new GoldenDragonSpell());

    // Explosion school
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BLAST_ORB = register(new BlastOrbSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CHAIN_DETONATION = register(new ChainDetonationSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SIGILS_OF_RUIN = register(new SigilsOfRuinSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DETONATION_WAVE = register(new DetonationWaveSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> IMPLOSION = register(new ImplosionSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> VOLATILE_BRAND = register(new VolatileBrandSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BLAST_LEAP = register(new BlastLeapSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CLUSTER_BARRAGE = register(new ClusterBarrageSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> REACTIVE_SHIELD = register(new ReactiveShieldSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CATACLYSM = register(new CataclysmSpell());

    // Book school (the 10 planned spells, then 3 extra)
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RAZOR_PAGES = register(new RazorPagesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PAGE_STORM = register(new PageStormSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LIVING_INK = register(new LivingInkSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOME_OF_JUDGMENT = register(new TomeOfJudgmentSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BOOKMARK = register(new BookmarkSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SEAL_OF_SILENCE = register(new SealOfSilenceSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FORBIDDEN_LIBRARY = register(new ForbiddenLibrarySpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WORD_OF_POWER = register(new WordOfPowerSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> REWRITE = register(new RewriteSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> THE_FINAL_CHAPTER = register(new FinalChapterSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PAPER_CRANES = register(new PaperCranesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> QUILL_OF_FATE = register(new QuillOfFateSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PARCHMENT_AEGIS = register(new ParchmentAegisSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PAPER_TEMPEST = register(new PaperTempestSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FLYING_STACK = register(new FlyingStackSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ORIGAMI_DRAGON = register(new OrigamiDragonSpell());

    // Errethys school: summoned creatures of mana
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_RATS = register(new ManaRatsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_PIRANHA = register(new ManaPiranhaSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_DROMEDARY = register(new ManaDromedarySpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_SERPENT = register(new ManaSerpentSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_CROWS = register(new ManaCrowsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_FOX = register(new ManaFoxSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_WOLF = register(new ManaWolfSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_BUTTERFLY = register(new ManaButterflySpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_KANGAROO = register(new ManaKangarooSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_FALCON = register(new ManaFalconSpell());

    // Physical Prowess: fighting moves, not magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PUNCH = register(new PunchSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PUNCH_FLURRY = register(new PunchFlurrySkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SLASH = register(new SlashSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SLASH_FLURRY = register(new SlashFlurrySkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DASH = register(new DashSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LEAP = register(new LeapSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SCYTHE_REAP = register(new ScytheReapSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SCYTHE_DANCE = register(new ScytheDanceSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SWORD_RISING_CUT = register(new SwordRisingCutSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SWORD_LUNGE = register(new SwordLungeSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DAGGER_STABS = register(new DaggerStabsSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DAGGER_FLANK = register(new DaggerFlankSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EVADE = register(new EvadeSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUSH = register(new RushSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SCYTHE_HOOK = register(new ScytheHookSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SWORD_DISARM = register(new SwordDisarmSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DAGGER_FINISHER = register(new DaggerFinisherSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HAMMER_CONCUSSION = register(new HammerConcussionSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HAMMER_QUAKE = register(new HammerQuakeSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HAMMER_RAM = register(new HammerRamSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AXE_CLEAVE = register(new AxeCleaveSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AXE_THROW = register(new AxeThrowSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AXE_SUNDER = register(new AxeSunderSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SPEAR_THRUST = register(new SpearThrustSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SPEAR_CHARGE = register(new SpearChargeSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SPEAR_JAVELIN = register(new SpearJavelinSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LONGSWORD_CUT = register(new LongswordCutSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LONGSWORD_SWEEP = register(new LongswordSweepSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LONGSWORD_PARRY = register(new LongswordParrySkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BOW_GUIDED = register(new BowGuidedSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BOW_VOLLEY = register(new BowVolleySkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BOW_RAIN = register(new BowRainSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CROSSBOW_PIERCE = register(new CrossbowPierceSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CROSSBOW_BURST = register(new CrossbowBurstSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CROSSBOW_HARPOON = register(new CrossbowHarpoonSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> KNIFE_THROW = register(new KnifeThrowSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> KNIFE_FAN = register(new KnifeFanSkill());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> KNIFE_PIN = register(new KnifePinSkill());


    // Boss Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CRUSHING_METEOR = register(new CrushingMeteorSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> JUDGMENT_BLADE = register(new JudgmentBladeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ANNIHILATION_BEAM = register(new AnnihilationBeamSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TRIPLE_NOVA = register(new TripleNovaSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PENDULUM = register(new PendulumSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SEISMIC_RIFT = register(new SeismicRiftSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> VOID_ORB = register(new VoidOrbSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CROSS_OF_LIGHT = register(new CrossOfLightSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUNE_PRISON = register(new RunePrisonSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LAST_JUDGMENT = register(new LastJudgmentSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> THOUSAND_BLADES = register(new ThousandBladesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EXECUTIONERS_WHEEL = register(new ExecutionersWheelSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ABYSSAL_MAW = register(new AbyssalMawSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DIVINE_PRISM = register(new DivinePrismSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CHAINS_OF_THE_DAMNED = register(new ChainsOfTheDamnedSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PENTACLE_OF_THORNS = register(new PentacleOfThornsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SERAPH_WINGS = register(new SeraphWingsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BELL_OF_DOOM = register(new BellOfDoomSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> INFERNAL_CHECKERBOARD = register(new InfernalCheckerboardSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TITANS_FIST = register(new TitansFistSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOPPLING_COLUMNS = register(new TopplingColumnsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> REAPERS_SCYTHE = register(new ReapersScytheSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> IRON_MAIDENS = register(new IronMaidensSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SPECTRAL_KINGS = register(new SpectralKingsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CLOCK_OF_FATE = register(new ClockOfFateSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CRIMSON_LOTUS = register(new CrimsonLotusSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COLLAPSE = register(new CollapseSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WRATH_OF_THE_HEAVENS = register(new WrathOfTheHeavensSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ECLIPSE = register(new EclipseSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FALLEN_MOON = register(new FallenMoonSpell());

    // Rune Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WARD_RUNE = register(new WardRuneSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MENDING_RUNESTONE = register(new MendingRunestoneSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EDGE_RUNE = register(new EdgeRuneSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUNIC_PATH = register(new RunicPathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUNIC_BULWARK = register(new RunicBulwarkSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SNARE_RUNE = register(new SnareRuneSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUNE_SHARDS = register(new RuneShardsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PETRIFYING_RUNE = register(new PetrifyingRuneSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MENHIR_SURGE = register(new MenhirSurgeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CIRCLE_OF_ELDERS = register(new CircleOfEldersSpell());

    // Glyph Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FIRE_GLYPH = register(new FireGlyphSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FROST_GLYPH = register(new FrostGlyphSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> STORM_GLYPH = register(new StormGlyphSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TIDE_GLYPH = register(new TideGlyphSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GALE_GLYPH = register(new GaleGlyphSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> STONE_GLYPH = register(new StoneGlyphSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ELEMENTAL_SEAL = register(new ElementalSealSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ELEMENTAL_SCRIPT = register(new ElementalScriptSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GLYPH_RESONANCE = register(new GlyphResonanceSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PRIMORDIAL_GLYPH = register(new PrimordialGlyphSpell());

    // Token Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_THROW = register(new TokenThrowSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_WAVE = register(new TokenWaveSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_STORM = register(new TokenStormSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_RICOCHET = register(new TokenRicochetSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COIN_FLIP = register(new CoinFlipSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_AEGIS = register(new TokenAegisSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_RAIN = register(new TokenRainSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_MINES = register(new TokenMinesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SOUL_TRIBUTE = register(new SoulTributeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOKEN_HAND = register(new TokenHandSpell());

    // Moss, Copper and Rust
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COPPER_SPIKES = register(new CopperSpikesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ANCIENT_PILLAR = register(new AncientPillarSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TOPPLING_WALL = register(new TopplingWallSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUST = register(new RustSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> STRANGLING_VINES = register(new StranglingVinesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MOSS_SPORES = register(new MossSporesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COLONNADE = register(new ColonnadeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ANCIENT_LIGHTNING_ROD = register(new LightningRodSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ANCIENT_BELL = register(new AncientBellSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUNKEN_TEMPLE = register(new SunkenTempleSpell());

    // Wall Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BRICK_BOLT = register(new BrickBoltSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RISING_WALL = register(new RisingWallSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BRICK_RAIN = register(new BrickRainSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WALL_BURST = register(new WallBurstSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MORTAR = register(new MortarSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CRUSHING_WALLS = register(new CrushingWallsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BRICK_CAGE = register(new BrickCageSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RAMPART_CHARGE = register(new RampartChargeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BASTION = register(new BastionSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FALLEN_CITADEL = register(new CitadelSpell());
    // Wall Magic: the great area spells
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BRICK_QUAKE = register(new BrickQuakeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CLOSING_KEEP = register(new ClosingKeepSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GREAT_RAMPART = register(new GreatRampartSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SIEGE = register(new SiegeSpell());

    // Bone Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BONE_LANCE = register(new BoneLanceSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BONE_SPIKES = register(new BoneSpikesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SPINE_WHIP = register(new SpineWhipSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BONE_VOLLEY = register(new BoneVolleySpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BONE_WARD = register(new BoneWardSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RIB_CAGE = register(new RibCageSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> OSSUARY = register(new OssuarySpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SKELETAL_GRASP = register(new SkeletalGraspSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BONE_WYRM = register(new BoneWyrmSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GASHADOKURO = register(new GashadokuroSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COLOSSAL_SPIKE = register(new ColossalSpikeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ELBOW_BLADE = register(new ElbowBladeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HOWLING_SKULL = register(new HowlingSkullSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BONE_NOVA = register(new BoneNovaSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BONE_TEMPEST = register(new BoneTempestSpell());

    // Wood Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LOG_HURL = register(new LogHurlSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GRASPING_ROOTS = register(new GraspingRootsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SEED_PODS = register(new SeedPodsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BRAMBLE_HEDGE = register(new BrambleHedgeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> VINE_LASH = register(new VineLashSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RING_SAW = register(new RingSawSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SPLINTER_STORM = register(new SplinterStormSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AMBER_PRISON = register(new AmberPrisonSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WOOD_GUARDIAN = register(new WoodGuardianSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WORLD_TREE = register(new WorldTreeSpell());

    // Water Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TIDE_LANCE = register(new TideLanceSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PRESSURE_JET = register(new PressureJetSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GEYSER = register(new GeyserSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BUBBLE_PRISON = register(new BubblePrisonSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CLEANSING_RAIN = register(new CleansingRainSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MAELSTROM = register(new MaelstromSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> JELLYFISH_SWARM = register(new JellyfishSwarmSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TYPHOON = register(new TyphoonSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TIDAL_WAVE = register(new TidalWaveSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ABYSSAL_DOME = register(new AbyssalDomeSpell());

    // Stellar Fire: every spell in each of the seven colours, red to violet then green (each list in that order)
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> EMBER_BOLT = heats(EmberBoltSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> STELLAR_BREATH = heats(StellarBreathSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> CORONA = heats(CoronaSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> FIRE_PILLARS = heats(FirePillarsSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> WILL_O_WISPS = heats(WillOWispsSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> FLAME_CRESCENT = heats(FlameCrescentSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> SEARING_BRAND = heats(SearingBrandSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> PHOENIX = heats(PhoenixSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> SUPERNOVA = heats(SupernovaSpell::new);
    public static final List<DeferredHolder<AbstractSpell, AbstractSpell>> STELLAR_PYRE = heats(StellarPyreSpell::new);

    // Wind Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GUST_BLADE = register(new GustBladeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GALE = register(new GaleSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> UPDRAFT = register(new UpdraftSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TAILWIND = register(new TailwindSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CYCLONE = register(new CycloneSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> VACUUM_SPHERE = register(new VacuumSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WIND_WALL = register(new WindWallSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BLADE_STORM = register(new BladeStormSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SKYFALL = register(new SkyfallSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HURRICANE = register(new HurricaneSpell());

    // Echo Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SOUND_WAVE = register(new SoundWaveSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SHRIEK = register(new ShriekSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ECHO_STEP = register(new EchoStepSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SCULK_TENDRILS = register(new SculkTendrilsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SCULK_SENSOR = register(new SculkSensorSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RESONANT_BURST = register(new ResonantBurstSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SONIC_BOOM = register(new SonicBoomSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SILENCE_DOME = register(new SilenceDomeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DEEP_CHOIR = register(new DeepChoirSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> REQUIEM = register(new RequiemSpell());

    // Sand Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SAND_LANCE = register(new SandLanceSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SCARAB_SWARM = register(new ScarabSwarmSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DESERT_GRASP = register(new DesertGraspSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SAND_VEIL = register(new SandVeilSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HOURGLASS = register(new HourglassSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DUNE_SERPENT = register(new DuneSerpentSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUN_LENS = register(new SunLensSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SANDFALL = register(new SandfallSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SAND_DJINN = register(new SandDjinnSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SIMOOM = register(new SimoomSpell());

    // Lotus Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RAZOR_PETALS = register(new RazorPetalsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LOTUS_BUD = register(new LotusBudSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SACRED_POND = register(new SacredPondSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LOTUS_THRONE = register(new LotusThroneSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MIRE_ROOTS = register(new MireRootsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SEED_POD = register(new SeedPodSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LOTUS_MANDALA = register(new LotusMandalaSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LOTUS_LANTERNS = register(new LotusLanternsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PETAL_STORM = register(new PetalStormSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> THOUSAND_PETAL_LOTUS = register(new ThousandPetalLotusSpell());

    // Gem Magic
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUBY_RAY = register(new RubyRaySpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUBY_HEART = register(new RubyHeartSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LAPIS_SEAL = register(new LapisSealSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LAPIS_CONSTELLATION = register(new LapisConstellationSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DIAMOND_PRISM = register(new DiamondPrismSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DIAMOND_SHELL = register(new DiamondShellSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AMETHYST_RESONANCE = register(new AmethystResonanceSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AMETHYST_GEODE = register(new AmethystGeodeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EMERALD_BLOOM = register(new EmeraldBloomSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CROWN_OF_GEMS = register(new CrownOfGemsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUBY_SHARDS = register(new RubyShardsSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUBY_NOVA = register(new RubyNovaSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RUBY_METEOR = register(new RubyMeteorSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SHOOTING_STAR = register(new ShootingStarSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CELESTIAL_VAULT = register(new CelestialVaultSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LAPIS_MIRROR = register(new LapisMirrorSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DIAMOND_LANCE = register(new DiamondLanceSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DIAMOND_DUST = register(new DiamondDustSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DIAMOND_BASTION = register(new DiamondBastionSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AMETHYST_SPIKES = register(new AmethystSpikesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BUDDING_AMETHYST = register(new BuddingAmethystSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AMETHYST_CHIMES = register(new AmethystChimesSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EMERALD_LIFELINE = register(new EmeraldLifelineSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EMERALD_AEGIS = register(new EmeraldAegisSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EMERALD_TREE = register(new EmeraldTreeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EMERALD_THORNS = register(new EmeraldThornsSpell());

    // Cosmos: time
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TIME_LOOP = register(new TimeLoopSpell());

    // Cosmos: space
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CAPTIVE_ORBIT = register(new CaptiveOrbitSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COMET_RIDE = register(new CometRideSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> POCKET_MOON = register(new PocketMoonSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LIVING_CONSTELLATION = register(new LivingConstellationSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ASTEROID_BELT = register(new AsteroidBeltSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ZERO_GRAVITY = register(new ZeroGravitySpell());

    // Cosmos: ultimates
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BLACK_HOLE = register(new BlackHoleSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RESET_CHARGE = register(new ResetChargeSpell());

    private ModSpells() {}

    private static DeferredHolder<AbstractSpell, AbstractSpell> register(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    /** A Stellar Fire spell in each of the seven colours, red to violet then green. */
    private static List<DeferredHolder<AbstractSpell, AbstractSpell>> heats(java.util.function.IntFunction<AbstractSpell> make) {
        List<DeferredHolder<AbstractSpell, AbstractSpell>> out = new java.util.ArrayList<>();
        for (int tier = 0; tier < com.waylays.arcanes.spells.fire.HeatColors.TIERS; tier++) {
            out.add(register(make.apply(tier)));
        }
        return List.copyOf(out);
    }
}
