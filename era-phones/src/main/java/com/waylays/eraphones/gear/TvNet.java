package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** What a television shows, sent to the players near it as a video recorder plays a film into it. */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class TvNet {
    /** No tape playing a film: the recorder's blue screen. */
    public static final int BLUE = -1;
    /** A film still being read: snow. */
    public static final int LOADING = -2;
    /** How far a screen is seen from (blocks). */
    public static final double SEEN = 40;

    private TvNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(Frame.TYPE, Frame.CODEC, (p, c) -> com.waylays.eraphones.gear.client.TvClient.frame(p));
    }

    /** Shows {@code index} (a frame squeezed in {@code data}, or {@link #BLUE} / {@link #LOADING}) on the set at {@code tv}. */
    public static void show(ServerLevel level, BlockPos tv, int index, byte[] data) {
        PacketDistributor.sendToPlayersNear(level, null, tv.getX() + .5, tv.getY() + .5, tv.getZ() + .5, SEEN, new Frame(tv, index, data == null ? new byte[0] : data));
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        Video.close();
    }

    /** A picture for the television at {@code pos}. */
    public record Frame(BlockPos pos, int index, byte[] data) implements CustomPacketPayload {
        public static final Type<Frame> TYPE = new Type<>(EraPhones.id("tv_frame"));
        public static final StreamCodec<FriendlyByteBuf, Frame> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBlockPos(p.pos);
            b.writeVarInt(p.index);
            b.writeByteArray(p.data);
        }, b -> new Frame(b.readBlockPos(), b.readVarInt(), b.readByteArray(1 << 20)));

        @Override
        public Type<Frame> type() {
            return TYPE;
        }
    }
}
