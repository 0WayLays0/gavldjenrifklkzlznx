package com.waylays.eraphones.server;

import com.waylays.eraphones.Era;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The phone booths in the world, by their numbers ("555-9xxx"): a booth's number is a line like a phone's, it can call
 * any phone and be called; it rings out loud in the street, and whoever picks up its handset answers.
 */
public final class Booths {
    private static final Map<String, GlobalPos> BOOTHS = new ConcurrentHashMap<>();
    /** How far from its booth a caller may stand (blocks). */
    public static final double REACH = 3.5;

    private Booths() {}

    public static boolean is(String number) {
        return number != null && BOOTHS.containsKey(number);
    }

    public static void put(String number, GlobalPos where) {
        BOOTHS.put(number, where);
    }

    public static void remove(String number, GlobalPos where) {
        BOOTHS.remove(number, where);
    }

    @Nullable
    public static GlobalPos where(String number) {
        return BOOTHS.get(number);
    }

    /** A new number for a booth, its line made. */
    public static String newNumber(MinecraftServer server) {
        LineData d = LineData.get(server);
        Random r = new Random();
        for (int k = 0; k < 2000; k++) {
            String n = "555-9" + String.format("%03d", r.nextInt(1000));
            if (d.line(n) == null) {
                d.lineOrMake(n, Era.GSM);
                return n;
            }
        }
        return "555-9999";
    }

    /** Whether {@code p} stands at the booth of {@code number}. */
    public static boolean near(@Nullable ServerPlayer p, String number) {
        GlobalPos g = BOOTHS.get(number);
        return p != null && g != null && p.level().dimension() == g.dimension() && p.position().distanceTo(BlockPos_center(g.pos())) <= REACH;
    }

    private static net.minecraft.world.phys.Vec3 BlockPos_center(BlockPos p) {
        return net.minecraft.world.phys.Vec3.atBottomCenterOf(p).add(0, 1, 0);
    }

    /** The booth's bell, rung out loud: two short rings every two seconds. */
    static void ring(MinecraftServer server, String number, int ticks) {
        GlobalPos g = BOOTHS.get(number);
        if (g == null) {
            return;
        }
        ServerLevel level = server.getLevel(g.dimension());
        int t = ticks % 40;
        if (level != null && (t == 0 || t == 6)) {
            level.playSound(null, g.pos().above(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.2f, 1.6f);
            level.playSound(null, g.pos().above(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, .8f, 1.9f);
        }
    }
}
