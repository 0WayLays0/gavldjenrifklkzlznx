package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.snow.SnowScene;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.ElementSpell;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for the elemental schools. Snow: the school is there with its five spells and its
 * codex; the snowball runs into and chills an enemy in its way, then bursts; the snow globe holds an enemy in and
 * shatters on it; the crown looses its icicles; the ribbon of ice slows whoever crosses it; the blizzard freezes and
 * hurts those inside.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class ElementGameTests {
    private ElementGameTests() {}

    private static Witch caster(GameTestHelper helper, double x, double z) {
        Witch w = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(x, 2, z));
        w.setYRot(0);
        return w;
    }

    private static Vindicator foe(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(x, 2, z));
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    private static int count(ResourceLocation id, GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(id);
        helper.assertTrue(school != null, "no school " + id);
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof ElementSpell, entry.getId() + " is in the school but not one of its spells");
                count++;
            }
        }
        return count;
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void elementSchoolsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(count(ModSchools.SNOW_RESOURCE, helper) == 5, "expected 5 Snow spells");
        helper.assertTrue(new ItemStack(ModItems.FIRST_FROST_CODEX.get()).getItem() instanceof SpellBook, "the Codex of the First Frost is not a spellbook");
        helper.assertTrue(new ItemStack(Items.SNOWBALL).is(ModSchools.SNOW_FOCUS), "the snow focus tag");
        helper.succeed();
    }

    private static Vec3 at(GameTestHelper helper, double x, double z) {
        return helper.absoluteVec(new Vec3(x, 2, z));
    }

    /** The snowball rolls into the enemy in its way, hurts, slows and chills it, then bursts at the end of its run. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_roll", timeoutTicks = 80)
    public static void avalancheBallRunsThemOver(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator foe = foe(helper, 4.5, 5.5);
        float full = foe.getHealth();
        SnowScene ball = SnowScene.spawn(helper.getLevel(), caster, ModSpells.AVALANCHE_BALL.get(), SnowScene.Kind.ROLL, at(helper, 4.5, 1.5), 0,
                SnowScene.ROLL_TIME + 25, 5);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(foe.getHealth() < full, "the snowball did not hurt the enemy in its way");
            helper.assertTrue(foe.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && foe.getTicksFrozen() > 0, "the snowball did not slow and chill it");
        });
        helper.runAfterDelay(SnowScene.ROLL_TIME + 4, () -> {
            helper.assertTrue(ball.hasBurst(), "the snowball did not burst");
            done(helper);
        });
    }

    /** The globe keeps its prisoner in, then shatters on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_globe", timeoutTicks = 140)
    public static void snowGlobeHoldsThenShatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator foe = foe(helper, 4.5, 5.5);
        float full = foe.getHealth();
        Vec3 centre = at(helper, 4.5, 5.5);
        SnowScene globe = SnowScene.spawn(helper.getLevel(), caster, ModSpells.SNOW_GLOBE.get(), SnowScene.Kind.GLOBE, centre, 0, SnowScene.GLOBE_TIME, 6);
        helper.runAfterDelay(20, () -> foe.setDeltaMovement(1.5, 0, 0));
        helper.runAfterDelay(40, () -> helper.assertTrue(foe.position().multiply(1, 0, 1).distanceTo(centre.multiply(1, 0, 1)) < SnowScene.GLOBE_R + .5,
                "the prisoner left the globe"));
        helper.runAfterDelay(SnowScene.GLOBE_TIME - 8, () -> {
            helper.assertTrue(globe.hasBurst() && foe.getHealth() < full, "the globe did not shatter on its prisoner");
            done(helper);
        });
    }

    /** The crown looses an icicle at the nearest enemy; the ribbon of ice slows whoever crosses it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_crown", timeoutTicks = 100)
    public static void crownShootsAndRibbonSlows(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch crowned = caster(helper, 1.5, 1.5);
        Vindicator shot = foe(helper, 7.5, 1.5);
        float full = shot.getHealth();
        SnowScene crown = SnowScene.spawn(helper.getLevel(), crowned, ModSpells.HOARFROST_CROWN.get(), SnowScene.Kind.CROWN, at(helper, 1.5, 1.5), 0,
                SnowScene.CROWN_TIME, 4);
        Vindicator crossing = foe(helper, 5.5, 6.5);
        // A ribbon of ice laid by no one, crossing the second enemy's spot
        SnowScene.spawn(helper.getLevel(), null, ModSpells.FROST_GLIDE.get(), SnowScene.Kind.TRAIL, at(helper, 5.5, 3), 0, SnowScene.TRAIL_TIME, 4);
        helper.runAfterDelay(SnowScene.CROWN_EVERY + 5, () -> {
            helper.assertTrue(shot.getHealth() < full && crown.strikes() > 0, "the crown loosed no icicle");
            helper.assertTrue(crossing.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the ribbon of ice did not slow whoever crossed it");
            done(helper);
        });
    }

    /** The blizzard freezes and hurts those inside, and shatters the frozen at its end. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_whiteout", timeoutTicks = 220)
    public static void whiteoutFreezesAndShatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 5.5, 5.5);
        float full = foe.getHealth();
        SnowScene storm = SnowScene.spawn(helper.getLevel(), caster, ModSpells.WHITEOUT.get(), SnowScene.Kind.WHITEOUT, at(helper, 1.5, 1.5), 0,
                SnowScene.WHITEOUT_TIME, 8);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(foe.getTicksFrozen() > 0 && foe.hasEffect(MobEffects.BLINDNESS), "the blizzard did not chill and blind the enemy");
            helper.assertTrue(foe.getHealth() < full, "the blizzard did not hurt the enemy");
        });
        helper.runAfterDelay(SnowScene.SHATTER_AT + 3, () -> {
            helper.assertTrue(storm.hasBurst(), "the blizzard did not shatter the frozen");
            done(helper);
        });
    }
}
