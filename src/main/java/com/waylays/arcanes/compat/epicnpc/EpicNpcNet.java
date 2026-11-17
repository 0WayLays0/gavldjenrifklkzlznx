package com.waylays.arcanes.compat.epicnpc;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The "Epic Fight combat: yes / no" switch of each NPC, set from Custom NPCs' edit screen (the NPC wand). An NPC
 * switched off carries the entity tag {@link EpicNpc#CLASSIC_TAG} (kept with the NPC in the world, and also settable
 * with {@code /tag}); the players who see it are told, so that their game draws it as Custom NPCs does.
 */
public final class EpicNpcNet {
    /** What each NPC's players were last told (true: plain Custom NPCs combat). */
    private static final Map<Entity, Boolean> TOLD = new WeakHashMap<>();

    private EpicNpcNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(Mode.TYPE, Mode.CODEC, (p, c) -> c.enqueueWork(() -> {
            Player player = c.player();
            Entity e = player == null ? null : player.level().getEntity(p.entity());
            if (e != null) {
                setClassic(e, !p.epicFight());
            }
        }));
        r.playToServer(Switch.TYPE, Switch.CODEC, (p, c) -> c.enqueueWork(() -> onSwitch(p, c)));
    }

    /** Switches the NPC to plain Custom NPCs combat ({@code classic}) or to Epic Fight's. */
    public static void setClassic(Entity npc, boolean classic) {
        if (classic) {
            npc.addTag(EpicNpc.CLASSIC_TAG);
        } else {
            npc.removeTag(EpicNpc.CLASSIC_TAG);
        }
    }

    private static void onSwitch(Switch p, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer player)) {
            return;
        }
        Entity npc = player.serverLevel().getEntity(p.entity());
        // Who may edit an NPC with the wand: a game master (operator) or a player in creative mode, close to it
        if (!EpicNpc.isNpc(npc) || !(player.hasPermissions(2) || player.isCreative()) || player.distanceToSqr(npc) > 64 * 64) {
            return;
        }
        setClassic(npc, !p.epicFight());
        tell(npc);
        ArcanesOublies.LOGGER.info("{} switched the NPC {} to {}", player.getGameProfile().getName(), npc.getName().getString(),
                p.epicFight() ? "Epic Fight combat" : "Custom NPCs' plain combat");
    }

    private static void tell(Entity npc) {
        boolean classic = npc.getTags().contains(EpicNpc.CLASSIC_TAG);
        TOLD.put(npc, classic);
        PacketDistributor.sendToPlayersTrackingEntity(npc, new Mode(npc.getId(), !classic));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity npc = event.getTarget();
        if (event.getEntity() instanceof ServerPlayer player && EpicNpc.isNpc(npc) && npc.getTags().contains(EpicNpc.CLASSIC_TAG)) {
            PacketDistributor.sendToPlayer(player, new Mode(npc.getId(), false));
        }
    }

    /** Catches a switch made with {@code /tag}, once a second. */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity npc = event.getEntity();
        if (npc.level().isClientSide() || npc.tickCount % 20 != 0 || !EpicNpc.isNpc(npc)) {
            return;
        }
        boolean classic = npc.getTags().contains(EpicNpc.CLASSIC_TAG);
        Boolean told = TOLD.get(npc);
        if (told == null ? classic : told != classic) {
            tell(npc);
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payload(String name) {
        return new CustomPacketPayload.Type<>(ArcanesOublies.id("epicnpc_" + name));
    }

    /** Server to client: whether the NPC {@code entity} fights with Epic Fight. */
    public record Mode(int entity, boolean epicFight) implements CustomPacketPayload {
        public static final Type<Mode> TYPE = payload("mode");
        public static final StreamCodec<FriendlyByteBuf, Mode> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.entity);
            b.writeBoolean(p.epicFight);
        }, b -> new Mode(b.readVarInt(), b.readBoolean()));

        @Override
        public Type<Mode> type() {
            return TYPE;
        }
    }

    /** Client to server: the switch of the NPC {@code entity} was set in its edit screen. */
    public record Switch(int entity, boolean epicFight) implements CustomPacketPayload {
        public static final Type<Switch> TYPE = payload("switch");
        public static final StreamCodec<FriendlyByteBuf, Switch> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.entity);
            b.writeBoolean(p.epicFight);
        }, b -> new Switch(b.readVarInt(), b.readBoolean()));

        @Override
        public Type<Switch> type() {
            return TYPE;
        }
    }
}
