package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.aurora.AuroraScene;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.aurora.AuroraSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.joml.Vector3f;

/**
 * Server-side checks run by CI for Aurora Magic: the school is there with its ten spells; the lance goes through and
 * slows, the wisps each find their enemy, the curtain sears what is in it and spares what is beside it, the glide strikes
 * what it passes, the mantle hardens its wearer and sears who comes close, the blessing heals, the spiral sears, the
 * serpent dives on its prey, the halo's beams fall, the Boreal Night's waves sweep and its lights gather. Each test runs
 * in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class AuroraGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private AuroraGameTests() {}

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

    private static void cast(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster) {
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
    }

    private static AuroraScene scene(GameTestHelper helper, LivingEntity caster, DeferredHolder<AbstractSpell, AbstractSpell> spell, AuroraScene.Kind kind,
                                     double x, double y, double z, Vector3f dir, float size, int life, float damage) {
        return AuroraScene.spawn(helper.getLevel(), caster, spell.get(), kind, helper.absoluteVec(new Vec3(x, y, z)), dir, size, life, damage);
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void auroraSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.AURORA_RESOURCE);
        helper.assertTrue(school != null, "Aurora school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof AuroraSpell, entry.getId() + " is in the Aurora school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Aurora spells, found " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.AURORA_MAGIC),
                "aurora_magic damage type missing");
        helper.assertTrue(new ItemStack(ModItems.BOREAL_CODEX.get()).getItem() instanceof SpellBook, "the Boreal Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.BLUE_ICE).is(ModSchools.AURORA_FOCUS), "blue ice is not the school's focus");
        helper.succeed();
    }

    /** The lance goes through the enemies in its way, slowing each, and spares its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_lance", timeoutTicks = 60)
    public static void lanceGoesThroughAndSlows(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk first = husk(helper, 4.5, 3.5);
        Husk second = husk(helper, 4.5, 6.5);
        scene(helper, caster, ModSpells.AURORA_LANCE, AuroraScene.Kind.LANCE, 4.5, 3, 1.6, new Vector3f(0, 0, 1), 1, 40, 3);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(first), "the lance did not strike the first husk");
            helper.assertTrue(hurt(second), "the lance did not go through to the second husk");
            helper.assertTrue(first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && second.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husks struck by the lance are not slowed");
            helper.assertTrue(!hurt(caster), "the lance struck its caster");
            done(helper);
        });
    }

    /** Two wisps each find their own enemy and burst on it, marking it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_wisps", timeoutTicks = 100)
    public static void wispsEachFindTheirEnemy(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk left = husk(helper, 1.5, 5.5);
        Husk right = husk(helper, 7.5, 7.5);
        scene(helper, caster, ModSpells.POLAR_WISPS, AuroraScene.Kind.WISP, 4.5, 3.5, 1.5, new Vector3f(-1, 0, 0), .5f, 80, 3).index(0);
        scene(helper, caster, ModSpells.POLAR_WISPS, AuroraScene.Kind.WISP, 4.5, 3.5, 1.5, new Vector3f(1, 0, 0), .5f, 80, 3).index(1);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(left) && hurt(right), "the wisps have not both struck their husk yet");
            helper.assertTrue(left.hasEffect(MobEffects.GLOWING) && right.hasEffect(MobEffects.GLOWING), "the husks struck by the wisps are not marked");
            helper.assertTrue(!hurt(caster), "a wisp struck its caster");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The curtain sears, slows and marks the enemy standing in it, and spares the one beside it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_curtain", timeoutTicks = 60)
    public static void curtainSearsWhatIsInIt(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk inside = husk(helper, 5.5, 4.5);
        Husk beside = husk(helper, 4.5, 7.5);
        scene(helper, caster, ModSpells.BOREALIS_CURTAIN, AuroraScene.Kind.CURTAIN, 4.5, 2, 4.5, new Vector3f(1, 0, 0), AuroraScene.CURTAIN_LENGTH, 120, 3);
        helper.runAfterDelay(AuroraScene.CURTAIN_EVERY + 3, () -> {
            helper.assertTrue(hurt(inside), "the curtain did not sear the husk in it");
            helper.assertTrue(inside.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && inside.hasEffect(MobEffects.GLOWING), "the husk in the curtain is not slowed and marked");
            helper.assertTrue(!hurt(beside), "the curtain seared a husk beside it");
            helper.assertTrue(!hurt(caster), "the curtain seared its caster");
            done(helper);
        });
    }

    /** The glide strikes and marks the enemy on the rider's way, and not the one off it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_glide", timeoutTicks = 60)
    public static void glideStrikesWhatItPasses(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk onTheWay = husk(helper, 4.5, 4.5);
        Husk offTheWay = husk(helper, .5, 4.5);
        scene(helper, caster, ModSpells.AURORA_GLIDE, AuroraScene.Kind.GLIDE, 4.5, 2, .5, new Vector3f(0, 0, 1), 1, 30, 3).follow(caster);
        helper.runAfterDelay(2, () -> {
            Vec3 to = helper.absoluteVec(new Vec3(4.5, 2, 8.5));
            caster.teleportTo(to.x, to.y, to.z);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(hurt(onTheWay), "the glide did not strike the husk on its way");
            helper.assertTrue(onTheWay.hasEffect(MobEffects.GLOWING), "the husk struck by the glide is not marked");
            helper.assertTrue(!hurt(offTheWay), "the glide struck a husk off its way");
            helper.assertTrue(!hurt(caster), "the glide hurt its rider");
            done(helper);
        });
    }

    /** The mantle hardens and quickens its wearer, and sears and slows the enemy who comes close. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_mantle", timeoutTicks = 60)
    public static void mantleHardensAndSears(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 6, 4.5);
        cast(helper, ModSpells.AURORA_MANTLE, caster);
        helper.runAfterDelay(AuroraScene.MANTLE_EVERY + 3, () -> {
            helper.assertTrue(caster.hasEffect(MobEffects.DAMAGE_RESISTANCE) && caster.hasEffect(MobEffects.MOVEMENT_SPEED), "the mantle did not harden and quicken its wearer");
            helper.assertTrue(hurt(husk), "the mantle did not sear the husk close by");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk seared by the mantle is not slowed");
            helper.assertTrue(!hurt(caster), "the mantle seared its wearer");
            done(helper);
        });
    }

    /** The blessing heals its caster, shields it and lets it regenerate, and does nothing for the enemy beside it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_blessing", timeoutTicks = 60)
    public static void blessingHealsTheCaster(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        Husk husk = husk(helper, 6.5, 4.5);
        husk.setHealth(10);
        scene(helper, caster, ModSpells.BOREAL_BLESSING, AuroraScene.Kind.BLESSING, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 8, 40, 6);
        helper.runAfterDelay(AuroraScene.BLESSING_LAND - 4, () -> helper.assertTrue(caster.getHealth() <= 10, "the blessing healed before it came down"));
        helper.runAfterDelay(AuroraScene.BLESSING_LAND + 2, () -> {
            helper.assertTrue(caster.getHealth() > 10, "the blessing did not heal its caster");
            helper.assertTrue(caster.hasEffect(MobEffects.REGENERATION) && caster.hasEffect(MobEffects.ABSORPTION), "the blessing did not shield its caster nor let it regenerate");
            helper.assertTrue(husk.getHealth() <= 10 && !husk.hasEffect(MobEffects.REGENERATION), "the blessing healed the husk");
            done(helper);
        });
    }

    /** The spiral sears and marks the enemy it draws in. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_spiral", timeoutTicks = 60)
    public static void spiralSears(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 6.5, 4.5);
        scene(helper, caster, ModSpells.CELESTIAL_SPIRAL, AuroraScene.Kind.SPIRAL, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 4, 120, 3);
        helper.runAfterDelay(AuroraScene.SPIRAL_EVERY + 3, () -> {
            helper.assertTrue(hurt(husk), "the spiral did not sear the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.GLOWING), "the husk in the spiral is not marked");
            helper.assertTrue(!hurt(caster), "the spiral seared its caster");
            done(helper);
        });
    }

    /** The serpent dives on the nearest enemy and strikes it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_serpent", timeoutTicks = 100)
    public static void serpentDivesOnItsPrey(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 2.5, 2.5);
        Husk husk = husk(helper, 6.5, 6.5);
        scene(helper, caster, ModSpells.AURORA_SERPENT, AuroraScene.Kind.SERPENT, 2.5, 2, 2.5, new Vector3f(0, 1, 0), 1, 200, 3);
        helper.runAfterDelay(AuroraScene.SERPENT_FIRST + 2, () -> helper.assertTrue(!hurt(husk), "the serpent struck before its head came down"));
        helper.runAfterDelay(AuroraScene.SERPENT_FIRST + AuroraScene.SERPENT_DIVE / 2 + 4, () -> {
            helper.assertTrue(hurt(husk), "the serpent's dive did not strike the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.GLOWING), "the husk struck by the serpent is not marked");
            helper.assertTrue(!hurt(caster), "the serpent struck its caster");
            done(helper);
        });
    }

    /** The halo's first beam falls on the enemy beneath it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_halo", timeoutTicks = 60)
    public static void haloBeamsFall(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 5.5, 4.5);
        scene(helper, caster, ModSpells.POLAR_HALO, AuroraScene.Kind.HALO, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 4, 120, 3);
        helper.runAfterDelay(AuroraScene.HALO_OPEN - 4, () -> helper.assertTrue(!hurt(husk), "the halo struck before it opened"));
        helper.runAfterDelay(AuroraScene.HALO_OPEN + 3, () -> {
            helper.assertTrue(hurt(husk), "the halo's beam did not strike the husk beneath it");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk struck by the halo is not slowed");
            helper.assertTrue(!hurt(caster), "the halo struck its caster");
            done(helper);
        });
    }

    /** The Boreal Night's first wave sweeps over the enemy while its caster regenerates; then its lights gather and strike. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "aurora_night", timeoutTicks = AuroraScene.NIGHT_GATHER + 40)
    public static void nightSweepsThenGathers(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        caster.setHealth(10);
        Husk husk = husk(helper, 7.5, 4.5);
        scene(helper, caster, ModSpells.BOREAL_NIGHT, AuroraScene.Kind.NIGHT, 4.5, 2, 4.5, new Vector3f(0, 1, 0), 8, AuroraScene.NIGHT_GATHER + 30, 4);
        helper.runAfterDelay(AuroraScene.NIGHT_FALL - 4, () -> helper.assertTrue(!hurt(husk), "the night struck before its first wave"));
        helper.runAfterDelay(AuroraScene.NIGHT_FALL + AuroraScene.NIGHT_SWEEP + 2, () -> {
            helper.assertTrue(hurt(husk), "the first wave did not sweep over the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && husk.hasEffect(MobEffects.GLOWING), "the husk swept by the wave is not slowed and marked");
            helper.assertTrue(caster.hasEffect(MobEffects.REGENERATION), "the caster does not regenerate under the night");
        });
        helper.runAfterDelay(AuroraScene.NIGHT_GATHER + 2, () -> {
            helper.assertTrue(!husk.isAlive() || husk.hasEffect(MobEffects.LEVITATION), "the gathered lights did not strike the husk");
            helper.assertTrue(caster.getHealth() > 10, "the night did not heal its caster");
            done(helper);
        });
    }
}
