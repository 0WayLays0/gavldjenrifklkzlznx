package com.waylays.eraphones.event;

import com.waylays.eraphones.EraPhones;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.waylays.eraphones.compat.StreamCodec;
import com.waylays.eraphones.compat.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import com.waylays.eraphones.compat.IPayloadContext;
import com.waylays.eraphones.compat.PayloadRegistrar;

/**
 * The event's things between the server and the players: the code box's wheels (shown, tried, or set by a game
 * master), and where each bearer of the star fragment pushes it.
 */
public final class EventNet {
    private static final double REACH = 5;

    private EventNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(OpenCode.TYPE, OpenCode.CODEC, (p, c) -> c.enqueueWork(() -> com.waylays.eraphones.event.client.CodeScreen.open(p)));
        r.playToServer(TryCode.TYPE, TryCode.CODEC, (p, c) -> c.enqueueWork(() -> tryCode(p, c)));
        r.playToServer(Push.TYPE, Push.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) {
                StarFragment.push(sp, p.x(), p.z());
            }
        }));
    }

    private static void tryCode(TryCode t, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p) || p.distanceToSqr(t.pos().getCenter()) > REACH * REACH
                || !(p.level().getBlockEntity(t.pos()) instanceof CodeBoxBlockEntity box) || !CodeBoxBlockEntity.validCode(t.code())) {
            return;
        }
        if (t.set()) {
            // A game master setting the code on the wheels
            if (p.isCreative() && p.hasPermissions(2)) {
                box.setCode(t.code());
                p.displayClientMessage(Component.translatable("event.eraphones.codebox.code_set", t.code()).withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        box.attempt(p, t.code());
    }

    /** Shows the box's wheels: as many as its code has digits; to a game master in creative, its code as well. */
    public record OpenCode(BlockPos pos, int digits, String known) implements CustomPacketPayload {
        public static final Type<OpenCode> TYPE = new Type<>(EraPhones.id("code_open"));
        public static final StreamCodec<FriendlyByteBuf, OpenCode> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBlockPos(p.pos);
            b.writeVarInt(p.digits);
            b.writeUtf(p.known, 8);
        }, b -> new OpenCode(b.readBlockPos(), b.readVarInt(), b.readUtf(8)));

        @Override
        public Type<OpenCode> type() {
            return TYPE;
        }
    }

    /** The wheels turned to {@code code}: tried, or (set) made the box's code by a game master. */
    public record TryCode(BlockPos pos, String code, boolean set) implements CustomPacketPayload {
        public static final Type<TryCode> TYPE = new Type<>(EraPhones.id("code_try"));
        public static final StreamCodec<FriendlyByteBuf, TryCode> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.code, 8);
            b.writeBoolean(p.set);
        }, b -> new TryCode(b.readBlockPos(), b.readUtf(8), b.readBoolean()));

        @Override
        public Type<TryCode> type() {
            return TYPE;
        }
    }

    /** Where a bearer of the star fragment pushes it (a direction on the ground, length 0 to 1). */
    public record Push(float x, float z) implements CustomPacketPayload {
        public static final Type<Push> TYPE = new Type<>(EraPhones.id("fragment_push"));
        public static final StreamCodec<FriendlyByteBuf, Push> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeFloat(p.x);
            b.writeFloat(p.z);
        }, b -> new Push(b.readFloat(), b.readFloat()));

        @Override
        public Type<Push> type() {
            return TYPE;
        }
    }
}
