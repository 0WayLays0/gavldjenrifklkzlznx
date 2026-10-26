package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.cosmos.AsteroidBelt;
import com.waylays.arcanes.entity.cosmos.BlackHole;
import com.waylays.arcanes.entity.cosmos.CaptivePlanet;
import com.waylays.arcanes.entity.cosmos.CosmicVariant;
import com.waylays.arcanes.entity.cosmos.PocketMoon;
import com.waylays.arcanes.entity.cosmos.ResetCharge;
import com.waylays.arcanes.entity.cosmos.RiddenComet;
import com.waylays.arcanes.entity.cosmos.StarLion;
import com.waylays.arcanes.entity.cosmos.TimeDoor;
import com.waylays.arcanes.entity.cosmos.TimeLoop;
import com.waylays.arcanes.entity.cosmos.TimeStopBubble;
import com.waylays.arcanes.entity.cosmos.TimeStorm;
import com.waylays.arcanes.entity.cosmos.ZeroGravityField;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.cosmos.CosmosSpell;
import com.waylays.arcanes.spells.cosmos.LivingConstellationSpell;
import com.waylays.arcanes.spells.cosmos.PocketUniverse;
import com.waylays.arcanes.spells.cosmos.PruningBatonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

/**
 * Server-side checks run by CI for Cosmos: the school is there; the pocket universe exists, its islands are built and
 * grow, and only their owners may build on them; the time door sends what goes in out of the other door; the pruning
 * baton sends its target back where it was and prunes the weak; the time loop snaps its prisoner back; stopped time
 * holds a creature still and keeps the blows for later; a creature's variant casts the baton. Space: the captive
 * planet swings its prisoner round, the comet carries its rider and crashes, the pocket moon strikes in orbit, the
 * asteroid belt crushes what it closes on, zero gravity lifts foes and slams them down, the Lion answers its
 * summoner. The ultimates: the black hole draws in and bursts, the reset charge restores allies and sends foes back,
 * the storm at the end of time devours the weak.
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
        helper.assertTrue(count == 15, "expected 15 Cosmos spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.CODEX_OUT_OF_TIME.get()).getItem() instanceof SpellBook, "the Codex Out of Time is not a spellbook");
        helper.assertTrue(new ItemStack(Items.CLOCK).is(ModSchools.COSMOS_FOCUS), "clocks are not the school's focus");
        helper.succeed();
    }

    /**
     * An island is built round its centre, grows outward, and gets its door out. (The test server only has the
     * worlds of its preset, not the pocket universe that datapacks add to real worlds: when it is missing the island
     * is built far out in the test world.)
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_pocket", timeoutTicks = 100)
    public static void pocketIslandsAreBuiltAndGrow(GameTestHelper helper) {
        ServerLevel pocket = PocketUniverse.level(helper.getLevel().getServer());
        if (pocket == null) {
            pocket = helper.getLevel();
        }
        BlockPos c = PocketUniverse.centre(9000);
        PocketUniverse.build(pocket, c, 0, 1, 7);
        helper.assertTrue(!pocket.getBlockState(c).isAir(), "no ground in the middle of the island (its stopped clock)");
        helper.assertTrue(!pocket.getBlockState(c.below(3)).isAir(), "the island has no rock under its grass");
        // Past the ragged edge of a level 1 island (11 across its radius at most 12), within that of level 3 (17, at least 12.6)
        BlockPos ring = c.east(12);
        helper.assertTrue(pocket.getBlockState(ring).isAir(), "the level 1 island already reaches the ring of level 3");
        PocketUniverse.build(pocket, c, 1, 3, 7);
        helper.assertTrue(pocket.getBlockState(ring).is(Blocks.GRASS_BLOCK), "the island did not grow out to level 3");
        TimeDoor.standAtIsland(pocket, c);
        TimeDoor.standAtIsland(pocket, c);
        int doors = pocket.getEntitiesOfClass(TimeDoor.class, new AABB(c).inflate(6), d -> d.getMode() == TimeDoor.Mode.EXIT).size();
        helper.assertTrue(doors == 1, "expected one door out of the island, found " + doors);
        helper.succeed();
    }

    /** Only the owner of a plot may change it; between plots, no one. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_pocket_rules")
    public static void onlyOwnersBuildInTheirPocket(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        PocketUniverse.Plots plots = PocketUniverse.plots(helper.getLevel().getServer());
        int plot = plots.plotOf(owner.getUUID());
        BlockPos home = PocketUniverse.centre(plot).east(5);
        helper.assertTrue(PocketUniverse.plotAt(home) == plot, "the island's ground is not in its plot");
        helper.assertTrue(PocketUniverse.canBuild(owner, home), "the owner cannot build on their own island");
        helper.assertTrue(!PocketUniverse.canBuild(stranger, home), "a stranger can build on someone else's island");
        helper.assertTrue(!PocketUniverse.canBuild(owner, PocketUniverse.centre(plot + 1)), "the owner can build on the next island");
        helper.assertTrue(!PocketUniverse.canBuild(owner, home.east(PocketUniverse.SPACING / 2)), "the owner can build between plots");
        helper.succeed();
    }

    /** Whatever stands in the first door comes out of the second. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_door", timeoutTicks = 80)
    public static void timeDoorSendsThrough(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vec3 in = helper.absoluteVec(new Vec3(2.5, 2, 4.5));
        Vec3 out = helper.absoluteVec(new Vec3(7.5, 2, 4.5));
        TimeDoor.gate(helper.getLevel(), caster, in, 0, out, 0, 100);
        Vindicator foe = foe(helper, 2.5, 4.5);
        helper.succeedWhen(() -> {
            helper.assertTrue(foe.position().distanceTo(out) < 2, "the vindicator did not come out of the second door");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The baton sends its target back where it stood three seconds before. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_baton", timeoutTicks = 140)
    public static void pruningBatonSendsBack(GameTestHelper helper) {
        Villager caster = caster(helper, 4.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 7.5);
        Vec3 start = foe.position();
        helper.runAfterDelay(70, () -> {
            Vec3 near = helper.absoluteVec(new Vec3(4.5, 2, 3.8));
            foe.teleportTo(helper.getLevel(), near.x, near.y, near.z, Set.of(), 0, 0);
        });
        helper.runAfterDelay(74, () -> ModSpells.PRUNING_BATON.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true)));
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(foe), "the baton did not strike");
            helper.assertTrue(foe.position().distanceTo(start) < 1, "the vindicator was not sent back where it stood");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** What the baton strikes when weak enough is pruned; bosses never are. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_prune", timeoutTicks = 60)
    public static void pruningBatonPrunesTheWeak(GameTestHelper helper) {
        helper.assertTrue(Math.abs(PruningBatonSpell.pruneLine(10, 20) - 6) < 1.0E-3, "the pruning line at level 10 is not 30%");
        helper.assertTrue(PruningBatonSpell.pruneLine(1, 10) >= 3, "the pruning line is below 3 health");
        Villager caster = caster(helper, 4.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 3.8);
        foe.setHealth(4);
        ModSpells.PRUNING_BATON.get().onCast(helper.getLevel(), 10, caster, CastSource.COMMAND, new MagicData(true));
        helper.succeedWhen(() -> {
            helper.assertTrue(!foe.isAlive(), "the weakened vindicator was not pruned");
            GameTestSetup.clearLeftovers(helper);
        });
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

    /** In stopped time a creature does not move and takes no blow, until time starts again: then all of it lands. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_stop", timeoutTicks = 120)
    public static void timeStopHoldsAndKeepsTheBlows(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        Vec3 at = foe.position();
        boolean stillBefore = foe.isNoAi();
        TimeStopBubble.stop(helper.getLevel(), caster, 8, 40);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(TimeStopBubble.isFrozen(foe), "the vindicator is not frozen");
            foe.setDeltaMovement(.8, 0, 0);
            foe.hurt(helper.getLevel().damageSources().mobAttack(caster), 5);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(foe.position().distanceTo(at) < .1, "the frozen vindicator moved");
            helper.assertTrue(foe.getHealth() == foe.getMaxHealth(), "the blow landed while time was stopped");
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(!TimeStopBubble.isFrozen(foe), "the vindicator is still frozen");
            helper.assertTrue(foe.isNoAi() == stillBefore, "the vindicator's own will was not given back as it was");
            helper.assertTrue(foe.getHealth() < foe.getMaxHealth(), "the kept blow did not land when time started again");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** A creature's variant (it has no spellbook) swings the baton at what its original looks at. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_variant", timeoutTicks = 80)
    public static void variantCastsASpell(GameTestHelper helper) {
        Villager caster = caster(helper, 3, 1.5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        CosmicVariant variant = CosmicVariant.scanIn(helper.getLevel(), caster, helper.absoluteVec(new Vec3(4.6, 2, 1.5)));
        helper.assertTrue(variant != null, "no variant scanned in");
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(foe), "the variant's spell did not strike");
            helper.assertTrue(!hurt(caster), "the variant's spell struck its original");
            GameTestSetup.clearLeftovers(helper);
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

    /** The storm devours a weakened foe. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "cosmos_storm", timeoutTicks = 100)
    public static void endOfTimeDevoursTheWeak(GameTestHelper helper) {
        Villager caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 5.5, 5.5);
        foe.setHealth(6);
        TimeStorm.unleash(helper.getLevel(), caster, ModSpells.END_OF_TIME.get(), 1, foe.position(), 200, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(!foe.isAlive(), "the storm did not devour the weakened vindicator");
            helper.assertTrue(caster.isAlive() && !hurt(caster), "the storm hurt its caster");
            GameTestSetup.clearLeftovers(helper);
        });
    }
}
