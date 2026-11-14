package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.block.RadioBlockEntity;
import com.waylays.eraphones.gear.block.VcrBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The things' packets: setting a walkie-talkie, a radio, pressing a video recorder's keys (to the server); an identity
 * card shown, the stations on the air (to the players).
 */
public final class GearNet {
    private GearNet() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payload(String name) {
        return new CustomPacketPayload.Type<>(EraPhones.id("gear_" + name));
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Walkie.TYPE, Walkie.CODEC, GearNet::walkie);
        r.playToServer(Radio.TYPE, Radio.CODEC, GearNet::radio);
        r.playToServer(Vcr.TYPE, Vcr.CODEC, GearNet::vcr);
        r.playToServer(Note.TYPE, Note.CODEC, GearNet::note);
        TapeNet.register(r);
        TvNet.register(r);
        PhotoNet.register(r);
        CamNet.register(r);
        com.waylays.eraphones.walkman.Walkmans.register(r);
        com.waylays.eraphones.arms.Arms.register(r);
        r.playToClient(Stations.TYPE, Stations.CODEC, (p, c) -> com.waylays.eraphones.gear.client.GearClient.stations(p));
    }

    /** The walkie-talkie in {@code hand}: switched on or off, on {@code channel}. */
    public record Walkie(boolean offhand, boolean on, int channel) implements CustomPacketPayload {
        public static final Type<Walkie> TYPE = payload("walkie");
        public static final StreamCodec<FriendlyByteBuf, Walkie> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBoolean(p.offhand);
            b.writeBoolean(p.on);
            b.writeVarInt(p.channel);
        }, b -> new Walkie(b.readBoolean(), b.readBoolean(), b.readVarInt()));

        @Override
        public Type<Walkie> type() {
            return TYPE;
        }
    }

    /** The radio at {@code pos}: switched on or off, its frequency (tenths of MHz), its volume (0 to 10). */
    public record Radio(BlockPos pos, boolean on, int frequency, int volume) implements CustomPacketPayload {
        public static final Type<Radio> TYPE = payload("radio");
        public static final StreamCodec<FriendlyByteBuf, Radio> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBlockPos(p.pos);
            b.writeBoolean(p.on);
            b.writeVarInt(p.frequency);
            b.writeVarInt(p.volume);
        }, b -> new Radio(b.readBlockPos(), b.readBoolean(), b.readVarInt(), b.readVarInt()));

        @Override
        public Type<Radio> type() {
            return TYPE;
        }
    }

    /** A key pressed on the video recorder at {@code pos}: insert, eject, play, pause, stop, rewind, forward. */
    public record Vcr(BlockPos pos, String key) implements CustomPacketPayload {
        public static final Type<Vcr> TYPE = payload("vcr");
        public static final StreamCodec<FriendlyByteBuf, Vcr> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.key, 16);
        }, b -> new Vcr(b.readBlockPos(), b.readUtf(16)));

        @Override
        public Type<Vcr> type() {
            return TYPE;
        }
    }

    public static final int NOTE_TITLE = 48, NOTE_TEXT = 2000;

    /** The sheet of paper in hand, written: its title, text, paper, ink, typed or by hand; signed if {@code sign}. */
    public record Note(boolean offhand, String title, String text, int style, int ink, boolean typed, boolean sign) implements CustomPacketPayload {
        public static final Type<Note> TYPE = payload("note");
        public static final StreamCodec<FriendlyByteBuf, Note> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBoolean(p.offhand);
            b.writeUtf(p.title, NOTE_TITLE);
            b.writeUtf(p.text, NOTE_TEXT);
            b.writeVarInt(p.style);
            b.writeVarInt(p.ink);
            b.writeBoolean(p.typed);
            b.writeBoolean(p.sign);
        }, b -> new Note(b.readBoolean(), b.readUtf(NOTE_TITLE), b.readUtf(NOTE_TEXT), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()));

        public Note {
            title = title.length() > NOTE_TITLE ? title.substring(0, NOTE_TITLE) : title;
            text = text.length() > NOTE_TEXT ? text.substring(0, NOTE_TEXT) : text;
        }

        @Override
        public Type<Note> type() {
            return TYPE;
        }
    }

    /** The frequencies on the air. */
    public record Stations(List<Integer> frequencies) implements CustomPacketPayload {
        public static final Type<Stations> TYPE = payload("stations");
        public static final StreamCodec<FriendlyByteBuf, Stations> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.frequencies.size());
            p.frequencies.forEach(b::writeVarInt);
        }, b -> {
            int n = Math.min(b.readVarInt(), 256);
            List<Integer> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(b.readVarInt());
            }
            return new Stations(l);
        });

        @Override
        public Type<Stations> type() {
            return TYPE;
        }
    }

    // ---------------------------------------------------------------- on the server

    private static void walkie(Walkie p, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer sp)) {
            return;
        }
        ItemStack s = sp.getItemInHand(p.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(s.getItem() instanceof WalkieItem)) {
            return;
        }
        boolean was = Gear.on(s);
        Gear.setOn(s, p.on && Gear.battery(s) > 0);
        s.set(com.waylays.eraphones.registry.GearRegistry.CHANNEL.get(), Math.max(1, Math.min(16, p.channel)));
        if (was != Gear.on(s)) {
            sp.level().playSound(null, sp.blockPosition(), com.waylays.eraphones.registry.GearRegistry.TALKIE_SQUELCH.get(), SoundSource.PLAYERS, .4f, 1.2f);
        }
    }

    private static void note(Note p, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer sp)) {
            return;
        }
        InteractionHand hand = p.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = sp.getItemInHand(hand);
        if (!(held.getItem() instanceof NoteItem) || NoteItem.signed(held)) {
            return;
        }
        // One sheet written on; the blank ones it came from stay
        ItemStack sheet = held.getCount() > 1 ? held.split(1) : held;
        sheet.set(com.waylays.eraphones.registry.GearRegistry.NOTE_TITLE.get(), p.title.strip());
        sheet.set(com.waylays.eraphones.registry.GearRegistry.NOTE_TEXT.get(), p.text);
        sheet.set(com.waylays.eraphones.registry.GearRegistry.NOTE_STYLE.get(), Math.floorMod(p.style, 8));
        sheet.set(com.waylays.eraphones.registry.GearRegistry.NOTE_INK.get(), Math.floorMod(p.ink, 5));
        sheet.set(com.waylays.eraphones.registry.GearRegistry.NOTE_TYPED.get(), p.typed);
        if (p.sign) {
            sheet.set(com.waylays.eraphones.registry.GearRegistry.NOTE_AUTHOR.get(), com.waylays.eraphones.rp.RpService.name(sp));
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, .7f, 1.1f);
        }
        if (sheet != held && !sp.getInventory().add(sheet)) {
            sp.drop(sheet, false);
        }
    }

    private static boolean near(ServerPlayer sp, BlockPos pos) {
        return sp.level().isLoaded(pos) && sp.distanceToSqr(pos.getCenter()) < 64;
    }

    private static void radio(Radio p, IPayloadContext c) {
        if (c.player() instanceof ServerPlayer sp && near(sp, p.pos) && sp.level().getBlockEntity(p.pos) instanceof RadioBlockEntity r) {
            boolean was = r.on();
            r.set(p.on, p.frequency, p.volume);
            if (was != p.on) {
                sp.level().playSound(null, p.pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, .5f, p.on ? 1.4f : 1f);
            }
        }
    }

    private static void vcr(Vcr p, IPayloadContext c) {
        if (c.player() instanceof ServerPlayer sp && near(sp, p.pos) && sp.level().getBlockEntity(p.pos) instanceof VcrBlockEntity v) {
            v.press(sp, p.key);
        }
    }
}
