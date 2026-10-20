package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.physical.Missile;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.physical.AxeSunderSkill;
import com.waylays.arcanes.spells.physical.AxeThrowSkill;
import com.waylays.arcanes.spells.physical.BowGuidedSkill;
import com.waylays.arcanes.spells.physical.BowRainSkill;
import com.waylays.arcanes.spells.physical.BowVolleySkill;
import com.waylays.arcanes.spells.physical.CrossbowBurstSkill;
import com.waylays.arcanes.spells.physical.CrossbowHarpoonSkill;
import com.waylays.arcanes.spells.physical.CrossbowPierceSkill;
import com.waylays.arcanes.spells.physical.DaggerFinisherSkill;
import com.waylays.arcanes.spells.physical.EvadeSkill;
import com.waylays.arcanes.spells.physical.HammerConcussionSkill;
import com.waylays.arcanes.spells.physical.HammerQuakeSkill;
import com.waylays.arcanes.spells.physical.HammerRamSkill;
import com.waylays.arcanes.spells.physical.KnifeFanSkill;
import com.waylays.arcanes.spells.physical.KnifePinSkill;
import com.waylays.arcanes.spells.physical.KnifeThrowSkill;
import com.waylays.arcanes.spells.physical.LongswordCutSkill;
import com.waylays.arcanes.spells.physical.LongswordParrySkill;
import com.waylays.arcanes.spells.physical.LongswordSweepSkill;
import com.waylays.arcanes.spells.physical.ScytheHookSkill;
import com.waylays.arcanes.spells.physical.SpearJavelinSkill;
import com.waylays.arcanes.spells.physical.SpearThrustSkill;
import com.waylays.arcanes.spells.physical.SwordDisarmSkill;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Server-side checks run by CI for the weapon styles of Physical Prowess (the third move of the scythe, sword and
 * dagger, the evade, the hammer, axe, spear, longsword, bow, crossbow and throwing knives): who each move reaches and
 * what it does to them. Casters are no-AI witches facing +Z; moves that carry the fighter are tried on armor stands,
 * which move under their own weight, and victims that must be thrown about are pigs with their AI. Moves whose
 * missiles or victims leave the platform run in batches of their own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class WeaponGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private WeaponGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return e.getHealth() < e.getMaxHealth();
    }

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, ? extends AbstractSpell> spell, LivingEntity caster) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    /** A no-AI witch facing exactly +Z (a new creature faces a few random degrees off). */
    private static Witch caster(GameTestHelper helper, double x, double z) {
        Witch witch = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(x, 2, z));
        witch.setYRot(0);
        witch.setYHeadRot(0);
        witch.setYBodyRot(0);
        return witch;
    }

    private static IronGolem golem(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(x, 2, z));
    }

    /** Turns {@code entity} to look at {@code at}. */
    private static void lookAt(LivingEntity entity, Vec3 at) {
        Vec3 d = at.subtract(entity.getEyePosition());
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setXRot(pitch);
    }

    // ---------------------------------------------------------------- moving, scythe, sword, dagger

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_evade", timeoutTicks = 60)
    public static void evadeLeapsFarBackward(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, 7.5));
        double[] start = new double[1];
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(ModSpells.EVADE.get().checkPreCastConditions(helper.getLevel(), 1, stand, new MagicData(true)), "the stand cannot push off the ground");
            start[0] = stand.getZ();
            cast(helper, ModSpells.EVADE, stand);
        });
        helper.runAfterDelay(4 + EvadeSkill.TAKE_OFF + EvadeSkill.BEATS + 12, () -> {
            double moved = start[0] - stand.getZ();
            helper.assertTrue(moved >= 4.5 && moved <= 8, "the evade went " + moved + " blocks back instead of about 6");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_hook", timeoutTicks = 40)
    public static void reapersHookDragsTheVictimIn(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(4.5, 2, 6.5));
        double[] start = new double[1];
        helper.runAfterDelay(ScytheHookSkill.HIT_TICK - 1, () -> start[0] = pig.getZ());
        cast(helper, ModSpells.SCYTHE_HOOK, caster);
        helper.runAfterDelay(ScytheHookSkill.HIT_TICK + 12, () -> {
            helper.assertTrue(hurt(pig), "the hook missed the pig 5 blocks away");
            helper.assertTrue(start[0] - pig.getZ() > 1.5, "the pig was not dragged in (moved " + (start[0] - pig.getZ()) + ")");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 120)
    public static void disarmTakesTheWeaponForAWhile(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator vindicator = helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(4.5, 2, 3.5));
        vindicator.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        cast(helper, ModSpells.SWORD_DISARM, caster);
        helper.runAfterDelay(SwordDisarmSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(vindicator.getMainHandItem().isEmpty(), "the vindicator kept its axe");
            helper.assertTrue(hurt(vindicator), "the disarm did no damage");
        });
        int back = SwordDisarmSkill.HIT_TICK + ((SwordDisarmSkill) ModSpells.SWORD_DISARM.get()).getDisarmTicks(1) + 4;
        helper.runAfterDelay(back, () -> {
            helper.assertTrue(vindicator.getMainHandItem().is(Items.IRON_AXE), "the vindicator never got its axe back");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void coupDeGraceFinishesTheWounded(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        IronGolem golem = golem(helper, 4.5, 3.5);
        helper.assertTrue(DaggerFinisherSkill.multiplier(golem) == 1, "a golem at full health counts as wounded");
        golem.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 200));
        helper.assertTrue(DaggerFinisherSkill.multiplier(golem) == DaggerFinisherSkill.WOUNDED, "a bleeding golem does not count as wounded");
        golem.removeAllEffects();
        golem.setHealth(golem.getMaxHealth() * .3f);
        helper.assertTrue(DaggerFinisherSkill.multiplier(golem) == DaggerFinisherSkill.EXECUTE, "a golem at 30% health is not finished off");
        float before = golem.getHealth();
        cast(helper, ModSpells.DAGGER_FINISHER, caster);
        helper.runAfterDelay(DaggerFinisherSkill.HIT_TICK + 2, () -> {
            // 4 x 0.8 + 1 (bare hand), times 2.5
            helper.assertTrue(before - golem.getHealth() >= 9, "the coup de grâce did only " + (before - golem.getHealth()));
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- hammer

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void concussionTurnsTheVictimAround(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 3.5));
        // Facing the fighter
        pig.setYRot(180);
        pig.setYHeadRot(180);
        pig.setYBodyRot(180);
        cast(helper, ModSpells.HAMMER_CONCUSSION, caster);
        helper.runAfterDelay(HammerConcussionSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(hurt(pig), "the blow missed");
            helper.assertTrue(Math.abs(Mth.wrapDegrees(pig.getYRot() - 180)) > 170, "the pig was not turned round (facing " + pig.getYRot() + ")");
            helper.assertTrue(pig.hasEffect(MobEffects.CONFUSION) && pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the pig is not dazed");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_quake", timeoutTicks = 50)
    public static void earthshakerHitsEveryoneStandingNearTheImpact(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Pig front = helper.spawn(EntityType.PIG, new Vec3(4.5, 2, 3.8));
        Pig side = helper.spawn(EntityType.PIG, new Vec3(6.8, 2, 3.8));
        Pig far = helper.spawn(EntityType.PIG, new Vec3(.5, 2, 8.5));
        cast(helper, ModSpells.HAMMER_QUAKE, caster);
        helper.runAfterDelay(HammerQuakeSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(hurt(front) && hurt(side), "the quake missed a pig standing near the impact");
            helper.assertTrue(!hurt(far), "the quake reached a pig 6 blocks from the impact");
            helper.assertTrue(front.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the quake did not slow its victims");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_ram", timeoutTicks = 50)
    public static void batteringRamSendsTheVictimFlying(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(4.5, 2, 2.5));
        double[] start = new double[1];
        helper.runAfterDelay(HammerRamSkill.HIT_TICK - 1, () -> start[0] = pig.getZ());
        cast(helper, ModSpells.HAMMER_RAM, caster);
        helper.runAfterDelay(HammerRamSkill.HIT_TICK + 15, () -> {
            helper.assertTrue(hurt(pig), "the ram missed");
            helper.assertTrue(pig.getZ() - start[0] > 3, "the pig flew only " + (pig.getZ() - start[0]) + " blocks");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- axe

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void cleaveHitsOnlyTheNearest(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        IronGolem near = golem(helper, 4.5, 3.5);
        Pig behindIt = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 5.5));
        cast(helper, ModSpells.AXE_CLEAVE, caster);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(hurt(near), "the cleave missed");
            helper.assertTrue(!hurt(behindIt), "the cleave went through to the next enemy");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_axe_throw", timeoutTicks = 50)
    public static void thrownAxeHitsAtRange(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        IronGolem golem = golem(helper, 4.5, 7.5);
        caster.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        cast(helper, ModSpells.AXE_THROW, caster);
        helper.runAfterDelay(AxeThrowSkill.THROW_TICK + 1, () -> {
            List<Missile> axes = helper.getLevel().getEntitiesOfClass(Missile.class, caster.getBoundingBox().inflate(4));
            helper.assertTrue(axes.size() == 1 && axes.get(0).getKind() == Missile.Kind.AXE, "expected one thrown axe, got " + axes.size());
            helper.assertTrue(axes.get(0).getItem().is(Items.GOLDEN_AXE), "the thrown axe does not look like the fighter's own");
        });
        helper.runAfterDelay(AxeThrowSkill.THROW_TICK + 15, () -> {
            helper.assertTrue(hurt(golem), "the thrown axe missed the golem 7 blocks away");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void armorSplitterSundersTheArmor(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator vindicator = helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(4.5, 2, 3.5));
        vindicator.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        double[] armor = new double[1];
        // The chestplate's armor counts from the vindicator's next tick
        helper.runAfterDelay(2, () -> {
            armor[0] = vindicator.getAttributeValue(Attributes.ARMOR);
            helper.assertTrue(armor[0] > 0, "the chestplate gives no armor");
            cast(helper, ModSpells.AXE_SUNDER, caster);
        });
        helper.runAfterDelay(2 + AxeSunderSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(vindicator.hasEffect(ModEffects.SUNDERED), "the armor was not split");
            helper.assertTrue(vindicator.getAttributeValue(Attributes.ARMOR) < armor[0], "Sundered took no armor away (" + armor[0] + " -> "
                    + vindicator.getAttributeValue(Attributes.ARMOR) + ")");
            helper.assertTrue(vindicator.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() > 0, "the chestplate took no wear");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- spear

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void longThrustReachesFiveBlocks(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        IronGolem first = golem(helper, 4.5, 2.8);
        IronGolem second = golem(helper, 4.5, 4.8);
        cast(helper, ModSpells.SPEAR_THRUST, caster);
        helper.runAfterDelay(SpearThrustSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(hurt(first) && hurt(second), "the thrust did not reach both golems");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_charge", timeoutTicks = 50)
    public static void chargeSkewersTheFirstInTheWayAndStops(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, .5));
        IronGolem golem = golem(helper, 4.5, 6.5);
        helper.runAfterDelay(4, () -> cast(helper, ModSpells.SPEAR_CHARGE, stand));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(hurt(golem), "the charge did not skewer the golem");
            helper.assertTrue(stand.getZ() > 2.5 + helper.absoluteVec(Vec3.ZERO).z, "the stand barely moved");
            helper.assertTrue(stand.getZ() < golem.getZ(), "the charge ran on past the golem");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_javelin", timeoutTicks = 50)
    public static void javelinGoesThroughTwo(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        IronGolem first = golem(helper, 4.5, 4.5);
        IronGolem second = golem(helper, 4.5, 7.5);
        cast(helper, ModSpells.SPEAR_JAVELIN, caster);
        helper.runAfterDelay(SpearJavelinSkill.THROW_TICK + 10, () -> {
            helper.assertTrue(hurt(first) && hurt(second), "the javelin did not go through to the second golem");
            helper.assertTrue(first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the javelin did not slow its victim");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- longsword

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void wrathCutStaggersTheWholeArc(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Pig left = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.2, 2, 3.8));
        Pig right = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.8, 2, 3.8));
        cast(helper, ModSpells.LONGSWORD_CUT, caster);
        helper.runAfterDelay(LongswordCutSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(hurt(left) && hurt(right), "the cut missed a pig in its arc");
            helper.assertTrue(left.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the cut did not stagger");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void halfMoonReachesTheSides(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 2.5);
        // Off to the side, a little ahead
        Pig side = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, 2, 3));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, .5));
        cast(helper, ModSpells.LONGSWORD_SWEEP, caster);
        helper.runAfterDelay(LongswordSweepSkill.HIT_TICK + 2, () -> {
            helper.assertTrue(hurt(side), "the sweep missed the pig at the fighter's side");
            helper.assertTrue(!hurt(behind), "the sweep hit behind the fighter");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void parryTurnsABlowFromInFrontAndRipostes(GameTestHelper helper) {
        Witch fighter = caster(helper, 4.5, 1.5);
        Vindicator attacker = helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(4.5, 2, 3));
        cast(helper, ModSpells.LONGSWORD_PARRY, fighter);
        helper.assertTrue(LongswordParrySkill.isGuarding(fighter), "the guard is not up");
        helper.runAfterDelay(3, () -> {
            boolean hurtNow = fighter.hurt(helper.getLevel().damageSources().mobAttack(attacker), 6);
            helper.assertTrue(!hurtNow && !hurt(fighter), "the blow got through the parry");
            helper.assertTrue(!LongswordParrySkill.isGuarding(fighter), "the guard stayed up after a parry");
        });
        helper.runAfterDelay(3 + LongswordParrySkill.RIPOSTE_TICK + 2, () -> {
            helper.assertTrue(hurt(attacker), "no riposte");
            helper.assertTrue(attacker.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the attacker was not thrown off balance");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void parryDoesNotCoverTheBack(GameTestHelper helper) {
        Witch fighter = caster(helper, 4.5, 3.5);
        Vindicator attacker = helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(4.5, 2, 1.5));
        cast(helper, ModSpells.LONGSWORD_PARRY, fighter);
        helper.runAfterDelay(3, () -> {
            fighter.hurt(helper.getLevel().damageSources().mobAttack(attacker), 6);
            helper.assertTrue(hurt(fighter), "a blow from behind was parried");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- bow

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_guided", timeoutTicks = 80)
    public static void guidedArrowFollowsTheArchersEyes(GameTestHelper helper) {
        Witch archer = caster(helper, 4.5, .5);
        // Well off the line the arrow leaves on
        IronGolem golem = golem(helper, 7.5, 7.5);
        cast(helper, ModSpells.BOW_GUIDED, archer);
        helper.runAfterDelay(BowGuidedSkill.SHOT_TICK + 1, () -> lookAt(archer, golem.getBoundingBox().getCenter()));
        helper.succeedWhen(() -> helper.assertTrue(hurt(golem), "the arrow did not turn onto the golem the archer looked at"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_volley", timeoutTicks = 50)
    public static void volleyFansOut(GameTestHelper helper) {
        Witch archer = caster(helper, 4.5, .5);
        IronGolem middle = golem(helper, 4.5, 6.5);
        IronGolem left = golem(helper, 7.2, 6.5);
        IronGolem right = golem(helper, 1.8, 6.5);
        int[] most = {0};
        helper.onEachTick(() -> most[0] = Math.max(most[0], helper.getLevel().getEntitiesOfClass(Missile.class, archer.getBoundingBox().inflate(16)).size()));
        cast(helper, ModSpells.BOW_VOLLEY, archer);
        helper.runAfterDelay(BowVolleySkill.SHOT_TICK + 10, () -> {
            helper.assertTrue(most[0] == BowVolleySkill.ARROWS, "expected " + BowVolleySkill.ARROWS + " arrows, saw " + most[0]);
            helper.assertTrue(hurt(middle), "the volley missed straight ahead");
            helper.assertTrue(hurt(left) || hurt(right), "the volley did not fan out to the sides");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_rain", timeoutTicks = 80)
    public static void arrowRainFallsWhereTheArcherLooked(GameTestHelper helper) {
        // An armor stand keeps looking where it is turned (a mob's look control would level its eyes again)
        ArmorStand archer = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, .5));
        // Looking down at the floor a few blocks ahead
        Vec3 expected = helper.absoluteVec(new Vec3(4.5, 2, 5.5));
        lookAt(archer, expected);
        helper.onEachTick(() -> lookAt(archer, expected));
        Vec3 spot = BowRainSkill.target(helper.getLevel(), archer);
        helper.assertTrue(spot.distanceTo(expected) < 1.5, "the rain would fall at " + spot + " instead of " + expected);
        cast(helper, ModSpells.BOW_RAIN, archer);
        helper.runAfterDelay(BowRainSkill.SHOT_TICK + BowRainSkill.FALL_DELAY + 7, () -> {
            AABB above = new AABB(spot, spot).inflate(BowRainSkill.RADIUS + 1, 0, BowRainSkill.RADIUS + 1).expandTowards(0, 16, 0);
            List<Missile> falling = helper.getLevel().getEntitiesOfClass(Missile.class, above, m -> m.getDeltaMovement().y < 0 || m.isStuck());
            helper.assertTrue(falling.size() >= 2, "no arrows coming down on the spot (" + falling.size() + ")");
            helper.assertTrue(!hurt(archer), "the rain fell on the archer");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- crossbow

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_pierce", timeoutTicks = 40)
    public static void piercingBoltGoesThroughFour(GameTestHelper helper) {
        Witch shooter = caster(helper, 4.5, .5);
        IronGolem[] golems = {golem(helper, 4.5, 2.2), golem(helper, 4.5, 3.8), golem(helper, 4.5, 5.4), golem(helper, 4.5, 7)};
        cast(helper, ModSpells.CROSSBOW_PIERCE, shooter);
        helper.runAfterDelay(CrossbowPierceSkill.SHOT_TICK + 6, () -> {
            for (int i = 0; i < golems.length; i++) {
                helper.assertTrue(hurt(golems[i]), "the bolt stopped before golem " + (i + 1));
            }
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_burst", timeoutTicks = 50)
    public static void burstShootsThreeBolts(GameTestHelper helper) {
        Witch shooter = caster(helper, 4.5, .5);
        IronGolem golem = golem(helper, 4.5, 4.5);
        cast(helper, ModSpells.CROSSBOW_BURST, shooter);
        int last = CrossbowBurstSkill.SHOTS[CrossbowBurstSkill.SHOTS.length - 1];
        helper.runAfterDelay(last + 8, () -> {
            // Three bolts of 3 x 0.6
            helper.assertTrue(golem.getMaxHealth() - golem.getHealth() >= 4.5, "too few bolts hit (" + (golem.getMaxHealth() - golem.getHealth()) + " damage)");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_harpoon", timeoutTicks = 50)
    public static void harpoonHaulsTheVictimIn(GameTestHelper helper) {
        // An armor stand keeps looking where it is turned (a mob's look control would level its eyes again)
        ArmorStand shooter = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, .5));
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(4.5, 2, 7.5));
        // It cannot walk off, but the rope can still drag it
        pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 10));
        double[] start = new double[1];
        helper.runAfterDelay(CrossbowHarpoonSkill.SHOT_TICK, () -> start[0] = pig.getZ());
        // Aimed at the pig, which is shorter than the shooter's eyes, a little high for the drop
        helper.onEachTick(() -> lookAt(shooter, pig.getBoundingBox().getCenter().add(0, .3, 0)));
        cast(helper, ModSpells.CROSSBOW_HARPOON, shooter);
        helper.runAfterDelay(CrossbowHarpoonSkill.SHOT_TICK + 16, () -> {
            helper.assertTrue(hurt(pig), "the harpoon missed");
            helper.assertTrue(start[0] - pig.getZ() > 2, "the pig was not hauled in (moved " + (start[0] - pig.getZ()) + ")");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_harpoon_wall", timeoutTicks = 50)
    public static void harpoonHaulsTheShooterToAWall(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, .5));
        for (int y = 2; y <= 4; y++) {
            for (int x = 3; x <= 5; x++) {
                helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }
        double[] start = new double[1];
        helper.runAfterDelay(4, () -> {
            start[0] = stand.getZ();
            cast(helper, ModSpells.CROSSBOW_HARPOON, stand);
        });
        helper.runAfterDelay(4 + CrossbowHarpoonSkill.SHOT_TICK + 14, () -> {
            helper.assertTrue(stand.getZ() - start[0] > 2, "the rope did not haul the stand to the wall (moved " + (stand.getZ() - start[0]) + ")");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- throwing knives

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_knife", timeoutTicks = 40)
    public static void preciseThrowLeavesItBleeding(GameTestHelper helper) {
        Witch thrower = caster(helper, 4.5, .5);
        IronGolem golem = golem(helper, 4.5, 4.5);
        cast(helper, ModSpells.KNIFE_THROW, thrower);
        helper.runAfterDelay(KnifeThrowSkill.THROW_TICK + 6, () -> {
            helper.assertTrue(hurt(golem), "the knife missed");
            helper.assertTrue(golem.hasEffect(ModEffects.BLEEDING), "the knife left no bleeding");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_fan", timeoutTicks = 40)
    public static void fanOfKnivesSpreads(GameTestHelper helper) {
        Witch thrower = caster(helper, 4.5, .5);
        IronGolem middle = golem(helper, 4.5, 5);
        IronGolem left = golem(helper, 7.4, 5.5);
        IronGolem right = golem(helper, 1.6, 5.5);
        cast(helper, ModSpells.KNIFE_FAN, thrower);
        helper.runAfterDelay(KnifeFanSkill.THROW_TICK + 8, () -> {
            helper.assertTrue(hurt(middle), "no knife went straight ahead");
            helper.assertTrue(hurt(left) || hurt(right), "the knives did not fan out");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "weapons_pin", timeoutTicks = 40)
    public static void pinningThrowPinsTheFoot(GameTestHelper helper) {
        Witch thrower = caster(helper, 4.5, .5);
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        cast(helper, ModSpells.KNIFE_PIN, thrower);
        helper.runAfterDelay(KnifePinSkill.THROW_TICK + 6, () -> {
            helper.assertTrue(pig.hasEffect(ModEffects.PINNED), "the pig was not pinned");
            helper.assertTrue(pig.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0, "a pinned pig can still walk");
            helper.succeed();
        });
    }
}
