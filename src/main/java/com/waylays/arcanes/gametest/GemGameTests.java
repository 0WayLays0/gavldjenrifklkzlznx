package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gem.AmethystGeode;
import com.waylays.arcanes.entity.gem.AmethystResonance;
import com.waylays.arcanes.entity.gem.DiamondPrism;
import com.waylays.arcanes.entity.gem.DiamondShell;
import com.waylays.arcanes.entity.gem.EmeraldBloom;
import com.waylays.arcanes.entity.gem.GemCrown;
import com.waylays.arcanes.entity.gem.LapisConstellation;
import com.waylays.arcanes.entity.gem.LapisSeal;
import com.waylays.arcanes.entity.gem.RubyHeart;
import com.waylays.arcanes.entity.gem.RubyRay;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.gem.GemSpell;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Gem Magic: the school is there; the ruby ray burns through what is in line and a
 * prism splits it at the enemies around; the burning heart sets itself in its target and bursts; the lapis seal
 * silences and binds, the constellation's stars strike; the diamond shell takes most of a blow; the resonance's waves
 * hurt and daze; the geode holds and pierces; the emerald bloom heals and cleanses; the crown acts and strikes.
 * Each test runs in a batch of its own and takes down what it raised.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class GemGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private GemGameTests() {}

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

    private static AbstractSpell spell(DeferredHolder<AbstractSpell, AbstractSpell> holder) {
        return holder.get();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void gemSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.GEM_RESOURCE);
        helper.assertTrue(school != null, "Gem school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof GemSpell, entry.getId() + " is in the Gem school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 26, "expected 26 Gem spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.JEWELLERS_CODEX.get()).getItem() instanceof SpellBook, "the Jeweller's Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.DIAMOND).is(ModSchools.GEM_FOCUS), "diamonds are not the school's focus");
        helper.succeed();
    }

    /** The ray goes through both enemies in line, burning them. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_ray", timeoutTicks = 60)
    public static void rubyRayBurnsThrough(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator near = foe(helper, 4.5, 4.5);
        Vindicator far = foe(helper, 4.5, 7.5);
        helper.onEachTick(() -> RubyRay.hold(helper.getLevel(), caster, spell(ModSpells.RUBY_RAY), 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(near) && near.isOnFire(), "the ray did not burn the near enemy");
            helper.assertTrue(hurt(far) && far.isOnFire(), "the ray did not go through to the far enemy");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** A prism in the ray's way catches it and splits it at the enemies on either side (the prism's own rays do nothing here). */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_prism", timeoutTicks = 80)
    public static void prismSplitsTheRay(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator left = foe(helper, 1.5, 6.5);
        Vindicator right = foe(helper, 7.5, 6.5);
        DiamondPrism.place(helper.getLevel(), caster, spell(ModSpells.DIAMOND_PRISM), helper.absoluteVec(new Vec3(4.5, 2, 5.5)), 400, 0);
        helper.onEachTick(() -> RubyRay.hold(helper.getLevel(), caster, spell(ModSpells.RUBY_RAY), 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(left) && hurt(right), "the split rays did not reach both enemies (left hurt " + hurt(left) + ", right hurt " + hurt(right) + ")");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_heart", timeoutTicks = 160)
    public static void rubyHeartBeatsAndBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator target = foe(helper, 4.5, 5.5);
        Vec3 look = caster.getLookAngle();
        RubyHeart heart = RubyHeart.hurl(helper.getLevel(), caster, spell(ModSpells.RUBY_HEART), caster.getEyePosition().add(look.scale(.8)),
                look.scale(1.2), 6);
        boolean[] setIn = {false};
        helper.onEachTick(() -> setIn[0] |= heart.getHost() == target.getId());
        helper.succeedWhen(() -> {
            helper.assertTrue(setIn[0], "the heart did not set itself in the enemy");
            helper.assertTrue(heart.isRemoved(), "the heart has not burst yet");
            helper.assertTrue(hurt(target), "the heart did not hurt its host");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_seal", timeoutTicks = 120)
    public static void lapisSealBinds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator target = foe(helper, 4.5, 5.5);
        LapisSeal seal = LapisSeal.seal(helper.getLevel(), caster, spell(ModSpells.LAPIS_SEAL), target, 40, 5);
        helper.runAfterDelay(LapisSeal.RISE + 2, () -> {
            helper.assertTrue(target.hasEffect(ModEffects.SILENCE), "the sealed enemy is not silenced");
            helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the sealed enemy is not bound");
        });
        helper.runAfterDelay(LapisSeal.RISE + 40 + 4, () -> {
            helper.assertTrue(hurt(target), "the seal did not burst on the enemy");
            helper.assertTrue(!target.hasEffect(ModEffects.SILENCE), "the enemy is still silenced after the seal broke");
            helper.assertTrue(seal.getLife() <= seal.tickCount, "the seal outlived its time");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_stars", timeoutTicks = 120)
    public static void constellationStarsStrike(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator target = foe(helper, 4.5, 6.5);
        LapisConstellation sky = LapisConstellation.gather(helper.getLevel(), caster, spell(ModSpells.LAPIS_CONSTELLATION), 400, 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(sky.getStars() != (1 << LapisConstellation.STARS) - 1, "no star left the ring");
            helper.assertTrue(hurt(target), "no star struck the enemy");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** A blow on the shell loses most of its strength on a plate, which breaks off and flies back at the striker. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_shell", timeoutTicks = 80)
    public static void diamondShellTakesTheBlow(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator striker = foe(helper, 4.5, 4.5);
        DiamondShell shell = DiamondShell.encase(helper.getLevel(), caster, spell(ModSpells.DIAMOND_SHELL), 400, 4);
        helper.runAfterDelay(2, () -> {
            float before = caster.getHealth();
            caster.invulnerableTime = 0;
            caster.hurt(helper.getLevel().damageSources().mobAttack(striker), 10);
            float lost = before - caster.getHealth();
            helper.assertTrue(lost > 0 && lost <= 10 * (1 - DiamondShell.ABSORB) + .01f, "the shell did not take the blow (lost " + lost + ")");
            helper.assertTrue(Integer.bitCount(shell.getPlates()) == DiamondShell.PLATES - 1, "no plate broke off");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(striker), "the broken plate did not strike back");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_resonance", timeoutTicks = 100)
    public static void resonanceWavesHurtAndDaze(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator target = foe(helper, 4.5, 6.5);
        AmethystResonance.ring(helper.getLevel(), caster, spell(ModSpells.AMETHYST_RESONANCE), caster.position(), AmethystResonance.RANGE, 3);
        helper.runAfterDelay(AmethystResonance.pulseAt(1) - 1, () -> helper.assertTrue(!hurt(target), "the wave struck before the crystals rang"));
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(target) && target.hasEffect(MobEffects.CONFUSION), "the wave did not hurt and daze the enemy");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_geode", timeoutTicks = 160)
    public static void geodeHoldsAndPierces(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator target = foe(helper, 4.5, 5.5);
        Vec3 start = target.position();
        AmethystGeode geode = AmethystGeode.close(helper.getLevel(), caster, spell(ModSpells.AMETHYST_GEODE), target, 40, 5);
        helper.runAfterDelay(AmethystGeode.FORM + AmethystGeode.GROW + 2, () -> {
            helper.assertTrue(hurt(target), "the crystals did not pierce the enemy");
            target.setDeltaMovement(1, 0, 0);
        });
        helper.runAfterDelay(AmethystGeode.FORM + AmethystGeode.GROW + 8, () -> helper.assertTrue(target.position().distanceTo(start) < .6,
                "the enemy got out of the geode (moved " + target.position().distanceTo(start) + ")"));
        helper.succeedWhen(() -> {
            helper.assertTrue(geode.isRemoved(), "the geode has not cracked open yet");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_bloom", timeoutTicks = 120)
    public static void emeraldBloomHealsAndCleanses(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(caster.getMaxHealth() / 2);
        caster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
        float before = caster.getHealth();
        EmeraldBloom.open(helper.getLevel(), caster, caster.position(), 100, 3);
        helper.runAfterDelay(EmeraldBloom.OPEN + EmeraldBloom.HEAL_TICKS + 2, () -> {
            helper.assertTrue(caster.getHealth() > before, "the bloom did not heal its caster");
            helper.assertTrue(!caster.hasEffect(MobEffects.WEAKNESS), "the bloom did not take the ailment off");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The ruby of the crown burns the enemy in its turn; then the fused gem strikes it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gem_crown", timeoutTicks = 320)
    public static void crownActsThenStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Vindicator target = foe(helper, 4.5, 6.5);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        target.setHealth(400);
        GemCrown.raise(helper.getLevel(), caster, spell(ModSpells.CROWN_OF_GEMS), caster.position(), target.position(), 6);
        float[] before = {0};
        helper.runAfterDelay(GemCrown.actsAt(com.waylays.arcanes.spells.gem.Gem.RUBY, 0) + 2,
                () -> helper.assertTrue(hurt(target) && target.isOnFire(), "the crown's ruby did not burn the enemy"));
        helper.runAfterDelay(GemCrown.STRIKE - 1, () -> before[0] = target.getHealth());
        helper.runAfterDelay(GemCrown.STRIKE + 2, () -> {
            helper.assertTrue(target.getHealth() < before[0], "the fused gem's strike did not hurt the enemy");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }
}
