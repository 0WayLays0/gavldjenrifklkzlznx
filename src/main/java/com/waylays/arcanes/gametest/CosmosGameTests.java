package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.cosmos.CosmicVariant;
import com.waylays.arcanes.entity.cosmos.TimeDoor;
import com.waylays.arcanes.entity.cosmos.TimeLoop;
import com.waylays.arcanes.entity.cosmos.TimeStopBubble;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.cosmos.CosmosSpell;
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
 * holds a creature still and keeps the blows for later; a creature's variant casts the baton.
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
                helper.assertTrue(spell instanceof CosmosSpell, entry.getId() + " is in the Cosmos school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 6, "expected 6 Cosmos spells, found " + count);
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
}
