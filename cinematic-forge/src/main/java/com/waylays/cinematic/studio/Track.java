package com.waylays.cinematic.studio;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A take: the frames of a performance, the items it used, the blocks it changed and the pose edits made on it. */
public final class Track {
    private static final int FORMAT = 1;

    public final List<Frame> frames = new ArrayList<>();
    public final List<ItemStack> palette = new ArrayList<>();
    public final List<BlockChange> blocks = new ArrayList<>();
    public final List<PoseKey> poseKeys = new ArrayList<>();

    public Track() {
        palette.add(ItemStack.EMPTY);
    }

    public int length() {
        return frames.size();
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    /** The frame at {@code tick}, held at both ends. */
    public Frame frame(int tick) {
        return frames.get(Mth.clamp(tick, 0, frames.size() - 1));
    }

    public int itemIndex(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        for (int i = 1; i < palette.size(); i++) {
            if (ItemStack.matches(palette.get(i), stack)) return i;
        }
        palette.add(stack.copy());
        return palette.size() - 1;
    }

    public ItemStack item(int index) {
        return index > 0 && index < palette.size() ? palette.get(index) : ItemStack.EMPTY;
    }

    /** Eased offset (degrees) of a limb at a fractional tick; keys hold their value outside their range. */
    public boolean poseOffset(BodyPart part, float tick, float[] out) {
        return poseOffset(poseKeys, part, tick, out);
    }

    public static boolean poseOffset(List<PoseKey> keys, BodyPart part, float tick, float[] out) {
        PoseKey before = null, after = null;
        for (PoseKey k : keys) {
            if (k.part() != part) continue;
            if (k.tick() <= tick && (before == null || k.tick() > before.tick())) before = k;
            if (k.tick() >= tick && (after == null || k.tick() < after.tick())) after = k;
        }
        if (before == null && after == null) return false;
        if (before == null) before = after;
        if (after == null) after = before;
        float t = after.tick() == before.tick() ? 0f : (tick - before.tick()) / (after.tick() - before.tick());
        t = t * t * (3f - 2f * t);
        out[0] = Mth.lerp(t, before.x(), after.x());
        out[1] = Mth.lerp(t, before.y(), after.y());
        out[2] = Mth.lerp(t, before.z(), after.z());
        return true;
    }

    public void sortKeys() {
        poseKeys.sort(Comparator.comparingInt(PoseKey::tick).thenComparing(PoseKey::part));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(FORMAT);
        buf.writeVarInt(palette.size() - 1);
        for (int i = 1; i < palette.size(); i++) buf.writeItem(palette.get(i));
        buf.writeVarInt(frames.size());
        for (Frame f : frames) f.write(buf);
        buf.writeVarInt(blocks.size());
        for (BlockChange b : blocks) b.write(buf);
        buf.writeVarInt(poseKeys.size());
        for (PoseKey k : poseKeys) k.write(buf);
    }

    public static Track read(FriendlyByteBuf buf) {
        int format = buf.readVarInt();
        if (format != FORMAT) throw new IllegalArgumentException("Unknown take format " + format);
        Track t = new Track();
        int items = buf.readVarInt();
        for (int i = 0; i < items; i++) t.palette.add(buf.readItem());
        int frames = check(buf.readVarInt());
        for (int i = 0; i < frames; i++) {
            Frame f = Frame.read(buf);
            for (int s = 0; s < Frame.SLOTS; s++) {
                if (f.equip[s] < 0 || f.equip[s] >= t.palette.size()) f.equip[s] = 0;
            }
            t.frames.add(f);
        }
        int blocks = check(buf.readVarInt());
        for (int i = 0; i < blocks; i++) t.blocks.add(BlockChange.read(buf));
        int keys = check(buf.readVarInt());
        for (int i = 0; i < keys; i++) t.poseKeys.add(PoseKey.read(buf));
        return t;
    }

    private static int check(int n) {
        if (n < 0 || n > 2_000_000) throw new IllegalArgumentException("Bad take size " + n);
        return n;
    }

    public byte[] toBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write(buf);
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } finally {
            buf.release();
        }
    }

    public static Track fromBytes(byte[] bytes) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            return read(buf);
        } finally {
            buf.release();
        }
    }

    public Track copy() {
        return fromBytes(toBytes());
    }
}
