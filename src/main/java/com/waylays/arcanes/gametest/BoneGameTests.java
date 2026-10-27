package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.bone.BoneColossus;
import com.waylays.arcanes.entity.bone.BoneNova;
import com.waylays.arcanes.entity.bone.BoneTempest;
import com.waylays.arcanes.entity.bone.GiantSpike;
import com.waylays.arcanes.item.BoneBladeItem;
import com.waylays.arcanes.entity.bone.BoneWard;
import com.waylays.arcanes.entity.bone.Grasp;
import com.waylays.arcanes.entity.bone.RibCage;
import com.waylays.arcanes.entity.bone.SpikeField;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.bone.BoneSpell;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Server-side checks run by CI for Bone Magic: the school is there; the lance runs through two enemies; the spikes, the
 * whip, the volley, the ossuary and the wyrm strike; the ward smashes a shot; the rib cage holds its prisoner and grinds
 * it; the grasp lifts its prey and slams it down; the giant skeleton smashes an enemy ahead. Each test runs in a batch
 * of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class BoneGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private BoneGameTests() {}

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

    private static void castAt(GameTestHelper helper, DeferredHolder<AbstractSpell, AbstractSpell> spell, LivingEntity caster, LivingEntity target) {
        MagicData data = new MagicData(true);
        data.setAdditionalCastData(new TargetEntityCastData(target));
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, data);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void boneSchoolIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.BONE_RESOURCE);
        helper.assertTrue(school != null, "Bone school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof BoneSpell, entry.getId() + " is in the Bone school but is not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 15, "expected 15 Bone spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.OSSUARY_CODEX.get()).getItem() instanceof SpellBook, "the Ossuary Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.BONE).is(ModSchools.BONE_FOCUS), "bones are not the school's focus");
        helper.succeed();
    }

    /** The lance runs through the first husk and on into the second. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_lance", timeoutTicks = 60)
    public static void boneLanceRunsThrough(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk near = husk(helper, 4.5, 3.5);
        Husk far = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.BONE_LANCE, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(near), "the lance did not strike the first husk");
            helper.assertTrue(hurt(far), "the lance did not run on into the second husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_spikes", timeoutTicks = 60)
    public static void boneSpikesImpale(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        cast(helper, ModSpells.BONE_SPIKES, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "no spike impaled the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_whip", timeoutTicks = 60)
    public static void spineWhipStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 5.5, 4.5);
        cast(helper, ModSpells.SPINE_WHIP, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the whip did not strike the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_volley", timeoutTicks = 60)
    public static void boneVolleyStrikes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        cast(helper, ModSpells.BONE_VOLLEY, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "no splinter struck the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** A snowball thrown at the warded witch is smashed by a plate, which breaks. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_ward", timeoutTicks = 60)
    public static void boneWardSmashesAShot(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 2.5);
        cast(helper, ModSpells.BONE_WARD, caster);
        BoneWard ward = BoneWard.of(helper.getLevel(), caster);
        helper.assertTrue(ward != null, "no ward rose round the caster");
        int plates = ward.getPlates();
        helper.runAfterDelay(2, () -> {
            Vec3 from = helper.absoluteVec(new Vec3(4.5, 3, 6.5));
            Snowball ball = new Snowball(helper.getLevel(), from.x, from.y, from.z);
            ball.setDeltaMovement(0, 0, -1.0);
            helper.getLevel().addFreshEntity(ball);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(ball.isRemoved(), "the snowball was not smashed");
                helper.assertTrue(ward.getPlates() == plates - 1, "no plate broke smashing the snowball (" + ward.getPlates() + " of " + plates + ")");
                GameTestSetup.clearLeftovers(helper);
                helper.succeed();
            });
        });
    }

    /** The cage holds its prisoner where it closed and grinds it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_cage", timeoutTicks = 100)
    public static void ribCageHoldsAndGrinds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        Vec3 at = husk.position();
        castAt(helper, ModSpells.RIB_CAGE, caster, husk);
        helper.runAfterDelay(RibCage.CLOSE + 2, () -> husk.setDeltaMovement(1, 0, 0));
        helper.runAfterDelay(RibCage.CLOSE + 24, () -> {
            helper.assertTrue(hurt(husk), "the ribs did not grind the husk");
            helper.assertTrue(husk.position().distanceTo(at) < 1, "the husk got out of the cage");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_ossuary", timeoutTicks = 100)
    public static void ossuaryImpales(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        SpikeField.erupt(helper.getLevel(), caster, ModSpells.OSSUARY.get(), SpikeField.Mode.ZONE, husk.position(), 0, 1.2f, 60, 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "no spike of the ossuary impaled the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    /** The hand lifts the husk high in its fist, then slams it down. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_grasp", timeoutTicks = 100)
    public static void skeletalGraspLiftsAndSlams(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double ground = husk.getY();
        castAt(helper, ModSpells.SKELETAL_GRASP, caster, husk);
        helper.runAfterDelay(Grasp.LIFTED + 2, () -> helper.assertTrue(husk.getY() > ground + 2, "the hand did not lift the husk (" + (husk.getY() - ground) + ")"));
        helper.runAfterDelay(Grasp.IMPACT + 2, () -> {
            helper.assertTrue(hurt(husk), "the hand did not crush the husk");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_wyrm", timeoutTicks = 240)
    public static void boneWyrmHunts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        // The wyrm roams past the platform's chunk: keep the chunks around ticking
        List<ChunkPos> forced = GameTestSetup.forceChunksAround(helper, 1);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.BONE_WYRM, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the wyrm never struck the husk");
            GameTestSetup.clearLeftovers(helper);
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x, chunk.z, false));
        });
    }

    /** The skeleton rises behind the caster and smashes a fist down on the husk ahead. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_colossus", timeoutTicks = 160)
    public static void gashadokuroSmashes(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        List<ChunkPos> forced = GameTestSetup.forceChunksAround(helper, 1);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 7.5);
        BoneColossus.raise(helper.getLevel(), caster, ModSpells.GASHADOKURO.get(), 120, 6);
        helper.runAfterDelay(BoneColossus.RISE + BoneColossus.IMPACT + 3, () -> {
            helper.assertTrue(hurt(husk), "the skeleton's fist did not strike the husk");
            GameTestSetup.clearLeftovers(helper);
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x, chunk.z, false));
            helper.succeed();
        });
    }

    /** The colossal spike bursts out under the husk and hurls it high. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_giant_spike", timeoutTicks = 80)
    public static void colossalSpikeHurlsHigh(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, .5, .5);
        Husk husk = husk(helper, 4.5, 4.5);
        double ground = husk.getY();
        boolean[] lifted = {false};
        GiantSpike.raise(helper.getLevel(), caster, ModSpells.COLOSSAL_SPIKE.get(), husk.position(), 10, 4);
        helper.onEachTick(() -> lifted[0] |= husk.getY() > ground + 1);
        helper.runAfterDelay(GiantSpike.TELEGRAPH - 2, () -> helper.assertTrue(!hurt(husk), "the husk was hurt before the spike burst"));
        helper.runAfterDelay(GiantSpike.TELEGRAPH + 12, () -> {
            helper.assertTrue(hurt(husk), "the spike did not impale the husk");
            helper.assertTrue(lifted[0], "the spike did not hurl the husk up");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    /** The blade is put in the empty hand, cuts like a strong sword, and crumbles once its time is up. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_blade", timeoutTicks = 20)
    public static void elbowBladeLendsAWeapon(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        cast(helper, ModSpells.ELBOW_BLADE, player);
        ItemStack blade = player.getMainHandItem();
        helper.assertTrue(blade.is(ModItems.BONE_BLADE.get()), "no bone blade in the empty hand, but " + blade);
        double bare = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) >= bare + BoneBladeItem.damage(1) - .01, "the blade does not add its damage");
        helper.assertTrue(BoneBladeItem.expiresAt(blade) > helper.getLevel().getGameTime(), "the blade is already spent");
        // One that is spent crumbles away at its next tick in an inventory
        ItemStack spent = BoneBladeItem.forge(helper.getLevel(), 1, 0);
        spent.getItem().inventoryTick(spent, helper.getLevel(), player, 0, false);
        helper.assertTrue(spent.isEmpty(), "a spent blade did not crumble");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_howl", timeoutTicks = 100)
    public static void howlingSkullBursts(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.HOWLING_SKULL, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the skull did not strike the husk");
            GameTestSetup.clearLeftovers(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_nova", timeoutTicks = 40)
    public static void boneNovaThrowsBack(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.BONE_NOVA, caster);
        helper.runAfterDelay(BoneNova.GROW + 2, () -> {
            helper.assertTrue(hurt(husk), "the spikes did not strike the husk");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "bone_tempest", timeoutTicks = 80)
    public static void boneTempestLacerates(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 4.5);
        Husk husk = husk(helper, 4.5, 7.5);
        cast(helper, ModSpells.BONE_TEMPEST, caster);
        helper.runAfterDelay(BoneTempest.GROW + 10, () -> {
            helper.assertTrue(hurt(husk), "the tempest did not lacerate the husk");
            GameTestSetup.clearLeftovers(helper);
            helper.succeed();
        });
    }
}
