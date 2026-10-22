package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.wall.Citadel;
import com.waylays.arcanes.entity.wall.Rampart;
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
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Wall Magic: the school is there; the volley, the rain, the burst, the mortar, the
 * crushing walls and the charge hurt; the rising wall, the oubliette and the bastion are built of real bricks (the wall
 * stops a shot, the oubliette shuts its prisoner in) and are taken away when they end; the fortress shells the enemy
 * and then blows apart on it. Each test runs in a batch of its own.
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
     * Takes down what the test raised (ramparts, walls, bricks, the fortress): they outlast the test, and left standing
     * they would stop the shots of the tests run next on the same spot. A rampart taken down puts its blocks back.
     */
    private static void cleanUp(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
    }

    /** Whether the block at (x, y, z) is one of Wall Magic's three bricks. */
    private static boolean brick(GameTestHelper helper, int x, int y, int z) {
        BlockState state = helper.getBlockState(new BlockPos(x, y, z));
        return state.is(Blocks.STONE_BRICKS) || state.is(Blocks.MOSSY_STONE_BRICKS) || state.is(Blocks.CRACKED_STONE_BRICKS);
    }

    private static Rampart rampart(GameTestHelper helper, LivingEntity near) {
        var found = helper.getLevel().getEntitiesOfClass(Rampart.class, near.getBoundingBox().inflate(8));
        helper.assertTrue(found.size() == 1, "expected 1 rampart, found " + found.size());
        return found.get(0);
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
        // No clean-up here: this test raises nothing, and it runs alongside other schools' tests whose shots it would take
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

    /**
     * The wall is built of real bricks 3 blocks ahead (x 2 to 6, rows y 2 to 4 at z 3), a snowball thrown at the
     * caster breaks on it, and the bricks are gone once the wall has crumbled.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_rising", timeoutTicks = 80)
    public static void risingWallStopsAShot(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        cast(helper, ModSpells.RISING_WALL, caster);
        Rampart wall = rampart(helper, caster);
        helper.runAfterDelay(wall.built() + 1, () -> {
            for (int x = 2; x <= 6; x++) {
                for (int y = 2; y <= 4; y++) {
                    helper.assertTrue(brick(helper, x, y, 3), "no brick at " + x + " " + y + " 3");
                }
            }
            Vec3 from = helper.absoluteVec(new Vec3(4.5, 3.2, 7.5));
            Snowball ball = new Snowball(helper.getLevel(), from.x, from.y, from.z);
            ball.setDeltaMovement(0, 0, -1.0);
            helper.getLevel().addFreshEntity(ball);
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(ball.isRemoved(), "the snowball was not stopped");
                helper.assertTrue(helper.relativeVec(ball.position()).z > 2.5, "the snowball went through the wall");
                wall.crumbleNow();
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(!brick(helper, 4, 2, 3), "the bricks were left behind when the wall crumbled");
                    cleanUp(helper);
                    helper.succeed();
                });
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
        helper.runAfterDelay(WallPiece.BURST_DELAY, () -> helper.assertTrue(!hurt(husk), "the wall burst before it was built"));
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

    /** The husk is walled in a closed cell of real bricks: a ring round its block (4, 2, 4), three high, and a lid. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_cage", timeoutTicks = 60)
    public static void brickCageShutsIn(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        castAt(helper, ModSpells.BRICK_CAGE, caster, husk);
        helper.assertTrue(hurt(husk) && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the walled-in husk is not hurt and slowed");
        Rampart cage = rampart(helper, husk);
        helper.runAfterDelay(cage.built() + 1, () -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        for (int y = 2; y <= 4; y++) {
                            helper.assertTrue(brick(helper, 4 + dx, y, 4 + dz), "the cell has a gap at " + (4 + dx) + " " + y + " " + (4 + dz));
                        }
                    }
                    helper.assertTrue(brick(helper, 4 + dx, 5, 4 + dz), "the lid has a gap at " + (4 + dx) + " 5 " + (4 + dz));
                }
            }
            Vec3 in = helper.relativeVec(husk.position());
            helper.assertTrue(husk.isAlive() && in.x > 4 && in.x < 5 && in.z > 4 && in.z < 5,
                    "the husk is not in its cell: at " + in + ", alive " + husk.isAlive());
            helper.assertTrue(husk.hasEffect(MobEffects.DARKNESS), "the husk is not in the dark");
            cleanUp(helper);
            helper.succeed();
        });
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

    /** A keep of real bricks rises round the caster: walls 3 blocks out on every side, a taller tower at each corner. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_bastion", timeoutTicks = 80)
    public static void bastionStandsAround(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        cast(helper, ModSpells.BASTION, caster);
        helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the caster does not resist damage");
        Rampart keep = rampart(helper, caster);
        helper.runAfterDelay(keep.built() + 1, () -> {
            for (int i = 1; i <= 7; i++) {
                for (int y = 2; y <= 4; y++) {
                    helper.assertTrue(brick(helper, i, y, 1) && brick(helper, i, y, 7) && brick(helper, 1, y, i) && brick(helper, 7, y, i),
                            "the keep has a gap in row " + y + " at " + i);
                }
            }
            helper.assertTrue(brick(helper, 1, 5, 1) && brick(helper, 7, 6, 7), "the corner towers are missing");
            helper.assertTrue(!brick(helper, 4, 2, 4) && caster.isAlive(), "the keep was built over the caster");
            cleanUp(helper);
            helper.succeed();
        });
    }

    /**
     * The fortress rises round the husk (its wall and towers out beyond the test's barrier walls; the towers lob their
     * shells over them), the towers shell the husk, then the fortress blows apart on it.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "wall_citadel", timeoutTicks = 300)
    public static void citadelShellsThenFalls(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        husk.setHealth(400);
        Citadel.raise(helper.getLevel(), caster, ModSpells.FALLEN_CITADEL.get(), husk.position(), 8);
        float[] before = {0};
        helper.runAfterDelay(Citadel.RISEN + 2 * Citadel.VOLLEY_TICKS + 30, () -> {
            helper.assertTrue(hurt(husk), "no shell from the towers hit the husk");
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE), "the caster is not shielded by the fortress");
        });
        helper.runAfterDelay(Citadel.COLLAPSE - 1, () -> before[0] = husk.getHealth());
        helper.runAfterDelay(Citadel.COLLAPSE + 2, () -> {
            helper.assertTrue(husk.getHealth() < before[0], "the fortress blowing apart did not hurt the husk");
            cleanUp(helper);
            helper.succeed();
        });
    }
}
