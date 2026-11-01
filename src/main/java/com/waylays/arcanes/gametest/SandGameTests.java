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
 * Server-side checks run by CI for Sand Magic: the school is there with its ten spells; the blast blinds, the shards
 * cut, quicksand holds, the pillar hurls up, the storm scours, the dune wave carries off, the worm bursts out, the
 * sarcophagus binds, the obelisk burns, the pyramid walls in and falls. Each test that looses sand runs in a batch of
 * its own.
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
                                   double x, double z, float size, int life, float damage) {
        return SandScene.spawn(helper.getLevel(), caster, spell.get(), kind, helper.absoluteVec(new Vec3(x, 2, z)), new Vector3f(0, 0, 1), size, life, damage);
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

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_blast", timeoutTicks = 40)
    public static void sandBlastBlindsTheConeAhead(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk ahead = husk(helper, 4.5, 3.5);
        Husk aside = husk(helper, 8.5, 1.5);
        cast(helper, ModSpells.SAND_BLAST, caster);
        helper.assertTrue(hurt(ahead) && ahead.hasEffect(MobEffects.BLINDNESS), "the husk ahead is not hurt and blinded");
        helper.assertTrue(!hurt(aside), "the sand blast hit a husk off to the side");
        done(helper);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_glass", timeoutTicks = 40)
    public static void glassShardsCut(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.GLASS_SHARD, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "no shard of glass cut the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_quicksand", timeoutTicks = 60)
    public static void quicksandHoldsAndHurts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.QUICKSAND, SandScene.Kind.QUICKSAND, 4.5, 4.5, 3.5f, 100, 2);
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk in the quicksand is not slowed");
            helper.assertTrue(hurt(husk), "the quicksand did not hurt the husk");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_pillar", timeoutTicks = 40)
    public static void pillarHurlsUp(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double y0 = husk.getY();
        double[] highest = {y0};
        helper.onEachTick(() -> highest[0] = Math.max(highest[0], husk.getY()));
        scene(helper, caster, ModSpells.SANDSTONE_PILLAR, SandScene.Kind.PILLAR, 4.5, 4.5, 4, 60, 2);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(husk), "the pillar did not strike the husk");
            helper.assertTrue(highest[0] > y0 + 1.5, "the pillar did not hurl the husk up (" + (highest[0] - y0) + ")");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_storm", timeoutTicks = 40)
    public static void sandstormScoursAroundItsCaster(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 7, 4.5);
        cast(helper, ModSpells.SANDSTORM, caster);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(husk) && husk.hasEffect(MobEffects.BLINDNESS), "the husk in the sandstorm is not hurt and blinded");
            helper.assertTrue(!hurt(caster), "the sandstorm hurt its caster");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_wave", timeoutTicks = 40)
    public static void duneWaveCarriesOff(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4);
        double z0 = husk.getZ();
        cast(helper, ModSpells.DUNE_WAVE, caster);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(hurt(husk), "the dune wave did not strike the husk");
            helper.assertTrue(husk.getZ() > z0 + 1, "the dune wave did not carry the husk along (" + (husk.getZ() - z0) + ")");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_worm", timeoutTicks = 60)
    public static void sandwormBurstsOut(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.SAND_WORM, SandScene.Kind.WORM, 4.5, 4.5, 1.4f, 80, 3);
        helper.runAfterDelay(SandScene.WORM_BURST - 3, () -> helper.assertTrue(!hurt(husk), "the worm struck before it burst out"));
        helper.runAfterDelay(SandScene.WORM_BURST + 3, () -> {
            helper.assertTrue(hurt(husk), "the worm did not strike the husk standing over it");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_sarcophagus", timeoutTicks = 60)
    public static void sarcophagusBindsItsVictim(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        cast(helper, ModSpells.SARCOPHAGUS, caster);
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the husk is not bound");
            helper.assertTrue(hurt(husk), "the sarcophagus did not wither the husk");
            done(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_obelisk", timeoutTicks = 60)
    public static void obeliskBurnsTheNearestFoe(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.OBELISK, SandScene.Kind.OBELISK, 2.5, 2.5, 5.5f, 200, 2);
        helper.runAfterDelay(SandScene.OBELISK_EVERY + 3, () -> {
            helper.assertTrue(hurt(husk), "the obelisk's beam did not strike the husk");
            helper.assertTrue(husk.isOnFire() || !husk.isAlive(), "the obelisk's beam did not set the husk burning");
            done(helper);
        });
    }

    /** The pyramid blinds what it walls in, then falls in on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "sand_pyramid", timeoutTicks = 140)
    public static void pyramidWallsInThenFalls(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.PYRAMID, SandScene.Kind.PYRAMID, 4.5, 4.5, 3, SandScene.PYRAMID_RISE + SandScene.PYRAMID_HOLD + 32, 6);
        helper.runAfterDelay(SandScene.PYRAMID_RISE + 4, () -> helper.assertTrue(husk.hasEffect(MobEffects.BLINDNESS), "the husk walled in is not blinded"));
        int fall = SandScene.PYRAMID_RISE + SandScene.PYRAMID_HOLD;
        helper.runAfterDelay(fall + 3, () -> {
            helper.assertTrue(hurt(husk), "the pyramid did not fall in on the husk");
            helper.assertTrue(!hurt(caster), "the pyramid hurt its caster");
            done(helper);
        });
    }
}
