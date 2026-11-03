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
import com.waylays.arcanes.spells.boss.InfernalCheckerboardSpell;
import com.waylays.arcanes.spells.boss.LancesOfJudgmentSpell;
import com.waylays.arcanes.spells.boss.BossSpell;
import com.waylays.arcanes.spells.boss.LastJudgmentSpell;
import com.waylays.arcanes.spells.boss.ThousandBladesSpell;
import com.waylays.arcanes.spells.boss.WrathOfTheHeavensSpell;
import net.minecraft.world.effect.MobEffects;
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
        helper.assertTrue(count == 30, "expected 30 Boss spells, got " + count);
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
        // The shelters reach up to eleven blocks out, over the neighbouring tests' ground: clear what those left there
        GameTestSetup.clearLeftovers(helper);
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
            helper.assertTrue(hurt(exposed) && exposed.getLastHurtByMob() == caster, "the judgment spared a pig in the open");
            // Struck by the spell, that is by its caster: the shelter stands on ground another test may have left
            // burning, and that is not the judgment
            helper.assertTrue(safe.getLastHurtByMob() != caster, "the judgment struck a pig in its shelter");
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

    // ---------------------------------------------------------------- the ten great attacks

    private static long props(GameTestHelper helper, BossProp.Kind kind) {
        return bossEntities(helper).stream().filter(e -> e instanceof BossProp p && p.getKind() == kind).count();
    }

    private static void cast(GameTestHelper helper, AbstractSpell spell, Witch caster) {
        spell.onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    private static void done(GameTestHelper helper) {
        bossEntities(helper).forEach(Entity::discard);
        helper.succeed();
    }

    /**
     * The ten great attacks, cast together: each shows its scenery (the swords, the wheel, the eclipse, the moon, ...).
     * Nothing lands: everything is taken away before the first warning runs out, once the storm has called its last bolt.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_scenery", timeoutTicks = 120)
    public static void greatAttacksShowTheirScenery(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setXRot(60);
        List<AbstractSpell> spells = List.of(ModSpells.THOUSAND_BLADES.get(), ModSpells.EXECUTIONERS_WHEEL.get(), ModSpells.ABYSSAL_MAW.get(),
                ModSpells.DIVINE_PRISM.get(), ModSpells.CHAINS_OF_THE_DAMNED.get(), ModSpells.PENTACLE_OF_THORNS.get(), ModSpells.SERAPH_WINGS.get(),
                ModSpells.WRATH_OF_THE_HEAVENS.get(), ModSpells.ECLIPSE.get(), ModSpells.FALLEN_MOON.get());
        for (AbstractSpell spell : spells) {
            cast(helper, spell, caster);
        }
        Set<BossProp.Kind> seen = new HashSet<>();
        helper.onEachTick(() -> bossEntities(helper).forEach(e -> {
            if (e instanceof BossProp p) {
                seen.add(p.getKind());
            }
        }));
        int lands = BossTelegraph.warningTicks();
        int last = WrathOfTheHeavensSpell.bolts(1) * WrathOfTheHeavensSpell.CALL;
        helper.assertTrue(last + 6 < lands, "the storm still calls bolts when the first warnings run out");
        helper.runAfterDelay(last + 4, () -> {
            for (BossProp.Kind kind : List.of(BossProp.Kind.SWORD, BossProp.Kind.WHEEL, BossProp.Kind.MAW, BossProp.Kind.PRISM, BossProp.Kind.CHAINS,
                    BossProp.Kind.THORNS, BossProp.Kind.WINGS, BossProp.Kind.STORM, BossProp.Kind.BOLT, BossProp.Kind.ECLIPSE, BossProp.Kind.MOON)) {
                helper.assertTrue(seen.contains(kind), "no " + kind + " was shown");
            }
            long swords = props(helper, BossProp.Kind.SWORD);
            helper.assertTrue(swords >= ThousandBladesSpell.swords(1) / 2, "only " + swords + " swords for " + ThousandBladesSpell.swords(1));
            done(helper);
        });
    }

    /**
     * The third ten attacks, cast together: each shows its scenery (the bell, the fist, the lances, ...), and the board is
     * marked in all its twenty-five squares. Nothing lands: everything is taken away first.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_scenery_3", timeoutTicks = 60)
    public static void thirdTenShowTheirScenery(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setXRot(60);
        List<AbstractSpell> spells = List.of(ModSpells.BELL_OF_DOOM.get(), ModSpells.INFERNAL_CHECKERBOARD.get(), ModSpells.TITANS_FIST.get(),
                ModSpells.LANCES_OF_JUDGMENT.get(), ModSpells.REAPERS_SCYTHE.get(), ModSpells.ORBITAL_STRIKE.get(), ModSpells.SPECTRAL_KINGS.get(),
                ModSpells.CLOCK_OF_FATE.get(), ModSpells.CRIMSON_LOTUS.get(), ModSpells.COLLAPSE.get());
        for (AbstractSpell spell : spells) {
            cast(helper, spell, caster);
        }
        for (BossProp.Kind kind : List.of(BossProp.Kind.BELL, BossProp.Kind.FIST, BossProp.Kind.LANCE, BossProp.Kind.SCYTHE, BossProp.Kind.ORBITAL,
                BossProp.Kind.KING, BossProp.Kind.CLOCK, BossProp.Kind.LOTUS, BossProp.Kind.COLLAPSE)) {
            helper.assertTrue(props(helper, kind) >= 1, "no " + kind + " was shown");
        }
        long kings = props(helper, BossProp.Kind.KING), lances = props(helper, BossProp.Kind.LANCE);
        helper.assertTrue(kings == 4, "expected 4 kings, got " + kings);
        helper.assertTrue(lances == LancesOfJudgmentSpell.LINES, "expected " + LancesOfJudgmentSpell.LINES + " lances, got " + lances);
        long squares = bossEntities(helper).stream()
                .filter(e -> e instanceof BossTelegraph t && t.getSpell() == ModSpells.INFERNAL_CHECKERBOARD.get()).count();
        int cells = InfernalCheckerboardSpell.GRID * InfernalCheckerboardSpell.GRID;
        helper.assertTrue(squares == cells, "the board has " + squares + " squares, not " + cells);
        done(helper);
    }

    /** The bell's first toll strikes what stands in its first ring, when its warning runs out. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_bell", timeoutTicks = 140)
    public static void bellTollsOnItsRing(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Pig pig = pig(helper, 4.5, 7.5);
        cast(helper, ModSpells.BELL_OF_DOOM.get(), caster);
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(lands - 5, () -> helper.assertTrue(!hurt(pig), "the bell tolled before its warning ran out"));
        helper.runAfterDelay(lands + 4, () -> {
            helper.assertTrue(hurt(pig), "the first toll missed the pig in its ring");
            helper.assertTrue(!hurt(caster), "the bell struck its caster");
            done(helper);
        });
    }

    /** The wheel cuts what stands in its strip as it rolls over it, not before, and not beside it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_wheel", timeoutTicks = 160)
    public static void executionersWheelCutsItsStrip(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Pig ahead = pig(helper, 4.5, 6.5);
        Pig beside = pig(helper, 8.3, 6.5);
        cast(helper, ModSpells.EXECUTIONERS_WHEEL.get(), caster);
        helper.assertTrue(props(helper, BossProp.Kind.WHEEL) == 1, "no wheel formed");
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(lands - 5, () -> helper.assertTrue(!hurt(ahead), "the wheel cut before its warning ran out"));
        helper.runAfterDelay(lands + 4, () -> {
            helper.assertTrue(hurt(ahead), "the wheel missed the pig in its strip");
            helper.assertTrue(!hurt(beside), "the wheel cut a pig beside its strip");
            helper.assertTrue(!hurt(caster), "the wheel cut its caster");
            done(helper);
        });
    }

    /** The first band of the eclipse strikes round its caster and leaves its victim in the dark. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_eclipse", timeoutTicks = 140)
    public static void eclipseDarkensWhomItStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Pig pig = pig(helper, 4.5, 6.5);
        cast(helper, ModSpells.ECLIPSE.get(), caster);
        helper.assertTrue(props(helper, BossProp.Kind.ECLIPSE) == 1, "no eclipse in the sky");
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(lands - 5, () -> helper.assertTrue(!hurt(pig), "the eclipse struck before its warning ran out"));
        helper.runAfterDelay(lands + 3, () -> {
            helper.assertTrue(hurt(pig) && pig.hasEffect(MobEffects.DARKNESS), "the pig in the first band is not hurt and in the dark");
            helper.assertTrue(!hurt(caster), "the eclipse struck its caster");
            done(helper);
        });
    }

    /** The chains slow whoever they bind while they hold, then the hellfire burns them. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_chains", timeoutTicks = 140)
    public static void chainsBindThenBurn(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, 1.5);
        Pig pig = pig(helper, 5.5, 5.5);
        cast(helper, ModSpells.CHAINS_OF_THE_DAMNED.get(), caster);
        helper.assertTrue(props(helper, BossProp.Kind.CHAINS) == 1, "no chains burst out round the pig");
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(30, () -> helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the bound pig is not slowed"));
        helper.runAfterDelay(lands + 3, () -> {
            helper.assertTrue(hurt(pig), "the hellfire missed the bound pig");
            helper.assertTrue(!hurt(caster), "the chains hurt their caster");
            done(helper);
        });
    }

    /** The prism's first beam strikes along the line ahead of its caster and leaves its victim glowing. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_prism", timeoutTicks = 140)
    public static void prismBeamStrikesAlongItsLine(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Pig pig = pig(helper, 4.5, 4.5);
        cast(helper, ModSpells.DIVINE_PRISM.get(), caster);
        helper.assertTrue(props(helper, BossProp.Kind.PRISM) == 1, "no prism over the caster");
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(lands - 5, () -> helper.assertTrue(!hurt(pig), "the prism struck before its warning ran out"));
        helper.runAfterDelay(lands + 2, () -> {
            helper.assertTrue(hurt(pig) && pig.hasEffect(MobEffects.GLOWING), "the pig on the first line is not hurt and glowing");
            helper.assertTrue(!hurt(caster), "the prism hurt its caster");
            done(helper);
        });
    }

    /** The maw bites what is still in it: hurt, slowed, withering. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_maw", timeoutTicks = 140)
    public static void abyssalMawBitesWhatStaysIn(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, 1.5);
        Pig pig = pig(helper, 5.5, 4.5);
        BossTelegraph t = BossTelegraph.circle(helper.getLevel(), caster, ModSpells.ABYSSAL_MAW.get(), 1, helper.absoluteVec(new Vec3(4.5, 2, 4.5)), 3)
                .damage(4).part("maw").spawn();
        int lands = t.getFireTick() - t.getStartTick();
        helper.runAfterDelay(lands + 3, () -> {
            helper.assertTrue(hurt(pig), "the maw missed the pig in it");
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && pig.hasEffect(MobEffects.WITHER), "the bitten pig is not slowed and withering");
            done(helper);
        });
    }

    /** The moon crushes what is under it and sets it burning; the blast hits what is round the crater. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_moon", timeoutTicks = 140)
    public static void fallenMoonCrushesThenBlasts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 7.5, .5);
        Pig under = pig(helper, 2.5, 2.5);
        Pig round = pig(helper, 6.5, 7.8);
        AbstractSpell moon = ModSpells.FALLEN_MOON.get();
        BossTelegraph crash = BossTelegraph.circle(helper.getLevel(), caster, moon, 1, helper.absoluteVec(new Vec3(2.5, 2, 2.5)), 1.8f)
                .damage(4).part("moon").spawn();
        BossTelegraph.ring(helper.getLevel(), caster, moon, 1, helper.absoluteVec(new Vec3(6.5, 2, 6.5)), .8f, 2).damage(4).part("blast").spawn();
        int lands = crash.getFireTick() - crash.getStartTick();
        helper.runAfterDelay(lands + 3, () -> {
            helper.assertTrue(hurt(under) && under.isOnFire(), "the pig under the moon is not crushed and burning");
            helper.assertTrue(hurt(round), "the blast missed the pig round the crater");
            helper.assertTrue(!hurt(caster), "the moon hurt its caster");
            done(helper);
        });
    }

    /** The wings blast what is in the fan ahead and lift it; what is off to the side is spared. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_wings", timeoutTicks = 140)
    public static void seraphWingsBlastTheFanAhead(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Pig ahead = pig(helper, 4.5, 5.5);
        Pig aside = pig(helper, 8.5, 1.5);
        cast(helper, ModSpells.SERAPH_WINGS.get(), caster);
        helper.assertTrue(props(helper, BossProp.Kind.WINGS) == 1, "no wings unfolded");
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(lands + 3, () -> {
            helper.assertTrue(hurt(ahead) && ahead.hasEffect(MobEffects.LEVITATION), "the pig ahead is not hurt and lifted");
            helper.assertTrue(!hurt(aside), "the wings hit a pig off to the side");
            helper.assertTrue(!hurt(caster), "the wings hurt their caster");
            done(helper);
        });
    }

    /** The storm marks its first bolt on the foe under it and strikes it when the warning runs out. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_storm", timeoutTicks = 160)
    public static void stormHuntsTheFoeUnderIt(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, 1.5);
        Pig pig = pig(helper, 6.5, 6.5);
        cast(helper, ModSpells.WRATH_OF_THE_HEAVENS.get(), caster);
        helper.assertTrue(props(helper, BossProp.Kind.STORM) == 1, "no storm gathered");
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(lands - 5, () -> helper.assertTrue(!hurt(pig), "a bolt struck before its warning ran out"));
        // By then the storm has called its last bolt: taking everything away leaves nothing behind
        helper.runAfterDelay(lands + 4, () -> {
            helper.assertTrue(hurt(pig), "the storm's first bolt missed the pig it was called on");
            helper.assertTrue(!hurt(caster), "the storm struck its caster");
            done(helper);
        });
    }

    /** The first sword falls on the foe near the spot the caster aims at. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_blades", timeoutTicks = 140)
    public static void thousandBladesFallOnTheFoe(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        caster.setXRot(30);
        Pig pig = pig(helper, 4.5, 5.5);
        cast(helper, ModSpells.THOUSAND_BLADES.get(), caster);
        int lands = BossTelegraph.warningTicks();
        helper.runAfterDelay(lands - 5, () -> helper.assertTrue(!hurt(pig), "a sword fell before its warning ran out"));
        helper.runAfterDelay(lands + 3, () -> {
            helper.assertTrue(hurt(pig), "no sword fell on the pig");
            helper.assertTrue(!hurt(caster), "a sword fell on its caster");
            done(helper);
        });
    }

    /** Thorns burst out along a stroke of the star: what stands on it is hurt and withering, what stands beside it is not. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "boss_thorns", timeoutTicks = 140)
    public static void thornsBurstAlongTheirStroke(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, .5);
        Pig on = pig(helper, 4.5, 4.5);
        Pig beside = pig(helper, 7.5, 4.5);
        BossTelegraph t = BossTelegraph.line(helper.getLevel(), caster, ModSpells.PENTACLE_OF_THORNS.get(), 1, helper.absoluteVec(new Vec3(4.5, 2, 1)), 0, 6, 2)
                .damage(4).part("stroke").spawn();
        int lands = t.getFireTick() - t.getStartTick();
        helper.runAfterDelay(lands + 3, () -> {
            helper.assertTrue(hurt(on) && on.hasEffect(MobEffects.WITHER), "the pig on the stroke is not hurt and withering");
            helper.assertTrue(!hurt(beside), "the thorns hurt a pig beside the stroke");
            done(helper);
        });
    }
}
