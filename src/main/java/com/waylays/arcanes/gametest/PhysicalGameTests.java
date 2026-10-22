package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.physical.DaggerStabsSkill;
import com.waylays.arcanes.spells.physical.DashSkill;
import com.waylays.arcanes.spells.physical.LeapSkill;
import com.waylays.arcanes.spells.physical.RushSkill;
import com.waylays.arcanes.spells.physical.PunchSkill;
import com.waylays.arcanes.spells.physical.ScytheDanceSkill;
import com.waylays.arcanes.spells.physical.ScytheReapSkill;
import com.waylays.arcanes.spells.physical.SwordRisingCutSkill;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Physical Prowess, the fighting moves: wiring into Iron's (and plain physical
 * damage that armor reduces), who each blow reaches, how far the dash and the leap go, what the weapon moves add
 * (slowing, lifting, bleeding, getting behind), and that the Seal of Silence does not stop them. Casters are no-AI
 * witches facing +Z; the dash and the leap are tried on armor stands, which move under their own weight. Moves that
 * reach past the platform run in batches of their own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class PhysicalGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2

    private PhysicalGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return e.getHealth() < e.getMaxHealth();
    }

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, ? extends AbstractSpell> spell, LivingEntity caster) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void physicalSchoolIsRegistered(GameTestHelper helper) {
        SchoolType physical = SchoolRegistry.getSchool(ModSchools.PHYSICAL_RESOURCE);
        helper.assertTrue(physical != null, "Physical Prowess missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            if (SpellRegistry.getSpell(entry.getId()).getSchoolType() == physical) {
                count++;
            }
        }
        helper.assertTrue(count == 37, "expected 37 fighting moves, got " + count);
        Holder<DamageType> damage = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModSchools.PHYSICAL_STRIKE);
        helper.assertTrue(!damage.is(DamageTypeTags.BYPASSES_ARMOR), "fighting moves should not go through armor");
        helper.assertTrue(physical.isFocus(new ItemStack(Items.LEATHER)), "leather is not the Physical Prowess focus");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, timeoutTicks = 40)
    public static void punchHitsOnlyTheNearestInFront(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        IronGolem near = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 3.5));
        Pig further = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.5, 2, 4.3));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, .5));
        cast(helper, ModSpells.PUNCH, caster);
        helper.runAfterDelay(PunchSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(hurt(near), "the punch missed the golem in front");
            helper.assertTrue(!hurt(further), "the punch hit more than the nearest enemy");
            helper.assertTrue(!hurt(behind), "the punch hit behind the fighter");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, timeoutTicks = 60)
    public static void punchFlurryHitsAgainAndAgain(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 3.5));
        cast(helper, ModSpells.PUNCH_FLURRY, caster);
        helper.runAfterDelay(25, () -> {
            // Seven jabs and a straight punch: far more than any single blow
            helper.assertTrue(golem.getHealth() <= golem.getMaxHealth() - 8, "the flurry landed too few blows (health " + golem.getHealth() + ")");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, timeoutTicks = 40)
    public static void slashCutsEveryoneInTheArc(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Pig left = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6, 2, 3.5));
        Pig right = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(3, 2, 3.5));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, .5));
        cast(helper, ModSpells.SLASH, caster);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(hurt(left) && hurt(right), "the slash missed a pig in its arc");
            helper.assertTrue(!hurt(behind), "the slash hit behind the fighter");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_moves", timeoutTicks = 40)
    public static void dashCoversFourBlocksAndStops(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, .5));
        double[] start = new double[1];
        helper.runAfterDelay(4, () -> {
            start[0] = stand.getZ();
            cast(helper, ModSpells.DASH, stand);
        });
        helper.runAfterDelay(4 + DashSkill.STEPS + 10, () -> {
            double moved = stand.getZ() - start[0];
            helper.assertTrue(moved >= 3.2 && moved <= 5.5, "the dash went " + moved + " blocks instead of about 4");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_moves", timeoutTicks = 50)
    public static void leapGoesThreeBlocksHigh(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, 4.5));
        double[] heights = {0, 0};
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(ModSpells.LEAP.get().checkPreCastConditions(helper.getLevel(), 1, stand, new MagicData(true)), "the stand cannot leap from the ground");
            heights[0] = stand.getY();
            heights[1] = stand.getY();
            cast(helper, ModSpells.LEAP, stand);
        });
        helper.onEachTick(() -> heights[1] = Math.max(heights[1], stand.getY()));
        helper.runAfterDelay(30, () -> {
            double rise = heights[1] - heights[0];
            helper.assertTrue(rise >= 2.4 && rise <= 3.6, "the leap rose " + rise + " blocks instead of about 3");
            helper.assertTrue(((LeapSkill) ModSpells.LEAP.get()).getHeight(3) == 6, "the leap at level 3 should go 6 blocks high");
            helper.succeed();
        });
    }

    /** The rush runs at the creature ahead, stops in front of it and shoulders it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_moves", timeoutTicks = 50)
    public static void rushRunsAtTheEnemyAndShouldersIt(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, .5));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 7.5));
        double[] start = new double[1];
        helper.runAfterDelay(4, () -> {
            stand.setYRot(0);
            stand.setYHeadRot(0);
            start[0] = stand.getZ();
            cast(helper, ModSpells.RUSH, stand);
        });
        helper.runAfterDelay(4 + RushSkill.MAX_TICKS + 6, () -> {
            double moved = stand.getZ() - start[0];
            helper.assertTrue(moved >= 4.5, "the rush only went " + moved + " blocks towards the pig 7 blocks away");
            helper.assertTrue(stand.distanceTo(pig) < 3, "the rush did not stop in front of the pig");
            helper.assertTrue(hurt(pig), "the pig was not shouldered");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void leapNeedsTheGround(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 5, 4.5));
        stand.setNoGravity(true);
        helper.assertTrue(!ModSpells.LEAP.get().checkPreCastConditions(helper.getLevel(), 1, stand, new MagicData(true)), "leapt from mid-air");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_scythe", timeoutTicks = 60)
    public static void scytheReapCutsTheLegsInAWideArc(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 2.5));
        IronGolem front = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 5.5));
        Pig side = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, 2, 2.5));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, .5));
        cast(helper, ModSpells.SCYTHE_REAP, caster);
        helper.runAfterDelay(ScytheReapSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(hurt(front) && hurt(side), "the sweep missed something in its half-circle");
            helper.assertTrue(front.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the sweep did not slow its victim");
            helper.assertTrue(!hurt(behind), "the sweep hit behind the fighter");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_scythe", timeoutTicks = 80)
    public static void scytheDanceSpinsAllAround(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 1.5));
        cast(helper, ModSpells.SCYTHE_DANCE, caster);
        helper.runAfterDelay(ScytheDanceSkill.SPIN_TICK - 2, () -> helper.assertTrue(!hurt(behind), "the first slash reached behind the fighter"));
        helper.runAfterDelay(ScytheDanceSkill.SPIN_TICK + 2, () -> {
            helper.assertTrue(hurt(behind), "the spin missed the pig behind the fighter");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_sword", timeoutTicks = 40)
    public static void risingCutLiftsItsVictim(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(4.5, 2, 3.5));
        double[] top = {0};
        cast(helper, ModSpells.SWORD_RISING_CUT, caster);
        double floor = helper.absoluteVec(new Vec3(0, 2, 0)).y;
        helper.onEachTick(() -> top[0] = Math.max(top[0], pig.getY() - floor));
        helper.runAfterDelay(SwordRisingCutSkill.HIT_TICK + 12, () -> {
            helper.assertTrue(hurt(pig), "the cut missed the pig");
            helper.assertTrue(top[0] > .8, "the pig was not lifted (rose " + top[0] + ")");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_sword", timeoutTicks = 40)
    public static void lungeDrivesThroughTheWholeLine(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, .5));
        IronGolem first = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 2.5));
        IronGolem second = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 4.5));
        cast(helper, ModSpells.SWORD_LUNGE, caster);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(first) && hurt(second), "the thrust did not go through both golems");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_dagger", timeoutTicks = 40)
    public static void quickStabsLeaveItBleeding(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 3.5));
        cast(helper, ModSpells.DAGGER_STABS, caster);
        int last = DaggerStabsSkill.STABS[DaggerStabsSkill.STABS.length - 1];
        helper.runAfterDelay(last + 2, () -> {
            MobEffectInstance bleeding = golem.getEffect(ModEffects.BLEEDING);
            helper.assertTrue(bleeding != null, "the stabs left no bleeding");
            helper.assertTrue(bleeding.getAmplifier() == 1, "the third stab did not deepen the wound");
            helper.assertTrue(hurt(golem), "the stabs did no damage");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "physical_dagger", timeoutTicks = 40)
    public static void flankGetsBehindAndBackstabs(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 4.5));
        // The golem faces the fighter: behind it is the far side
        golem.setYRot(180);
        golem.setYBodyRot(180);
        golem.setYHeadRot(180);
        helper.assertTrue(ModSpells.DAGGER_FLANK.get().checkPreCastConditions(helper.getLevel(), 1, caster, new MagicData(true)), "no enemy found in front");
        cast(helper, ModSpells.DAGGER_FLANK, caster);
        helper.assertTrue(caster.getZ() > golem.getZ(), "the fighter did not get behind the golem");
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(hurt(golem), "the backstab missed");
            helper.assertTrue(golem.hasEffect(ModEffects.BLEEDING), "the backstab left no bleeding");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void silenceDoesNotStopFightingMoves(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(ModEffects.SILENCE, 200));
        SchoolType physical = SchoolRegistry.getSchool(ModSchools.PHYSICAL_RESOURCE);
        SchoolType book = SchoolRegistry.getSchool(ModSchools.BOOK_RESOURCE);
        helper.assertTrue(!NeoForge.EVENT_BUS.post(new SpellPreCastEvent(player, ModSpells.PUNCH.get().getSpellId(), 1, physical, CastSource.SPELLBOOK)).isCanceled(),
                "a silenced fighter could not punch");
        helper.assertTrue(NeoForge.EVENT_BUS.post(new SpellPreCastEvent(player, ModSpells.PAGE_STORM.get().getSpellId(), 1, book, CastSource.SPELLBOOK)).isCanceled(),
                "the silence no longer stops magic");
        helper.succeed();
    }
}
