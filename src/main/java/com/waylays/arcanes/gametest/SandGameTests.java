package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.sand.SandScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.sand.SandSpell;
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
 * Server-side checks run by CI for Sand Magic: the school is there with its ten spells; the lance drills through three,
 * the scarabs bite, the hand holds then crushes, the veil hardens and scours, the hourglass slows then shatters, the
 * serpent bites, the lens burns, the sand falls and buries, the djinn hurls bolts, the simoom whirls. Each test that
 * looses sand runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class SandGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private SandGameTests() {}

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

    private static SandScene scene(GameTestHelper helper, LivingEntity caster, DeferredHolder<AbstractSpell, AbstractSpell> spell, SandScene.Kind kind,
                                   double x, double y, double z, float size, int life, float damage) {
        return SandScene.spawn(helper.getLevel(), caster, spell.get(), kind, helper.absoluteVec(new Vec3(x, y, z)), new Vector3f(0, 0, 1), size, life, damage);
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void sandSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.SAND_RESOURCE);
        helper.assertTrue(school != null, "Sand school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof SandSpell, entry.getId() + " is in the Sand school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Sand spells, found " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.SAND_MAGIC),
                "sand_magic damage type missing");
        helper.assertTrue(new ItemStack(ModItems.DUNES_CODEX.get()).getItem() instanceof SpellBook, "the Codex of Dunes is not a spellbook");
        helper.assertTrue(new ItemStack(Items.SAND).is(ModSchools.SAND_FOCUS), "sand is not the school's focus");
        helper.succeed();
    }

    /** The lance drills through three enemies, slowing them, and stops there. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_lance", timeoutTicks = 60)
    public static void sandLanceDrillsThroughThree(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk[] line = {husk(helper, 4.5, 2.5), husk(helper, 4.5, 4.5), husk(helper, 4.5, 6.5), husk(helper, 4.5, 8.5)};
        cast(helper, ModSpells.SAND_LANCE, caster);
        helper.runAfterDelay(20, () -> {
            for (int k = 0; k < 3; k++) {
                helper.assertTrue(hurt(line[k]), "the lance did not drill through husk " + (k + 1));
                helper.assertTrue(line[k].hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "husk " + (k + 1) + " is not slowed");
            }
            helper.assertTrue(!hurt(line[3]), "the lance went through a fourth husk");
            done(helper);
        });
    }

    /** The scarabs fly to their prey and bite it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_swarm", timeoutTicks = 80)
    public static void scarabsBiteTheirPrey(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        scene(helper, caster, ModSpells.SCARAB_SWARM, SandScene.Kind.SWARM, 4.5, 3, 1.5, .7f, 100, 2);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(hurt(husk), "the scarabs never bit the husk");
            helper.assertTrue(!hurt(caster), "the scarabs bit their caster");
            done(helper);
        });
    }

    /** The hand holds its victim fast, then closes its fist on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_grasp", timeoutTicks = 80)
    public static void graspHoldsThenCrushes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.DESERT_GRASP, SandScene.Kind.GRASP, 4.5, 2, 4.5, 1.6f, 40, 6).follow(husk);
        float[] afterRise = new float[1];
        helper.runAfterDelay(SandScene.GRASP_RISE + 2, () -> {
            helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the hand does not hold the husk");
            afterRise[0] = husk.getHealth();
        });
        helper.runAfterDelay(40 - SandScene.GRASP_CRUSH + 3, () -> {
            helper.assertTrue(husk.getHealth() < afterRise[0], "the fist did not close on the husk");
            done(helper);
        });
    }

    /** The veil hardens its wearer and scours what comes close. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_veil", timeoutTicks = 60)
    public static void veilHardensAndScours(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 6.0);
        cast(helper, ModSpells.SAND_VEIL, caster);
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the veil gave its wearer no resistance");
            helper.assertTrue(hurt(husk), "the veil did not scour the husk beside it");
            helper.assertTrue(!hurt(caster), "the veil hurt its wearer");
            done(helper);
        });
    }

    /** The hourglass slows what is under it, and shatters on it when its sand has run. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_hourglass", timeoutTicks = 100)
    public static void hourglassSlowsThenShatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 5.5, 5.5);
        scene(helper, caster, ModSpells.HOURGLASS, SandScene.Kind.HOURGLASS, 4.5, 2, 4.5, 5, 60, 6);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk under the hourglass is not slowed");
            helper.assertTrue(!hurt(husk), "the hourglass struck before it shattered");
        });
        helper.runAfterDelay(60 - 12 + 3, () -> {
            helper.assertTrue(hurt(husk), "the hourglass did not shatter on the husk");
            done(helper);
        });
    }

    /** The serpent slides to its prey and bites it, again and again. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_serpent", timeoutTicks = 160)
    public static void serpentHuntsAndBites(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        SandScene serpent = scene(helper, caster, ModSpells.DUNE_SERPENT, SandScene.Kind.SERPENT, 2.5, 2, 1.5, .8f, 200, 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(serpent.bites() >= 2, "the serpent has bitten " + serpent.bites() + " times");
            helper.assertTrue(hurt(husk), "the serpent's bites did not hurt the husk");
            helper.assertTrue(!hurt(caster), "the serpent bit its caster");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The lens's burning point slides after the nearest enemy and sets it alight. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_lens", timeoutTicks = 100)
    public static void lensBurnsTheNearest(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 5.5, 5.5);
        scene(helper, caster, ModSpells.SUN_LENS, SandScene.Kind.LENS, 3.5, 2, 3.5, 1.6f, 90, 2);
        helper.runAfterDelay(55, () -> {
            helper.assertTrue(hurt(husk), "the lens never burnt the husk");
            helper.assertTrue(husk.getRemainingFireTicks() > 0 || !husk.isAlive(), "the husk is not on fire");
            helper.assertTrue(!hurt(caster), "the lens burnt its caster");
            done(helper);
        });
    }

    /** The sand falling buries what is under it: hurt, blinded, weighed down. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_fall", timeoutTicks = 60)
    public static void sandfallBuriesWhatIsUnder(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.SANDFALL, SandScene.Kind.SANDFALL, 4.5, 2, 4.5, 3.5f, 60, 2);
        helper.runAfterDelay(SandScene.POUR_EVERY * 2 + 3, () -> {
            helper.assertTrue(hurt(husk), "the sand did not bury the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.BLINDNESS), "the husk is not blinded");
            helper.assertTrue(!hurt(caster), "the sand fell on its caster");
            done(helper);
        });
    }

    /** The djinn at its master's shoulder hurls a bolt of sand at the nearest enemy. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_djinn", timeoutTicks = 80)
    public static void djinnHurlsBolts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 2.5);
        Husk husk = husk(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.SAND_DJINN, SandScene.Kind.DJINN, 2.5, 2, 2.5, 1, 120, 3).follow(caster);
        helper.runAfterDelay(SandScene.DJINN_EVERY + 4, () -> {
            helper.assertTrue(hurt(husk), "the djinn's bolt did not strike the husk");
            helper.assertTrue(!hurt(caster), "the djinn struck its master");
            done(helper);
        });
    }

    /** The simoom whirls and blinds what it catches, and spares its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_simoom", timeoutTicks = 60)
    public static void simoomWhirlsAndBlinds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 6.5, 4.5);
        scene(helper, caster, ModSpells.SIMOOM, SandScene.Kind.SIMOOM, 4.5, 2, 4.5, 5, 120, 2);
        helper.runAfterDelay(23, () -> {
            helper.assertTrue(hurt(husk), "the simoom did not scour the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.BLINDNESS), "the husk caught in the simoom is not blinded");
            helper.assertTrue(!hurt(caster), "the simoom hurt its caster");
            done(helper);
        });
    }
}
