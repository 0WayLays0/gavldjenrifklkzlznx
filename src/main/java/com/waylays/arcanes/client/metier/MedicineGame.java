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
 * Medicine at the medical table: a remedy prepared the way a healer does. First the ingredients are ground in the
 * mortar: hold the left button and turn the pestle round the bowl (the mouse in circles), neither too slowly (nothing
 * happens) nor too fast (it splashes out of the bowl), until the paste is smooth. Then the dose: hold the left button
 * (or space) to pour into the phial and let go at its mark; the remedy keeps flowing a moment after. Too little and it
 * is weak, too much and it runs over.
 */
public class MedicineGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_medicine");
    /** The mortar's middle and size; the phial's place and size. */
    private static final float MX = 150, MY = 152, MR = 54, PX = 318, PY0 = 74, PY1 = 196, PW = 34;
    private static final float LIMIT = 40;
    /** The colours of the bits of herb, root and seed in the bowl. */
    private static final int[] GRAIN_COLORS = {0xFF5A8A3A, 0xFF7AA048, 0xFF8A5A30, 0xFFB08850, 0xFFA03A4A, 0xFF6A6A30};

    private enum Phase {GRIND, POUR, DONE}

    private Phase phase = Phase.GRIND;
    private final float band0, band1, mark, tolerance;
    /** Grinding: the pestle's angle and speed (turns a second), how smooth the paste is, how well it went, splashes. */
    private float angle, speed, smooth, grindGood, grindTime, lastAngle = Float.NaN;
    private int splashes;
    private float splashAt = -10;
    private boolean holding;
    /** Pouring: the level in the phial (0 to 1), the flow, how long since the pouring stopped. */
    private float level, flow, pourScore = -1;
    private boolean pouring;
    private float mouseX = MX, mouseY = MY;
    private final Draw.Bits bits = new Draw.Bits();
    /** Bits of herb and grain in the bowl: angle, distance from the middle, size, colour (an index in GRAIN_COLORS). */
    private final List<float[]> grains = new ArrayList<>();

    public MedicineGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                        Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF3E9A86);
        float d = (float) Math.sqrt(Math.max(.3, difficulty));
        // The pace that grinds well, in turns a second: narrower the harder it is
        float mid = 1.15f + random.nextFloat() * .3f;
        float half = .55f / d;
        this.band0 = mid - half;
        this.band1 = mid + half;
        this.mark = .55f + random.nextFloat() * .3f;
        this.tolerance = .05f / d;
        for (int i = 0; i < 46; i++) {
            float a = random.nextFloat() * Mth.TWO_PI, r = Mth.sqrt(random.nextFloat()) * MR * .62f;
            grains.add(new float[]{a, r, 2.2f + random.nextFloat() * 3.2f, random.nextInt(GRAIN_COLORS.length)});
        }
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return phase == Phase.GRIND ? List.of(control("hold", "medicine.ctl_grind")) : List.of(control("click_hold", "medicine.ctl_pour"));
    }

    private float grindScore() {
        return grindTime <= 0 ? 1 : Mth.clamp(grindGood / grindTime, 0, 1);
    }

    @Override
    protected float liveScore() {
        float g = grindScore() * Math.min(1, smooth) - splashes * .12f;
        if (phase == Phase.GRIND) {
            return Mth.clamp(g * .5f + .25f, 0, 1);
        }
        float pour = pourScore >= 0 ? pourScore : Mth.clamp(1 - Math.abs(level - mark) / (tolerance * 6), 0, 1);
        return Mth.clamp(g * .5f + pour * .5f, 0, 1);
    }

    @Override
    protected void update(float dt) {
        if (!playing()) {
            return;
        }
        if (time >= LIMIT && phase != Phase.DONE) {
            phase = Phase.DONE;
            finish(liveScore() * .7f, false, Component.translatable("metier.arcanes_oublies.medicine.too_long"));
            return;
        }
        if (phase == Phase.GRIND) {
            grind(dt);
        } else if (phase == Phase.POUR) {
            pour(dt);
        }
    }

    private void grind(float dt) {
        // The pestle's turning, from how the mouse goes round the bowl while held
        float a = (float) Math.atan2(mouseY - MY, mouseX - MX);
        float dist = Mth.sqrt((mouseX - MX) * (mouseX - MX) + (mouseY - MY) * (mouseY - MY));
        float turn = 0;
        if (holding && dist > MR * .15f && !Float.isNaN(lastAngle)) {
            turn = Mth.wrapDegrees((a - lastAngle) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        }
        lastAngle = holding ? a : Float.NaN;
        float instant = Math.abs(turn) / Mth.TWO_PI / Math.max(dt, .001f);
        speed = Mth.lerp(Math.min(1, dt * 6), speed, holding ? instant : 0);
        angle += turn;
        if (speed > .05f) {
            grindTime += dt;
            boolean good = speed >= band0 && speed <= band1;
            if (good) {
                grindGood += dt;
                smooth += dt / 6.5f;
                if (random.nextFloat() < dt * 6) {
                    sound(SoundEvents.GRAVEL_HIT, .7f + random.nextFloat() * .3f, .25f);
                }
            } else if (speed < band0) {
                smooth += dt / 6.5f * .2f;
            } else if (time - splashAt > .8f) {
                // Too fast: the paste splashes over the rim
                splashAt = time;
                splashes++;
                shake(2);
                flash(Component.translatable("metier.arcanes_oublies.medicine.splash"), 0xFFFFB070);
                sound(SoundEvents.SLIME_SQUISH_SMALL, 1.2f, .6f);
                float sa = angle;
                for (int i = 0; i < 6; i++) {
                    bits.add(time, MX + Mth.cos(sa) * MR * .9f, MY + Mth.sin(sa) * MR * .5f - 8, Mth.cos(sa + i) * 60, -60 - random.nextFloat() * 50,
                            .7f, 3.5f, 0xFF6A8A3A, 300, false);
                }
                if (splashes >= 4) {
                    phase = Phase.DONE;
                    finish(liveScore() * .4f, true, Component.translatable("metier.arcanes_oublies.medicine.spilt"));
                    return;
                }
            }
        }
        // The grains wear down as the paste comes
        for (float[] g : grains) {
            g[0] += turn * (1.1f - g[1] / MR);
        }
        if (smooth >= 1) {
            phase = Phase.POUR;
            holding = false;
            flash(Component.translatable("metier.arcanes_oublies.medicine.ground"), 0xFFD8F8C8);
            sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.4f, .5f);
        }
    }

    private void pour(float dt) {
        // The flow follows the button with a lag; the remedy keeps running a moment after
        float want = pouring ? .34f : 0;
        flow = Mth.lerp(Math.min(1, dt * (pouring ? 3.5f : 2.2f)), flow, want);
        if (flow < .004f && !pouring) {
            flow = 0;
        }
        level += flow * dt;
        if (flow > .01f && random.nextFloat() < dt * 10) {
            sound(SoundEvents.BOTTLE_FILL, 1.4f + level * .5f, .12f);
        }
        if (level >= 1) {
            phase = Phase.DONE;
            pourScore = 0;
            flash(Component.translatable("metier.arcanes_oublies.medicine.overflow"), 0xFFFF9080);
            finish(liveScore() * .5f, true, Component.translatable("metier.arcanes_oublies.medicine.overflow"));
            return;
        }
        if (!pouring && flow == 0 && level > .05f) {
            // Settled: judged against the mark
            float off = Math.abs(level - mark);
            pourScore = Mth.clamp(1 - off / (tolerance * 6), 0, 1);
            phase = Phase.DONE;
            if (off <= tolerance) {
                flash(Component.translatable("metier.arcanes_oublies.medicine.exact"), 0xFFB0FFD0);
            } else {
                flash(Component.translatable(level < mark ? "metier.arcanes_oublies.medicine.short" : "metier.arcanes_oublies.medicine.over"), 0xFFFFD080);
            }
            finish(liveScore(), false, null);
        }
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (button != 0) {
            return;
        }
        if (phase == Phase.GRIND) {
            holding = true;
        } else if (phase == Phase.POUR) {
            pouring = true;
        }
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        if (button == 0) {
            holding = false;
            pouring = false;
        }
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_SPACE && phase == Phase.POUR) {
            pouring = true;
        }
    }

    @Override
    protected void onKeyUp(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            pouring = false;
        }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        mouseX = mx;
        mouseY = my;
        drawMortar(g);
        drawPhial(g);
        bits.draw(g, time);
        drawGauges(g);
    }

    private int paste() {
        float s = Mth.clamp(smooth, 0, 1);
        return Draw.lerp(0xFF6A7A3A, 0xFF8AB060, s);
    }

    private void drawMortar(GuiGraphics g) {
        // The bowl, seen a little from above: its shadow, its outside, its rim, the hollow and what is in it
        Draw.shadow(g, MX, MY + MR * .62f + 6, MR * 2.4f, 22, 180);
        Draw.vgrad(g, MX - MR, MY, MX + MR, MY + MR * .62f, 0xFFA8A49A, 0xFF5E5A54);
        Draw.disc(g, MX - MR * .55f, MY + MR * .5f, MR * .45f, 0xFF6A665E);
        Draw.disc(g, MX + MR * .55f, MY + MR * .5f, MR * .45f, 0xFF56524C);
        Draw.rect(g, MX - MR * .55f, MY + MR * .3f, MX + MR * .55f, MY + MR * .95f, 0xFF615D56);
        Draw.rect(g, MX - MR * .6f, MY + MR * .9f, MX + MR * .6f, MY + MR + 2, 0xFF4A4640);
        ellipse(g, MX, MY, MR, MR * .42f, 0xFFC8C4BA);
        ellipse(g, MX, MY + 1, MR * .86f, MR * .34f, 0xFF3A3632);
        // The paste: rough with grains at first, smooth and even at the end
        ellipse(g, MX, MY + 4, MR * .78f, MR * .27f, paste());
        float rough = 1 - Mth.clamp(smooth, 0, 1);
        for (float[] gr : grains) {
            float size = gr[2] * (.15f + .85f * rough);
            if (size < .5f) {
                continue;
            }
            float x = MX + Mth.cos(gr[0]) * gr[1], y = MY + 4 + Mth.sin(gr[0]) * gr[1] * .34f;
            Draw.disc(g, x, y, size, Draw.argb(230, GRAIN_COLORS[(int) gr[3]]));
            Draw.disc(g, x - size * .3f, y - size * .3f, size * .35f, 0x50FFFFFF);
        }
        Draw.glow(g, MX - MR * .3f, MY - 2, MR * .5f, Draw.argb((int) (60 * smooth), 0xFFE8FFD0));
        // The pestle, its head in the bowl where it is turning, its handle leaning out
        float hx = MX + Mth.cos(angle) * MR * .45f, hy = MY + 4 + Mth.sin(angle) * MR * .16f;
        float tx = hx + 30 + Mth.cos(angle) * 10, ty = hy - 62;
        Draw.line(g, hx, hy, tx, ty, 11, 0xFF8A6A48);
        Draw.line(g, hx + 2, hy, tx + 2, ty, 3, 0x40FFFFFF);
        Draw.disc(g, tx, ty, 6.5f, 0xFF9A7A56);
        Draw.disc(g, hx, hy, 8, 0xFFB0AA9E);
        Draw.disc(g, hx - 2, hy - 2, 3, 0x60FFFFFF);
    }

    private static void ellipse(GuiGraphics g, float cx, float cy, float rx, float ry, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(1, ry / rx, 1);
        Draw.disc(g, 0, 0, rx, color);
        g.pose().popPose();
    }

    private void drawPhial(GuiGraphics g) {
        boolean active = phase != Phase.GRIND;
        float x0 = PX - PW / 2, x1 = PX + PW / 2;
        float alpha = active ? 1 : .45f;
        // The phial: clear glass, its neck, the mark to fill to
        Draw.shadow(g, PX, PY1 + 6, PW * 2, 14, (int) (150 * alpha));
        Draw.rect(g, x0, PY0 + 24, x1, PY1, Draw.argb((int) (70 * alpha), 0xFFD8F0FF));
        Draw.rect(g, PX - 8, PY0, PX + 8, PY0 + 24, Draw.argb((int) (70 * alpha), 0xFFD8F0FF));
        // The remedy in it
        float y = Mth.lerp(level, PY1, PY0 + 24);
        if (level > 0) {
            Draw.vgrad(g, x0 + 2, y, x1 - 2, PY1 - 2, Draw.argb((int) (235 * alpha), 0xFF9ADA7A), Draw.argb((int) (235 * alpha), 0xFF4A8A3A));
            Draw.rect(g, x0 + 2, y, x1 - 2, y + 1.5f, Draw.argb((int) (255 * alpha), 0xFFD8FFC0));
        }
        // The stream falling into it while it pours
        if (flow > .004f) {
            float w = 1 + flow * 14;
            Draw.rect(g, PX - w / 2, PY0 - 30, PX + w / 2, y, 0xE08ACA6A);
            Draw.glow(g, PX, y, 6 + flow * 20, 0x60D8FFC0);
        }
        // The glass's edges and highlight
        Draw.rect(g, x0, PY0 + 24, x0 + 1.5f, PY1, Draw.argb((int) (200 * alpha), 0xFFFFFFFF));
        Draw.rect(g, x1 - 1.5f, PY0 + 24, x1, PY1, Draw.argb((int) (120 * alpha), 0xFFFFFFFF));
        Draw.rect(g, x0, PY1 - 1.5f, x1, PY1, Draw.argb((int) (200 * alpha), 0xFFFFFFFF));
        Draw.rect(g, x0 + 5, PY0 + 30, x0 + 8, PY1 - 12, Draw.argb((int) (90 * alpha), 0xFFFFFFFF));
        Draw.rect(g, PX - 10, PY0 - 3, PX + 10, PY0 + 1, Draw.argb((int) (220 * alpha), 0xFFE8F4FF));
        // The mark: a line round the glass, and the band that counts as exact
        float my = Mth.lerp(mark, PY1, PY0 + 24);
        float band = (Mth.lerp(mark + tolerance, PY1, PY0 + 24) - my);
        Draw.rect(g, x0 - 4, my + band, x1 + 4, my - band, Draw.argb((int) (60 * alpha), 0xFF7AFFB0));
        Draw.rect(g, x0 - 6, my - .7f, x1 + 6, my + .7f, Draw.argb((int) (255 * alpha), 0xFF2A3A30));
        Draw.text(g, Component.translatable("metier.arcanes_oublies.medicine.mark"), x1 + 9, my - 4, Draw.argb((int) (255 * alpha), 0xFFF0E8D8), .75f);
        // The flask it is poured from, tilted over the phial while pouring
        if (active) {
            float tilt = Mth.clamp(flow * 3, 0, 1);
            float fx = PX - 20 - 8 * tilt, fy = PY0 - 44;
            Draw.disc(g, fx, fy, 14, 0xC0D8F0FF);
            Draw.disc(g, fx, fy + 2, 11, 0xE07ABA5A);
            Draw.line(g, fx + 8, fy - 8, PX - 2, PY0 - 30 - 4 * (1 - tilt), 5, 0xC0D8F0FF);
        }
    }

    private void drawGauges(GuiGraphics g) {
        // The paste coming smooth, and the pace of the pestle against the pace that grinds well
        float gx0 = 40, gx1 = 260, gy = AY0 + 12;
        Draw.text(g, Component.translatable("metier.arcanes_oublies.medicine.paste"), gx0, gy - 10, 0xFF2A2420, .75f);
        Draw.pill(g, gx0, gy, gx1, gy + 7, 0xFF3A3430);
        Draw.pill(g, gx0, gy, gx0 + (gx1 - gx0) * Mth.clamp(smooth, 0, 1), gy + 7, 0xFF9ADA7A);
        if (phase == Phase.GRIND) {
            float py = gy + 20, top = 2.6f;
            Draw.text(g, Component.translatable("metier.arcanes_oublies.medicine.pace"), gx0, py - 1, 0xFF2A2420, .75f);
            float px0 = gx0 + 40;
            Draw.pill(g, px0, py, gx1, py + 7, 0xFF3A3430);
            Draw.rect(g, px0 + (gx1 - px0) * band0 / top, py, px0 + (gx1 - px0) * Math.min(top, band1) / top, py + 7, 0xB050D070);
            float at = px0 + (gx1 - px0) * Mth.clamp(speed / top, 0, 1);
            boolean good = speed >= band0 && speed <= band1;
            Draw.glow(g, at, py + 3.5f, good ? 10 : 6, good ? 0xC0FFE070 : 0x80FFFFFF);
            Draw.rect(g, at - 1, py - 3, at + 1, py + 10, speed > band1 ? 0xFFFF7050 : 0xFFFFFFFF);
            for (int i = 0; i < splashes; i++) {
                Draw.disc(g, gx1 + 10 + i * 9, py + 3.5f, 3, 0xFFFF9060);
            }
        }
    }
}
