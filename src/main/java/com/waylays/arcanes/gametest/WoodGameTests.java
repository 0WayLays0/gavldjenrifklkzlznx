package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.wood.AmberPrison;
import com.waylays.arcanes.entity.wood.BrambleHedge;
import com.waylays.arcanes.entity.wood.RootSnare;
import com.waylays.arcanes.entity.wood.SeedPod;
import com.waylays.arcanes.entity.wood.VineLash;
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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Server-side checks run by CI for Wood Magic: the school is there; the hurled log strikes; the roots hold their
 * prisoner and crush it; the brambles tear what is in them; the seed pod bursts and leaves a thicket; the vine yanks
 * its catch to the caster; the ring saw cuts; the splinter cyclone shreds; the amber holds its prisoner and bursts;
 * the guardian answers its summoner and strikes; the world tree's quake strikes and its crown heals. Each test runs in
 * a batch of its own.
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

    /** The log is torn up, hurled, and bowls the husk over. */
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

    /** The vine catches the husk and yanks it to the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_vine", timeoutTicks = 60)
    public static void vineLashYanks(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 7.5);
        double before = husk.distanceTo(caster);
        cast(helper, ModSpells.VINE_LASH, caster);
        helper.runAfterDelay(VineLash.REACH + VineLash.PULL, () -> {
            helper.assertTrue(hurt(husk), "the vine did not tear the husk");
            helper.assertTrue(husk.distanceTo(caster) < before - 2, "the vine did not yank the husk in (" + before + " -> " + husk.distanceTo(caster) + ")");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_saw", timeoutTicks = 60)
    public static void ringSawCuts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.RING_SAW, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the saw did not cut the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_cyclone", timeoutTicks = 80)
    public static void splinterCycloneShreds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.SPLINTER_STORM, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the splinters did not shred the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The amber holds its prisoner where it stood, then bursts and cuts it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wood_amber", timeoutTicks = 100)
    public static void amberHoldsAndBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        Vec3 at = husk.position();
        AmberPrison prison = AmberPrison.encase(helper.getLevel(), caster, ModSpells.AMBER_PRISON.get(), husk, 30, 4);
        helper.runAfterDelay(AmberPrison.RISE + 2, () -> husk.setDeltaMovement(1, .5, 0));
        helper.runAfterDelay(AmberPrison.RISE + 12, () -> helper.assertTrue(husk.position().distanceTo(at) < .5, "the husk got out of the amber"));
        helper.runAfterDelay(prison.burstTick() + 2, () -> {
            helper.assertTrue(hurt(husk), "the amber did not cut the husk when it burst");
            GameTestSetup.clearLeftovers(helper);
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
