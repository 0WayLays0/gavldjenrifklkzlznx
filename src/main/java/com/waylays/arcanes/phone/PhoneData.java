package com.waylays.arcanes.phone;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the Arcanophones of a world keep on the server, so it is there when a player comes back: the messages between
 * players (delivered to the absent when they return), each player's contacts and unread counts, the names last seen,
 * the posts of the Echoes feed, the emerald accounts of the bank, and who shares their position.
 */
public class PhoneData extends SavedData {
    public static final int MAX_THREAD = 200, MAX_POSTS = 100, MAX_HISTORY = 30;
    private static final String NAME = "arcanes_oublies_phone";

    public record Msg(UUID from, String text, long time) {}

    public record Post(int id, UUID author, String text, long time, Set<UUID> likes) {}

    /** A line of a bank account's history: {@code kind} deposit, withdraw, sent or received; {@code other} the other player. */
    public record Txn(String kind, String other, long amount, long time) {}

    final Map<String, List<Msg>> threads = new HashMap<>();
    final Map<UUID, Map<UUID, Integer>> unread = new HashMap<>();
    final Map<UUID, LinkedHashSet<UUID>> contacts = new HashMap<>();
    final Map<UUID, String> names = new HashMap<>();
    final List<Post> posts = new ArrayList<>();
    final Map<UUID, Long> balances = new HashMap<>();
    final Map<UUID, List<Txn>> history = new HashMap<>();
    final Set<UUID> sharing = new HashSet<>();
    int nextPost = 1;

    public static PhoneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PhoneData::new, PhoneData::load, null), NAME);
    }

    static String key(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public List<Msg> thread(UUID a, UUID b) {
        return threads.getOrDefault(key(a, b), List.of());
    }

    void add(UUID from, UUID to, String text, long time) {
        List<Msg> t = threads.computeIfAbsent(key(from, to), k -> new ArrayList<>());
        t.add(new Msg(from, text, time));
        while (t.size() > MAX_THREAD) {
            t.remove(0);
        }
        unread.computeIfAbsent(to, k -> new HashMap<>()).merge(from, 1, Integer::sum);
        // Each is now in the other's conversations; the sender keeps the receiver as a contact
        contacts.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
        setDirty();
    }

    void read(UUID reader, UUID other) {
        Map<UUID, Integer> m = unread.get(reader);
        if (m != null && m.remove(other) != null) {
            setDirty();
        }
    }

    public int unread(UUID reader, UUID other) {
        return unread.getOrDefault(reader, Map.of()).getOrDefault(other, 0);
    }

    public int unreadTotal(UUID reader) {
        return unread.getOrDefault(reader, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
    }

    /** The players {@code who} has a conversation with, latest first. */
    public List<UUID> partners(UUID who) {
        Map<UUID, Long> last = new LinkedHashMap<>();
        String me = who.toString();
        for (Map.Entry<String, List<Msg>> e : threads.entrySet()) {
            String[] p = e.getKey().split("\\|");
            if (p.length != 2 || e.getValue().isEmpty() || !(p[0].equals(me) || p[1].equals(me))) {
                continue;
            }
            UUID other = UUID.fromString(p[0].equals(me) ? p[1] : p[0]);
            last.put(other, e.getValue().get(e.getValue().size() - 1).time());
        }
        List<UUID> out = new ArrayList<>(last.keySet());
        out.sort((x, y) -> Long.compare(last.get(y), last.get(x)));
        return out;
    }

    public Set<UUID> contacts(UUID who) {
        return contacts.getOrDefault(who, new LinkedHashSet<>());
    }

    public String name(UUID id) {
        return names.getOrDefault(id, id.toString().substring(0, 8));
    }

    public long balance(UUID id) {
        return balances.getOrDefault(id, 0L);
    }

    void credit(UUID id, long amount, String kind, String other, long time) {
        balances.merge(id, amount, Long::sum);
        List<Txn> h = history.computeIfAbsent(id, k -> new ArrayList<>());
        h.add(0, new Txn(kind, other, amount, time));
        while (h.size() > MAX_HISTORY) {
            h.remove(h.size() - 1);
        }
        setDirty();
    }

    public List<Txn> history(UUID id) {
        return history.getOrDefault(id, List.of());
    }

    // ---------------------------------------------------------------- saving

    private static PhoneData load(CompoundTag tag, HolderLookup.Provider registries) {
        PhoneData d = new PhoneData();
        CompoundTag th = tag.getCompound("Threads");
        for (String k : th.getAllKeys()) {
            List<Msg> list = new ArrayList<>();
            for (Tag t : th.getList(k, Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                list.add(new Msg(c.getUUID("From"), c.getString("Text"), c.getLong("Time")));
            }
            d.threads.put(k, list);
        }
        CompoundTag un = tag.getCompound("Unread");
        for (String k : un.getAllKeys()) {
            Map<UUID, Integer> m = new HashMap<>();
            CompoundTag c = un.getCompound(k);
            for (String o : c.getAllKeys()) {
                m.put(UUID.fromString(o), c.getInt(o));
            }
            d.unread.put(UUID.fromString(k), m);
        }
        CompoundTag co = tag.getCompound("Contacts");
        for (String k : co.getAllKeys()) {
            LinkedHashSet<UUID> s = new LinkedHashSet<>();
            for (Tag t : co.getList(k, Tag.TAG_STRING)) {
                s.add(UUID.fromString(t.getAsString()));
            }
            d.contacts.put(UUID.fromString(k), s);
        }
        CompoundTag na = tag.getCompound("Names");
        for (String k : na.getAllKeys()) {
            d.names.put(UUID.fromString(k), na.getString(k));
        }
        for (Tag t : tag.getList("Posts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Set<UUID> likes = new HashSet<>();
            for (Tag l : c.getList("Likes", Tag.TAG_STRING)) {
                likes.add(UUID.fromString(l.getAsString()));
            }
            d.posts.add(new Post(c.getInt("Id"), c.getUUID("Author"), c.getString("Text"), c.getLong("Time"), likes));
        }
        CompoundTag ba = tag.getCompound("Balances");
        for (String k : ba.getAllKeys()) {
            d.balances.put(UUID.fromString(k), ba.getLong(k));
        }
        CompoundTag hi = tag.getCompound("History");
        for (String k : hi.getAllKeys()) {
            List<Txn> list = new ArrayList<>();
            for (Tag t : hi.getList(k, Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                list.add(new Txn(c.getString("Kind"), c.getString("Other"), c.getLong("Amount"), c.getLong("Time")));
            }
            d.history.put(UUID.fromString(k), list);
        }
        for (Tag t : tag.getList("Sharing", Tag.TAG_STRING)) {
            d.sharing.add(UUID.fromString(t.getAsString()));
        }
        d.nextPost = Math.max(1, tag.getInt("NextPost"));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag th = new CompoundTag();
        threads.forEach((k, list) -> {
            ListTag l = new ListTag();
            for (Msg m : list) {
                CompoundTag c = new CompoundTag();
                c.putUUID("From", m.from());
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
            m.forEach((o, n) -> c.putInt(o.toString(), n));
            un.put(k.toString(), c);
        });
        tag.put("Unread", un);
        CompoundTag co = new CompoundTag();
        contacts.forEach((k, s) -> {
            ListTag l = new ListTag();
            s.forEach(u -> l.add(net.minecraft.nbt.StringTag.valueOf(u.toString())));
            co.put(k.toString(), l);
        });
        tag.put("Contacts", co);
        CompoundTag na = new CompoundTag();
        names.forEach((k, v) -> na.putString(k.toString(), v));
        tag.put("Names", na);
        ListTag po = new ListTag();
        for (Post p : posts) {
            CompoundTag c = new CompoundTag();
            c.putInt("Id", p.id());
            c.putUUID("Author", p.author());
            c.putString("Text", p.text());
            c.putLong("Time", p.time());
            ListTag likes = new ListTag();
            p.likes().forEach(u -> likes.add(net.minecraft.nbt.StringTag.valueOf(u.toString())));
            c.put("Likes", likes);
            po.add(c);
        }
        tag.put("Posts", po);
        CompoundTag ba = new CompoundTag();
        balances.forEach((k, v) -> ba.putLong(k.toString(), v));
        tag.put("Balances", ba);
        CompoundTag hi = new CompoundTag();
        history.forEach((k, list) -> {
            ListTag l = new ListTag();
            for (Txn t : list) {
                CompoundTag c = new CompoundTag();
                c.putString("Kind", t.kind());
                c.putString("Other", t.other());
                c.putLong("Amount", t.amount());
                c.putLong("Time", t.time());
                l.add(c);
            }
            hi.put(k.toString(), l);
        });
        tag.put("History", hi);
        ListTag sh = new ListTag();
        sharing.forEach(u -> sh.add(net.minecraft.nbt.StringTag.valueOf(u.toString())));
        tag.put("Sharing", sh);
        tag.putInt("NextPost", nextPost);
        return tag;
    }
}
