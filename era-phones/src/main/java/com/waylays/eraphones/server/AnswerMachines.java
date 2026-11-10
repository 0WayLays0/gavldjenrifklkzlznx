package com.waylays.eraphones.server;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.block.AnswerMachineBlockEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The answering machines on the phone network, by the number each is linked to (while their chunk is loaded), and
 * the messages being left on them: the caller's voice (with Simple Voice Chat) and what they typed.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class AnswerMachines {
    /** The longest message a machine takes (seconds). */
    public static final int LONGEST = 60;
    private static final Map<String, GlobalPos> MACHINES = new ConcurrentHashMap<>();
    private static final Map<UUID, Recording> RECORDING = new ConcurrentHashMap<>();

    private AnswerMachines() {}

    static final class Recording {
        final GlobalPos where;
        final String from;
        final long at;
        short[] pcm = new short[48000];
        int length;
        final StringBuilder text = new StringBuilder();

        Recording(GlobalPos where, String from, long at) {
            this.where = where;
            this.from = from;
            this.at = at;
        }
    }

    public static void put(String number, GlobalPos where) {
        MACHINES.put(number, where);
    }

    public static void remove(String number, GlobalPos where) {
        MACHINES.remove(number, where);
    }

    /** The machine linked to {@code number}, if one is there and loaded. */
    @Nullable
    static AnswerMachineBlockEntity at(MinecraftServer server, String number) {
        GlobalPos g = MACHINES.get(number);
        if (g == null) {
            return null;
        }
        ServerLevel level = server.getLevel(g.dimension());
        if (level == null || !level.isLoaded(g.pos()) || !(level.getBlockEntity(g.pos()) instanceof AnswerMachineBlockEntity m)
                || !number.equals(m.number()) || com.waylays.eraphones.gear.Power.cut(level, g.pos())) {
            return null;
        }
        return m;
    }

    static void begin(UUID caller, GlobalPos where, String from) {
        RECORDING.put(caller, new Recording(where, from, System.currentTimeMillis()));
    }

    /** Whether {@code caller}'s voice is being taken down by a machine. */
    public static boolean recording(UUID caller) {
        return RECORDING.containsKey(caller);
    }

    /** A piece of {@code caller}'s voice (48 kHz) for the machine. */
    public static void voice(UUID caller, short[] pcm) {
        Recording r = RECORDING.get(caller);
        if (r == null || pcm == null) {
            return;
        }
        synchronized (r) {
            int max = LONGEST * 48000;
            int n = Math.min(pcm.length, max - r.length);
            if (n <= 0) {
                return;
            }
            if (r.length + n > r.pcm.length) {
                r.pcm = Arrays.copyOf(r.pcm, Math.min(max, Math.max(r.length + n, r.pcm.length * 2)));
            }
            System.arraycopy(pcm, 0, r.pcm, r.length, n);
            r.length += n;
        }
    }

    static void text(UUID caller, String line) {
        Recording r = RECORDING.get(caller);
        if (r != null && r.text.length() < 2000) {
            r.text.append(r.text.isEmpty() ? "" : " / ").append(line);
        }
    }

    /** The message is over: it is kept on its machine. */
    static void finish(MinecraftServer server, UUID caller) {
        Recording r = RECORDING.remove(caller);
        if (r == null) {
            return;
        }
        ServerLevel level = server.getLevel(r.where.dimension());
        if (level != null && level.getBlockEntity(r.where.pos()) instanceof AnswerMachineBlockEntity m) {
            short[] pcm;
            synchronized (r) {
                pcm = Arrays.copyOf(r.pcm, r.length);
            }
            m.keep(r.from, r.at, pcm, r.text.toString());
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        MACHINES.clear();
        RECORDING.clear();
    }
}
