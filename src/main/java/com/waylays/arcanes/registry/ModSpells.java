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
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Spells added to Iron's spell registry. Every spell class also needs {@code @AutoSpellConfig} and a no-arg
 * constructor, a spell icon at textures/gui/spell_icons/{@code <spell>}.png and its lang keys.
 */
public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ArcanesOublies.MODID);

    // Gold school
    public static final RegistryObject<AbstractSpell> GOLD_BOLT = register(new GoldBoltSpell());
    public static final RegistryObject<AbstractSpell> GILDED_CHAINS = register(new GildedChainsSpell());
    public static final RegistryObject<AbstractSpell> GOLDFALL = register(new GoldfallSpell());
    public static final RegistryObject<AbstractSpell> GOLDEN_SPIKES = register(new GoldenSpikesSpell());
    public static final RegistryObject<AbstractSpell> MIDAS_CURSE = register(new MidasCurseSpell());
    public static final RegistryObject<AbstractSpell> AURUM_BLADE = register(new AurumBladeSpell());
    public static final RegistryObject<AbstractSpell> MOLTEN_TORRENT = register(new MoltenTorrentSpell());
    public static final RegistryObject<AbstractSpell> GILDED_ARMOR = register(new GildedArmorSpell());
    public static final RegistryObject<AbstractSpell> GOLDEN_SENTINELS = register(new GoldenSentinelsSpell());
    public static final RegistryObject<AbstractSpell> SUN_OF_ELDORADO = register(new SunOfEldoradoSpell());

    // Explosion school
    public static final RegistryObject<AbstractSpell> BLAST_ORB = register(new BlastOrbSpell());
    public static final RegistryObject<AbstractSpell> CHAIN_DETONATION = register(new ChainDetonationSpell());
    public static final RegistryObject<AbstractSpell> SIGILS_OF_RUIN = register(new SigilsOfRuinSpell());
    public static final RegistryObject<AbstractSpell> DETONATION_WAVE = register(new DetonationWaveSpell());
    public static final RegistryObject<AbstractSpell> IMPLOSION = register(new ImplosionSpell());
    public static final RegistryObject<AbstractSpell> VOLATILE_BRAND = register(new VolatileBrandSpell());
    public static final RegistryObject<AbstractSpell> BLAST_LEAP = register(new BlastLeapSpell());
    public static final RegistryObject<AbstractSpell> CLUSTER_BARRAGE = register(new ClusterBarrageSpell());
    public static final RegistryObject<AbstractSpell> REACTIVE_SHIELD = register(new ReactiveShieldSpell());
    public static final RegistryObject<AbstractSpell> CATACLYSM = register(new CataclysmSpell());

    // Book school (the 10 planned spells, then 3 extra)
    public static final RegistryObject<AbstractSpell> RAZOR_PAGES = register(new RazorPagesSpell());
    public static final RegistryObject<AbstractSpell> PAGE_STORM = register(new PageStormSpell());
    public static final RegistryObject<AbstractSpell> LIVING_INK = register(new LivingInkSpell());
    public static final RegistryObject<AbstractSpell> TOME_OF_JUDGMENT = register(new TomeOfJudgmentSpell());
    public static final RegistryObject<AbstractSpell> BOOKMARK = register(new BookmarkSpell());
    public static final RegistryObject<AbstractSpell> SEAL_OF_SILENCE = register(new SealOfSilenceSpell());
    public static final RegistryObject<AbstractSpell> FORBIDDEN_LIBRARY = register(new ForbiddenLibrarySpell());
    public static final RegistryObject<AbstractSpell> WORD_OF_POWER = register(new WordOfPowerSpell());
    public static final RegistryObject<AbstractSpell> REWRITE = register(new RewriteSpell());
    public static final RegistryObject<AbstractSpell> THE_FINAL_CHAPTER = register(new FinalChapterSpell());
    public static final RegistryObject<AbstractSpell> PAPER_CRANES = register(new PaperCranesSpell());
    public static final RegistryObject<AbstractSpell> QUILL_OF_FATE = register(new QuillOfFateSpell());
    public static final RegistryObject<AbstractSpell> PARCHMENT_AEGIS = register(new ParchmentAegisSpell());
    public static final RegistryObject<AbstractSpell> PAPER_TEMPEST = register(new PaperTempestSpell());
    public static final RegistryObject<AbstractSpell> FLYING_STACK = register(new FlyingStackSpell());
    public static final RegistryObject<AbstractSpell> ORIGAMI_DRAGON = register(new OrigamiDragonSpell());

    // Errethys school: summoned creatures of mana
    public static final RegistryObject<AbstractSpell> MANA_RATS = register(new ManaRatsSpell());
    public static final RegistryObject<AbstractSpell> MANA_SALMON = register(new ManaSalmonSpell());
    public static final RegistryObject<AbstractSpell> MANA_DROMEDARY = register(new ManaDromedarySpell());
    public static final RegistryObject<AbstractSpell> MANA_SERPENT = register(new ManaSerpentSpell());
    public static final RegistryObject<AbstractSpell> MANA_CROWS = register(new ManaCrowsSpell());

    private ModSpells() {}

    private static RegistryObject<AbstractSpell> register(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }
}
