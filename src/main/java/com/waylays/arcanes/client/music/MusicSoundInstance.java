package com.waylays.arcanes.client.music;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.CompletableFuture;

/**
 * Music played on a player: it follows them, and is heard up to {@code range} blocks from them, fading with distance
 * (range 0: heard by that player alone, as if in their ears). The sound engine is handed the music's own stream instead of
 * the sound's file. Played in the jukebox (records) volume of the game's sound options.
 */
public final class MusicSoundInstance extends AbstractTickableSoundInstance {
    /** The ranges the music can be heard to, each a sound event of its own (sounds.json), and 0 for "only me". */
    public static final int[] RANGES = {0, 8, 16, 24, 32, 48, 64};

    private final Entity source;
    private final MusicStream stream;
    private final boolean self;

    public MusicSoundInstance(Entity source, MusicStream stream, float volume, int range) {
        super(SoundEvent.createVariableRangeEvent(location(range)), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.source = source;
        this.stream = stream;
        this.self = range <= 0;
        this.volume = volume;
        this.pitch = 1;
        this.looping = false;
        this.delay = 0;
        if (self) {
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.x = this.y = this.z = 0;
        } else {
            this.relative = false;
            this.attenuation = Attenuation.LINEAR;
            follow();
        }
    }

    /** The nearest range there is a sound event for. */
    public static int snap(int range) {
        int best = RANGES[0];
        for (int r : RANGES) {
            if (Math.abs(r - range) < Math.abs(best - range)) {
                best = r;
            }
        }
        return best;
    }

    private static net.minecraft.resources.ResourceLocation location(int range) {
        int r = snap(range);
        return ArcanesOublies.id(r <= 0 ? "music_player.self" : "music_player.r" + r);
    }

    private void follow() {
        this.x = source.getX();
        this.y = source.getY() + source.getBbHeight() * .6;
        this.z = source.getZ();
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public MusicStream stream() {
        return stream;
    }

    public Entity source() {
        return source;
    }

    @Override
    public void tick() {
        if (source.isRemoved()) {
            stop();
            return;
        }
        if (!self) {
            follow();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return CompletableFuture.completedFuture(stream);
    }
}
