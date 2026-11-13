package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gathering, "les racines": the plant has to come out whole, roots and all. Hold the mouse button (or space) to pull,
 * let go to ease off: the tension must stay in the green band, where the roots give little by little. The roots
 * resist unevenly and the wind now and then shoves the band aside; pulled past the red line too long, the stem snaps
 * and all is lost. Petals and seeds shaken loose can be caught on the way down for a better harvest. A good tool
 * (shears…) gives a wider band.
 */
public class HarvestGame extends MinigameScreen {
    private static final float LIMIT = 24;
    private static final float RED = .9f;
    private static final float GX = 302, GY0 = 50, GY1 = 186;

    private record Petal(float x, float y, float vx, float born) {}

    private final float half;
    private float tension, progress, inBand, snap, gust, gustAt = 4, bonus;
    private boolean pulling;
    private final List<Petal> petals = new ArrayList<>();
    private float nextPetal = 3;
    private final float[][] roots;

    public HarvestGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, Component heading,
                       Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, List.of(), heading, sub, perfectAt, criticalBelow, 0xFF4E8A44);
        this.half = (float) (.12 / Math.pow(difficulty, .6));
        // The roots: branches spreading down from the stem's foot
        roots = new float[14][];
        for (int i = 0; i < roots.length; i++) {
            float a = Mth.HALF_PI + (random.nextFloat() - .5f) * 2.2f, len = 20 + random.nextFloat() * 40;
            float x0 = 150 + (random.nextFloat() - .5f) * 10, y0 = 128 + random.nextFloat() * 6;
            roots[i] = new float[] {x0, y0, x0 + Mth.cos(a) * len, y0 + Mth.sin(a) * len, .5f + random.nextFloat()};
        }
    }

    @Override
    protected List<Component> rules() {
        return List.of(Component.translatable("metier.arcanes_oublies.harvest.rule1"), Component.translatable("metier.arcanes_oublies.harvest.rule2"),
                Component.translatable("metier.arcanes_oublies.harvest.rule3"));
    }

    /** Where the green band is centred now. */
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
        // The wind
        if (time >= gustAt) {
            gust = (random.nextBoolean() ? 1 : -1) * (.12f + random.nextFloat() * .1f);
            gustAt = time + 3.5f + random.nextFloat() * 3 / (float) difficulty;
            flash(Component.translatable("metier.arcanes_oublies.harvest.gust"), 0xFFD0F0FF);
            sound(SoundEvents.AZALEA_LEAVES_HIT, .7f, .5f);
        }
        gust *= (float) Math.pow(.35, dt);
        float c = center();
        if (Math.abs(tension - c) <= half) {
            progress += dt * .12f * (1 - .5f * Math.abs(tension - c) / half);
            inBand += dt;
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
        // Petals and seeds shaken loose
        if (time >= nextPetal) {
            petals.add(new Petal(130 + random.nextFloat() * 40, 70, (random.nextFloat() - .5f) * 40, time));
            nextPetal = time + 2.5f + random.nextFloat() * 3;
        }
        petals.removeIf(p -> time - p.born() > 2.6f);
        if (progress >= 1) {
            sound(SoundEvents.ROOTED_DIRT_BREAK, 1.1f, .9f);
            finish(liveScore(), false, null);
        } else if (time >= LIMIT) {
            finish(liveScore(), false, Component.translatable("metier.arcanes_oublies.harvest.timeout"));
        }
    }

    private float[] petalPos(Petal p) {
        float t = time - p.born();
        return new float[] {p.x() + p.vx() * t + Mth.sin(t * 5) * 8, p.y() + 40 * t};
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        for (Petal p : petals) {
            float[] pos = petalPos(p);
            if (Mth.square(mx - pos[0]) + Mth.square(my - pos[1]) < 12 * 12) {
                petals.remove(p);
                bonus = Math.min(.12f, bonus + .04f);
                flash(Component.translatable("metier.arcanes_oublies.harvest.caught"), 0xFFFFE0F0);
                sound(SoundEvents.CHERRY_LEAVES_HIT, 1.4f, .6f);
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

    @Override
    protected void drawGame(GuiGraphics g, int mx, int my) {
        // Sky and soil, in cross-section
        g.fillGradient(AX0, AY0, AX1, 120, 0xFF9CC8E8, 0xFFD8ECD0);
        g.fillGradient(AX0, 120, AX1, AY1, 0xFF5A3E26, 0xFF2E1E12);
        g.fill(AX0, 118, AX1, 122, 0xFF4E8A34);
        for (int i = 0; i < 40; i++) {
            int x = AX0 + (i * 37) % (AX1 - AX0), y = 128 + (i * 53) % 60;
            g.fill(x, y, x + 2, y + 2, 0x30000000);
        }
        // The roots, paler as they let go
        float lift = Math.min(1, progress) * 14;
        for (float[] r : roots) {
            int c = Draw.lerp(0xFFE8D0A0, 0xFF8A6A40, progress);
            Draw.line(g, r[0], r[1] - lift, r[2], r[3] - lift * .6f, r[4] + .6f, c);
        }
        // The stem, straining and bending with the tension and the wind
        float sway = center() - .55f;
        float strain = Mth.clamp((tension - .6f) * 2, 0, 1);
        int stemColor = snap > 0 ? Draw.lerp(0xFF4E9A3A, 0xFFE05030, snap / .45f) : 0xFF4E9A3A;
        float topX = 150 + sway * 60, topY = 72 - lift;
        Draw.line(g, 150, 128 - lift, (150 + topX) / 2 + sway * 20, 100 - lift, 2.2f + strain, stemColor);
        Draw.line(g, (150 + topX) / 2 + sway * 20, 100 - lift, topX, topY + 12, 2.2f + strain, stemColor);
        Draw.item(g, subject, topX - 16, topY - 18, 2);
        // The hand, pulling
        if (pulling) {
            g.fill((int) topX - 6, (int) topY + 10, (int) topX + 6, (int) topY + 18, 0xFFE8B898);
        }
        // Petals falling
        for (Petal p : petals) {
            float[] pos = petalPos(p);
            Draw.disc(g, pos[0], pos[1], 3, 0xFFFFA040);
            Draw.disc(g, pos[0] + 1, pos[1] - 1, 1.5f, 0xFFFFE0A0);
        }
        // The tension gauge: the green band where the roots give, the red line where the stem snaps
        g.fill((int) GX - 1, (int) GY0 - 1, (int) GX + 15, (int) GY1 + 1, 0xFF201810);
        g.fillGradient((int) GX, (int) GY0, (int) GX + 14, (int) GY1, 0xFF6A2020, 0xFF2A2A30);
        float c = center();
        int bandTop = (int) (GY1 - (c + half) * (GY1 - GY0)), bandBot = (int) (GY1 - (c - half) * (GY1 - GY0));
        g.fill((int) GX, bandTop, (int) GX + 14, bandBot, 0xC050D060);
        int red = (int) (GY1 - RED * (GY1 - GY0));
        g.fill((int) GX - 2, red, (int) GX + 16, red + 1, 0xFFFF4040);
        int ty = (int) (GY1 - tension * (GY1 - GY0));
        g.fill((int) GX - 4, ty - 1, (int) GX + 18, ty + 2, 0xFFFFFFFF);
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.harvest.tension"), GX + 7, GY0 - 10, 0xFFF0EAD8, .65f);
        // Progress: the roots letting go
        g.fill(AX0 + 10, AY1 - 12, AX0 + 150, AY1 - 6, 0x80000000);
        g.fill(AX0 + 10, AY1 - 12, AX0 + 10 + (int) (140 * Math.min(1, progress)), AY1 - 6, 0xFF9AE07A);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.harvest.roots"), AX0 + 10, AY1 - 22, 0xFFF0EAD8, .7f);
        // Time left
        Draw.text(g, Component.literal(String.format(java.util.Locale.ROOT, "%.0f s", Math.max(0, LIMIT - time))), AX0 + 8, AY0 + 6, 0xFF203020, .9f);
    }
}
