package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.fire.FirePillar;
import com.waylays.arcanes.entity.fire.FlameCrescent;
import com.waylays.arcanes.entity.fire.Phoenix;
import com.waylays.arcanes.entity.fire.SearingBrand;
import com.waylays.arcanes.entity.fire.StellarPyre;
import com.waylays.arcanes.entity.fire.Wisp;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.fire.Heat;
import com.waylays.arcanes.spells.fire.StarfireSpell;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Stellar Fire: the school is there; casting stokes the caster's heat, which cools a
 * step at a time; the bolt, the corona, the pillars, the wisps, the crescent, the brand, the phoenix and the pyre burn
 * what they reach; the supernova burns and spends all the caster's heat. Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class FireGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private FireGameTests() {}

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
    public static void starfireSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.STARFIRE_RESOURCE);
        helper.assertTrue(school != null, "Stellar Fire school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof StarfireSpell, entry.getId() + " is in the Stellar Fire school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Stellar Fire spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.STARFIRE_CODEX.get()).getItem() instanceof SpellBook, "the Starfire Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.BLAZE_POWDER).is(ModSchools.STARFIRE_FOCUS), "blaze powder is not the school's focus");
        helper.succeed();
    }

    /** Casting stokes the caster's heat a step; left alone it cools a step at a time. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_heat", timeoutTicks = Heat.COOL + 40)
    public static void heatStokesAndCools(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        cast(helper, ModSpells.EMBER_BOLT, caster);
        helper.assertTrue(Heat.of(caster) == 1, "casting did not stoke the heat (" + Heat.of(caster) + ")");
        Heat.set(caster, 4);
        helper.runAfterDelay(Heat.COOL + 10, () -> {
            helper.assertTrue(Heat.of(caster) == 3, "the heat did not cool a single step (" + Heat.of(caster) + ")");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_bolt", timeoutTicks = 60)
    public static void emberBoltBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.EMBER_BOLT, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the bolt did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_corona", timeoutTicks = 60)
    public static void coronaBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 7.5);
        cast(helper, ModSpells.CORONA, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the corona did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The pillar bursts up under the husk, burns it and throws it up. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_pillar", timeoutTicks = 60)
    public static void firePillarBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double floor = husk.getY();
        FirePillar.raise(helper.getLevel(), caster, ModSpells.FIRE_PILLARS.get(), husk.position(), 0, true, 3, 2);
        double[] highest = {floor};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], husk.getY()));
        helper.runAfterDelay(FirePillar.WARN + 8, () -> {
            helper.assertTrue(hurt(husk), "the pillar did not burn the husk");
            helper.assertTrue(highest[0] > floor + 1, "the pillar did not throw the husk up (" + (highest[0] - floor) + ")");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_wisp", timeoutTicks = 120)
    public static void wispsHunt(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        Wisp.kindle(helper.getLevel(), caster, ModSpells.WILL_O_WISPS.get(), 0, 200, 2, 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the wisp did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_crescent", timeoutTicks = 60)
    public static void crescentCuts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        FlameCrescent.slash(helper.getLevel(), caster, ModSpells.FLAME_CRESCENT.get(), caster.position().add(0, 1, 1), new Vec3(0, 0, 1), 0, 3, 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the crescent did not cut the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The brand smoulders, then a pillar bursts up under its husk. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_brand", timeoutTicks = SearingBrand.FUSE + 30)
    public static void brandBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        SearingBrand.mark(helper.getLevel(), caster, ModSpells.SEARING_BRAND.get(), husk, 3, 3);
        helper.runAfterDelay(SearingBrand.FUSE - 5, () -> helper.assertTrue(!hurt(husk), "the brand burst before its fuse ran out"));
        helper.runAfterDelay(SearingBrand.FUSE + 8, () -> {
            helper.assertTrue(hurt(husk), "the brand's pillar did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_phoenix", timeoutTicks = Phoenix.OUT + 20)
    public static void phoenixBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        Phoenix.rise(helper.getLevel(), caster, ModSpells.PHOENIX.get(), 3, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the phoenix did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The supernova burns what is round the caster and spends all their heat. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_nova", timeoutTicks = 40)
    public static void supernovaSpendsHeat(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 7.5);
        Heat.set(caster, 4);
        cast(helper, ModSpells.SUPERNOVA, caster);
        helper.assertTrue(hurt(husk), "the supernova did not burn the husk");
        helper.assertTrue(Heat.of(caster) == 0, "the supernova did not spend the caster's heat (" + Heat.of(caster) + ")");
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    /** The pyre burns what stands in it and lifts its caster's heat. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_pyre", timeoutTicks = 60)
    public static void pyreBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 7.5);
        StellarPyre.kindle(helper.getLevel(), caster, ModSpells.STELLAR_PYRE.get(), 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the pyre did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }
}
