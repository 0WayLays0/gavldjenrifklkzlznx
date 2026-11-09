package com.waylays.eraphones.server;

import com.waylays.eraphones.Era;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.item.PhoneItem;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Calls between numbers. Dialling rings the phone of that number wherever it is carried (for everyone near it to
 * hear, unless it is silent); answered, the two ends speak (each line typed reaches the other end) until one hangs
 * up, a phone goes flat or out of reach. Game masters can call from a hidden number with lines said by themselves.
 */
public final class CallService {
    public static final int OVER = 0, CALLING = 1, RINGING = 2, TALKING = 3;
    /** How long a phone rings before the call is missed (ticks). */
    static final int RING_TICKS = 20 * 25;
    /** Between two lines of a hidden caller's script (ticks). */
    static final int SCRIPT_GAP = 20 * 3;

    static final class Call {
        final String from, to;
        @Nullable final UUID caller;
        @Nullable UUID callee;
        final Era callerEra;
        Era calleeEra;
        boolean talking;
        long since;
        int ticks;
        final List<String> script;
        int scriptAt;

        Call(String from, String to, @Nullable UUID caller, Era callerEra, Era calleeEra, List<String> script) {
            this.from = from;
            this.to = to;
            this.caller = caller;
            this.callerEra = callerEra;
            this.calleeEra = calleeEra;
            this.script = script;
        }

        boolean hidden() {
            return Numbers.hidden(from);
        }
    }

    static final List<Call> CALLS = new ArrayList<>();

    private CallService() {}

    @Nullable
    static Call of(String number) {
        for (Call c : CALLS) {
            if (c.from.equals(number) || c.to.equals(number)) {
                return c;
            }
        }
        return null;
    }

    @Nullable
    static Call of(UUID player) {
        for (Call c : CALLS) {
            if (player.equals(c.caller) || player.equals(c.callee)) {
                return c;
            }
        }
        return null;
    }

    public static boolean ringing(String number) {
        Call c = of(number);
        return c != null && !c.talking && c.to.equals(number);
    }

    private static void state(@Nullable ServerPlayer p, int state, String mine, String number, String display, long since, String reason) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new PhoneNet.CallState(state, mine, number, display, since, reason));
        }
    }

    // ---------------------------------------------------------------- the phone's requests

    public static void dial(ServerPlayer p, String raw) {
        PhoneService.Session s = PhoneService.active(p);
        if (s == null || of(p.getUUID()) != null || of(s.number) != null) {
            return;
        }
        String to = Numbers.normalize(raw);
        LineData d = LineData.get(p.server);
        if (to == null) {
            state(p, OVER, s.number, raw, raw, 0, "bad_number");
            return;
        }
        if (PhoneService.bars(p, s.era) <= 0) {
            state(p, OVER, s.number, to, d.display(s.number, to), 0, "no_network");
            return;
        }
        if (to.equals(s.number)) {
            state(p, OVER, s.number, to, to, 0, "busy");
            return;
        }
        LineData.Line target = d.line(to);
        if (target == null) {
            state(p, OVER, s.number, to, to, 0, "unassigned");
            return;
        }
        if (of(to) != null) {
            state(p, OVER, s.number, to, d.display(s.number, to), 0, "busy");
            return;
        }
        if (Booths.is(to)) {
            Call c = new Call(s.number, to, p.getUUID(), s.era, Era.GSM, List.of());
            CALLS.add(c);
            state(p, CALLING, s.number, to, d.display(s.number, to), 0, "");
            return;
        }
        ServerPlayer callee = PhoneService.holder(p.server, to);
        if (callee == null || PhoneService.bars(callee, target.era) <= 0) {
            d.logCall(s.number, new LineData.Call(to, 0, PhoneService.now(), 0));
            d.logCall(to, new LineData.Call(s.number, 2, PhoneService.now(), 0));
            state(p, OVER, s.number, to, d.display(s.number, to), 0, "unreachable");
            PhoneService.sendCalls(p, d, s.number);
            return;
        }
        Call c = new Call(s.number, to, p.getUUID(), s.era, target.era, List.of());
        c.callee = callee.getUUID();
        CALLS.add(c);
        state(p, CALLING, s.number, to, d.display(s.number, to), 0, "");
        state(callee, RINGING, to, s.number, d.display(to, s.number), 0, "");
    }

    /** A game master's call from a hidden number: it rings, and once answered says {@code script}, line by line. */
    public static boolean hidden(MinecraftServer server, String to, List<String> script) {
        LineData d = LineData.get(server);
        LineData.Line target = d.line(to);
        if (target == null || of(to) != null) {
            return false;
        }
        if (Booths.is(to)) {
            CALLS.add(new Call(Numbers.HIDDEN, to, null, Era.GSM, Era.GSM, new ArrayList<>(script)));
            return true;
        }
        ServerPlayer callee = PhoneService.holder(server, to);
        if (callee == null) {
            return false;
        }
        Call c = new Call(Numbers.HIDDEN, to, null, target.era, target.era, new ArrayList<>(script));
        c.callee = callee.getUUID();
        CALLS.add(c);
        state(callee, RINGING, to, Numbers.HIDDEN, "", 0, "");
        return true;
    }

    /** {@code p}, at the booth of {@code booth}, dials {@code raw} (the coin already taken). */
    public static boolean dialFromBooth(ServerPlayer p, String booth, String raw) {
        if (of(p.getUUID()) != null || of(booth) != null) {
            return false;
        }
        String to = Numbers.normalize(raw);
        LineData d = LineData.get(p.server);
        if (to == null) {
            state(p, OVER, booth, raw, raw, 0, "bad_number");
            return false;
        }
        LineData.Line target = d.line(to);
        if (to.equals(booth) || target == null) {
            state(p, OVER, booth, to, to, 0, to.equals(booth) ? "busy" : "unassigned");
            return false;
        }
        if (of(to) != null) {
            state(p, OVER, booth, to, d.display(booth, to), 0, "busy");
            return false;
        }
        Call c;
        if (Booths.is(to)) {
            c = new Call(booth, to, p.getUUID(), Era.GSM, Era.GSM, List.of());
        } else {
            ServerPlayer callee = PhoneService.holder(p.server, to);
            if (callee == null || PhoneService.bars(callee, target.era) <= 0) {
                state(p, OVER, booth, to, d.display(booth, to), 0, "unreachable");
                return false;
            }
            c = new Call(booth, to, p.getUUID(), Era.GSM, target.era, List.of());
            c.callee = callee.getUUID();
            state(callee, RINGING, to, booth, d.display(to, booth), 0, "");
        }
        CALLS.add(c);
        state(p, CALLING, booth, to, d.display(booth, to), 0, "");
        return true;
    }

    /** {@code p} picks up the handset of the booth {@code booth}: answers it if it rings. */
    public static boolean answerBooth(ServerPlayer p, String booth) {
        Call c = of(booth);
        if (c == null || c.talking || !c.to.equals(booth) || of(p.getUUID()) != null) {
            return false;
        }
        c.callee = p.getUUID();
        c.talking = true;
        c.since = PhoneService.now();
        c.ticks = 0;
        LineData d = LineData.get(p.server);
        ServerPlayer caller = c.caller == null ? null : p.server.getPlayerList().getPlayer(c.caller);
        state(caller, TALKING, c.from, c.to, d.display(c.from, c.to), c.since, "");
        state(p, TALKING, c.to, c.from, d.display(c.to, c.from), c.since, "");
        return true;
    }

    /** Whether the booth {@code booth} is ringing. */
    public static boolean boothRinging(String booth) {
        Call c = of(booth);
        return c != null && !c.talking && c.to.equals(booth);
    }

    public static void answer(ServerPlayer p, boolean accept) {
        Call c = of(p.getUUID());
        if (c == null || c.talking || !p.getUUID().equals(c.callee) || PhoneService.find(p, c.to).isEmpty()) {
            return;
        }
        if (!accept) {
            end(p.server, c, "declined");
            return;
        }
        c.talking = true;
        c.since = PhoneService.now();
        c.ticks = 0;
        LineData d = LineData.get(p.server);
        ServerPlayer caller = c.caller == null ? null : p.server.getPlayerList().getPlayer(c.caller);
        state(caller, TALKING, c.from, c.to, d.display(c.from, c.to), c.since, "");
        state(p, TALKING, c.to, c.from, d.display(c.to, c.from), c.since, "");
    }

    public static void hangUp(ServerPlayer p) {
        Call c = of(p.getUUID());
        if (c != null) {
            end(p.server, c, "ended");
        }
    }

    /** A line said into the phone: those around hear it said, the other end hears it through the phone. */
    public static void speak(ServerPlayer p, String raw) {
        Call c = of(p.getUUID());
        String text = raw.strip();
        if (c == null || !c.talking || text.isEmpty()) {
            return;
        }
        com.waylays.eraphones.rp.RpService.say(p, text.length() > 256 ? text.substring(0, 256) : text);
    }

    /** Passes what {@code p} just said to the other end of their call, if they are talking on one. */
    public static void relay(ServerPlayer p, String text) {
        Call c = of(p.getUUID());
        if (c == null || !c.talking) {
            return;
        }
        UUID other = p.getUUID().equals(c.caller) ? c.callee : c.caller;
        PacketDistributor.sendToPlayer(p, new PhoneNet.CallLine(true, text));
        if (other != null) {
            ServerPlayer o = p.server.getPlayerList().getPlayer(other);
            if (o != null) {
                PacketDistributor.sendToPlayer(o, new PhoneNet.CallLine(false, text));
            }
        }
    }

    /** Tells a phone just opened where its call stands. */
    static void resend(ServerPlayer p) {
        Call c = of(p.getUUID());
        if (c == null) {
            return;
        }
        LineData d = LineData.get(p.server);
        if (p.getUUID().equals(c.caller)) {
            state(p, c.talking ? TALKING : CALLING, c.from, c.to, d.display(c.from, c.to), c.since, "");
        } else {
            state(p, c.talking ? TALKING : RINGING, c.to, c.from, d.display(c.to, c.from), c.since, "");
        }
    }

    // ---------------------------------------------------------------- ending, ticking

    static void end(MinecraftServer server, Call c, String reason) {
        if (!CALLS.remove(c)) {
            return;
        }
        LineData d = LineData.get(server);
        long now = PhoneService.now();
        int seconds = c.talking ? (int) ((now - c.since) / 1000) : 0;
        if (!c.hidden()) {
            d.logCall(c.from, new LineData.Call(c.to, 0, now, seconds));
        }
        int kind = c.talking ? 1 : "declined".equals(reason) ? 3 : 2;
        d.logCall(c.to, new LineData.Call(c.from, kind, now, seconds));
        ServerPlayer caller = c.caller == null ? null : server.getPlayerList().getPlayer(c.caller);
        ServerPlayer callee = c.callee == null ? null : server.getPlayerList().getPlayer(c.callee);
        String callerReason = reason.equals("declined") ? "busy" : reason;
        state(caller, OVER, c.from, c.to, d.display(c.from, c.to), seconds, callerReason);
        state(callee, OVER, c.to, c.from, d.display(c.to, c.from), seconds, reason);
        if (caller != null) {
            PhoneService.sendCalls(caller, d, c.from);
        }
        if (callee != null) {
            PhoneService.sendCalls(callee, d, c.to);
            if (kind == 2) {
                PacketDistributor.sendToPlayer(callee, new PhoneNet.Notice("missed", d.display(c.to, c.from), "", c.from));
            }
        }
    }

    /** Whether {@code p} still holds a working phone of {@code number} in reach; else why not. */
    @Nullable
    private static String trouble(@Nullable ServerPlayer p, String number, Era era) {
        if (p == null) {
            return "lost";
        }
        if (Booths.is(number)) {
            return Booths.near(p, number) ? null : "lost";
        }
        ItemStack st = PhoneService.find(p, number);
        if (st.isEmpty()) {
            return "lost";
        }
        if (PhoneItem.battery(st) <= 0) {
            return "battery";
        }
        if (PhoneService.bars(p, era) <= 0) {
            return "lost";
        }
        return null;
    }

    public static void tick(MinecraftServer server) {
        if (CALLS.isEmpty()) {
            return;
        }
        LineData d = LineData.get(server);
        Iterator<Call> it = new ArrayList<>(CALLS).iterator();
        while (it.hasNext()) {
            Call c = it.next();
            c.ticks++;
            ServerPlayer caller = c.caller == null ? null : server.getPlayerList().getPlayer(c.caller);
            ServerPlayer callee = c.callee == null ? null : server.getPlayerList().getPlayer(c.callee);
            if (!c.talking) {
                LineData.Line target = d.line(c.to);
                if (Booths.is(c.to)) {
                    Booths.ring(server, c.to, c.ticks);
                } else if (callee != null && target != null && !target.silent) {
                    Ringer.ring(callee, c.calleeEra, c.ticks, c.hidden());
                }
                if (c.ticks > RING_TICKS) {
                    end(server, c, "no_answer");
                    continue;
                }
                if (Booths.is(c.from) && !Booths.near(caller, c.from)) {
                    end(server, c, "ended");
                    continue;
                }
                if (Booths.is(c.to)) {
                    if (c.caller != null && caller == null) {
                        end(server, c, "ended");
                    }
                    continue;
                }
                if (callee == null || PhoneService.find(callee, c.to).isEmpty() || c.caller != null && caller == null) {
                    end(server, c, c.caller != null && caller == null ? "ended" : "no_answer");
                    continue;
                }
                continue;
            }
            // Talking: a hidden caller's lines, one at a time, then it hangs up
            if (c.hidden() && c.ticks % SCRIPT_GAP == 0) {
                if (c.scriptAt < c.script.size()) {
                    if (callee != null) {
                        PacketDistributor.sendToPlayer(callee, new PhoneNet.CallLine(false, c.script.get(c.scriptAt)));
                    }
                    c.scriptAt++;
                } else {
                    end(server, c, "ended");
                    continue;
                }
            }
            if (c.ticks % 20 == 0) {
                String t = c.caller == null ? null : trouble(caller, c.from, c.callerEra);
                if (t == null) {
                    t = trouble(callee, c.to, c.calleeEra);
                }
                if (t != null) {
                    end(server, c, t);
                }
            }
        }
    }

    /** The player at the other end of {@code player}'s call, while they are talking; null otherwise. */
    @Nullable
    public static UUID otherEnd(UUID player) {
        Call c = of(player);
        if (c == null || !c.talking) {
            return null;
        }
        return player.equals(c.caller) ? c.callee : c.caller;
    }

    /** The kind of phone {@code player} speaks into on the call they are talking on; null when not talking. */
    @Nullable
    public static Era voiceEra(UUID player) {
        Call c = of(player);
        if (c == null || !c.talking) {
            return null;
        }
        return player.equals(c.caller) ? c.callerEra : c.calleeEra;
    }

    /** Who is on a call (its phone drains faster), by number. */
    static boolean talking(String number) {
        Call c = of(number);
        return c != null && c.talking;
    }

    static void left(MinecraftServer server, UUID player) {
        Call c = of(player);
        if (c != null) {
            end(server, c, "lost");
        }
    }
}
