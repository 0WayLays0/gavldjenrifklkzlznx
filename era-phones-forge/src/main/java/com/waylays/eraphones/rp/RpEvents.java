package com.waylays.eraphones.rp;

import com.waylays.eraphones.EraPhones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.waylays.eraphones.compat.PacketDistributor;

/**
 * The role play in the game: the chat becomes speech heard around the speaker, under their character's name; the
 * character's name is the player's name everywhere (over the head, in the list); a player coming near someone who
 * hides from them is kept from seeing them.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class RpEvents {
    private RpEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChat(ServerChatEvent event) {
        if (!RpConfig.get(RpConfig.LOCAL_CHAT, true)) {
            return;
        }
        event.setCanceled(true);
        String text = event.getRawText().strip();
        if (!text.isEmpty()) {
            RpService.say(event.getPlayer(), text.length() > 256 ? text.substring(0, 256) : text);
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p && RpService.named(p)) {
            event.setDisplayname(Component.literal(RpService.name(p)));
        }
    }

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p && RpService.named(p)) {
            event.setDisplayName(Component.literal(RpService.name(p)));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            RpVisibility.startedTracking(viewer, target);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        PacketDistributor.sendToPlayer(p, new RpNet.Names(RpService.names(p.server), true));
        RpNameplates.joined(p);
        if (RpService.named(p)) {
            RpService.syncName(p);
        } else {
            p.sendSystemMessage(Component.translatable("rp.eraphones.choose_name").withStyle(ChatFormatting.GOLD));
        }
        if (RpVisibility.hidden(p)) {
            p.sendSystemMessage(Component.translatable("rp.eraphones.still_hidden").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RpVisibility.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        RpCommands.register(event.getDispatcher());
        com.waylays.eraphones.horror.HorrorCommands.register(event.getDispatcher());
        com.waylays.eraphones.gear.GearCommands.register(event.getDispatcher());
    }
}
