package com.waylays.arcanes.metier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The grindstone: a right click on it with a worn weapon (or any item with durability) starts its sharpening
 * minigame ({@code GrindGame}). A critical success gives back 200 durability, a success 100, a failure takes 50.
 * Each weapon may go back to the grindstone only 12 hours (real time) later. Game masters keep some weapons from it
 * altogether (/meule): the kind of item, or one item only.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Grindstone extends SavedData {
    /** The minigame sessions' trade id. */
    static final String ID = "arcanes:meule";
    private static final String NAME = ArcanesOublies.MODID + "_meule";
    /** On the item: when it may go back to the grindstone (ms since 1970), and whether it is kept from it. */
    private static final String NEXT = "arcanes_meule_suivante", BANNED = "arcanes_meule_interdite";
    private static final long COOLDOWN = 12L * 3600 * 1000;
    private static final int CRITICAL = 200, SUCCESS = 100, FAILURE = 50;
    private static final float PERFECT_AT = .82f, CRITICAL_BELOW = .3f;

    /** The kinds of item kept from the grindstone. */
    private final Set<String> banned = new TreeSet<>();

    private static Grindstone get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Grindstone::new, Grindstone::load, null), NAME);
    }

    private static Grindstone load(CompoundTag tag, HolderLookup.Provider registries) {
        Grindstone g = new Grindstone();
        for (Tag t : tag.getList("interdits", Tag.TAG_STRING)) {
            g.banned.add(t.getAsString());
        }
        return g;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        banned.forEach(s -> list.add(StringTag.valueOf(s)));
        tag.put("interdits", list);
        return tag;
    }

    // ---------------------------------------------------------------- the minigame

    /** Whether a right click on this block, with what the player holds, is for the grindstone's minigame. */
    static boolean wants(ServerPlayer player, BlockState state) {
        return state.is(Blocks.GRINDSTONE) && player.getMainHandItem().isDamageableItem();
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    static boolean banned(MinecraftServer server, ItemStack stack) {
        return data(stack).getBoolean(BANNED) || get(server).banned.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    static void start(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack held = player.getMainHandItem();
        if (banned(level.getServer(), held)) {
            rp(player, Component.translatable("metier.arcanes_oublies.grind.banned"));
            return;
        }
        if (held.getDamageValue() <= 0) {
            rp(player, Component.translatable("metier.arcanes_oublies.grind.whole"));
            return;
        }
        long left = data(held).getLong(NEXT) - System.currentTimeMillis();
        if (left > 0) {
            rp(player, Component.translatable("metier.arcanes_oublies.grind.rest", MetierService.time(left / 1000)));
            return;
        }
        MetierService.begin(player, level, pos, state, ID, MetierDefs.Game.GRIND, -1, held.getItem(), 1, held.copyWithCount(1), ItemStack.EMPTY,
                List.of(), Component.translatable("metier.arcanes_oublies.grind.title").getString(), held.getHoverName().getString(), PERFECT_AT,
                CRITICAL_BELOW);
    }

    /** The minigame's end: the weapon's durability, its next turn at the grindstone, a line in character. */
    static void settle(ServerPlayer player, int session, ServerLevel level, BlockPos pos, BlockState state, @Nullable Item weapon, float score,
                       boolean critical, boolean tooFast) {
        ItemStack held = player.getMainHandItem();
        if (player.level() != level || player.distanceToSqr(pos.getCenter()) > MetierService.MAX_DISTANCE * MetierService.MAX_DISTANCE
                || !level.getBlockState(pos).is(state.getBlock())) {
            PacketDistributor.sendToPlayer(player, new MetierNet.Stop(session, "metier.arcanes_oublies.stop.gone"));
            return;
        }
        if (weapon == null || !held.is(weapon) || !held.isDamageableItem()) {
            PacketDistributor.sendToPlayer(player, new MetierNet.Stop(session, "metier.arcanes_oublies.stop.tool"));
            return;
        }
        score = Math.max(0, Math.min(1, score));
        MetierDefs.Outcome outcome = critical || tooFast || score < CRITICAL_BELOW ? MetierDefs.Outcome.CRITICAL
                : score >= PERFECT_AT ? MetierDefs.Outcome.PERFECT : MetierDefs.Outcome.PARTIAL;
        int change = switch (outcome) {
            case PERFECT -> CRITICAL;
            case PARTIAL -> SUCCESS;
            case CRITICAL -> -FAILURE;
        };
        int max = held.getMaxDamage();
        // A failure wears the blade but never breaks it
        held.setDamageValue(Math.max(0, Math.min(max - 1, held.getDamageValue() - change)));
        CustomData.update(DataComponents.CUSTOM_DATA, held, t -> t.putLong(NEXT, System.currentTimeMillis() + COOLDOWN));
        level.playSound(null, pos, switch (outcome) {
            case PERFECT -> SoundEvents.ANVIL_USE;
            case PARTIAL -> SoundEvents.GRINDSTONE_USE;
            case CRITICAL -> SoundEvents.ITEM_BREAK;
        }, SoundSource.PLAYERS, .7f, outcome == MetierDefs.Outcome.PERFECT ? 1.5f : 1f);
        rp(player, Component.translatable(switch (outcome) {
            case PERFECT -> "metier.arcanes_oublies.grind.perfect";
            case PARTIAL -> "metier.arcanes_oublies.grind.done";
            case CRITICAL -> "metier.arcanes_oublies.grind.failed";
        }, held.getHoverName(), Math.abs(change)));
        // The result screen: durability given back (negative wear), or lost
        PacketDistributor.sendToPlayer(player, new MetierNet.Outcome(session, outcome.ordinal(), List.of(), -change));
    }

    /** A line in character, in the chat. */
    private static void rp(ServerPlayer player, Component line) {
        player.sendSystemMessage(Component.literal("✦ ").withStyle(ChatFormatting.DARK_GRAY).append(line.copy().withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC)));
    }

    // ---------------------------------------------------------------- the game masters' list

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("meule").requires(s -> s.hasPermission(RpConfig.get(RpConfig.REPAIR_PERMISSION, 2)))
                .then(Commands.literal("interdire").executes(c -> kind(c, true))
                        .then(Commands.literal("objet").executes(c -> one(c, true))))
                .then(Commands.literal("autoriser").executes(c -> kind(c, false))
                        .then(Commands.literal("objet").executes(c -> one(c, false))))
                .then(Commands.literal("liste").executes(Grindstone::list))
                .then(Commands.literal("reset").executes(c -> {
                    ItemStack held = held(c);
                    if (held == null) {
                        return 0;
                    }
                    CustomData.update(DataComponents.CUSTOM_DATA, held, t -> t.remove(NEXT));
                    c.getSource().sendSuccess(() -> Component.translatable("metier.arcanes_oublies.grind.reset", held.getHoverName()), true);
                    return 1;
                })));
    }

    @Nullable
    private static ItemStack held(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack held = c.getSource().getPlayerOrException().getMainHandItem();
        if (held.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("metier.arcanes_oublies.grind.hold"));
            return null;
        }
        return held;
    }

    /** Keeps (or lets back) every item of the held one's kind from the grindstone. */
    private static int kind(CommandContext<CommandSourceStack> c, boolean ban) throws CommandSyntaxException {
        ItemStack held = held(c);
        if (held == null) {
            return 0;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
        Grindstone g = get(c.getSource().getServer());
        if (ban) {
            g.banned.add(id.toString());
        } else {
            g.banned.remove(id.toString());
        }
        g.setDirty();
        c.getSource().sendSuccess(() -> Component.translatable(ban ? "metier.arcanes_oublies.grind.banned_kind" : "metier.arcanes_oublies.grind.allowed_kind",
                held.getItem().getDescription()), true);
        return 1;
    }

    /** Keeps (or lets back) this one item from the grindstone. */
    private static int one(CommandContext<CommandSourceStack> c, boolean ban) throws CommandSyntaxException {
        ItemStack held = held(c);
        if (held == null) {
            return 0;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, held, t -> {
            if (ban) {
                t.putBoolean(BANNED, true);
            } else {
                t.remove(BANNED);
            }
        });
        c.getSource().sendSuccess(() -> Component.translatable(ban ? "metier.arcanes_oublies.grind.banned_one" : "metier.arcanes_oublies.grind.allowed_one",
                held.getHoverName()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        Set<String> all = get(c.getSource().getServer()).banned;
        c.getSource().sendSuccess(() -> Component.translatable("metier.arcanes_oublies.grind.list", all.isEmpty() ? "—" : String.join(", ", all)), false);
        return all.size();
    }
}
