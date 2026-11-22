package com.waylays.cinematic.client;

import com.waylays.cinematic.net.SyncMsg;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The film's shots for the selected scene. Each shot starts at a moment (a cut from the one before) and is a path
 * through camera points, a fixed camera, a camera following an actor, or an orbit. Points are taken from the current
 * view: fly with the free camera, move the timeline, press K (or "Point here").
 */
public class DirectorScreen extends Screen {
    private static int listOffset;
    private TimelineSlider timeline;

    public DirectorScreen() {
        super(Component.translatable("cinematic.director.title"));
    }

    private Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private Button btn(Component label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(label, b -> action.run()).bounds(x, y, w, 20).build());
    }

    private CameraTrack track() {
        return CameraTrack.forScene(ClientStudio.selectedScene);
    }

    private void changed() {
        CameraTrack t = track();
        t.sort();
        t.save();
        rebuildWidgets();
    }

    private int lw, rx, rw, pointsTop, bottomTop;

    @Override
    protected void init() {
        CameraTrack t = track();
        boolean scene = ClientStudio.scene(ClientStudio.selectedScene) != null;
        int m = Ui.MARGIN, p = Ui.pitch(height);
        lw = Math.min(220, (width - 3 * m) * 2 / 5);
        rx = m + lw + 18;
        rw = width - rx - m;
        bottomTop = height - 72;
        btn(tr("cinematic.studio.back"), width - m - 110, 6, 110, () -> minecraft.setScreen(new StudioScreen()));

        // The shots
        int y = Ui.TOP + 15;
        btn(tr("cinematic.director.new_shot"), m, y, lw, this::newShot).active = scene;
        y += p;
        int rows = Math.max(1, (bottomTop - 12 - y) / p);
        listOffset = Math.max(0, Math.min(listOffset, t.shots.size() - rows));
        boolean scroll = t.shots.size() > rows;
        int bw = scroll ? lw - 24 : lw;
        for (int i = 0; i < rows && listOffset + i < t.shots.size(); i++) {
            int idx = listOffset + i;
            CameraTrack.Shot s = t.shots.get(idx);
            String target = targetName(s.targetId());
            Component label = Component.literal((idx == t.selected ? "> " : "") + (idx + 1) + ".  " + TimelineSlider.time(s.start) + "   ")
                    .append(tr(s.type.key())).append(target.isEmpty() ? "" : " (" + target + ")");
            btn(label, m, y + i * p, bw, () -> {
                t.selected = idx;
                ClientStudio.previewing = false;
                ClientStudio.cursor = s.start;
                rebuildWidgets();
            });
        }
        if (scroll) {
            btn(Component.literal("^"), m + lw - 20, y, 20, () -> {
                listOffset = Math.max(0, listOffset - 1);
                rebuildWidgets();
            });
            btn(Component.literal("v"), m + lw - 20, y + (rows - 1) * p, 20, () -> {
                listOffset++;
                rebuildWidgets();
            });
        }

        // The selected shot
        int hw = (rw - Ui.GAP) / 2, rx2 = rx + hw + Ui.GAP;
        CameraTrack.Shot s = t.selectedShot();
        pointsTop = -1;
        if (s != null) {
            y = Ui.TOP + 15;
            btn(tr("cinematic.director.type", tr(s.type.key())), rx, y, hw, () -> {
                s.type = CameraTrack.Type.values()[(s.type.ordinal() + 1) % CameraTrack.Type.values().length];
                changed();
            });
            String target = targetName(s.targetId());
            btn(tr("cinematic.director.target", target.isEmpty() ? tr("cinematic.director.none") : Component.literal(target)), rx2, y, hw,
                    () -> {
                        s.target = nextTarget(s.targetId());
                        changed();
                    });
            y += p;
            btn(tr(s.lookAt ? "cinematic.director.look_on" : "cinematic.director.look_off"), rx, y, hw, () -> {
                s.lookAt = !s.lookAt;
                changed();
            });
            if (s.type == CameraTrack.Type.ORBIT) {
                btn(Component.literal("-"), rx2, y, 20, () -> {
                    s.orbitSpeed -= 4f;
                    changed();
                });
                btn(tr("cinematic.director.orbit_speed", String.format(java.util.Locale.ROOT, "%.0f", s.orbitSpeed)), rx2 + 22, y, hw - 44, () -> {
                }).active = false;
                btn(Component.literal("+"), rx2 + hw - 20, y, 20, () -> {
                    s.orbitSpeed += 4f;
                    changed();
                });
            }
            pointsTop = y + p + 8;
            y = pointsTop + 15;
            btn(tr("cinematic.director.add_point"), rx, y, hw, this::addPoint);
            btn(tr("cinematic.director.remove_point"), rx2, y, hw, () -> {
                int now = Math.round(ClientStudio.cursor);
                if (!s.keys.removeIf(k -> k.tick == now) && !s.keys.isEmpty()) s.keys.remove(s.keys.size() - 1);
                changed();
            });
            y += p;
            btn(tr("cinematic.director.go_point"), rx, y, hw, () -> {
                if (s.keys.isEmpty()) return;
                CameraTrack.Key near = s.keys.get(0);
                for (CameraTrack.Key k : s.keys) if (Math.abs(k.tick - ClientStudio.cursor) < Math.abs(near.tick - ClientStudio.cursor)) near = k;
                ClientStudio.previewing = false;
                ClientStudio.cursor = near.tick;
                StudioCamera.goTo(near);
                onClose();
            });
            btn(tr("cinematic.director.start_here"), rx2, y, hw, () -> {
                s.start = Math.round(ClientStudio.cursor);
                changed();
            });
            y += p;
            btn(tr("cinematic.director.delete_shot"), rx, y, rw, () -> {
                t.shots.remove(s);
                t.selected = -1;
                changed();
            });
        }

        // Preview and playback
        int ty = bottomTop + 15;
        timeline = addRenderableWidget(new TimelineSlider(m, ty, width - 2 * m, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int n = 4, w = (width - 2 * m - (n - 1) * Ui.GAP) / n, x = m, by = ty + 26;
        btn(tr(ClientStudio.previewing ? "cinematic.studio.preview_pause" : "cinematic.studio.preview"), x, by, w, () -> {
            if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
            ClientStudio.previewing = !ClientStudio.previewing;
            if (ClientStudio.previewing && StudioCamera.free()) StudioCamera.toggleFree(minecraft);
            rebuildWidgets();
        });
        x += w + Ui.GAP;
        btn(tr("cinematic.studio.play"), x, by, w, () -> {
            ClientStudio.playOnServer();
            onClose();
        }).active = scene && ClientStudio.canDirect();
        x += w + Ui.GAP;
        btn(tr(StudioCamera.free() ? "cinematic.studio.freecam_on" : "cinematic.studio.freecam_off"), x, by, w, () -> {
            StudioCamera.toggleFree(minecraft);
            if (StudioCamera.free()) onClose();
            else rebuildWidgets();
        });
        x += w + Ui.GAP;
        btn(tr(StudioCamera.filmCamera ? "cinematic.studio.filmcam_on" : "cinematic.studio.filmcam_off"), x, by, w, () -> {
            StudioCamera.filmCamera = !StudioCamera.filmCamera;
            rebuildWidgets();
        });
    }

    private static String targetName(UUID id) {
        SyncMsg.ActorInfo a = ClientStudio.actor(id);
        return a == null ? "" : a.name();
    }

    private static String nextTarget(UUID current) {
        SyncMsg.SceneInfo sc = ClientStudio.scene(ClientStudio.selectedScene);
        if (sc == null || sc.actors().isEmpty()) return null;
        List<UUID> ids = new ArrayList<>();
        for (SyncMsg.ActorInfo a : sc.actors()) ids.add(a.id());
        int i = current == null ? -1 : ids.indexOf(current);
        return i + 1 >= ids.size() ? null : ids.get(i + 1).toString();
    }

    /** A cut: a new shot starting at the cursor, from the current view. */
    private void newShot() {
        CameraTrack t = track();
        CameraTrack.Shot s = new CameraTrack.Shot();
        s.start = Math.round(ClientStudio.cursor);
        s.keys.add(StudioCamera.currentKey(s.start));
        t.shots.add(s);
        t.sort();
        t.selected = t.shots.indexOf(s);
        changed();
    }

    private void addPoint() {
        addPoint(track());
        rebuildWidgets();
    }

    /** Adds the current view as a point of the selected shot (or of a new one), at the cursor. Used by the K key too. */
    public static Component addPoint(CameraTrack t) {
        int now = Math.round(ClientStudio.cursor);
        CameraTrack.Shot s = t.selectedShot();
        if (s == null) {
            s = new CameraTrack.Shot();
            s.start = now;
            t.shots.add(s);
            t.sort();
            t.selected = t.shots.indexOf(s);
        }
        CameraTrack.putKey(s, StudioCamera.currentKey(now));
        t.save();
        return Component.translatable("cinematic.director.point_added", s.keys.size(), t.selected + 1, TimelineSlider.time(now));
    }

    @Override
    public void tick() {
        if (!timeline.isFocused() || ClientStudio.previewing) timeline.sync();
        CameraTrack t = track();
        int[] cuts = t.shots.stream().mapToInt(s -> s.start).toArray();
        CameraTrack.Shot s = t.selectedShot();
        int[] points = s == null ? new int[0] : s.keys.stream().mapToInt(k -> k.tick).toArray();
        timeline.setMarks(cuts, points);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            int step = (mods & GLFW.GLFW_MOD_SHIFT) != 0 ? 20 : 5;
            ClientStudio.previewing = false;
            ClientStudio.cursor = Math.max(0, Math.round(ClientStudio.cursor) + (key == GLFW.GLFW_KEY_LEFT ? -step : step));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int m = Ui.MARGIN;
        String scene = ClientStudio.selectedScene.isEmpty() ? "-" : ClientStudio.selectedScene;
        Ui.header(g, font, width, title.copy().append(" : " + scene), tr("cinematic.director.subtitle"));
        int colH = bottomTop - 12 - Ui.TOP;
        Ui.panel(g, m, Ui.TOP, lw, colH);
        Ui.panel(g, rx, Ui.TOP, rw, colH);
        Ui.panel(g, m, bottomTop, width - 2 * m, 61);
        Ui.section(g, font, m, Ui.TOP, lw, tr("cinematic.director.section_shots"));
        Ui.section(g, font, m, bottomTop, width - 2 * m, tr("cinematic.director.section_play"));
        CameraTrack t = track();
        CameraTrack.Shot s = t.selectedShot();
        if (s == null) {
            Ui.section(g, font, rx, Ui.TOP, rw, tr("cinematic.director.section_shot_none"));
            g.drawWordWrap(font, tr(t.shots.isEmpty() ? "cinematic.director.empty" : "cinematic.director.pick"), rx, Ui.TOP + 20, rw, Ui.HINT);
        } else {
            Ui.section(g, font, rx, Ui.TOP, rw, tr("cinematic.director.section_shot", t.selected + 1));
            if (pointsTop > 0) Ui.section(g, font, rx, pointsTop, rw, tr("cinematic.director.section_points", s.keys.size()));
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
