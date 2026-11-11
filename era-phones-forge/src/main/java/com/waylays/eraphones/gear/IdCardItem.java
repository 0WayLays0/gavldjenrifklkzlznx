package com.waylays.eraphones.gear;

import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.rp.RpData;
import com.waylays.eraphones.rp.RpService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.waylays.eraphones.compat.PacketDistributor;

import java.util.List;

/**
 * An identity card. Blank, it is made out to whoever first uses it (their character's name and description, a number
 * of its own) and stays theirs; using it shows it, using it on someone shows it to them.
 */
public class IdCardItem extends Item {
    public IdCardItem(Properties properties) {
        super(properties);
    }

    /** Makes the card out to {@code p} if it is still blank. */
    public static void issue(ItemStack stack, ServerPlayer p) {
        if (GearRegistry.CARD_OWNER.get().get(stack) != null) {
            return;
        }
        GearRegistry.CARD_OWNER.get().set(stack, p.getUUID().toString());
        GearRegistry.CARD_NAME.get().set(stack, RpService.name(p));
        RpData.Identity id = RpData.get(p.server).identity(p.getUUID());
        GearRegistry.CARD_DESC.get().set(stack, id == null ? "" : id.description());
        long n = (p.getUUID().getLeastSignificantBits() ^ p.level().getGameTime() * 2654435761L) & 0xFFFFFFFL;
        GearRegistry.CARD_NUMBER.get().set(stack, String.format("%03d-%02d-%04d", n % 900 + 100, n / 900 % 90 + 10, n / 81000 % 9000 + 1000));
    }

    public static GearNet.Card card(ItemStack stack, String shownBy) {
        return new GearNet.Card(GearRegistry.CARD_OWNER.get().getOrDefault(stack, ""), GearRegistry.CARD_NAME.get().getOrDefault(stack, ""),
                GearRegistry.CARD_NUMBER.get().getOrDefault(stack, ""), GearRegistry.CARD_DESC.get().getOrDefault(stack, ""), shownBy);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            issue(stack, sp);
            PacketDistributor.sendToPlayer(sp, card(stack, ""));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ServerPlayer other) || !(player instanceof ServerPlayer sp)) {
            return target instanceof Player ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        issue(stack, sp);
        PacketDistributor.sendToPlayer(other, card(stack, RpService.name(sp)));
        sp.displayClientMessage(Component.translatable("gear.eraphones.card_shown", RpService.name(other)).withStyle(ChatFormatting.GRAY), true);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, .6f, 1.4f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        String name = GearRegistry.CARD_NAME.get().get(stack);
        if (name == null) {
            tooltip.add(Component.translatable("item.eraphones.carte_identite.blank").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal(name).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("N° " + GearRegistry.CARD_NUMBER.get().getOrDefault(stack, "")).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.eraphones.carte_identite.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
