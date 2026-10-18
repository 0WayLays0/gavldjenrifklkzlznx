package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaCamel;
import com.waylays.arcanes.entity.errethys.ManaCrow;
import com.waylays.arcanes.entity.errethys.ManaRat;
import com.waylays.arcanes.entity.errethys.ManaSalmon;
import com.waylays.arcanes.entity.errethys.ManaSerpent;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Server-side checks run by CI for Errethys, the school of summoned mana creatures: each spell brings its creatures,
 * on the caster's side, the summoner can ride the dromedary, and each kind of creature fights (the serpent's bite
 * poisons). Fights run in batches of their
 * own, and every test sends its creatures away at the end, so they never go after another test's mobs.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class ErrethysGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2

    private ErrethysGameTests() {}

    private static <T extends LivingEntity> List<T> around(GameTestHelper helper, Class<T> type, LivingEntity center) {
        return helper.getLevel().getEntitiesOfClass(type, center.getBoundingBox().inflate(8), e -> !e.isRemoved());
    }

    private static void dismissAll(GameTestHelper helper, LivingEntity center) {
        around(helper, ManaSummon.class, center).forEach(ManaSummon::dissolve);
        around(helper, ManaCamel.class, center).forEach(ManaCamel::dissolve);
    }

    @GameTest(template = "platform")
    public static void errethysSchoolIsRegistered(GameTestHelper helper) {
        SchoolType errethys = SchoolRegistry.getSchool(ModSchools.ERRETHYS_RESOURCE);
        helper.assertTrue(errethys != null, "Errethys missing from Iron's school registry");
        int count = 0;
        for (RegistryObject<AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            if (SpellRegistry.getSpell(entry.getId()).getSchoolType() == errethys) {
                count++;
            }
        }
        helper.assertTrue(count == 5, "expected 5 Errethys spells, got " + count);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModSchools.ERRETHYS_MAGIC),
                "errethys_magic damage type missing");
        helper.assertTrue(errethys.isFocus(new ItemStack(Items.PRISMARINE_CRYSTALS)), "prismarine crystals are not the Errethys focus");
        helper.succeed();
    }

    @GameTest(template = "platform", batch = "errethys_summon")
    public static void eachSpellSummonsItsCreatures(GameTestHelper helper) {
        Witch caster = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        MagicData magicData = new MagicData(true);
        ModSpells.MANA_RATS.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_SALMON.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_SERPENT.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        ModSpells.MANA_CROWS.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, magicData);
        helper.assertTrue(around(helper, ManaRat.class, caster).size() == 3, "expected 3 rats, got " + around(helper, ManaRat.class, caster).size());
        helper.assertTrue(around(helper, ManaSalmon.class, caster).size() == 2, "expected 2 salmon, got " + around(helper, ManaSalmon.class, caster).size());
        helper.assertTrue(around(helper, ManaSerpent.class, caster).size() == 1, "expected 1 serpent, got " + around(helper, ManaSerpent.class, caster).size());
        helper.assertTrue(around(helper, ManaCrow.class, caster).size() == 2, "expected 2 crows, got " + around(helper, ManaCrow.class, caster).size());
        for (ManaSummon summon : around(helper, ManaSummon.class, caster)) {
            helper.assertTrue(caster.getUUID().equals(summon.getOwnerUUID()), "a creature has no master: " + summon);
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
        Player stranger = helper.makeMockPlayer();
        helper.assertTrue(camel.mobInteract(stranger, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "a stranger climbed on the dromedary");
        dismissAll(helper, caster);
        helper.succeed();
    }

    @GameTest(template = "platform", batch = "errethys_ride")
    public static void summonerRidesTheDromedary(GameTestHelper helper) {
        Player rider = helper.makeMockPlayer();
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

    @GameTest(template = "platform", batch = "errethys_life", timeoutTicks = 40)
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
     * A villager casts {@code spell} near a vindicator (a monster that poison works on) and the test waits for the
     * creatures to hurt it, and for {@code extra}.
     */
    private static void fight(GameTestHelper helper, RegistryObject<AbstractSpell> spell, Runnable extra, Vindicator[] foe) {
        Villager caster = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(4.5, 2, 1.5));
        foe[0] = helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(4.5, 2, 6.5));
        Vindicator target = foe[0];
        spell.get().onCast(helper.getLevel(), 1, caster, CastSource.COMMAND, new MagicData(true));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the creatures left the vindicator alone (health " + target.getHealth() + ")");
            helper.assertTrue(caster.getHealth() == caster.getMaxHealth(), "a creature turned on its summoner");
            extra.run();
            dismissAll(helper, caster);
        });
    }

    @GameTest(template = "platform", batch = "errethys_rats", timeoutTicks = 300)
    public static void manaRatsBite(GameTestHelper helper) {
        fight(helper, ModSpells.MANA_RATS, () -> {}, new Vindicator[1]);
    }

    @GameTest(template = "platform", batch = "errethys_salmon", timeoutTicks = 300)
    public static void manaSalmonRam(GameTestHelper helper) {
        fight(helper, ModSpells.MANA_SALMON, () -> {}, new Vindicator[1]);
    }

    @GameTest(template = "platform", batch = "errethys_serpent", timeoutTicks = 300)
    public static void manaSerpentPoisons(GameTestHelper helper) {
        Vindicator[] foe = new Vindicator[1];
        fight(helper, ModSpells.MANA_SERPENT, () -> helper.assertTrue(foe[0].hasEffect(MobEffects.POISON), "the bite did not poison"), foe);
    }

    @GameTest(template = "platform", batch = "errethys_crows", timeoutTicks = 300)
    public static void manaCrowsPeck(GameTestHelper helper) {
        fight(helper, ModSpells.MANA_CROWS, () -> {}, new Vindicator[1]);
    }
}
