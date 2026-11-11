package com.waylays.eraphones.gear;

import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** An instant photograph in its white frame. Used, it is looked at closely; fresh out of the camera, it is still developing. */
public class PhotoItem extends Item {
    public PhotoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && stack.get(GearRegistry.PHOTO.get()) != null) {
            com.waylays.eraphones.gear.client.PhotoClient.open(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Sneaking, on a block's face: pinned there (a wall), or laid there (a table, the floor). */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (player == null || !player.isShiftKeyDown() || stack.get(GearRegistry.PHOTO.get()) == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        Level level = ctx.getLevel();
        net.minecraft.core.Direction face = ctx.getClickedFace();
        net.minecraft.core.BlockPos pos = ctx.getClickedPos();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.relative(face);
        }
        net.minecraft.world.level.block.state.BlockState state = com.waylays.eraphones.event.EventRegistry.PINNED_PHOTO.get().defaultBlockState()
                .setValue(PinnedPhotoBlock.FACING, face);
        if (!level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level, pos)) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            net.minecraft.util.RandomSource r = level.getRandom();
            float angle = face.getAxis().isVertical()
                    ? (face == net.minecraft.core.Direction.UP ? 180 - player.getYRot() : player.getYRot()) + (r.nextFloat() - .5f) * 24
                    : (r.nextFloat() - .5f) * 14;
            level.setBlock(pos, state, 3);
            if (level.getBlockEntity(pos) instanceof PinnedPhotoBlockEntity be) {
                be.set(stack, angle, r.nextInt(5));
            }
            stack.consume(1, player);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, .8f, 1.3f);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String by = stack.get(GearRegistry.PHOTO_BY.get());
        if (by != null) {
            tooltip.add(Component.translatable("item.eraphones.photo.by", by).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("item.eraphones.photo.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
