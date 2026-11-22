package com.waylays.cinematic.studio;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Pose;

/** One tick of a recorded performance. */
public final class Frame {
    public static final int GROUND = 1, SPRINT = 2, SWING_OFFHAND = 4, USING = 8, USE_OFFHAND = 16, FLYING = 32;
    /** Equipment slots in {@link net.minecraft.world.entity.EquipmentSlot#values()} order. */
    public static final int SLOTS = 6;

    public double x, y, z;
    public float yaw, pitch, headYaw, bodyYaw;
    public byte pose;
    public int flags;
    /** Arm swing progress, 0..255 for 0..1. */
    public int attack;
    public int useTicks;
    public int emote;
    public final int[] equip = new int[SLOTS];

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public Pose mcPose() {
        return switch (pose) {
            case 1 -> Pose.CROUCHING;
            case 2 -> Pose.SWIMMING;
            case 3 -> Pose.FALL_FLYING;
            case 4 -> Pose.SLEEPING;
            case 5 -> Pose.SPIN_ATTACK;
            default -> Pose.STANDING;
        };
    }

    public static byte poseId(Pose p) {
        return switch (p) {
            case CROUCHING -> 1;
            case SWIMMING -> 2;
            case FALL_FLYING -> 3;
            case SLEEPING -> 4;
            case SPIN_ATTACK -> 5;
            default -> 0;
        };
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(headYaw);
        buf.writeFloat(bodyYaw);
        buf.writeByte(pose);
        buf.writeVarInt(flags);
        buf.writeByte(attack);
        buf.writeVarInt(useTicks);
        buf.writeVarInt(emote);
        for (int e : equip) buf.writeVarInt(e);
    }

    public static Frame read(FriendlyByteBuf buf) {
        Frame f = new Frame();
        f.x = buf.readDouble();
        f.y = buf.readDouble();
        f.z = buf.readDouble();
        f.yaw = buf.readFloat();
        f.pitch = buf.readFloat();
        f.headYaw = buf.readFloat();
        f.bodyYaw = buf.readFloat();
        f.pose = buf.readByte();
        f.flags = buf.readVarInt();
        f.attack = buf.readUnsignedByte();
        f.useTicks = buf.readVarInt();
        f.emote = buf.readVarInt();
        for (int i = 0; i < SLOTS; i++) f.equip[i] = buf.readVarInt();
        return f;
    }
}
