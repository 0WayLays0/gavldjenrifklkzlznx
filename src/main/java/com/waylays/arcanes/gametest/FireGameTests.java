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
import com.waylays.arcanes.spells.fire.HeatColors;
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

import java.util.List;

/**
 * Server-side checks run by CI for Stellar Fire: the school is there with every spell in each of the six heats; a
 * hotter heat strikes harder and the heat does not change with casting; the bolt, the corona, the pillars, the wisps,
 * the crescent, the brand, the phoenix, the supernova and the pyre burn what they reach. Each test runs in a batch of
 * its own.
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

    /** A spell in heat {@code tier} (0 red to 5 violet). */
    private static AbstractSpell at(List<DeferredHolder<AbstractSpell, AbstractSpell>> heats, int tier) {
        return heats.get(tier).get();
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
        helper.assertTrue(count == 10 * HeatColors.TIERS, "expected 10 Stellar Fire spells in 6 heats, found " + count);
        for (int tier = 0; tier < HeatColors.TIERS; tier++) {
            StarfireSpell bolt = (StarfireSpell) at(ModSpells.EMBER_BOLT, tier);
            helper.assertTrue(bolt.tier() == tier, "the ember bolt at " + tier + " burns at heat " + bolt.tier());
            helper.assertTrue(bolt.getSpellName().equals("ember_bolt_" + HeatColors.ID[tier]), "the ember bolt at " + tier + " is named " + bolt.getSpellName());
        }
        helper.assertTrue(new ItemStack(ModItems.STARFIRE_CODEX.get()).getItem() instanceof SpellBook, "the Starfire Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.BLAZE_POWDER).is(ModSchools.STARFIRE_FOCUS), "blaze powder is not the school's focus");
        helper.succeed();
    }

    /**
     * Each heat strikes harder than the one below and costs a little more; a spell's strength is its own, whoever casts
     * it and whatever they cast before. Nothing is cast here: this test shares its batch with many others.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void hotterStrikesHarder(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        for (int tier = 1; tier < HeatColors.TIERS; tier++) {
            StarfireSpell cooler = (StarfireSpell) at(ModSpells.CORONA, tier - 1), hotter = (StarfireSpell) at(ModSpells.CORONA, tier);
            helper.assertTrue(hotter.hot(1, caster) > cooler.hot(1, caster), "heat " + tier + " does not strike harder than heat " + (tier - 1));
            helper.assertTrue(hotter.getManaCost(1) >= cooler.getManaCost(1), "heat " + tier + " costs less than heat " + (tier - 1));
            helper.assertTrue(hotter.getMinLevel() == cooler.getMinLevel() && hotter.getMaxLevel() == cooler.getMaxLevel(),
                    "the heats of the corona do not share their levels");
        }
        StarfireSpell red = (StarfireSpell) at(ModSpells.EMBER_BOLT, 0);
        helper.assertTrue(red.hot(1, caster) == red.hot(1, null), "the ember bolt's strength depends on who casts it");
        caster.discard();
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_bolt", timeoutTicks = 60)
    public static void emberBoltBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.EMBER_BOLT.get(2), caster);
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
        cast(helper, ModSpells.CORONA.get(2), caster);
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
        FirePillar.raise(helper.getLevel(), caster, at(ModSpells.FIRE_PILLARS, 2), husk.position(), 0, true, 3, 2);
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
        Wisp.kindle(helper.getLevel(), caster, at(ModSpells.WILL_O_WISPS, 2), 0, 200, 2, 1);
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
        FlameCrescent.slash(helper.getLevel(), caster, at(ModSpells.FLAME_CRESCENT, 2), caster.position().add(0, 1, 1), new Vec3(0, 0, 1), 0, 3, 2);
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
        SearingBrand.mark(helper.getLevel(), caster, at(ModSpells.SEARING_BRAND, 2), husk, 3, 3);
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
        Phoenix.rise(helper.getLevel(), caster, at(ModSpells.PHOENIX, 2), 3, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the phoenix did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The supernova burns what is round the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_nova", timeoutTicks = 40)
    public static void supernovaBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 7.5);
        cast(helper, ModSpells.SUPERNOVA.get(2), caster);
        helper.assertTrue(hurt(husk), "the supernova did not burn the husk");
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    /** The pyre burns what stands in it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "fire_pyre", timeoutTicks = 60)
    public static void pyreBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 7.5);
        StellarPyre.kindle(helper.getLevel(), caster, at(ModSpells.STELLAR_PYRE, 2), 2, 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the pyre did not burn the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }
}
