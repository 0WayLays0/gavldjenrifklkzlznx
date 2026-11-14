package com.waylays.arcanes.rp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The characters of the world: each player's character's first and last name and the description others read when
 * they look at them; and who, if anyone, is seen only by a chosen few.
 */
public class RpData extends SavedData {
    private static final String NAME = "eraphones_rp";

    public record Identity(String first, String last, String description) {
        public String full() {
            return (first + " " + last).trim();
        }
    }

    final Map<UUID, Identity> identities = new HashMap<>();
    /** Players seen only by these (an empty set: by no one). */
    final Map<UUID, Set<UUID>> seenOnlyBy = new HashMap<>();
    /** Whether the names over the heads are hidden for everyone: hidden unless a game master shows them (/pseudos afficher). */
    boolean hideNames = true;

    public static RpData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(RpData::new, RpData::load, null), NAME);
    }

    @Nullable
    public Identity identity(UUID id) {
        return identities.get(id);
    }

    public void setName(UUID id, String first, String last) {
        Identity old = identities.get(id);
        identities.put(id, new Identity(first, last, old == null ? "" : old.description()));
        setDirty();
    }

    public void setDescription(UUID id, String description) {
        Identity old = identities.get(id);
        identities.put(id, new Identity(old == null ? "" : old.first(), old == null ? "" : old.last(), description));
        setDirty();
    }

    public Map<UUID, Identity> identities() {
        return identities;
    }

    @Nullable
    public Set<UUID> seenOnlyBy(UUID id) {
        return seenOnlyBy.get(id);
    }

    public void hide(UUID id, Set<UUID> viewers) {
        seenOnlyBy.put(id, new HashSet<>(viewers));
        setDirty();
    }

    public boolean hideNames() {
        return hideNames;
    }

    public void hideNames(boolean hide) {
        hideNames = hide;
        setDirty();
    }

    public void show(UUID id) {
        if (seenOnlyBy.remove(id) != null) {
            setDirty();
        }
    }

    private static RpData load(CompoundTag tag, HolderLookup.Provider registries) {
        RpData d = new RpData();
        CompoundTag ids = tag.getCompound("Identities");
        for (String k : ids.getAllKeys()) {
            CompoundTag c = ids.getCompound(k);
            d.identities.put(UUID.fromString(k), new Identity(c.getString("First"), c.getString("Last"), c.getString("Description")));
        }
        CompoundTag hid = tag.getCompound("SeenOnlyBy");
        for (String k : hid.getAllKeys()) {
            Set<UUID> s = new HashSet<>();
            for (Tag t : hid.getList(k, Tag.TAG_STRING)) {
                s.add(UUID.fromString(t.getAsString()));
            }
            d.seenOnlyBy.put(UUID.fromString(k), s);
        }
        d.hideNames = !tag.getBoolean("NamesShown");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag ids = new CompoundTag();
        identities.forEach((k, v) -> {
            CompoundTag c = new CompoundTag();
            c.putString("First", v.first());
            c.putString("Last", v.last());
            c.putString("Description", v.description());
            ids.put(k.toString(), c);
        });
        tag.put("Identities", ids);
        CompoundTag hid = new CompoundTag();
        seenOnlyBy.forEach((k, v) -> {
            ListTag l = new ListTag();
            v.forEach(u -> l.add(StringTag.valueOf(u.toString())));
            hid.put(k.toString(), l);
        });
        tag.put("SeenOnlyBy", hid);
        tag.putBoolean("NamesShown", !hideNames);
        return tag;
    }
}
