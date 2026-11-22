package com.waylays.cinematic.studio;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The studio of a world: its scenes, their actors and takes (data/cinematic_studio.dat). */
public final class StudioData extends SavedData {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String NAME = "cinematic_studio";

    public static final class Actor {
        public UUID id = UUID.randomUUID();
        public UUID entity;
        public String name = "";
        public String skin = "";
        public boolean slim;
        public ResourceKey<Level> dim = Level.OVERWORLD;
        public Track track = new Track();
        public String scene = "";
    }

    public static final class Scene {
        public String name;
        public final List<Actor> actors = new ArrayList<>();

        Scene(String name) {
            this.name = name;
        }

        public int length() {
            int len = 0;
            for (Actor a : actors) len = Math.max(len, a.track.length());
            return len;
        }
    }

    public final Map<String, Scene> scenes = new LinkedHashMap<>();

    public static StudioData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StudioData::load, StudioData::new, NAME);
    }

    public Scene addScene(String name) {
        Scene s = new Scene(name);
        scenes.put(name, s);
        setDirty();
        return s;
    }

    public Actor actor(UUID id) {
        if (id == null) return null;
        for (Scene s : scenes.values()) {
            for (Actor a : s.actors) if (a.id.equals(id)) return a;
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Scene s : scenes.values()) {
            CompoundTag st = new CompoundTag();
            st.putString("Name", s.name);
            ListTag actors = new ListTag();
            for (Actor a : s.actors) {
                CompoundTag at = new CompoundTag();
                at.putUUID("Id", a.id);
                if (a.entity != null) at.putUUID("Entity", a.entity);
                at.putString("Name", a.name);
                at.putString("Skin", a.skin);
                at.putBoolean("Slim", a.slim);
                at.putString("Dim", a.dim.location().toString());
                at.putByteArray("Take", a.track.toBytes());
                actors.add(at);
            }
            st.put("Actors", actors);
            list.add(st);
        }
        tag.put("Scenes", list);
        return tag;
    }

    public static StudioData load(CompoundTag tag) {
        StudioData d = new StudioData();
        ListTag list = tag.getList("Scenes", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag st = list.getCompound(i);
            Scene s = new Scene(st.getString("Name"));
            ListTag actors = st.getList("Actors", 10);
            for (int j = 0; j < actors.size(); j++) {
                CompoundTag at = actors.getCompound(j);
                Actor a = new Actor();
                a.id = at.getUUID("Id");
                a.entity = at.hasUUID("Entity") ? at.getUUID("Entity") : null;
                a.name = at.getString("Name");
                a.skin = at.getString("Skin");
                a.slim = at.getBoolean("Slim");
                a.scene = s.name;
                ResourceLocation dim = ResourceLocation.tryParse(at.getString("Dim"));
                if (dim != null) a.dim = ResourceKey.create(Registries.DIMENSION, dim);
                try {
                    a.track = Track.fromBytes(at.getByteArray("Take"));
                } catch (RuntimeException e) {
                    LOG.error("Cinematic: the take of actor {} could not be read, it starts empty", a.name, e);
                }
                s.actors.add(a);
            }
            d.scenes.put(s.name, s);
        }
        return d;
    }
}
