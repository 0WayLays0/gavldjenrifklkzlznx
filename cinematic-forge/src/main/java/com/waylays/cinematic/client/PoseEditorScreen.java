package com.waylays.cinematic.client;

import com.waylays.cinematic.studio.BodyPart;
import com.waylays.cinematic.studio.PoseKey;
import com.waylays.cinematic.studio.Track;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Retouches a take limb by limb: at a moment of the timeline, turn the head, body, arms or legs (degrees, on top of
 * the recorded movement) and set a key; the pose eases from key to key. The camera turns around the actor
 * (right-drag, wheel to zoom); the world stays visible in the middle.
 */
public class PoseEditorScreen extends Screen {
    private final UUID actor;
    private static BodyPart part = BodyPart.RIGHT_ARM;
    private final Angle[] sliders = new Angle[3];
    private TimelineSlider timeline;
    private boolean dirty, live;
    private float lastCursor = -1;
    private int lw, rx, rw, keysTop, bottomTop;

    public PoseEditorScreen(UUID actor) {
        super(Component.translatable("cinematic.pose.title"));
        this.actor = actor;
    }

    private Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private Button btn(Component label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(label, b -> action.run()).bounds(x, y, w, 20).build());
    }

    @Override
    protected void init() {
        if (ClientStudio.editActor() == null || !ClientStudio.editActor().equals(actor)) ClientStudio.beginEdit(actor);
        StudioCamera.setOrbit(true);
        ClientStudio.previewing = false;
        int m = Ui.MARGIN, p = Ui.pitch(height);
        lw = 112;
        rw = 184;
        rx = width - m - rw;
        bottomTop = height - 72;

        int y = Ui.TOP + 15;
        for (BodyPart bp : BodyPart.values()) {
            Component label = tr(bp.key());
            if (bp == part) label = Component.literal("> ").append(label).append(" <");
            btn(label, m, y, lw, () -> {
                part = bp;
                live = false;
                ClientStudio.setLive(part, null);
                rebuildWidgets();
                loadSliders();
            });
            y += p;
        }

        String[] axes = {"x", "y", "z"};
        y = Ui.TOP + 15;
        for (int i = 0; i < 3; i++) {
            sliders[i] = addRenderableWidget(new Angle(rx, y, rw, axes[i]));
            y += p;
        }
        keysTop = y + 8;
        y = keysTop + 15;
        int hw = (rw - Ui.GAP) / 2;
        btn(tr("cinematic.pose.set_key"), rx, y, hw, this::setKey);
        btn(tr("cinematic.pose.delete_key"), rx + hw + Ui.GAP, y, hw, this::deleteKey);
        y += p;
        btn(tr("cinematic.pose.prev_key"), rx, y, hw, () -> jumpKey(-1));
        btn(tr("cinematic.pose.next_key"), rx + hw + Ui.GAP, y, hw, () -> jumpKey(1));

        int ty = bottomTop + 15;
        timeline = addRenderableWidget(new TimelineSlider(m, ty, width - 2 * m, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int n = 5, w = (width - 2 * m - (n - 1) * Ui.GAP) / n, x = m, by = ty + 26;
        btn(tr("cinematic.pose.play"), x, by, w, () -> {
            if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
            ClientStudio.previewing = !ClientStudio.previewing;
        });
        x += w + Ui.GAP;
        btn(tr("cinematic.pose.reset_angles"), x, by, w, () -> {
            for (Angle a : sliders) a.set(0);
            changed();
        });
        x += w + Ui.GAP;
        btn(tr("cinematic.pose.clear_part"), x, by, w, () -> {
            ClientStudio.editKeys().removeIf(k -> k.part() == part);
            dirty = true;
            loadSliders();
        });
        x += w + Ui.GAP;
        btn(tr("cinematic.pose.save"), x, by, w, () -> {
            ClientStudio.saveEdit();
            dirty = false;
        }).active = ClientStudio.canDirect();
        x += w + Ui.GAP;
        btn(tr(dirty ? "cinematic.pose.close_unsaved" : "cinematic.pose.close"), x, by, w, this::onClose);
        loadSliders();
    }

    private int now() {
        return Math.round(ClientStudio.cursor);
    }

    /** The sliders show the pose at the cursor. */
    private void loadSliders() {
        float[] v = new float[3];
        if (!Track.poseOffset(ClientStudio.editKeys(), part, ClientStudio.cursor, v)) v = new float[3];
        for (int i = 0; i < 3; i++) sliders[i].set(v[i]);
        live = false;
        ClientStudio.setLive(part, null);
        lastCursor = ClientStudio.cursor;
    }

    private void changed() {
        live = true;
        ClientStudio.setLive(part, new float[] {sliders[0].degrees(), sliders[1].degrees(), sliders[2].degrees()});
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            rebuildWidgets();
        }
    }

    private void setKey() {
        List<PoseKey> keys = ClientStudio.editKeys();
        int t = now();
        PoseKey k = new PoseKey(t, part, sliders[0].degrees(), sliders[1].degrees(), sliders[2].degrees());
        keys.removeIf(o -> o.tick() == t && o.part() == part);
        keys.add(k);
        keys.sort((a, b) -> a.tick() != b.tick() ? Integer.compare(a.tick(), b.tick()) : a.part().compareTo(b.part()));
        markDirty();
        loadSliders();
    }

    private void deleteKey() {
        int t = now();
        if (ClientStudio.editKeys().removeIf(k -> k.tick() == t && k.part() == part)) markDirty();
        loadSliders();
    }

    private void jumpKey(int dir) {
        int t = now();
        Integer best = null;
        for (PoseKey k : ClientStudio.editKeys()) {
            if (k.part() != part) continue;
            if (dir < 0 && k.tick() < t && (best == null || k.tick() > best)) best = k.tick();
            if (dir > 0 && k.tick() > t && (best == null || k.tick() < best)) best = k.tick();
        }
        if (best != null) {
            ClientStudio.previewing = false;
            ClientStudio.cursor = best;
            loadSliders();
        }
    }

    @Override
    public void tick() {
        if (!timeline.isFocused() || ClientStudio.previewing) timeline.sync();
        // Moving in time shows the pose there (a change not set as a key is dropped)
        if (ClientStudio.cursor != lastCursor) loadSliders();
        int[] others = ClientStudio.editKeys().stream().filter(k -> k.part() != part).mapToInt(PoseKey::tick).toArray();
        int[] mine = ClientStudio.editKeys().stream().filter(k -> k.part() == part).mapToInt(PoseKey::tick).toArray();
        timeline.setMarks(others, mine);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            int step = (mods & GLFW.GLFW_MOD_SHIFT) != 0 ? 20 : 1;
            ClientStudio.previewing = false;
            ClientStudio.cursor = Math.max(0, Math.min(ClientStudio.sceneLength(ClientStudio.selectedScene),
                    now() + (key == GLFW.GLFW_KEY_LEFT ? -step : step)));
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE) {
            ClientStudio.previewing = !ClientStudio.previewing;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            setKey();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            StudioCamera.orbitDrag(dx, dy);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        StudioCamera.orbitZoom(delta);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int m = Ui.MARGIN;
        var info = ClientStudio.actor(actor);
        Ui.header(g, font, width, title.copy().append(info == null ? "" : " : " + info.name()), tr("cinematic.pose.subtitle"));
        int colH = bottomTop - 12 - Ui.TOP;
        Ui.panel(g, m, Ui.TOP, lw, colH);
        Ui.panel(g, rx, Ui.TOP, rw, colH);
        Ui.panel(g, m, bottomTop, width - 2 * m, 61);
        Ui.section(g, font, m, Ui.TOP, lw, tr("cinematic.pose.section_part"));
        Ui.section(g, font, rx, Ui.TOP, rw, tr("cinematic.pose.section_angles", tr(part.key())));
        Ui.section(g, font, rx, keysTop, rw, tr("cinematic.pose.section_keys", TimelineSlider.time(ClientStudio.cursor)));
        Ui.section(g, font, m, bottomTop, width - 2 * m, tr("cinematic.pose.section_time"));
        // Between the columns, over the world: how to look around, and the state of the edit
        int cx = (m + lw + rx) / 2;
        int gap = rx - (m + lw) - 28;
        var lines = font.split(tr("cinematic.pose.hint_view"), Math.max(40, gap));
        int hy = bottomTop - 16 - lines.size() * 10;
        for (var line : lines) {
            g.drawCenteredString(font, line, cx, hy, 0xFFFFFFFF);
            hy += 10;
        }
        if (live) g.drawCenteredString(font, tr("cinematic.pose.live"), cx, Ui.TOP + 4, 0xFFFFC060);
        else if (dirty) g.drawCenteredString(font, tr("cinematic.pose.unsaved"), cx, Ui.TOP + 4, 0xFFFFC060);
        super.render(g, mx, my, pt);
    }

    @Override
    public void removed() {
        StudioCamera.setOrbit(false);
        ClientStudio.endEdit();
        ClientStudio.previewing = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** -180..180 degrees. */
    private final class Angle extends AbstractSliderButton {
        private final String axis;

        Angle(int x, int y, int w, String axis) {
            super(x, y, w, 20, Component.empty(), 0.5);
            this.axis = axis;
            updateMessage();
        }

        float degrees() {
            return Math.round((float) (value * 360.0 - 180.0));
        }

        void set(float deg) {
            value = Math.max(0, Math.min(1, (deg + 180.0) / 360.0));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (axis == null) return;
            setMessage(Component.translatable("cinematic.pose.axis_" + axis, String.format(Locale.ROOT, "%.0f", degrees())));
        }

        @Override
        protected void applyValue() {
            changed();
        }
    }
}
