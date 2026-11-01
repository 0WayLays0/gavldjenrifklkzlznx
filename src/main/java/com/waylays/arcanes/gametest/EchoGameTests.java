package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.echo.EchoScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.echo.EchoFx;
import com.waylays.arcanes.spells.echo.EchoSpell;
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
import net.minecraft.world.entity.animal.IronGolem;
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
 * Server-side checks run by CI for Echo Magic: the school is there with its ten spells; each spell's blow lands where it
 * should and leaves Resonance; the burst spends it; the step moves its caster; tendrils hold; the sensor hears only what
 * moves; the dome silences; the choir and the requiem strike. Each test that looses sound runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class EchoGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private EchoGameTests() {}

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

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void echoSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.ECHO_RESOURCE);
        helper.assertTrue(school != null, "Echo school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof EchoSpell, entry.getId() + " is in the Echo school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Echo spells, found " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.ECHO_MAGIC),
                "echo_magic damage type missing");
        helper.assertTrue(new ItemStack(ModItems.ECHOES_CODEX.get()).getItem() instanceof SpellBook, "the Codex of Echoes is not a spellbook");
        helper.assertTrue(new ItemStack(Items.ECHO_SHARD).is(ModSchools.ECHO_FOCUS), "the echo shard is not the school's focus");
        helper.succeed();
    }

    /** Resonance stacks up to its most, and Echo blows strike harder for it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_resonance", timeoutTicks = 40)
    public static void resonanceStacksToItsMost(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Husk husk = husk(helper, 4.5, 4.5);
        EchoFx.resonate(husk, 2);
        helper.assertTrue(EchoFx.resonance(husk) == 2, "two levels of resonance expected, found " + EchoFx.resonance(husk));
        EchoFx.resonate(husk, 9);
        helper.assertTrue(EchoFx.resonance(husk) == EchoFx.MAX, "resonance went past its most: " + EchoFx.resonance(husk));
        done(helper);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_wave", timeoutTicks = 40)
    public static void soundWaveLeavesResonance(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        caster.setXRot(12);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.SOUND_WAVE, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the sound wave did not strike the husk");
            helper.assertTrue(EchoFx.resonance(husk) >= 1, "the sound wave left no resonance");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_shriek", timeoutTicks = 40)
    public static void shriekDarkensTheConeAhead(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk ahead = husk(helper, 4.5, 3.5);
        Husk aside = husk(helper, 8.5, 1.5);
        cast(helper, ModSpells.SHRIEK, caster);
        helper.assertTrue(hurt(ahead) && ahead.hasEffect(MobEffects.DARKNESS), "the husk ahead is not hurt and in the dark");
        helper.assertTrue(EchoFx.resonance(ahead) >= 1, "the shriek left no resonance");
        helper.assertTrue(!hurt(aside), "the shriek hit a husk off to the side");
        done(helper);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_step", timeoutTicks = 80)
    public static void echoStepMovesAndItsEchoBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk near = husk(helper, 6, .5);
        double z0 = caster.getZ();
        cast(helper, ModSpells.ECHO_STEP, caster);
        helper.assertTrue(caster.getZ() > z0 + 3, "the echo step did not move its caster (" + (caster.getZ() - z0) + ")");
        helper.runAfterDelay(EchoScene.STEP_DELAY - 5, () -> helper.assertTrue(!hurt(near), "the echo burst too soon"));
        helper.runAfterDelay(EchoScene.STEP_DELAY + 3, () -> {
            helper.assertTrue(hurt(near), "the echo left behind did not burst on the husk beside it");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_tendrils", timeoutTicks = 60)
    public static void tendrilsLashAndHold(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        EchoScene.spawn(helper.getLevel(), caster, ModSpells.SCULK_TENDRILS.get(), EchoScene.Kind.TENDRILS, helper.absoluteVec(new Vec3(4.5, 2, 4.5)),
                new Vector3f(0, 1, 0), 3, 60, 2);
        helper.runAfterDelay(EchoScene.LASH + 3, () -> {
            helper.assertTrue(hurt(husk), "the tendrils did not lash the husk");
            helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the tendrils do not hold the husk");
            helper.assertTrue(EchoFx.resonance(husk) >= 1, "the tendrils left no resonance");
            done(helper);
        });
    }

    /** The sensor leaves alone what stands still, and strikes what moves. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_sensor", timeoutTicks = 80)
    public static void sensorHearsOnlyWhatMoves(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 6.5, 6.5);
        EchoScene.spawn(helper.getLevel(), caster, ModSpells.SCULK_SENSOR.get(), EchoScene.Kind.SENSOR, helper.absoluteVec(new Vec3(3.5, 2, 3.5)),
                new Vector3f(0, 1, 0), 1, 200, 2);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!hurt(husk), "the sensor struck a husk standing still");
            husk.setDeltaMovement(.25, 0, 0);
            husk.hurtMarked = true;
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(hurt(husk), "the sensor did not strike the husk that moved");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_burst", timeoutTicks = 40)
    public static void resonantBurstSpendsTheResonance(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk ringing = husk(helper, 2.5, 6.5);
        Husk quiet = husk(helper, 6.5, 6.5);
        EchoFx.resonate(ringing, 3);
        cast(helper, ModSpells.RESONANT_BURST, caster);
        helper.assertTrue(hurt(ringing) && EchoFx.resonance(ringing) == 0, "the ringing husk's resonance did not burst");
        helper.assertTrue(!hurt(quiet) && EchoFx.resonance(quiet) == 1, "the quiet husk should only be left ringing");
        done(helper);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_boom", timeoutTicks = 40)
    public static void sonicBoomGoesThroughItsLine(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk first = husk(helper, 4.5, 3.5);
        Husk second = husk(helper, 4.5, 7.5);
        Husk aside = husk(helper, 8.5, 4.5);
        cast(helper, ModSpells.SONIC_BOOM, caster);
        helper.assertTrue(hurt(first) && hurt(second), "the sonic boom did not go through both husks in its line");
        helper.assertTrue(EchoFx.resonance(first) >= 2, "the sonic boom left less than two resonance");
        helper.assertTrue(!hurt(aside), "the sonic boom hit a husk beside its line");
        done(helper);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_dome", timeoutTicks = 40)
    public static void domeSilencesEnemiesAndHardensItsSide(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 6.5, 6.5);
        cast(helper, ModSpells.SILENCE_DOME, caster);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(husk.hasEffect(ModEffects.SILENCE), "the husk inside the dome is not silenced");
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the caster inside the dome is not hardened");
            helper.assertTrue(!caster.hasEffect(ModEffects.SILENCE), "the dome silenced its own caster");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_choir", timeoutTicks = 80)
    public static void deepChoirStrikesTheNearestFoe(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 7.5, 7.5);
        cast(helper, ModSpells.DEEP_CHOIR, caster);
        long shriekers = helper.getLevel().getEntitiesOfClass(EchoScene.class, caster.getBoundingBox().inflate(8), s -> s.getKind() == EchoScene.Kind.SHRIEKER).size();
        helper.assertTrue(shriekers == 3, "expected 3 shriekers, found " + shriekers);
        helper.runAfterDelay(EchoScene.SHRIEK_EVERY + 4, () -> {
            helper.assertTrue(hurt(husk), "no shrieker struck the husk");
            helper.assertTrue(EchoFx.resonance(husk) >= 2 || !husk.isAlive(), "the shriek left less than two resonance");
            done(helper);
        });
    }

    /** The requiem's walls of sound strike, and its last beat bursts the resonance and silences the field. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "echo_requiem", timeoutTicks = 140)
    public static void requiemStrikesThenSilences(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(7.5, 2, 4.5));
        cast(helper, ModSpells.REQUIEM, caster);
        helper.runAfterDelay(EchoScene.REQUIEM_FIRST + 2, () -> {
            helper.assertTrue(hurt(golem), "the first wall of sound did not strike the golem");
            helper.assertTrue(EchoFx.resonance(golem) >= 1, "the wall of sound left no resonance");
        });
        int last = EchoScene.REQUIEM_FIRST + (EchoScene.REQUIEM_WAVES - 1) * EchoScene.REQUIEM_STEP;
        helper.runAfterDelay(last + 2, () -> {
            helper.assertTrue(golem.hasEffect(ModEffects.SILENCE), "the last beat did not silence the golem");
            helper.assertTrue(EchoFx.resonance(golem) == 0, "the last beat did not burst the golem's resonance");
            helper.assertTrue(!hurt(caster), "the requiem hurt its caster");
            done(helper);
        });
    }
}
