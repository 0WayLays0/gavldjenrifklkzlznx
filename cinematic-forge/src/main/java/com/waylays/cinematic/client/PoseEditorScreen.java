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
 * (right-drag, wheel to zoom); the world stays visible.
 */
public class PoseEditorScreen extends Screen {
    private final UUID actor;
    private static BodyPart part = BodyPart.RIGHT_ARM;
    private final Angle[] sliders = new Angle[3];
    private TimelineSlider timeline;
    private boolean dirty, live;
    private float lastCursor = -1;

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
        int y = 28;
        for (BodyPart p : BodyPart.values()) {
            Component label = tr(p.key());
            if (p == part) label = Component.literal("> ").append(label);
            btn(label, 8, y, 100, () -> {
                part = p;
                live = false;
                ClientStudio.setLive(part, null);
                rebuildWidgets();
                loadSliders();
            });
            y += 22;
        }
        int rx = width - 168;
        String[] axes = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) {
            sliders[i] = addRenderableWidget(new Angle(rx, 28 + i * 24, 160, axes[i]));
        }
        int by = 28 + 3 * 24 + 4, hw = 78;
        btn(tr("cinematic.pose.set_key"), rx, by, hw, this::setKey);
        btn(tr("cinematic.pose.delete_key"), rx + 82, by, hw, this::deleteKey);
        btn(tr("cinematic.pose.prev_key"), rx, by + 22, hw, () -> jumpKey(-1));
        btn(tr("cinematic.pose.next_key"), rx + 82, by + 22, hw, () -> jumpKey(1));
        btn(tr("cinematic.pose.reset_angles"), rx, by + 44, hw, () -> {
            for (Angle a : sliders) a.set(0);
            changed();
        });
        btn(tr("cinematic.pose.clear_part"), rx + 82, by + 44, hw, () -> {
            ClientStudio.editKeys().removeIf(k -> k.part() == part);
            dirty = true;
            loadSliders();
        });
        timeline = addRenderableWidget(new TimelineSlider(8, height - 50, width - 16, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int bw = Math.min(120, (width - 16 - 9) / 4), x = 8, bottom = height - 26;
        btn(tr("cinematic.pose.play"), x, bottom, bw, () -> {
            if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
            ClientStudio.previewing = !ClientStudio.previewing;
        });
        x += bw + 3;
        btn(tr("cinematic.pose.save"), x, bottom, bw, () -> {
            ClientStudio.saveEdit();
            dirty = false;
        }).active = ClientStudio.canDirect();
        x += bw + 3;
        btn(tr("cinematic.pose.undo_all"), x, bottom, bw, () -> {
            ClientStudio.beginEdit(actor);
            dirty = false;
            loadSliders();
        });
        x += bw + 3;
        btn(tr("cinematic.pose.close"), x, bottom, bw, this::onClose);
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

    private void setKey() {
        List<PoseKey> keys = ClientStudio.editKeys();
        int t = now();
        keys.removeIf(k -> k.tick() == t && k.part() == part);
        keys.add(new PoseKey(t, part, sliders[0].degrees(), sliders[1].degrees(), sliders[2].degrees()));
        keys.sort((a, b) -> a.tick() != b.tick() ? Integer.compare(a.tick(), b.tick()) : a.part().compareTo(b.part()));
        dirty = true;
        loadSliders();
    }

    private void deleteKey() {
        int t = now();
        if (ClientStudio.editKeys().removeIf(k -> k.tick() == t && k.part() == part)) dirty = true;
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
        if (ClientStudio.cursor != lastCursor && !live) loadSliders();
        else if (ClientStudio.cursor != lastCursor) {
            // Moving in time drops an unset change
            loadSliders();
        }
        int[] all = ClientStudio.editKeys().stream().filter(k -> k.part() != part).mapToInt(PoseKey::tick).toArray();
        int[] mine = ClientStudio.editKeys().stream().filter(k -> k.part() == part).mapToInt(PoseKey::tick).toArray();
        timeline.setMarks(all, mine);
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
        g.fill(0, 0, 116, height - 72, 0x70000000);
        g.fill(width - 176, 0, width, height - 72, 0x70000000);
        g.fill(0, height - 72, width, height, 0x70000000);
        var info = ClientStudio.actor(actor);
        g.drawString(font, title.copy().append(info == null ? "" : " - " + info.name()), 8, 8, 0xFFFFFF);
        g.drawString(font, tr("cinematic.pose.at", TimelineSlider.time(ClientStudio.cursor), tr(part.key())), width - 168, 12, 0xFFE070);
        g.drawCenteredString(font, tr("cinematic.pose.hint"), width / 2, height - 66, 0xC0C0C0);
        if (dirty) g.drawString(font, tr("cinematic.pose.unsaved"), 8, 8 + 10, 0xFFC060);
        if (live) g.drawString(font, tr("cinematic.pose.live"), width - 168, 28 + 3 * 24 + 4 + 68, 0xFFC060);
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
            setMessage(Component.translatable("cinematic.pose.axis_" + axis, String.format(Locale.ROOT, "%.0f", degrees())));
        }

        @Override
        protected void applyValue() {
            changed();
        }
    }
}
