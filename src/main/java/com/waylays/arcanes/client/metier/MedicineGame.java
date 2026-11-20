package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Medicine at the medical table: a gash across the patient's forearm, to be stitched. Each stitch is a point on one
 * side of the wound then the other; a ring closes on the point to pierce: click it (or press space) on the point as
 * the ring meets it. Sure and in time, the thread draws the lips together; early, late or beside it, the patient
 * winces. The wound bleeds all the while, a little less with each stitch: too much blood, or too much pain and the
 * patient faints, and it is a failure.
 */
public class MedicineGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_medicine");
    /** The forearm across the scene: its ends, its middle, half its width. */
    private static final float ARM_X0 = 22, ARM_X1 = 398, ARM_Y = 140, ARM_HALF = 46;
    /** The rings: their size when they appear, the size of a stitch's point. */
    private static final float RING0 = 26, POINT = 6;

    /** A stitch's point: where, when its ring closes, and how it went (-1 not yet, 0 missed, else its quality). */
    private static final class Point {
        final float x, y, due;
        float quality = -1;

        Point(float x, float y, float due) {
            this.x = x;
            this.y = y;
            this.due = due;
        }
    }

    private final List<Point> points = new ArrayList<>();
    private final float window, bleedRate;
    private final float[] woundX, woundY;
    private int next;
    private float pain, bleed, maxPain, quality, beat, mouseX = -100, mouseY = -100;
    private float lastBeat;
    private final Draw.Bits bits = new Draw.Bits();
    /** Drops of blood running down from the wound: x, y, when. */
    private final List<float[]> drops = new ArrayList<>();
    private float dropDebt;

    public MedicineGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                        Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF3E9A86);
        float d = (float) Math.max(.3, difficulty);
        this.window = 1.35f / (float) Math.sqrt(d);
        this.bleedRate = .022f * d;
        // The wound: a jagged gash slanting across the arm
        int n = 24;
        woundX = new float[n];
        woundY = new float[n];
        float x0 = 96, x1 = 324, tilt = (random.nextFloat() - .5f) * 30;
        for (int i = 0; i < n; i++) {
            float t = i / (n - 1f);
            woundX[i] = Mth.lerp(t, x0, x1);
            woundY[i] = ARM_Y + tilt * (t - .5f) * 2 + 6 * Mth.sin(t * 7 + random.nextFloat()) + (random.nextFloat() - .5f) * 3;
        }
        // The stitches along it: each a point above then below, in turn
        int stitches = Math.round(5 + 1.5f * d);
        float gap = window * .78f;
        float at = 1.2f;
        for (int s = 0; s < stitches; s++) {
            float t = (s + .5f) / stitches;
            float wx = Mth.lerp(t, x0 + 6, x1 - 6);
            float wy = woundAt(wx);
            float side = 13 + random.nextFloat() * 3;
            points.add(new Point(wx - 3, wy - side, at += gap));
            points.add(new Point(wx + 3, wy + side, at += gap * .8f));
        }
    }

    private float woundAt(float x) {
        for (int i = 1; i < woundX.length; i++) {
            if (x <= woundX[i]) {
                float t = (x - woundX[i - 1]) / Math.max(.01f, woundX[i] - woundX[i - 1]);
                return Mth.lerp(t, woundY[i - 1], woundY[i]);
            }
        }
        return woundY[woundY.length - 1];
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return List.of(control("click", "medicine.ctl_stitch"));
    }

    @Override
    protected float liveScore() {
        int done = 0;
        for (Point p : points) {
            if (p.quality >= 0) {
                done++;
            }
        }
        float average = done == 0 ? 1 : quality / done;
        float progress = done / (float) points.size();
        return Mth.clamp(average * (.3f + .7f * progress) - maxPain * .2f - Math.max(0, bleed - .5f) * .2f, 0, 1);
    }

    /** How big the ring of {@code p} is now (POINT when it should be pierced). */
    private float ring(Point p) {
        return POINT + (RING0 - POINT) * (p.due - time) / window;
    }

    @Override
    protected void update(float dt) {
        float bpm = 70 + 80 * pain + 30 * bleed;
        beat += dt * bpm / 60;
        if (beat >= 1) {
            beat -= 1;
            lastBeat = time;
            if (playing() && (pain > .5f || bleed > .6f)) {
                sound(SoundEvents.WARDEN_HEARTBEAT, 1.2f, .25f + .4f * Math.max(pain, bleed));
            }
        }
        if (!playing()) {
            return;
        }
        pain = Math.max(0, pain - dt * .05f);
        int stitched = 0;
        for (Point p : points) {
            if (p.quality > 0) {
                stitched++;
            }
        }
        float closing = stitched / (float) points.size();
        bleed = Math.min(1, bleed + dt * bleedRate * (1.2f - closing));
        // Blood: drops welling up along the open part of the wound
        dropDebt += dt * (1 + 6 * bleed) * (1 - closing);
        while (dropDebt >= 1) {
            dropDebt--;
            float x = Mth.lerp(random.nextFloat(), woundX[0], woundX[woundX.length - 1]);
            drops.add(new float[]{x, woundAt(x) + 2, time, .8f + random.nextFloat() * 1.2f});
        }
        drops.removeIf(d -> time - d[2] > 2.6f);
        // A ring that closed with nothing done: missed
        while (next < points.size() && time - points.get(next).due > window * .3f) {
            miss(points.get(next), false);
        }
        if (bleed >= 1) {
            finish(liveScore() * .4f, true, Component.translatable("metier.arcanes_oublies.medicine.blood"));
        } else if (pain >= 1) {
            finish(liveScore() * .4f, true, Component.translatable("metier.arcanes_oublies.medicine.fainted"));
        } else if (next >= points.size()) {
            flash(Component.translatable("metier.arcanes_oublies.medicine.closed"), 0xFFE0FFF0);
            finish(liveScore(), false, null);
        }
    }

    private void miss(Point p, boolean beside) {
        p.quality = 0;
        next++;
        pain = Math.min(1, pain + (beside ? .2f : .16f));
        maxPain = Math.max(maxPain, pain);
        shake(2);
        flash(Component.translatable(beside ? "metier.arcanes_oublies.medicine.beside" : "metier.arcanes_oublies.medicine.missed"), 0xFFFF9080);
        sound(SoundEvents.PLAYER_HURT, 1.3f, .35f);
    }

    /** The needle goes in at {@code x, y}. */
    private void pierce(float x, float y) {
        if (!playing() || next >= points.size()) {
            return;
        }
        Point p = points.get(next);
        float dist = Mth.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y));
        float r = ring(p);
        if (r > RING0 * .95f) {
            // Too early for this one: the needle goes in for nothing
            pain = Math.min(1, pain + .08f);
            maxPain = Math.max(maxPain, pain);
            flash(Component.translatable("metier.arcanes_oublies.medicine.early"), 0xFFFFC080);
            return;
        }
        if (dist > 12) {
            miss(p, true);
            return;
        }
        float timing = Mth.clamp(1 - Math.abs(r - POINT) / (RING0 * .5f), 0, 1);
        float aim = Mth.clamp(1 - dist / 12, 0, 1);
        float q = Mth.clamp(.25f + .5f * timing + .25f * aim, 0, 1);
        p.quality = q;
        quality += q;
        next++;
        bleed = Math.max(0, bleed - .05f);
        if (q > .85f) {
            flash(Component.translatable("metier.arcanes_oublies.medicine.neat"), 0xFFB0FFD0);
        }
        bits.add(time, p.x, p.y, 0, -20, .4f, 6, 0xFFFFFFFF, 0, true);
        sound(SoundEvents.WOOL_PLACE, 1.4f + q * .4f, .6f);
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (button == 0) {
            pierce((float) mx, (float) my);
        }
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            pierce(mouseX, mouseY);
        }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        mouseX = mx;
        mouseY = my;
        drawArm(g);
        drawWound(g);
        drawStitches(g);
        // The next points: the one to pierce with its ring, the one after faintly
        for (int i = next; i < Math.min(points.size(), next + 2); i++) {
            Point p = points.get(i);
            float r = ring(p);
            boolean now = i == next;
            if (r > RING0 * 1.4f) {
                continue;
            }
            float a = now ? 1 : .35f;
            Draw.glow(g, p.x, p.y, 12, Draw.argb((int) (90 * a), 0xFFB0FFD0));
            Draw.disc(g, p.x, p.y, 2.2f, Draw.argb((int) (255 * a), 0xFF1E6A4A));
            if (now) {
                float rr = Math.max(POINT * .6f, r);
                boolean good = Math.abs(r - POINT) < 4;
                Draw.ring(g, p.x, p.y, rr, Draw.argb(220, good ? 0xFF7AFFB0 : 0xFFFFFFFF), false);
                Draw.ring(g, p.x, p.y, POINT, 0x80FFFFFF, false);
            }
        }
        bits.draw(g, time);
        drawNeedle(g, mx, my);
        drawMonitor(g);
    }

    private void drawArm(GuiGraphics g) {
        float y0 = ARM_Y - ARM_HALF, y1 = ARM_Y + ARM_HALF;
        Draw.shadow(g, (ARM_X0 + ARM_X1) / 2, y1 + 8, ARM_X1 - ARM_X0, 26, 160);
        // Skin: lit from above, rounded at its edges
        Draw.vgrad(g, ARM_X0, y0, ARM_X1, ARM_Y, 0xFFE8B898, 0xFFDCA484);
        Draw.vgrad(g, ARM_X0, ARM_Y, ARM_X1, y1, 0xFFDCA484, 0xFFB07858);
        Draw.rect(g, ARM_X0, y0, ARM_X1, y0 + 1.5f, 0x50FFFFFF);
        Draw.vgrad(g, ARM_X0, y1 - 8, ARM_X1, y1, 0x00000000, 0x40501810);
        // A rolled-up linen sleeve at one end, a clean cloth under the other
        Draw.vgrad(g, ARM_X0 - 6, y0 - 6, ARM_X0 + 34, y1 + 6, 0xFFF4F0E6, 0xFFC8C0B0);
        for (int i = 0; i < 4; i++) {
            Draw.rect(g, ARM_X0 + 4 + i * 8, y0 - 6, ARM_X0 + 5 + i * 8, y1 + 6, 0x30000000);
        }
        // Fine hair and freckles, for the skin to read as skin
        for (int i = 0; i < 40; i++) {
            float x = ARM_X0 + 40 + (i * 97 % 330), y = y0 + 6 + (i * 53 % (int) (ARM_HALF * 2 - 12));
            Draw.disc(g, x, y, .7f, 0x40804030);
        }
    }

    private void drawWound(GuiGraphics g) {
        int stitched = 0;
        for (Point p : points) {
            if (p.quality > 0) {
                stitched++;
            }
        }
        float open = 1 - .7f * stitched / (float) points.size();
        // Bruised skin round it, the open lips, the dark of the cut
        for (int i = 1; i < woundX.length; i++) {
            Draw.line(g, woundX[i - 1], woundY[i - 1], woundX[i], woundY[i], 10 * open + 3, 0x30802020);
        }
        for (int i = 1; i < woundX.length; i++) {
            float t = i / (float) woundX.length;
            float w = (2 + 4 * Mth.sin(Mth.PI * t)) * open + 1;
            Draw.line(g, woundX[i - 1], woundY[i - 1], woundX[i], woundY[i], w + 1.6f, 0xFFC04A48);
            Draw.line(g, woundX[i - 1], woundY[i - 1], woundX[i], woundY[i], w, 0xFF6A0E14);
        }
        // Blood running from it
        for (float[] d : drops) {
            float age = time - d[2];
            float len = Math.min(d[3] * 14, age * 18);
            float a = age > 2 ? 1 - (age - 2) / .6f : 1;
            Draw.line(g, d[0], d[1], d[0] + .6f, d[1] + len, 1.8f, Draw.argb((int) (220 * a), 0xFF8A0E16));
            Draw.disc(g, d[0] + .6f, d[1] + len, 1.6f, Draw.argb((int) (230 * a), 0xFFA01A20));
        }
    }

    private void drawStitches(GuiGraphics g) {
        // The thread: from each point to the next one stitched, across the wound, a knot at each pair's end
        Point prev = null;
        for (int i = 0; i < next && i < points.size(); i++) {
            Point p = points.get(i);
            if (p.quality <= 0) {
                prev = null;
                continue;
            }
            if (prev != null) {
                int c = Draw.lerp(0xFF2A3A6A, 0xFF101828, 1 - p.quality);
                Draw.line(g, prev.x, prev.y, p.x, p.y, 1.6f, c);
                Draw.line(g, prev.x, prev.y - .6f, p.x, p.y - .6f, .6f, 0x70FFFFFF);
            }
            Draw.disc(g, p.x, p.y, 1.8f, 0xFF1A2440);
            if (i % 2 == 1) {
                Draw.disc(g, p.x + 2.5f, p.y + 1.5f, 1.6f, 0xFF1A2440);
            }
            prev = i % 2 == 0 ? p : null;
        }
    }

    private void drawNeedle(GuiGraphics g, float mx, float my) {
        if (!inArea(mx, my)) {
            return;
        }
        // The thread trailing back to the last stitch
        for (int i = Math.min(next, points.size()) - 1; i >= 0; i--) {
            if (points.get(i).quality > 0) {
                Point p = points.get(i);
                float cx = (p.x + mx) / 2, cy = Math.max(p.y, my) + 18;
                float px = p.x, py = p.y;
                for (int s = 1; s <= 10; s++) {
                    float t = s / 10f;
                    float x = (1 - t) * (1 - t) * p.x + 2 * (1 - t) * t * cx + t * t * mx;
                    float y = (1 - t) * (1 - t) * p.y + 2 * (1 - t) * t * cy + t * t * my;
                    Draw.line(g, px, py, x, y, 1, 0xC02A3A6A);
                    px = x;
                    py = y;
                }
                break;
            }
        }
        // A curved surgeon's needle, its point at the mouse
        float px = mx, py = my;
        for (int s = 1; s <= 8; s++) {
            float a = Mth.PI + s * Mth.PI / 9;
            float x = mx + 9 + 9 * Mth.cos(a), y = my - 9 * Mth.sin(a) * .6f;
            Draw.line(g, px, py, x, y, 1.4f, 0xFFD8DCE4);
            px = x;
            py = y;
        }
        Draw.disc(g, mx, my, 1, 0xFFFFFFFF);
    }

    /** A heart monitor in the corner: its trace, faster with pain and loss of blood; the two gauges under it. */
    private void drawMonitor(GuiGraphics g) {
        float x0 = AX1 - 104, y0 = AY0 + 8, x1 = AX1 - 8, y1 = AY0 + 42;
        Draw.rect(g, x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFF2A3030);
        Draw.rect(g, x0, y0, x1, y1, 0xFF081410);
        int color = pain > .6f || bleed > .7f ? 0xFFFF6A5A : 0xFF6AFF9A;
        float px = x0, py = (y0 + y1) / 2;
        for (float x = x0; x <= x1; x += 1.5f) {
            float phase = (x - x0) / (x1 - x0) * 3 - (time - lastBeat) * 2.2f;
            float f = phase - (float) Math.floor(phase);
            float y = (y0 + y1) / 2;
            if (f > .45f && f < .5f) {
                y -= 12;
            } else if (f >= .5f && f < .55f) {
                y += 7;
            } else if (f > .65f && f < .75f) {
                y -= 2.5f;
            }
            Draw.line(g, px, py, x, y, 1, color);
            px = x;
            py = y;
        }
        Draw.glow(g, x1 - 6, y0 + 6, 6, Draw.argb((int) (200 * Math.max(0, 1 - (time - lastBeat) * 4)), color));
        float gx0 = x0, gx1 = x1;
        Draw.text(g, Component.translatable("metier.arcanes_oublies.medicine.pain"), gx0, y1 + 5, 0xFFF0DCC0, .7f);
        Draw.pill(g, gx0 + 36, y1 + 6, gx1, y1 + 11, 0xFF2E2420);
        Draw.pill(g, gx0 + 36, y1 + 6, gx0 + 36 + (gx1 - gx0 - 36) * pain, y1 + 11, 0xFFFFA060);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.medicine.bleeding"), gx0, y1 + 15, 0xFFF0DCC0, .7f);
        Draw.pill(g, gx0 + 36, y1 + 16, gx1, y1 + 21, 0xFF2E2420);
        Draw.pill(g, gx0 + 36, y1 + 16, gx0 + 36 + (gx1 - gx0 - 36) * bleed, y1 + 21, 0xFFD0303A);
    }
}
