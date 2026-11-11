package com.waylays.eraphones.horror;

import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.waylays.eraphones.compat.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The game master's remote: used, it opens a board listing the players about (64 blocks), to pick who, and keys for
 * every fright of /horreur and the narrator, no command to type.
 */
public class RemoteItem extends Item {
    public static final double REACH = 64;

    public RemoteItem(Properties properties) {
        super(properties);
    }

    public static boolean allowed(ServerPlayer p) {
        return p.hasPermissions(RpConfig.get(RpConfig.HORROR_PERMISSION, 2));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (!allowed(sp)) {
                sp.displayClientMessage(Component.translatable("item.eraphones.telecommande_mj.denied").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            List<ServerPlayer> about = new ArrayList<>(sp.serverLevel().getPlayers(p -> p != sp && p.distanceTo(sp) <= REACH));
            about.sort(Comparator.comparingDouble(p -> p.distanceTo(sp)));
            List<String> ids = new ArrayList<>(), names = new ArrayList<>();
            for (ServerPlayer p : about) {
                ids.add(p.getUUID().toString());
                names.add(p.getGameProfile().getName() + " (" + Math.round(p.distanceTo(sp)) + " m)");
            }
            PacketDistributor.sendToPlayer(sp, new HorrorNet.OpenRemote(ids, names));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eraphones.telecommande_mj.help").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Does {@code action} (with its words {@code arg}) to {@code targets}, for game master {@code gm}. */
    public static void act(ServerPlayer gm, List<ServerPlayer> targets, String action, String arg) {
        if (targets.isEmpty()) {
            return;
        }
        String text = arg == null ? "" : arg.trim();
        switch (action) {
            case "murmure" -> {
                if (text.isEmpty()) {
                    return;
                }
                HorrorService.show(targets, "murmure", 60 + Math.min(400, text.length() * 3), text, null);
            }
            case "narration" -> {
                if (text.isEmpty()) {
                    return;
                }
                HorrorService.show(targets, "narration", 80 + Math.min(500, text.length() * 4), text, null);
                Component line = Component.literal("✦ " + text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                for (ServerPlayer p : targets) {
                    p.sendSystemMessage(line);
                }
            }
            case "brouillard" -> HorrorService.show(targets, "brouillard", 60 * 20, "", null);
            case "coupure" -> HorrorService.show(targets, "coupure", 8 * 20, "", null);
            case "sang" -> HorrorService.show(targets, "sang", 8 * 20, "", null);
            case "tremblement" -> HorrorService.show(targets, "tremblement", 4 * 20, "", null);
            case "ballon" -> HorrorService.show(targets, "ballon", 400, "", null);
            case "peur" -> HorrorService.show(targets, "peur", 30 * 20, "", null);
            case "flotter" -> HorrorService.levitate(targets, 6 * 20);
            case "vertige" -> HorrorService.dizzy(targets, 15 * 20);
            case "stop" -> HorrorService.stop(targets);
            default -> {
                if (action.startsWith("son:") && !HorrorService.sound(targets, action.substring(4), null)) {
                    return;
                }
                if (!action.startsWith("son:")) {
                    return;
                }
            }
        }
        gm.displayClientMessage(Component.translatable("rp.eraphones.horror.sent", action.startsWith("son:") ? action.substring(4) : action, targets.size())
                .withStyle(ChatFormatting.GRAY), true);
    }
}
