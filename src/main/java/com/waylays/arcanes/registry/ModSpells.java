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
import com.waylays.arcanes.spells.errethys.ManaCrowsSpell;
import com.waylays.arcanes.spells.errethys.ManaDromedarySpell;
import com.waylays.arcanes.spells.errethys.ManaRatsSpell;
import com.waylays.arcanes.spells.errethys.ManaSalmonSpell;
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
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_SALMON = register(new ManaSalmonSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_DROMEDARY = register(new ManaDromedarySpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_SERPENT = register(new ManaSerpentSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_CROWS = register(new ManaCrowsSpell());

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

    private ModSpells() {}

    private static DeferredHolder<AbstractSpell, AbstractSpell> register(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }
}
