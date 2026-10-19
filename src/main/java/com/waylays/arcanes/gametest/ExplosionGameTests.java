package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesConfig;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.BlastLeapEffect;
import com.waylays.arcanes.effect.ReactiveShieldEffect;
import com.waylays.arcanes.effect.VolatileBrandEffect;
import com.waylays.arcanes.entity.explosion.CataclysmCore;
import com.waylays.arcanes.entity.explosion.ChainDetonation;
import com.waylays.arcanes.entity.explosion.ClusterBomblet;
import com.waylays.arcanes.entity.explosion.ClusterShell;
import com.waylays.arcanes.entity.explosion.ExplosiveOrb;
import com.waylays.arcanes.entity.explosion.ImplosionCore;
import com.waylays.arcanes.entity.explosion.RuinSigil;
import com.waylays.arcanes.entity.explosion.ShieldBubble;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.explosion.SigilsOfRuinSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Server-side checks run by CI for the Explosion school: wiring into Iron's Spells, and each spell's mechanics
 * (damage, chain, cascade, pull, charges, flight, crown). Spells with a wide area run in their own batch so they
 * cannot reach the other tests. Rendering, particles and sounds are covered by the client smoke test.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class ExplosionGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2
    private static final TagKey<Item> CURIOS_SPELLBOOK = TagKey.create(Registries.ITEM, new ResourceLocation("curios", "spellbook"));

    private ExplosionGameTests() {}

    private static String describe(GameTestHelper helper, LivingEntity entity) {
        DamageSource last = entity.getLastDamageSource();
        return "health " + entity.getHealth() + "/" + entity.getMaxHealth() + " at " + helper.relativeVec(entity.position())
                + ", last hit " + (last == null ? "none" : last.getMsgId() + " by " + last.getDirectEntity());
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS)
    public static void explosionSchoolIsRegistered(GameTestHelper helper) {
        SchoolType explosion = SchoolRegistry.getSchool(ModSchools.EXPLOSION_RESOURCE);
        helper.assertTrue(explosion != null, "Explosion school missing from Iron's school registry");
        int count = 0;
        for (RegistryObject<AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            if (SpellRegistry.getSpell(entry.getId()).getSchoolType() == explosion) {
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Explosion spells, got " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.EXPLOSION_MAGIC),
                "explosion_magic damage type missing");
        helper.assertTrue(explosion.isFocus(new ItemStack(Items.TNT)), "TNT is not the Explosion focus");
        ItemStack codex = new ItemStack(ModItems.DETONATION_CODEX.get());
        helper.assertTrue(codex.getItem() instanceof SpellBook, "Detonation Codex is not a spellbook");
        helper.assertTrue(codex.is(CURIOS_SPELLBOOK), "Detonation Codex does not fit the curios spellbook slot");
        helper.assertTrue(!ArcanesConfig.explosionsBreakBlocks(), "explosions break blocks by default");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 80)
    public static void blastOrbExplodesWithoutBreakingBlocks(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 6.5));
        Pig splash = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 2, 7.0));
        ExplosiveOrb orb = new ExplosiveOrb(helper.getLevel(), caster);
        orb.setPos(helper.absoluteVec(new Vec3(4.5, 2.2, 2.5)));
        orb.shoot(new Vec3(0, 0, 1), ExplosiveOrb.SPEED);
        orb.setDamage(5f);
        helper.getLevel().addFreshEntity(orb);
        helper.succeedWhen(() -> {
            helper.assertTrue(orb.isRemoved(), "orb is still flying (at " + helper.relativeVec(orb.position()) + ")");
            helper.assertTrue(target.getHealth() <= target.getMaxHealth() - 3f, "the blast missed the target [" + describe(helper, target) + "]");
            helper.assertTrue(splash.getHealth() < splash.getMaxHealth(), "the blast has no area [" + describe(helper, splash) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the blast hurt the caster");
            helper.assertBlockNotPresent(Blocks.AIR, new BlockPos(4, 1, 6));
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
    public static void chainDetonationJumpsBetweenEnemies(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 7.5));
        Pig first = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(1.5, 2, 1.5));
        Pig second = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 3.0));
        Pig third = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, 2, 4.5));
        helper.getLevel().addFreshEntity(new ChainDetonation(helper.getLevel(), caster, first, 3f, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(first.getHealth() < first.getMaxHealth(), "the first blast missed [" + describe(helper, first) + "]");
            helper.assertTrue(second.getHealth() < second.getMaxHealth(), "the chain did not jump to the second pig [" + describe(helper, second) + "]");
            helper.assertTrue(third.getHealth() < third.getMaxHealth(), "the chain did not jump to the third pig [" + describe(helper, third) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the chain hurt the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 120)
    public static void sigilsTriggerAndCascade(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig stepping = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 2, 4.5));
        // Close to the second sigil, but not on it: only the cascade can reach this one
        Pig bystander = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 2, 6.7));
        RuinSigil a = new RuinSigil(helper.getLevel(), caster, 3f);
        Vec3 atA = helper.absoluteVec(new Vec3(2.5, 2, 4.5));
        a.moveTo(atA.x, atA.y, atA.z);
        RuinSigil b = new RuinSigil(helper.getLevel(), caster, 3f);
        Vec3 atB = helper.absoluteVec(new Vec3(6.5, 2, 4.5));
        b.moveTo(atB.x, atB.y, atB.z);
        helper.getLevel().addFreshEntity(a);
        helper.getLevel().addFreshEntity(b);
        helper.runAfterDelay(RuinSigil.ARM_TICKS - 3, () ->
                helper.assertTrue(stepping.getHealth() == stepping.getMaxHealth(), "the sigil went off before it was armed"));
        helper.succeedWhen(() -> {
            helper.assertTrue(a.isRemoved() && b.isRemoved(), "a sigil did not go off (first " + !a.isRemoved() + ", second " + !b.isRemoved() + ")");
            helper.assertTrue(stepping.getHealth() < stepping.getMaxHealth(), "the stepped-on sigil did not hurt [" + describe(helper, stepping) + "]");
            helper.assertTrue(bystander.getHealth() < bystander.getMaxHealth(), "the cascade did not reach the second sigil [" + describe(helper, bystander) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a sigil hurt the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void sigilsOfRuinPlacesItsSigils(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        SigilsOfRuinSpell spell = (SigilsOfRuinSpell) ModSpells.SIGILS_OF_RUIN.get();
        spell.onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<RuinSigil> sigils = helper.getLevel().getEntitiesOfClass(RuinSigil.class, caster.getBoundingBox().inflate(8));
        helper.assertTrue(sigils.size() == spell.getSigilCount(1), "expected " + spell.getSigilCount(1) + " sigils, got " + sigils.size());
        for (RuinSigil sigil : sigils) {
            Vec3 rel = helper.relativeVec(sigil.position());
            helper.assertTrue(Math.abs(rel.y - 2) < .01, "a sigil is not on the floor but at " + rel);
            helper.assertTrue(rel.z > 2.5, "a sigil is not in front of the caster but at " + rel);
        }
        sigils.forEach(RuinSigil::discard);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "explosion_wave", timeoutTicks = 80)
    public static void detonationWaveRollsOutward(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        Pig near = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.0, 2, 4.5));
        Pig far = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, 2, 8.5));
        ModSpells.DETONATION_WAVE.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.runAfterDelay(2, () -> helper.assertTrue(far.getHealth() == far.getMaxHealth(), "the far pig was hit before the wave reached it"));
        helper.succeedWhen(() -> {
            helper.assertTrue(near.getHealth() < near.getMaxHealth(), "the first ring missed [" + describe(helper, near) + "]");
            helper.assertTrue(far.getHealth() < far.getMaxHealth(), "the outer ring missed [" + describe(helper, far) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the wave hurt the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "explosion_implosion", timeoutTicks = 120)
    public static void implosionPullsThenDetonates(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.0, 2, 4.5));
        ImplosionCore core = new ImplosionCore(helper.getLevel(), caster, 6f, 6f);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2.2, 4.5));
        core.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(core);
        Vec3 casterStart = caster.position();
        double startDistance = target.position().distanceTo(core.position());
        helper.runAfterDelay(ImplosionCore.CONTRACT_TICK - 5, () -> {
            double distance = target.position().distanceTo(core.position());
            helper.assertTrue(distance < startDistance - 1, "the pig was not pulled in (" + startDistance + " -> " + distance + ")");
            helper.assertTrue(caster.position().distanceTo(casterStart) < .2, "the caster was pulled in");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(core.isRemoved(), "the core has not detonated yet");
            helper.assertTrue(target.getHealth() <= target.getMaxHealth() - 3f, "the detonation missed [" + describe(helper, target) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the detonation hurt the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 120)
    public static void volatileBrandExplodesOnTheFifthHit(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.0, 2, 1.0));
        Husk attacker = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(7.5, 2, 7.5));
        IronGolem branded = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 4.5));
        Pig bystander = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 6.3));
        VolatileBrandEffect.brand(branded, caster, 1, 400, 6f);
        DamageSource hit = helper.getLevel().damageSources().mobAttack(attacker);
        for (int i = 0; i < VolatileBrandEffect.MAX_CHARGES; i++) {
            int n = i + 1;
            // Spaced out so each hit gets through the golem's hurt immunity
            helper.runAfterDelay(2 + i * 12, () -> {
                helper.assertTrue(branded.hasEffect(ModEffects.VOLATILE_BRAND.get()), "the brand went off before hit " + n);
                helper.assertTrue(bystander.getHealth() == bystander.getMaxHealth(), "the brand exploded early, before hit " + n);
                branded.hurt(hit, 1f);
                helper.assertTrue(VolatileBrandEffect.charges(branded) == n || n == VolatileBrandEffect.MAX_CHARGES,
                        "hit " + n + " did not charge the brand (" + VolatileBrandEffect.charges(branded) + ")");
            });
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(!branded.hasEffect(ModEffects.VOLATILE_BRAND.get()), "the brand is still on");
            helper.assertTrue(bystander.getHealth() < bystander.getMaxHealth(), "the brand did not explode [" + describe(helper, bystander) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the brand hurt its caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
    public static void blastLeapDivesAndLandsWithoutFallDamage(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 2, 4.5));
        double startY = caster.getY();
        // Straight up, so it lands back on the platform
        caster.setDeltaMovement(0, 1.25, 0);
        BlastLeapEffect.leap(caster, Vec3.ZERO, 6f);
        helper.runAfterDelay(BlastLeapEffect.DIVE_TICK - 2, () ->
                helper.assertTrue(caster.getY() > startY + 3, "the caster did not fly up (" + (caster.getY() - startY) + " blocks)"));
        helper.succeedWhen(() -> {
            helper.assertTrue(!caster.hasEffect(ModEffects.BLAST_LEAP.get()), "the caster has not landed");
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the landing blast missed [" + describe(helper, target) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the leap hurt the caster [" + describe(helper, caster) + "]");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "explosion_cluster", timeoutTicks = 200)
    public static void clusterShellRainsBomblets(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.0, 2, 1.0));
        Pig[] pigs = {
                helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.0, 2, 4.5)),
                helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(1.0, 2, 4.5)),
                helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 8.0)),
                helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 1.0))
        };
        ClusterShell shell = new ClusterShell(helper.getLevel(), caster, 8);
        shell.setPos(helper.absoluteVec(new Vec3(4.5, 3, 4.5)));
        shell.setDeltaMovement(0, .6, 0);
        shell.setDamage(3f);
        helper.getLevel().addFreshEntity(shell);
        helper.runAfterDelay(11, () -> {
            helper.assertTrue(shell.isRemoved(), "the shell did not burst");
            int bomblets = helper.getLevel().getEntitiesOfClass(ClusterBomblet.class, caster.getBoundingBox().inflate(16)).size();
            helper.assertTrue(bomblets == 8, "expected 8 bomblets, got " + bomblets);
        });
        helper.succeedWhen(() -> {
            int left = helper.getLevel().getEntitiesOfClass(ClusterBomblet.class, caster.getBoundingBox().inflate(16)).size();
            helper.assertTrue(left == 0, left + " bomblets have not exploded yet");
            boolean anyHit = false;
            for (Pig pig : pigs) {
                anyHit |= pig.getHealth() < pig.getMaxHealth();
            }
            helper.assertTrue(anyHit, "no bomblet hit any of the four pigs");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a bomblet hurt the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 80)
    public static void reactiveShieldExplodesThreeTimes(GameTestHelper helper) {
        Pig bearer = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        Husk attacker = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(6.0, 2, 4.5));
        ReactiveShieldEffect.shield(bearer, 1, 400, 2f);
        helper.assertTrue(ShieldBubble.bubblesOf(bearer).size() == 1, "no shell around the bearer");
        DamageSource hit = helper.getLevel().damageSources().mobAttack(attacker);
        float before = bearer.getHealth();
        bearer.hurt(hit, 4f);
        helper.assertTrue(before - bearer.getHealth() < 4f, "the shield took nothing of the blow");
        helper.assertTrue(attacker.getHealth() < attacker.getMaxHealth(), "the shield did not explode on the attacker");
        helper.assertTrue(ReactiveShieldEffect.charges(bearer) == ReactiveShieldEffect.CHARGES - 1, "the explosion did not use a charge");
        helper.runAfterDelay(15, () -> bearer.hurt(hit, 2f));
        helper.runAfterDelay(30, () -> bearer.hurt(hit, 2f));
        helper.runAfterDelay(34, () -> {
            helper.assertTrue(!bearer.hasEffect(ModEffects.REACTIVE_SHIELD.get()), "the shield is still on after three explosions");
            helper.assertTrue(ShieldBubble.bubblesOf(bearer).isEmpty(), "the shell is still drawn after the shield broke");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "explosion_ultimate", timeoutTicks = 200)
    public static void cataclysmDetonatesWithItsCrown(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        IronGolem enemy = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(7.5, 2, 7.5));
        Vec3 ground = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        CataclysmCore core = new CataclysmCore(helper.getLevel(), caster, 30f, ground.y);
        core.moveTo(ground.x, ground.y + 5, ground.z);
        helper.getLevel().addFreshEntity(core);
        helper.runAfterDelay(CataclysmCore.GROW_TICKS - 5, () ->
                helper.assertTrue(enemy.getHealth() == enemy.getMaxHealth(), "the sphere hurt before it exploded"));
        helper.succeedWhen(() -> {
            helper.assertTrue(core.isRemoved(), "the crown of explosions is not over");
            helper.assertTrue(enemy.getHealth() <= enemy.getMaxHealth() - 20, "the cataclysm dealt too little [" + describe(helper, enemy) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the cataclysm hurt the caster");
        });
    }
}
