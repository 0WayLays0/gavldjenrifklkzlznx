package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mining, "points de rupture": a boulder of ore sits in the gallery. Its weak points light up one after the other (two
 * at once as the vein gives), each with a ring closing on it; strike the point as the ring meets it. A clean blow
 * splits the rock along the vein; too early or too late barely bites; striking beside the point, or letting it pass,
 * shakes the rock, and shaken too often it caves in. A worse pick gives smaller windows, faster rings, less room.
 */
public class MiningGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_mining"), ROCK = Draw.tex("boulder"), ORE = Draw.tex("boulder_ore");
    private static final float CX = 210, CY = 140, BW = 300, BH = 169;
    private static final float RX = 124, RY = 64;
    private static final float INNER = 8, OUTER = 38;

    private record Point(float x, float y, float born) {}

    private record Crack(float x, float y, float[] path, float born) {}

    private final int strikes;
    private final float ringTime;
    private final int maxShake;
    private final int ore;
    private final List<Point> points = new ArrayList<>();
    private final List<Crack> cracks = new ArrayList<>();
    private final Draw.Bits bits = new Draw.Bits();
    private int spawned, done, shaken;
    private float total;
    private float nextAt = .5f;
    private float swingAt = -1, hitAt = -1;

    public MiningGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, Component heading,
                      Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, List.of(), heading, sub, perfectAt, criticalBelow, 0xFF8A7458);
        this.strikes = 5 + (int) Math.round(difficulty * 1.2);
        this.ringTime = (float) (1.35 / difficulty);
        this.maxShake = difficulty >= 1.3 ? 2 : 3;
        this.ore = oreColor(subject);
    }

    private static int oreColor(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        if (id.contains("copper")) return 0xFFE88A52;
        if (id.contains("iron")) return 0xFFE8C4A4;
        if (id.contains("gold")) return 0xFFFFD84A;
        if (id.contains("diamond")) return 0xFF6AF0E8;
        if (id.contains("emerald")) return 0xFF4AE880;
        if (id.contains("redstone")) return 0xFFFF3A2A;
        if (id.contains("lapis")) return 0xFF4A6AF0;
        if (id.contains("coal")) return 0xFF3A3A3A;
        if (id.contains("quartz")) return 0xFFF4ECE4;
        return 0xFFE88A52;
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return List.of(control("click", "mining.ctl_strike"));
    }

    @Override
    protected float liveScore() {
        return strikes == 0 ? 0 : total / strikes;
    }

    private boolean inRock(float x, float y) {
        float dx = (x - CX) / RX, dy = (y - CY) / RY;
        return dx * dx + dy * dy < 1;
    }

    @Override
    protected void update(float dt) {
        if (spawned < strikes && time >= nextAt && points.size() < 2) {
            boolean pair = spawned >= strikes / 2 && spawned + 1 < strikes && random.nextFloat() < (difficulty >= 1.2 ? .5f : .3f) && points.isEmpty();
            spawn(0);
            if (pair) {
                spawn(ringTime * .45f);
            }
            nextAt = time + ringTime + .4f;
        }
        for (int i = points.size() - 1; i >= 0; i--) {
            Point p = points.get(i);
            if (time - p.born() > ringTime * 1.3f) {
                points.remove(i);
                done++;
                miss(p.x(), p.y(), Component.translatable("metier.arcanes_oublies.mining.passed"));
            }
        }
        if (shaken >= maxShake) {
            finish(liveScore(), true, Component.translatable("metier.arcanes_oublies.mining.collapse"));
        } else if (done >= strikes && points.isEmpty()) {
            finish(liveScore(), false, null);
        }
    }

    private void spawn(float delay) {
        for (int tries = 0; tries < 40; tries++) {
            float x = CX + (random.nextFloat() * 2 - 1) * (RX - 22), y = CY + (random.nextFloat() * 2 - 1) * (RY - 14);
            if (!inRock(x, y)) {
                continue;
            }
            boolean clear = true;
            for (Point o : points) {
                if (Mth.square(o.x() - x) + Mth.square(o.y() - y) < 64 * 64) {
                    clear = false;
                }
            }
            if (clear) {
                points.add(new Point(x, y, time + delay));
                spawned++;
                return;
            }
        }
    }

    private float ring(Point p) {
        float t = (time - p.born()) / ringTime;
        return INNER + (OUTER - INNER) * (1 - t);
    }

    private void miss(float x, float y, Component why) {
        shaken++;
        shake(8);
        flash(why, 0xFFFF7060);
        sound(SoundEvents.GRAVEL_BREAK, .7f, .8f);
        bits.chips(random, time, x, y, 6, 0xFF6A625A);
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        swingAt = time;
        Point best = null;
        float bestD = 16;
        for (Point p : points) {
            if (time < p.born()) {
                continue;
            }
            float d = (float) Math.sqrt(Mth.square(mx - p.x()) + Mth.square(my - p.y()));
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best == null) {
            if (inRock((float) mx, (float) my)) {
                miss((float) mx, (float) my, Component.translatable("metier.arcanes_oublies.mining.beside"));
            }
            return;
        }
        points.remove(best);
        done++;
        hitAt = time;
        float err = Math.abs(ring(best) - INNER);
        float q;
        Component word;
        int color;
        if (err < 3f) {
            q = 1;
            word = Component.translatable("metier.arcanes_oublies.mining.clean");
            color = 0xFFFFE070;
        } else if (err < 7.5f) {
            q = .75f;
            word = Component.translatable("metier.arcanes_oublies.mining.good");
            color = 0xFFB8E0FF;
        } else {
            q = .35f;
            word = Component.translatable(ring(best) > INNER ? "metier.arcanes_oublies.mining.early" : "metier.arcanes_oublies.mining.late");
            color = 0xFFFFA060;
        }
        total += q;
        flash(word, color);
        shake(q >= 1 ? 5 : 3);
        sound(q >= .75f ? SoundEvents.STONE_BREAK : SoundEvents.STONE_HIT, q >= 1 ? 1.2f : .9f, .9f);
        if (q >= 1) {
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, .5f);
        }
        // A crack running from the point, wandering
        int n = 3 + (int) (q * 3);
        float[] path = new float[n * 2 + 2];
        float a = random.nextFloat() * Mth.TWO_PI, x = best.x(), y = best.y();
        path[0] = x;
        path[1] = y;
        for (int i = 1; i <= n; i++) {
            a += (random.nextFloat() - .5f) * 1.1f;
            float step = 6 + random.nextFloat() * 6;
            x += Mth.cos(a) * step;
            y += Mth.sin(a) * step * .7f;
            path[i * 2] = x;
            path[i * 2 + 1] = y;
        }
        cracks.add(new Crack(best.x(), best.y(), path, time));
        bits.sparks(random, time, best.x(), best.y(), 8 + (int) (q * 10), 140, 0xFFFFC070);
        bits.chips(random, time, best.x(), best.y(), 5 + (int) (q * 6), ore);
    }

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        // Lantern light flickering on the wall
        float flick = .85f + .1f * Mth.sin(time * 7) + .05f * Mth.sin(time * 17);
        Draw.glow(g, AX0 + 60, AY0 + 30, 90, Draw.argb((int) (40 * flick), 0xFFFFA040));
        // The boulder, its shadow, its veins, glowing more as it gives
        Draw.shadow(g, CX + 6, CY + BH * .42f, BW * 1.05f, 40, 190);
        float hit = hitAt < 0 ? 0 : Math.max(0, 1 - (time - hitAt) * 6);
        float bob = hit * 1.5f;
        Draw.picture(g, ROCK, CX - BW / 2, CY - BH / 2 + bob, CX + BW / 2, CY + BH / 2 + bob, 512, 288, 0xFFFFFFFF);
        Draw.picture(g, ORE, CX - BW / 2, CY - BH / 2 + bob, CX + BW / 2, CY + BH / 2 + bob, 512, 288, ore);
        float shown = (float) done / Math.max(1, strikes);
        Draw.glow(g, CX, CY, 110, Draw.argb((int) (20 + 40 * shown), ore));
        // Cracks: dark, with a lit lip
        for (Crack c : cracks) {
            float grow = Math.min(1, (time - c.born()) * 8);
            float[] p = c.path();
            int segs = p.length / 2 - 1;
            int drawn = Math.max(1, (int) (segs * grow));
            for (int i = 0; i < drawn; i++) {
                float w = 2.6f * (1 - i / (float) segs) + .6f;
                Draw.stroke(g, p[i * 2], p[i * 2 + 1] + bob, p[i * 2 + 2], p[i * 2 + 3] + bob, w, 0xF0140F0C);
                Draw.line(g, p[i * 2] + .6f, p[i * 2 + 1] + bob + w * .6f, p[i * 2 + 2] + .6f, p[i * 2 + 3] + bob + w * .6f, .6f, 0x50FFE6C0);
            }
            if (time - c.born() < .3f) {
                Draw.glow(g, c.x(), c.y() + bob, 22, Draw.argb((int) (200 * (1 - (time - c.born()) / .3f)), 0xFFFFE0A0));
            }
        }
        // Weak points and their closing rings
        for (Point p : points) {
            if (time < p.born()) {
                continue;
            }
            float r = ring(p);
            float in = Math.min(1, (time - p.born()) * 6);
            float err = Math.abs(r - INNER);
            float pulse = .7f + .3f * Mth.sin(time * 12);
            Draw.glow(g, p.x(), p.y(), 26 * in, Draw.argb((int) (120 * pulse), ore));
            Draw.glow(g, p.x(), p.y(), 10 * in, Draw.argb((int) (230 * pulse), 0xFFFFF4D0));
            Draw.disc(g, p.x(), p.y(), 2.2f * in, 0xFFFFFFFF);
            int rc = err < 3f ? 0xFFFFE070 : err < 7.5f ? 0xFFBEE6FF : 0xFFFFFFFF;
            Draw.ring(g, p.x(), p.y(), INNER + 1.5f, Draw.argb(110, 0xFFFFE070), true);
            Draw.ring(g, p.x(), p.y(), Math.max(2, r), Draw.argb((int) (240 * in), rc), true);
            if (err < 3f) {
                Draw.glow(g, p.x(), p.y(), r + 8, Draw.argb(90, 0xFFFFE070));
            }
        }
        bits.draw(g, time);
        // Strikes left (nuggets) and the rock's shaking (stones that crack)
        float sx = AX0 + 16;
        for (int i = 0; i < strikes; i++) {
            boolean used = i < done;
            Draw.disc(g, sx + i * 11, AY0 + 14, 4, used ? 0xFF3A342E : ore);
            if (!used) {
                Draw.disc(g, sx + i * 11 - 1.2f, AY0 + 12.8f, 1.4f, 0x90FFFFFF);
            }
        }
        for (int i = 0; i < maxShake; i++) {
            float x = AX1 - 18 - i * 14;
            boolean lost = i < shaken;
            Draw.disc(g, x, AY0 + 14, 5, lost ? 0xFFB83A2A : 0xFF5A524A);
            if (lost) {
                Draw.line(g, x - 3, AY0 + 11, x + 3, AY0 + 17, 1.2f, 0xFF1A0E0A);
                Draw.line(g, x + 3, AY0 + 11, x - 3, AY0 + 17, 1.2f, 0xFF1A0E0A);
            }
        }
        // The pick, swinging when it strikes
        if (inArea(mx, my)) {
            float swing = swingAt < 0 ? 1 : Mth.clamp((time - swingAt) / .2f, 0, 1);
            float angle = -30 + (swing < 1 ? 75 * Mth.sin(swing * Mth.PI) : 0);
            Draw.shadow(g, mx + 6, my + 8, 26, 12, 70);
            Draw.itemRotated(g, tool.isEmpty() ? new ItemStack(Items.IRON_PICKAXE) : tool, mx + 12, my - 10, 2.1f, angle);
        }
    }
}
