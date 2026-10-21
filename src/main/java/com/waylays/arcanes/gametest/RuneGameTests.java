package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.rune.ElderCircle;
import com.waylays.arcanes.entity.rune.PetrifyBind;
import com.waylays.arcanes.entity.rune.RuneBulwark;
import com.waylays.arcanes.entity.rune.RuneSigil;
import com.waylays.arcanes.entity.rune.Runestone;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.rune.PetrifyingRuneSpell;
import com.waylays.arcanes.spells.rune.RuneSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Rune Magic: the school is there; the support runes shield, heal, speed up and
 * sharpen their caster's side; the wall stops arrows; the snare, the shards, the petrifying rune, the menhirs and the
 * Circle of the Elders hurt enemies.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class RuneGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private RuneGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return !e.isAlive() || e.getHealth() < e.getMaxHealth();
    }

    /** A witch facing +Z, standing still: the caster. */
    private static Witch caster(GameTestHelper helper, double x, double z) {
        Witch witch = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(x, 2, z));
        witch.setYRot(0);
        witch.setYHeadRot(0);
        witch.setYBodyRot(0);
        return witch;
    }

    private static Pig pig(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(x, 2, z));
    }

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster, MagicData data) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, data);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void runeSchoolIsRegistered(GameTestHelper helper) {
        SchoolType rune = SchoolRegistry.getSchool(ModSchools.RUNE_RESOURCE);
        helper.assertTrue(rune != null, "Rune school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == rune) {
                count++;
                helper.assertTrue(spell instanceof RuneSpell, entry.getId() + " is in the Rune school but is no Rune spell");
            }
        }
        helper.assertTrue(count == 10, "expected 10 Rune spells, got " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.RUNE_MAGIC),
                "rune_magic damage type missing");
        helper.assertTrue(rune.isFocus(new ItemStack(Items.CHISELED_STONE_BRICKS)), "chiseled stone bricks are not the Rune focus");
        helper.assertTrue(new ItemStack(ModItems.ELDER_RUNE_CODEX.get()).getItem() instanceof SpellBook, "the Elder Rune Codex is not a spellbook");
        helper.succeed();
    }

    /** The ward shields whoever of the caster's side stands in it (here the caster). */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_ward", timeoutTicks = 60)
    public static void wardRuneShieldsTheCasterInside(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 4.5);
        cast(helper, ModSpells.WARD_RUNE, caster, new MagicData(true));
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(RuneSigil.class, caster.getBoundingBox().inflate(2)).isEmpty(), "no ward carved");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the caster standing in the ward is not shielded");
            helper.succeed();
        });
    }

    /** The mending stone heals the caster's side around it at its first pulse. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_mending", timeoutTicks = 80)
    public static void mendingRunestoneHealsAroundIt(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 2.5);
        caster.setHealth(10);
        Runestone.mending(helper.getLevel(), caster, helper.absoluteVec(new Vec3(4.5, 2, 5.5)), 200, 4);
        helper.runAfterDelay(Runestone.MENDING_RISE + 6, () -> {
            helper.assertTrue(caster.getHealth() > 10, "the caster beside the stone was not healed");
            helper.succeed();
        });
    }

    /** Close blows struck under the Edge Rune deal its damage on top. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_edge", timeoutTicks = 40)
    public static void edgeRuneSharpensCloseBlows(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 2.5);
        Pig pig = pig(helper, 4.5, 4.5);
        cast(helper, ModSpells.EDGE_RUNE, caster, new MagicData(true));
        helper.assertTrue(caster.hasEffect(ModEffects.RUNE_EDGE), "the caster's weapon got no rune");
        float before = pig.getHealth();
        pig.hurt(helper.getLevel().damageSources().mobAttack(caster), 2);
        helper.assertTrue(before - pig.getHealth() > 2.5f, "the blow dealt only " + (before - pig.getHealth()));
        helper.succeed();
    }

    /** Walking on the path speeds up the caster's side. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_path", timeoutTicks = 40)
    public static void runicPathSpeedsUpWhoeverWalksIt(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 2.5);
        RuneSigil.carve(helper.getLevel(), caster, RuneSigil.Kind.PATH, helper.absoluteVec(new Vec3(4.5, 2, 1.5)), 0, 6, 2.4f, 100, 1, 0);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.MOVEMENT_SPEED), "the caster on the path is not faster");
            helper.succeed();
        });
    }

    /** An arrow shot at the wall stops there: the pig behind it is spared. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_bulwark", timeoutTicks = 60)
    public static void runicBulwarkStopsArrows(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Pig pig = pig(helper, 4.5, 7.5);
        Vec3 wallAt = helper.absoluteVec(new Vec3(4.5, 2, 5.5));
        RuneBulwark.raise(helper.getLevel(), caster, wallAt, 0, 100);
        helper.runAfterDelay(RuneBulwark.RISE_TICKS + 2, () -> {
            Arrow arrow = EntityType.ARROW.create(helper.getLevel());
            helper.assertTrue(arrow != null, "no arrow");
            Vec3 from = helper.absoluteVec(new Vec3(4.5, 2.4, 2.5));
            arrow.moveTo(from.x, from.y, from.z, 0, 0);
            arrow.setDeltaMovement(0, .05, 1.6);
            helper.getLevel().addFreshEntity(arrow);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!hurt(pig), "the arrow went through the wall and hit the pig");
                helper.assertTrue(arrow.isRemoved() || arrow.getZ() < wallAt.z + .7, "the arrow flew past the wall");
                helper.succeed();
            });
        });
    }

    /** An enemy stepping on an armed snare sets it off and is hurt and slowed. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_snare", timeoutTicks = 80)
    public static void snareRuneBurstsUnderAnEnemy(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 5.5));
        RuneSigil snare = RuneSigil.carve(helper.getLevel(), caster, RuneSigil.Kind.SNARE, at, 0, RuneSigil.SNARE_RADIUS, 0, 600, 1, 6);
        Pig pig = pig(helper, 4.5, 7.5);
        helper.runAfterDelay(RuneSigil.ARM_TICKS + 4, () -> {
            helper.assertTrue(!hurt(pig), "the snare went off before anything stepped on it");
            pig.teleportTo(at.x, at.y, at.z);
        });
        helper.runAfterDelay(RuneSigil.ARM_TICKS + 12, () -> {
            helper.assertTrue(snare.burstAge(0) >= 0, "the snare did not go off under the pig");
            helper.assertTrue(hurt(pig), "the pig that stepped on the snare was not hurt");
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the pig that stepped on the snare is not slowed");
            helper.succeed();
        });
    }

    /** A fan of shards thrown at a husk hits it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_shards", timeoutTicks = 40)
    public static void runeShardsHitWhatTheCasterAimsAt(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        caster.setXRot(6);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 5.5));
        cast(helper, ModSpells.RUNE_SHARDS, caster, new MagicData(true));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(hurt(husk), "no shard hit the husk");
            helper.succeed();
        });
    }

    /** The petrifying rune roots its target, then shatters on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_petrify", timeoutTicks = 120)
    public static void petrifyingRuneRootsThenShatters(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Pig pig = pig(helper, 4.5, 5.5);
        MagicData data = new MagicData(true);
        data.setAdditionalCastData(new TargetEntityCastData(pig));
        cast(helper, ModSpells.PETRIFYING_RUNE, caster, data);
        helper.assertTrue(pig.hasEffect(ModEffects.PETRIFIED), "the target is not petrified");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(PetrifyBind.class, pig.getBoundingBox().inflate(2)).isEmpty(), "no stone on the target");
        helper.runAfterDelay(PetrifyingRuneSpell.duration(1) + 4, () -> {
            helper.assertTrue(hurt(pig), "the shattering stone did not hurt its target");
            helper.assertTrue(!pig.hasEffect(ModEffects.PETRIFIED), "the target is still petrified after the stone shattered");
            helper.succeed();
        });
    }

    /** The line of menhirs throws up and hurts what stands on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_menhir", timeoutTicks = 60)
    public static void menhirSurgeThrowsUpWhatStandsOnIt(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        Pig pig = pig(helper, 4.5, 4.2);
        cast(helper, ModSpells.MENHIR_SURGE, caster, new MagicData(true));
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(Runestone.class, caster.getBoundingBox().inflate(16)).isEmpty(), "no menhir");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(hurt(pig), "the pig on the menhirs' line was not hurt");
            helper.succeed();
        });
    }

    /** The circle heals its caster inside and strikes the enemy inside. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "rune_circle", timeoutTicks = 100)
    public static void circleOfEldersHealsAndStrikes(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        Pig pig = pig(helper, 4.5, 7.5);
        cast(helper, ModSpells.CIRCLE_OF_ELDERS, caster, new MagicData(true));
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ElderCircle.class, caster.getBoundingBox().inflate(2)).isEmpty(), "no circle raised");
        helper.runAfterDelay(ElderCircle.RISE_TICKS + 4, () -> {
            helper.assertTrue(caster.getHealth() > 10, "the caster inside the circle was not healed");
            helper.assertTrue(hurt(pig), "the pig inside the circle was not struck");
            helper.succeed();
        });
    }
}
