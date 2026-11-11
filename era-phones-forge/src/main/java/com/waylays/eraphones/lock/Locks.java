package com.waylays.eraphones.lock;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.waylays.eraphones.compat.StreamCodec;
import com.waylays.eraphones.compat.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import com.waylays.eraphones.compat.PacketDistributor;
import com.waylays.eraphones.compat.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The padlocks in the world: which doors, trapdoors, gates and chests are locked, and with which key. A locked thing
 * opens only for a hand holding its key (a game master in creative goes through); it cannot be broken (but in
 * creative) nor blown up. Crouching with its key, one takes the padlock off. The players see the padlocks hanging.
 *
 * <p>/cle copier (a copy of the key in hand), /cle info; /badge creer &lt;name&gt;, /badge copier.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Locks extends SavedData {
    private static final String NAME = EraPhones.MODID + "_cadenas";
    private static final Random RANDOM = new Random();
    /** By dimension, the locked blocks and their key. */
    private final Map<String, Map<Long, String>> locks = new HashMap<>();

    private Locks() {}

    public static Locks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Locks::load, Locks::new, NAME);
    }

    private static Locks load(CompoundTag tag) {
        Locks l = new Locks();
        for (String dim : tag.getAllKeys()) {
            Map<Long, String> m = new HashMap<>();
            for (Tag t : tag.getList(dim, Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                m.put(c.getLong("Pos"), c.getString("Key"));
            }
            l.locks.put(dim, m);
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<String, Map<Long, String>> e : locks.entrySet()) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, String> l : e.getValue().entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putLong("Pos", l.getKey());
                c.putString("Key", l.getValue());
                list.add(c);
            }
            tag.put(e.getKey(), list);
        }
        return tag;
    }

    private Map<Long, String> in(Level level) {
        return locks.computeIfAbsent(level.dimension().location().toString(), k -> new HashMap<>());
    }

    /** The key a block is locked with, or null. */
    public static String keyAt(ServerLevel level, BlockPos pos) {
        return get(level.getServer()).in(level).get(pos.asLong());
    }

    public static String newKey() {
        String abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            b.append(abc.charAt(RANDOM.nextInt(abc.length())));
        }
        return b.toString();
    }

    /** The blocks one padlock holds together: both halves of a door, both halves of a double chest. */
    private static List<BlockPos> parts(Level level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        BlockState s = level.getBlockState(pos);
        out.add(pos);
        if (s.getBlock() instanceof DoorBlock) {
            out.add(s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
        } else if (s.getBlock() instanceof ChestBlock && s.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
            out.add(pos.relative(ChestBlock.getConnectedDirection(s)));
        }
        return out;
    }

    public static boolean lockable(BlockState s) {
        return s.getBlock() instanceof DoorBlock || s.getBlock() instanceof TrapDoorBlock || s.getBlock() instanceof FenceGateBlock
                || s.getBlock() instanceof ChestBlock || s.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock;
    }

    /** {@code p} puts a padlock on what is at {@code pos} (with {@code key}'s secret, or a new one: its key is given). */
    public static void lock(ServerPlayer p, BlockPos pos, String key, ItemStack padlock) {
        ServerLevel level = p.serverLevel();
        BlockState s = level.getBlockState(pos);
        if (!lockable(s)) {
            p.displayClientMessage(Component.translatable("lock.eraphones.not_lockable").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Locks data = get(p.server);
        if (data.in(level).containsKey(pos.asLong())) {
            p.displayClientMessage(Component.translatable("lock.eraphones.already").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean fresh = key == null;
        if (fresh) {
            key = newKey();
        }
        for (BlockPos q : parts(level, pos)) {
            data.in(level).put(q.asLong(), key);
        }
        data.setDirty();
        if (!p.getAbilities().instabuild) {
            padlock.shrink(1);
        }
        if (fresh) {
            ItemStack k = new ItemStack(LockRegistry.KEY_ITEM.get());
            LockRegistry.KEY.get().set(k, key);
            if (!p.addItem(k)) {
                p.drop(k, false);
            }
        }
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, .7f, 1.6f);
        p.displayClientMessage(Component.translatable("lock.eraphones.locked", key).withStyle(ChatFormatting.GOLD), true);
        sync(level);
    }

    private static boolean holdsKey(Player p, String key) {
        for (InteractionHand h : InteractionHand.values()) {
            if (key.equals(KeyItem.id(p.getItemInHand(h)))) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player p = event.getEntity();
        String key;
        if (level.isClientSide) {
            key = com.waylays.eraphones.lock.client.LockClient.keyKnown(event.getPos()) ? "?" : null;
            // The player's side only knows a padlock is there: it lets the server decide, but does not swing the door
            if (key != null && !p.isCreative() && !(p.getMainHandItem().getItem() instanceof KeyItem) && !(p.getOffhandItem().getItem() instanceof KeyItem)) {
                event.setCanceled(true);
            }
            return;
        }
        key = keyAt((ServerLevel) level, event.getPos());
        if (key == null) {
            return;
        }
        if (p.isSecondaryUseActive() && holdsKey(p, key)) {
            // The padlock taken off
            Locks data = get(level.getServer());
            for (BlockPos q : parts(level, event.getPos())) {
                data.in(level).remove(q.asLong());
            }
            data.setDirty();
            ItemStack lock = new ItemStack(LockRegistry.PADLOCK.get());
            if (!p.addItem(lock)) {
                p.drop(lock, false);
            }
            level.playSound(null, event.getPos(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, .7f, 1.6f);
            p.displayClientMessage(Component.translatable("lock.eraphones.unlocked").withStyle(ChatFormatting.GOLD), true);
            sync((ServerLevel) level);
            event.setCanceled(true);
            return;
        }
        if (holdsKey(p, key) || p.isCreative()) {
            if (holdsKey(p, key)) {
                level.playSound(null, event.getPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, .6f, 1.8f);
            }
            return;
        }
        event.setCanceled(true);
        level.playSound(null, event.getPos(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, .8f, 1);
        p.displayClientMessage(Component.translatable("lock.eraphones.is_locked").withStyle(ChatFormatting.GRAY), true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || keyAt(level, event.getPos()) == null) {
            return;
        }
        if (!event.getPlayer().isCreative()) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("lock.eraphones.is_locked").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Locks data = get(level.getServer());
        for (BlockPos q : parts(level, event.getPos())) {
            data.in(level).remove(q.asLong());
        }
        data.setDirty();
        sync(level);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Map<Long, String> m = get(level.getServer()).in(level);
            event.getAffectedBlocks().removeIf(b -> m.containsKey(b.asLong()));
        }
    }

    // ---------------------------------------------------------------- the players see them

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            send(p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            send(p);
        }
    }

    private static void send(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new Positions(new ArrayList<>(get(p.server).in(p.level()).keySet())));
    }

    private static void sync(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, new Positions(new ArrayList<>(get(level.getServer()).in(level).keySet())));
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Positions.TYPE, Positions.CODEC, (p, c) -> c.enqueueWork(() -> com.waylays.eraphones.lock.client.LockClient.positions(p)));
    }

    /** The padlocked blocks of the dimension the player is in. */
    public record Positions(List<Long> blocks) implements CustomPacketPayload {
        public static final Type<Positions> TYPE = new Type<>(EraPhones.id("padlocks"));
        public static final StreamCodec<FriendlyByteBuf, Positions> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.blocks.size());
            for (long l : p.blocks) {
                b.writeLong(l);
            }
        }, b -> {
            int n = Math.min(b.readVarInt(), 100000);
            List<Long> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                l.add(b.readLong());
            }
            return new Positions(l);
        });

        @Override
        public Type<Positions> type() {
            return TYPE;
        }
    }

    // ---------------------------------------------------------------- /cle, /badge

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        int level = RpConfig.get(RpConfig.HORROR_PERMISSION, 2);
        event.getDispatcher().register(Commands.literal("cle").requires(s -> s.hasPermission(level))
                .then(Commands.literal("copier").executes(c -> copy(c, LockRegistry.KEY_ITEM.get(), true)))
                .then(Commands.literal("nouvelle").executes(Locks::newKeyCommand)));
        event.getDispatcher().register(Commands.literal("badge").requires(s -> s.hasPermission(level))
                .then(Commands.literal("creer").then(Commands.argument("nom", StringArgumentType.greedyString()).executes(Locks::newBadge)))
                .then(Commands.literal("copier").executes(c -> copy(c, LockRegistry.BADGE_ITEM.get(), false))));
    }

    private static int newKeyCommand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack k = new ItemStack(LockRegistry.KEY_ITEM.get());
        String id = newKey();
        LockRegistry.KEY.get().set(k, id);
        p.getInventory().add(k);
        c.getSource().sendSuccess(() -> Component.translatable("lock.eraphones.key_made", id).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int newBadge(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "nom");
        ItemStack b = new ItemStack(LockRegistry.BADGE_ITEM.get());
        String id = newKey();
        LockRegistry.BADGE.get().set(b, id);
        b.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        p.getInventory().add(b);
        c.getSource().sendSuccess(() -> Component.translatable("lock.eraphones.badge_made", name, id).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int copy(CommandContext<CommandSourceStack> c, net.minecraft.world.item.Item what, boolean key) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack held = p.getMainHandItem();
        if (!held.is(what)) {
            c.getSource().sendFailure(Component.translatable(key ? "lock.eraphones.hold_key" : "lock.eraphones.hold_badge"));
            return 0;
        }
        p.getInventory().add(held.copyWithCount(1));
        c.getSource().sendSuccess(() -> Component.translatable("lock.eraphones.copied").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    /** A direction-free helper for the renderers: whether a door at {@code s} faces along x. */
    public static boolean alongX(Direction d) {
        return d.getAxis() == Direction.Axis.X;
    }
}
