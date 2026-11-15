package com.waylays.arcanes.rp;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * What Époque RP kept when these commands were part of it. A player's own data and the attribute modifiers keep the
 * names they had there (so a world keeps its characters' sizes, health and speeds). The saved data of the world
 * (characters' names, fear, triggers) has names of its own here, so that it never meets an older Époque RP that
 * still has these commands: the first time, it is read from the file Époque RP left.
 */
public final class Legacy {
    public static final String PREFIX = "eraphones_";

    private Legacy() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("eraphones", path);
    }

    /** The world's saved data {@code name}, taken over from Époque RP's file the first time if there is one. */
    public static <T extends SavedData> T data(MinecraftServer server, String name, Supplier<T> fresh,
                                               BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(() -> {
            T old = readOld(server, PREFIX + name, load);
            if (old == null) {
                return fresh.get();
            }
            old.setDirty();
            ArcanesOublies.LOGGER.info("Took over Époque RP's saved data {} as {}", PREFIX + name, ArcanesOublies.MODID + "_" + name);
            return old;
        }, load, null), ArcanesOublies.MODID + "_" + name);
    }

    private static <T> T readOld(MinecraftServer server, String file, BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        try {
            Path p = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(file + ".dat");
            if (!Files.isRegularFile(p)) {
                return null;
            }
            CompoundTag root = NbtIo.readCompressed(p, NbtAccounter.unlimitedHeap());
            return load.apply(root.getCompound("data"), server.registryAccess());
        } catch (Exception e) {
            ArcanesOublies.LOGGER.warn("Époque RP's saved data {} could not be read: {}", file, e.toString());
            return null;
        }
    }

    /** An Époque RP from before these commands left it: both would answer them. Said once, in the log. */
    public static void warnOldEpoqueRp() {
        try {
            Class.forName("com.waylays.eraphones.rp.RpService", false, Legacy.class.getClassLoader());
            ArcanesOublies.LOGGER.warn("This Époque RP still has the role play commands (/setname, /me, /horreur...), now in Arcanes Oubliés: "
                    + "update it to 1.6.0 or later");
        } catch (ClassNotFoundException | LinkageError e) {
            // A recent Époque RP, or none
        }
    }
}
