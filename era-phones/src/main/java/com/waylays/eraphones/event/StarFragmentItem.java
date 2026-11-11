package com.waylays.eraphones.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** The star fragment, in a game master's hand: used on the ground, it is set there, floating and glowing. */
public class StarFragmentItem extends Item {
    public StarFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            BlockPos at = context.getClickedPos().relative(context.getClickedFace());
            StarFragment f = new StarFragment(EventRegistry.FRAGMENT.get(), level);
            f.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, context.getPlayer() == null ? 0 : context.getPlayer().getYRot(), 0);
            level.addFreshEntity(f);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
