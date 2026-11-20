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
 * Carpentry at the saw: a plank lies on two trestles, a pencil line drawn along it. Saw it: hold the left button and
 * move the mouse left and right (or press A and D in turn), each stroke cutting further; steer the blade up and down
 * with the mouse (or W and S) to keep it on the line. The kerf left behind shows how true the cut was. Stray too far
 * and the wood splits along its grain; three bad strokes and the plank is spoilt.
 */
public class CarpentryGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_carpentry");
    /** The plank: its ends, its middle, half its width. The cut runs from CX0 to CX1. */
    private static final float PX0 = 34, PX1 = 386, PY = 138, HALF = 24, CX0 = 52, CX1 = 368;
    private static final float LIMIT = 32;

    private final float amp1, amp2, f1, f2, ph1, ph2, tolerance, strokeGain;
    /** How far the cut has gone (0 to 1), the blade's height, where the cut front is. */
    private float progress, bladeY = PY, targetY = PY, mouseX = -1, mouseY = PY;
    private float accuracy, crack, maxCrack, played, strokeOffset;
    /** The current stroke: its direction, where it turned, how far it went; when the last one ended. */
    private int strokeDir;
    private float strokeFrom, lastStroke = -1;
    private boolean sawing, keyLeft;
    private float keySteer;
    private final List<float[]> kerf = new ArrayList<>();
    private final Draw.Bits bits = new Draw.Bits();
    private float cracked = -1;

    public CarpentryGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                         Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF9A6A38);
        float d = (float) Math.sqrt(Math.max(.3, difficulty));
        this.amp1 = Math.min(HALF - 8, 7 * d);
        this.amp2 = Math.min(5, 2.6f * d);
        this.f1 = .018f + random.nextFloat() * .008f;
        this.f2 = .055f + random.nextFloat() * .02f;
        this.ph1 = random.nextFloat() * Mth.TWO_PI;
        this.ph2 = random.nextFloat() * Mth.TWO_PI;
        this.tolerance = 3.4f / d;
        this.strokeGain = (float) (1 / (18 + 6 * difficulty));
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return List.of(control("drag", "carpentry.ctl_saw"), control("mouse", "carpentry.ctl_steer"), control("a_d", "carpentry.ctl_keys"));
    }

    /** The pencil line's height at {@code x}. */
    private float line(float x) {
        return PY + amp1 * Mth.sin(x * f1 + ph1) + amp2 * Mth.sin(x * f2 + ph2);
    }

    private float front() {
        return Mth.lerp(progress, CX0, CX1);
    }

    @Override
    protected float liveScore() {
        float done = Math.max(.0001f, progress);
        float true_ = accuracy / done;
        return Mth.clamp(true_ * (.35f + .65f * progress) - maxCrack * .25f, 0, 1);
    }

    @Override
    protected void update(float dt) {
        // The blade follows the mouse (or the keys), not quite at once
        if (keySteer != 0) {
            targetY = Mth.clamp(targetY + keySteer * 46 * dt, PY - HALF + 3, PY + HALF - 3);
        } else if (mouseX >= 0) {
            targetY = Mth.clamp(mouseY, PY - HALF + 3, PY + HALF - 3);
        }
        bladeY = Mth.lerp(Math.min(1, dt * 14), bladeY, targetY);
        strokeOffset *= (float) Math.pow(.02, dt);
        crack = Math.max(0, crack - dt * .05f);
        if (!playing()) {
            return;
        }
        played += dt;
        if (played >= LIMIT) {
            finish(liveScore() * .8f, false, Component.translatable("metier.arcanes_oublies.carpentry.too_long"));
        }
    }

    /** A stroke of {@code length} (window units): the cut goes on, the blade's place on the line is judged. */
    private void stroke(float length) {
        if (!playing() || progress >= 1) {
            return;
        }
        float rushed = lastStroke >= 0 && time - lastStroke < .12f ? .35f : 1;
        lastStroke = time;
        float gain = strokeGain * Mth.clamp(length / 40f, .3f, 1.2f) * rushed;
        float before = front();
        progress = Math.min(1, progress + gain);
        float after = front();
        float err = Math.abs(bladeY - line(after));
        float quality = Mth.clamp(1 - err / (tolerance * 2.5f), 0, 1);
        accuracy += quality * gain;
        for (float x = before; x <= after; x += 2) {
            kerf.add(new float[]{x, Mth.lerp((x - before) / Math.max(.01f, after - before), kerf.isEmpty() ? bladeY : kerf.get(kerf.size() - 1)[1], bladeY)});
        }
        bits.chips(random, time, after + 4, bladeY, 3, quality > .5f ? 0xFFE8C890 : 0xFFC09A68);
        sound(SoundEvents.AXE_STRIP, .8f + random.nextFloat() * .4f + (strokeDir > 0 ? .1f : 0), .35f);
        if (err > tolerance * 3) {
            // Off the line: the wood starts to split along its grain
            crack += .36f;
            maxCrack = Math.max(maxCrack, crack);
            shake(2.5f);
            flash(Component.translatable("metier.arcanes_oublies.carpentry.off_line"), 0xFFFFB060);
            sound(SoundEvents.WOOD_HIT, .6f, .7f);
            if (crack >= 1) {
                cracked = time;
                sound(SoundEvents.WOOD_BREAK, .7f, 1);
                finish(liveScore() * .4f, true, Component.translatable("metier.arcanes_oublies.carpentry.split"));
                return;
            }
        }
        if (progress >= 1) {
            flash(Component.translatable("metier.arcanes_oublies.carpentry.clean"), 0xFFFFF0C8);
            finish(liveScore(), false, null);
        }
    }

    /** The mouse moves while the button is held: a stroke ends each time it turns back after going far enough. */
    private void track(float mx) {
        if (!sawing) {
            return;
        }
        float moved = mx - mouseX;
        if (Math.abs(moved) < .5f) {
            return;
        }
        int dir = moved > 0 ? 1 : -1;
        if (strokeDir == 0) {
            strokeDir = dir;
            strokeFrom = mouseX;
        } else if (dir != strokeDir) {
            float length = Math.abs(mouseX - strokeFrom);
            if (length >= 14) {
                stroke(length);
            }
            strokeDir = dir;
            strokeFrom = mouseX;
        }
        strokeOffset = Mth.clamp(strokeOffset + moved * .6f, -22, 22);
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (button == 0) {
            sawing = true;
            strokeDir = 0;
        }
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        if (button == 0) {
            if (sawing && strokeDir != 0 && Math.abs(mouseX - strokeFrom) >= 14) {
                stroke(Math.abs(mouseX - strokeFrom));
            }
            sawing = false;
            strokeDir = 0;
        }
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT) {
            if (!keyLeft) {
                keyLeft = true;
                strokeOffset = -18;
                stroke(40);
            }
        } else if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT) {
            if (keyLeft) {
                keyLeft = false;
                strokeOffset = 18;
                stroke(40);
            }
        } else if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) {
            keySteer = -1;
        } else if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) {
            keySteer = 1;
        }
    }

    @Override
    protected void onKeyUp(int key) {
        if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) {
            keySteer = 0;
        }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        if (keySteer == 0 && (mx != mouseX || my != mouseY)) {
            track(mx);
            mouseX = mx;
            mouseY = my;
        }
        drawTrestle(g, 86);
        drawTrestle(g, 334);
        drawPlank(g);
        drawSaw(g);
        bits.draw(g, time);
        // How far the cut has gone, and the split starting
        float bx0 = 120, bx1 = 300, by = AY0 + 12;
        Draw.text(g, Component.translatable("metier.arcanes_oublies.carpentry.cut"), bx0, by - 10, 0xFFF0DCC0, .75f);
        Draw.pill(g, bx0, by, bx1, by + 7, 0xFF2E2420);
        Draw.pill(g, bx0, by, bx0 + (bx1 - bx0) * progress, by + 7, 0xFFE8C890);
        if (maxCrack > 0) {
            Draw.text(g, Component.translatable("metier.arcanes_oublies.carpentry.crack"), bx1 + 10, by - 10, 0xFFFFB090, .75f);
            Draw.pill(g, bx1 + 10, by, bx1 + 80, by + 7, 0xFF2E2420);
            Draw.pill(g, bx1 + 10, by, bx1 + 10 + 70 * Mth.clamp(crack, 0, 1), by + 7, 0xFFE06040);
        }
    }

    private void drawTrestle(GuiGraphics g, float x) {
        float top = PY + HALF + 4;
        Draw.shadow(g, x, AY1 - 6, 70, 14, 150);
        Draw.line(g, x - 22, AY1 - 4, x - 4, top, 5, 0xFF5A3A20);
        Draw.line(g, x + 22, AY1 - 4, x + 4, top, 5, 0xFF4A2E18);
        Draw.rect(g, x - 26, top - 2, x + 26, top + 5, 0xFF6A4628);
        Draw.rect(g, x - 26, top - 2, x + 26, top - 1, 0xFF9A7048);
    }

    private void drawPlank(GuiGraphics g) {
        float y0 = PY - HALF, y1 = PY + HALF;
        Draw.shadow(g, (PX0 + PX1) / 2, y1 + 8, PX1 - PX0 + 20, 18, 170);
        // The edge seen below the face, then the face, its grain running along
        Draw.rect(g, PX0, y1, PX1, y1 + 6, 0xFF7A5230);
        Draw.vgrad(g, PX0, y0, PX1, y1, 0xFFD8A868, 0xFFB88448);
        for (int i = 0; i < 9; i++) {
            float base = y0 + 3 + i * (2 * HALF - 6) / 8f;
            float prevX = PX0, prevY = base + 1.2f * Mth.sin(PX0 * .03f + i);
            for (float x = PX0 + 8; x <= PX1; x += 8) {
                float y = base + 1.2f * Mth.sin(x * .03f + i) + .8f * Mth.sin(x * .11f + i * 2);
                Draw.line(g, prevX, prevY, x, y, .7f, Draw.argb(i % 3 == 0 ? 90 : 55, 0xFF7A4A20));
                prevX = x;
                prevY = y;
            }
        }
        // A knot
        Draw.disc(g, 262, PY + 9, 5, 0xFF8A5A2C);
        Draw.disc(g, 262, PY + 9, 2.5f, 0xFF6A4020);
        // The end grain at each end
        Draw.rect(g, PX0 - 4, y0, PX0, y1 + 6, 0xFFA87440);
        Draw.rect(g, PX1, y0, PX1 + 4, y1 + 6, 0xFF8A5C30);
        Draw.rect(g, PX0, y0, PX1, y0 + 1, 0x60FFFFFF);
        // The pencil line ahead of the saw, dashed; faint behind it
        float front = front();
        for (float x = CX0; x < CX1; x += 6) {
            boolean ahead = x >= front;
            if (!ahead && ((int) (x / 6)) % 2 == 1) {
                continue;
            }
            Draw.line(g, x, line(x), Math.min(CX1, x + 4), line(Math.min(CX1, x + 4)), ahead ? 1.1f : .8f, ahead ? 0xE0303034 : 0x50303034);
        }
        // The kerf: where the blade truly went
        for (int i = 1; i < kerf.size(); i++) {
            float[] a = kerf.get(i - 1), b = kerf.get(i);
            Draw.line(g, a[0], a[1], b[0], b[1], 2.6f, 0xFF3A2414);
        }
        // The split, if the wood gave
        if (maxCrack > 0 && !kerf.isEmpty()) {
            float[] last = kerf.get(kerf.size() - 1);
            float len = 30 * Math.max(crack, cracked >= 0 ? 1 : 0) + 6;
            float x = last[0], y = last[1];
            for (int i = 0; i < 6; i++) {
                float nx = x + len / 6, ny = y + (i % 2 == 0 ? 1.5f : -1.2f);
                Draw.line(g, x, y, nx, ny, 1.2f, 0xFF2A1A10);
                x = nx;
                y = ny;
            }
        }
    }

    private void drawSaw(GuiGraphics g) {
        float front = front();
        float off = strokeOffset;
        float tip = front + 16 + off, heel = front - 96 + off;
        float y = bladeY;
        boolean onLine = Math.abs(bladeY - line(front)) <= tolerance;
        // Where the blade bites, and a mark of how true it is
        Draw.glow(g, front, line(front), 10, onLine ? 0x7070FF90 : 0x70FF7050);
        Draw.disc(g, front, line(front), 2, onLine ? 0xFF90FF90 : 0xFFFF8060);
        // The blade, seen from above: a long steel plate, its teeth along it
        Draw.shadow(g, (tip + heel) / 2, y + 6, tip - heel + 10, 10, 90);
        Draw.rect(g, heel, y - 4, tip, y + 2, 0xFFB8BCC4);
        Draw.rect(g, heel, y - 4, tip, y - 3, 0xFFE8ECF4);
        Draw.rect(g, heel, y + 1, tip, y + 2, 0xFF6A6E78);
        for (float x = heel + 2; x < tip; x += 3) {
            Draw.line(g, x, y + 2, x + 1.5f, y + 3.5f, .8f, 0xFF8A8E98);
        }
        // The handle: walnut, its grip open
        float hx = heel - 22;
        Draw.rect(g, hx, y - 10, heel + 4, y + 9, 0xFF5A3418);
        Draw.rect(g, hx + 5, y - 5, heel - 2, y + 4, Draw.argb(200, 0xFF2A1A10));
        Draw.rect(g, hx, y - 10, heel + 4, y - 9, 0xFF8A5A30);
        Draw.disc(g, heel - 1, y - 6, 1.4f, 0xFFD8C080);
        Draw.disc(g, heel - 1, y + 5, 1.4f, 0xFFD8C080);
    }
}
