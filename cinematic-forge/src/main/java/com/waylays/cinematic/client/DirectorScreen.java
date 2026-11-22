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

    @Override
    protected void init() {
        CameraTrack t = track();
        boolean scene = ClientStudio.scene(ClientStudio.selectedScene) != null;
        int left = 8, top = 24;
        int rows = Math.max(1, (height - top - 110) / 22);
        listOffset = Math.max(0, Math.min(listOffset, t.shots.size() - rows));
        for (int i = 0; i < rows && listOffset + i < t.shots.size(); i++) {
            int idx = listOffset + i;
            CameraTrack.Shot s = t.shots.get(idx);
            String target = targetName(s.targetId());
            Component label = Component.literal((idx == t.selected ? "> " : "") + (idx + 1) + ". " + TimelineSlider.time(s.start) + "  ")
                    .append(tr(s.type.key())).append(target.isEmpty() ? "" : " - " + target);
            btn(label, left, top + i * 22, 190, () -> {
                t.selected = idx;
                ClientStudio.previewing = false;
                ClientStudio.cursor = s.start;
                rebuildWidgets();
            });
        }
        if (t.shots.size() > rows) {
            btn(Component.literal("^"), left + 192, top, 18, () -> {
                listOffset = Math.max(0, listOffset - 1);
                rebuildWidgets();
            });
            btn(Component.literal("v"), left + 192, top + (rows - 1) * 22, 18, () -> {
                listOffset++;
                rebuildWidgets();
            });
        }

        int rx = Math.max(220, width - 228), w = 220;
        btn(tr("cinematic.director.new_shot"), rx, top, w, this::newShot).active = scene;
        CameraTrack.Shot s = t.selectedShot();
        if (s != null) {
            int y = top + 30;
            btn(tr("cinematic.director.type", tr(s.type.key())), rx, y, w, () -> {
                s.type = CameraTrack.Type.values()[(s.type.ordinal() + 1) % CameraTrack.Type.values().length];
                changed();
            });
            String target = targetName(s.targetId());
            btn(tr("cinematic.director.target", target.isEmpty() ? tr("cinematic.director.none") : Component.literal(target)), rx, y + 22, w,
                    () -> {
                        s.target = nextTarget(s.targetId());
                        changed();
                    });
            btn(tr(s.lookAt ? "cinematic.director.look_on" : "cinematic.director.look_off"), rx, y + 44, w, () -> {
                s.lookAt = !s.lookAt;
                changed();
            });
            if (s.type == CameraTrack.Type.ORBIT) {
                btn(Component.literal("-"), rx, y + 66, 20, () -> {
                    s.orbitSpeed -= 4f;
                    changed();
                });
                btn(tr("cinematic.director.orbit_speed", String.format(java.util.Locale.ROOT, "%.0f", s.orbitSpeed)), rx + 22, y + 66, w - 44, () -> {
                });
                btn(Component.literal("+"), rx + w - 20, y + 66, 20, () -> {
                    s.orbitSpeed += 4f;
                    changed();
                });
            }
            btn(tr("cinematic.director.add_point"), rx, y + 92, w, this::addPoint);
            btn(tr("cinematic.director.remove_point"), rx, y + 114, w / 2 - 2, () -> {
                int now = Math.round(ClientStudio.cursor);
                if (!s.keys.removeIf(k -> k.tick == now) && !s.keys.isEmpty()) s.keys.remove(s.keys.size() - 1);
                changed();
            });
            btn(tr("cinematic.director.go_point"), rx + w / 2 + 2, y + 114, w / 2 - 2, () -> {
                if (s.keys.isEmpty()) return;
                CameraTrack.Key near = s.keys.get(0);
                for (CameraTrack.Key k : s.keys) if (Math.abs(k.tick - ClientStudio.cursor) < Math.abs(near.tick - ClientStudio.cursor)) near = k;
                ClientStudio.previewing = false;
                ClientStudio.cursor = near.tick;
                StudioCamera.goTo(near);
                onClose();
            });
            btn(tr("cinematic.director.start_here"), rx, y + 136, w, () -> {
                s.start = Math.round(ClientStudio.cursor);
                changed();
            });
            btn(tr("cinematic.director.delete_shot"), rx, y + 158, w, () -> {
                t.shots.remove(s);
                t.selected = -1;
                changed();
            });
        }

        timeline = addRenderableWidget(new TimelineSlider(8, height - 50, width - 16, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int n = 5, bw = Math.min(124, (width - 16 - (n - 1) * 3) / n), x = 8, by = height - 26;
        btn(tr(ClientStudio.previewing ? "cinematic.studio.preview_pause" : "cinematic.studio.preview"), x, by, bw, () -> {
            if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
            ClientStudio.previewing = !ClientStudio.previewing;
            if (ClientStudio.previewing && StudioCamera.free()) StudioCamera.toggleFree(minecraft);
            rebuildWidgets();
        });
        x += bw + 3;
        btn(tr("cinematic.studio.play"), x, by, bw, () -> {
            ClientStudio.playOnServer();
            onClose();
        }).active = scene && ClientStudio.canDirect();
        x += bw + 3;
        btn(tr(StudioCamera.free() ? "cinematic.studio.freecam_on" : "cinematic.studio.freecam_off"), x, by, bw, () -> {
            StudioCamera.toggleFree(minecraft);
            if (StudioCamera.free()) onClose();
            else rebuildWidgets();
        });
        x += bw + 3;
        btn(tr(StudioCamera.filmCamera ? "cinematic.studio.filmcam_on" : "cinematic.studio.filmcam_off"), x, by, bw, () -> {
            StudioCamera.filmCamera = !StudioCamera.filmCamera;
            rebuildWidgets();
        });
        x += bw + 3;
        btn(tr("cinematic.studio.back"), x, by, bw, () -> minecraft.setScreen(new StudioScreen()));
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
        g.fill(0, 0, 214, height - 56, 0x70000000);
        g.fill(Math.max(220, width - 228) - 4, 0, width, height - 56, 0x70000000);
        g.fill(0, height - 56, width, height, 0x70000000);
        g.drawString(font, title.copy().append(" - " + (ClientStudio.selectedScene.isEmpty() ? "-" : ClientStudio.selectedScene)), 8, 8, 0xFFFFFF);
        if (track().shots.isEmpty()) g.drawString(font, tr("cinematic.director.empty"), 8, 30, 0xA0A0A0);
        CameraTrack.Shot s = track().selectedShot();
        if (s != null) {
            g.drawString(font, tr("cinematic.director.points", s.keys.size()), Math.max(220, width - 228), 12, 0xFFE070);
        }
        g.drawCenteredString(font, tr("cinematic.director.hint"), width / 2, height - 64, 0xC0C0C0);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
