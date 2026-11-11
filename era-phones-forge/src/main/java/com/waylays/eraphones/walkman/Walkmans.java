package com.waylays.eraphones.walkman;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.Audio;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.waylays.eraphones.compat.StreamCodec;
import com.waylays.eraphones.compat.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import com.waylays.eraphones.compat.PacketDistributor;
import com.waylays.eraphones.compat.PayloadRegistrar;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cassette players playing: for each listener, the tape running (through Simple Voice Chat, heard by them alone),
 * stopped when the player puts the walkman away out of their pockets, takes the cassette out, or leaves; the tape's
 * place kept on the cassette. Those around see the headphones on their head while it plays.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Walkmans {
    private static final Map<UUID, Session> PLAYING = new ConcurrentHashMap<>();

    private Walkmans() {}

    private static final class Session {
        final String file;
        final Tape.Deck deck = new Tape.Deck();
        Object handle;

        Session(String file) {
            this.file = file;
        }
    }

    public static boolean listening(ServerPlayer p) {
        return PLAYING.containsKey(p.getUUID());
    }

    /** Plays, or pauses, the cassette in {@code walkman}. */
    public static void toggle(ServerPlayer p, ItemStack walkman) {
        if (PLAYING.containsKey(p.getUUID())) {
            stop(p);
            p.displayClientMessage(Component.translatable("walkman.eraphones.paused").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ItemStack tape = WalkmanItem.cassette(walkman);
        if (tape.isEmpty()) {
            p.displayClientMessage(Component.translatable("walkman.eraphones.no_tape").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        String file = GearRegistry.TAPE.get().get(tape);
        if (file == null) {
            p.displayClientMessage(Component.translatable("walkman.eraphones.blank").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Audio audio = Audio.get();
        if (audio == null) {
            p.displayClientMessage(Component.translatable("walkman.eraphones.no_voice").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Session s = new Session(file);
        s.deck.file = file;
        s.deck.pos = GearRegistry.TAPE_POS.get().getOrDefault(tape, 0L);
        short[] pcm = Tape.sound(file);
        if (pcm != null && s.deck.pos >= pcm.length - Audio.FRAME) {
            // At its end: wound back to the start
            s.deck.pos = 0;
        }
        s.deck.mode = Tape.Mode.PLAY;
        s.handle = audio.playTo(p, s.deck::frame);
        if (s.handle == null) {
            p.displayClientMessage(Component.translatable("walkman.eraphones.no_voice").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        PLAYING.put(p.getUUID(), s);
        tell(p, true);
        p.displayClientMessage(Component.translatable("walkman.eraphones.playing", tape.getHoverName()).withStyle(ChatFormatting.GOLD), true);
    }

    /** Stops what {@code p} listens to, the tape's place written back on the cassette in their walkman. */
    public static void stop(ServerPlayer p) {
        Session s = PLAYING.remove(p.getUUID());
        if (s == null) {
            return;
        }
        Audio audio = Audio.get();
        if (audio != null && s.handle != null) {
            audio.stop(s.handle);
        }
        ItemStack walkman = find(p, s.file);
        if (!walkman.isEmpty()) {
            ItemStack tape = WalkmanItem.cassette(walkman);
            GearRegistry.TAPE_POS.get().set(tape, s.deck.pos);
            WalkmanItem.setCassette(walkman, tape);
        }
        tell(p, false);
    }

    /** The walkman {@code p} carries (hands, hotbar, pockets) holding the tape {@code file}; empty if none. */
    private static ItemStack find(ServerPlayer p, String file) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof WalkmanItem && file.equals(GearRegistry.TAPE.get().get(WalkmanItem.cassette(s)))) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % 10 != 0 || PLAYING.isEmpty()) {
            return;
        }
        Audio audio = Audio.get();
        for (Iterator<Map.Entry<UUID, Session>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Session> e = it.next();
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
            Session s = e.getValue();
            boolean over = audio == null || s.handle == null || !audio.playing(s.handle);
            if (p == null) {
                if (audio != null && s.handle != null) {
                    audio.stop(s.handle);
                }
                it.remove();
            } else if (over || find(p, s.file).isEmpty()) {
                stop(p);
                if (over) {
                    p.displayClientMessage(Component.translatable("walkman.eraphones.ended").withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            stop(p);
        }
    }

    /** One who comes to see a listener sees their headphones. */
    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && listening(target) && event.getEntity() instanceof ServerPlayer watcher) {
            PacketDistributor.sendToPlayer(watcher, new Listening(target.getUUID(), true));
        }
    }

    private static void tell(ServerPlayer p, boolean on) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new Listening(p.getUUID(), on));
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Listening.TYPE, Listening.CODEC, (l, c) -> c.enqueueWork(() -> com.waylays.eraphones.walkman.client.WalkmanClient.listening(l)));
    }

    /** Whether {@code player} has their headphones on, listening. */
    public record Listening(UUID player, boolean on) implements CustomPacketPayload {
        public static final Type<Listening> TYPE = new Type<>(EraPhones.id("walkman_listening"));
        public static final StreamCodec<FriendlyByteBuf, Listening> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUUID(p.player);
            b.writeBoolean(p.on);
        }, b -> new Listening(b.readUUID(), b.readBoolean()));

        @Override
        public Type<Listening> type() {
            return TYPE;
        }
    }
}
