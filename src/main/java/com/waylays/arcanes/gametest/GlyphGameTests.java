package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.glyph.GlyphMarkVisual;
import com.waylays.arcanes.entity.glyph.GlyphSeal;
import com.waylays.arcanes.entity.glyph.GlyphSign;
import com.waylays.arcanes.entity.glyph.GlyphStorm;
import com.waylays.arcanes.entity.glyph.GlyphZone;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphReaction;
import com.waylays.arcanes.spells.glyph.GlyphReactions;
import com.waylays.arcanes.spells.glyph.GlyphSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Glyph Magic: the school is there; a glyph is written before it goes off; its hit
 * inscribes its element's mark; another element striking the mark sets off their reaction (instant ones, freezing,
 * lasting zones); the wave, the lightning, the script, the seal, the resonance and the Primordial Glyph hurt enemies.
 * Glyph spells reach far, so each test that casts one runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class GlyphGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private GlyphGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return !e.isAlive() || e.getHealth() < e.getMaxHealth();
    }

    /** A witch facing +Z, standing still: the caster. */
    private static Witch caster(GameTestHelper helper, double x, double z) {
        Witch witch = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(x, 2, z));
        witch.setYRot(0);
        witch.setYHeadRot(0);
        witch.setYBodyRot(0);
        witch.setXRot(0);
        return witch;
    }

    /** A husk standing still: tall enough for bolts flying at a witch's chest height to meet it. */
    private static Husk husk(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, 2, z));
    }

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    private static boolean marked(LivingEntity e, GlyphElement element) {
        return e.hasEffect(GlyphReactions.mark(element));
    }

    private static void inscribe(LivingEntity e, GlyphElement element) {
        e.addEffect(new MobEffectInstance(GlyphReactions.mark(element), GlyphReactions.MARK_TICKS));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void glyphSchoolIsRegistered(GameTestHelper helper) {
        SchoolType glyph = SchoolRegistry.getSchool(ModSchools.GLYPH_RESOURCE);
        helper.assertTrue(glyph != null, "Glyph school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == glyph) {
                helper.assertTrue(spell instanceof GlyphSpell, entry.getId() + " is in the Glyph school but is not a glyph spell");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Glyph spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.SIX_INKS_CODEX.get()).getItem() instanceof SpellBook, "the Codex of the Six Inks is not a spellbook");
        helper.assertTrue(ModEffects.GLYPH_MARKS.size() == GlyphElement.values().length, "one mark effect per element expected");
        for (GlyphElement x : GlyphElement.values()) {
            for (GlyphElement y : GlyphElement.values()) {
                if (x != y) {
                    GlyphReaction r = GlyphReaction.of(x, y);
                    helper.assertTrue(r == GlyphReaction.of(y, x), "the reaction of " + x + " and " + y + " depends on their order");
                }
            }
        }
        helper.succeed();
    }

    /** The glyph is written first; once written, its comets fly, hurt, burn and leave the fire mark (shown over the head). */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_fire", timeoutTicks = 80)
    public static void fireGlyphWritesThenMarks(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.FIRE_GLYPH, caster);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(GlyphSign.class, caster.getBoundingBox().inflate(3)).isEmpty(), "no glyph written");
        helper.assertTrue(!hurt(husk), "the glyph went off before it was written");
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the husk was not hit");
            helper.assertTrue(marked(husk, GlyphElement.FIRE), "the husk does not bear the fire mark");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(GlyphMarkVisual.class, husk.getBoundingBox().inflate(3)).isEmpty(), "no mark over its head");
        });
    }

    /** Fire on a frost mark: Melt, which hits harder than the glyph alone, and wipes the mark. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_reactions")
    public static void meltHitsHarderAndWipesTheMark(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Husk plain = husk(helper, 2.5, 6.5);
        Husk frosted = husk(helper, 6.5, 6.5);
        inscribe(frosted, GlyphElement.FROST);
        AbstractSpell spell = ModSpells.FIRE_GLYPH.get();
        GlyphReaction none = GlyphReactions.strike(helper.getLevel(), caster, caster, plain, GlyphElement.FIRE, 4, spell);
        GlyphReaction melt = GlyphReactions.strike(helper.getLevel(), caster, caster, frosted, GlyphElement.FIRE, 4, spell);
        helper.assertTrue(none == null, "a reaction went off on an unmarked husk");
        helper.assertTrue(melt == GlyphReaction.MELT, "fire on frost gave " + melt);
        helper.assertTrue(!marked(frosted, GlyphElement.FROST) && !marked(frosted, GlyphElement.FIRE), "the marks were not wiped by the reaction");
        helper.assertTrue(marked(plain, GlyphElement.FIRE), "the unmarked husk was not marked");
        helper.assertTrue(frosted.getHealth() < plain.getHealth(), "Melt did not hit harder (" + frosted.getHealth() + " vs " + plain.getHealth() + ")");
        helper.succeed();
    }

    /** Frost on a tide mark: frozen solid. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_reactions")
    public static void freezeLocksInIce(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        inscribe(husk, GlyphElement.TIDE);
        GlyphReaction r = GlyphReactions.strike(helper.getLevel(), caster, caster, husk, GlyphElement.FROST, 2, ModSpells.FROST_GLYPH.get());
        helper.assertTrue(r == GlyphReaction.FREEZE, "frost on tide gave " + r);
        helper.assertTrue(husk.hasEffect(ModEffects.GLYPH_FROZEN), "the husk is not frozen");
        helper.succeed();
    }

    /** Tide on a fire mark: Steam, a lasting zone that keeps hurting whoever stands in it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_zone", timeoutTicks = 60)
    public static void steamOpensALastingZone(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        inscribe(husk, GlyphElement.FIRE);
        GlyphReaction r = GlyphReactions.strike(helper.getLevel(), caster, caster, husk, GlyphElement.TIDE, 1, ModSpells.TIDE_GLYPH.get());
        helper.assertTrue(r == GlyphReaction.STEAM, "tide on fire gave " + r);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(GlyphZone.class, husk.getBoundingBox().inflate(3)).isEmpty(), "no steam zone opened");
        float after = husk.getHealth();
        helper.succeedWhen(() -> helper.assertTrue(husk.getHealth() < after, "the steam does not hurt the husk standing in it"));
    }

    /** The tide's wave rolls over the enemy in front: hurt and marked. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_tide", timeoutTicks = 60)
    public static void tideGlyphWaveStrikes(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.TIDE_GLYPH, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the wave did not hit the husk");
            helper.assertTrue(marked(husk, GlyphElement.TIDE), "the husk does not bear the tide mark");
        });
    }

    /** The lightning strikes the enemy in sight, then leaps to the ones beside it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_storm", timeoutTicks = 60)
    public static void stormGlyphLeapsFromEnemyToEnemy(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 0.5);
        caster.setXRot(8);
        Husk first = husk(helper, 4.5, 5.5);
        Husk second = husk(helper, 2.5, 7.5);
        Husk third = husk(helper, 6.5, 7.5);
        cast(helper, ModSpells.STORM_GLYPH, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(first), "the husk in sight was not struck");
            helper.assertTrue(hurt(second) && hurt(third), "the lightning did not leap to the husks beside it");
        });
    }

    /** The script's glyphs erupt one after another along the ground. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_script", timeoutTicks = 80)
    public static void elementalScriptErupts(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 0.5);
        Husk near = husk(helper, 4.5, 3.0);
        Husk far = husk(helper, 4.5, 5.2);
        cast(helper, ModSpells.ELEMENTAL_SCRIPT, caster);
        helper.succeedWhen(() -> helper.assertTrue(hurt(near) && hurt(far), "the glyphs on the ground did not erupt under both husks"));
    }

    /** Resonance sets off a mark: the bearer is hurt and the mark is spent. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_resonance")
    public static void resonanceDetonatesMarks(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk marked = husk(helper, 4.5, 6.5);
        Husk bare = husk(helper, 1.5, 7.5);
        inscribe(marked, GlyphElement.STONE);
        cast(helper, ModSpells.GLYPH_RESONANCE, caster);
        helper.assertTrue(hurt(marked), "the marked husk was not hurt");
        helper.assertTrue(!marked(marked, GlyphElement.STONE), "the mark was not spent");
        helper.assertTrue(!hurt(bare), "a husk with no mark and far from the burst was hurt");
        helper.succeed();
    }

    /** The seal's glyphs loose their bolts at an enemy that comes near. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_seal", timeoutTicks = 100)
    public static void elementalSealFiresAtEnemies(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.ELEMENTAL_SEAL, caster);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(GlyphSeal.class, caster.getBoundingBox().inflate(3)).isEmpty(), "no seal raised");
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the seal never struck the husk"));
    }

    /** The great glyph is written in the sky, then its elements rain down on the enemies below. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "glyph_primordial", timeoutTicks = 200)
    public static void primordialGlyphRainsElements(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.PRIMORDIAL_GLYPH, caster);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(GlyphStorm.class, caster.getBoundingBox().inflate(3)).isEmpty(), "no glyph written in the sky");
        helper.runAfterDelay(GlyphStorm.WRITE_TICKS - 2, () -> helper.assertTrue(!hurt(husk), "the sky glyph struck before it was written"));
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "no element fell on the husk"));
    }
}
