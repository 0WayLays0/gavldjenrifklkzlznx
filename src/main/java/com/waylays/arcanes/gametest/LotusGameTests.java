package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.lotus.LotusScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.lotus.LotusSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
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
import org.joml.Vector3f;

/**
 * Server-side checks run by CI for Lotus Magic: the school is there with its ten spells; a razor petal cuts the first in
 * its way, the bud holds then flings, the pond heals and slows, the throne hardens and shoots, the roots seize, the pod
 * shoots, the mandala's rings strike, a lantern hunts and bursts, the storm cuts, the great lotus strikes and heals.
 * Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class LotusGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private LotusGameTests() {}

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

    private static LotusScene scene(GameTestHelper helper, LivingEntity caster, DeferredHolder<AbstractSpell, AbstractSpell> spell, LotusScene.Kind kind,
                                    double x, double y, double z, Vector3f dir, float size, int life, float damage) {
        return LotusScene.spawn(helper.getLevel(), caster, spell.get(), kind, helper.absoluteVec(new Vec3(x, y, z)), dir, size, life, damage);
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void lotusSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.LOTUS_RESOURCE);
        helper.assertTrue(school != null, "Lotus school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof LotusSpell, entry.getId() + " is in the Lotus school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Lotus spells, found " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.LOTUS_MAGIC),
                "lotus_magic damage type missing");
        helper.assertTrue(new ItemStack(ModItems.LOTUS_CODEX.get()).getItem() instanceof SpellBook, "the Lotus Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.LILY_PAD).is(ModSchools.LOTUS_FOCUS), "the lily pad is not the school's focus");
        helper.succeed();
    }

    /** A razor petal cuts the first enemy in its way, and stops there. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_petal", timeoutTicks = 60)
    public static void razorPetalCutsTheFirst(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk first = husk(helper, 4.5, 4.5);
        Husk second = husk(helper, 4.5, 7.5);
        LotusScene petal = scene(helper, caster, ModSpells.RAZOR_PETALS, LotusScene.Kind.PETAL, 4.5, 3, 1.6, new Vector3f(0, 0, 1), .5f, 30, 3);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(first), "the petal did not cut the husk in its way");
            helper.assertTrue(!hurt(second), "the petal went through to a second husk");
            helper.assertTrue(petal.isDone() || petal.isRemoved(), "the petal did not stop where it struck");
            helper.assertTrue(!hurt(caster), "the petal cut its caster");
            done(helper);
        });
    }

    /** The bud holds its prey, then bursts open and flings it up. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_bud", timeoutTicks = 60)
    public static void budHoldsThenFlings(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.LOTUS_BUD, LotusScene.Kind.BUD, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 1.3f, 40, 4);
        helper.runAfterDelay(LotusScene.BUD_RISE + 2, () -> {
            helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the bud does not hold the husk");
            helper.assertTrue(!hurt(husk), "the bud struck before it burst");
        });
        helper.runAfterDelay(LotusScene.BUD_BURST + 2, () -> {
            helper.assertTrue(hurt(husk), "the bud did not burst on the husk");
            helper.assertTrue(!hurt(caster), "the bud hurt its caster");
            done(helper);
        });
    }

    /** The pond heals the caster standing in it and slows the enemy wading in it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_pond", timeoutTicks = 80)
    public static void pondHealsAndSlows(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        Husk husk = husk(helper, 6.5, 4.5);
        scene(helper, caster, ModSpells.SACRED_POND, LotusScene.Kind.POND, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 5, 120, 3);
        helper.runAfterDelay(LotusScene.POND_EVERY * 2 + 3, () -> {
            helper.assertTrue(caster.getHealth() > 10, "the pond did not heal its caster");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk wading in the pond is not slowed");
            helper.assertTrue(!hurt(husk), "the pond hurt the husk");
            done(helper);
        });
    }

    /** The throne hardens its caster and looses a petal at the nearest enemy. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_throne", timeoutTicks = 80)
    public static void throneHardensAndShoots(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 2.5);
        Husk husk = husk(helper, 6.5, 6.5);
        cast(helper, ModSpells.LOTUS_THRONE, caster);
        helper.runAfterDelay(LotusScene.THRONE_EVERY + 4, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the throne did not harden its caster");
            helper.assertTrue(hurt(husk), "the throne's petal did not strike the husk");
            helper.assertTrue(!hurt(caster), "the throne hurt its caster");
            done(helper);
        });
    }

    /** The roots seize what is in the mud: held, hurt, slowed. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_roots", timeoutTicks = 60)
    public static void rootsSeizeWhatIsInTheMud(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.MIRE_ROOTS, LotusScene.Kind.ROOTS, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 4, 80, 3);
        helper.runAfterDelay(LotusScene.ROOTS_FIRST + 2, () -> {
            helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the roots did not seize the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk in the mud is not slowed");
            helper.assertTrue(hurt(husk), "the roots did not hurt the husk");
            helper.assertTrue(!hurt(caster), "the roots hurt their caster");
            done(helper);
        });
    }

    /** The pod at its master's shoulder shoots a seed at the nearest enemy. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_pod", timeoutTicks = 80)
    public static void podShootsTheNearest(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 2.5);
        Husk husk = husk(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.SEED_POD, LotusScene.Kind.POD, 2.5, 2, 2.5, new Vector3f(0, 0, 1), 1, 120, 3).follow(caster);
        helper.runAfterDelay(LotusScene.POD_EVERY + 4, () -> {
            helper.assertTrue(hurt(husk), "the pod's seed did not strike the husk");
            helper.assertTrue(!hurt(caster), "the pod shot its master");
            done(helper);
        });
    }

    /** The mandala's first ring flares under what stands on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_mandala", timeoutTicks = 80)
    public static void mandalaRingsStrike(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.LOTUS_MANDALA, LotusScene.Kind.MANDALA, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 7, 70, 3);
        helper.runAfterDelay(LotusScene.MANDALA_DRAWN - 4, () -> helper.assertTrue(!hurt(husk), "the mandala struck before it was drawn"));
        helper.runAfterDelay(LotusScene.MANDALA_DRAWN + 2, () -> {
            helper.assertTrue(hurt(husk), "the mandala's first ring did not strike the husk on it");
            helper.assertTrue(husk.hasEffect(MobEffects.GLOWING), "the husk struck by the mandala is not marked");
            done(helper);
        });
    }

    /** A lantern rises, drifts to its enemy and bursts on it in fire. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_lantern", timeoutTicks = 120)
    public static void lanternHuntsAndBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 6.5, 6.5);
        LotusScene lantern = scene(helper, caster, ModSpells.LOTUS_LANTERNS, LotusScene.Kind.LANTERN, 2.5, 3, 2.5, new Vector3f(1, 0, 0), 2.5f, 100, 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(lantern.isDone() || lantern.isRemoved(), "the lantern has not burst yet");
            helper.assertTrue(hurt(husk), "the lantern's burst did not hurt the husk");
            helper.assertTrue(!hurt(caster), "the lantern hurt its caster");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The storm of petals cuts and blinds what is round its caster, and spares the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_storm", timeoutTicks = 60)
    public static void stormCutsAndBlinds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 6.5, 4.5);
        cast(helper, ModSpells.PETAL_STORM, caster);
        helper.runAfterDelay(LotusScene.STORM_EVERY + 3, () -> {
            helper.assertTrue(hurt(husk), "the storm did not cut the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.BLINDNESS), "the husk in the storm is not blinded");
            helper.assertTrue(!hurt(caster), "the storm cut its caster");
            done(helper);
        });
    }

    /** In full bloom the great lotus strikes its foes and heals its caster's side. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lotus_thousand", timeoutTicks = 100)
    public static void thousandPetalsStrikeAndHeal(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 2.5);
        caster.setHealth(10);
        Husk husk = husk(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.THOUSAND_PETAL_LOTUS, LotusScene.Kind.THOUSAND, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 10, 170, 8);
        helper.runAfterDelay(LotusScene.THOUSAND_BLOOM - 4, () -> helper.assertTrue(!hurt(husk), "the lotus struck before it bloomed"));
        helper.runAfterDelay(LotusScene.THOUSAND_BLOOM + 3, () -> {
            helper.assertTrue(hurt(husk), "the lotus's bloom did not strike the husk");
            helper.assertTrue(caster.getHealth() > 10, "the lotus's bloom did not heal its caster");
            helper.assertTrue(caster.hasEffect(MobEffects.REGENERATION), "the caster was given no regeneration");
            done(helper);
        });
    }
}
