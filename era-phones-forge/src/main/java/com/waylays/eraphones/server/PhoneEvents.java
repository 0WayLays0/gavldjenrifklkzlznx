package com.waylays.eraphones.server;

import com.waylays.eraphones.Era;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.item.PhoneItem;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import com.waylays.eraphones.compat.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The phones' clock on the server: calls ring and run, batteries drain (screen on, calls), phones a game master
 * troubles buzz, and the smartphones that are open learn where the contacts sharing their position are.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class PhoneEvents {
    /** Fractions of battery owed, by number (drained a thousandth at a time). */
    private static final Map<String, Float> OWED = new HashMap<>();
    /** Phones being haunted, by number: ticks left. */
    static final Map<String, Integer> HAUNTED = new HashMap<>();

    private PhoneEvents() {}

    private static void drain(ServerPlayer p, String number, float amount) {
        ItemStack st = PhoneService.find(p, number);
        if (st.isEmpty()) {
            return;
        }
        float owed = OWED.getOrDefault(number, 0f) + amount;
        int whole = (int) owed;
        OWED.put(number, owed - whole);
        if (whole > 0) {
            int before = PhoneItem.battery(st);
            PhoneItem.setBattery(st, before - whole);
            if (before > 50 && before - whole <= 50) {
                PhoneService.info(p, "info.eraphones.battery_low", "");
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        CallService.tick(server);
        Ringer.tick(server);
        hauntTick(server);
        int t = server.getTickCount();
        if (t % 20 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                PhoneService.Session s = PhoneService.session(p.getUUID());
                if (s != null && s.open) {
                    drain(p, s.number, s.era.screenDrain);
                }
            }
            for (CallService.Call c : new ArrayList<>(CallService.CALLS)) {
                if (!c.talking) {
                    continue;
                }
                if (c.caller != null) {
                    ServerPlayer a = server.getPlayerList().getPlayer(c.caller);
                    if (a != null) {
                        drain(a, c.from, c.callerEra.callDrain);
                    }
                }
                if (c.callee != null) {
                    ServerPlayer b = server.getPlayerList().getPlayer(c.callee);
                    if (b != null) {
                        drain(b, c.to, c.calleeEra.callDrain);
                    }
                }
            }
        }
        if (t % 40 == 0) {
            positions(server);
        }
    }

    private static void hauntTick(MinecraftServer server) {
        if (HAUNTED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Integer>> it = HAUNTED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            int left = e.getValue() - 1;
            if (left <= 0) {
                it.remove();
                continue;
            }
            e.setValue(left);
            ServerPlayer p = PhoneService.holder(server, e.getKey());
            if (p != null) {
                Ringer.haunt(p, left);
            }
        }
    }

    /** Haunts the phone of {@code number} for {@code ticks}; false if no one carries it. */
    public static boolean haunt(MinecraftServer server, String number, int ticks, String text) {
        ServerPlayer p = PhoneService.holder(server, number);
        if (p == null) {
            return false;
        }
        HAUNTED.put(number, ticks);
        PacketDistributor.sendToPlayer(p, new PhoneNet.Haunt(ticks, text));
        return true;
    }

    private static void positions(MinecraftServer server) {
        LineData d = LineData.get(server);
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            PhoneService.Session s = PhoneService.session(viewer.getUUID());
            if (s == null || !s.open || s.era != Era.SMART) {
                continue;
            }
            LineData.Line me = d.line(s.number);
            if (me == null) {
                continue;
            }
            List<PhoneNet.Spot> spots = new ArrayList<>();
            for (LineData.Contact c : me.contacts) {
                LineData.Line other = d.line(c.number());
                if (other == null || !other.sharing || other.contact(s.number) == null) {
                    continue;
                }
                ServerPlayer o = PhoneService.holder(server, c.number());
                if (o != null && o != viewer && PhoneService.bars(o, other.era) > 0) {
                    spots.add(new PhoneNet.Spot(c.name(), c.number(), o.getX(), o.getZ(), o.level().dimension().location().toString()));
                }
            }
            PacketDistributor.sendToPlayer(viewer, new PhoneNet.Positions(spots));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        LineData d = LineData.get(p.server);
        for (Map.Entry<String, ItemStack> e : PhoneService.carried(p).entrySet()) {
            int unread = d.unreadTotal(e.getKey());
            if (unread > 0 && PhoneItem.battery(e.getValue()) > 0) {
                PacketDistributor.sendToPlayer(p, new PhoneNet.Notice("info", String.valueOf(unread), "info.eraphones.unread_waiting", e.getKey()));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            CallService.left(p.server, p.getUUID());
            PhoneService.forget(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        PhoneCommands.register(event.getDispatcher());
    }
}
