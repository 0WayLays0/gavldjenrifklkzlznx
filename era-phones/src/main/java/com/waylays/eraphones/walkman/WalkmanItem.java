package com.waylays.eraphones.walkman;

import com.waylays.eraphones.gear.CassetteItem;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A personal cassette player, its headphones on a wire: one cassette in it at a time. Used, it plays (or pauses) the
 * cassette in the listener's headphones alone, and it keeps on playing in a pocket; with a cassette in the other
 * hand, that one is put in (the one inside coming out into that hand); crouching, the cassette comes out. In the
 * inventory, a cassette right-clicked onto it goes in, and right-clicking it with nothing takes the cassette out.
 */
public class WalkmanItem extends Item {
    public WalkmanItem(Properties properties) {
        super(properties);
    }

    public static ItemStack cassette(ItemStack walkman) {
        ItemContainerContents c = walkman.get(DataComponents.CONTAINER);
        return c == null ? ItemStack.EMPTY : c.copyOne();
    }

    public static void setCassette(ItemStack walkman, ItemStack cassette) {
        walkman.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(cassette.isEmpty() ? List.of() : List.of(cassette.copyWithCount(1))));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack walkman = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p)) {
            return InteractionResultHolder.success(walkman);
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack inOther = player.getItemInHand(other);
        if (player.isSecondaryUseActive()) {
            ItemStack out = cassette(walkman);
            if (out.isEmpty()) {
                p.displayClientMessage(Component.translatable("walkman.eraphones.empty").withStyle(ChatFormatting.GRAY), true);
            } else {
                Walkmans.stop(p);
                out = cassette(walkman);
                setCassette(walkman, ItemStack.EMPTY);
                if (!player.addItem(out)) {
                    player.drop(out, false);
                }
                click(p, .8f);
            }
        } else if (inOther.getItem() instanceof CassetteItem) {
            Walkmans.stop(p);
            ItemStack old = cassette(walkman);
            setCassette(walkman, inOther);
            inOther.shrink(1);
            if (!old.isEmpty()) {
                if (inOther.isEmpty()) {
                    player.setItemInHand(other, old);
                } else if (!player.addItem(old)) {
                    player.drop(old, false);
                }
            }
            click(p, 1.1f);
            p.displayClientMessage(Component.translatable("walkman.eraphones.loaded", cassette(walkman).getHoverName()).withStyle(ChatFormatting.GRAY), true);
        } else {
            Walkmans.toggle(p, walkman);
        }
        return InteractionResultHolder.consume(walkman);
    }

    private static void click(ServerPlayer p, float pitch) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, .25f, 1.8f * pitch);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack walkman, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }
        ItemStack inside = cassette(walkman);
        if (carried.getItem() instanceof CassetteItem && inside.isEmpty()) {
            setCassette(walkman, carried);
            carried.shrink(1);
            return true;
        }
        if (carried.isEmpty() && !inside.isEmpty()) {
            if (player instanceof ServerPlayer p) {
                Walkmans.stop(p);
            }
            setCassette(walkman, ItemStack.EMPTY);
            access.set(inside);
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack c = cassette(stack);
        if (c.isEmpty()) {
            tooltip.add(Component.translatable("walkman.eraphones.no_tape").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("walkman.eraphones.has_tape", c.getHoverName()).withStyle(ChatFormatting.GRAY));
            if (c.get(GearRegistry.TAPE.get()) != null) {
                tooltip.add(Component.translatable("item.eraphones.cassette.wound", Tape.clock(c.getOrDefault(GearRegistry.TAPE_POS.get(), 0L)))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("walkman.eraphones.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
