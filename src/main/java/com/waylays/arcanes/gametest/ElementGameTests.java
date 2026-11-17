package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.acid.AcidScene;
import com.waylays.arcanes.entity.lava.LavaScene;
import com.waylays.arcanes.entity.plant.PlantScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.entity.snow.SnowScene;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side checks run by CI for the elemental schools. Snow: the school is there with its five spells and its
 * codex; the snowball runs into and chills an enemy in its way, then bursts; the snow globe holds an enemy in and
 * shatters on it; the crown looses its icicles; the ribbon of ice slows whoever crosses it; the blizzard freezes and
 * hurts those inside. Lava: the spit lands and its pool burns; the geyser flings and burns; the carapace burns whoever
 * strikes its wearer; the river slows and burns; the caldera caves in on those inside it. Plants: the bloom's thorns make
 * bleed; the pollen weakens enemies and heals the caster; the seeds' spores poison; the sunlight heals; the maw bites
 * and feeds its caster. Acid: the spray burns, sunders and eats the armour worn; the pool burns those standing in it; the
 * rain burns and weakens; the bubble eats armour and melts its victim; the tide rolls over, weakens and corrodes.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class ElementGameTests {
    private ElementGameTests() {}

    private static Witch caster(GameTestHelper helper, double x, double z) {
        Witch w = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(x, 2, z));
        w.setYRot(0);
        return w;
    }

    private static Vindicator foe(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new Vec3(x, 2, z));
    }

    private static void done(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        helper.succeed();
    }

    private static int count(ResourceLocation id, GameTestHelper helper) {
        SchoolType school = SchoolRegistry.getSchool(id);
        helper.assertTrue(school != null, "no school " + id);
        int count = 0;
        for (DeferredHolder<AbstractSpell, ? extends AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getId());
            if (spell.getSchoolType() == school) {
                helper.assertTrue(spell instanceof ElementSpell, entry.getId() + " is in the school but not one of its spells");
                count++;
            }
        }
        return count;
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void elementSchoolsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(count(ModSchools.SNOW_RESOURCE, helper) == 5, "expected 5 Snow spells");
        helper.assertTrue(new ItemStack(ModItems.FIRST_FROST_CODEX.get()).getItem() instanceof SpellBook, "the Codex of the First Frost is not a spellbook");
        helper.assertTrue(new ItemStack(Items.SNOWBALL).is(ModSchools.SNOW_FOCUS), "the snow focus tag");
        helper.assertTrue(count(ModSchools.LAVA_RESOURCE, helper) == 5, "expected 5 Lava spells");
        helper.assertTrue(new ItemStack(ModItems.MAGMA_CODEX.get()).getItem() instanceof SpellBook, "the Magma Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.MAGMA_CREAM).is(ModSchools.LAVA_FOCUS), "the lava focus tag");
        helper.assertTrue(count(ModSchools.PLANT_RESOURCE, helper) == 5, "expected 5 Plant spells");
        helper.assertTrue(new ItemStack(ModItems.HERBARIUM_CODEX.get()).getItem() instanceof SpellBook, "the Herbarium Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.BONE_MEAL).is(ModSchools.PLANT_FOCUS), "the plant focus tag");
        helper.assertTrue(count(ModSchools.ACID_RESOURCE, helper) == 5, "expected 5 Acid spells");
        helper.assertTrue(new ItemStack(ModItems.CORROSIVE_CODEX.get()).getItem() instanceof SpellBook, "the Corrosive Codex is not a spellbook");
        helper.assertTrue(new ItemStack(Items.SLIME_BALL).is(ModSchools.ACID_FOCUS), "the acid focus tag");
        helper.succeed();
    }

    private static Vec3 at(GameTestHelper helper, double x, double z) {
        return helper.absoluteVec(new Vec3(x, 2, z));
    }

    /** The snowball rolls into the enemy in its way, hurts, slows and chills it, then bursts at the end of its run. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_roll", timeoutTicks = 80)
    public static void avalancheBallRunsThemOver(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator foe = foe(helper, 4.5, 5.5);
        float full = foe.getHealth();
        SnowScene ball = SnowScene.spawn(helper.getLevel(), caster, ModSpells.AVALANCHE_BALL.get(), SnowScene.Kind.ROLL, at(helper, 4.5, 1.5), 0,
                SnowScene.ROLL_TIME + 25, 5);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(foe.getHealth() < full, "the snowball did not hurt the enemy in its way");
            helper.assertTrue(foe.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && foe.getTicksFrozen() > 0, "the snowball did not slow and chill it");
        });
        helper.runAfterDelay(SnowScene.ROLL_TIME + 4, () -> {
            helper.assertTrue(ball.hasBurst(), "the snowball did not burst");
            done(helper);
        });
    }

    /** The globe keeps its prisoner in, then shatters on it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_globe", timeoutTicks = 140)
    public static void snowGlobeHoldsThenShatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator foe = foe(helper, 4.5, 5.5);
        float full = foe.getHealth();
        Vec3 centre = at(helper, 4.5, 5.5);
        SnowScene globe = SnowScene.spawn(helper.getLevel(), caster, ModSpells.SNOW_GLOBE.get(), SnowScene.Kind.GLOBE, centre, 0, SnowScene.GLOBE_TIME, 6);
        helper.runAfterDelay(20, () -> foe.setDeltaMovement(1.5, 0, 0));
        helper.runAfterDelay(40, () -> helper.assertTrue(foe.position().multiply(1, 0, 1).distanceTo(centre.multiply(1, 0, 1)) < SnowScene.GLOBE_R + .5,
                "the prisoner left the globe"));
        helper.runAfterDelay(SnowScene.GLOBE_TIME - 8, () -> {
            helper.assertTrue(globe.hasBurst() && foe.getHealth() < full, "the globe did not shatter on its prisoner");
            done(helper);
        });
    }

    /** The crown looses an icicle at the nearest enemy; the ribbon of ice slows whoever crosses it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_crown", timeoutTicks = 100)
    public static void crownShootsAndRibbonSlows(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch crowned = caster(helper, 1.5, 1.5);
        Vindicator shot = foe(helper, 7.5, 1.5);
        float full = shot.getHealth();
        SnowScene crown = SnowScene.spawn(helper.getLevel(), crowned, ModSpells.HOARFROST_CROWN.get(), SnowScene.Kind.CROWN, at(helper, 1.5, 1.5), 0,
                SnowScene.CROWN_TIME, 4);
        Vindicator crossing = foe(helper, 5.5, 6.5);
        // A ribbon of ice laid by no one, crossing the second enemy's spot
        SnowScene.spawn(helper.getLevel(), null, ModSpells.FROST_GLIDE.get(), SnowScene.Kind.TRAIL, at(helper, 5.5, 3), 0, SnowScene.TRAIL_TIME, 4);
        helper.runAfterDelay(SnowScene.CROWN_EVERY + 5, () -> {
            helper.assertTrue(shot.getHealth() < full && crown.strikes() > 0, "the crown loosed no icicle");
            helper.assertTrue(crossing.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the ribbon of ice did not slow whoever crossed it");
            done(helper);
        });
    }

    /** The blizzard freezes and hurts those inside, and shatters the frozen at its end. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "snow_whiteout", timeoutTicks = 220)
    public static void whiteoutFreezesAndShatters(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, 1.5);
        Vindicator foe = foe(helper, 5.5, 5.5);
        float full = foe.getHealth();
        SnowScene storm = SnowScene.spawn(helper.getLevel(), caster, ModSpells.WHITEOUT.get(), SnowScene.Kind.WHITEOUT, at(helper, 1.5, 1.5), 0,
                SnowScene.WHITEOUT_TIME, 8);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(foe.getTicksFrozen() > 0 && foe.hasEffect(MobEffects.BLINDNESS), "the blizzard did not chill and blind the enemy");
            helper.assertTrue(foe.getHealth() < full, "the blizzard did not hurt the enemy");
        });
        helper.runAfterDelay(SnowScene.SHATTER_AT + 3, () -> {
            helper.assertTrue(storm.hasBurst(), "the blizzard did not shatter the frozen");
            done(helper);
        });
    }

    /** The spit lands where it was thrown and its pool burns; the geyser flings and burns those over it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lava_spit", timeoutTicks = 100)
    public static void magmaSpitAndGeyserBurn(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, 1.5);
        Vindicator pooled = foe(helper, 6.5, 6.5);
        Vindicator flung = foe(helper, 1.5, 6.5);
        float full = pooled.getHealth();
        LavaScene spit = LavaScene.spawn(helper.getLevel(), caster, ModSpells.MAGMA_SPIT.get(), LavaScene.Kind.SPIT, at(helper, 1.5, 1.5).add(0, 1.5, 0),
                at(helper, 6.5, 6.5), 0, LavaScene.SPIT_FLIGHT + 5, 5);
        LavaScene.spawn(helper.getLevel(), caster, ModSpells.LAVA_GEYSER.get(), LavaScene.Kind.GEYSER, at(helper, 1.5, 6.5), null, 0,
                LavaScene.GEYSER_WARN + LavaScene.GEYSER_BURST + 20, 6);
        float flungFull = flung.getHealth();
        helper.runAfterDelay(LavaScene.GEYSER_WARN + 3, () -> {
            helper.assertTrue(spit.landed() && pooled.getHealth() < full && pooled.isOnFire(), "the spit did not land and burn");
            helper.assertTrue(flung.getHealth() < flungFull && flung.isOnFire(), "the geyser did not burn the enemy over it");
            done(helper);
        });
    }

    /** Whoever strikes the carapace's wearer catches fire; the river slows and burns; the caldera caves in on those inside. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "lava_carapace", timeoutTicks = 100)
    public static void carapaceRiverAndCaldera(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch wearer = caster(helper, 1.5, 1.5);
        Vindicator striker = foe(helper, 2.5, 1.5);
        LavaScene.spawn(helper.getLevel(), wearer, ModSpells.OBSIDIAN_CARAPACE.get(), LavaScene.Kind.CARAPACE, at(helper, 1.5, 1.5), null, 0,
                LavaScene.CARAPACE_TIME, 4);
        Vindicator wading = foe(helper, 6.5, 5.5);
        LavaScene.spawn(helper.getLevel(), null, ModSpells.LAVA_RIVER.get(), LavaScene.Kind.RIVER, at(helper, 6.5, 2), null, 0, LavaScene.RIVER_TIME, 5);
        Vindicator sunk = foe(helper, 4.5, 7.5);
        float sunkFull = sunk.getHealth();
        Witch other = caster(helper, 4.5, 4.5);
        LavaScene caldera = LavaScene.spawn(helper.getLevel(), other, ModSpells.CALDERA.get(), LavaScene.Kind.CALDERA, at(helper, 4.5, 4.5), null, 0,
                LavaScene.CALDERA_TIME, 8);
        helper.runAfterDelay(3, () -> wearer.hurt(helper.getLevel().damageSources().mobAttack(striker), 2));
        helper.runAfterDelay(LavaScene.RIVER_FLOW + 5, () -> {
            helper.assertTrue(striker.isOnFire(), "the carapace did not burn its wearer's striker");
            helper.assertTrue(wading.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && wading.isOnFire(), "the river did not slow and burn the enemy in it");
        });
        helper.runAfterDelay(LavaScene.CALDERA_COLLAPSE + 3, () -> {
            helper.assertTrue(caldera.collapsed() && sunk.getHealth() < sunkFull && sunk.isOnFire(), "the caldera did not cave in on the enemy inside");
            done(helper);
        });
    }

    /** The bloom's thorns make bleed; the pollen weakens enemies and heals the caster; the seeds' spores poison. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "plant_bloom", timeoutTicks = 100)
    public static void bloomPollenAndSeeds(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        caster.setHealth(caster.getMaxHealth() * .4f);
        float hurt = caster.getHealth();
        Vindicator pierced = foe(helper, 1.5, 7.5);
        Vindicator sneezing = foe(helper, 5.5, 1.5);
        PlantScene.spawn(helper.getLevel(), caster, ModSpells.THORN_BLOOM.get(), PlantScene.Kind.BLOOM, at(helper, 2.5, 6.5), 0, PlantScene.BLOOM_TIME, 5);
        PlantScene pollen = PlantScene.spawn(helper.getLevel(), caster, ModSpells.POLLEN_CLOUD.get(), PlantScene.Kind.POLLEN, at(helper, 4.5, 1), 0,
                PlantScene.POLLEN_TIME, 4);
        Vindicator poisoned = foe(helper, 7.5, 7.5);
        // A volley thrown from the corner toward the far one (yaw -45: south-east)
        PlantScene seeds = PlantScene.spawn(helper.getLevel(), caster, ModSpells.SEED_VOLLEY.get(), PlantScene.Kind.SEEDS, at(helper, 3, 3), -45, PlantScene.SEEDS_TIME, 5);
        helper.runAfterDelay(PlantScene.SEED_BURST + 3, () -> {
            helper.assertTrue(pierced.hasEffect(ModEffects.BLEEDING), "the bloom's thorns did not make the enemy bleed");
            helper.assertTrue(sneezing.hasEffect(MobEffects.WEAKNESS) && caster.getHealth() > hurt && pollen.healed() > 0,
                    "the pollen did not weaken the enemy and heal its caster");
            helper.assertTrue(seeds.strikes() > 0 && poisoned.hasEffect(MobEffects.POISON), "the seeds' spores did not poison");
            done(helper);
        });
    }

    /** The sunlight heals its caster; the maw bites those it closes on and feeds its caster. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "plant_maw", timeoutTicks = 100)
    public static void sunlightAndMaw(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch sunned = caster(helper, 1.5, 1.5);
        sunned.setHealth(sunned.getMaxHealth() * .4f);
        PlantScene sun = PlantScene.spawn(helper.getLevel(), sunned, ModSpells.PHOTOSYNTHESIS.get(), PlantScene.Kind.SUN, at(helper, 1.5, 1.5), 0,
                PlantScene.SUN_TIME, 4);
        Witch eater = caster(helper, 7.5, 1.5);
        eater.setHealth(eater.getMaxHealth() * .4f);
        float hungry = eater.getHealth();
        Vindicator eaten = foe(helper, 5.5, 6.5);
        float full = eaten.getHealth();
        PlantScene maw = PlantScene.spawn(helper.getLevel(), eater, ModSpells.VERDANT_MAW.get(), PlantScene.Kind.MAW, at(helper, 5.5, 6.5), 0, PlantScene.MAW_TIME, 3);
        helper.runAfterDelay(PlantScene.MAW_SNAP + 3, () -> {
            helper.assertTrue(sun.healed() > 0, "the sunlight did not heal its caster");
            helper.assertTrue(eaten.getHealth() < full && maw.strikes() > 0, "the maw did not bite");
            helper.assertTrue(eater.getHealth() > hungry, "the maw did not feed its caster");
            done(helper);
        });
    }

    private static Vindicator armoured(GameTestHelper helper, double x, double z) {
        Vindicator v = foe(helper, x, z);
        v.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        v.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        return v;
    }

    private static boolean worn(Vindicator v) {
        return v.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() > 0 || v.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() > 0
                || v.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    /** The spray burns, sunders and eats the armour of the enemy ahead; the pool burns and sunders the one standing in it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "acid_spray", timeoutTicks = 80)
    public static void acidSprayAndPool(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, .5);
        Vindicator sprayed = armoured(helper, 4.5, 4.5);
        float full = sprayed.getHealth();
        AcidScene spray = AcidScene.spawn(helper.getLevel(), caster, ModSpells.ACID_SPRAY.get(), AcidScene.Kind.SPRAY, caster.getEyePosition(), 0, 0, null,
                AcidScene.SPRAY_TIME + 14, 5);
        Vindicator wading = foe(helper, 1.5, 7.5);
        AcidScene pool = AcidScene.spawn(helper.getLevel(), caster, ModSpells.CORROSIVE_POOL.get(), AcidScene.Kind.POOL, at(helper, 1.5, 7.5), 0, 0, null,
                AcidScene.POOL_TIME, 5);
        helper.runAfterDelay(AcidScene.SPRAY_TIME + 3, () -> {
            helper.assertTrue(spray.strikes() > 0 && sprayed.getHealth() < full, "the spray did not burn the enemy ahead");
            helper.assertTrue(sprayed.hasEffect(ModEffects.SUNDERED) && worn(sprayed) && spray.eaten() > 0, "the spray did not eat the enemy's armour");
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(pool.strikes() > 0 && wading.hasEffect(ModEffects.SUNDERED), "the pool did not burn the enemy standing in it");
            done(helper);
        });
    }

    /** The rain burns and weakens those below; the bubble eats its victim's armour, then melts it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "acid_rain", timeoutTicks = 100)
    public static void acidRainAndDissolve(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 4.5, 7.5);
        Vindicator soaked = foe(helper, 1.5, 1.5);
        float soakedFull = soaked.getHealth();
        AcidScene rain = AcidScene.spawn(helper.getLevel(), caster, ModSpells.ACID_RAIN.get(), AcidScene.Kind.RAIN, at(helper, 1.5, 1.5), 0, 0, null,
                AcidScene.RAIN_TIME, 4);
        Vindicator melted = armoured(helper, 7.5, 1.5);
        float meltedFull = melted.getHealth();
        AcidScene bubble = AcidScene.spawn(helper.getLevel(), caster, ModSpells.DISSOLVE.get(), AcidScene.Kind.DISSOLVE, melted.position(), 0, 0, melted,
                AcidScene.DISSOLVE_TIME, 3);
        helper.runAfterDelay(AcidScene.DISSOLVE_TIME - 8, () -> {
            helper.assertTrue(rain.strikes() > 0 && soaked.getHealth() < soakedFull && soaked.hasEffect(MobEffects.WEAKNESS),
                    "the rain did not burn and weaken the enemy below");
            helper.assertTrue(bubble.strikes() >= 3 && melted.getHealth() < meltedFull && worn(melted) && bubble.eaten() > 0,
                    "the bubble did not eat the armour of its victim and melt it");
            done(helper);
        });
    }

    /** The tide rolls out over the enemy, hurts, weakens and corrodes it. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "acid_tide", timeoutTicks = 80)
    public static void corrodingTideRollsOver(GameTestHelper helper) {
        GameTestSetup.clearLeftovers(helper);
        Witch caster = caster(helper, 1.5, 1.5);
        Vindicator swept = armoured(helper, 6.5, 6.5);
        float full = swept.getHealth();
        AcidScene tide = AcidScene.spawn(helper.getLevel(), caster, ModSpells.CORRODING_TIDE.get(), AcidScene.Kind.TIDE, at(helper, 1.5, 1.5), 0, 0, null,
                AcidScene.TIDE_TIME, 5);
        helper.runAfterDelay(AcidScene.TIDE_ROLL + 2, () -> {
            helper.assertTrue(tide.strikes() > 0 && swept.getHealth() < full, "the tide did not hurt the enemy it rolled over");
            helper.assertTrue(swept.hasEffect(MobEffects.WEAKNESS) && swept.hasEffect(ModEffects.SUNDERED) && worn(swept), "the tide did not corrode the enemy");
            done(helper);
        });
    }
}
