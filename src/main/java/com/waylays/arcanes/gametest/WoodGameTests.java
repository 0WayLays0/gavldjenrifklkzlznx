package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.wood.BarkShell;
import com.waylays.arcanes.entity.wood.BrambleHedge;
import com.waylays.arcanes.entity.wood.FallingTree;
import com.waylays.arcanes.entity.wood.RootSnare;
import com.waylays.arcanes.entity.wood.SeedPod;
import com.waylays.arcanes.entity.wood.SuddenGrove;
import com.waylays.arcanes.entity.wood.WoodGuardian;
import com.waylays.arcanes.entity.wood.WorldTree;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.wood.WoodGuardianSpell;
import com.waylays.arcanes.spells.wood.WoodSpell;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Server-side checks run by CI for Wood Magic: the school is there; the hurled log strikes; the roots hold their
 * prisoner and crush it; the brambles tear what is in them; the seed pod bursts and leaves a thicket; the bark takes
 * the edge off a blow and tears the striker; the falling tree crushes what it lands on; the leaf storm cuts; the grove's
 * trees grow as real blocks, mend the caster and are gone at the end; the guardian answers its summoner and strikes;
 * the world tree's quake strikes and its crown heals. Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class WoodGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private WoodGameTests() {}

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

    private static int logsAround(GameTestHelper helper, BlockPos centre, int r) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -2, -r), centre.offset(r, 10, r))) {
            if (helper.getLevel().getBlockState(pos).is(Blocks.OAK_LOG)) {
                n++;
            }
        }
        return n;
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void woodSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.WOOD_RESOURCE);
        helper.assertTrue(school != null, "Wood school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof WoodSpell || spell instanceof WoodGuardianSpell, entry.getId() + " is in the Wood school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Wood spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.SYLVAN_CODEX.get()).getItem() instanceof SpellBook, "the Sylvan Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.OAK_LOG).is(ModSchools.WOOD_FOCUS), "logs are not the school's focus");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_log", timeoutTicks = 60)
    public static void logHurlStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        cast(helper, ModSpells.LOG_HURL, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the log did not strike the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The roots hold their prisoner where it stood and crush it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_roots", timeoutTicks = 100)
    public static void rootsHoldAndCrush(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        Vec3 at = husk.position();
        RootSnare.seize(helper.getLevel(), caster, ModSpells.GRASPING_ROOTS.get(), husk, 60, 3);
        helper.runAfterDelay(RootSnare.GRIP + 2, () -> husk.setDeltaMovement(1, 0, 0));
        helper.runAfterDelay(RootSnare.GRIP + 24, () -> {
            helper.assertTrue(hurt(husk), "the roots did not crush the husk");
            helper.assertTrue(husk.position().distanceTo(at) < 1, "the husk got out of the roots");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_hedge", timeoutTicks = 80)
    public static void brambleHedgeTears(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        BrambleHedge.grow(helper.getLevel(), caster, ModSpells.BRAMBLE_HEDGE.get(), BrambleHedge.Mode.LINE, husk.position(), 0, 6, 100, 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the brambles did not tear the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** A pod dropped on the husk bursts in thorns and leaves a thicket of brambles. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_pods", timeoutTicks = 60)
    public static void seedPodBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        SeedPod.lob(helper.getLevel(), caster, ModSpells.SEED_PODS.get(), husk.position().add(0, 3.5, 0), new Vec3(0, -.6, 0), 4);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the pod did not burst on the husk");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(BrambleHedge.class, husk.getBoundingBox().inflate(4)).isEmpty(), "no thicket sprang up");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** Through the bark a blow does far less, and the striker is torn by the thorns. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_bark", timeoutTicks = 60)
    public static void barkskinGuardsAndTears(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 2.5);
        Husk husk = husk(helper, 4.5, 4);
        cast(helper, ModSpells.BARKSKIN, caster);
        helper.assertTrue(BarkShell.of(helper.getLevel(), caster) != null, "no bark grew on the caster");
        helper.runAfterDelay(BarkShell.GROW + 2, () -> {
            float before = caster.getHealth();
            caster.invulnerableTime = 0;
            caster.hurt(helper.getLevel().damageSources().mobAttack(husk), 10);
            float lost = before - caster.getHealth();
            helper.assertTrue(lost > 0 && lost < 7, "the bark did not take the edge off the blow (lost " + lost + ")");
            helper.assertTrue(hurt(husk), "the thorns did not tear the striker");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The tree grows beside the caster and falls on the husk ahead. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_tree", timeoutTicks = 100)
    public static void fallingTreeCrushes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        List<ChunkPos> forced = GameTestSetup.forceChunksAround(helper, 1);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.FALLING_TREE, caster);
        helper.runAfterDelay(FallingTree.impact() + 3, () -> {
            helper.assertTrue(hurt(husk), "the tree did not crush the husk");
            helper.assertTrue(!hurt(caster), "the tree fell on its caster");
            GameTestSetup.clearLeftovers(helper);
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x, chunk.z, false));
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_storm", timeoutTicks = 80)
    public static void leafStormCuts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.LEAF_STORM, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the leaves did not cut the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The grove's trees grow as real blocks, the caster is mended inside it, and the trees are gone at the end. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_grove", timeoutTicks = 140)
    public static void groveGrowsMendsAndGoes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        List<ChunkPos> forced = GameTestSetup.forceChunksAround(helper, 1);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        BlockPos centre = caster.blockPosition();
        SuddenGrove grove = SuddenGrove.plant(helper.getLevel(), caster, ModSpells.SUDDEN_GROVE.get(), 5, 80, 4, 2);
        helper.runAfterDelay(4 + 5 * SuddenGrove.GAP + 10, () -> {
            helper.assertTrue(logsAround(helper, centre, 8) >= 8, "the grove's trees did not grow (" + logsAround(helper, centre, 8) + " logs)");
            helper.assertTrue(caster.getHealth() > 10, "the grove did not mend its caster");
        });
        helper.runAfterDelay(85, () -> {
            helper.assertTrue(logsAround(helper, centre, 8) == 0, "the grove's trees were left behind");
            helper.assertTrue(grove.isRemoved() || grove.tickCount > 80, "the grove did not end");
            GameTestSetup.clearLeftovers(helper);
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x, chunk.z, false));
            helper.succeed();
        });
    }

    /** The guardian pulls itself up for its summoner, is on their side, strikes, and goes back when sent away. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_guardian", timeoutTicks = 80)
    public static void guardianAnswersAndStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.WOOD_GUARDIAN, caster);
        var guardians = helper.getLevel().getEntitiesOfClass(WoodGuardian.class, helper.getBounds().inflate(4), g -> caster.getUUID().equals(g.getSummonerUUID()));
        helper.assertTrue(guardians.size() == 1, "expected one guardian, found " + guardians.size());
        WoodGuardian guardian = guardians.get(0);
        helper.assertTrue(guardian.isAlliedTo(caster), "the guardian is not on its summoner's side");
        helper.runAfterDelay(WoodGuardian.FORM_TICKS + 2, () -> {
            guardian.doHurtTarget(husk);
            helper.assertTrue(hurt(husk), "the guardian's blow did not strike the husk");
            ((WoodGuardianSpell) ModSpells.WOOD_GUARDIAN.get()).dismissSummons(helper.getLevel(), caster);
        });
        helper.runAfterDelay(WoodGuardian.FORM_TICKS + 6, () -> {
            helper.assertTrue(!guardian.isAlive(), "the guardian did not go back into the earth");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The world tree's quake strikes the husk where it breaks out, and its crown heals the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_world_tree", timeoutTicks = 120)
    public static void worldTreeQuakesAndHeals(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        List<ChunkPos> forced = GameTestSetup.forceChunksAround(helper, 1);
        Witch caster = caster(helper, 1.5, 1.5);
        caster.setHealth(10);
        Husk husk = husk(helper, 5.5, 6.5);
        WorldTree.raise(helper.getLevel(), caster, ModSpells.WORLD_TREE.get(), husk.position(), 90, 3, 6, 2);
        helper.runAfterDelay(WorldTree.BREAK_OUT + 2, () -> helper.assertTrue(hurt(husk), "the quake did not strike the husk"));
        helper.runAfterDelay(WorldTree.RISE + 15, () -> {
            helper.assertTrue(caster.getHealth() > 10, "the tree's crown did not heal its caster");
            GameTestSetup.clearLeftovers(helper);
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x, chunk.z, false));
            helper.succeed();
        });
    }
}
