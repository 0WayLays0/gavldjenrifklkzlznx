package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.melody.MelodyScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.melody.MelodySpell;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.joml.Vector3f;

/**
 * Server-side checks run by CI for Music Magic: the school is there with its ten spells; a note bends to its enemy and
 * strikes it, the staff slash strikes before its caster and not behind, the soothing melody heals, the crescendo
 * strikes down its way, the lullaby puts an enemy to sleep and a blow wakes it, the war drum strikes and rallies, the
 * harp's rays strike, the trumpets blast before their caster and not behind, the storm organ calls a pillar down and
 * sounds its last chord, the orchestra's notes fall and its finale shields the caster. Each test runs in a batch of its
 * own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class MelodyGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private MelodyGameTests() {}

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

    private static Husk husk(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, 2, z));
    }

    private static MelodyScene scene(GameTestHelper helper, LivingEntity caster, DeferredHolder<AbstractSpell, AbstractSpell> spell, MelodyScene.Kind kind,
                                     double x, double y, double z, Vector3f dir, float size, int life, float damage) {
        return MelodyScene.spawn(helper.getLevel(), caster, spell.get(), kind, helper.absoluteVec(new Vec3(x, y, z)), dir, size, life, damage);
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void melodySchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.MELODY_RESOURCE);
        helper.assertTrue(school != null, "Music school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof MelodySpell, entry.getId() + " is in the Music school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Music spells, found " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.MELODY_MAGIC),
                "melody_magic damage type missing");
        helper.assertTrue(new ItemStack(ModItems.SYMPHONY_CODEX.get()).getItem() instanceof SpellBook, "the Symphony Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.NOTE_BLOCK).is(ModSchools.MELODY_FOCUS), "the note block is not the school's focus");
        helper.succeed();
    }

    /** A note bends toward an enemy a little off its way and strikes it, and spares its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_note", timeoutTicks = 60)
    public static void noteBendsAndStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 6, 6.5);
        MelodyScene note = scene(helper, caster, ModSpells.NOTE_VOLLEY, MelodyScene.Kind.NOTE, 4.5, 3, 1.6, new Vector3f(0, 0, 1), .6f, 40, 3);
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(hurt(husk), "the note did not bend to strike the husk off its way");
            helper.assertTrue(!hurt(caster), "the note struck its caster");
            helper.assertTrue(note.isDone() || note.isRemoved(), "the note did not stop where it struck");
            done(helper);
        });
    }

    /** The staff slash strikes the enemy before its caster, not the one behind. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_slash", timeoutTicks = 40)
    public static void staffSlashStrikesAhead(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 3);
        Husk ahead = husk(helper, 4.5, 6);
        Husk behind = husk(helper, 4.5, .8);
        scene(helper, caster, ModSpells.STAFF_SLASH, MelodyScene.Kind.STAFF, 4.5, 2, 3, new Vector3f(0, 0, 1), MelodyScene.SLASH_REACH, 16, 3);
        helper.runAfterDelay(MelodyScene.SLASH_HIT + 3, () -> {
            helper.assertTrue(hurt(ahead), "the staff slash did not strike the husk before its caster");
            helper.assertTrue(!hurt(behind), "the staff slash struck the husk behind its caster");
            helper.assertTrue(!hurt(caster), "the staff slash struck its caster");
            done(helper);
        });
    }

    /** The soothing melody heals its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_soothe", timeoutTicks = 60)
    public static void soothingMelodyHeals(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        scene(helper, caster, ModSpells.SOOTHING_MELODY, MelodyScene.Kind.SOOTHE, 4.5, 2, 4.5, new Vector3f(0, 0, 1), (float) MelodyScene.SOOTHE_RADIUS, 100, 4)
                .follow(caster);
        helper.runAfterDelay(MelodyScene.SOOTHE_EVERY + 3, () -> {
            helper.assertTrue(caster.getHealth() > 12, "the soothing melody did not heal its caster (" + caster.getHealth() + ")");
            done(helper);
        });
    }

    /** The crescendo's rings strike the enemy down their way. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_crescendo", timeoutTicks = 60)
    public static void crescendoStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 7.5);
        scene(helper, caster, ModSpells.CRESCENDO, MelodyScene.Kind.CRESCENDO, 4.5, 3, 1.6, new Vector3f(0, 0, 1), MelodyScene.CRESC_TO, 40, 3);
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(hurt(husk), "the crescendo did not strike the husk down its way");
            helper.assertTrue(!hurt(caster), "the crescendo struck its caster");
            done(helper);
        });
    }

    /** The lullaby puts an enemy to sleep, and a blow wakes it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_lullaby", timeoutTicks = 80)
    public static void lullabySleepsUntilStruck(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        MelodyScene song = scene(helper, caster, ModSpells.LULLABY, MelodyScene.Kind.LULLABY, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 5, 120, 0);
        helper.runAfterDelay(MelodyScene.LULL_RISE + 3, () -> {
            helper.assertTrue(song.sleepers() == 1, "the lullaby did not put the husk to sleep");
            helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the sleeping husk can still walk");
            helper.assertTrue(!hurt(husk), "the lullaby hurt the husk");
            husk.hurt(helper.getLevel().damageSources().generic(), 2);
        });
        helper.runAfterDelay(MelodyScene.LULL_RISE + 8, () -> {
            helper.assertTrue(song.sleepers() == 0, "the blow did not wake the husk");
            helper.assertTrue(!husk.hasEffect(ModEffects.ENTANGLED), "the woken husk still cannot walk");
            done(helper);
        });
    }

    /** The war drum's beat strikes the enemy near it and rallies the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_drum", timeoutTicks = 60)
    public static void warDrumStrikesAndRallies(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 4.5);
        Husk husk = husk(helper, 7.5, 4.5);
        scene(helper, caster, ModSpells.WAR_DRUM, MelodyScene.Kind.DRUM, 4.5, 2, 4.5, new Vector3f(0, 0, 1), (float) MelodyScene.DRUM_REACH, 120, 3);
        helper.runAfterDelay(MelodyScene.DRUM_FIRST + 3, () -> {
            helper.assertTrue(hurt(husk), "the drum's beat did not strike the husk");
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_BOOST), "the drum's beat did not rally its caster");
            helper.assertTrue(!hurt(caster), "the drum's beat struck its caster");
            done(helper);
        });
    }

    /** The harp's first note flies as a ray at the enemy. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_harp", timeoutTicks = 60)
    public static void harpRayStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 7.5);
        MelodyScene harp = scene(helper, caster, ModSpells.HARP_OF_LIGHT, MelodyScene.Kind.HARP, 4.5, 2, 1.5, new Vector3f(0, 0, 1), 1, 160, 3).follow(caster);
        helper.runAfterDelay(MelodyScene.HARP_RISE + 3, () -> {
            helper.assertTrue(hurt(husk), "the harp's ray did not strike the husk");
            helper.assertTrue(harp.strikes() >= 1, "the harp did not count its strike");
            done(helper);
        });
    }

    /** The trumpets' blast strikes the enemy before their caster, not the one behind. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_trumpets", timeoutTicks = 60)
    public static void trumpetsBlastAhead(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 3);
        Husk ahead = husk(helper, 4.5, 8);
        Husk behind = husk(helper, 4.5, .6);
        scene(helper, caster, ModSpells.PHANTOM_TRUMPETS, MelodyScene.Kind.TRUMPETS, 4.5, 2, 3, new Vector3f(0, 0, 1), (float) MelodyScene.TRUMP_REACH, 86, 3)
                .follow(caster);
        helper.runAfterDelay(MelodyScene.TRUMP_FIRST + 3, () -> {
            helper.assertTrue(hurt(ahead), "the trumpets' blast did not strike the husk before their caster");
            helper.assertTrue(!hurt(behind), "the trumpets' blast struck the husk behind their caster");
            done(helper);
        });
    }

    /** The storm organ calls a pillar down on the enemy, then sounds its last chord. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_organ", timeoutTicks = 220)
    public static void stormOrganPillarAndFinale(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        MelodyScene organ = scene(helper, caster, ModSpells.STORM_ORGAN, MelodyScene.Kind.ORGAN, 4.5, 2, 4.5, new Vector3f(0, 0, -1), 12, 190, 1);
        helper.runAfterDelay(MelodyScene.ORGAN_RISE + MelodyScene.ORGAN_FALL + 3, () -> {
            helper.assertTrue(hurt(husk), "no pillar of sound fell on the husk");
            helper.assertTrue(!hurt(caster), "a pillar of sound struck the caster");
        });
        helper.runAfterDelay(MelodyScene.ORGAN_FINALE + 3, () -> {
            helper.assertTrue(organ.isDone(), "the organ did not sound its last chord");
            done(helper);
        });
    }

    /** The orchestra's notes fall on the enemy and its caster is healed; its finale shields the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "melody_orchestra", timeoutTicks = 260)
    public static void orchestraPlaysItsFinale(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 7.5, 4.5);
        MelodyScene orchestra = scene(helper, caster, ModSpells.PHANTOM_ORCHESTRA, MelodyScene.Kind.ORCHESTRA, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 14, 260, 2);
        helper.runAfterDelay(MelodyScene.ORCH_RISE + MelodyScene.ORCH_FALL + 3, () -> {
            helper.assertTrue(hurt(husk), "no note fell on the husk");
            helper.assertTrue(caster.hasEffect(MobEffects.REGENERATION), "the orchestra did not heal its caster");
        });
        helper.runAfterDelay(MelodyScene.ORCH_FINALE + 3, () -> {
            helper.assertTrue(orchestra.isDone(), "the orchestra did not play its finale");
            helper.assertTrue(caster.hasEffect(MobEffects.ABSORPTION), "the finale did not shield the caster");
            done(helper);
        });
    }
}
