package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.ParchmentAegisEffect;
import com.waylays.arcanes.entity.book.BookmarkMarker;
import com.waylays.arcanes.entity.book.ChapterRune;
import com.waylays.arcanes.entity.book.FinalChapter;
import com.waylays.arcanes.entity.book.FlyingGrimoire;
import com.waylays.arcanes.entity.book.FlyingStack;
import com.waylays.arcanes.entity.book.InkHands;
import com.waylays.arcanes.entity.book.OrigamiDragon;
import com.waylays.arcanes.entity.book.PageStorm;
import com.waylays.arcanes.entity.book.PaperTempest;
import com.waylays.arcanes.entity.book.QuillStroke;
import com.waylays.arcanes.entity.book.SilenceSeal;
import com.waylays.arcanes.entity.book.TomeOfJudgment;
import com.waylays.arcanes.entity.book.WordOfPower;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.SealOfSilenceSpell;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side checks run by CI for the Book school's 16 spells: wiring into Iron's Spells, then what each spell does
 * to its targets (and never to its caster). Spells whose projectiles or areas reach past the test platform run in
 * batches of their own so they cannot touch another test's mobs.
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

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS)
    public static void bookSchoolIsRegistered(GameTestHelper helper) {
        SchoolType book = SchoolRegistry.getSchool(ModSchools.BOOK_RESOURCE);
        helper.assertTrue(book != null, "Book school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            if (SpellRegistry.getSpell(entry.getId()).getSchoolType() == book) {
                count++;
            }
        }
        helper.assertTrue(count == 16, "expected 16 Book spells, got " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.BOOK_MAGIC),
                "book_magic damage type missing");
        helper.assertTrue(book.isFocus(new ItemStack(Items.BOOK)), "a book is not the Book focus");
        helper.assertTrue(ModItems.LIVING_GRIMOIRE.get() != null, "Living Grimoire missing");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
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

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
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

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_ultimate", timeoutTicks = 220)
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

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_razor", timeoutTicks = 60)
    public static void razorPagesCutThroughALine(GameTestHelper helper) {
        // A witch with no AI looks south (+Z): the middle page flies straight through both husks
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        Husk first = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 4.5));
        Husk second = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 6.5));
        ModSpells.RAZOR_PAGES.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.succeedWhen(() -> {
            helper.assertTrue(first.getHealth() < first.getMaxHealth(), "the first husk was not cut [" + describe(helper, first) + "]");
            helper.assertTrue(second.getHealth() < second.getMaxHealth(), "the page did not cut through to the second husk [" + describe(helper, second) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a page cut the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_judgment", timeoutTicks = 120)
    public static void tomeOfJudgmentClapsShut(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(2.5, 2, 4.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 4.5));
        Vec3 ground = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        TomeOfJudgment tome = new TomeOfJudgment(helper.getLevel(), caster, 20f);
        tome.moveTo(ground.x, ground.y, ground.z, 0, 0);
        helper.getLevel().addFreshEntity(tome);
        helper.runAfterDelay(TomeOfJudgment.IMPACT_TICK - 4, () ->
                helper.assertTrue(golem.getHealth() == golem.getMaxHealth(), "the book crushed before it closed"));
        helper.runAfterDelay(TomeOfJudgment.IMPACT_TICK + 3, () -> {
            helper.assertTrue(golem.getHealth() <= golem.getMaxHealth() - 15, "the closing book did not crush the golem [" + describe(helper, golem) + "]");
            helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the crushed golem is not slowed");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the book crushed its caster");
        });
        helper.succeedWhen(() -> helper.assertTrue(tome.isRemoved(), "the book is still there"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 60)
    public static void bookmarkBringsTheCasterBack(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Vec3 home = caster.position();
        MagicData magicData = new MagicData(true);
        String spellId = ModSpells.BOOKMARK.get().getSpellId();
        ModSpells.BOOKMARK.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        List<BookmarkMarker> markers = helper.getLevel().getEntitiesOfClass(BookmarkMarker.class, caster.getBoundingBox().inflate(2));
        helper.assertTrue(markers.size() == 1, "expected one bookmark, got " + markers.size());
        helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(spellId), "no recast waiting for the return");
        Vec3 away = helper.absoluteVec(new Vec3(6.5, 2, 6.5));
        caster.teleportTo(away.x, away.y, away.z);
        helper.runAfterDelay(5, () -> {
            ModSpells.BOOKMARK.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
            helper.assertTrue(caster.position().distanceTo(home) < .5, "the caster did not come back (" + helper.relativeVec(caster.position()) + ")");
            helper.assertTrue(!magicData.getPlayerRecasts().hasRecastForSpell(spellId), "the recast is still open after the return");
        });
        helper.succeedWhen(() -> helper.assertTrue(markers.get(0).isRemoved(), "the used bookmark did not fade"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 100)
    public static void sealOfSilenceStopsCasting(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.5, 2, 5.5));
        SealOfSilenceSpell.seal(helper.getLevel(), caster, pig, 30);
        helper.assertTrue(pig.hasEffect(ModEffects.SILENCE), "the target is not silenced");
        // A silenced player's casts are refused before they start
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String spellId = ModSpells.RAZOR_PAGES.get().getSpellId();
        SchoolType book = SchoolRegistry.getSchool(ModSchools.BOOK_RESOURCE);
        helper.assertTrue(!NeoForge.EVENT_BUS.post(new SpellPreCastEvent(player, spellId, 1, book, CastSource.SPELLBOOK)).isCanceled(), "a cast was refused before the seal");
        player.addEffect(new MobEffectInstance(ModEffects.SILENCE, 100));
        helper.assertTrue(NeoForge.EVENT_BUS.post(new SpellPreCastEvent(player, spellId, 1, book, CastSource.SPELLBOOK)).isCanceled(), "a silenced player could still cast");
        helper.runAfterDelay(10, () -> helper.assertTrue(!helper.getLevel().getEntitiesOfClass(SilenceSeal.class, pig.getBoundingBox().inflate(2)).isEmpty(),
                "no seal on the silenced pig"));
        // The seal breaks with the effect
        helper.succeedWhen(() -> {
            helper.assertTrue(!pig.hasEffect(ModEffects.SILENCE), "still silenced");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(SilenceSeal.class, pig.getBoundingBox().inflate(2)).isEmpty(), "the seal outlived the silence");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_library", timeoutTicks = 300)
    public static void forbiddenLibraryGrimoiresFight(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(6.5, 2, 6.5));
        ModSpells.FORBIDDEN_LIBRARY.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<FlyingGrimoire> grimoires = helper.getLevel().getEntitiesOfClass(FlyingGrimoire.class, caster.getBoundingBox().inflate(4));
        helper.assertTrue(grimoires.size() == 2, "expected 2 grimoires at level 1, got " + grimoires.size());
        for (FlyingGrimoire grimoire : grimoires) {
            helper.assertTrue(caster.getUUID().equals(grimoire.getSummonerUUID()), "a grimoire has no master");
            helper.assertTrue(SpellUtil.isAlly(caster, grimoire), "the caster's spells would hurt its grimoire");
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "the grimoires left the husk alone [" + describe(helper, husk) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a grimoire turned on its master");
            // Done: send the books back so they cannot wander into other tests
            grimoires.forEach(FlyingGrimoire::burst);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_word", timeoutTicks = 60)
    public static void wordOfPowerExplodesForward(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 7.5));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(.5, 2, .5));
        ModSpells.WORD_OF_POWER.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<WordOfPower> words = helper.getLevel().getEntitiesOfClass(WordOfPower.class, caster.getBoundingBox().inflate(5));
        helper.assertTrue(words.size() == 1, "expected one word, got " + words.size());
        helper.runAfterDelay(WordOfPower.DETONATE_TICK - 3, () ->
                helper.assertTrue(golem.getHealth() == golem.getMaxHealth(), "the word went off early"));
        helper.runAfterDelay(WordOfPower.DETONATE_TICK + 2, () -> {
            helper.assertTrue(golem.getHealth() <= golem.getMaxHealth() - 5, "the blast missed the golem ahead [" + describe(helper, golem) + "]");
            helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "the blast went backward");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the blast hurt the caster");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS)
    public static void rewriteErasesHarmAndHeals(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        caster.setHealth(10);
        caster.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        ModSpells.REWRITE.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.assertTrue(!caster.hasEffect(MobEffects.POISON) && !caster.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a harmful effect survived the rewrite");
        helper.assertTrue(caster.hasEffect(MobEffects.MOVEMENT_SPEED), "the rewrite erased a good effect");
        helper.assertTrue(caster.getHealth() > 10, "the rewrite did not heal (" + caster.getHealth() + ")");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_cranes", timeoutTicks = 140)
    public static void paperCranesDiveOnTheirTarget(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(4.5, 2, 7.5));
        ModSpells.PAPER_CRANES.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "no crane reached the golem [" + describe(helper, golem) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a crane hit the caster");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_quill", timeoutTicks = 80)
    public static void quillOfFateEruptsAlongTheLine(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, .5));
        Pig onLine = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 5.5));
        Pig aside = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(.5, 2, 5.5));
        ModSpells.QUILL_OF_FATE.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.runAfterDelay(QuillStroke.ERUPT_TICK - 3, () ->
                helper.assertTrue(onLine.getHealth() == onLine.getMaxHealth(), "the line erupted before the writing was done"));
        helper.runAfterDelay(QuillStroke.ERUPT_TICK + 2, () -> {
            helper.assertTrue(!onLine.isAlive() || onLine.getHealth() < onLine.getMaxHealth(), "the pillars missed the pig on the line [" + describe(helper, onLine) + "]");
            helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "the pillars hit a pig off the line");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the pillars hit the caster");
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, timeoutTicks = 80)
    public static void parchmentAegisCatchesAndStrikesBack(GameTestHelper helper) {
        Witch bearer = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        Husk attacker = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 1.5));
        ModSpells.PARCHMENT_AEGIS.get().onCast(helper.getLevel(), 1, bearer, CastSource.COMMAND, new MagicData(true));
        int pages = ParchmentAegisEffect.pages(bearer);
        helper.assertTrue(pages == 3, "expected 3 pages at level 1, got " + pages);
        // An arrow flying in from the side is caught by a page
        Vec3 from = helper.absoluteVec(new Vec3(8.5, 3.2, 4.5));
        Arrow arrow = new Arrow(helper.getLevel(), from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(-1.2, 0, 0);
        helper.getLevel().addFreshEntity(arrow);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(arrow.isRemoved(), "the arrow got through the pages");
            helper.assertTrue(bearer.getHealth() == bearer.getMaxHealth(), "the arrow hurt the bearer");
            helper.assertTrue(ParchmentAegisEffect.pages(bearer) == 2, "catching the arrow did not use a page (" + ParchmentAegisEffect.pages(bearer) + ")");
            // A blow from the husk: a page softens it and flies back at the husk
            bearer.invulnerableTime = 0;
            bearer.hurt(helper.getLevel().damageSources().mobAttack(attacker), 8);
            helper.assertTrue(bearer.getHealth() > bearer.getMaxHealth() - 8, "the page did not soften the blow");
            helper.assertTrue(ParchmentAegisEffect.pages(bearer) == 1, "softening the blow did not use a page");
        });
        helper.succeedWhen(() -> helper.assertTrue(attacker.getHealth() < attacker.getMaxHealth(), "no page flew back at the attacker [" + describe(helper, attacker) + "]"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_tempest", timeoutTicks = 100)
    public static void paperTempestCutsAndBlindsWhatItCatches(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(6.5, 2, 5.5));
        caster.setTarget(golem);
        ModSpells.PAPER_TEMPEST.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<PaperTempest> storms = helper.getLevel().getEntitiesOfClass(PaperTempest.class, caster.getBoundingBox().inflate(16));
        helper.assertTrue(storms.size() == 1, "expected 1 tempest, got " + storms.size());
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "the tempest never cut the golem [" + describe(helper, golem) + "]");
            helper.assertTrue(golem.hasEffect(MobEffects.BLINDNESS), "the golem caught in the tempest is not blinded");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the tempest cut its caster");
            storms.forEach(PaperTempest::discard);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_stack")
    public static void flyingStackCarriesOnlyItsSummoner(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        ModSpells.FLYING_STACK.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.assertTrue(caster.getVehicle() instanceof FlyingStack, "the caster is not riding a flying stack");
        FlyingStack stack = (FlyingStack) caster.getVehicle();
        helper.assertTrue(caster.getUUID().equals(stack.getSummonerUUID()), "the stack has no master");
        helper.assertTrue(SpellUtil.isAlly(caster, stack), "the caster's spells would tear its own stack");
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(stack.mobInteract(stranger, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "a stranger climbed on the stack");
        stack.dismissSummon();
        helper.assertTrue(stack.isRemoved(), "the stack is still there after being sent away");
        helper.assertTrue(!caster.isPassenger(), "the scattered stack kept its rider");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_stack", timeoutTicks = FlyingStack.RIDERLESS_TICKS + 40)
    public static void anEmptyFlyingStackScattersAndItsRiderFloatsDown(GameTestHelper helper) {
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        FlyingStack stack = ModEntities.FLYING_STACK.get().create(helper.getLevel());
        helper.assertTrue(stack != null, "no flying stack");
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 5, 4.5));
        stack.moveTo(at.x, at.y, at.z, 0, 0);
        stack.setup(rider, 1200, 20, .6f);
        helper.getLevel().addFreshEntity(stack);
        rider.startRiding(stack, true);
        helper.assertTrue(rider.getVehicle() == stack && stack.getControllingPassenger() == rider, "the rider does not steer the stack");
        rider.stopRiding();
        helper.assertTrue(rider.hasEffect(MobEffects.SLOW_FALLING), "getting off in mid-air should float the rider down");
        helper.succeedWhen(() -> helper.assertTrue(stack.isRemoved(), "the empty stack never scattered"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, batch = "book_dragon", timeoutTicks = 300)
    public static void origamiDragonHuntsWhatItsSummonerFights(GameTestHelper helper) {
        // The dragon circles 8 blocks out, past the platform's chunk: keep the chunks around ticking or it freezes there
        List<ChunkPos> forced = forceChunksAround(helper, 1);
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(6.5, 2, 6.5));
        caster.setTarget(golem);
        ModSpells.ORIGAMI_DRAGON.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<OrigamiDragon> dragons = helper.getLevel().getEntitiesOfClass(OrigamiDragon.class, caster.getBoundingBox().inflate(16));
        helper.assertTrue(dragons.size() == 1, "expected 1 dragon, got " + dragons.size());
        OrigamiDragon dragon = dragons.get(0);
        helper.assertTrue(caster.getUUID().equals(dragon.getSummonerUUID()), "the dragon has no master");
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "the dragon never hurt its prey [" + describe(helper, golem) + "]");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the dragon hurt its summoner");
            dragon.dismissSummon();
            helper.assertTrue(dragon.isRemoved(), "the dragon is still there after being sent away");
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x, chunk.z, false));
        });
    }

    /** Force-loads the chunks within {@code radius} of the platform's middle (those not already forced) and returns them. */
    private static List<ChunkPos> forceChunksAround(GameTestHelper helper, int radius) {
        ChunkPos middle = new ChunkPos(BlockPos.containing(helper.absoluteVec(new Vec3(4.5, 2, 4.5))));
        List<ChunkPos> forced = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (helper.getLevel().setChunkForced(middle.x + dx, middle.z + dz, true)) {
                    forced.add(new ChunkPos(middle.x + dx, middle.z + dz));
                }
            }
        }
        return forced;
    }
}
