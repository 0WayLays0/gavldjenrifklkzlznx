package com.waylays.eraphones.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /boitecode, for the game master looking at a code box: code &lt;digits&gt; (3 to 6), fermer (locked again, its lid
 * shut), ouvrir, info.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class CodeBoxCommands {
    private CodeBoxCommands() {}

    private interface Act {
        int run(CommandContext<CommandSourceStack> c, CodeBoxBlockEntity box) throws CommandSyntaxException;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("boitecode").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .executes(c -> on(c, CodeBoxCommands::info))
                .then(Commands.literal("info").executes(c -> on(c, CodeBoxCommands::info)))
                .then(Commands.literal("code").then(Commands.argument("chiffres", StringArgumentType.word()).executes(c -> on(c, (cc, box) -> {
                    String code = StringArgumentType.getString(cc, "chiffres");
                    if (!CodeBoxBlockEntity.validCode(code)) {
                        cc.getSource().sendFailure(Component.translatable("event.eraphones.codebox.bad_code"));
                        return 0;
                    }
                    box.setCode(code);
                    return say(cc, "event.eraphones.codebox.code_set", code);
                }))))
                .then(Commands.literal("fermer").executes(c -> on(c, (cc, box) -> {
                    box.setOpen(false);
                    return say(cc, "event.eraphones.codebox.locked");
                })))
                .then(Commands.literal("ouvrir").executes(c -> on(c, (cc, box) -> {
                    box.setOpen(true);
                    return say(cc, "event.eraphones.codebox.unlocked");
                }))));
    }

    private static int info(CommandContext<CommandSourceStack> c, CodeBoxBlockEntity box) {
        return say(c, "event.eraphones.codebox.info", box.code(), Component.translatable(box.open() ? "event.eraphones.codebox.is_open"
                : "event.eraphones.codebox.is_locked"));
    }

    private static int say(CommandContext<CommandSourceStack> c, String key, Object... args) {
        c.getSource().sendSuccess(() -> Component.translatable(key, args).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int on(CommandContext<CommandSourceStack> c, Act act) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(8)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.MISS || !(p.level().getBlockEntity(hit.getBlockPos()) instanceof CodeBoxBlockEntity box)) {
            c.getSource().sendFailure(Component.translatable("event.eraphones.codebox.look_at_one"));
            return 0;
        }
        return act.run(c, box);
    }
}
