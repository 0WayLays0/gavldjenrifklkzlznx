package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.clockwork.ClockworkScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.clockwork.ClockworkSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
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
 * Server-side checks run by CI for Clockwork Magic: the school is there with its ten spells; the saw-gear cuts through,
 * a rivet strikes the first in its way, the mainspring flings its rider up, the trap bites and holds, the turret fires,
 * the cage holds and strikes, the automaton walks to its enemy and punches, the locomotive flings what is on its track,
 * the airship's bombs burst, the Great Mechanism's core strikes and then overloads. Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class ClockworkGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private ClockworkGameTests() {}

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

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    private static ClockworkScene scene(GameTestHelper helper, LivingEntity caster, DeferredHolder<AbstractSpell, AbstractSpell> spell, ClockworkScene.Kind kind,
                                     double x, double y, double z, Vector3f dir, float size, int life, float damage) {
        return ClockworkScene.spawn(helper.getLevel(), caster, spell.get(), kind, helper.absoluteVec(new Vec3(x, y, z)), dir, size, life, damage);
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void clockworkSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.CLOCKWORK_RESOURCE);
        helper.assertTrue(school != null, "Clockwork school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof ClockworkSpell, entry.getId() + " is in the Clockwork school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Clockwork spells, found " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.CLOCKWORK_MAGIC),
                "clockwork_magic damage type missing");
        helper.assertTrue(new ItemStack(ModItems.CLOCKMAKERS_CODEX.get()).getItem() instanceof SpellBook, "the Clockmaker's Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.CLOCK).is(ModSchools.CLOCKWORK_FOCUS), "the clock is not the school's focus");
        helper.succeed();
    }

    /** The saw-gear cuts through the enemies in its way, and spares its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_saw", timeoutTicks = 60)
    public static void sawCutsThrough(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk first = husk(helper, 4.5, 3.5);
        Husk second = husk(helper, 4.5, 6.5);
        scene(helper, caster, ModSpells.CLOCKWORK_SAWBLADE, ClockworkScene.Kind.SAW, 4.5, 3, 1.6, new Vector3f(0, 0, 1), .8f, 40, 3);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(first) && hurt(second), "the saw-gear did not cut through both husks");
            helper.assertTrue(!hurt(caster), "the saw-gear cut its caster");
            done(helper);
        });
    }

    /** A rivet strikes the first enemy in its way and stops there. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_rivet", timeoutTicks = 60)
    public static void rivetStrikesTheFirst(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk first = husk(helper, 4.5, 3.5);
        Husk second = husk(helper, 4.5, 6.5);
        ClockworkScene rivet = scene(helper, caster, ModSpells.RIVET_VOLLEY, ClockworkScene.Kind.RIVET, 4.5, 3, 1.6, new Vector3f(0, 0, 1), .3f, 30, 3);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(hurt(first), "the rivet did not strike the husk in its way");
            helper.assertTrue(!hurt(second), "the rivet went through to a second husk");
            helper.assertTrue(rivet.isDone() || rivet.isRemoved(), "the rivet did not stop where it struck");
            done(helper);
        });
    }

    /** The mainspring flings its rider up and knocks back the enemy beside it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_spring", timeoutTicks = 60)
    public static void springFlingsItsRider(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 6, 4.5);
        double y0 = caster.getY();
        scene(helper, caster, ModSpells.MAINSPRING_LEAP, ClockworkScene.Kind.SPRING, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 1, 90, 4).follow(caster);
        helper.runAfterDelay(ClockworkScene.SPRING_LAUNCH + 4, () -> {
            helper.assertTrue(caster.getY() > y0 + .8, "the spring did not fling its rider up");
            helper.assertTrue(hurt(husk), "the spring did not strike the husk beside it");
            helper.assertTrue(!hurt(caster), "the spring hurt its rider");
            done(helper);
        });
    }

    /** The trap bites the enemy that steps in and holds it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_trap", timeoutTicks = 60)
    public static void trapBitesAndHolds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.GEAR_TRAP, ClockworkScene.Kind.TRAP, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 1.2f, 600, 3);
        helper.runAfterDelay(ClockworkScene.TRAP_ARM - 4, () -> helper.assertTrue(!hurt(husk), "the trap bit before it was set"));
        helper.runAfterDelay(ClockworkScene.TRAP_ARM + 3, () -> {
            helper.assertTrue(hurt(husk), "the trap did not bite the husk in it");
            helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the trap does not hold the husk");
            helper.assertTrue(!hurt(caster), "the trap bit its caster");
            done(helper);
        });
    }

    /** The turret fires at the enemy it can see. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_turret", timeoutTicks = 60)
    public static void turretFires(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.CLOCKWORK_TURRET, ClockworkScene.Kind.TURRET, 2.5, 2, 2.5, new Vector3f(0, 0, 1), 1, 200, 3);
        helper.runAfterDelay(ClockworkScene.TURRET_RISE - 4, () -> helper.assertTrue(!hurt(husk), "the turret fired before it stood"));
        helper.runAfterDelay(ClockworkScene.TURRET_RISE + 3, () -> {
            helper.assertTrue(hurt(husk), "the turret did not fire at the husk");
            helper.assertTrue(!hurt(caster), "the turret fired at its caster");
            done(helper);
        });
    }

    /** The cage holds and strikes the enemy in it, then bursts on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_gyro", timeoutTicks = 120)
    public static void cageHoldsAndStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 5, 4.5);
        scene(helper, caster, ModSpells.GYRO_CAGE, ClockworkScene.Kind.GYRO, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 1.5f, 60, 2);
        helper.runAfterDelay(ClockworkScene.GYRO_CLOSE + 3, () -> {
            helper.assertTrue(hurt(husk), "the cage did not strike the husk in it");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the cage does not hold the husk");
        });
        helper.runAfterDelay(60 - ClockworkScene.BURST - 1, () -> {
            float before = husk.getHealth();
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(!husk.isAlive() || husk.getHealth() < before, "the cage did not burst on the husk");
                helper.assertTrue(!hurt(caster), "the cage hurt its caster");
                done(helper);
            });
        });
    }

    /** The automaton walks to the enemy and punches it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_automaton", timeoutTicks = 140)
    public static void automatonWalksAndPunches(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 2.5, 7.5);
        scene(helper, caster, ModSpells.BRASS_AUTOMATON, ClockworkScene.Kind.AUTOMATON, 2.5, 2, 1.5, new Vector3f(0, 0, 1), 1, 400, 4);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the automaton has not punched the husk yet");
            helper.assertTrue(!hurt(caster), "the automaton hurt its master");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The locomotive flings the enemy on its track, and passes the one beside it by. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_locomotive", timeoutTicks = 80)
    public static void locomotiveFlingsWhatIsOnTheTrack(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 7.5, .5);
        Husk onTrack = husk(helper, 1.5, 6.5);
        Husk beside = husk(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.BRASS_LOCOMOTIVE, ClockworkScene.Kind.LOCOMOTIVE, 1.5, 2, .5, new Vector3f(0, 0, 1), 1,
                ClockworkScene.LOCO_RAILS + ClockworkScene.LOCO_RUN + 22, 3);
        helper.runAfterDelay(ClockworkScene.LOCO_RAILS - 2, () -> helper.assertTrue(!hurt(onTrack), "the locomotive struck before its rails were laid"));
        helper.runAfterDelay(ClockworkScene.LOCO_RAILS + ClockworkScene.LOCO_RUN + 2, () -> {
            helper.assertTrue(hurt(onTrack), "the locomotive did not strike the husk on its track");
            helper.assertTrue(!hurt(beside), "the locomotive struck a husk beside its track");
            helper.assertTrue(!hurt(caster), "the locomotive struck its caster");
            done(helper);
        });
    }

    /** The airship's bombs burst on the ground below it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_airship", timeoutTicks = 100)
    public static void airshipBombsBurst(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.AIRSHIP_BOMBARDMENT, ClockworkScene.Kind.AIRSHIP, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 1, 110, 3);
        helper.runAfterDelay(ClockworkScene.bombDrop(0) + ClockworkScene.SHIP_FALL - 2, () -> helper.assertTrue(!hurt(husk), "a bomb burst before it landed"));
        helper.runAfterDelay(ClockworkScene.bombDrop(4) + ClockworkScene.SHIP_FALL + 2, () -> {
            helper.assertTrue(hurt(husk), "the bombs did not strike the husk under the airship's way");
            helper.assertTrue(!hurt(caster), "a bomb struck the caster");
            done(helper);
        });
    }

    /** The Great Mechanism's core strikes the enemy and hardens the caster; then it overloads, and shields the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "clockwork_mechanism",
            timeoutTicks = ClockworkScene.MECH_OVERLOAD + 40)
    public static void mechanismStrikesThenOverloads(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 7.5, 4.5);
        scene(helper, caster, ModSpells.GREAT_MECHANISM, ClockworkScene.Kind.MECHANISM, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 8, ClockworkScene.MECH_OVERLOAD + 30, 4);
        helper.runAfterDelay(ClockworkScene.MECH_RISE - 4, () -> helper.assertTrue(!hurt(husk), "the mechanism struck before it rose"));
        helper.runAfterDelay(ClockworkScene.MECH_RISE + 12, () -> {
            helper.assertTrue(hurt(husk), "the core's arc did not strike the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.GLOWING), "the husk struck by the core is not marked");
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the mechanism did not harden its caster");
        });
        helper.runAfterDelay(ClockworkScene.MECH_OVERLOAD + 2, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.ABSORPTION), "the overload did not shield the caster");
            helper.assertTrue(!hurt(caster), "the mechanism hurt its caster");
            done(helper);
        });
    }
}
