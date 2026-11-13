package com.waylays.arcanes.client.metier;

import com.mojang.math.Axis;
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
 * Gathering, "les racines": the plant has to come out whole, roots and all. Hold the button (or space) to pull, let go
 * to ease off: the tension must stay in the green band, where the roots give little by little. The roots resist
 * unevenly and gusts of wind shove the band aside; past the red line too long, the stem snaps and all is lost. Petals
 * shaken loose can be caught on their way down for a better harvest.
 */
public class HarvestGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_harvest");
    private static final float LIMIT = 24, RED = .9f;
    private static final float GROUND = 126, PX = 168;
    private static final float GX = 372, GY0 = 66, GY1 = 200;

    private record Petal(float x, float y, float vx, float spin, float born) {}

    private record Streak(float y, float born, float len) {}

    private final float half;
    private float tension, shownTension, progress, inBand, snap, gust, gustAt = 4, bonus;
    private boolean pulling;
    private final List<Petal> petals = new ArrayList<>();
    private final List<Streak> streaks = new ArrayList<>();
    private final Draw.Bits bits = new Draw.Bits();
    private float nextPetal = 3;
    private final float[][] roots;

    public HarvestGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, Component heading,
                       Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, List.of(), heading, sub, perfectAt, criticalBelow, 0xFF5A8A48);
        this.half = (float) (.12 / Math.pow(difficulty, .6));
        // The roots: branches spreading down from the stem's foot, each with a fork
        roots = new float[12][];
        for (int i = 0; i < roots.length; i++) {
            float a = Mth.HALF_PI + (random.nextFloat() - .5f) * 2.4f, len = 22 + random.nextFloat() * 40;
            roots[i] = new float[] {a, len, (random.nextFloat() - .5f) * 1.2f, .55f + random.nextFloat() * .5f, random.nextFloat()};
        }
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return List.of(control("hold", "harvest.ctl_pull"), control("click", "harvest.ctl_petal"));
    }

    private float center() {
        return Mth.clamp(.55f + .17f * Mth.sin(time * .9f * (float) difficulty) + .06f * Mth.sin(time * 2.3f) + gust, .25f, .8f);
    }

    @Override
    protected float liveScore() {
        return Mth.clamp(.65f * Math.min(1, progress) + .23f * (time > 0 ? inBand / time : 0) + bonus, 0, 1);
    }

    @Override
    protected void update(float dt) {
        tension += (pulling ? .95f * (float) Math.pow(difficulty, .3) : -.75f) * dt;
        tension = Mth.clamp(tension, 0, 1.05f);
        shownTension = Mth.lerp(Math.min(1, dt * 18), shownTension, tension);
        if (time >= gustAt) {
            gust = (random.nextBoolean() ? 1 : -1) * (.12f + random.nextFloat() * .1f);
            gustAt = time + 3.5f + random.nextFloat() * 3 / (float) difficulty;
            flash(Component.translatable("metier.arcanes_oublies.harvest.gust"), 0xFFE0F4FF);
            sound(SoundEvents.AZALEA_LEAVES_HIT, .7f, .5f);
            for (int i = 0; i < 7; i++) {
                streaks.add(new Streak(AY0 + 10 + random.nextFloat() * (GROUND - AY0 - 20), time + random.nextFloat() * .4f, 30 + random.nextFloat() * 50));
            }
        }
        gust *= (float) Math.pow(.35, dt);
        float c = center();
        if (Math.abs(tension - c) <= half) {
            progress += dt * .12f * (1 - .5f * Math.abs(tension - c) / half);
            inBand += dt;
            if (random.nextFloat() < dt * 6) {
                bits.add(time, PX + (random.nextFloat() - .5f) * 30, GROUND + 4, (random.nextFloat() - .5f) * 30, -20 - random.nextFloat() * 30, .7f,
                        1.5f + random.nextFloat(), 0xFF6A4A30, 200, false);
            }
        } else if (tension < c) {
            progress = Math.max(0, progress - dt * .02f);
        }
        if (tension > RED) {
            snap += dt;
            shake(1.5f);
            if (snap > .45f) {
                finish(liveScore() * .3f, true, Component.translatable("metier.arcanes_oublies.harvest.snapped"));
                return;
            }
        } else {
            snap = Math.max(0, snap - dt * .5f);
        }
        if (time >= nextPetal) {
            petals.add(new Petal(PX + (random.nextFloat() - .5f) * 30, GROUND - 70, (random.nextFloat() - .5f) * 40, random.nextFloat() * 6, time));
            nextPetal = time + 2.5f + random.nextFloat() * 3;
        }
        petals.removeIf(p -> time - p.born() > 2.4f);
        streaks.removeIf(s -> time - s.born() > 1.2f);
        if (progress >= 1) {
            sound(SoundEvents.ROOTED_DIRT_BREAK, 1.1f, .9f);
            finish(liveScore(), false, null);
        } else if (time >= LIMIT) {
            finish(liveScore(), false, Component.translatable("metier.arcanes_oublies.harvest.timeout"));
        }
    }

    private float[] petalPos(Petal p) {
        float t = time - p.born();
        return new float[] {p.x() + p.vx() * t + Mth.sin(t * 4 + p.spin()) * 10, p.y() + 34 * t + 6 * t * t};
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        for (Petal p : petals) {
            float[] pos = petalPos(p);
            if (Mth.square(mx - pos[0]) + Mth.square(my - pos[1]) < 13 * 13) {
                petals.remove(p);
                bonus = Math.min(.12f, bonus + .04f);
                flash(Component.translatable("metier.arcanes_oublies.harvest.caught"), 0xFFFFE0F0);
                sound(SoundEvents.CHERRY_LEAVES_HIT, 1.4f, .6f);
                bits.sparks(random, time, pos[0], pos[1], 8, 60, 0xFFFFC080);
                return;
            }
        }
        pulling = true;
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        pulling = false;
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            pulling = true;
        }
    }

    @Override
    protected void onKeyUp(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            pulling = false;
        }
    }

    private static void leaf(GuiGraphics g, float x, float y, float len, float width, float angle, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().mulPose(Axis.ZP.rotation(angle));
        Draw.sprite(g, Draw.DISC, len / 2, 0, len, width, color, false, 64, 64);
        Draw.line(g, 0, 0, len * .9f, 0, .6f, Draw.argb(120, 0xFF2A5A20));
        g.pose().popPose();
    }

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        float lift = Math.min(1, progress) * 16;
        float sway = center() - .55f + Mth.sin(time * 1.3f) * .02f;
        float strain = Mth.clamp((tension - .55f) * 2.2f, 0, 1);
        float tremble = tension > RED ? (random.nextFloat() - .5f) * 2 : 0;
        // The hole the roots come out of, in the soil
        Draw.shadow(g, PX, GROUND + 24 - lift * .3f, 110, 70, (int) (60 + 80 * Math.min(1, progress)));
        // Roots: pale, tapering, forking, lifting and straightening as they let go
        float footX = PX + tremble, footY = GROUND + 4 - lift;
        for (float[] r : roots) {
            float a = Mth.lerp(Math.min(1, progress) * .5f, r[0], Mth.HALF_PI);
            float len = r[1] * (1 - Math.min(1, progress) * .25f);
            float mxr = footX + Mth.cos(a) * len * .55f, myr = footY + Mth.sin(a) * len * .55f;
            float ex = footX + Mth.cos(a + r[2] * .4f) * len, ey = footY + Mth.sin(a + r[2] * .4f) * len;
            int c = Draw.lerp(0xFFE8D4A8, 0xFFB8986A, r[4]);
            Draw.stroke(g, footX, footY, mxr, myr, 2.6f * r[3] + 1, c);
            Draw.stroke(g, mxr, myr, ex, ey, 1.6f * r[3] + .5f, c);
            Draw.stroke(g, mxr, myr, mxr + Mth.cos(a + r[2]) * len * .35f, myr + Mth.sin(a + r[2]) * len * .35f, 1, Draw.argb(220, c));
        }
        // The stem, bending with the wind and straining with the pull
        float topX = PX + sway * 70 + tremble, topY = GROUND - 78 - lift - strain * 4;
        float midX = (PX + topX) / 2 + sway * 26, midY = (footY + topY) / 2;
        int stem = snap > 0 ? Draw.lerp(0xFF4E9A3A, 0xFFE05030, snap / .45f) : Draw.lerp(0xFF4E9A3A, 0xFF6AB84A, strain);
        float thick = 3 + strain * 1.2f;
        int segs = 8;
        float px = footX, py = footY;
        for (int i = 1; i <= segs; i++) {
            float t = i / (float) segs;
            float x = (1 - t) * (1 - t) * footX + 2 * (1 - t) * t * midX + t * t * topX;
            float y = (1 - t) * (1 - t) * footY + 2 * (1 - t) * t * midY + t * t * topY;
            Draw.stroke(g, px, py, x, y, thick * (1 - t * .35f), stem);
            px = x;
            py = y;
        }
        leaf(g, Mth.lerp(.35f, footX, topX), Mth.lerp(.35f, footY, topY), 22, 9, -2.4f + sway, 0xFF5AAA3E);
        leaf(g, Mth.lerp(.55f, footX, topX), Mth.lerp(.55f, footY, topY), 20, 8, -.6f + sway, 0xFF4E9A36);
        // The flower, and the hand's grip glowing on the stem
        Draw.glow(g, topX, topY - 4, 34, Draw.argb(70, 0xFFFFB050));
        Draw.itemAt(g, subject, topX, topY - 8, 40);
        if (pulling) {
            float gy = Mth.lerp(.7f, footY, topY);
            Draw.glow(g, Mth.lerp(.7f, footX, topX), gy, 14, Draw.argb((int) (120 + 100 * strain), strain > .8f ? 0xFFFF6040 : 0xFFFFF0C0));
        }
        // Wind streaks and petals
        for (Streak s : streaks) {
            float t = (time - s.born()) / 1.2f;
            if (t < 0) {
                continue;
            }
            float dir = gust >= 0 ? 1 : -1;
            float x = dir > 0 ? AX0 + t * 520 - 60 : AX1 - t * 520 + 60;
            Draw.stroke(g, x, s.y(), x - dir * s.len(), s.y() + 2, 1.2f, Draw.argb((int) (110 * Mth.sin(t * Mth.PI)), 0xFFFFFFFF));
        }
        for (Petal p : petals) {
            float[] pos = petalPos(p);
            float t = time - p.born();
            boolean hover = Mth.square(mx - pos[0]) + Mth.square(my - pos[1]) < 13 * 13;
            if (hover) {
                Draw.glow(g, pos[0], pos[1], 14, 0x90FFE0A0);
            }
            g.pose().pushPose();
            g.pose().translate(pos[0], pos[1], 0);
            g.pose().mulPose(Axis.ZP.rotation(t * 3 + p.spin()));
            Draw.sprite(g, Draw.DISC, 0, 0, 9, 5 + 2 * Mth.sin(t * 6 + p.spin()), 0xFFFF9A3A, false, 64, 64);
            Draw.sprite(g, Draw.DISC, -1, -.5f, 5, 2.5f, 0xFFFFD890, false, 64, 64);
            g.pose().popPose();
        }
        bits.draw(g, time);
        // The tension gauge: the green band where the roots give, the red line where the stem snaps
        float c = center();
        Draw.pill(g, GX - 3, GY0 - 3, GX + 17, GY1 + 3, 0xD0141210);
        Draw.vgrad(g, GX, GY0 + 7, GX + 14, GY1 - 7, 0xFF4A1A16, 0xFF20201E);
        Draw.disc(g, GX + 7, GY0 + 7, 7, 0xFF4A1A16);
        Draw.disc(g, GX + 7, GY1 - 7, 7, 0xFF20201E);
        float span = GY1 - GY0;
        float bt = GY1 - (c + half) * span, bb = GY1 - (c - half) * span;
        Draw.rect(g, GX, bt, GX + 14, bb, 0xD050D060);
        Draw.rect(g, GX, bt, GX + 14, bt + 1, 0xFFA0FFA0);
        Draw.rect(g, GX, bb - 1, GX + 14, bb, 0xFFA0FFA0);
        Draw.glow(g, GX + 7, (bt + bb) / 2, (bb - bt) * .8f + 6, 0x4040FF60);
        float red = GY1 - RED * span;
        Draw.rect(g, GX - 4, red - .5f, GX + 18, red + 1, 0xFFFF4A3A);
        float ty = GY1 - shownTension * span;
        boolean in = Math.abs(tension - c) <= half;
        Draw.glow(g, GX + 7, ty, 12, Draw.argb(160, in ? 0xFFB0FFB0 : tension > RED ? 0xFFFF6040 : 0xFFFFFFFF));
        Draw.pill(g, GX - 6, ty - 2, GX + 20, ty + 2, 0xFFFFFFFF);
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.harvest.tension"), GX + 7, GY0 - 14, 0xFFFFFFFF, .8f);
        // The roots letting go, and the time left
        Draw.text(g, Component.translatable("metier.arcanes_oublies.harvest.roots"), AX0 + 12, AY0 + 9, 0xFF2A3A20, .85f);
        Draw.pill(g, AX0 + 12, AY0 + 21, AX0 + 132, AY0 + 28, 0x90FFFFFF);
        if (progress > .01f) {
            Draw.pill(g, AX0 + 12, AY0 + 21, AX0 + 12 + 120 * Math.min(1, progress), AY0 + 28, 0xFF6ABA4A);
        }
        float left = Math.max(0, LIMIT - time);
        Draw.text(g, Component.literal(String.format(java.util.Locale.ROOT, "%.0f s", left)), AX1 - 74, AY0 + 9, left < 6 ? 0xFFC03020 : 0xFF2A3A20, 1);
    }
}
