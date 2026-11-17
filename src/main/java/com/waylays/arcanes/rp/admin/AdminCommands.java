package com.waylays.arcanes.rp.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.arcanes.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * Game masters' tools: /invsee (a player's inventory, to look into, take from and put into) and /repair (the item in
 * hand's durability, back to full or to a given value).
 */
public final class AdminCommands {
    private AdminCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("invsee").requires(s -> s.hasPermission(RpConfig.get(RpConfig.INVSEE_PERMISSION, 2)))
                .then(Commands.argument("joueur", EntityArgument.player()).executes(AdminCommands::invsee)));
        d.register(Commands.literal("repair").requires(s -> s.hasPermission(RpConfig.get(RpConfig.REPAIR_PERMISSION, 2)))
                .then(Commands.literal("full").executes(c -> repair(c, -1)))
                .then(Commands.argument("durabilite", IntegerArgumentType.integer(1)).executes(c -> repair(c, IntegerArgumentType.getInteger(c, "durabilite")))));
    }

    private static int invsee(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer me = c.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(c, "joueur");
        if (target == me) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.invsee.self"));
            return 0;
        }
        InvseeMenu.Box box = new InvseeMenu.Box(target);
        me.openMenu(new SimpleMenuProvider((id, inventory, player) -> new InvseeMenu(id, inventory, box),
                Component.translatable("rp.arcanes_oublies.invsee.title", target.getGameProfile().getName())));
        return 1;
    }

    /** {@code value}: the durability left to set (from 1 to the item's most), or -1 for full. */
    private static int repair(CommandContext<CommandSourceStack> c, int value) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.repair.none"));
            return 0;
        }
        int max = stack.getMaxDamage();
        if (value > max) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.repair.range", max));
            return 0;
        }
        int left = value < 0 ? max : value;
        stack.setDamageValue(max - left);
        c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.repair.done", stack.getHoverName(), left, max)
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
