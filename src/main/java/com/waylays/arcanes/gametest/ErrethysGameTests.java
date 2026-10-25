package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.entity.book.FlyingGrimoire;
import com.waylays.arcanes.entity.errethys.ManaButterfly;
import com.waylays.arcanes.entity.errethys.ManaCamel;
import com.waylays.arcanes.entity.errethys.ManaCrow;
import com.waylays.arcanes.entity.errethys.ManaFalcon;
import com.waylays.arcanes.entity.errethys.ManaFox;
import com.waylays.arcanes.entity.errethys.ManaKangaroo;
import com.waylays.arcanes.entity.errethys.ManaRat;
import com.waylays.arcanes.entity.errethys.ManaPiranha;
import com.waylays.arcanes.entity.errethys.ManaSerpent;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.entity.errethys.ManaWolf;
import com.waylays.arcanes.entity.gold.GoldenSentinel;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.SummoningSpell;
import com.waylays.arcanes.spells.errethys.ManaPiranhaSpell;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Server-side checks run by CI for Errethys, the school of summoned mana creatures: each spell brings one creature,
 * on the caster's side, sending summons away takes only that spell's, the summoner can ride the dromedary, and each
 * kind of creature stays peaceful until its summoner fights, then fights (the serpent's bite poisons). Fights run in batches of their
 * own, and every test sends its creatures away at the end, so they never go after another test's mobs.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class ErrethysGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2

    /** Ticks the creatures are left with the vindicator before anyone fights: they must leave it alone. */
    private static final int PEACE_TICKS = 60;

    private ErrethysGameTests() {}

    private static <T extends LivingEntity> List<T> around(GameTestHelper helper, Class<T> type, LivingEntity center) {
        return helper.getLevel().getEntitiesOfClass(type, center.getBoundingBox().inflate(8), e -> !e.isRemoved());
    }

    /** A pool of water on the platform, x0..x1 by z0..z1, one block deep, walled in with glass so it does not spread. */
    private static void pool(GameTestHelper helper, int x0, int z0, int x1, int z1) {
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean inside = x >= x0 && x <= x1 && z >= z0 && z <= z1;
                helper.setBlock(new BlockPos(x, 2, z), inside ? Blocks.WATER : Blocks.GLASS);
            }
        }
    }

    private static void dismissAll(GameTestHelper helper, LivingEntity center) {
        for (LivingEntity entity : around(helper, LivingEntity.class, center)) {
            if (entity instanceof OwnedSummon summon) {
                summon.dismissSummon();
            }
        }
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void errethysSchoolIsRegistered(GameTestHelper helper) {
        SchoolType errethys = SchoolRegistry.getSchool(ModSchools.ERRETHYS_RESOURCE);
        helper.assertTrue(errethys != null, "Errethys missing from Iron's school registry");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            if (SpellRegistry.getSpell(entry.getId()).getSchoolType() == errethys) {
                count++;
            }
        }
        helper.assertTrue(count == 10, "expected 10 Errethys spells, got " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.ERRETHYS_MAGIC),
                "errethys_magic damage type missing");
        helper.assertTrue(errethys.isFocus(new ItemStack(Items.PRISMARINE_CRYSTALS)), "prismarine crystals are not the Errethys focus");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_summon")
    public static void eachSpellSummonsOneCreature(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        MagicData magicData = new MagicData(true);
        // The piranha needs water
        pool(helper, 4, 7, 4, 7);
        ModSpells.MANA_RATS.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_PIRANHA.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_SERPENT.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_CROWS.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_FOX.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_WOLF.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_BUTTERFLY.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_KANGAROO.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        // Casting again replaces the creature instead of adding one
        ModSpells.MANA_RATS.get().onCast(helper.getLevel(), 5, caster, CastSource.COMMAND, magicData);
        helper.assertTrue(around(helper, ManaRat.class, caster).size() == 1, "expected 1 rat, got " + around(helper, ManaRat.class, caster).size());
        helper.assertTrue(around(helper, ManaPiranha.class, caster).size() == 1, "expected 1 piranha, got " + around(helper, ManaPiranha.class, caster).size());
        helper.assertTrue(around(helper, ManaSerpent.class, caster).size() == 1, "expected 1 serpent, got " + around(helper, ManaSerpent.class, caster).size());
        helper.assertTrue(around(helper, ManaCrow.class, caster).size() == 1, "expected 1 crow, got " + around(helper, ManaCrow.class, caster).size());
        helper.assertTrue(around(helper, ManaFox.class, caster).size() == 1, "expected 1 fox, got " + around(helper, ManaFox.class, caster).size());
        helper.assertTrue(around(helper, ManaWolf.class, caster).size() == 1, "expected 1 wolf, got " + around(helper, ManaWolf.class, caster).size());
        helper.assertTrue(around(helper, ManaButterfly.class, caster).size() == 1, "expected 1 butterfly, got " + around(helper, ManaButterfly.class, caster).size());
        helper.assertTrue(around(helper, ManaKangaroo.class, caster).size() == 1, "expected 1 kangaroo, got " + around(helper, ManaKangaroo.class, caster).size());
        ManaRat rat = around(helper, ManaRat.class, caster).get(0);
        helper.assertTrue(rat.getMaxHealth() == 1, "the rat should have 1 health, has " + rat.getMaxHealth());
        for (ManaSummon summon : around(helper, ManaSummon.class, caster)) {
            helper.assertTrue(caster.getUUID().equals(summon.getSummonerUUID()), "a creature has no master: " + summon);
            helper.assertTrue(SpellUtil.isAlly(caster, summon), "the caster's spells would hurt " + summon);
        }
        // The dromedary: one at a time, saddled and tame
        ModSpells.MANA_DROMEDARY.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_DROMEDARY.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        List<ManaCamel> camels = around(helper, ManaCamel.class, caster);
        helper.assertTrue(camels.size() == 1, "casting again should replace the dromedary, got " + camels.size());
        ManaCamel camel = camels.get(0);
        helper.assertTrue(camel.isSaddled() && camel.isTamed(), "the dromedary is not ready to ride");
        helper.assertTrue(SpellUtil.isAlly(caster, camel), "the caster's spells would hurt its dromedary");
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(camel.mobInteract(stranger, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "a stranger climbed on the dromedary");
        dismissAll(helper, caster);
        helper.succeed();
    }

    /** What a player's recast does: only that spell's summons, and only the caster's, go away. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_summon")
    public static void sendingSummonsAwayTakesOnlyThatSpells(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(2.5, 2, 4.5));
        Witch other = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(6.5, 2, 4.5));
        MagicData magicData = new MagicData(true);
        ModSpells.MANA_RATS.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_RATS.get().onCast(helper.getLevel(), 1, other, CastSource.COMMAND, magicData);
        ModSpells.MANA_SERPENT.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        ModSpells.GOLDEN_SENTINELS.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        ModSpells.FORBIDDEN_LIBRARY.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        int sentinels = around(helper, GoldenSentinel.class, caster).size();
        int grimoires = around(helper, FlyingGrimoire.class, caster).size();
        helper.assertTrue(sentinels > 0 && grimoires > 0, "the sentinels or the grimoires did not come");

        ((SummoningSpell) ModSpells.MANA_RATS.get()).dismissSummons(helper.getLevel(), caster);
        List<ManaRat> rats = around(helper, ManaRat.class, caster);
        helper.assertTrue(rats.size() == 1 && other.getUUID().equals(rats.get(0).getSummonerUUID()), "the caster's rat should be gone and the other's stay");
        helper.assertTrue(around(helper, ManaSerpent.class, caster).size() == 1, "the serpent went away with the rat");

        ((SummoningSpell) ModSpells.GOLDEN_SENTINELS.get()).dismissSummons(helper.getLevel(), caster);
        helper.assertTrue(around(helper, GoldenSentinel.class, caster).isEmpty(), "sentinels left after being sent away");
        helper.assertTrue(around(helper, FlyingGrimoire.class, caster).size() == grimoires, "the grimoires went away with the sentinels");

        ((SummoningSpell) ModSpells.FORBIDDEN_LIBRARY.get()).dismissSummons(helper.getLevel(), caster);
        helper.assertTrue(around(helper, FlyingGrimoire.class, caster).isEmpty(), "grimoires left after being sent away");
        dismissAll(helper, caster);
        dismissAll(helper, other);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_ride")
    public static void summonerRidesTheDromedary(GameTestHelper helper) {
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        ManaCamel camel = ModEntities.MANA_DROMEDARY.get().create(helper.getLevel());
        helper.assertTrue(camel != null, "no dromedary");
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        camel.moveTo(at.x, at.y, at.z, 0, 0);
        camel.setup(rider, 200, 40);
        helper.getLevel().addFreshEntity(camel);
        camel.mobInteract(rider, InteractionHand.MAIN_HAND);
        helper.assertTrue(rider.getVehicle() == camel, "the summoner could not climb on");
        helper.assertTrue(camel.getControllingPassenger() == rider, "the summoner does not steer the dromedary");
        camel.dissolve();
        helper.assertTrue(!rider.isPassenger(), "the fading dromedary kept its rider");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_ride")
    public static void summonerRidesTheFalcon(GameTestHelper helper) {
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        ManaFalcon falcon = ModEntities.MANA_FALCON.get().create(helper.getLevel());
        helper.assertTrue(falcon != null, "no falcon");
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 3, 4.5));
        falcon.moveTo(at.x, at.y, at.z, 0, 0);
        falcon.setup(rider, 200, 15, .9f);
        helper.getLevel().addFreshEntity(falcon);
        helper.assertTrue(falcon.getMaxHealth() == 15, "the falcon should have 15 health, has " + falcon.getMaxHealth());
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(falcon.mobInteract(stranger, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "a stranger climbed on the falcon");
        falcon.mobInteract(rider, InteractionHand.MAIN_HAND);
        helper.assertTrue(rider.getVehicle() == falcon, "the summoner could not climb on");
        helper.assertTrue(falcon.getControllingPassenger() == rider, "the summoner does not steer the falcon");
        falcon.dissolve();
        helper.assertTrue(!rider.isPassenger(), "the fading falcon kept its rider");
        helper.succeed();
    }

    /** The butterfly heals its summoner by three hearts at its first beat of light, ten seconds in. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_butterfly", timeoutTicks = ManaButterfly.HEAL_EVERY + 60)
    public static void butterflyHealsItsSummoner(GameTestHelper helper) {
        Villager caster = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(4.5, 2, 4.5));
        caster.setHealth(6);
        ModSpells.MANA_BUTTERFLY.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.assertTrue(around(helper, ManaButterfly.class, caster).size() == 1, "no butterfly");
        helper.runAfterDelay(ManaButterfly.HEAL_EVERY - 20, () -> helper.assertTrue(caster.getHealth() <= 6, "the butterfly healed too early"));
        helper.succeedWhen(() -> {
            helper.assertTrue(caster.getHealth() >= 6 + ManaButterfly.HEAL - .01f, "the butterfly did not heal its summoner (health " + caster.getHealth() + ")");
            dismissAll(helper, caster);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_life", timeoutTicks = 40)
    public static void creaturesFadeWhenTheirTimeIsUp(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(1.5, 2, 1.5));
        ManaRat rat = ModEntities.MANA_RAT.get().create(helper.getLevel());
        helper.assertTrue(rat != null, "no rat");
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        rat.moveTo(at.x, at.y, at.z, 0, 0);
        rat.setup(caster, 5, 6, 2);
        helper.getLevel().addFreshEntity(rat);
        helper.succeedWhen(() -> helper.assertTrue(rat.isRemoved(), "the rat outlived its time"));
    }

    /**
     * A villager casts {@code spell} near a vindicator (a monster that poison works on): the creatures leave it alone
     * while nobody fights; then the villager strikes it ({@code defend} false) or is struck by it ({@code defend} true),
     * and the test waits for the creatures to hurt it, and for {@code extra}.
     */
    private static void fight(GameTestHelper helper, DeferredHolder<AbstractSpell, ? extends AbstractSpell> spell, Runnable extra, Vindicator[] foe,
                              boolean defend) {
        Villager caster = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(4.5, 2, 1.5));
        foe[0] = helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(4.5, 2, 6.5));
        Vindicator target = foe[0];
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        float[] afterStrike = {-1};
        helper.runAfterDelay(PEACE_TICKS, () -> {
            helper.assertTrue(target.getHealth() == target.getMaxHealth(), "the creatures attacked a monster nobody was fighting");
            if (defend) {
                caster.hurt(helper.getLevel().damageSources().mobAttack(target), 1);
            } else {
                target.hurt(helper.getLevel().damageSources().mobAttack(caster), 1);
            }
            afterStrike[0] = target.getHealth();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(afterStrike[0] >= 0, "the summoner has not fought yet");
            helper.assertTrue(target.getHealth() < afterStrike[0], "the creatures left the vindicator alone (health " + target.getHealth() + ")");
            for (LivingEntity e : around(helper, LivingEntity.class, caster)) {
                helper.assertTrue(!(e instanceof OwnedSummon) || !(e instanceof net.minecraft.world.entity.Mob mob) || mob.getTarget() != caster,
                        "a creature turned on its summoner");
            }
            extra.run();
            dismissAll(helper, caster);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_rats", timeoutTicks = 300)
    public static void manaRatsBite(GameTestHelper helper) {
        fight(helper, ModSpells.MANA_RATS, () -> {}, new Vindicator[1], false);
    }

    /** The piranha bites, and goes on biting the same prey in a frenzy. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_piranha", timeoutTicks = 300)
    public static void manaPiranhaBitesInAFrenzy(GameTestHelper helper) {
        // The vindicator stands in a pool the piranha can reach it through
        pool(helper, 3, 5, 5, 7);
        Vindicator[] foe = new Vindicator[1];
        fight(helper, ModSpells.MANA_PIRANHA, () -> {
            List<ManaPiranha> piranhas = helper.getLevel().getEntitiesOfClass(ManaPiranha.class, foe[0].getBoundingBox().inflate(8));
            helper.assertTrue(!piranhas.isEmpty() && piranhas.get(0).getFrenzy() >= 1, "the piranha did not bite again in a frenzy");
        }, foe, false);
        // Each bite knocks the prey back: without this it could drift out of the small pool between two bites
        AttributeInstance steady = foe[0].getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (steady != null) {
            steady.setBaseValue(1);
        }
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_dry", timeoutTicks = ManaPiranha.DRY_TICKS + 40)
    public static void thePiranhaNeedsWater(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 1.5));
        helper.assertTrue(ManaPiranhaSpell.findWater(helper.getLevel(), caster) == null, "found water on a dry platform");
        ModSpells.MANA_PIRANHA.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.assertTrue(around(helper, ManaPiranha.class, caster).isEmpty(), "a piranha came with no water around");
        // One stranded on land comes apart
        ManaPiranha piranha = ModEntities.MANA_PIRANHA.get().create(helper.getLevel());
        helper.assertTrue(piranha != null, "no piranha");
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        piranha.moveTo(at.x, at.y, at.z, 0, 0);
        piranha.setup(caster, 1200, 10, 2);
        helper.getLevel().addFreshEntity(piranha);
        helper.succeedWhen(() -> helper.assertTrue(piranha.isRemoved(), "the stranded piranha did not fade"));
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_serpent", timeoutTicks = 300)
    public static void manaSerpentPoisons(GameTestHelper helper) {
        Vindicator[] foe = new Vindicator[1];
        fight(helper, ModSpells.MANA_SERPENT, () -> helper.assertTrue(foe[0].hasEffect(MobEffects.POISON), "the bite did not poison"), foe, true);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_crows", timeoutTicks = 300)
    public static void manaCrowsPeck(GameTestHelper helper) {
        fight(helper, ModSpells.MANA_CROWS, () -> {}, new Vindicator[1], true);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_fox", timeoutTicks = 300)
    public static void manaFoxBleeds(GameTestHelper helper) {
        Vindicator[] foe = new Vindicator[1];
        fight(helper, ModSpells.MANA_FOX, () -> helper.assertTrue(foe[0].hasEffect(ModEffects.BLEEDING), "the fox's bite did not bleed"), foe, false);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_wolf", timeoutTicks = 300)
    public static void manaWolfBites(GameTestHelper helper) {
        fight(helper, ModSpells.MANA_WOLF, () -> {}, new Vindicator[1], true);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "errethys_kangaroo", timeoutTicks = 300)
    public static void manaKangarooBoxes(GameTestHelper helper) {
        fight(helper, ModSpells.MANA_KANGAROO, () -> {}, new Vindicator[1], false);
    }
}
