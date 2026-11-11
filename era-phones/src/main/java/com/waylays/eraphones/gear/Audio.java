package com.waylays.eraphones.gear;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Sound played out of things in the world (a video recorder, a radio) through Simple Voice Chat, which the voice
 * package puts here when it is on the server ({@link #get()} is null without it). Sound is 48 000 samples a second,
 * one channel, in frames of 960.
 */
public interface Audio {
    int RATE = 48000, FRAME = 960;

    /** The decoded sound of an MP3 file, as said above; null if it cannot be read. */
    @Nullable
    short[] decodeMp3(Path file);

    /** Starts a sound coming out of {@code pos}, heard {@code distance} blocks round; {@code frames} gives each frame, null to end. */
    Object play(ServerLevel level, Vec3 pos, float distance, Supplier<short[]> frames);

    /** Starts a sound heard by {@code listener} alone, as in headphones; null if it cannot be (no voice chat for them). */
    @Nullable
    Object playTo(net.minecraft.server.level.ServerPlayer listener, Supplier<short[]> frames);

    void stop(Object handle);

    boolean playing(Object handle);

    static Audio get() {
        return Holder.audio;
    }

    static void set(@Nullable Audio audio) {
        Holder.audio = audio;
    }

    final class Holder {
        private static volatile Audio audio;

        private Holder() {}
    }
}
