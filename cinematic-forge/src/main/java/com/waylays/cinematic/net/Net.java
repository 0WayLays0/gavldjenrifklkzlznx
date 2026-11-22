package com.waylays.cinematic.net;

import com.waylays.cinematic.CinematicMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Arrays;
import java.util.function.BiConsumer;

/** The studio's packets. Big payloads (skins, takes) travel in chunks. */
public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(CinematicMod.MOD_ID, "studio"),
            () -> VERSION, NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));
    /** Client to server packets are limited to 32 kB. */
    public static final int UP_CHUNK = 30_000;
    public static final int DOWN_CHUNK = 500_000;

    private Net() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ActionMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActionMsg::encode).decoder(ActionMsg::new).consumerMainThread(ActionMsg::handle).add();
        CHANNEL.messageBuilder(UploadMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UploadMsg::encode).decoder(UploadMsg::new).consumerMainThread(UploadMsg::handle).add();
        CHANNEL.messageBuilder(PoseKeysMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PoseKeysMsg::encode).decoder(PoseKeysMsg::new).consumerMainThread(PoseKeysMsg::handle).add();
        CHANNEL.messageBuilder(SyncMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMsg::encode).decoder(SyncMsg::new).consumerMainThread(SyncMsg::handle).add();
        CHANNEL.messageBuilder(DownloadMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DownloadMsg::encode).decoder(DownloadMsg::new).consumerMainThread(DownloadMsg::handle).add();
        CHANNEL.messageBuilder(PlayStateMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayStateMsg::encode).decoder(PlayStateMsg::new).consumerMainThread(PlayStateMsg::handle).add();
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    public static void toAll(MinecraftServer server, Object msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) toPlayer(p, msg);
    }

    /** Cuts {@code data} into pieces: (offset, piece). Empty data still sends one empty piece. */
    public static void chunks(byte[] data, int size, BiConsumer<Integer, byte[]> out) {
        if (data.length == 0) {
            out.accept(0, new byte[0]);
            return;
        }
        for (int off = 0; off < data.length; off += size) {
            out.accept(off, Arrays.copyOfRange(data, off, Math.min(data.length, off + size)));
        }
    }

    /** Puts pieces back together. */
    public static final class Assembler {
        private final byte[] data;
        private int received;

        public Assembler(int total) {
            data = new byte[total];
        }

        public int total() {
            return data.length;
        }

        /** Returns the whole data once every piece arrived, else null. */
        public byte[] add(int offset, byte[] piece) {
            if (offset < 0 || offset + piece.length > data.length) throw new IllegalArgumentException("Piece out of range");
            System.arraycopy(piece, 0, data, offset, piece.length);
            received += piece.length;
            return received >= data.length ? data : null;
        }
    }
}
