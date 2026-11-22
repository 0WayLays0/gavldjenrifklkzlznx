package com.waylays.cinematic.net;

import com.waylays.cinematic.studio.StudioServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** A director's order (client to server). */
public record ActionMsg(Action action, String scene, UUID actor, String text, int number) {
    private static final UUID NONE = new UUID(0, 0);

    public enum Action {
        CREATE_SCENE, DELETE_SCENE, CREATE_ACTOR, DELETE_ACTOR, DUPLICATE_ACTOR, RENAME_ACTOR, SET_SLIM, SET_SKIN, MOVE_HERE,
        CLEAR_TAKE, RECORD, PLAY, STOP
    }

    public ActionMsg(FriendlyByteBuf buf) {
        this(buf.readEnum(Action.class), buf.readUtf(64), readId(buf), buf.readUtf(256), buf.readVarInt());
    }

    private static UUID readId(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        return NONE.equals(id) ? null : id;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(scene == null ? "" : scene, 64);
        buf.writeUUID(actor == null ? NONE : actor);
        buf.writeUtf(text == null ? "" : text, 256);
        buf.writeVarInt(number);
    }

    public static void handle(ActionMsg msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getSender() != null) StudioServer.INSTANCE.onAction(ctx.get().getSender(), msg);
        ctx.get().setPacketHandled(true);
    }
}
