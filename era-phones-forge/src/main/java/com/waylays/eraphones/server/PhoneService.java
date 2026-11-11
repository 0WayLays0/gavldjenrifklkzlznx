package com.waylays.eraphones.server;

import com.waylays.eraphones.Era;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.Signal;
import com.waylays.eraphones.item.PhoneItem;
import com.waylays.eraphones.network.PhoneNet;
import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.waylays.eraphones.compat.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the phones ask of the server. A phone opened starts a session (which phone, by its slot and number); every
 * later request acts for that number, and only while the phone is still carried and has battery. Messages wait on
 * the server for a phone that is off, away or out of reach.
 */
public final class PhoneService {
    /** The off hand, as a slot. */
    public static final int OFFHAND = 40;
    /** The least time between two messages or posts from one phone (ms). */
    private static final long SPAM = 400;
    private static final Map<String, Long> LAST = new HashMap<>();

    /** The phone a player has open (or last opened). */
    public static final class Session {
        int slot;
        public final String number;
        public final Era era;
        boolean open = true;

        Session(int slot, String number, Era era) {
            this.slot = slot;
            this.number = number;
            this.era = era;
        }
    }

    static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private PhoneService() {}

    static long now() {
        return System.currentTimeMillis();
    }

    // ---------------------------------------------------------------- finding phones

    public static ItemStack stackAt(ServerPlayer p, int slot) {
        if (slot == OFFHAND) {
            return p.getOffhandItem();
        }
        Inventory inv = p.getInventory();
        return slot >= 0 && slot < inv.items.size() ? inv.items.get(slot) : ItemStack.EMPTY;
    }

    /** The phone of that number {@code p} carries (anywhere in the inventory), or empty. */
    public static ItemStack find(ServerPlayer p, String number) {
        if (number.equals(PhoneItem.number(p.getOffhandItem()))) {
            return p.getOffhandItem();
        }
        for (ItemStack st : p.getInventory().items) {
            if (st.getItem() instanceof PhoneItem && number.equals(PhoneItem.number(st))) {
                return st;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The phones {@code p} carries, by number. */
    public static Map<String, ItemStack> carried(ServerPlayer p) {
        Map<String, ItemStack> out = new HashMap<>();
        for (ItemStack st : p.getInventory().items) {
            String n = PhoneItem.number(st);
            if (n != null) {
                out.put(n, st);
            }
        }
        String n = PhoneItem.number(p.getOffhandItem());
        if (n != null) {
            out.put(n, p.getOffhandItem());
        }
        return out;
    }

    /** Who carries the phone of that number, if it is on (battery) — online only. */
    @Nullable
    public static ServerPlayer holder(MinecraftServer server, String number) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ItemStack st = find(p, number);
            if (!st.isEmpty() && PhoneItem.battery(st) > 0) {
                return p;
            }
        }
        return null;
    }

    public static int bars(ServerPlayer p, Era era) {
        return Signal.bars(p.level(), p.blockPosition(), era);
    }

    /** {@code p}'s session, checked: the phone still carried (followed if it moved) and not flat; else null. */
    @Nullable
    public static Session active(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) {
            return null;
        }
        ItemStack st = stackAt(p, s.slot);
        if (!(st.getItem() instanceof PhoneItem) || !s.number.equals(PhoneItem.number(st))) {
            st = find(p, s.number);
            if (st.isEmpty()) {
                SESSIONS.remove(p.getUUID());
                return null;
            }
            s.slot = st == p.getOffhandItem() ? OFFHAND : p.getInventory().items.indexOf(st);
        }
        if (PhoneItem.battery(st) <= 0) {
            return null;
        }
        return s;
    }

    @Nullable
    static Session session(UUID id) {
        return SESSIONS.get(id);
    }

    private static boolean spam(String number) {
        long t = now();
        Long last = LAST.get(number);
        if (last != null && t - last < SPAM) {
            return true;
        }
        LAST.put(number, t);
        return false;
    }

    static void info(ServerPlayer p, String key, String arg) {
        PacketDistributor.sendToPlayer(p, new PhoneNet.Notice("info", arg, key, ""));
    }

    // ---------------------------------------------------------------- requests

    public static void open(ServerPlayer p, int slot) {
        ItemStack st = stackAt(p, slot);
        if (!(st.getItem() instanceof PhoneItem phone)) {
            return;
        }
        String number = PhoneItem.ensure(st, p.serverLevel());
        LineData d = LineData.get(p.server);
        LineData.Line line = d.lineOrMake(number, phone.era);
        Session s = new Session(slot, number, phone.era);
        SESSIONS.put(p.getUUID(), s);
        PacketDistributor.sendToPlayer(p, new PhoneNet.Hello(number, phone.era.ordinal(), line.handle, line.sharing, line.silent));
        if (PhoneItem.battery(st) <= 0) {
            return;
        }
        sendContacts(p, d, number);
        sendCalls(p, d, number);
        if (phone.era.sms) {
            sendInbox(p, d, number);
        }
        if (phone.era == Era.SMART) {
            sendFeed(p, d, number);
            sendWeather(p);
        }
        if (phone.era == Era.SMART || phone.era == Era.FLIP) {
            sendNews(p, d);
        }
        CallService.resend(p);
    }

    public static void close(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null) {
            s.open = false;
        }
    }

    public static boolean isOpen(UUID player) {
        Session s = SESSIONS.get(player);
        return s != null && s.open;
    }

    public static void ask(ServerPlayer p, int what, String arg) {
        Session s = active(p);
        if (s == null) {
            return;
        }
        LineData d = LineData.get(p.server);
        LineData.Line line = d.lineOrMake(s.number, s.era);
        switch (what) {
            case 1 -> {
                String other = Numbers.hidden(arg) ? arg : Numbers.normalize(arg);
                if (other != null) {
                    d.read(s.number, other);
                    sendThread(p, d, s.number, other);
                    sendInbox(p, d, s.number);
                }
            }
            case 2 -> sendWeather(p);
            case 3, 4 -> {
                line.sharing = what == 3;
                d.setDirty();
                hello(p, s, line);
            }
            case 5 -> sendFeed(p, d, s.number);
            case 6 -> sendNews(p, d);
            case 7, 8 -> {
                line.silent = what == 7;
                d.setDirty();
                hello(p, s, line);
            }
            case 9 -> sendCalls(p, d, s.number);
            case 10 -> sendContacts(p, d, s.number);
            case 11 -> sendInbox(p, d, s.number);
            default -> {
            }
        }
    }

    private static void hello(ServerPlayer p, Session s, LineData.Line line) {
        PacketDistributor.sendToPlayer(p, new PhoneNet.Hello(s.number, s.era.ordinal(), line.handle, line.sharing, line.silent));
    }

    public static void sms(ServerPlayer p, String rawTo, String rawText) {
        Session s = active(p);
        if (s == null || !s.era.sms) {
            return;
        }
        String text = rawText.strip();
        if (s.era == Era.GSM && text.length() > PhoneNet.SMS_GSM) {
            text = text.substring(0, PhoneNet.SMS_GSM);
        }
        String to = Numbers.normalize(rawTo);
        if (text.isEmpty() || spam(s.number)) {
            return;
        }
        if (to == null) {
            info(p, "info.eraphones.bad_number", rawTo);
            return;
        }
        if (bars(p, s.era) <= 0) {
            info(p, "info.eraphones.no_network", "");
            return;
        }
        LineData d = LineData.get(p.server);
        if (!d.exists(to)) {
            info(p, "info.eraphones.unassigned", to);
            return;
        }
        if (to.equals(s.number)) {
            return;
        }
        deliver(p.server, d, s.number, to, text);
        sendThread(p, d, s.number, to);
        sendInbox(p, d, s.number);
    }

    /** Stores a message from {@code from} to {@code to} and tells the phone of {@code to} if it is on and in reach. */
    public static void deliver(MinecraftServer server, LineData d, String from, String to, String text) {
        d.add(from, to, text, now());
        LineData.Line target = d.line(to);
        ServerPlayer receiver = holder(server, to);
        if (receiver == null || target == null) {
            return;
        }
        if (bars(receiver, target.era) <= 0 || !target.era.sms) {
            return;
        }
        PacketDistributor.sendToPlayer(receiver, new PhoneNet.Notice("sms", d.display(to, from), text, from));
        if (!target.silent) {
            Ringer.message(receiver, target.era, Numbers.hidden(from));
        }
        Session rs = SESSIONS.get(receiver.getUUID());
        if (rs != null && rs.number.equals(to)) {
            sendInbox(receiver, d, to);
        }
    }

    public static void contact(ServerPlayer p, String rawName, String rawNumber, boolean remove) {
        Session s = active(p);
        if (s == null) {
            return;
        }
        String number = Numbers.normalize(rawNumber);
        String name = rawName.strip();
        if (number == null) {
            info(p, "info.eraphones.bad_number", rawNumber);
            return;
        }
        LineData d = LineData.get(p.server);
        LineData.Line line = d.lineOrMake(s.number, s.era);
        line.contacts.removeIf(c -> c.number().equals(number) || !c.name().isEmpty() && c.name().equals(name) && name.startsWith("#"));
        if (!remove) {
            if (name.isEmpty()) {
                return;
            }
            if (line.contacts.size() >= LineData.MAX_CONTACTS) {
                info(p, "info.eraphones.directory_full", "");
                return;
            }
            line.contacts.add(new LineData.Contact(name.length() > 24 ? name.substring(0, 24) : name, number));
            line.contacts.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        }
        d.setDirty();
        sendContacts(p, d, s.number);
        sendInbox(p, d, s.number);
        sendCalls(p, d, s.number);
    }

    public static void post(ServerPlayer p, String rawText, int like) {
        Session s = active(p);
        if (s == null || s.era != Era.SMART) {
            return;
        }
        LineData d = LineData.get(p.server);
        LineData.Line line = d.lineOrMake(s.number, s.era);
        if (like >= 0) {
            for (LineData.Post post : d.posts()) {
                if (post.id() == like) {
                    if (!post.likes().remove(s.number)) {
                        post.likes().add(s.number);
                    }
                    d.setDirty();
                }
            }
            sendFeed(p, d, s.number);
            return;
        }
        String text = rawText.strip();
        if (text.isEmpty() || line.handle.isEmpty() || spam(s.number)) {
            return;
        }
        if (bars(p, s.era) <= 0) {
            info(p, "info.eraphones.no_network", "");
            return;
        }
        d.post(s.number, line.handle, text, now());
        for (ServerPlayer other : p.server.getPlayerList().getPlayers()) {
            Session os = SESSIONS.get(other.getUUID());
            if (os != null && os.era == Era.SMART && os.open) {
                sendFeed(other, d, os.number);
            }
            if (other != p) {
                for (Map.Entry<String, ItemStack> e : carried(other).entrySet()) {
                    if (PhoneItem.era(e.getValue()) == Era.SMART && PhoneItem.battery(e.getValue()) > 0) {
                        PacketDistributor.sendToPlayer(other, new PhoneNet.Notice("post", "@" + line.handle, text, e.getKey()));
                        break;
                    }
                }
            }
        }
    }

    public static void handle(ServerPlayer p, String raw) {
        Session s = active(p);
        if (s == null) {
            return;
        }
        String h = raw.strip().replace("@", "").replace(' ', '_');
        h = h.replaceAll("[^\\p{L}\\p{N}_.\\-]", "");
        if (h.length() < 2 || h.length() > 20) {
            info(p, "info.eraphones.bad_handle", "");
            return;
        }
        LineData d = LineData.get(p.server);
        for (LineData.Line other : d.lines.values()) {
            if (!other.number.equals(s.number) && other.handle.equalsIgnoreCase(h)) {
                info(p, "info.eraphones.handle_taken", h);
                return;
            }
        }
        LineData.Line line = d.lineOrMake(s.number, s.era);
        line.handle = h;
        d.setDirty();
        hello(p, s, line);
        sendFeed(p, d, s.number);
    }

    // ---------------------------------------------------------------- answers

    static void sendInbox(ServerPlayer p, LineData d, String me) {
        List<PhoneNet.Conv> convs = new ArrayList<>();
        for (String other : d.partners(me)) {
            List<LineData.Msg> t = d.thread(me, other);
            LineData.Msg last = t.get(t.size() - 1);
            convs.add(new PhoneNet.Conv(other, d.display(me, other), cut(last.text(), 120), last.from().equals(me), last.time(), d.unread(me, other)));
            if (convs.size() >= 50) {
                break;
            }
        }
        PacketDistributor.sendToPlayer(p, new PhoneNet.Inbox(convs));
    }

    static void sendThread(ServerPlayer p, LineData d, String me, String other) {
        List<PhoneNet.Line> lines = new ArrayList<>();
        List<LineData.Msg> t = d.thread(me, other);
        for (int i = Math.max(0, t.size() - 80); i < t.size(); i++) {
            LineData.Msg m = t.get(i);
            lines.add(new PhoneNet.Line(m.from().equals(me), m.text(), m.time()));
        }
        PacketDistributor.sendToPlayer(p, new PhoneNet.Thread(other, d.display(me, other), lines));
    }

    static void sendContacts(ServerPlayer p, LineData d, String me) {
        LineData.Line line = d.line(me);
        List<PhoneNet.ContactView> out = new ArrayList<>();
        if (line != null) {
            for (LineData.Contact c : line.contacts) {
                LineData.Line other = d.line(c.number());
                boolean shares = other != null && other.sharing && other.contact(me) != null;
                out.add(new PhoneNet.ContactView(c.name(), c.number(), shares));
            }
        }
        PacketDistributor.sendToPlayer(p, new PhoneNet.Contacts(out));
    }

    static void sendCalls(ServerPlayer p, LineData d, String me) {
        LineData.Line line = d.line(me);
        List<PhoneNet.CallView> out = new ArrayList<>();
        if (line != null) {
            for (LineData.Call c : line.calls) {
                out.add(new PhoneNet.CallView(c.other(), d.display(me, c.other()), c.kind(), c.time(), c.seconds()));
            }
        }
        PacketDistributor.sendToPlayer(p, new PhoneNet.CallLog(out));
    }

    static void sendFeed(ServerPlayer p, LineData d, String me) {
        List<PhoneNet.PostView> posts = new ArrayList<>();
        for (LineData.Post post : d.posts()) {
            posts.add(new PhoneNet.PostView(post.id(), post.handle(), post.text(), post.time(), post.likes().size(), post.likes().contains(me),
                    post.author().equals(me)));
            if (posts.size() >= 40) {
                break;
            }
        }
        PacketDistributor.sendToPlayer(p, new PhoneNet.Feed(posts));
    }

    static void sendNews(ServerPlayer p, LineData d) {
        List<PhoneNet.NewsView> out = new ArrayList<>();
        for (LineData.News n : d.news()) {
            out.add(new PhoneNet.NewsView(n.title(), n.text(), n.time()));
        }
        PacketDistributor.sendToPlayer(p, new PhoneNet.NewsList(out));
    }

    static void sendWeather(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        var data = level.getServer().getWorldData().overworldData();
        PacketDistributor.sendToPlayer(p, new PhoneNet.Weather(data.getClearWeatherTime(), data.getRainTime(), data.getThunderTime(),
                level.isRaining(), level.isThundering()));
    }

    static String cut(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    /** Forgets everything about a player who left (their calls end). */
    static void forget(UUID id) {
        SESSIONS.remove(id);
    }

    /** Makes sure {@code stack} counts as a phone of the registry (for code outside the item). */
    static boolean isPhone(ItemStack stack) {
        return stack.getItem() instanceof PhoneItem && EraRegistry.NUMBER.get().get(stack) != null;
    }
}
