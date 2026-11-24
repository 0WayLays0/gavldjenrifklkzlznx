package com.waylays.arcanes.rp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

/**
 * /marchesurleau (staff only): water bears the player up like ground, until it is taken back (crouching, they go into
 * the water as usual). Alone, it switches the staff member's own; with players, theirs ("oui" or "non" to set rather
 * than switch).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class WaterWalkCommand {
    private WaterWalkCommand() {}

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("marchesurleau").requires(s -> s.hasPermission(2))
                .executes(c -> run(c, List.of(c.getSource().getPlayerOrException()), null))
                .then(Commands.argument("joueurs", EntityArgument.players())
                        .executes(c -> run(c, EntityArgument.getPlayers(c, "joueurs"), null))
                        .then(Commands.literal("oui").executes(c -> run(c, EntityArgument.getPlayers(c, "joueurs"), true)))
                        .then(Commands.literal("non").executes(c -> run(c, EntityArgument.getPlayers(c, "joueurs"), false)))));
    }

    private static int run(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, Boolean on) throws CommandSyntaxException {
        for (ServerPlayer p : players) {
            boolean walk = on != null ? on : !p.hasEffect(ModEffects.WATER_WALKING);
            if (walk) {
                p.addEffect(new MobEffectInstance(ModEffects.WATER_WALKING, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            } else {
                p.removeEffect(ModEffects.WATER_WALKING);
            }
            c.getSource().sendSuccess(() -> Component.translatable(walk ? "rp.arcanes_oublies.water_walk.on" : "rp.arcanes_oublies.water_walk.off",
                    p.getDisplayName()), true);
        }
        return players.size();
    }
}
