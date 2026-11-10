package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** A photograph taken: the picture comes from its taker's screen, the print is made by the server. */
public final class PhotoNet {
    /** The picture: its side (pixels), and the most a picture may weigh squeezed (under a packet's 32 KiB). */
    public static final int SIDE = 112, MAX = 30000;

    private PhotoNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToServer(Taken.TYPE, Taken.CODEC, PhotoNet::taken);
    }

    /** What became of the last picture the server was sent (for the tests). */
    public static volatile String last = "none";

    private static void taken(Taken t, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p) || t.data().length == 0 || t.data().length > MAX) {
            last = "refused: " + t.data().length + " bytes";
            return;
        }
        InteractionHand hand = t.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack camera = p.getItemInHand(hand);
        if (!(camera.getItem() instanceof CameraItem) || CameraItem.film(camera) <= 0) {
            last = "refused: " + camera + " in hand, film " + (camera.getItem() instanceof CameraItem ? CameraItem.film(camera) : -1);
            return;
        }
        last = "printed";
        if (!p.getAbilities().instabuild) {
            camera.set(GearRegistry.FILM.get(), CameraItem.film(camera) - 1);
        }
        ItemStack photo = new ItemStack(GearRegistry.PHOTOGRAPH.get());
        photo.set(GearRegistry.PHOTO.get(), t.data());
        photo.set(GearRegistry.PHOTO_TIME.get(), p.level().getGameTime());
        photo.set(GearRegistry.PHOTO_BY.get(), com.waylays.eraphones.rp.RpService.name(p));
        if (!p.getInventory().add(photo)) {
            p.drop(photo, false);
        }
        p.level().playSound(null, p.blockPosition(), GearRegistry.SHUTTER.get(), SoundSource.PLAYERS, .8f, 1f);
    }

    /** From a player's screen: the picture their camera took, squeezed. */
    public record Taken(boolean offhand, byte[] data) implements CustomPacketPayload {
        public static final Type<Taken> TYPE = new Type<>(EraPhones.id("photo_taken"));
        public static final StreamCodec<FriendlyByteBuf, Taken> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBoolean(p.offhand);
            b.writeByteArray(p.data);
        }, b -> new Taken(b.readBoolean(), b.readByteArray(MAX)));

        @Override
        public Type<Taken> type() {
            return TYPE;
        }
    }
}
