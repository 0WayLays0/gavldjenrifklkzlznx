package com.waylays.arcanes.metier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The deposits being worked: how many attempts each has had, and those worked out (what they were, what trade, and
 * when they come back, in game ticks of the overworld). Kept with the world.
 */
public final class Deposits extends SavedData {
    private static final String NAME = "arcanes_oublies_deposits";

    /** A worked-out deposit. */
    public static final class Spent {
        public final ResourceKey<Level> dim;
        public final BlockPos pos;
        public final BlockState original;
        public final String def;
        public final long backAt;

        Spent(ResourceKey<Level> dim, BlockPos pos, BlockState original, String def, long backAt) {
            this.dim = dim;
            this.pos = pos;
            this.original = original;
            this.def = def;
            this.backAt = backAt;
        }
    }

    private final Map<String, Spent> spent = new HashMap<>();
    private final Map<String, Integer> worked = new HashMap<>();

    public static Deposits get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Deposits::new, Deposits::load, null), NAME);
    }

    private static String key(ResourceKey<Level> dim, BlockPos pos) {
        return dim.location() + "|" + pos.asLong();
    }

    @Nullable
    public Spent spent(ResourceKey<Level> dim, BlockPos pos) {
        return spent.get(key(dim, pos));
    }

    /** One more attempt on the deposit; true if it is now worked out ({@code uses} attempts). */
    public boolean work(ResourceKey<Level> dim, BlockPos pos, int uses) {
        String k = key(dim, pos);
        int n = worked.getOrDefault(k, 0) + 1;
        setDirty();
        if (n >= uses) {
            worked.remove(k);
            return true;
        }
        worked.put(k, n);
        return false;
    }

    public void deplete(ResourceKey<Level> dim, BlockPos pos, BlockState original, String def, long backAt) {
        spent.put(key(dim, pos), new Spent(dim, pos.immutable(), original, def, backAt));
        setDirty();
    }

    public void forget(ResourceKey<Level> dim, BlockPos pos) {
        if (spent.remove(key(dim, pos)) != null | worked.remove(key(dim, pos)) != null) {
            setDirty();
        }
    }

    public Iterator<Spent> iterator() {
        return spent.values().iterator();
    }

    public java.util.Collection<Spent> all() {
        return spent.values();
    }

    public void removeSpent(Spent s) {
        spent.remove(key(s.dim, s.pos));
        setDirty();
    }

    private static Deposits load(CompoundTag tag, HolderLookup.Provider registries) {
        Deposits d = new Deposits();
        for (Tag t : tag.getList("Spent", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ResourceLocation dimId = ResourceLocation.tryParse(c.getString("Dim"));
            if (dimId == null) {
                continue;
            }
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimId);
            BlockPos pos = BlockPos.of(c.getLong("Pos"));
            BlockState original = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), c.getCompound("Original"));
            d.spent.put(key(dim, pos), new Spent(dim, pos, original, c.getString("Def"), c.getLong("BackAt")));
        }
        CompoundTag w = tag.getCompound("Worked");
        for (String k : w.getAllKeys()) {
            d.worked.put(k, w.getInt(k));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Spent s : spent.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("Dim", s.dim.location().toString());
            c.putLong("Pos", s.pos.asLong());
            c.put("Original", NbtUtils.writeBlockState(s.original));
            c.putString("Def", s.def);
            c.putLong("BackAt", s.backAt);
            list.add(c);
        }
        tag.put("Spent", list);
        CompoundTag w = new CompoundTag();
        worked.forEach(w::putInt);
        tag.put("Worked", w);
        return tag;
    }
}
