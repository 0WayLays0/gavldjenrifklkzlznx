package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.ChapterRune;
import com.waylays.arcanes.entity.book.FinalChapter;
import com.waylays.arcanes.entity.book.InkHands;
import com.waylays.arcanes.entity.book.PageStorm;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Server-side checks run by CI for the Book school (3 of its 10 spells so far): wiring into Iron's Spells, the page
 * storm's cuts, the ink hands' grip and blindness, and The Final Chapter's runes.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class BookGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2

    private BookGameTests() {}

    private static String describe(GameTestHelper helper, LivingEntity entity) {
        DamageSource last = entity.getLastDamageSource();
        return "health " + entity.getHealth() + "/" + entity.getMaxHealth() + " at " + helper.relativeVec(entity.position())
                + ", last hit " + (last == null ? "none" : last.getMsgId() + " by " + last.getDirectEntity());
    }

    @GameTest(template = "platform")
    public static void bookSchoolIsRegistered(GameTestHelper helper) {
        SchoolType book = SchoolRegistry.getSchool(ModSchools.BOOK_RESOURCE);
        helper.assertTrue(book != null, "Book school missing from Iron's school registry");
        int count = 0;
        for (RegistryObject<AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            if (SpellRegistry.getSpell(entry.getId()).getSchoolType() == book) {
                count++;
            }
        }
        helper.assertTrue(count == 3, "expected 3 Book spells, got " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.BOOK_MAGIC),
                "book_magic damage type missing");
        helper.assertTrue(book.isFocus(new ItemStack(Items.BOOK)), "a book is not the Book focus");
        helper.succeed();
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void pageStormSlashesItsTarget(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.0, 2, 1.0));
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        Pig bystander = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, 2, 4.5));
        PageStorm storm = new PageStorm(helper.getLevel(), caster, target, 1.5f, 40, 10);
        helper.getLevel().addFreshEntity(storm);
        helper.runAfterDelay(PageStorm.GATHER_TICKS - 2, () ->
                helper.assertTrue(target.getHealth() == target.getMaxHealth(), "the pages cut before they gathered"));
        helper.succeedWhen(() -> {
            helper.assertTrue(storm.isRemoved(), "the storm is still blowing");
            helper.assertTrue(target.getHealth() <= target.getMaxHealth() - 4.5f, "fewer than three cuts landed [" + describe(helper, target) + "]");
            helper.assertTrue(bystander.getHealth() == bystander.getMaxHealth(), "the storm cut a bystander");
        });
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void livingInkSeizesAndBlinds(GameTestHelper helper) {
        // A witch with no AI looks south (+Z), the wave runs that way
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Pig front = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 5.5));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, .5));
        ModSpells.LIVING_INK.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        double[] heldAt = new double[1];
        helper.runAfterDelay(20, () -> {
            List<InkHands> hands = helper.getLevel().getEntitiesOfClass(InkHands.class, front.getBoundingBox().inflate(1));
            helper.assertTrue(!hands.isEmpty(), "no ink hands rose under the pig in front");
            helper.assertTrue(front.hasEffect(MobEffects.BLINDNESS), "the seized pig is not blinded");
            helper.assertTrue(front.getHealth() < front.getMaxHealth(), "the hands did not hurt [" + describe(helper, front) + "]");
            heldAt[0] = front.getX();
            front.setDeltaMovement(1, 0, 0);
        });
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(Math.abs(front.getX() - heldAt[0]) < .3, "the seized pig moved away (" + (front.getX() - heldAt[0]) + " blocks)");
            helper.assertTrue(behind.getHealth() == behind.getMaxHealth() && !behind.hasEffect(MobEffects.BLINDNESS), "the ink reached the pig behind the caster");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the ink hurt the caster");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", batch = "book_ultimate", timeoutTicks = 220)
    public static void finalChapterMarksThenDetonates(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(7.5, 2, 7.5));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(1.5, 2, 7.5));
        Vec3 ground = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        FinalChapter chapter = new FinalChapter(helper.getLevel(), caster, 20f, 5f);
        chapter.moveTo(ground.x, ground.y + 5, ground.z);
        helper.getLevel().addFreshEntity(chapter);
        helper.runAfterDelay(FinalChapter.MARK_TICK + 2, () -> {
            List<ChapterRune> runes = helper.getLevel().getEntitiesOfClass(ChapterRune.class, caster.getBoundingBox().inflate(12));
            helper.assertTrue(runes.size() == 2, "expected 2 runes (golem and pig), got " + runes.size());
            for (ChapterRune rune : runes) {
                helper.assertTrue(rune.getTarget() != caster, "the caster was marked");
            }
            helper.assertTrue(golem.getHealth() == golem.getMaxHealth(), "a rune went off before its time");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(chapter.isRemoved(), "the book has not closed yet");
            helper.assertTrue(golem.getHealth() <= golem.getMaxHealth() - 15, "the golem's rune did not explode [" + describe(helper, golem) + "]");
            helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "the pig's rune did not explode");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a rune hurt the caster");
        });
    }
}
