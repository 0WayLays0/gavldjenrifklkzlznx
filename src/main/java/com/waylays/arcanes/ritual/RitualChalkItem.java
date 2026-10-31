package com.waylays.arcanes.ritual;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ritual Chalk: sneak and use it in the air to choose the ritual; use it on the ground to draw that ritual's figure
 * (the one who draws it leads the ritual), or on a wall for the rituals drawn on walls (the Portal). For the Portal,
 * sneak and use it on the ground first to mark where the portal will lead. The rituals begun at an Altar of Rites are
 * not drawn in chalk.
 */
public class RitualChalkItem extends Item {
    private static final String RITUAL = "arcanes_oublies_ritual";
    private static final String ANCHOR = "arcanes_oublies_anchor";
    private static final String ANCHOR_LEVEL = "arcanes_oublies_anchor_level";

    public RitualChalkItem(Properties properties) {
        super(properties);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static Ritual ritual(ItemStack stack) {
        return Ritual.byIndex(tag(stack).getInt(RITUAL));
    }

    public static void setRitual(ItemStack stack, Ritual ritual) {
        CompoundTag tag = tag(stack);
        tag.putInt(RITUAL, ritual.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Where the chalk's portal anchor is, if it has one. */
    @Nullable
    public static Vec3 anchor(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(ANCHOR)) {
            return null;
        }
        long packed = tag.getLong(ANCHOR);
        BlockPos pos = BlockPos.of(packed);
        return Vec3.atBottomCenterOf(pos);
    }

    @Nullable
    public static ResourceLocation anchorLevel(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.contains(ANCHOR_LEVEL) ? ResourceLocation.tryParse(tag.getString(ANCHOR_LEVEL)) : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        Ritual next = ritual(stack).nextChalked();
        setRitual(stack, next);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_chosen", next.title(), next.participants)
                    .withStyle(s -> s.withColor(next.color)), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos above = context.getClickedPos().above();
        Ritual ritual = ritual(stack);
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (ritual != Ritual.PORTAL) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                CompoundTag tag = tag(stack);
                tag.putLong(ANCHOR, above.asLong());
                tag.putString(ANCHOR_LEVEL, level.dimension().location().toString());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_anchor", above.getX(), above.getY(), above.getZ())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                level.playSound(null, above, SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1, .8f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Vec3 at = Vec3.atBottomCenterOf(above);
        if (!level.getEntitiesOfClass(RitualCircle.class, new net.minecraft.world.phys.AABB(at, at).inflate(RitualCircle.RADIUS * 2.5)).isEmpty()) {
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_too_close").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (ritual == Ritual.PORTAL && anchor(stack) == null) {
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_no_anchor").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (ritual.surface == Ritual.Surface.ALTAR) {
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_at_altar", ritual.title()).withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.FAIL;
        }
        Direction face = context.getClickedFace();
        if (ritual.surface == Ritual.Surface.WALL) {
            if (!face.getAxis().isHorizontal()) {
                player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_on_wall", ritual.title()).withStyle(ChatFormatting.GOLD), true);
                return InteractionResult.FAIL;
            }
            // On the wall's face, at the height of the floor before it
            BlockPos front = context.getClickedPos().relative(face);
            int floor = Mth.floor(player.getY());
            Vec3 foot = Vec3.atBottomCenterOf(new BlockPos(front.getX(), floor, front.getZ())).subtract(Vec3.atLowerCornerOf(face.getNormal()).scale(.48));
            RitualCircle.draw((ServerLevel) level, player, ritual, foot, face.toYRot(), face, anchor(stack), anchorLevel(stack));
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            return InteractionResult.CONSUME;
        }
        RitualCircle.draw((ServerLevel) level, player, ritual, at, player.getYRot(), anchor(stack), anchorLevel(stack));
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Ritual ritual = ritual(stack);
        tooltip.add(Component.translatable("ui.arcanes_oublies.ritual_selected", ritual.title()).withStyle(s -> s.withColor(ritual.color)));
        tooltip.add(Component.translatable("ui.arcanes_oublies.ritual_needs", ritual.participants, ritual.channelTicks / 20).withStyle(ChatFormatting.GRAY));
        tooltip.add(ritual.description().copy().withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("ui.arcanes_oublies.ritual_howto").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
