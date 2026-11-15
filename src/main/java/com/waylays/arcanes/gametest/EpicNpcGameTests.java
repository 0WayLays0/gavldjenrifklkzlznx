package com.waylays.arcanes.gametest;

import com.waylays.arcanes.compat.epicnpc.EpicNpc;
import com.waylays.arcanes.compat.epicnpc.EpicNpcBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Custom NPCs with Epic Fight (run by the CI job that has both mods, under their own namespace): an NPC gets an Epic
 * Fight body from the built-in data pack; its blow is held back while its attack animation swings, then lands.
 */
@GameTestHolder("arcanes_epicnpc")
@PrefixGameTestTemplate(false)
public final class EpicNpcGameTests {
    private EpicNpcGameTests() {}

    private static Mob npc(GameTestHelper helper, BlockPos at) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpc"));
        Mob npc = (Mob) type.create(helper.getLevel());
        BlockPos p = helper.absolutePos(at);
        npc.moveTo(p.getX() + .5, p.getY(), p.getZ() + .5, 0, 0);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    @GameTest(template = "platform", skyAccess = true, timeoutTicks = 60)
    public static void anNpcHasAnEpicFightBody(GameTestHelper helper) {
        helper.assertTrue(EpicNpc.active(), "Epic Fight and Custom NPCs are not both loaded");
        Mob npc = npc(helper, new BlockPos(2, 2, 2));
        helper.succeedWhen(() -> helper.assertTrue(EpicNpcBridge.patch(npc) != null, "no Epic Fight patch on the NPC"));
    }

    @GameTest(template = "platform", skyAccess = true, timeoutTicks = 100)
    public static void anNpcBlowLandsWithItsSwing(GameTestHelper helper) {
        Mob npc = npc(helper, new BlockPos(1, 2, 2));
        npc.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        target.setHealth(target.getMaxHealth());
        float full = target.getHealth();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(EpicNpcBridge.patch(npc) != null, "no Epic Fight patch on the NPC");
            npc.doHurtTarget(target);
            helper.assertTrue(target.getHealth() == full, "the blow landed before the swing");
        });
        helper.runAfterDelay(40, () -> helper.succeedWhen(() -> helper.assertTrue(target.getHealth() < full, "the blow never landed")));
    }
}
