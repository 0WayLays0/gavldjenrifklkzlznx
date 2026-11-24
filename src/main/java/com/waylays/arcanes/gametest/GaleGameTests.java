package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gale.GaleScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.ElementSpell;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
 * Server-side checks run by CI for the Gale: the school is there with its six spells, its codex and its focus; the
 * blade of air cuts the husk it flies through; the weight of the air bows the husk under it; Cor Leonis cuts the husk
 * beside its bearer. Each test runs in a batch of its own.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class GaleGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2

    private GaleGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return !e.isAlive() || e.getHealth() < e.getMaxHealth();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gale_school")
    public static void theGaleIsRegistered(GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(ModSchools.GALE_RESOURCE);
        helper.assertTrue(school != null, "no Gale school");
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof ElementSpell, entry.getId() + " is in the Gale but not one of its spells");
                count++;
            }
        }
        helper.assertTrue(count == 6, "expected 6 Gale spells, found " + count);
        helper.assertTrue(new ItemStack(ModItems.VIOLET_GALE_CODEX.get()).getItem() instanceof SpellBook, "the Codex of the Violet Gale is not a spellbook");
        helper.assertTrue(new ItemStack(Items.FEATHER).is(ModSchools.GALE_FOCUS), "the gale focus tag");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gale_slash", timeoutTicks = 60)
    public static void airSlashCutsWhatItFliesThrough(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(6.5, 2, 4.5));
        GaleScene.spawn(helper.getLevel(), null, null, GaleScene.Kind.SLASH, helper.absoluteVec(new Vec3(1.5, 3, 4.5)), new Vec3(1, 0, 0), 0,
                GaleScene.SLASH_TIME + 10, 4);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(hurt(husk), "the blade of air did not cut the husk in its way");
            husk.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gale_gravity", timeoutTicks = 60)
    public static void crushingGravityBowsWhoeverIsUnderIt(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 4.5));
        GaleScene.spawn(helper.getLevel(), null, null, GaleScene.Kind.GRAVITY, helper.absoluteVec(new Vec3(4.5, 2, 4.5)), new Vec3(0, 0, 1), 0, 40, 4);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(husk.hasEffect(ModEffects.CRUSHING_GRAVITY), "the husk under the weight of the air is not bowed by it");
            husk.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "gale_storm", timeoutTicks = 80)
    public static void corLeonisCutsBesideItsBearer(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch bearer = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(6.5, 2, 4.5));
        GaleScene.spawn(helper.getLevel(), bearer, null, GaleScene.Kind.STORM, bearer.position(), new Vec3(0, 0, 1), 0, 60, 3);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(hurt(husk), "the storm of blades did not cut the husk beside its bearer");
            helper.assertTrue(!hurt(bearer), "the storm of blades cut its own bearer");
            husk.discard();
            bearer.discard();
            helper.succeed();
        });
    }
}
