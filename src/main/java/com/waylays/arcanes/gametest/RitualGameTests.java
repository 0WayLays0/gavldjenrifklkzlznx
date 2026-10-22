package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.ritual.Meteor;
import com.waylays.arcanes.entity.ritual.RitualDome;
import com.waylays.arcanes.entity.ritual.RitualPortal;
import com.waylays.arcanes.entity.ritual.StormCloud;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.ritual.Ritual;
import com.waylays.arcanes.ritual.RitualChalkItem;
import com.waylays.arcanes.ritual.RitualCircle;
import com.waylays.arcanes.ritual.RitualEffects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Server-side checks run by CI for the rituals: the chalk keeps its ritual; a circle held by its ritualists to the end
 * casts its ritual; a circle left with an empty station breaks and lashes back; the meteor, the dome, the portal and the
 * colossus do what they should. The witches kneeling on the stations carry the ritualist tag (the staff can give it to
 * any creature), as the test cannot have real players.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class RitualGameTests {
    // Test coordinates: y=1 is the platform's floor block, entities stand at y=2; the platform is 9 x 9

    private RitualGameTests() {}

    private static boolean hurt(LivingEntity e) {
        return !e.isAlive() || e.getHealth() < e.getMaxHealth();
    }

    private static RitualCircle circle(GameTestHelper helper, Ritual ritual) {
        return RitualCircle.draw(helper.getLevel(), null, ritual, helper.absoluteVec(new Vec3(4.5, 2, 4.5)), 0, null, null);
    }

    /** A witch kneeling on each station of the circle. */
    private static List<Witch> ritualists(GameTestHelper helper, RitualCircle circle) {
        List<Witch> out = new ArrayList<>();
        for (int i = 0; i < circle.getRitual().participants; i++) {
            Witch witch = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(4.5, 2, 4.5));
            Vec3 at = circle.position().add(circle.station(i));
            witch.moveTo(at.x, at.y, at.z);
            witch.addTag(RitualCircle.RITUALIST_TAG);
            out.add(witch);
        }
        return out;
    }

    private static void cleanUp(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(Entity.class, helper.getBounds().inflate(32),
                e -> e instanceof RitualCircle || e instanceof StormCloud || e instanceof RitualDome || e instanceof Meteor
                        || e instanceof RitualPortal).forEach(Entity::discard);
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void chalkKeepsItsRitual(GameTestHelper helper) {
        ItemStack chalk = new ItemStack(ModItems.RITUAL_CHALK.get());
        helper.assertTrue(RitualChalkItem.ritual(chalk) == Ritual.METEOR, "a new chalk does not start on the meteor");
        RitualChalkItem.setRitual(chalk, Ritual.STORM.next());
        helper.assertTrue(RitualChalkItem.ritual(chalk) == Ritual.PORTAL, "the chalk did not keep its ritual");
        helper.assertTrue(RitualChalkItem.anchor(chalk) == null, "a new chalk has a portal anchor");
        for (Ritual ritual : Ritual.values()) {
            helper.assertTrue(ritual.title().getContents() instanceof TranslatableContents, ritual + " has no translated name");
            helper.assertTrue(ritual.channelTicks >= 20 * 20, ritual + " is cast in under twenty seconds");
        }
        helper.succeed();
    }

    /** Three ritualists hold the storm's circle to the end; the storm gathers and lightning strikes the husk. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "ritual_storm", timeoutTicks = 900)
    public static void stormRitualIsCastAndStrikes(GameTestHelper helper) {
        RitualCircle circle = circle(helper, Ritual.STORM);
        List<Witch> witches = ritualists(helper, circle);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 4.5));
        helper.runAfterDelay(RitualCircle.DRAW + 5, () -> helper.assertTrue(circle.getState() == RitualCircle.State.CHANNELING,
                "the ritual did not begin with every station held (" + circle.getState() + ")"));
        helper.runAfterDelay(RitualCircle.DRAW + Ritual.STORM.channelTicks / 2, () -> helper.assertTrue(!hurt(husk),
                "something struck before the ritual was cast"));
        helper.succeedWhen(() -> {
            helper.assertTrue(circle.getState() == RitualCircle.State.DONE, "the ritual was not cast");
            helper.assertTrue(hurt(husk), "the storm did not strike the husk");
            for (Witch witch : witches) {
                helper.assertTrue(!hurt(witch), "the storm struck a ritualist");
            }
            cleanUp(helper);
        });
    }

    /** A ritualist taken away mid-ritual: the circle breaks and lashes back at the others. */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "ritual_break", timeoutTicks = 300)
    public static void ritualBreaksWhenAStationEmpties(GameTestHelper helper) {
        RitualCircle circle = circle(helper, Ritual.METEOR);
        List<Witch> witches = ritualists(helper, circle);
        helper.runAfterDelay(RitualCircle.DRAW + 20, () -> {
            helper.assertTrue(circle.getState() == RitualCircle.State.CHANNELING, "the ritual did not begin");
            witches.get(0).discard();
        });
        helper.runAfterDelay(RitualCircle.DRAW + 20 + RitualCircle.GRACE / 2, () -> helper.assertTrue(circle.getState() == RitualCircle.State.CHANNELING,
                "the ritual broke before its leeway ran out"));
        helper.succeedWhen(() -> {
            helper.assertTrue(circle.getState() == RitualCircle.State.FAILED, "the ritual did not break");
            helper.assertTrue(hurt(witches.get(1)) && hurt(witches.get(2)), "the broken ritual did not lash back");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "ritual_meteor", timeoutTicks = 120)
    public static void meteorStrikes(GameTestHelper helper) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, 2, 4.5));
        Meteor.fall(helper.getLevel(), husk.position(), Set.of(), 1);
        helper.runAfterDelay(Meteor.FALL - 5, () -> helper.assertTrue(!hurt(husk), "the meteor struck before it landed"));
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt(husk), "the meteor did not hurt the husk");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "ritual_dome", timeoutTicks = 100)
    public static void domeKeepsHostilesOut(GameTestHelper helper) {
        Vec3 c = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(5.5, 2, 4.5));
        RitualDome.raise(helper.getLevel(), c, 3, 200, Set.of(), 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.position().distanceTo(c) > 3, "the husk is still inside the dome");
            cleanUp(helper);
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "ritual_portal")
    public static void portalOpensBothWays(GameTestHelper helper) {
        Vec3 a = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        Vec3 b = helper.absoluteVec(new Vec3(7.5, 2, 7.5));
        RitualPortal.open(helper.getLevel(), a, helper.getLevel(), b, 100);
        int portals = helper.getLevel().getEntitiesOfClass(RitualPortal.class, helper.getBounds().inflate(2)).size();
        helper.assertTrue(portals == 2, "expected a portal and its twin, found " + portals);
        cleanUp(helper);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "ritual_colossus")
    public static void colossusWakes(GameTestHelper helper) {
        IronGolem golem = RitualEffects.colossus(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 2, 4.5)), 1);
        helper.assertTrue(golem != null && golem.isAlive(), "no colossus woke");
        helper.assertTrue(golem.getAttributeValue(Attributes.SCALE) == RitualEffects.COLOSSUS_SCALE, "the colossus is not giant");
        helper.assertTrue(golem.getMaxHealth() >= 300, "the colossus is not sturdy");
        golem.discard();
        helper.succeed();
    }
}
