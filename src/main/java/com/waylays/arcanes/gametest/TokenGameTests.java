package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.token.TokenAegis;
import com.waylays.arcanes.entity.token.TokenHand;
import com.waylays.arcanes.entity.token.TokenMarker;
import com.waylays.arcanes.entity.token.TokenMine;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.token.TokenSpell;
import com.waylays.arcanes.spells.token.TokenThrowSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for Token Magic: the school is there; a thrown token hurts and stays where it lands,
 * and the caster can teleport to it; the ricochet bounces on; the wave, the storm, the trap tokens, the tribute (which
 * heals), the coin flip and the Hand of Souls work; the aegis catches a blow. Token spells reach far or linger, so each
 * test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class TokenGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private TokenGameTests() {}

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
    public static void tokenSchoolIsRegistered(GameTestHelper helper) {
        SchoolType token = SchoolRegistry.getSchool(ModSchools.TOKEN_RESOURCE);
        helper.assertTrue(token != null, "Token school missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == token) {
                helper.assertTrue(spell instanceof TokenSpell, entry.getId() + " is in the Token school but is not a token spell");
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Token spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.SOUL_LEDGER.get()).getItem() instanceof SpellBook, "the Soul Ledger is not a spellbook");
        helper.assertTrue(new ItemStack(ModItems.SOUL_TOKEN.get()).is(ModSchools.TOKEN_FOCUS), "the Soul Token is not the school's focus");
        helper.succeed();
    }

    /** The thrown token hurts what it hits and stays where it fell. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_throw", timeoutTicks = 60)
    public static void thrownTokenHurtsAndStays(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.TOKEN_THROW, caster);
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the token did not hit the husk");
            helper.assertTrue(TokenMarker.find(helper.getLevel(), caster.getUUID()) != null, "no token left where it fell");
        });
    }

    /** The caster steps through to their token. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_teleport")
    public static void casterTeleportsToTheToken(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Vec3 spot = helper.absoluteVec(new Vec3(7.5, 2, 7.5));
        TokenMarker.place(helper.getLevel(), caster, spot, 200);
        helper.assertTrue(TokenThrowSpell.teleport(helper.getLevel(), caster), "found no token to teleport to");
        helper.assertTrue(caster.position().distanceTo(spot) < 1.5, "the caster is " + caster.position().distanceTo(spot) + " blocks from the token");
        helper.assertTrue(TokenMarker.find(helper.getLevel(), caster.getUUID()) == null, "the token is still there after the teleport");
        helper.succeed();
    }

    /** The ricochet hits the first husk, then bounces to the two behind it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_ricochet", timeoutTicks = 60)
    public static void ricochetBouncesOn(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, .5);
        Husk first = husk(helper, 4.5, 4.5);
        Husk second = husk(helper, 2.5, 7.5);
        Husk third = husk(helper, 6.5, 7.5);
        cast(helper, ModSpells.TOKEN_RICOCHET, caster);
        helper.succeedWhen(() -> helper.assertTrue(hurt(first) && hurt(second) && hurt(third), "the token did not bounce through all three husks"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_wave", timeoutTicks = 60)
    public static void waveRollsOverTheEnemy(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.TOKEN_WAVE, caster);
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the wave did not strike the husk"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_storm", timeoutTicks = 60)
    public static void stormSlashesTheEnemyInside(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 2.5);
        Husk husk = husk(helper, 4.5, 4.5);
        cast(helper, ModSpells.TOKEN_STORM, caster);
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the storm did not slash the husk inside it"));
    }

    /** A blow on the caster is caught by a token: no damage, one token fewer. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_aegis")
    public static void aegisCatchesABlow(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 2.5);
        Husk husk = husk(helper, 4.5, 4.5);
        cast(helper, ModSpells.TOKEN_AEGIS, caster);
        TokenAegis aegis = TokenAegis.of(helper.getLevel(), caster);
        helper.assertTrue(aegis != null, "no tokens around the caster");
        int before = aegis.getCount();
        caster.hurt(helper.getLevel().damageSources().mobAttack(husk), 4);
        helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "the blow went through the aegis");
        helper.assertTrue(aegis.getCount() == before - 1, "no token was spent on the blow");
        helper.succeed();
    }

    /** A trap token under a husk arms, then bursts. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_mines", timeoutTicks = 60)
    public static void trapTokenBurstsUnderTheEnemy(GameTestHelper helper) {
        Witch caster = caster(helper, 1.5, 1.5);
        Husk husk = husk(helper, 6.5, 6.5);
        TokenMine.place(helper.getLevel(), caster, ModSpells.TOKEN_MINES.get(), husk.position(), 4);
        helper.runAfterDelay(TokenMine.ARM_TICKS - 4, () -> helper.assertTrue(!hurt(husk), "the token burst before it was armed"));
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the trap token never burst under the husk"));
    }

    /** The tribute hurts the enemies around, and their tokens heal the caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_tribute", timeoutTicks = 60)
    public static void tributeHurtsAndHeals(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 2.5);
        caster.setHealth(10);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.SOUL_TRIBUTE, caster);
        helper.assertTrue(hurt(husk), "the husk gave no tribute");
        helper.succeedWhen(() -> helper.assertTrue(caster.getHealth() > 10, "the tribute token did not heal the caster"));
    }

    /** Heads or tails, the toss does something: the husk is struck, or the caster healed or strengthened. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_flip", timeoutTicks = 60)
    public static void coinFlipDoesSomething(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        caster.setHealth(10);
        Husk husk = husk(helper, 4.5, 5.5);
        cast(helper, ModSpells.COIN_FLIP, caster);
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk) || caster.getHealth() > 10 || caster.hasEffect(MobEffects.DAMAGE_BOOST),
                "the toss did nothing"));
    }

    /** The hand gathers, seizes the husk and crushes it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "token_hand", timeoutTicks = 200)
    public static void handOfSoulsSeizesAndCrushes(GameTestHelper helper) {
        Witch caster = caster(helper, 4.5, 1.5);
        Husk husk = husk(helper, 4.5, 6.5);
        cast(helper, ModSpells.TOKEN_HAND, caster);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(TokenHand.class, husk.getBoundingBox().inflate(10)).isEmpty(), "no hand gathered");
        helper.runAfterDelay(TokenHand.ASSEMBLE - 2, () -> helper.assertTrue(!hurt(husk), "the hand struck before it had formed"));
        helper.succeedWhen(() -> helper.assertTrue(hurt(husk), "the hand never hurt the husk"));
    }
}
