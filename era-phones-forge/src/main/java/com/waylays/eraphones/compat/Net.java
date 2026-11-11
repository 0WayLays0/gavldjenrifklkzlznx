package com.waylays.eraphones.compat;

import com.waylays.eraphones.EraPhones;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The mod's packets over one Forge channel: each packet goes in an envelope (its index, then its fields), handled on
 * the main thread by what was registered for it. The packets are registered in the same order on both sides.
 */
public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(EraPhones.MODID, "main"), () -> VERSION,
            VERSION::equals, VERSION::equals);

    @SuppressWarnings({"rawtypes"})
    record Entry(int index, CustomPacketPayload.Type type, StreamCodec codec, IPayloadHandler handler, boolean toServer) {}

    private static final List<Entry> BY_INDEX = new ArrayList<>();
    private static final Map<ResourceLocation, Entry> BY_ID = new HashMap<>();
    private static boolean opened;

    private Net() {}

    static <T extends CustomPacketPayload> void add(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                    IPayloadHandler<T> handler, boolean toServer) {
        if (BY_ID.containsKey(type.id())) {
            return;
        }
        Entry e = new Entry(BY_INDEX.size(), type, codec, handler, toServer);
        BY_INDEX.add(e);
        BY_ID.put(type.id(), e);
    }

    /** Opens the channel and has {@code registering} register every packet (once, at start). */
    public static void open(Consumer<PayloadRegistrar> registering) {
        if (opened) {
            return;
        }
        opened = true;
        registering.accept(new PayloadRegistrar());
        CHANNEL.messageBuilder(Envelope.class, 0).encoder(Envelope::encode).decoder(Envelope::decode).consumerMainThread(Envelope::handle).add();
    }

    public record Envelope(CustomPacketPayload payload) {
        @SuppressWarnings("unchecked")
        void encode(FriendlyByteBuf buf) {
            Entry e = BY_ID.get(payload.type().id());
            if (e == null) {
                throw new IllegalStateException("Unknown packet " + payload.type().id());
            }
            buf.writeVarInt(e.index());
            e.codec().encode(new RegistryFriendlyByteBuf(buf), payload);
        }

        static Envelope decode(FriendlyByteBuf buf) {
            int i = buf.readVarInt();
            if (i < 0 || i >= BY_INDEX.size()) {
                throw new io.netty.handler.codec.DecoderException("Unknown packet " + i);
            }
            return new Envelope((CustomPacketPayload) BY_INDEX.get(i).codec().decode(new RegistryFriendlyByteBuf(buf)));
        }

        @SuppressWarnings("unchecked")
        static void handle(Envelope env, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            Entry e = BY_ID.get(env.payload.type().id());
            if (e != null) {
                Player player = c.getDirection().getReceptionSide().isServer() ? c.getSender()
                        : DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientAccess::player);
                if (player != null || !c.getDirection().getReceptionSide().isServer()) {
                    e.handler().handle(env.payload, new IPayloadContext(player));
                }
            }
            c.setPacketHandled(true);
        }
    }
}
