package com.waylays.eraphones.server;

import com.waylays.eraphones.Era;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The rings, played where the phone is so that everyone near hears it: the 1985 brick's electronic trill, the 1997
 * handset's monophonic tune, the 2004 flip phone's polyphonic bells, the 2026 smartphone's marimba. A hidden caller's
 * ring comes out too low, as if the line were wrong. All tunes are original, made of note-block sounds.
 */
public final class Ringer {
    private record Note(int tick, Holder<SoundEvent> sound, int semitone, float volume) {}

    private record Pending(UUID player, int at, Holder<SoundEvent> sound, float pitch, float volume) {}

    private static final List<Pending> PENDING = new ArrayList<>();
    private static int clock;

    private static final int[] CYCLE = {50, 60, 72, 64};
    private static final List<List<Note>> RINGS = new ArrayList<>();

    static {
        // 1985: two bursts of trill
        List<Note> brick = new ArrayList<>();
        for (int k = 0; k < 10; k++) {
            brick.add(new Note(k * 2, SoundEvents.NOTE_BLOCK_BIT, k % 2 == 0 ? 20 : 16, .9f));
            brick.add(new Note(24 + k * 2, SoundEvents.NOTE_BLOCK_BIT, k % 2 == 0 ? 20 : 16, .9f));
        }
        RINGS.add(brick);
        // 1997: a little square-wave tune
        int[] gsm = {19, 17, 15, 12, 15, 17, 19, 19, 22};
        int[] gsmAt = {0, 3, 6, 9, 15, 18, 21, 24, 30};
        List<Note> g = new ArrayList<>();
        for (int k = 0; k < gsm.length; k++) {
            g.add(new Note(gsmAt[k], SoundEvents.NOTE_BLOCK_BIT, gsm[k], .8f));
        }
        RINGS.add(g);
        // 2004: bells over a bass
        int[] flip = {12, 16, 19, 16, 21, 19, 16, 12};
        List<Note> f = new ArrayList<>();
        for (int k = 0; k < flip.length; k++) {
            f.add(new Note(k * 4, SoundEvents.NOTE_BLOCK_BELL, flip[k], .7f));
        }
        f.add(new Note(0, SoundEvents.NOTE_BLOCK_HARP, 0, .6f));
        f.add(new Note(16, SoundEvents.NOTE_BLOCK_HARP, 5, .6f));
        f.add(new Note(32, SoundEvents.NOTE_BLOCK_GUITAR, 7, .5f));
        RINGS.add(f);
        // 2026: a marimba figure, a chime at its end
        int[] smart = {7, 11, 14, 19, 14, 11, 14, 19};
        List<Note> s = new ArrayList<>();
        for (int k = 0; k < smart.length; k++) {
            s.add(new Note(k * 3, SoundEvents.NOTE_BLOCK_XYLOPHONE, smart[k], .7f));
        }
        s.add(new Note(26, SoundEvents.NOTE_BLOCK_CHIME, 19, .6f));
        RINGS.add(s);
    }

    private Ringer() {}

    static float pitch(int semitone) {
        return (float) Math.pow(2, (semitone - 12) / 12.0);
    }

    private static void play(ServerPlayer p, Holder<SoundEvent> sound, float pitch, float volume) {
        p.level().playSound(null, p.getX(), p.getY() + 1, p.getZ(), sound.value(), SoundSource.PLAYERS, volume, Math.max(.5f, Math.min(2f, pitch)));
    }

    /** The ring for tick {@code t} of the call. */
    static void ring(ServerPlayer p, Era era, int t, boolean hidden) {
        int cycle = CYCLE[era.ordinal()];
        int at = t % cycle;
        for (Note n : RINGS.get(era.ordinal())) {
            if (n.tick == at) {
                float pitch = pitch(n.semitone);
                play(p, hidden ? SoundEvents.NOTE_BLOCK_BIT : n.sound, hidden ? pitch * .55f : pitch, n.volume);
            }
        }
        if (hidden && at == 0) {
            play(p, SoundEvents.NOTE_BLOCK_BASEDRUM, .5f, .4f);
        }
    }

    /** A message's chime: two beeps, three bells, two marimba notes; from a hidden number, low and wrong. */
    static void message(ServerPlayer p, Era era, boolean hidden) {
        float shift = hidden ? .55f : 1;
        switch (era) {
            case GSM -> {
                later(p, 0, SoundEvents.NOTE_BLOCK_BIT, pitch(22) * shift, .6f);
                later(p, 4, SoundEvents.NOTE_BLOCK_BIT, pitch(22) * shift, .6f);
            }
            case FLIP -> {
                later(p, 0, SoundEvents.NOTE_BLOCK_BELL, pitch(12) * shift, .6f);
                later(p, 3, SoundEvents.NOTE_BLOCK_BELL, pitch(16) * shift, .6f);
                later(p, 6, SoundEvents.NOTE_BLOCK_BELL, pitch(19) * shift, .6f);
            }
            case SMART -> {
                later(p, 0, SoundEvents.NOTE_BLOCK_CHIME, pitch(14) * shift, .5f);
                later(p, 4, SoundEvents.NOTE_BLOCK_CHIME, pitch(19) * shift, .5f);
            }
            default -> {
            }
        }
    }

    /** The low, broken buzz of a phone gone wrong. */
    static void haunt(ServerPlayer p, int t) {
        if (t % 8 == 0) {
            play(p, SoundEvents.NOTE_BLOCK_BIT, .5f + (t * 37 % 7) * .03f, .35f);
        }
        if (t % 30 == 0) {
            play(p, SoundEvents.NOTE_BLOCK_DIDGERIDOO, .5f, .5f);
        }
    }

    private static void later(ServerPlayer p, int delay, Holder<SoundEvent> sound, float pitch, float volume) {
        PENDING.add(new Pending(p.getUUID(), clock + delay, sound, pitch, volume));
    }

    static void tick(net.minecraft.server.MinecraftServer server) {
        clock++;
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending n = it.next();
            if (n.at > clock) {
                continue;
            }
            it.remove();
            ServerPlayer p = server.getPlayerList().getPlayer(n.player);
            if (p != null) {
                play(p, n.sound, n.pitch, n.volume);
            }
        }
    }
}
