package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.verdigris.AncientPillar;
import com.waylays.arcanes.entity.verdigris.SunkenTemple;
import com.waylays.arcanes.entity.verdigris.ToppleWall;
import com.waylays.arcanes.entity.verdigris.VineSnare;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.verdigris.VerdigrisSpell;
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

/**
 * Server-side checks run by CI for Moss, Copper and Rust Magic: the school is there; the copper spikes impale, the
 * pillar throws up, the wall falls on what is in front, the rust rusts, the vines hold, the spores poison, the
 * colonnade throws away, the lightning rod strikes, the bell crushes and the sunken temple buries. These spells reach
 * far or linger, so each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class VerdigrisGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private VerdigrisGameTests() {}

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

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void verdigrisSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.VERDIGRIS_RESOURCE);
        helper.assertTrue(school != null, "Moss, Copper and Rust school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof VerdigrisSpell, entry.getId() + " is in the Moss, Copper and Rust school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Moss, Copper and Rust spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.RUSTED_CODEX.get()).getItem() instanceof SpellBook, "the Rusted Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.COPPER_INGOT).is(ModSchools.VERDIGRIS_FOCUS), "copper is not the school's focus");
        helper.succeed();
    }

    /** The spikes burst out under the husk four blocks ahead and leave it bleeding. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_spikes", timeoutTicks = 60)
    public static void copperSpikesImpale(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.COPPER_SPIKES, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the spikes did not hit the husk");
            helper.assertTrue(husk.hasEffect(ModEffects.BLEEDING), "the husk is not bleeding");
        });
    }

    /** The pillar bursts out under the husk the caster looks at and throws it up. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_pillar", timeoutTicks = 60)
    public static void ancientPillarThrowsUp(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        double ground = husk.getY();
        cast(helper, ModSpells.ANCIENT_PILLAR, caster);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(AncientPillar.class, husk.getBoundingBox().inflate(1.5, 3, 1.5)).isEmpty(),
                "no pillar under the husk");
        boolean[] thrown = {false};
        helper.onEachTick(() -> thrown[0] |= husk.getY() > ground + 1.5);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the pillar did not hurt the husk");
            helper.assertTrue(thrown[0], "the pillar did not throw the husk up");
        });
    }

    /** The wall rises in front of the caster and falls flat on the husk beyond it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_wall", timeoutTicks = 80)
    public static void topplingWallCrushes(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 5, 5.5);
        cast(helper, ModSpells.TOPPLING_WALL, caster);
        helper.runAfterDelay(ToppleWall.IMPACT - 3, () -> helper.assertTrue(!hurt(husk), "the wall hurt the husk before it fell"));
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the wall did not fall on the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the husk is not slowed under the wall");
        });
    }

    /** The clot of rust hits the husk and rusts it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_rust", timeoutTicks = 60)
    public static void rustRustsTheEnemy(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.RUST, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the rust did not hit the husk");
            helper.assertTrue(husk.hasEffect(ModEffects.RUSTED), "the husk is not rusted");
        });
    }

    /** Vines seize the husk: it is held in place and squeezed. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_vines", timeoutTicks = 60)
    public static void vinesHoldAndSqueeze(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        MagicData data = new MagicData(true);
        data.setAdditionalCastData(new TargetEntityCastData(husk));
        ModSpells.STRANGLING_VINES.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, data);
        helper.assertTrue(husk.hasEffect(ModEffects.ENTANGLED), "the husk is not entangled");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(VineSnare.class, husk.getBoundingBox().inflate(2)).isEmpty(), "no vines on the husk");
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the vines never squeezed the husk"));
    }

    /** The moss spreads under the husk and its spores choke it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_spores", timeoutTicks = 60)
    public static void sporesPoisonTheEnemy(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.MOSS_SPORES, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the spores did not hurt the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.POISON), "the husk is not poisoned");
        });
    }

    /** The ring of columns bursts up around the caster and throws the husk next to it away. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_colonnade", timeoutTicks = 40)
    public static void colonnadeThrowsAway(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 3.5);
        Husk husk = husk(helper, 4.5, 5.5);
        double start = husk.position().distanceTo(caster.position());
        cast(helper, ModSpells.COLONNADE, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the columns did not hurt the husk");
            helper.assertTrue(husk.position().distanceTo(caster.position()) > start + .5, "the columns did not throw the husk away");
        });
    }

    /** The rod draws lightning on the husk near it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_rod", timeoutTicks = 100)
    public static void lightningRodStrikes(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Husk husk = husk(helper, 6.5, 6.5);
        AncientPillar.raise(helper.getLevel(), caster, ModSpells.ANCIENT_LIGHTNING_ROD.get(), AncientPillar.Kind.ROD,
                helper.absoluteVec(new Vec3(3.5, 2, 3.5)), 0, 200, 1, 5);
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the rod never struck the husk"));
    }

    /** The bell drops on the husk the caster looks at. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_bell", timeoutTicks = 60)
    public static void bellCrushesTheEnemy(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.ANCIENT_BELL, caster);
        helper.runAfterDelay(2, () -> helper.assertTrue(!hurt(husk), "the bell struck before it fell"));
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the bell never crushed the husk"));
    }

    /** The temple rises around the husk, rains rubble on it, then collapses on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "verdigris_temple", timeoutTicks = 160)
    public static void sunkenTempleBuries(GameTestHelper helper) {
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
        husk.setHealth(400);
        SunkenTemple.raise(helper.getLevel(), caster, ModSpells.SUNKEN_TEMPLE.get(), husk.position(), 8);
        float[] before = {0};
        helper.runAfterDelay(SunkenTemple.IMPACT - 2, () -> {
            helper.assertTrue(hurt(husk), "no rubble fell on the husk inside");
            before[0] = husk.getHealth();
        });
        helper.runAfterDelay(SunkenTemple.IMPACT + 4, () -> {
            helper.assertTrue(husk.getHealth() < before[0], "the columns' collapse did not hurt the husk");
            helper.succeed();
        });
    }
}
