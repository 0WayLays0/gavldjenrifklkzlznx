package com.waylays.arcanes.phone;

import com.mojang.authlib.GameProfile;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.PhonePackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of the Arcanophone: it keeps the messages (delivering them when their receiver is back), the
 * contacts, the Echoes feed and the emerald accounts, and passes the shared positions on to the contacts of whoever
 * shares theirs.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class PhoneServer {
    /** The least time between two messages or posts of one player (ms). */
    private static final long SPAM = 400;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private PhoneServer() {}

    private static long now() {
        return System.currentTimeMillis();
    }

    private static PhoneData data(ServerPlayer p) {
        PhoneData d = PhoneData.get(p.server);
        String old = d.names.put(p.getUUID(), p.getGameProfile().getName());
        if (!p.getGameProfile().getName().equals(old)) {
            d.setDirty();
        }
        return d;
    }

    /** The player of that name: online, seen before by the phones, or known to the server. */
    @Nullable
    static UUID resolve(MinecraftServer server, PhoneData d, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            d.names.put(online.getUUID(), online.getGameProfile().getName());
            return online.getUUID();
        }
        for (Map.Entry<UUID, String> e : d.names.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) {
                return e.getKey();
            }
        }
        if (server.getProfileCache() != null) {
            GameProfile profile = server.getProfileCache().get(name).orElse(null);
            if (profile != null) {
                d.names.put(profile.getId(), profile.getName());
                return profile.getId();
            }
        }
        return null;
    }

    private static boolean spam(ServerPlayer p) {
        long t = now();
        Long last = LAST.get(p.getUUID());
        if (last != null && t - last < SPAM) {
            return true;
        }
        LAST.put(p.getUUID(), t);
        return false;
    }

    private static void info(ServerPlayer p, String key, String arg) {
        PacketDistributor.sendToPlayer(p, new PhonePackets.Notice("info", arg, key));
    }

    // ---------------------------------------------------------------- what the phone asks for

    public static void ask(ServerPlayer p, PhonePackets.Ask a) {
        PhoneData d = data(p);
        switch (a.what()) {
            case 0 -> {
                sendInbox(p, d);
                sendContacts(p, d);
                sendFeed(p, d);
                sendAccount(p, d);
                sendWeather(p);
            }
            case 1 -> {
                UUID other = resolve(p.server, d, a.arg());
                if (other != null) {
                    d.read(p.getUUID(), other);
                    sendThread(p, d, other);
                    sendInbox(p, d);
                }
            }
            case 2 -> sendWeather(p);
            case 3, 4 -> {
                if (a.what() == 3) {
                    d.sharing.add(p.getUUID());
                } else {
                    d.sharing.remove(p.getUUID());
                }
                d.setDirty();
                sendContacts(p, d);
            }
            default -> {
            }
        }
    }

    public static void send(ServerPlayer p, PhonePackets.Send s) {
        String text = s.text().strip();
        if (text.isEmpty() || spam(p)) {
            return;
        }
        PhoneData d = data(p);
        UUID to = resolve(p.server, d, s.to());
        if (to == null) {
            info(p, "phone.arcanes_oublies.unknown_player", s.to());
            return;
        }
        if (to.equals(p.getUUID())) {
            return;
        }
        d.add(p.getUUID(), to, text, now());
        sendThread(p, d, to);
        sendInbox(p, d);
        ServerPlayer receiver = p.server.getPlayerList().getPlayer(to);
        if (receiver != null) {
            PacketDistributor.sendToPlayer(receiver, new PhonePackets.Notice("message", p.getGameProfile().getName(), text));
            sendInbox(receiver, d);
            sendThread(receiver, d, p.getUUID());
        }
    }

    public static void contact(ServerPlayer p, PhonePackets.Contact c) {
        PhoneData d = data(p);
        UUID who = resolve(p.server, d, c.name());
        if (who == null) {
            info(p, "phone.arcanes_oublies.unknown_player", c.name());
            return;
        }
        if (who.equals(p.getUUID())) {
            return;
        }
        if (c.add()) {
            d.contacts.computeIfAbsent(p.getUUID(), k -> new java.util.LinkedHashSet<>()).add(who);
        } else {
            d.contacts(p.getUUID()).remove(who);
        }
        d.setDirty();
        sendContacts(p, d);
    }

    public static void echo(ServerPlayer p, PhonePackets.Echo e) {
        PhoneData d = data(p);
        if (e.like() >= 0) {
            for (PhoneData.Post post : d.posts) {
                if (post.id() == e.like()) {
                    if (!post.likes().remove(p.getUUID())) {
                        post.likes().add(p.getUUID());
                    }
                    d.setDirty();
                }
            }
            sendFeed(p, d);
            return;
        }
        String text = e.text().strip();
        if (text.isEmpty() || spam(p)) {
            return;
        }
        d.posts.add(0, new PhoneData.Post(d.nextPost++, p.getUUID(), text, now(), new java.util.HashSet<>()));
        while (d.posts.size() > PhoneData.MAX_POSTS) {
            d.posts.remove(d.posts.size() - 1);
        }
        d.setDirty();
        for (ServerPlayer other : p.server.getPlayerList().getPlayers()) {
            sendFeed(other, d);
            if (other != p) {
                PacketDistributor.sendToPlayer(other, new PhonePackets.Notice("echo", p.getGameProfile().getName(), text));
            }
        }
    }

    public static void bank(ServerPlayer p, PhonePackets.Bank b) {
        PhoneData d = data(p);
        int amount = Math.max(0, Math.min(b.amount(), 64 * 36));
        if (amount == 0) {
            return;
        }
        UUID me = p.getUUID();
        switch (b.action()) {
            case 0 -> {
                // Deposit: emeralds taken from the inventory
                int taken = 0;
                Inventory inv = p.getInventory();
                for (int i = 0; i < inv.getContainerSize() && taken < amount; i++) {
                    ItemStack st = inv.getItem(i);
                    if (st.is(Items.EMERALD)) {
                        int n = Math.min(st.getCount(), amount - taken);
                        st.shrink(n);
                        taken += n;
                    }
                }
                if (taken == 0) {
                    info(p, "phone.arcanes_oublies.no_emeralds", "");
                } else {
                    d.credit(me, taken, "deposit", "", now());
                }
            }
            case 1 -> {
                int n = (int) Math.min(amount, d.balance(me));
                if (n <= 0) {
                    info(p, "phone.arcanes_oublies.not_enough", "");
                } else {
                    d.credit(me, -n, "withdraw", "", now());
                    int left = n;
                    while (left > 0) {
                        int k = Math.min(64, left);
                        ItemStack st = new ItemStack(Items.EMERALD, k);
                        if (!p.getInventory().add(st)) {
                            p.drop(st, false);
                        }
                        left -= k;
                    }
                }
            }
            case 2 -> {
                UUID to = resolve(p.server, d, b.to());
                if (to == null || to.equals(me)) {
                    info(p, "phone.arcanes_oublies.unknown_player", b.to());
                } else if (d.balance(me) < amount) {
                    info(p, "phone.arcanes_oublies.not_enough", "");
                } else {
                    d.credit(me, -amount, "sent", d.name(to), now());
                    d.credit(to, amount, "received", p.getGameProfile().getName(), now());
                    ServerPlayer receiver = p.server.getPlayerList().getPlayer(to);
                    if (receiver != null) {
                        PacketDistributor.sendToPlayer(receiver, new PhonePackets.Notice("bank", p.getGameProfile().getName(), String.valueOf(amount)));
                        sendAccount(receiver, d);
                    }
                }
            }
            default -> {
            }
        }
        sendAccount(p, d);
    }

    // ---------------------------------------------------------------- answers

    static void sendInbox(ServerPlayer p, PhoneData d) {
        List<PhonePackets.Conv> convs = new ArrayList<>();
        for (UUID other : d.partners(p.getUUID())) {
            List<PhoneData.Msg> t = d.thread(p.getUUID(), other);
            PhoneData.Msg last = t.get(t.size() - 1);
            convs.add(new PhonePackets.Conv(d.name(other), other, cut(last.text(), 120), last.from().equals(p.getUUID()), last.time(),
                    d.unread(p.getUUID(), other), p.server.getPlayerList().getPlayer(other) != null));
            if (convs.size() >= 50) {
                break;
            }
        }
        PacketDistributor.sendToPlayer(p, new PhonePackets.Inbox(convs));
    }

    static void sendThread(ServerPlayer p, PhoneData d, UUID other) {
        List<PhonePackets.Line> lines = new ArrayList<>();
        List<PhoneData.Msg> t = d.thread(p.getUUID(), other);
        for (int i = Math.max(0, t.size() - 80); i < t.size(); i++) {
            PhoneData.Msg m = t.get(i);
            lines.add(new PhonePackets.Line(m.from().equals(p.getUUID()), m.text(), m.time()));
        }
        PacketDistributor.sendToPlayer(p, new PhonePackets.Thread(d.name(other), other, lines));
    }

    static void sendContacts(ServerPlayer p, PhoneData d) {
        List<PhonePackets.Person> contacts = new ArrayList<>();
        for (UUID c : d.contacts(p.getUUID())) {
            contacts.add(new PhonePackets.Person(d.name(c), c, p.server.getPlayerList().getPlayer(c) != null,
                    d.sharing.contains(c) && d.contacts(c).contains(p.getUUID())));
        }
        List<PhonePackets.Person> online = new ArrayList<>();
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o != p) {
                online.add(new PhonePackets.Person(o.getGameProfile().getName(), o.getUUID(), true, false));
            }
        }
        // Whether this player shares, as the first entry of the online list's flag is not used: sent as a contact to self
        contacts.add(0, new PhonePackets.Person(p.getGameProfile().getName(), p.getUUID(), true, d.sharing.contains(p.getUUID())));
        PacketDistributor.sendToPlayer(p, new PhonePackets.Contacts(contacts, online));
    }

    static void sendFeed(ServerPlayer p, PhoneData d) {
        List<PhonePackets.PostView> posts = new ArrayList<>();
        for (PhoneData.Post post : d.posts) {
            posts.add(new PhonePackets.PostView(post.id(), d.name(post.author()), post.author(), post.text(), post.time(), post.likes().size(),
                    post.likes().contains(p.getUUID())));
            if (posts.size() >= 40) {
                break;
            }
        }
        PacketDistributor.sendToPlayer(p, new PhonePackets.Feed(posts));
    }

    static void sendAccount(ServerPlayer p, PhoneData d) {
        List<PhonePackets.TxnView> h = new ArrayList<>();
        for (PhoneData.Txn t : d.history(p.getUUID())) {
            h.add(new PhonePackets.TxnView(t.kind(), t.other(), t.amount(), t.time()));
        }
        PacketDistributor.sendToPlayer(p, new PhonePackets.Account(d.balance(p.getUUID()), h));
    }

    static void sendWeather(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        var data = level.getServer().getWorldData().overworldData();
        PacketDistributor.sendToPlayer(p, new PhonePackets.Weather(data.getClearWeatherTime(), data.getRainTime(), data.getThunderTime(),
                level.isRaining(), level.isThundering()));
    }

    private static String cut(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PhoneData d = data(p);
            int unread = d.unreadTotal(p.getUUID());
            if (unread > 0) {
                sendInbox(p, d);
                PacketDistributor.sendToPlayer(p, new PhonePackets.Notice("info", String.valueOf(unread), "phone.arcanes_oublies.unread_waiting"));
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 40 != 0) {
            return;
        }
        PhoneData d = PhoneData.get(server);
        if (d.sharing.isEmpty()) {
            return;
        }
        // Each player is told where their sharing contacts are (those who also count them as a contact)
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            List<PhonePackets.Spot> spots = new ArrayList<>();
            for (UUID c : d.contacts(viewer.getUUID())) {
                if (!d.sharing.contains(c) || !d.contacts(c).contains(viewer.getUUID())) {
                    continue;
                }
                ServerPlayer o = server.getPlayerList().getPlayer(c);
                if (o != null) {
                    spots.add(new PhonePackets.Spot(o.getGameProfile().getName(), c, o.getX(), o.getZ(), o.level().dimension().location().toString()));
                }
            }
            if (!spots.isEmpty()) {
                PacketDistributor.sendToPlayer(viewer, new PhonePackets.Positions(spots));
            }
        }
    }
}
