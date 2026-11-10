package com.waylays.eraphones.bike;

import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The bicycle in hand: used on the ground, it is stood there, facing the way its owner faces. */
public class BikeItem extends Item {
    public BikeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }
        Level level = ctx.getLevel();
        Vec3 at = ctx.getClickLocation();
        BikeEntity bike = GearRegistry.BIKE.get().create(level);
        if (bike == null) {
            return InteractionResult.FAIL;
        }
        bike.moveTo(at.x, at.y, at.z, ctx.getPlayer() == null ? 0 : ctx.getPlayer().getYRot(), 0);
        if (!level.noCollision(bike, bike.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.addFreshEntity(bike);
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eraphones.velo.help").withStyle(ChatFormatting.GRAY));
    }
}
