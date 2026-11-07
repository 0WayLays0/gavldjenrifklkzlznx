package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.mend.MendScene;
import com.waylays.arcanes.entity.trap.TrapScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.mend.MendSpell;
import com.waylays.arcanes.spells.trap.TrapSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Server-side checks run by CI for Mending Magic and Trap Magic: both schools are there with their ten spells; healing
 * reaches the one ally it is laid on (the thread, the echo, suture, purge, the bud blooming, the wisp answering a wound,
 * Rewind undoing wounds, the cocoon letting no blow through, a second life undoing a death); traps spring on enemies,
 * curse them and never root or stun them; the echo trap grows stronger; the plague passes on; the cursed web curses.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class MendTrapGameTests {
    private MendTrapGameTests() {}

    private static Witch witch(GameTestHelper helper, double x, double z) {
        Witch w = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(x, 2, z));
        w.setYRot(0);
        return w;
    }

    private static Husk husk(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, 2, z));
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    private static int count(SchoolType school, Class<?> base, GameTestHelper helper) {
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(base.isInstance(spell), entry.getId() + " is in the school but not one of its spells");
                count++;
            }
        }
        return count;
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void mendAndTrapSchoolsAreRegistered(GameTestHelper helper) {
        SchoolType mend = SchoolRegistry.getSchool(ModSchools.MEND_RESOURCE), trap = SchoolRegistry.getSchool(ModSchools.TRAP_RESOURCE);
        helper.assertTrue(mend != null && trap != null, "a school is missing");
        helper.assertTrue(count(mend, MendSpell.class, helper) == 10, "expected 10 Mending spells");
        helper.assertTrue(count(trap, TrapSpell.class, helper) == 10, "expected 10 Trap spells");
        helper.assertTrue(new ItemStack(ModItems.CODEX_OF_MENDING.get()).getItem() instanceof SpellBook, "the Codex of Mending is not a spellbook");
        helper.assertTrue(new ItemStack(ModItems.TRAPPERS_CODEX.get()).getItem() instanceof SpellBook, "the Trapper's Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.GOLDEN_APPLE).is(ModSchools.MEND_FOCUS) && new ItemStack(Items.TRIPWIRE_HOOK).is(ModSchools.TRAP_FOCUS), "focus tags");
        helper.succeed();
    }

    /** The thread heals the ally it is tied to; the echo heals three times; suture heals more the deeper the wound. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "mend_heals", timeoutTicks = 120)
    public static void healsReachTheirAlly(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = witch(helper, 2.5, 2.5);
        Witch tied = witch(helper, 6.5, 2.5);
        Witch echoed = witch(helper, 6.5, 6.5);
        tied.setHealth(4);
        echoed.setHealth(4);
        MendScene thread = MendScene.spawn(helper.getLevel(), caster, tied, MendScene.Kind.THREAD, 120, 2);
        MendScene echo = MendScene.spawn(helper.getLevel(), caster, echoed, MendScene.Kind.ECHO, 110, 4);
        helper.runAfterDelay(25, () -> helper.assertTrue(tied.getHealth() > 5, "the thread did not heal its ally"));
        helper.runAfterDelay(MendScene.ECHO_2 + 3, () -> {
            helper.assertTrue(echo.healed() >= 6.9f, "the echoes did not all heal (" + echo.healed() + ")");
            helper.assertTrue(thread.healed() > 0, "the thread healed nothing");
            done(helper);
        });
    }

    /** The bud blooms when its ally falls below half; the wisp heals half of a wound; the cocoon lets no blow through. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "mend_guard", timeoutTicks = 80)
    public static void budWispAndCocoon(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = witch(helper, 4.5, .5);
        Witch budded = witch(helper, 1.5, 4.5);
        Witch guarded = witch(helper, 4.5, 4.5);
        Witch cocooned = witch(helper, 7.5, 4.5);
        MendScene bud = MendScene.spawn(helper.getLevel(), caster, budded, MendScene.Kind.BUD, 400, 6);
        MendScene wisp = MendScene.spawn(helper.getLevel(), caster, guarded, MendScene.Kind.WISP, 300, 0);
        MendScene.spawn(helper.getLevel(), caster, cocooned, MendScene.Kind.COCOON, 80, 4);
        helper.runAfterDelay(5, () -> {
            budded.setHealth(budded.getMaxHealth() * .3f);
            guarded.hurt(helper.getLevel().damageSources().generic(), 8);
            float before = cocooned.getHealth();
            cocooned.hurt(helper.getLevel().damageSources().generic(), 8);
            helper.assertTrue(cocooned.getHealth() >= before, "a blow passed the cocoon");
        });
        helper.runAfterDelay(5 + MendScene.WISP_DELAY + 4, () -> {
            helper.assertTrue(bud.isDone() && bud.healed() > 0, "the bud did not bloom");
            helper.assertTrue(wisp.healed() >= 3.9f, "the wisp did not heal half of the wound (" + wisp.healed() + ")");
            done(helper);
        });
    }

    /** Rewind brings back the health an ally had before its wounds; a second life undoes a death. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "mend_rewind", timeoutTicks = 60)
    public static void rewindAndSecondLife(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = witch(helper, 4.5, .5);
        Witch hurt = witch(helper, 2.5, 4.5);
        Witch doomed = witch(helper, 6.5, 4.5);
        MendScene.spawn(helper.getLevel(), caster, doomed, MendScene.Kind.SECOND, 400, 0);
        helper.runAfterDelay(3, () -> {
            hurt.hurt(helper.getLevel().damageSources().generic(), 10);
            doomed.hurt(helper.getLevel().damageSources().genericKill(), 1000);
        });
        helper.runAfterDelay(8, () -> {
            float wounded = hurt.getHealth();
            hurt.invulnerableTime = 0;
            ModSpells.REWIND.get().onCast(helper.getLevel(), 1, hurt, CastSource.COMMAND, new MagicData(true));
            helper.assertTrue(hurt.getHealth() > wounded + 9, "Rewind did not undo the wound (" + wounded + " -> " + hurt.getHealth() + ")");
            helper.assertTrue(doomed.isAlive() && doomed.getHealth() >= doomed.getMaxHealth() * .4f, "the second life did not undo the death");
            done(helper);
        });
    }

    /** Traps curse the enemy that steps in, never their caster, and never root or stun it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "trap_spring", timeoutTicks = 60)
    public static void trapsCurseWithoutStun(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = witch(helper, 4.5, .5);
        Husk a = husk(helper, 1.5, 4.5), b = husk(helper, 4.5, 4.5), c = husk(helper, 7.5, 4.5);
        TrapScene.spawn(helper.getLevel(), caster, TrapScene.Kind.SNARE, helper.absoluteVec(new Vec3(1.5, 2, 4.5)), 1, 600, 1, 0);
        TrapScene.spawn(helper.getLevel(), caster, TrapScene.Kind.RUST, helper.absoluteVec(new Vec3(4.5, 2, 4.5)), 1, 600, 1, 0);
        TrapScene.spawn(helper.getLevel(), caster, TrapScene.Kind.BLEED, helper.absoluteVec(new Vec3(7.5, 2, 4.5)), 1, 600, 1, 0);
        TrapScene.spawn(helper.getLevel(), caster, TrapScene.Kind.HEX, helper.absoluteVec(new Vec3(4.5, 2, .5)), 1, 600, 1, 0);
        helper.runAfterDelay(TrapScene.ARM + 4, () -> {
            helper.assertTrue(a.hasEffect(MobEffects.POISON) && a.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the snare did not poison and slow");
            helper.assertTrue(b.hasEffect(ModEffects.RUSTED) && b.hasEffect(ModEffects.SUNDERED), "the rust jaw did not rust and sunder");
            helper.assertTrue(c.hasEffect(ModEffects.BLEEDING), "the bloodletter did not bleed");
            helper.assertTrue(caster.getActiveEffects().isEmpty(), "a trap cursed its caster");
            for (LivingEntity e : new LivingEntity[] {a, b, c}) {
                helper.assertTrue(!e.hasEffect(ModEffects.ENTANGLED) && !e.hasEffect(ModEffects.PINNED) && !e.hasEffect(MobEffects.LEVITATION),
                        "a trap rooted or held its enemy");
                MobEffectInstance slow = e.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                helper.assertTrue(slow == null || slow.getAmplifier() < 3, "a trap all but stopped its enemy");
            }
            done(helper);
        });
    }

    /** The echo trap springs again on the same enemy, stronger; the plague passes on; the cursed web curses. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "trap_lasting", timeoutTicks = 120)
    public static void echoPlagueAndWeb(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = witch(helper, 4.5, .5);
        Husk echoed = husk(helper, 1.5, 7.5);
        Husk carrier = husk(helper, 7.5, 7.5), near = husk(helper, 7.5, 5.5);
        Husk webbed = husk(helper, 4.5, 4.5);
        TrapScene.spawn(helper.getLevel(), caster, TrapScene.Kind.ECHO, helper.absoluteVec(new Vec3(1.5, 2, 7.5)), 1, 600, 1, 0);
        TrapScene.spawn(helper.getLevel(), caster, TrapScene.Kind.PLAGUE, helper.absoluteVec(new Vec3(7.5, 2, 7.5)), 1, 600, 1, 0);
        TrapScene web = TrapScene.spawn(helper.getLevel(), caster, TrapScene.Kind.WEB, helper.absoluteVec(new Vec3(4.5, 2, 4.5)), 3, 400, 1, 0);
        helper.runAfterDelay(TrapScene.ARM + TrapScene.ECHO_REST + 6, () -> {
            MobEffectInstance slow = echoed.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slow != null && slow.getAmplifier() >= 1, "the echo trap did not spring again, stronger");
            helper.assertTrue(near.hasEffect(MobEffects.POISON), "the plague did not pass on");
            helper.assertTrue(!webbed.getActiveEffects().isEmpty(), "the cursed web cursed nothing");
            helper.assertTrue(web.isSprung() || web.sinceStruck(0) < 1000, "the cursed web did not flare");
            done(helper);
        });
    }
}
