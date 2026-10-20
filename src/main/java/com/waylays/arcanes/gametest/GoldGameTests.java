package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.MidasCurseEffect;
import com.waylays.arcanes.entity.gold.AurumBladeEntity;
import com.waylays.arcanes.entity.gold.EldoradoSun;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import com.waylays.arcanes.entity.gold.GoldenSentinel;
import com.waylays.arcanes.entity.gold.GoldfallIngot;
import com.waylays.arcanes.entity.gold.MoltenGoldBlob;
import com.waylays.arcanes.entity.gold.MoltenPool;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.gold.GoldenSentinelsSpell;
import com.waylays.arcanes.spells.gold.GoldfallSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Server-side checks run by CI: the Gold school is wired into Iron's Spells and both spells do what they say.
 * Rendering, particles and sounds can only be checked in game.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class GoldGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2
    private static final TagKey<Item> CURIOS_SPELLBOOK = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "spellbook"));

    private GoldGameTests() {}

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS)
    public static void goldSchoolIsRegistered(GameTestHelper helper) {
        SchoolType gold = SchoolRegistry.getSchool(ModSchools.GOLD_RESOURCE);
        helper.assertTrue(gold != null, "Gold school missing from Iron's school registry");
        int goldSpells = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            helper.assertTrue(spell != SpellRegistry.none(), entry.getId() + " missing from Iron's spell registry");
            if (spell.getSchoolType() == gold) {
                goldSpells++;
            }
        }
        helper.assertTrue(goldSpells == 10, "expected 10 Gold spells, got " + goldSpells);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.GOLD_MAGIC),
                "gold_magic damage type missing");
        helper.assertTrue(gold.isFocus(new ItemStack(Items.GOLD_BLOCK)), "gold block is not the Gold focus");
        ItemStack grimoire = new ItemStack(ModItems.GOLDEN_GRIMOIRE.get());
        helper.assertTrue(grimoire.getItem() instanceof SpellBook, "Golden Grimoire is not a spellbook");
        helper.assertTrue(grimoire.is(CURIOS_SPELLBOOK), "Golden Grimoire does not fit the curios spellbook slot");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
    public static void goldBoltHitsAndShatters(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 6.5));
        Pig bystander = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.8, 2, 7.0));
        GoldBoltProjectile bolt = new GoldBoltProjectile(helper.getLevel(), caster);
        bolt.setPos(helper.absoluteVec(new Vec3(4.5, 2.3, 2.5)));
        bolt.shoot(new Vec3(0, 0, 1));
        bolt.setDamage(6f);
        helper.getLevel().addFreshEntity(bolt);
        helper.succeedWhen(() -> {
            helper.assertTrue(bolt.isRemoved(), "bolt is still flying (at " + helper.relativeVec(bolt.position()) + ")");
            String state = " [target " + describe(helper, target) + ", bystander " + describe(helper, bystander) + "]";
            helper.assertTrue(target.getHealth() <= target.getMaxHealth() - 5f, "direct hit did not deal its damage" + state);
            helper.assertTrue(bystander.getHealth() < bystander.getMaxHealth(), "shards did not hit the bystander" + state);
            helper.assertTrue(bystander.getHealth() > target.getHealth(), "splash damage is as high as the direct hit" + state);
        });
    }

    private static String describe(GameTestHelper helper, LivingEntity entity) {
        DamageSource last = entity.getLastDamageSource();
        return "health " + entity.getHealth() + " at " + helper.relativeVec(entity.position())
                + ", last hit " + (last == null ? "none" : last.getMsgId() + " by " + last.getDirectEntity());
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 200)
    public static void gildedChainsPinTargetThenBreak(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 5.5));
        MagicData castData = new MagicData(true);
        castData.setAdditionalCastData(new TargetEntityCastData(target));
        ModSpells.GILDED_CHAINS.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, castData);

        helper.assertTrue(target.getVehicle() instanceof GildedChainsEntity, "target is not bound by the chains");
        GildedChainsEntity chains = (GildedChainsEntity) target.getVehicle();
        helper.assertTrue(target.getHealth() < target.getMaxHealth(), "binding did not hurt the target");
        target.stopRiding();
        helper.assertTrue(target.getVehicle() == chains, "target slipped out of the chains");

        helper.runAfterDelay(chains.getDuration() + 5, () -> {
            helper.assertTrue(chains.isRemoved(), "chains did not break after their duration");
            helper.assertTrue(target.getVehicle() == null, "target still bound after the chains broke");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 120)
    public static void goldfallIngotCrushesTheArea(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        GoldfallIngot ingot = new GoldfallIngot(helper.getLevel(), caster);
        ingot.setPos(helper.absoluteVec(new Vec3(4.5, 8, 4.5)));
        ingot.setDeltaMovement(0, -.4, 0);
        ingot.setDamage(5f);
        helper.getLevel().addFreshEntity(ingot);
        helper.succeedWhen(() -> {
            helper.assertTrue(ingot.isRemoved(), "ingot is still falling");
            helper.assertTrue(target.getHealth() <= target.getMaxHealth() - 4f, "impact did not hurt the target [" + describe(helper, target) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the caster was hurt by its own ingot");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 40)
    public static void goldfallDropsOneIngotPerCount(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        GoldfallSpell spell = (GoldfallSpell) ModSpells.GOLDFALL.get();
        spell.onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<GoldfallIngot> ingots = helper.getLevel().getEntitiesOfClass(GoldfallIngot.class, caster.getBoundingBox().inflate(40, 60, 40));
        int count = ingots.size();
        // They would land 32 blocks away, possibly on another test: clear them
        ingots.forEach(GoldfallIngot::discard);
        helper.assertTrue(count == spell.getIngotCount(1), "expected " + spell.getIngotCount(1) + " ingots, got " + count);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 80)
    public static void goldenSpikesHitInFrontOfTheCaster(GameTestHelper helper) {
        // A witch with no AI looks south (+Z), the spikes run that way
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.0));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, .5));
        ModSpells.GOLDEN_SPIKES.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "no spike hit the pig in front [" + describe(helper, target) + "]");
            helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "a spike hit the pig behind the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
    public static void midasCurseSlowsThenShatters(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        double baseSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
        MidasCurseEffect.curse(target, caster, 1, 40, 5f);
        helper.runAfterDelay(30, () -> helper.assertTrue(target.getAttributeValue(Attributes.MOVEMENT_SPEED) < baseSpeed * .5,
                "the cursed pig is not slowed (" + target.getAttributeValue(Attributes.MOVEMENT_SPEED) + " / " + baseSpeed + ")"));
        helper.succeedWhen(() -> {
            helper.assertTrue(!target.hasEffect(ModEffects.MIDAS_CURSE), "still cursed");
            helper.assertTrue(target.getHealth() <= target.getMaxHealth() - 4f, "the statue did not shatter [" + describe(helper, target) + "]");
            helper.assertTrue(target.getAttributeValue(Attributes.MOVEMENT_SPEED) == baseSpeed, "speed not restored after the curse");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
    public static void aurumBladeStrikesItsTarget(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        AurumBladeEntity blade = new AurumBladeEntity(helper.getLevel(), caster, 6f);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        blade.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(blade);
        helper.runAfterDelay(AurumBladeEntity.STRIKE_TICK - 4, () ->
                helper.assertTrue(target.getHealth() == target.getMaxHealth(), "the blade struck before plunging"));
        helper.succeedWhen(() -> helper.assertTrue(target.getHealth() <= target.getMaxHealth() - 5f, "the blade did not strike [" + describe(helper, target) + "]"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
    public static void moltenGoldBurnsAndPools(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 5.5));
        MoltenGoldBlob atPig = new MoltenGoldBlob(helper.getLevel(), caster);
        atPig.setPos(helper.absoluteVec(new Vec3(4.5, 2.4, 2.5)));
        atPig.shoot(new Vec3(0, 0, 1), .8f);
        atPig.setDamage(3f);
        MoltenGoldBlob atFloor = new MoltenGoldBlob(helper.getLevel(), caster);
        atFloor.setPos(helper.absoluteVec(new Vec3(7.5, 3, 7.5)));
        atFloor.shoot(new Vec3(0, -1, 0), .5f);
        atFloor.setDamage(3f);
        helper.getLevel().addFreshEntity(atPig);
        helper.getLevel().addFreshEntity(atFloor);
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "molten gold did not hurt the pig [" + describe(helper, target) + "]");
            helper.assertTrue(target.getRemainingFireTicks() > 0 || target.getHealth() <= target.getMaxHealth() - 3f, "the pig was not set on fire");
            int pools = helper.getLevel().getEntitiesOfClass(MoltenPool.class, caster.getBoundingBox().inflate(12)).size();
            helper.assertTrue(pools >= 1, "no molten pool on the floor");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 60)
    public static void gildedArmorReflectsDamage(GameTestHelper helper) {
        Witch attacker = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig wearer = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        double armorBefore = wearer.getAttributeValue(Attributes.ARMOR);
        wearer.addEffect(new MobEffectInstance(ModEffects.GILDED_ARMOR, 200, 0));
        helper.assertTrue(wearer.getAttributeValue(Attributes.ARMOR) >= armorBefore + 3, "Gilded Armor gave no armor");
        wearer.hurt(helper.getLevel().damageSources().mobAttack(attacker), 6f);
        helper.assertTrue(attacker.getHealth() < attacker.getMaxHealth(), "no damage was reflected to the attacker");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "summons", timeoutTicks = 300)
    public static void goldenSentinelsFightForTheirSummoner(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        Husk enemy = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(8.0, 2, 8.0));
        GoldenSentinelsSpell spell = (GoldenSentinelsSpell) ModSpells.GOLDEN_SENTINELS.get();
        spell.onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<GoldenSentinel> sentinels = helper.getLevel().getEntitiesOfClass(GoldenSentinel.class, caster.getBoundingBox().inflate(6));
        helper.assertTrue(sentinels.size() == spell.getCount(1), "expected " + spell.getCount(1) + " sentinels, got " + sentinels.size());
        for (GoldenSentinel sentinel : sentinels) {
            helper.assertTrue(Math.abs(helper.relativeVec(sentinel.position()).y - 2) < .01, "a sentinel did not rise on the floor but at " + helper.relativeVec(sentinel.position()));
        }
        helper.succeedWhen(() -> {
            StringBuilder state = new StringBuilder(" [difficulty " + helper.getLevel().getDifficulty() + ", husk " + describe(helper, enemy)
                    + (enemy.isRemoved() ? " REMOVED" : "") + ", caster" + (caster.isRemoved() ? " REMOVED" : " ok"));
            for (GoldenSentinel s : sentinels) {
                state.append(", sentinel at ").append(helper.relativeVec(s.position())).append(s.isRemoved() ? " REMOVED" : "")
                        .append(" target ").append(s.getTarget()).append(" onGround ").append(s.onGround());
            }
            state.append("]");
            helper.assertTrue(enemy.getHealth() < enemy.getMaxHealth(), "the sentinels did not attack the husk" + state);
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a sentinel hurt its summoner");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "ultimate", timeoutTicks = 260)
    public static void sunOfEldoradoBurnsThenExplodes(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        IronGolem enemy = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(7.5, 2, 7.5));
        ModSpells.SUN_OF_ELDORADO.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<EldoradoSun> suns = helper.getLevel().getEntitiesOfClass(EldoradoSun.class, caster.getBoundingBox().inflate(10));
        helper.assertTrue(suns.size() == 1, "expected one sun, got " + suns.size());
        EldoradoSun sun = suns.get(0);
        helper.runAfterDelay(EldoradoSun.FIRST_VOLLEY + 15, () ->
                helper.assertTrue(enemy.getHealth() < enemy.getMaxHealth(), "the sun's beams missed [" + describe(helper, enemy) + "]"));
        helper.succeedWhen(() -> {
            helper.assertTrue(sun.isRemoved(), "the sun has not exploded yet");
            helper.assertTrue(enemy.getHealth() <= enemy.getMaxHealth() - 40, "beams and explosion dealt too little [" + describe(helper, enemy) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the explosion hurt the caster");
        });
    }
}
