package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.block.BoothBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.server.CallService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The phone booth between the server and its user: picking up the handset (the booth's screen opens; a ringing booth
 * is answered), and dialling a number from it (free).
 */
public final class BoothNet {
    /** How far from the booth its user may be (blocks). */
    private static final double REACH = 4.5;

    private BoothNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.CODEC, (p, c) -> c.enqueueWork(() -> com.waylays.eraphones.gear.client.BoothScreen.open(p)));
        r.playToServer(Dial.TYPE, Dial.CODEC, (p, c) -> c.enqueueWork(() -> dial(p, c)));
    }

    /** {@code p} picks up the handset of {@code booth}: answers it if it rings, and the payphone is shown. */
    public static void use(ServerPlayer p, BoothBlockEntity booth) {
        if (booth.number().isEmpty()) {
            return;
        }
        boolean answered = CallService.boothRinging(booth.number()) && CallService.answerBooth(p, booth.number());
        p.level().playSound(null, booth.getBlockPos().above(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, .35f, 1.9f);
        PacketDistributor.sendToPlayer(p, new Open(booth.getBlockPos(), booth.number(), answered));
    }

    private static void dial(Dial d, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p) || p.distanceToSqr(d.pos().getCenter()) > REACH * REACH
                || !(p.level().getBlockEntity(d.pos()) instanceof BoothBlockEntity booth) || booth.number().isEmpty()) {
            return;
        }
        if (Power.cut(p.level(), d.pos())) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("booth.eraphones.dead"), true);
            return;
        }
        p.level().playSound(null, d.pos().above(), GearRegistry.BOOTH_KEY.get(), SoundSource.BLOCKS, .9f, 1f);
        CallService.dialFromBooth(p, booth.number(), d.number());
    }

    /** To the booth's user: its payphone, to be shown; {@code answered} if picking it up answered a call. */
    public record Open(BlockPos pos, String number, boolean answered) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(EraPhones.id("booth_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.number, 32);
            b.writeBoolean(p.answered);
        }, b -> new Open(b.readBlockPos(), b.readUtf(32), b.readBoolean()));

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /** From the booth's user: the number dialled. */
    public record Dial(BlockPos pos, String number) implements CustomPacketPayload {
        public static final Type<Dial> TYPE = new Type<>(EraPhones.id("booth_dial"));
        public static final StreamCodec<FriendlyByteBuf, Dial> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.number, 32);
        }, b -> new Dial(b.readBlockPos(), b.readUtf(32)));

        @Override
        public Type<Dial> type() {
            return TYPE;
        }
    }
}
