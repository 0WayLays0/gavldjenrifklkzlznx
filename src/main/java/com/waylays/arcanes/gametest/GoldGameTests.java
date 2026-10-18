package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

/**
 * Server-side checks run by CI: the Gold school is wired into Iron's Spells and both spells do what they say.
 * Rendering, particles and sounds can only be checked in game.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class GoldGameTests {
    private static final TagKey<Item> CURIOS_SPELLBOOK = TagKey.create(Registries.ITEM, new ResourceLocation("curios", "spellbook"));

    private GoldGameTests() {}

    @GameTest(template = "platform")
    public static void goldSchoolIsRegistered(GameTestHelper helper) {
        SchoolType gold = SchoolRegistry.getSchool(ModSchools.GOLD_RESOURCE);
        helper.assertTrue(gold != null, "Gold school missing from Iron's school registry");
        for (RegistryObject<AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            helper.assertTrue(spell != SpellRegistry.none(), entry.getId() + " missing from Iron's spell registry");
            helper.assertTrue(spell.getSchoolType() == gold, entry.getId() + " is not in the Gold school but in " + spell.getSchoolType().getId());
        }
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.GOLD_MAGIC),
                "gold_magic damage type missing");
        helper.assertTrue(gold.isFocus(new ItemStack(Items.GOLD_BLOCK)), "gold block is not the Gold focus");
        ItemStack grimoire = new ItemStack(ModItems.GOLDEN_GRIMOIRE.get());
        helper.assertTrue(grimoire.getItem() instanceof SpellBook, "Golden Grimoire is not a spellbook");
        helper.assertTrue(grimoire.is(CURIOS_SPELLBOOK), "Golden Grimoire does not fit the curios spellbook slot");
        helper.succeed();
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void goldBoltHitsAndShatters(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 1, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 1, 6.5));
        Pig bystander = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.8, 1, 7.0));
        GoldBoltProjectile bolt = new GoldBoltProjectile(helper.getLevel(), caster);
        bolt.setPos(helper.absoluteVec(new Vec3(4.5, 1.3, 2.5)));
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

    @GameTest(template = "platform", timeoutTicks = 200)
    public static void gildedChainsPinTargetThenBreak(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 1, 1.5));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 1, 5.5));
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
}
