package com.waylays.arcanes.network;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.music.RemoteMusic;
import com.waylays.arcanes.music.MusicRelay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The music player's packets. A player's music is played on the player: they tell the server what they play (a track
 * known by the hash of its file, its title, how loud, how far it carries, whether it loops); the server tells the players
 * in range, who ask for the file if they have not got it; the server asks the player playing it to send it up, in
 * pieces, and passes it on to them, in pieces too.
 */
public final class MusicPackets {
    /** The most a music file can weigh to be passed on to other players. */
    public static final int MAX_FILE = 24 * 1024 * 1024;
    /** A piece of a file sent up to the server (a client's packets to the server stay under 32 KiB). */
    public static final int UP_CHUNK = 30000;
    /** A piece of a file sent down to a client. */
    public static final int DOWN_CHUNK = 256 * 1024;
    static final int HASH = 40, TITLE = 160;

    private MusicPackets() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Start.TYPE, Start.CODEC, Start::handle);
        registrar.playToServer(Stop.TYPE, Stop.CODEC, Stop::handle);
        registrar.playToServer(Request.TYPE, Request.CODEC, Request::handle);
        registrar.playToServer(Upload.TYPE, Upload.CODEC, Upload::handle);
        registrar.playToClient(Play.TYPE, Play.CODEC, Play::handle);
        registrar.playToClient(Halt.TYPE, Halt.CODEC, Halt::handle);
        registrar.playToClient(SendUp.TYPE, SendUp.CODEC, SendUp::handle);
        registrar.playToClient(Data.TYPE, Data.CODEC, Data::handle);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(ArcanesOublies.id("music_" + name));
    }

    // ---------------------------------------------------------------- client to server

    /**
     * A player plays a track: {@code hash} of its file, its {@code title}, {@code volume} (0 to 1), {@code range} in blocks
     * (0: only the player hears it), whether it {@code loop}s, {@code offsetMs} into it now, {@code lengthMs} long (0 if
     * not known). Sent again whenever one of these changes.
     */
    public record Start(String hash, String title, float volume, int range, boolean loop, int offsetMs, int lengthMs, int size)
            implements CustomPacketPayload {
        public static final Type<Start> TYPE = type("start");
        public static final StreamCodec<FriendlyByteBuf, Start> CODEC = CustomPacketPayload.codec(Start::write, Start::new);

        Start(FriendlyByteBuf buf) {
            this(buf.readUtf(HASH), buf.readUtf(TITLE), buf.readFloat(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(hash, HASH);
            buf.writeUtf(title.length() > TITLE ? title.substring(0, TITLE) : title, TITLE);
            buf.writeFloat(volume);
            buf.writeVarInt(range);
            buf.writeBoolean(loop);
            buf.writeVarInt(offsetMs);
            buf.writeVarInt(lengthMs);
            buf.writeVarInt(size);
        }

        @Override
        public Type<Start> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                MusicRelay.start(player, this);
            }
        }
    }

    /** A player stops their music. */
    public record Stop() implements CustomPacketPayload {
        public static final Type<Stop> TYPE = type("stop");
        public static final StreamCodec<FriendlyByteBuf, Stop> CODEC = StreamCodec.unit(new Stop());

        @Override
        public Type<Stop> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                MusicRelay.stop(player);
            }
        }
    }

    /** A player asks for the file of a track they hear but have not got. */
    public record Request(String hash) implements CustomPacketPayload {
        public static final Type<Request> TYPE = type("request");
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = CustomPacketPayload.codec(Request::write, Request::new);

        Request(FriendlyByteBuf buf) {
            this(buf.readUtf(HASH));
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(hash, HASH);
        }

        @Override
        public Type<Request> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                MusicRelay.request(player, hash);
            }
        }
    }

    /** A piece ({@code index} of {@code total}) of the file of a track, sent up by the player playing it. */
    public record Upload(String hash, int index, int total, byte[] data) implements CustomPacketPayload {
        public static final Type<Upload> TYPE = type("upload");
        public static final StreamCodec<FriendlyByteBuf, Upload> CODEC = CustomPacketPayload.codec(Upload::write, Upload::new);

        Upload(FriendlyByteBuf buf) {
            this(buf.readUtf(HASH), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(UP_CHUNK));
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(hash, HASH);
            buf.writeVarInt(index);
            buf.writeVarInt(total);
            buf.writeByteArray(data);
        }

        @Override
        public Type<Upload> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                MusicRelay.upload(player, this);
            }
        }
    }

    // ---------------------------------------------------------------- server to client

    /** The player with entity {@code entityId} plays this track, {@code offsetMs} into it: heard here if in range. */
    public record Play(int entityId, String hash, String title, float volume, int range, boolean loop, int offsetMs, int lengthMs)
            implements CustomPacketPayload {
        public static final Type<Play> TYPE = type("play");
        public static final StreamCodec<FriendlyByteBuf, Play> CODEC = CustomPacketPayload.codec(Play::write, Play::new);

        Play(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readUtf(HASH), buf.readUtf(TITLE), buf.readFloat(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
        }

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeUtf(hash, HASH);
            buf.writeUtf(title, TITLE);
            buf.writeFloat(volume);
            buf.writeVarInt(range);
            buf.writeBoolean(loop);
            buf.writeVarInt(offsetMs);
            buf.writeVarInt(lengthMs);
        }

        @Override
        public Type<Play> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            RemoteMusic.play(this);
        }
    }

    /** The player with entity {@code entityId} stopped their music (or went out of range). */
    public record Halt(int entityId) implements CustomPacketPayload {
        public static final Type<Halt> TYPE = type("halt");
        public static final StreamCodec<FriendlyByteBuf, Halt> CODEC = CustomPacketPayload.codec(Halt::write, Halt::new);

        Halt(FriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
        }

        @Override
        public Type<Halt> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            RemoteMusic.halt(entityId);
        }
    }

    /** The server asks the player playing a track to send its file up: someone near needs it. */
    public record SendUp(String hash) implements CustomPacketPayload {
        public static final Type<SendUp> TYPE = type("send_up");
        public static final StreamCodec<FriendlyByteBuf, SendUp> CODEC = CustomPacketPayload.codec(SendUp::write, SendUp::new);

        SendUp(FriendlyByteBuf buf) {
            this(buf.readUtf(HASH));
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(hash, HASH);
        }

        @Override
        public Type<SendUp> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            RemoteMusic.sendUp(hash);
        }
    }

    /** A piece ({@code index} of {@code total}) of a track's file, passed on by the server. */
    public record Data(String hash, int index, int total, byte[] data) implements CustomPacketPayload {
        public static final Type<Data> TYPE = type("data");
        public static final StreamCodec<FriendlyByteBuf, Data> CODEC = CustomPacketPayload.codec(Data::write, Data::new);

        Data(FriendlyByteBuf buf) {
            this(buf.readUtf(HASH), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(DOWN_CHUNK));
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(hash, HASH);
            buf.writeVarInt(index);
            buf.writeVarInt(total);
            buf.writeByteArray(data);
        }

        @Override
        public Type<Data> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            RemoteMusic.data(this);
        }
    }
}
