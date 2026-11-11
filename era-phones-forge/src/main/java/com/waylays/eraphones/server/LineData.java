package com.waylays.eraphones.server;

import com.waylays.eraphones.Era;
import com.waylays.eraphones.Numbers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the phones of a world keep, by number (so it follows the phone, whoever holds it): each line's directory,
 * call log, social-network name and settings; the messages between numbers (kept for whoever is away); the posts of
 * the social network; the town news the game masters publish.
 */
public class LineData extends SavedData {
    public static final int MAX_THREAD = 200, MAX_POSTS = 100, MAX_CALLS = 40, MAX_NEWS = 30, MAX_CONTACTS = 100;
    private static final String NAME = "eraphones_lines";

    /** A call in the log: {@code kind} 0 made, 1 received, 2 missed, 3 refused; {@code seconds} it lasted. */
    public record Call(String other, int kind, long time, int seconds) {}

    public record Contact(String name, String number) {}

    public record Msg(String from, String text, long time) {}

    public record Post(int id, String author, String handle, String text, long time, Set<String> likes) {}

    public record News(String title, String text, long time) {}

    /** One phone line: its era, its directory, its call log, its name on the social network, its settings. */
    public static final class Line {
        public final String number;
        public Era era;
        public String handle = "";
        public boolean sharing, silent;
        public final List<Contact> contacts = new ArrayList<>();
        public final List<Call> calls = new ArrayList<>();

        Line(String number, Era era) {
            this.number = number;
            this.era = era;
        }

        @Nullable
        public Contact contact(String number) {
            for (Contact c : contacts) {
                if (c.number().equals(number)) {
                    return c;
                }
            }
            return null;
        }
    }

    final Map<String, Line> lines = new HashMap<>();
    final Map<String, List<Msg>> threads = new HashMap<>();
    final Map<String, Map<String, Integer>> unread = new HashMap<>();
    final List<Post> posts = new ArrayList<>();
    final List<News> news = new ArrayList<>();
    int nextPost = 1;

    public static LineData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(LineData::load, LineData::new, NAME);
    }

    // ---------------------------------------------------------------- lines

    /** A new line for a phone of {@code era}: a number no other phone has. */
    public String newLine(Era era, RandomSource random) {
        String n;
        int tries = 0;
        do {
            int digits = 100 + random.nextInt(9900);
            if (tries++ > 2000) {
                digits = random.nextInt(10000);
            }
            n = "555-" + String.format("%04d", digits);
        } while (lines.containsKey(n) && tries < 20000);
        lines.put(n, new Line(n, era));
        setDirty();
        return n;
    }

    @Nullable
    public Line line(String number) {
        return number == null ? null : lines.get(number);
    }

    /** The line, made if the world lost it (a phone carried over from another world). */
    public Line lineOrMake(String number, Era era) {
        return lines.computeIfAbsent(number, k -> {
            setDirty();
            return new Line(k, era);
        });
    }

    public boolean exists(String number) {
        return lines.containsKey(number);
    }

    /** How {@code other} shows on {@code viewer}'s phone: the name it was saved under, or else the number ("" if hidden). */
    public String display(String viewer, String other) {
        if (Numbers.hidden(other)) {
            return "";
        }
        Line l = lines.get(viewer);
        Contact c = l == null ? null : l.contact(other);
        return c != null ? c.name() : other;
    }

    public void logCall(String number, Call call) {
        Line l = lines.get(number);
        if (l == null) {
            return;
        }
        l.calls.add(0, call);
        while (l.calls.size() > MAX_CALLS) {
            l.calls.remove(l.calls.size() - 1);
        }
        setDirty();
    }

    // ---------------------------------------------------------------- messages

    static String key(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public List<Msg> thread(String a, String b) {
        return threads.getOrDefault(key(a, b), List.of());
    }

    public void add(String from, String to, String text, long time) {
        List<Msg> t = threads.computeIfAbsent(key(from, to), k -> new ArrayList<>());
        t.add(new Msg(from, text, time));
        while (t.size() > MAX_THREAD) {
            t.remove(0);
        }
        unread.computeIfAbsent(to, k -> new HashMap<>()).merge(from, 1, Integer::sum);
        setDirty();
    }

    public void read(String reader, String other) {
        Map<String, Integer> m = unread.get(reader);
        if (m != null && m.remove(other) != null) {
            setDirty();
        }
    }

    public int unread(String reader, String other) {
        return unread.getOrDefault(reader, Map.of()).getOrDefault(other, 0);
    }

    public int unreadTotal(String reader) {
        return unread.getOrDefault(reader, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
    }

    /** The numbers {@code who} has written with, latest first. */
    public List<String> partners(String who) {
        Map<String, Long> last = new LinkedHashMap<>();
        for (Map.Entry<String, List<Msg>> e : threads.entrySet()) {
            String[] p = e.getKey().split("\\|");
            if (p.length != 2 || e.getValue().isEmpty() || !(p[0].equals(who) || p[1].equals(who))) {
                continue;
            }
            last.put(p[0].equals(who) ? p[1] : p[0], e.getValue().get(e.getValue().size() - 1).time());
        }
        List<String> out = new ArrayList<>(last.keySet());
        out.sort((x, y) -> Long.compare(last.get(y), last.get(x)));
        return out;
    }

    // ---------------------------------------------------------------- the social network, the news

    public void post(String author, String handle, String text, long time) {
        posts.add(0, new Post(nextPost++, author, handle, text, time, new HashSet<>()));
        while (posts.size() > MAX_POSTS) {
            posts.remove(posts.size() - 1);
        }
        setDirty();
    }

    public void news(String title, String text, long time) {
        news.add(0, new News(title, text, time));
        while (news.size() > MAX_NEWS) {
            news.remove(news.size() - 1);
        }
        setDirty();
    }

    public List<Post> posts() {
        return posts;
    }

    public List<News> news() {
        return news;
    }

    // ---------------------------------------------------------------- saving

    private static LineData load(CompoundTag tag) {
        LineData d = new LineData();
        CompoundTag li = tag.getCompound("Lines");
        for (String k : li.getAllKeys()) {
            CompoundTag c = li.getCompound(k);
            Line l = new Line(k, Era.byId(c.getInt("Era")));
            l.handle = c.getString("Handle");
            l.sharing = c.getBoolean("Sharing");
            l.silent = c.getBoolean("Silent");
            for (Tag t : c.getList("Contacts", Tag.TAG_COMPOUND)) {
                CompoundTag x = (CompoundTag) t;
                l.contacts.add(new Contact(x.getString("Name"), x.getString("Number")));
            }
            for (Tag t : c.getList("Calls", Tag.TAG_COMPOUND)) {
                CompoundTag x = (CompoundTag) t;
                l.calls.add(new Call(x.getString("Other"), x.getInt("Kind"), x.getLong("Time"), x.getInt("Seconds")));
            }
            d.lines.put(k, l);
        }
        CompoundTag th = tag.getCompound("Threads");
        for (String k : th.getAllKeys()) {
            List<Msg> list = new ArrayList<>();
            for (Tag t : th.getList(k, Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                list.add(new Msg(c.getString("From"), c.getString("Text"), c.getLong("Time")));
            }
            d.threads.put(k, list);
        }
        CompoundTag un = tag.getCompound("Unread");
        for (String k : un.getAllKeys()) {
            Map<String, Integer> m = new HashMap<>();
            CompoundTag c = un.getCompound(k);
            for (String o : c.getAllKeys()) {
                m.put(o, c.getInt(o));
            }
            d.unread.put(k, m);
        }
        for (Tag t : tag.getList("Posts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Set<String> likes = new HashSet<>();
            for (Tag l : c.getList("Likes", Tag.TAG_STRING)) {
                likes.add(l.getAsString());
            }
            d.posts.add(new Post(c.getInt("Id"), c.getString("Author"), c.getString("Handle"), c.getString("Text"), c.getLong("Time"), likes));
        }
        for (Tag t : tag.getList("News", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.news.add(new News(c.getString("Title"), c.getString("Text"), c.getLong("Time")));
        }
        d.nextPost = Math.max(1, tag.getInt("NextPost"));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag li = new CompoundTag();
        lines.forEach((k, l) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("Era", l.era.ordinal());
            c.putString("Handle", l.handle);
            c.putBoolean("Sharing", l.sharing);
            c.putBoolean("Silent", l.silent);
            ListTag cs = new ListTag();
            for (Contact x : l.contacts) {
                CompoundTag o = new CompoundTag();
                o.putString("Name", x.name());
                o.putString("Number", x.number());
                cs.add(o);
            }
            c.put("Contacts", cs);
            ListTag ca = new ListTag();
            for (Call x : l.calls) {
                CompoundTag o = new CompoundTag();
                o.putString("Other", x.other());
                o.putInt("Kind", x.kind());
                o.putLong("Time", x.time());
                o.putInt("Seconds", x.seconds());
                ca.add(o);
            }
            c.put("Calls", ca);
            li.put(k, c);
        });
        tag.put("Lines", li);
        CompoundTag th = new CompoundTag();
        threads.forEach((k, list) -> {
            ListTag l = new ListTag();
            for (Msg m : list) {
                CompoundTag c = new CompoundTag();
                c.putString("From", m.from());
                c.putString("Text", m.text());
                c.putLong("Time", m.time());
                l.add(c);
            }
            th.put(k, l);
        });
        tag.put("Threads", th);
        CompoundTag un = new CompoundTag();
        unread.forEach((k, m) -> {
            CompoundTag c = new CompoundTag();
            m.forEach(c::putInt);
            un.put(k, c);
        });
        tag.put("Unread", un);
        ListTag po = new ListTag();
        for (Post p : posts) {
            CompoundTag c = new CompoundTag();
            c.putInt("Id", p.id());
            c.putString("Author", p.author());
            c.putString("Handle", p.handle());
            c.putString("Text", p.text());
            c.putLong("Time", p.time());
            ListTag likes = new ListTag();
            p.likes().forEach(u -> likes.add(StringTag.valueOf(u)));
            c.put("Likes", likes);
            po.add(c);
        }
        tag.put("Posts", po);
        ListTag ne = new ListTag();
        for (News n : news) {
            CompoundTag c = new CompoundTag();
            c.putString("Title", n.title());
            c.putString("Text", n.text());
            c.putLong("Time", n.time());
            ne.add(c);
        }
        tag.put("News", ne);
        tag.putInt("NextPost", nextPost);
        return tag;
    }
}
