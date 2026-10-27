package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.cosmos.AsteroidBelt;
import com.waylays.arcanes.entity.cosmos.BlackHole;
import com.waylays.arcanes.entity.cosmos.CaptivePlanet;
import com.waylays.arcanes.entity.cosmos.PocketMoon;
import com.waylays.arcanes.entity.cosmos.ResetCharge;
import com.waylays.arcanes.entity.cosmos.RiddenComet;
import com.waylays.arcanes.entity.cosmos.StarLion;
import com.waylays.arcanes.entity.cosmos.TimeLoop;
import com.waylays.arcanes.entity.cosmos.ZeroGravityField;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.cosmos.CosmosSpell;
import com.waylays.arcanes.spells.cosmos.LivingConstellationSpell;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

/**
 * Server-side checks run by CI for Cosmos: the school is there, the pruning line grows with the level, the time loop
 * snaps its prisoner back. Space: the captive planet swings its prisoner round, the comet carries its rider and
 * crashes, the pocket moon strikes in orbit, the asteroid belt crushes what it closes on, zero gravity lifts foes and
 * slams them down, the Lion answers its summoner. The ultimates: the black hole draws in and bursts, the reset charge
 * restores allies and sends foes back.
 * Each test runs in a batch of its own and takes down what it raised.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class CosmosGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private CosmosGameTests() {}

    /** A villager facing +Z, standing still: the caster. */
    private static Villager caster(GameTestHelper helper, double x, double z) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(x, 2, z));
        villager.setYRot(0);
        villager.setYHeadRot(0);
        villager.setYBodyRot(0);
        villager.setXRot(0);
        return villager;
    }

    private static Vindicator foe(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(x, 2, z));
    }

    private static boolean hurt(LivingEntity e) {
        return !e.isAlive() || e.getHealth() < e.getMaxHealth();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void cosmosSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.COSMOS_RESOURCE);
        helper.assertTrue(school != null, "Cosmos school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof CosmosSpell || spell instanceof LivingConstellationSpell,
                        entry.getId() + " is in the Cosmos school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 9, "expected 9 Cosmos spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.CODEX_OUT_OF_TIME.get()).getItem() instanceof SpellBook, "the Codex Out of Time is not a spellbook");
        helper.assertTrue(new ItemStack(Items.CLOCK).is(ModSchools.COSMOS_FOCUS), "clocks are not the school's focus");
        helper.succeed();
    }

    /** The pruning line: 10% of greatest health plus 2% a level, at most 30%, never under 3. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_prune")
    public static void pruningLineGrowsWithTheLevel(GameTestHelper helper) {
        helper.assertTrue(Math.abs(Cosmos.pruneLine(10, 20) - 6) < 1.0E-3, "the pruning line at level 10 is not 30%");
        helper.assertTrue(Math.abs(Cosmos.pruneLine(5, 100) - 20) < 1.0E-3, "the pruning line at level 5 is not 20%");
        helper.assertTrue(Cosmos.pruneLine(1, 10) >= 3, "the pruning line is below 3 health");
        helper.succeed();
    }

    /** The loop snaps its prisoner back where it stood when it closed, and stings it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_loop", timeoutTicks = TimeLoop.PERIOD + 60)
    public static void timeLoopSnapsBack(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        Vec3 anchor = foe.position();
        TimeLoop.close(helper.getLevel(), caster, foe, ModSpells.TIME_LOOP.get(), 1, 2);
        helper.runAfterDelay(10, () -> {
            Vec3 away = helper.absoluteVec(new Vec3(7.5, 2, 7.5));
            foe.teleportTo(helper.getLevel(), away.x, away.y, away.z, Set.of(), 0, 0);
        });
        helper.runAfterDelay(TimeLoop.PERIOD + 5, () -> {
            helper.assertTrue(foe.position().distanceTo(anchor) < 1, "the loop did not snap the vindicator back");
            helper.assertTrue(hurt(foe), "the loop did not sting");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The planet sweeps its prisoner off the ground into orbit and strikes it as it goes round. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_orbit", timeoutTicks = 140)
    public static void captivePlanetSwingsItsPrisoner(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        Vec3 start = foe.position();
        CaptivePlanet planet = CaptivePlanet.capture(helper.getLevel(), caster, foe, ModSpells.CAPTIVE_ORBIT.get(), 120, 4);
        helper.assertTrue(planet != null, "no planet came down");
        helper.succeedWhen(() -> {
            helper.assertTrue(foe.position().distanceTo(start) > 1, "the vindicator was not swept off into orbit");
            helper.assertTrue(hurt(foe), "the orbit did not strike");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The comet takes its rider up and, told to dive, crashes and throws the rider clear. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_comet", timeoutTicks = 120)
    public static void cometCarriesAndCrashes(GameTestHelper helper) {
        Villager rider = caster(helper, 4.5, 1.5);
        // In its path, just ahead
        Vindicator foe = foe(helper, 4.5, 3.5);
        RiddenComet comet = RiddenComet.call(helper.getLevel(), rider, 100, .8f, 4);
        helper.assertTrue(rider.getVehicle() == comet, "the caster is not riding the comet");
        helper.runAfterDelay(3, comet::dive);
        helper.succeedWhen(() -> {
            helper.assertTrue(!comet.isAlive(), "the comet did not crash");
            helper.assertTrue(rider.getVehicle() == null, "the rider was not thrown clear");
            helper.assertTrue(hurt(foe), "the crash did not strike the vindicator");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The moon strikes what is on its orbit. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_moon", timeoutTicks = 100)
    public static void pocketMoonStrikesInOrbit(GameTestHelper helper) {
        Villager caster = caster(helper, 2.5, 4.5);
        Vindicator foe = foe(helper, 2.5 + PocketMoon.ORBIT, 4.5);
        PocketMoon.raise(helper.getLevel(), caster, ModSpells.POCKET_MOON.get(), 200, 4);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(foe), "the moon did not strike the vindicator on its orbit");
            helper.assertTrue(!hurt(caster), "the moon struck its caster");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** Closing, the belt crushes what it encircles and spares the caster's side. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_belt", timeoutTicks = 100)
    public static void asteroidBeltCrushesWhatItCloses(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        AsteroidBelt belt = AsteroidBelt.raise(helper.getLevel(), caster, ModSpells.ASTEROID_BELT.get(), helper.absoluteVec(new Vec3(4.5, 2, 4.5)), 16, 200, 4);
        helper.runAfterDelay(20, belt::crush);
        helper.runAfterDelay(20 + AsteroidBelt.CRUSH_TICKS + 4, () -> {
            helper.assertTrue(hurt(foe), "the belt did not crush the vindicator");
            helper.assertTrue(!hurt(caster), "the belt hurt its caster");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** Foes float up in zero gravity (the caster gets a moon's gravity) and are slammed down when it ends. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_gravity", timeoutTicks = 140)
    public static void zeroGravityLiftsAndSlams(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        double ground = foe.getY();
        ZeroGravityField field = ZeroGravityField.open(helper.getLevel(), caster, ModSpells.ZERO_GRAVITY.get(), 8, 200, 4);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(foe.hasEffect(ModEffects.WEIGHTLESS), "the vindicator is not weightless");
            helper.assertTrue(caster.hasEffect(ModEffects.LOW_GRAVITY), "the caster does not have a moon's gravity");
            helper.assertTrue(foe.getY() > ground + .8, "the vindicator did not float up (" + (foe.getY() - ground) + ")");
            field.slam();
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(hurt(foe), "the vindicator was not slammed down");
            helper.assertTrue(!foe.hasEffect(ModEffects.WEIGHTLESS), "the vindicator is still weightless");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The Lion comes down for its summoner, is on their side, and goes when sent away. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_lion", timeoutTicks = 60)
    public static void lionAnswersItsSummoner(GameTestHelper helper) {
        Villager caster = caster(helper, 2.5, 1.5);
        ModSpells.LIVING_CONSTELLATION.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        var lions = helper.getLevel().getEntitiesOfClass(StarLion.class, helper.getBounds().inflate(4), l -> caster.getUUID().equals(l.getSummonerUUID()));
        helper.assertTrue(lions.size() == 1, "expected one Lion, found " + lions.size());
        StarLion lion = lions.get(0);
        helper.assertTrue(lion.isAlliedTo(caster), "the Lion is not on its summoner's side");
        ((LivingConstellationSpell) ModSpells.LIVING_CONSTELLATION.get()).dismissSummons(helper.getLevel(), caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(!lion.isAlive(), "the Lion did not go back to the sky");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The black hole draws foes in, and its burst strikes them. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_hole", timeoutTicks = 100)
    public static void blackHoleDrawsInAndBursts(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 7.5);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 4, 4.5));
        double before = foe.position().distanceTo(at);
        BlackHole hole = BlackHole.open(helper.getLevel(), caster, ModSpells.BLACK_HOLE.get(), at, 12, 200, 4);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(foe.position().distanceTo(at) < before - .5, "the vindicator was not drawn in");
            hole.collapse();
        });
        helper.runAfterDelay(25 + BlackHole.COLLAPSE_TICKS + 4, () -> {
            helper.assertTrue(hurt(foe), "the black hole did not strike");
            helper.assertTrue(!hurt(caster), "the black hole hurt its caster");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** At zero the charge restores the caster's health and sends the foe back where it stood, hurt. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_reset", timeoutTicks = ResetCharge.COUNTDOWN + 60)
    public static void resetChargeRestoresAndSendsBack(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        Vec3 start = foe.position();
        ResetCharge.set(helper.getLevel(), caster, ModSpells.RESET_CHARGE.get(), 1, helper.absoluteVec(new Vec3(3.5, 2, 3.5)), 10, 4);
        helper.runAfterDelay(10, () -> caster.hurt(helper.getLevel().damageSources().generic(), 6));
        helper.runAfterDelay(20, () -> {
            Vec3 away = helper.absoluteVec(new Vec3(7.5, 2, 7.5));
            foe.teleportTo(helper.getLevel(), away.x, away.y, away.z, Set.of(), 0, 0);
        });
        helper.runAfterDelay(ResetCharge.COUNTDOWN + 5, () -> {
            helper.assertTrue(caster.getHealth() >= caster.getMaxHealth() - .01f, "the caster's health was not restored (" + caster.getHealth() + ")");
            helper.assertTrue(hurt(foe), "the reset did not strike the vindicator");
            helper.assertTrue(!foe.isAlive() || foe.position().distanceTo(start) < 1.5, "the vindicator was not sent back where it stood");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

}
