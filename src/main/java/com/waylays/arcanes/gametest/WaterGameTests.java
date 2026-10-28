package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.water.AbyssalDome;
import com.waylays.arcanes.entity.water.Geyser;
import com.waylays.arcanes.entity.water.Jellyfish;
import com.waylays.arcanes.entity.water.Maelstrom;
import com.waylays.arcanes.entity.water.RainCloud;
import com.waylays.arcanes.entity.water.TidalWave;
import com.waylays.arcanes.entity.water.Typhoon;
import com.waylays.arcanes.entity.water.WaterBubble;
import com.waylays.arcanes.entity.water.WaterJet;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.water.WaterSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Water Magic: the school is there; the lance runs through a line of enemies; the jet
 * cuts; the geyser throws up what stands on it; the bubble carries its prisoner up and drowns it; the rain washes its
 * caster clean; the whirlpool draws in and batters; the jellyfish sting; the typhoon lifts and batters; the tidal wave
 * sweeps its catch along; under the dome the caster breathes water, enemies are crushed and those out of air drown.
 * Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class WaterGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private WaterGameTests() {}

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

    /** A creature that breathes air (husks, being undead, breathe water): for the drowning checks. */
    private static Pig breather(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(x, 2, z));
    }

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void waterSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.WATER_RESOURCE);
        helper.assertTrue(school != null, "Water school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof WaterSpell, entry.getId() + " is in the Water school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Water spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.TIDE_CODEX.get()).getItem() instanceof SpellBook, "the Tide Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.PRISMARINE_SHARD).is(ModSchools.WATER_FOCUS), "prismarine is not the school's focus");
        helper.succeed();
    }

    /** The lance runs through the first husk and on into the second. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_lance", timeoutTicks = 60)
    public static void tideLancePierces(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk near = husk(helper, 4.5, 3.5);
        Husk far = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.TIDE_LANCE, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(near), "the lance did not strike the first husk");
            helper.assertTrue(hurt(far), "the lance did not run on into the second husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_jet", timeoutTicks = 60)
    public static void pressureJetCuts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 5.5);
        for (int t = 0; t < 30; t++) {
            helper.runAfterDelay(t, () -> WaterJet.hold(helper.getLevel(), caster, ModSpells.PRESSURE_JET.get(), 3));
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the jet did not cut the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The geyser bursts under the husk and throws it up. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_geyser", timeoutTicks = 60)
    public static void geyserThrowsUp(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double floor = husk.getY();
        Geyser.burst(helper.getLevel(), caster, ModSpells.GEYSER.get(), husk.position(), 8, 3);
        double[] highest = {floor};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], husk.getY()));
        helper.runAfterDelay(Geyser.WARN + Geyser.RISE + 10, () -> {
            helper.assertTrue(hurt(husk), "the geyser did not hurt the husk");
            helper.assertTrue(highest[0] > floor + 2, "the geyser did not throw the husk up (" + (highest[0] - floor) + ")");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The bubble closes round its prisoner, carries it up and drowns it, then bursts. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_bubble", timeoutTicks = 120)
    public static void bubbleCarriesAndDrowns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Pig prisoner = breather(helper, 4.5, 4.5);
        double floor = prisoner.getY();
        int air = prisoner.getAirSupply();
        WaterBubble bubble = WaterBubble.trap(helper.getLevel(), caster, ModSpells.BUBBLE_PRISON.get(), prisoner, 60, 3);
        helper.runAfterDelay(WaterBubble.FORM + 30, () -> {
            helper.assertTrue(prisoner.getY() > floor + 1, "the bubble did not carry the pig up");
            helper.assertTrue(prisoner.getAirSupply() < air, "the pig did not run short of air in the bubble");
        });
        helper.runAfterDelay(bubble.burstTick() + 3, () -> {
            helper.assertTrue(hurt(prisoner), "the bubble did not hurt the pig when it burst");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The rain washes the poison off its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_rain", timeoutTicks = 80)
    public static void rainWashesClean(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        RainCloud.gather(helper.getLevel(), caster, 100, 4);
        helper.runAfterDelay(RainCloud.PULSE + 4, () -> {
            helper.assertTrue(!caster.hasEffect(MobEffects.POISON), "the rain did not wash the poison off its caster");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The whirlpool draws the husk in towards its heart and batters it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_maelstrom", timeoutTicks = 80)
    public static void maelstromDrawsIn(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 7.5);
        Vec3 heart = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        double before = husk.position().distanceTo(heart);
        Maelstrom.open(helper.getLevel(), caster, ModSpells.MAELSTROM.get(), heart, 100, 2);
        helper.runAfterDelay(Maelstrom.OPEN + 30, () -> {
            helper.assertTrue(hurt(husk), "the whirlpool did not batter the husk");
            helper.assertTrue(husk.position().distanceTo(heart) < before - 1, "the whirlpool did not draw the husk in");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_jelly", timeoutTicks = 100)
    public static void jellyfishSting(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        Jellyfish.release(helper.getLevel(), caster, ModSpells.JELLYFISH_SWARM.get(), caster.position().add(0, 2, 1), 0, 200, 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the jellyfish did not sting the husk");
            // Husks are undead, so poison does not take on them: the numbing does
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the sting did not numb the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The typhoon catches the husk, lifts it and batters it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_typhoon", timeoutTicks = 100)
    public static void typhoonLifts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double floor = husk.getY();
        Typhoon.raise(helper.getLevel(), caster, ModSpells.TYPHOON.get(), husk.position(), 0, 120, 2);
        double[] highest = {floor};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], husk.getY()));
        helper.runAfterDelay(Typhoon.FORM + 30, () -> {
            helper.assertTrue(hurt(husk), "the typhoon did not batter the husk");
            helper.assertTrue(highest[0] > floor + 1.5, "the typhoon did not lift the husk (" + (highest[0] - floor) + ")");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The wave catches the husk and sweeps it along before it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_wave", timeoutTicks = 60)
    public static void tidalWaveSweeps(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double z = husk.getZ();
        TidalWave.raise(helper.getLevel(), caster, ModSpells.TIDAL_WAVE.get(), helper.absoluteVec(new Vec3(4.5, 2, 1.5)), 0, 16, 3);
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(hurt(husk), "the wave did not batter the husk");
            helper.assertTrue(husk.getZ() > z + 1.5, "the wave did not sweep the husk along (" + (husk.getZ() - z) + ")");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /**
     * Under the dome the caster breathes water, a pig out of air drowns, and a husk (undead, it breathes water) is
     * crushed by the deep.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "water_dome", timeoutTicks = 120)
    public static void abyssalDomeDrowns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Pig pig = breather(helper, 4.5, 7.5);
        Husk husk = husk(helper, 1.5, 4.5);
        pig.setAirSupply(10);
        AbyssalDome.raise(helper.getLevel(), caster, ModSpells.ABYSSAL_DOME.get(), caster.position(), 8, 200, 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(caster.hasEffect(MobEffects.WATER_BREATHING), "the caster does not breathe water under the dome");
            helper.assertTrue(pig.getAirSupply() <= 0 && hurt(pig), "the pig did not drown under the dome");
            helper.assertTrue(hurt(husk), "the deep did not crush the husk under the dome");
            GameTestSetup.clearLeftovers(helper);
        });
    }
}
