package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.piege.Pieges;
import com.waylays.arcanes.piege.TrapBlock;
import com.waylays.arcanes.piege.TrapBlockEntity;
import com.waylays.arcanes.piege.TrapKind;
import com.waylays.arcanes.piege.TrapSetup;
import com.waylays.arcanes.rp.arms.Proc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Server-side checks run by CI for the traps: there are twelve; the jaws snap shut on whoever steps in and stay shut,
 * then open; the spikes rise under whoever stands on them and wound them; what /piege set on a trap rides with it from
 * the item to the placed trap and back, and its effect is given (a sure one here). Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class PiegeGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2

    private PiegeGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return !e.isAlive() || e.getHealth() < e.getMaxHealth();
    }

    private static BlockState trap(TrapKind kind) {
        return Pieges.TRAPS.get(kind).get().defaultBlockState();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "piege_list")
    public static void thereAreTwelveTraps(GameTestHelper helper) {
        helper.assertTrue(Pieges.TRAPS.size() == 12 && Pieges.TRAP_ITEMS.size() == 12, "expected twelve traps, found " + Pieges.TRAPS.size());
        for (TrapKind kind : TrapKind.values()) {
            helper.assertTrue(Pieges.kindOf(new ItemStack(Pieges.TRAP_ITEMS.get(kind).get())) == kind, "the " + kind.id + " item is not its trap");
        }
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "piege_bear", timeoutTicks = 200)
    public static void bearTrapSnapsShutAndOpensAgain(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        BlockPos at = new BlockPos(4, 2, 4);
        helper.setBlock(at, trap(TrapKind.BEAR));
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 4.5));
        boolean[] shut = {false};
        helper.onEachTick(() -> shut[0] |= helper.getBlockState(at).getValue(TrapBlock.ACTIVE));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(shut[0], "the jaws did not snap shut");
            helper.assertTrue(hurt(husk), "the jaws did not hurt the husk");
            husk.discard();
        });
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(!helper.getBlockState(at).getValue(TrapBlock.ACTIVE), "the jaws did not open again");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "piege_spikes", timeoutTicks = 100)
    public static void spikesRiseUnderWhoeverStandsOnThem(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        BlockPos at = new BlockPos(4, 2, 4);
        helper.setBlock(at, trap(TrapKind.SPIKES));
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 4.5));
        boolean[] up = {false};
        helper.onEachTick(() -> up[0] |= helper.getBlockState(at).getValue(TrapBlock.ACTIVE));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(up[0], "the spikes did not rise");
            helper.assertTrue(hurt(husk), "the spikes did not hurt the husk");
            husk.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "piege_setup", timeoutTicks = 100)
    public static void theSetupRidesWithTheTrapAndItsEffectIsGiven(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        TrapSetup setup = new TrapSetup(1, List.of(new Proc("minecraft:poison", 100, 2, 6, false, false)));
        ItemStack item = new ItemStack(Pieges.TRAP_ITEMS.get(TrapKind.CALTROPS).get());
        item.set(Pieges.SETUP.get(), setup);
        BlockPos at = new BlockPos(4, 2, 4);
        helper.setBlock(at, trap(TrapKind.CALTROPS));
        TrapBlockEntity trap = (TrapBlockEntity) helper.getBlockEntity(at);
        helper.assertTrue(trap != null, "the caltrops have no block entity");
        trap.applyComponentsFromItemStack(item);
        helper.assertTrue(trap.setup().equals(setup), "the setup did not go from the item to the trap: " + trap.setup());
        DataComponentMap back = trap.collectComponents();
        helper.assertTrue(setup.equals(back.get(Pieges.SETUP.get())), "the setup does not come back with the trap");
        Vindicator v = helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(4.5, 2, 4.5));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(hurt(v), "the caltrops did not prick the vindicator");
            helper.assertTrue(v.hasEffect(MobEffects.POISON) && v.getEffect(MobEffects.POISON).getAmplifier() == 1,
                    "the sure Poison II set on the caltrops was not given");
            v.discard();
            helper.succeed();
        });
    }
}
