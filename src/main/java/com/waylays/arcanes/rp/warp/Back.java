package com.waylays.arcanes.rp.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Collection;
import java.util.List;

/**
 * /back: a player taken back to where they stood before their last teleport (/tp, a warp, /warpgrp, /back itself, so
 * /back twice goes back and forth). The place is kept with the player, through deaths and disconnections.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Back {
    private static final String KEY = "arcanes_oublies_back";

    private Back() {}

    /** Remembers where the player stands now, before a teleport. */
    public static void remember(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putString("world", p.level().dimension().location().toString());
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        t.putFloat("pitch", p.getXRot());
        p.getPersistentData().put(KEY, t);
    }

    @SubscribeEvent
    public static void onTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            remember(p);
        }
    }

    @SubscribeEvent
    public static void onSpread(EntityTeleportEvent.SpreadPlayersCommand event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            remember(p);
        }
    }

    /** Kept through death (the respawned player is a new one). */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player old = event.getOriginal();
        if (old.getPersistentData().contains(KEY)) {
            event.getEntity().getPersistentData().put(KEY, old.getPersistentData().getCompound(KEY).copy());
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("back").requires(s -> s.hasPermission(RpConfig.get(RpConfig.BACK_PERMISSION, 2)))
                .executes(c -> back(c, List.of(c.getSource().getPlayerOrException())))
                .then(Commands.argument("joueurs", EntityArgument.players()).executes(c -> back(c, EntityArgument.getPlayers(c, "joueurs")))));
    }

    private static int back(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        int done = 0;
        for (ServerPlayer p : players) {
            CompoundTag t = p.getPersistentData().getCompound(KEY);
            ResourceLocation world = t.isEmpty() ? null : ResourceLocation.tryParse(t.getString("world"));
            ServerLevel level = world == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, world));
            if (level == null) {
                c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.back.none", p.getGameProfile().getName()));
                continue;
            }
            remember(p);
            p.teleportTo(level, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), t.getFloat("pitch"));
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .6f, 1.2f);
            p.sendSystemMessage(Component.translatable("rp.arcanes_oublies.back.done").withStyle(ChatFormatting.GRAY));
            done++;
        }
        int n = done;
        if (n > 0) {
            c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.back.sent", n).withStyle(ChatFormatting.GRAY), true);
        }
        return n;
    }
}
