package com.waylays.cinematic.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.waylays.cinematic.studio.Frame;
import com.waylays.cinematic.studio.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The film's camera for one scene: a list of shots, each from its start time to the next shot's (a cut). A shot is a
 * path through points, a fixed camera, a camera following an actor, or an orbit around one. Saved on the client in
 * .minecraft/cinematic/cameras/(world)/(scene).json.
 */
public final class CameraTrack {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, CameraTrack> LOADED = new HashMap<>();

    public enum Type {
        PATH, FIXED, FOLLOW, ORBIT;

        public String key() {
            return "cinematic.shot." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final class Key {
        public int tick;
        public double x, y, z;
        public float yaw, pitch, roll, fov = 70f;
    }

    public static final class Shot {
        public int start;
        public Type type = Type.PATH;
        public List<Key> keys = new ArrayList<>();
        public String target;
        public boolean lookAt;
        public float orbitSpeed = 12f;

        public UUID targetId() {
            try {
                return target == null || target.isEmpty() ? null : UUID.fromString(target);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** What the camera does at an instant. */
    public static final class View {
        public Vec3 pos;
        public float yaw, pitch, roll, fov;
        public float focus = -1f;
    }

    public List<Shot> shots = new ArrayList<>();
    private transient String file;
    public transient int selected = -1;

    // ------------------------------------------------------------------ storage

    private static Path folder() {
        Minecraft mc = Minecraft.getInstance();
        String world;
        if (mc.getSingleplayerServer() != null) world = "solo_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        else if (mc.getCurrentServer() != null) world = "server_" + mc.getCurrentServer().ip;
        else world = "unknown";
        return mc.gameDirectory.toPath().resolve("cinematic").resolve("cameras").resolve(safe(world));
    }

    private static String safe(String s) {
        String out = s.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return out.isEmpty() ? "_" : out;
    }

    public static CameraTrack forScene(String scene) {
        if (scene == null || scene.isEmpty()) return new CameraTrack();
        Path f = folder().resolve(safe(scene) + ".json");
        String key = f.toString();
        CameraTrack t = LOADED.get(key);
        if (t != null) return t;
        t = null;
        if (Files.isRegularFile(f)) {
            try {
                t = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), CameraTrack.class);
            } catch (IOException | RuntimeException e) {
                LOG.warn("Cinematic: camera file {} unreadable", f, e);
            }
        }
        if (t == null) t = new CameraTrack();
        if (t.shots == null) t.shots = new ArrayList<>();
        for (Shot s : t.shots) {
            if (s.keys == null) s.keys = new ArrayList<>();
            if (s.type == null) s.type = Type.PATH;
        }
        t.file = key;
        t.sort();
        LOADED.put(key, t);
        return t;
    }

    public static void forgetLoaded() {
        LOADED.clear();
    }

    public void save() {
        if (file == null) return;
        try {
            Path f = Path.of(file);
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Cinematic: could not save camera {}", file, e);
        }
    }

    // ------------------------------------------------------------------ editing

    public void sort() {
        Shot sel = selectedShot();
        shots.sort(Comparator.comparingInt(s -> s.start));
        for (Shot s : shots) s.keys.sort(Comparator.comparingInt(k -> k.tick));
        selected = sel == null ? -1 : shots.indexOf(sel);
    }

    public Shot selectedShot() {
        return selected >= 0 && selected < shots.size() ? shots.get(selected) : null;
    }

    public int lastTick() {
        int t = 0;
        for (Shot s : shots) {
            t = Math.max(t, s.start);
            for (Key k : s.keys) t = Math.max(t, k.tick);
        }
        return t;
    }

    public Shot shotAt(float time) {
        Shot cur = null;
        for (Shot s : shots) {
            if (s.start <= time) cur = s;
        }
        return cur != null ? cur : (shots.isEmpty() ? null : shots.get(0));
    }

    public static Key key(int tick, Vec3 pos, float yaw, float pitch, float roll, float fov) {
        Key k = new Key();
        k.tick = tick;
        k.x = pos.x;
        k.y = pos.y;
        k.z = pos.z;
        k.yaw = yaw;
        k.pitch = pitch;
        k.roll = roll;
        k.fov = fov;
        return k;
    }

    /** Adds or replaces the point at that tick in a shot. */
    public static void putKey(Shot s, Key k) {
        s.keys.removeIf(o -> o.tick == k.tick);
        s.keys.add(k);
        s.keys.sort(Comparator.comparingInt(o -> o.tick));
    }

    // ------------------------------------------------------------------ playback

    /** Where an actor's eyes are at a fractional tick, from its take. */
    static Vec3 actorEye(UUID id, float time) {
        Track t = ClientStudio.trackOf(id);
        if (t == null || t.isEmpty()) {
            var e = ClientStudio.findEntity(id);
            return e == null ? null : e.getEyePosition();
        }
        int i = (int) Math.floor(time);
        float u = time - i;
        Frame a = t.frame(i), b = t.frame(i + 1);
        double eye = a.pose == 1 ? 1.27 : (a.pose == 2 || a.pose == 3 ? 0.4 : 1.52);
        return new Vec3(Mth.lerp(u, a.x, b.x), Mth.lerp(u, a.y, b.y) + eye, Mth.lerp(u, a.z, b.z));
    }

    public View evaluate(float time) {
        Shot s = shotAt(time);
        if (s == null || s.keys.isEmpty()) return null;
        View v = new View();
        UUID target = s.targetId();
        Vec3 eye = target == null ? null : actorEye(target, time);
        Key k0 = s.keys.get(0);
        switch (s.type) {
            case PATH -> spline(s.keys, time, v);
            case FIXED -> fromKey(k0, v);
            case FOLLOW -> {
                fromKey(k0, v);
                if (eye != null) {
                    Vec3 eye0 = actorEye(target, k0.tick);
                    if (eye0 != null) v.pos = eye.add(new Vec3(k0.x, k0.y, k0.z).subtract(eye0));
                }
            }
            case ORBIT -> {
                fromKey(k0, v);
                if (eye != null) {
                    Vec3 eye0 = actorEye(target, k0.tick);
                    if (eye0 == null) eye0 = eye;
                    double dx = k0.x - eye0.x, dz = k0.z - eye0.z;
                    double r = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
                    double a0 = Math.atan2(dz, dx);
                    double a = a0 + Math.toRadians(s.orbitSpeed * (time - s.start) / 20.0);
                    v.pos = new Vec3(eye.x + Math.cos(a) * r, eye.y + (k0.y - eye0.y), eye.z + Math.sin(a) * r);
                }
            }
        }
        boolean look = eye != null && (s.lookAt || s.type == Type.ORBIT);
        if (look) {
            Vec3 d = eye.subtract(v.pos);
            v.yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            v.pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        }
        if (eye != null) v.focus = (float) eye.distanceTo(v.pos);
        return v;
    }

    private static void fromKey(Key k, View v) {
        v.pos = new Vec3(k.x, k.y, k.z);
        v.yaw = k.yaw;
        v.pitch = k.pitch;
        v.roll = k.roll;
        v.fov = k.fov;
    }

    /** A smooth curve through the points (cubic Hermite, tangents scaled by time: no jerk where points are uneven). */
    private static void spline(List<Key> keys, float time, View v) {
        int n = keys.size();
        if (n == 1 || time <= keys.get(0).tick) {
            fromKey(keys.get(0), v);
            return;
        }
        if (time >= keys.get(n - 1).tick) {
            fromKey(keys.get(n - 1), v);
            return;
        }
        // Channels, with the angles unwrapped so the camera turns the short way
        double[][] c = new double[n][7];
        for (int i = 0; i < n; i++) {
            Key k = keys.get(i);
            c[i][0] = k.x;
            c[i][1] = k.y;
            c[i][2] = k.z;
            c[i][3] = i == 0 ? k.yaw : c[i - 1][3] + Mth.wrapDegrees(k.yaw - c[i - 1][3]);
            c[i][4] = k.pitch;
            c[i][5] = k.roll;
            c[i][6] = k.fov;
        }
        int i = 0;
        while (i < n - 2 && time >= keys.get(i + 1).tick) i++;
        double t0 = keys.get(i).tick, t1 = keys.get(i + 1).tick;
        double h = Math.max(1e-3, t1 - t0);
        double u = (time - t0) / h;
        double u2 = u * u, u3 = u2 * u;
        double h00 = 2 * u3 - 3 * u2 + 1, h10 = u3 - 2 * u2 + u, h01 = -2 * u3 + 3 * u2, h11 = u3 - u2;
        double[] out = new double[7];
        for (int ch = 0; ch < 7; ch++) {
            double m0 = tangent(keys, c, i, ch), m1 = tangent(keys, c, i + 1, ch);
            out[ch] = h00 * c[i][ch] + h10 * h * m0 + h01 * c[i + 1][ch] + h11 * h * m1;
        }
        v.pos = new Vec3(out[0], out[1], out[2]);
        v.yaw = (float) out[3];
        v.pitch = (float) out[4];
        v.roll = (float) out[5];
        v.fov = (float) out[6];
    }

    private static double tangent(List<Key> keys, double[][] c, int i, int ch) {
        if (i <= 0 || i >= keys.size() - 1) return 0;
        double dt = keys.get(i + 1).tick - keys.get(i - 1).tick;
        return dt <= 0 ? 0 : (c[i + 1][ch] - c[i - 1][ch]) / dt;
    }
}
