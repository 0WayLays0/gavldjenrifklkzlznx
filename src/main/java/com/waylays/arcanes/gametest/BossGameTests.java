package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.boss.AnnihilationBeam;
import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.entity.boss.RuneWall;
import com.waylays.arcanes.entity.boss.VoidOrb;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.boss.BossSpell;
import com.waylays.arcanes.spells.boss.LastJudgmentSpell;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-side checks run by CI for Boss Magic: every attack warns long enough before it lands, whoever stays in a
 * marked zone is hit when (and only when) the warning runs out, whoever leaves it in time is spared, shelters work.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class BossGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9
    /** The least warning an attack may give: 3 seconds. */
    private static final int MIN_WARNING = 60;

    private BossGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return !e.isAlive() || e.getHealth() < e.getMaxHealth();
    }

    private static Witch caster(GameTestHelper helper, double x, double z) {
        Witch witch = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(x, 2, z));
        witch.setYRot(0);
        witch.setYHeadRot(0);
        witch.setYBodyRot(0);
        return witch;
    }

    private static Pig pig(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(x, 2, z));
    }

    /** Every Boss Magic thing around: warnings, scenery, beams, orbs, walls. */
    private static List<Entity> bossEntities(GameTestHelper helper) {
        AABB box = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(48);
        return helper.getLevel().getEntities((Entity) null, box, e -> e instanceof BossTelegraph || e instanceof BossProp
                || e instanceof AnnihilationBeam || e instanceof VoidOrb || e instanceof RuneWall);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void bossSchoolIsRegistered(GameTestHelper helper) {
        SchoolType boss = SchoolRegistry.getSchool(ModSchools.BOSS_RESOURCE);
        helper.assertTrue(boss != null, "Boss school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            helper.assertTrue(spell != SpellRegistry.none(), entry.getId() + " missing from Iron's spell registry");
            if (spell.getSchoolType() == boss) {
                count++;
                helper.assertTrue(spell instanceof BossSpell, entry.getId() + " is in the Boss school but is no Boss spell");
            }
        }
        helper.assertTrue(count == 10, "expected 10 Boss spells, got " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.BOSS_MAGIC),
                "boss_magic damage type missing");
        helper.assertTrue(boss.isFocus(new ItemStack(Items.NETHER_STAR)), "the nether star is not the Boss focus");
        helper.assertTrue(new ItemStack(ModItems.TYRANTS_CODEX.get()).getItem() instanceof SpellBook, "the Tyrant's Codex is not a spellbook");
        helper.succeed();
    }

    /**
     * Every Boss spell, cast once: each of its warnings (some are marked one after another) shows at least three
     * seconds before it lands. The warnings are taken away as soon as they are seen, so nothing lands here.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_warnings", timeoutTicks = 120)
    public static void everyBossAttackWarnsAtLeastThreeSeconds(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        // Aimed at the platform, so the attacks aimed at the ground are marked here and not on another test
        caster.setXRot(60);
        Map<String, Integer> shortest = new HashMap<>();
        Set<String> boss = new HashSet<>();
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            if (entry.get() instanceof BossSpell spell) {
                boss.add(spell.getSpellId());
                spell.onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
            }
        }
        helper.onEachTick(() -> {
            for (Entity e : bossEntities(helper)) {
                if (e instanceof BossTelegraph t && t.getSpell() != null) {
                    shortest.merge(t.getSpell().getSpellId(), t.getFireTick() - t.getStartTick(), Math::min);
                }
                e.discard();
            }
        });
        helper.runAfterDelay(100, () -> {
            for (String id : boss) {
                helper.assertTrue(shortest.containsKey(id), id + " drew no warning");
                helper.assertTrue(shortest.get(id) >= MIN_WARNING, id + " warns only " + shortest.get(id) + " ticks before it lands");
            }
            helper.succeed();
        });
    }

    /** Standing in a marked circle: nothing until the warning runs out, then the hit. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_zone", timeoutTicks = 140)
    public static void stayingInTheZoneIsHitWhenTheWarningRunsOut(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Pig pig = pig(helper, 4.5, 4.5);
        Vec3 center = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        BossTelegraph t = BossTelegraph.circle(helper.getLevel(), caster, ModSpells.CRUSHING_METEOR.get(), 1, center, 2.5f)
                .damage(4).part("impact").spawn();
        int warning = t.getFireTick() - t.getStartTick();
        helper.assertTrue(warning >= MIN_WARNING, "the warning lasts only " + warning + " ticks");
        helper.runAfterDelay(warning - 5, () -> helper.assertTrue(!hurt(pig), "the pig was hurt before the warning ran out"));
        helper.runAfterDelay(warning + 5, () -> {
            helper.assertTrue(hurt(pig), "the pig standing in the zone was not hurt");
            helper.assertTrue(!hurt(caster), "the caster was hurt by their own attack");
            helper.succeed();
        });
    }

    /** Leaving the circle while it is marked: spared. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_dodge", timeoutTicks = 140)
    public static void leavingTheZoneInTimeSparesYou(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Pig pig = pig(helper, 4.5, 4.5);
        Vec3 center = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        BossTelegraph t = BossTelegraph.circle(helper.getLevel(), caster, ModSpells.CRUSHING_METEOR.get(), 1, center, 2.5f)
                .damage(4).part("impact").spawn();
        int warning = t.getFireTick() - t.getStartTick();
        // Two seconds to notice, then a step out of the circle
        helper.runAfterDelay(40, () -> {
            Vec3 out = helper.absoluteVec(new Vec3(7.5, 2, 7.5));
            pig.teleportTo(out.x, out.y, out.z);
        });
        helper.runAfterDelay(warning + 5, () -> {
            helper.assertTrue(t.isAlive() || t.isRemoved(), "the warning vanished");
            helper.assertTrue(!hurt(pig), "the pig that left the zone in time was hurt");
            helper.succeed();
        });
    }

    /** A line hits along its length and nothing beside it; a ring spares its middle. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void zoneShapesHoldWhatTheyShow(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        AbstractSpell spell = ModSpells.JUDGMENT_BLADE.get();
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 2, 1.5));
        BossTelegraph line = BossTelegraph.line(helper.getLevel(), caster, spell, 1, start, 0, 6, 2);
        helper.assertTrue(line.contains(start.add(0, 0, 3)), "the middle of a line is not in it");
        helper.assertTrue(!line.contains(start.add(1.5, 0, 3)), "a point beside a line is in it");
        helper.assertTrue(!line.contains(start.add(0, 0, 6.5)), "a point past the end of a line is in it");
        helper.assertTrue(!line.contains(start.add(0, 0, -.5)), "a point behind a line is in it");
        helper.assertTrue(!line.contains(start.add(0, 8, 3)), "a point high above a line is in it");
        BossTelegraph ring = BossTelegraph.ring(helper.getLevel(), caster, spell, 1, start, 2, 4);
        helper.assertTrue(ring.contains(start.add(3, 0, 0)), "a point in a ring is not in it");
        helper.assertTrue(!ring.contains(start.add(1, 0, 0)), "a ring's middle is in it");
        helper.assertTrue(!ring.contains(start.add(5, 0, 0)), "a point outside a ring is in it");
        BossTelegraph cone = BossTelegraph.cone(helper.getLevel(), caster, spell, 1, start, 0, 5, 30);
        helper.assertTrue(cone.contains(start.add(0, 0, 4)), "a point ahead in a cone is not in it");
        helper.assertTrue(!cone.contains(start.add(3, 0, 1)), "a point off to the side is in a cone");
        helper.succeed();
    }

    /** The last judgment strikes the whole arena but its shelters. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_judgment", timeoutTicks = 240)
    public static void lastJudgmentSparesTheSheltered(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 4.5);
        LastJudgmentSpell spell = (LastJudgmentSpell) ModSpells.LAST_JUDGMENT.get();
        spell.onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        List<BossTelegraph> shelters = helper.getLevel().getEntitiesOfClass(BossTelegraph.class, caster.getBoundingBox().inflate(LastJudgmentSpell.RADIUS + 2),
                t -> t.getStyle() == BossTelegraph.Style.SAFE);
        helper.assertTrue(shelters.size() == LastJudgmentSpell.SAFE_SPOTS, "expected " + LastJudgmentSpell.SAFE_SPOTS + " shelters, got " + shelters.size());
        // One pig in a shelter, one in the open (beside the caster, where no shelter is)
        BossTelegraph shelter = shelters.get(0);
        Pig safe = EntityType.PIG.create(helper.getLevel());
        helper.assertTrue(safe != null, "could not make the sheltered pig");
        safe.moveTo(shelter.getX(), shelter.getY(), shelter.getZ());
        // No AI and no gravity: it stays in its shelter, which is often off the platform (a fall would hurt it)
        safe.setNoAi(true);
        safe.setNoGravity(true);
        helper.getLevel().addFreshEntity(safe);
        Pig exposed = pig(helper, 5.5, 4.5);
        BossTelegraph judgment = helper.getLevel().getEntitiesOfClass(BossTelegraph.class, caster.getBoundingBox().inflate(2),
                t -> t.getPart().equals("judgment")).stream().findFirst().orElse(null);
        helper.assertTrue(judgment != null, "no judgment marked");
        int lands = judgment.getFireTick() - (int) helper.getLevel().getGameTime();
        helper.runAfterDelay(lands + 5, () -> {
            helper.assertTrue(hurt(exposed), "the judgment spared a pig in the open");
            helper.assertTrue(!hurt(safe), "the judgment struck a pig in its shelter");
            helper.assertTrue(!hurt(caster), "the judgment struck its caster");
            bossEntities(helper).forEach(Entity::discard);
            helper.succeed();
        });
    }

    /** The beam burns what stands in its strip once it fires, not before, and not beside it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_beam", timeoutTicks = 80)
    public static void annihilationBeamBurnsItsStrip(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        Pig inStrip = pig(helper, 4.5, 5.5);
        Pig beside = pig(helper, 8.0, 5.5);
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 2, 1.0));
        int fire = (int) helper.getLevel().getGameTime() + 20;
        AnnihilationBeam.spawn(helper.getLevel(), caster, ModSpells.ANNIHILATION_BEAM.get(), start, 0, 7, 2.6f, fire, 2f);
        helper.runAfterDelay(15, () -> helper.assertTrue(!hurt(inStrip), "the beam burned before it fired"));
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(hurt(inStrip), "the beam missed the pig in its strip");
            helper.assertTrue(!hurt(beside), "the beam hurt a pig beside its strip");
            helper.assertTrue(!hurt(caster), "the beam hurt its caster");
            helper.succeed();
        });
    }

    /** Whoever touches the prison wall is hurt and thrown back. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, timeoutTicks = 60)
    public static void runeWallThrowsBackWhoeverTouchesIt(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 4.5);
        Pig pig = pig(helper, 7.5, 4.5);
        Vec3 center = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        RuneWall.spawn(helper.getLevel(), caster, ModSpells.RUNE_PRISON.get(), center, 3, 40, 2f);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(hurt(pig), "the pig touching the wall was not hurt");
            helper.assertTrue(!hurt(caster), "the wall hurt its caster");
            bossEntities(helper).forEach(Entity::discard);
            helper.succeed();
        });
    }
}
