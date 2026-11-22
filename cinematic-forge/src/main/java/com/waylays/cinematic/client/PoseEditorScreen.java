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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Retouches a take joint by joint. Pick a moment on the timeline, click a joint on the figure, move its sliders: the
 * pose is kept at that moment (a key) and the actor moves smoothly from key to key, on top of what was recorded.
 * The camera turns around the actor (right-drag, wheel to zoom); the world stays visible in the middle.
 */
public class PoseEditorScreen extends Screen {
    private final UUID actor;
    private static BodyPart part = BodyPart.RIGHT_ARM;
    private final List<Angle> sliders = new ArrayList<>();
    private TimelineSlider timeline;
    private Mannequin mannequin;
    private boolean dirty;
    private Button saveButton, closeButton;
    private float lastCursor = -1;
    private int lw, rx, rw, bottomTop, stateY;

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
        ClientStudio.setLive(part, null);
        StudioCamera.setOrbit(true);
        int m = Ui.MARGIN, p = Ui.pitch(height);
        lw = 120;
        rw = 200;
        rx = width - m - rw;
        bottomTop = height - 72;
        mannequin = new Mannequin(m, Ui.TOP + 16, lw, bottomTop - 12 - Ui.TOP - 30);

        // The joint's movements
        sliders.clear();
        int y = Ui.TOP + 15;
        for (Joints.Move mv : Joints.moves(part)) {
            sliders.add(addRenderableWidget(new Angle(rx, y, rw, mv)));
            y += p;
        }
        y += 4;
        int hw = (rw - Ui.GAP) / 2;
        btn(tr("cinematic.pose.straighten"), rx, y, hw, () -> {
            for (Angle a : sliders) a.setShown(0);
            keyNow();
        });
        btn(tr("cinematic.pose.delete_key"), rx + hw + Ui.GAP, y, hw, this::deleteKey);
        y += p;
        btn(tr("cinematic.pose.prev_key"), rx, y, hw, () -> jumpKey(-1));
        btn(tr("cinematic.pose.next_key"), rx + hw + Ui.GAP, y, hw, () -> jumpKey(1));
        stateY = y + 24;

        // Time
        int ty = bottomTop + 15;
        timeline = addRenderableWidget(new TimelineSlider(m, ty, width - 2 * m, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int n = 5, w = (width - 2 * m - (n - 1) * Ui.GAP) / n, x = m, by = ty + 26;
        btn(tr("cinematic.pose.back_1s"), x, by, w, () -> step(-20));
        x += w + Ui.GAP;
        btn(tr("cinematic.pose.play"), x, by, w, () -> {
            if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
            ClientStudio.previewing = !ClientStudio.previewing;
        });
        x += w + Ui.GAP;
        btn(tr("cinematic.pose.forward_1s"), x, by, w, () -> step(20));
        x += w + Ui.GAP;
        saveButton = btn(tr("cinematic.pose.save"), x, by, w, () -> {
            ClientStudio.saveEdit();
            dirty = false;
            updateButtons();
        });
        x += w + Ui.GAP;
        closeButton = btn(tr("cinematic.pose.close"), x, by, w, this::onClose);
        updateButtons();
        loadSliders();
    }

    private int now() {
        return Math.round(ClientStudio.cursor);
    }

    private void step(int ticks) {
        ClientStudio.previewing = false;
        ClientStudio.cursor = Math.max(0, Math.min(ClientStudio.sceneLength(ClientStudio.selectedScene), now() + ticks));
    }

    private PoseKey keyHere() {
        int t = now();
        for (PoseKey k : ClientStudio.editKeys()) if (k.tick() == t && k.part() == part) return k;
        return null;
    }

    /** The sliders show the pose at the cursor. */
    private void loadSliders() {
        float[] v = new float[3];
        if (!Track.poseOffset(ClientStudio.editKeys(), part, ClientStudio.cursor, v)) v = new float[3];
        for (Angle a : sliders) a.setStored(v[a.move.comp()]);
        lastCursor = ClientStudio.cursor;
    }

    /** Moving a slider keeps the joint's pose at this moment: a key is set (or updated) at once. */
    private void keyNow() {
        float[] v = new float[3];
        for (Angle a : sliders) v[a.move.comp()] = a.stored();
        int t = now();
        List<PoseKey> keys = ClientStudio.editKeys();
        keys.removeIf(k -> k.tick() == t && k.part() == part);
        keys.add(new PoseKey(t, part, v[0], v[1], v[2]));
        keys.sort((a, b) -> a.tick() != b.tick() ? Integer.compare(a.tick(), b.tick()) : a.part().compareTo(b.part()));
        markDirty();
    }

    private void markDirty() {
        dirty = true;
        updateButtons();
    }

    /** Changed in place: rebuilding the screen would drop the slider being dragged. */
    private void updateButtons() {
        saveButton.active = ClientStudio.canDirect() && dirty;
        closeButton.setMessage(tr(dirty ? "cinematic.pose.close_unsaved" : "cinematic.pose.close"));
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
        if (ClientStudio.cursor != lastCursor) loadSliders();
        int[] others = ClientStudio.editKeys().stream().filter(k -> k.part() != part).mapToInt(PoseKey::tick).toArray();
        int[] mine = ClientStudio.editKeys().stream().filter(k -> k.part() == part).mapToInt(PoseKey::tick).toArray();
        timeline.setMarks(others, mine);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        BodyPart picked = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? mannequin.at(mx, my) : null;
        if (picked != null) {
            part = picked;
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            step(((mods & GLFW.GLFW_MOD_SHIFT) != 0 ? 20 : 1) * (key == GLFW.GLFW_KEY_LEFT ? -1 : 1));
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE) {
            ClientStudio.previewing = !ClientStudio.previewing;
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
        Ui.section(g, font, rx, Ui.TOP, rw, tr(part.key()));
        Ui.section(g, font, m, bottomTop, width - 2 * m, tr("cinematic.pose.section_time"));

        Set<BodyPart> withKeys = EnumSet.noneOf(BodyPart.class);
        for (PoseKey k : ClientStudio.editKeys()) withKeys.add(k.part());
        mannequin.render(g, font, part, withKeys, mx, my);
        g.drawCenteredString(font, tr("cinematic.pose.front_view"), m + lw / 2, mannequin.bottom() + 4, Ui.HINT);

        // Is there a key at this moment, under the joint's buttons
        PoseKey here = keyHere();
        Component state = here != null ? tr("cinematic.pose.key_here", TimelineSlider.time(here.tick())) : tr("cinematic.pose.no_key_here");
        g.drawWordWrap(font, state, rx, stateY, rw, here != null ? 0xFF80FF80 : Ui.HINT);
        // How to look around, on the right of the time section's title
        Component hint = tr("cinematic.pose.hint_view");
        g.drawString(font, hint, width - m - font.width(hint), bottomTop, Ui.HINT);
        super.render(g, mx, my, pt);
        BodyPart hover = mannequin.at(mx, my);
        if (hover != null && hover != part) g.renderTooltip(font, tr(hover.key()), mx, my);
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

    /** One movement of the joint, within its natural limits. */
    private final class Angle extends AbstractSliderButton {
        final Joints.Move move;

        Angle(int x, int y, int w, Joints.Move move) {
            super(x, y, w, 20, Component.empty(), 0.5);
            this.move = move;
            setShown(0);
        }

        float shown() {
            return Math.round((float) (move.min() + value * (move.max() - move.min())));
        }

        float stored() {
            return shown() * move.sign();
        }

        void setShown(float deg) {
            value = Math.max(0, Math.min(1, (deg - move.min()) / (move.max() - move.min())));
            updateMessage();
        }

        void setStored(float deg) {
            setShown(deg * move.sign());
        }

        @Override
        protected void updateMessage() {
            if (move == null) return;
            setMessage(Component.translatable(move.key(), String.format(Locale.ROOT, "%.0f", shown())));
        }

        @Override
        protected void applyValue() {
            keyNow();
        }
    }
}
