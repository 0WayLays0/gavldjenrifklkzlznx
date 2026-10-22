package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.wall.BrickBolt;
import com.waylays.arcanes.entity.wall.Citadel;
import com.waylays.arcanes.entity.wall.WallPiece;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.wall.WallSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Wall Magic: the school is there; the brick, the rain, the burst, the mortar, the
 * crushing walls, the cage and the charge hurt; the rising wall stops a shot; the bastion stands round the caster; the
 * citadel lobs bricks and then falls on what is inside. Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class WallGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private WallGameTests() {}

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

    /**
     * Takes down what the test raised (walls, bricks, citadels): they outlast the test, and left standing they would
     * stop the shots of the tests run next on the same spot.
     */
    private static void cleanUp(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(Entity.class, helper.getBounds().inflate(16),
                e -> e instanceof WallPiece || e instanceof Citadel || e instanceof BrickBolt).forEach(Entity::discard);
    }

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    private static void castAt(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster, LivingEntity target) {
        MagicData data = new MagicData(true);
        data.setAdditionalCastData(new TargetEntityCastData(target));
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, data);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void wallSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.WALL_RESOURCE);
        helper.assertTrue(school != null, "Wall school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof WallSpell, entry.getId() + " is in the Wall school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Wall spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.RAMPART_CODEX.get()).getItem() instanceof SpellBook, "the Rampart Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.STONE_BRICKS).is(ModSchools.WALL_FOCUS), "stone bricks are not the school's focus");
        cleanUp(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_brick", timeoutTicks = 40)
    public static void brickBoltHits(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.BRICK_BOLT, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the brick did not hit the husk");
            cleanUp(helper);
        });
    }

    /** A snowball thrown at the caster through the wall is stopped by it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_rising", timeoutTicks = 60)
    public static void risingWallStopsAShot(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        cast(helper, ModSpells.RISING_WALL, caster);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(WallPiece.class, caster.getBoundingBox().inflate(6)).isEmpty(), "no wall rose");
        helper.runAfterDelay(WallPiece.RISE + 2, () -> {
            Vec3 from = helper.absoluteVec(new Vec3(4.5, 3.2, 7.5));
            Snowball ball = new Snowball(helper.getLevel(), from.x, from.y, from.z);
            ball.setDeltaMovement(0, 0, -1.0);
            helper.getLevel().addFreshEntity(ball);
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(ball.isRemoved(), "the snowball went through the wall");
                helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the snowball reached the caster");
                cleanUp(helper);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_rain", timeoutTicks = 120)
    public static void brickRainFalls(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.BRICK_RAIN, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "no brick fell on the husk");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_burst", timeoutTicks = 50)
    public static void wallBurstsOnTheEnemy(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.WALL_BURST, caster);
        helper.runAfterDelay(WallPiece.RISE + WallPiece.BURST_DELAY - 2, () -> helper.assertTrue(!hurt(husk), "the wall burst too early"));
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the bursting wall did not hit the husk");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_mortar", timeoutTicks = 60)
    public static void mortarLands(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.MORTAR, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the mortar did not hurt the husk");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_crush", timeoutTicks = 50)
    public static void crushingWallsCrush(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        castAt(helper, ModSpells.CRUSHING_WALLS, caster, husk);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the walls did not crush the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.CONFUSION), "the husk is not dazed");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_cage", timeoutTicks = 30)
    public static void brickCageShutsIn(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        castAt(helper, ModSpells.BRICK_CAGE, caster, husk);
        int bars = helper.getLevel().getEntitiesOfClass(WallPiece.class, husk.getBoundingBox().inflate(3)).size();
        helper.assertTrue(bars == 8, "expected 8 bars around the husk, found " + bars);
        helper.assertTrue(hurt(husk) && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the caged husk is not hurt and slowed");
        cleanUp(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_charge", timeoutTicks = 50)
    public static void rampartChargeSweeps(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double start = husk.getZ();
        cast(helper, ModSpells.RAMPART_CHARGE, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the rampart did not hit the husk");
            helper.assertTrue(husk.getZ() > start + 1, "the rampart did not sweep the husk away");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_bastion", timeoutTicks = 20)
    public static void bastionStandsAround(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        cast(helper, ModSpells.BASTION, caster);
        int walls = helper.getLevel().getEntitiesOfClass(WallPiece.class, caster.getBoundingBox().inflate(4)).size();
        helper.assertTrue(walls == 4, "expected 4 walls around the caster, found " + walls);
        helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the caster does not resist damage");
        cleanUp(helper);
        helper.succeed();
    }

    /** The citadel rises round the husk, its towers lob bricks on it, then it falls on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_citadel", timeoutTicks = 160)
    public static void citadelFallsOnTheEnemy(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        husk.setHealth(400);
        Citadel citadel = Citadel.raise(helper.getLevel(), caster, ModSpells.FALLEN_CITADEL.get(), husk.position(), 8);
        // Towers on the diagonals: they stand 6.5 blocks out, and only on the diagonals is that inside the barrier walls
        // around the test (bricks lobbed from outside them would hit the barriers)
        citadel.setYRot(45);
        float[] before = {0};
        helper.runAfterDelay(Citadel.IMPACT - 2, () -> {
            helper.assertTrue(hurt(husk), "no brick from the towers hit the husk");
            before[0] = husk.getHealth();
        });
        helper.runAfterDelay(Citadel.IMPACT + 4, () -> {
            helper.assertTrue(husk.getHealth() < before[0], "the citadel's fall did not hurt the husk");
            cleanUp(helper);
            helper.succeed();
        });
    }
}
