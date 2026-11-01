package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.wind.BladeStorm;
import com.waylays.arcanes.entity.wind.Cyclone;
import com.waylays.arcanes.entity.wind.Hurricane;
import com.waylays.arcanes.entity.wind.Skyfall;
import com.waylays.arcanes.entity.wind.VacuumSphere;
import com.waylays.arcanes.entity.wind.WindWall;
import com.waylays.arcanes.entity.wind.WindWard;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.wind.WindSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * Server-side checks run by CI for Wind Magic: the school is there with its ten spells; the blade cuts, the gale hurls
 * back, the updraft lifts its caster, tailwind wards and hastens, the cyclone carries up, the vacuum drags in and
 * bursts, the wall turns an arrow back, the blades cut, skyfall lifts and slams, the hurricane batters. Each test that
 * lets wind loose runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class WindGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private WindGameTests() {}

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
    public static void windSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.WIND_RESOURCE);
        helper.assertTrue(school != null, "Wind school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof WindSpell, entry.getId() + " is in the Wind school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Wind spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.WINDS_CODEX.get()).getItem() instanceof SpellBook, "the Codex of the Four Winds is not a spellbook");
        helper.assertTrue(new ItemStack(Items.FEATHER).is(ModSchools.WIND_FOCUS), "the feather is not the school's focus");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_blade", timeoutTicks = 40)
    public static void gustBladeCuts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.GUST_BLADE, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the gust blade did not cut the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_gale", timeoutTicks = 40)
    public static void galeHurlsBack(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 3.5);
        double z0 = husk.getZ();
        cast(helper, ModSpells.GALE, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the gale did not strike the husk");
            helper.assertTrue(husk.getZ() > z0 + 1.5, "the gale did not hurl the husk back (" + (husk.getZ() - z0) + ")");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_updraft", timeoutTicks = 40)
    public static void updraftLifts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        double y0 = caster.getY();
        double[] highest = {y0};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], caster.getY()));
        cast(helper, ModSpells.UPDRAFT, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(highest[0] > y0 + 3, "the updraft did not carry its caster up (" + (highest[0] - y0) + ")");
            helper.assertTrue(caster.hasEffect(MobEffects.SLOW_FALLING), "the caster does not drift down slowly");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_tailwind", timeoutTicks = 40)
    public static void tailwindWardsAndHastens(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        cast(helper, ModSpells.TAILWIND, caster);
        helper.assertTrue(caster.hasEffect(MobEffects.MOVEMENT_SPEED), "tailwind did not hasten its caster");
        Arrow arrow = EntityType.ARROW.create(helper.getLevel());
        Vec3 from = helper.absoluteVec(new Vec3(4.5, 3, 7.5));
        arrow.moveTo(from.x, from.y, from.z, 0, 0);
        arrow.setDeltaMovement(0, 0, -1.2);
        helper.getLevel().addFreshEntity(arrow);
        helper.succeedWhen(() -> {
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(WindWard.class, caster.getBoundingBox().inflate(2)).isEmpty(), "no ward of wind on the caster");
            helper.assertTrue(arrow.getDeltaMovement().z > 0 || arrow.isRemoved() && !hurt(caster), "the arrow was not turned aside");
            helper.assertTrue(!hurt(caster), "the arrow struck the warded caster");
            arrow.discard();
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_cyclone", timeoutTicks = 80)
    public static void cycloneCarriesUp(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double y0 = husk.getY();
        double[] highest = {y0};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], husk.getY()));
        Cyclone.loose(helper.getLevel(), caster, ModSpells.CYCLONE.get(), helper.absoluteVec(new Vec3(4.5, 2, 2.5)), new Vec3(0, 0, 1), 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the cyclone did not batter the husk");
            helper.assertTrue(highest[0] > y0 + 1, "the cyclone did not carry the husk up (" + (highest[0] - y0) + ")");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_vacuum", timeoutTicks = VacuumSphere.PULL + 30)
    public static void vacuumDragsInAndBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 7.5, 4.5);
        Vec3 c = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        double d0 = husk.position().distanceTo(c);
        VacuumSphere.open(helper.getLevel(), caster, ModSpells.VACUUM_SPHERE.get(), c, 4);
        helper.runAfterDelay(VacuumSphere.PULL - 4, () -> helper.assertTrue(husk.position().distanceTo(c) < d0 - .8,
                "the vacuum did not drag the husk in (" + (d0 - husk.position().distanceTo(c)) + ")"));
        helper.runAfterDelay(VacuumSphere.PULL + 4, () -> {
            helper.assertTrue(hurt(husk), "the vacuum's burst did not strike the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk is not winded");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_wall", timeoutTicks = 60)
    public static void wallTurnsArrowBack(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        WindWall.raise(helper.getLevel(), caster, ModSpells.WIND_WALL.get(), helper.absoluteVec(new Vec3(4.5, 2, 4.5)), 0, 100, 2);
        Arrow arrow = EntityType.ARROW.create(helper.getLevel());
        Vec3 from = helper.absoluteVec(new Vec3(4.5, 3.5, 8.5));
        arrow.moveTo(from.x, from.y, from.z, 0, 0);
        arrow.setDeltaMovement(0, 0, -1.2);
        helper.getLevel().addFreshEntity(arrow);
        helper.succeedWhen(() -> {
            helper.assertTrue(arrow.getDeltaMovement().z > 0, "the arrow was not thrown back by the wall");
            helper.assertTrue(!hurt(caster), "the arrow got through the wall");
            arrow.discard();
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_blades", timeoutTicks = 80)
    public static void bladesCut(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 7.5);
        BladeStorm.raise(helper.getLevel(), caster, ModSpells.BLADE_STORM.get(), 6, 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the blades did not cut the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_skyfall", timeoutTicks = Skyfall.LIFT + 40)
    public static void skyfallLiftsAndSlams(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double y0 = husk.getY();
        double[] highest = {y0};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], husk.getY()));
        Skyfall.seize(helper.getLevel(), caster, ModSpells.SKYFALL.get(), husk, 3);
        helper.runAfterDelay(Skyfall.LIFT + 12, () -> {
            helper.assertTrue(highest[0] > y0 + 5, "skyfall did not carry the husk up (" + (highest[0] - y0) + ")");
            helper.assertTrue(hurt(husk), "skyfall did not strike the husk");
            helper.assertTrue(husk.getY() < y0 + 2, "the husk was not slammed back down");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wind_hurricane", timeoutTicks = 100)
    public static void hurricaneBatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 1.5);
        Hurricane storm = Hurricane.raise(helper.getLevel(), caster, ModSpells.HURRICANE.get(), 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the hurricane did not batter the husk");
            helper.assertTrue(!hurt(caster), "the hurricane struck its own caster");
            storm.discard();
            GameTestSetup.clearLeftovers(helper);
        });
    }
}
