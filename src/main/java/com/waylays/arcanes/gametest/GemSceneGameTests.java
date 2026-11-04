package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gem.GemScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.joml.Vector3f;

/**
 * Server-side checks run by CI for Gem Magic's newer spells: ruby shards burn through two, the nova strikes round it,
 * the meteor lands then its pool burns; the shooting star bursts on its prey, the vault's stars fall, the mirror binds;
 * the diamond lance goes through the line, the dust cuts and blinds, the bastion hardens then shatters. Each test runs
 * in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class GemSceneGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private GemSceneGameTests() {}

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

    private static Vindicator foe(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(x, 2, z));
    }

    private static GemScene scene(GameTestHelper helper, LivingEntity caster, DeferredHolder<AbstractSpell, AbstractSpell> spell, GemScene.Kind kind, double x,
                                  double y, double z, Vector3f dir, float size, int life, float damage) {
        return GemScene.spawn(helper.getLevel(), caster, spell.get(), kind, helper.absoluteVec(new Vec3(x, y, z)), dir, size, life, damage);
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    /** A ruby shard burns through the first two in its way and stops before the third. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_shard", timeoutTicks = 60)
    public static void rubyShardBurnsThroughTwo(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator first = foe(helper, 4.5, 3.5), second = foe(helper, 4.5, 5.5), third = foe(helper, 4.5, 7.5);
        scene(helper, caster, ModSpells.RUBY_SHARDS, GemScene.Kind.SHARD, 4.5, 3, 1.6, new Vector3f(0, 0, 1), .5f, 30, 3);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(first) && hurt(second), "the shard did not burn through the first two");
            helper.assertTrue(first.isOnFire(), "the shard did not set its first victim alight");
            helper.assertTrue(!hurt(third), "the shard went through a third");
            done(helper);
        });
    }

    /** The nova's rays strike only once the ruby has gathered its light, and set what they strike alight. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_nova", timeoutTicks = 60)
    public static void rubyNovaStrikesRoundIt(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Vindicator foe = foe(helper, 6.5, 4.5);
        scene(helper, caster, ModSpells.RUBY_NOVA, GemScene.Kind.NOVA, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 9, 42, 4);
        helper.runAfterDelay(GemScene.NOVA_CHARGE - 3, () -> helper.assertTrue(!hurt(foe), "the nova struck before its ruby had gathered its light"));
        helper.runAfterDelay(GemScene.NOVA_CHARGE + 3, () -> {
            helper.assertTrue(hurt(foe), "the nova's rays did not strike the enemy");
            helper.assertTrue(foe.isOnFire(), "the nova did not set the enemy alight");
            helper.assertTrue(!hurt(caster), "the nova struck its caster");
            done(helper);
        });
    }

    /** The meteor strikes when it lands, not before; then its molten pool keeps burning. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_meteor", timeoutTicks = 100)
    public static void rubyMeteorLandsThenBurns(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Vindicator foe = foe(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.RUBY_METEOR, GemScene.Kind.METEOR, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 4.5f, 120, 4);
        float[] landed = {0};
        helper.runAfterDelay(GemScene.METEOR_FALL - 3, () -> helper.assertTrue(!hurt(foe), "the meteor struck before it landed"));
        helper.runAfterDelay(GemScene.METEOR_FALL + 2, () -> {
            helper.assertTrue(hurt(foe), "the meteor did not strike the enemy when it landed");
            foe.setPos(helper.absoluteVec(new Vec3(5, 2, 4.5)));
            foe.setDeltaMovement(Vec3.ZERO);
            landed[0] = foe.getHealth();
        });
        helper.runAfterDelay(GemScene.METEOR_FALL + GemScene.POOL_EVERY * 2 + 3, () -> {
            helper.assertTrue(foe.getHealth() < landed[0], "the molten pool did not burn the enemy in it");
            done(helper);
        });
    }

    /** The shooting star flies to its prey, bursts on it and weighs it down. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_star", timeoutTicks = 60)
    public static void shootingStarBurstsOnItsPrey(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator foe = foe(helper, 5.5, 6.5);
        scene(helper, caster, ModSpells.SHOOTING_STAR, GemScene.Kind.STAR, 4.5, 3, 1.6, new Vector3f(0, 0, 1), 2, 44, 3).follow(foe);
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(hurt(foe), "the star did not burst on its prey");
            helper.assertTrue(foe.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the star's prey is not weighed down");
            helper.assertTrue(!hurt(caster), "the star struck its caster");
            done(helper);
        });
    }

    /** Stars fall out of the vault onto the enemy beneath it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_vault", timeoutTicks = 80)
    public static void vaultStarsFall(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Vindicator foe = foe(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.CELESTIAL_VAULT, GemScene.Kind.VAULT, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 5, 100, 2);
        helper.runAfterDelay(GemScene.VAULT_OPEN + GemScene.VAULT_EVERY + 3, () -> {
            helper.assertTrue(hurt(foe), "no star fell on the enemy under the vault");
            helper.assertTrue(!hurt(caster), "a star fell on the caster");
            done(helper);
        });
    }

    /** The mirror binds the nearest enemy in sight: struck, silenced, slowed. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_mirror", timeoutTicks = 60)
    public static void mirrorBindsTheNearest(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 2.5);
        Vindicator foe = foe(helper, 6.5, 5.5);
        scene(helper, caster, ModSpells.LAPIS_MIRROR, GemScene.Kind.MIRROR, 2.5, 2, 2.5, new Vector3f(0, 0, 1), 1, 100, 2).follow(caster);
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(hurt(foe), "the mirror did not strike the enemy");
            helper.assertTrue(foe.hasEffect(ModEffects.SILENCE), "the mirror did not silence the enemy");
            helper.assertTrue(!hurt(caster), "the mirror struck its master");
            done(helper);
        });
    }

    /** The diamond lance goes through every enemy on its line at once. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_lance", timeoutTicks = 40)
    public static void diamondLanceGoesThroughTheLine(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator near = foe(helper, 4.5, 3.5), far = foe(helper, 4.5, 7.5);
        scene(helper, caster, ModSpells.DIAMOND_LANCE, GemScene.Kind.LANCE, 4.5, 3.3, .9, new Vector3f(0, 0, 1), 22, 16, 3);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(hurt(near) && hurt(far), "the lance did not go through both enemies on its line");
            helper.assertTrue(!hurt(caster), "the lance struck its caster");
            done(helper);
        });
    }

    /** The diamond dust cuts and blinds what is in it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_dust", timeoutTicks = 60)
    public static void diamondDustCutsAndBlinds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Vindicator foe = foe(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.DIAMOND_DUST, GemScene.Kind.DUST, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 3.5f, 80, 2);
        helper.runAfterDelay(GemScene.DUST_EVERY + 3, () -> {
            helper.assertTrue(hurt(foe), "the dust did not cut the enemy in it");
            helper.assertTrue(foe.hasEffect(MobEffects.BLINDNESS), "the dust did not blind the enemy");
            done(helper);
        });
    }

    /** The bastion hardens its side while it stands, and shatters on the enemies round it at the end. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_bastion", timeoutTicks = 80)
    public static void bastionHardensThenShatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 3.5, 4.5);
        Vindicator foe = foe(helper, 6.5, 4.5);
        int life = 40;
        scene(helper, caster, ModSpells.DIAMOND_BASTION, GemScene.Kind.BASTION, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 5, life, 4);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the bastion did not harden its caster");
            helper.assertTrue(!hurt(foe), "the bastion struck before it shattered");
        });
        helper.runAfterDelay(life - GemScene.BASTION_SHATTER + 3, () -> {
            helper.assertTrue(hurt(foe), "the bastion's shards did not strike the enemy");
            helper.assertTrue(!hurt(caster), "the bastion's shards struck its caster");
            done(helper);
        });
    }
    /** Amethyst clusters burst out along the line and strike what stands on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_spikes", timeoutTicks = 40)
    public static void spikesBurstAlongTheLine(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator foe = foe(helper, 4.5, 4.5);
        scene(helper, caster, ModSpells.AMETHYST_SPIKES, GemScene.Kind.SPIKES, 4.5, 2, .5, new Vector3f(0, 0, 1), 1, 32, 3);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(hurt(foe), "the spikes did not strike the enemy on their line");
            helper.assertTrue(!hurt(caster), "the spikes struck their caster");
            done(helper);
        });
    }

    /** The budding amethyst rings at its first stage, then shatters on what is round it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_budding", timeoutTicks = 120)
    public static void buddingRingsThenShatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Vindicator foe = foe(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.BUDDING_AMETHYST, GemScene.Kind.BUDDING, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 5, 100, 4);
        float[] before = {0};
        helper.runAfterDelay(GemScene.BUD_STAGE + 2, () -> helper.assertTrue(hurt(foe), "the budding amethyst's first ring did not strike the enemy"));
        helper.runAfterDelay(4 * GemScene.BUD_STAGE - 2, () -> before[0] = foe.getHealth());
        helper.runAfterDelay(4 * GemScene.BUD_STAGE + 2, () -> {
            helper.assertTrue(foe.getHealth() < before[0], "the budding amethyst did not shatter on the enemy");
            done(helper);
        });
    }

    /** The chimes ring at the enemy in sight and slow it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_chimes", timeoutTicks = 60)
    public static void chimesRingAtFoes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 2.5);
        Vindicator foe = foe(helper, 6.5, 5.5);
        scene(helper, caster, ModSpells.AMETHYST_CHIMES, GemScene.Kind.CHIMES, 2.5, 2, 2.5, new Vector3f(0, 0, 1), 1, 100, 3).follow(caster);
        helper.runAfterDelay(GemScene.CHIME_EVERY + 4, () -> {
            helper.assertTrue(hurt(foe), "the chimes did not strike the enemy");
            helper.assertTrue(foe.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the chimes did not slow the enemy");
            helper.assertTrue(!hurt(caster), "the chimes struck their master");
            done(helper);
        });
    }

    /** The lifeline takes an ailment off its patient at once, then heals them. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_lifeline", timeoutTicks = 60)
    public static void lifelineCleansesAndHeals(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
        scene(helper, caster, ModSpells.EMERALD_LIFELINE, GemScene.Kind.LIFELINE, 4.5, 2, 4.5, new Vector3f(0, 0, 1), 1, 68, 2).follow(caster).heal(caster);
        helper.runAfterDelay(GemScene.LIFELINE_EVERY + 3, () -> {
            helper.assertTrue(!caster.hasEffect(MobEffects.WEAKNESS), "the lifeline did not take the ailment off");
            helper.assertTrue(caster.getHealth() > 10, "the lifeline did not heal its patient");
            done(helper);
        });
    }

    /** The aegis shields its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_aegis", timeoutTicks = 40)
    public static void aegisShieldsItsCaster(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        ModSpells.EMERALD_AEGIS.get().onCast(helper.getLevel(), 1, caster, io.redspace.ironsspellbooks.api.spells.CastSource.COMMAND,
                new io.redspace.ironsspellbooks.api.magic.MagicData(true));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.ABSORPTION), "the aegis gave its caster no shield");
            helper.assertTrue(caster.getAbsorptionAmount() > 0, "the aegis's shield holds nothing");
            done(helper);
        });
    }

    /** The emerald tree heals its side round it once grown. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_tree", timeoutTicks = 60)
    public static void treeHealsItsSide(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 3.5, 4.5);
        caster.setHealth(10);
        scene(helper, caster, ModSpells.EMERALD_TREE, GemScene.Kind.TREE, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 5, 120, 3);
        helper.runAfterDelay(GemScene.TREE_GROW + 3, () -> {
            helper.assertTrue(caster.getHealth() > 10, "the tree did not heal its caster");
            done(helper);
        });
    }

    /** The thorns strike the enemies round the caster and heal the caster for part of it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_thorns", timeoutTicks = 40)
    public static void thornsStrikeAndDrain(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        Vindicator foe = foe(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.EMERALD_THORNS, GemScene.Kind.THORNS, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 7, 32, 6);
        helper.runAfterDelay(GemScene.THORNS_RISE - 2, () -> helper.assertTrue(!hurt(foe), "the thorns struck before they burst out"));
        helper.runAfterDelay(GemScene.THORNS_RISE + 2, () -> {
            helper.assertTrue(hurt(foe), "the thorns did not strike the enemy");
            helper.assertTrue(caster.getHealth() > 10, "the thorns did not heal their caster");
            done(helper);
        });
    }
}
