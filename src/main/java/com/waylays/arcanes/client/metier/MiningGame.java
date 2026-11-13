package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mining, "points de rupture": a boulder of ore fills the play area. Its weak points light up one after the other
 * (two at once as the vein gives), each with a ring closing on it; strike the point (click) as the ring meets it. A
 * clean blow cracks the rock along the vein; a blow too early or too late barely bites; striking beside the point, or
 * letting it pass, shakes the rock: shaken too often, it caves in and the attempt is lost. A worse pick gives smaller
 * windows, faster rings and less room for error.
 */
public class MiningGame extends MinigameScreen {
    private static final float CX = 170, CY = 112, RX = 120, RY = 66;
    private static final float INNER = 7, OUTER = 34;

    private record Point(float x, float y, float born) {}

    private record Debris(float x, float y, float vx, float vy, float born, int color) {}

    private final int strikes;
    private final float ringTime;
    private final int maxShake;
    private final int ore;
    private final int[][] cells;
    private final List<Point> points = new ArrayList<>();
    private final List<float[]> cracks = new ArrayList<>();
    private final List<Debris> debris = new ArrayList<>();
    private int spawned, done, shaken;
    private float total;
    private float nextAt = .4f;
    private float swingAt = -1;

    public MiningGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, Component heading,
                      Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, List.of(), heading, sub, perfectAt, criticalBelow, 0xFF7A6A58);
        this.strikes = 5 + (int) Math.round(difficulty * 1.2);
        this.ringTime = (float) (1.3 / difficulty);
        this.maxShake = difficulty >= 1.3 ? 2 : 3;
        this.ore = oreColor(subject);
        // The boulder: cells of stone, some of them ore
        cells = new int[40][24];
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 24; y++) {
                float u = (x * 8 + 4 - (CX - 160)) / 320f, v = y / 24f;
                cells[x][y] = random.nextInt(5) == 0 ? 2 : random.nextInt(3) == 0 ? 1 : 0;
            }
        }
    }

    private static int oreColor(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        if (id.contains("copper")) return 0xFFE07A40;
        if (id.contains("iron")) return 0xFFD8B090;
        if (id.contains("gold")) return 0xFFFFD84A;
        if (id.contains("diamond")) return 0xFF5AE8E0;
        if (id.contains("emerald")) return 0xFF3AE070;
        if (id.contains("redstone")) return 0xFFE02020;
        if (id.contains("lapis")) return 0xFF3050D0;
        if (id.contains("coal")) return 0xFF202020;
        if (id.contains("quartz")) return 0xFFF0E8E0;
        return 0xFFE07A40;
    }

    @Override
    protected List<Component> rules() {
        return List.of(Component.translatable("metier.arcanes_oublies.mining.rule1"), Component.translatable("metier.arcanes_oublies.mining.rule2"),
                Component.translatable("metier.arcanes_oublies.mining.rule3", maxShake));
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
        // New weak points
        if (spawned < strikes && time >= nextAt && points.size() < 2) {
            boolean pair = spawned >= strikes / 2 && spawned + 1 < strikes && random.nextFloat() < (difficulty >= 1.2 ? .5f : .3f) && points.isEmpty();
            spawn(0);
            if (pair) {
                spawn(ringTime * .45f);
            }
            nextAt = time + ringTime + .35f;
        }
        // Points let pass
        for (int i = points.size() - 1; i >= 0; i--) {
            Point p = points.get(i);
            if (time - p.born() > ringTime * 1.3f) {
                points.remove(i);
                done++;
                miss(p.x(), p.y(), Component.translatable("metier.arcanes_oublies.mining.passed"));
            }
        }
        debris.removeIf(d -> time - d.born() > 1.2f);
        if (shaken >= maxShake) {
            finish(liveScore(), true, Component.translatable("metier.arcanes_oublies.mining.collapse"));
        } else if (done >= strikes && points.isEmpty()) {
            finish(liveScore(), false, null);
        }
    }

    private void spawn(float delay) {
        for (int tries = 0; tries < 30; tries++) {
            float x = CX + (random.nextFloat() * 2 - 1) * (RX - 26), y = CY + (random.nextFloat() * 2 - 1) * (RY - 18);
            if (!inRock(x, y)) {
                continue;
            }
            boolean clear = true;
            for (Point o : points) {
                if (Mth.square(o.x() - x) + Mth.square(o.y() - y) < 60 * 60) {
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

    /** The ring's radius round {@code p} now. */
    private float ring(Point p) {
        float t = (time - p.born()) / ringTime;
        return INNER + (OUTER - INNER) * (1 - t);
    }

    private void miss(float x, float y, Component why) {
        shaken++;
        shake(7);
        flash(why, 0xFFFF7060);
        sound(SoundEvents.GRAVEL_BREAK, .7f, .8f);
        for (int i = 0; i < 6; i++) {
            debris.add(new Debris(x, y, (random.nextFloat() - .5f) * 80, -random.nextFloat() * 60, time, 0xFF6A6058));
        }
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        swingAt = time;
        Point best = null;
        float bestD = 15;
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
        float err = Math.abs(ring(best) - INNER);
        float q;
        Component word;
        int color;
        if (err < 2.5f) {
            q = 1;
            word = Component.translatable("metier.arcanes_oublies.mining.clean");
            color = 0xFFFFE070;
        } else if (err < 6.5f) {
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
        shake(q >= 1 ? 4 : 2.5f);
        sound(q >= .75f ? SoundEvents.STONE_BREAK : SoundEvents.STONE_HIT, q >= 1 ? 1.2f : .9f, .9f);
        // A crack along the vein from the point, and chips of ore
        float a = random.nextFloat() * Mth.TWO_PI, len = 18 + q * 30;
        cracks.add(new float[] {best.x(), best.y(), best.x() + Mth.cos(a) * len, best.y() + Mth.sin(a) * len * .7f, q});
        for (int i = 0; i < 6 + (int) (q * 8); i++) {
            debris.add(new Debris(best.x(), best.y(), (random.nextFloat() - .5f) * 120, -40 - random.nextFloat() * 90, time, random.nextBoolean() ? ore : 0xFF7A7068));
        }
    }

    @Override
    protected void drawGame(GuiGraphics g, int mx, int my) {
        // Cave wall
        g.fillGradient(AX0, AY0, AX1, AY1, 0xFF201C1A, 0xFF100E0D);
        // The boulder, cell by cell
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 24; y++) {
                float px = CX - RX + x * (RX * 2 / 40f), py = CY - RY + y * (RY * 2 / 24f);
                float cx = px + 3, cy = py + 3;
                if (!inRock(cx, cy)) {
                    continue;
                }
                float shade = .55f + .45f * (1 - (cy - (CY - RY)) / (RY * 2)) - .15f * Math.abs((cx - CX) / RX);
                int base = cells[x][y] == 2 ? ore : cells[x][y] == 1 ? 0xFF8A8278 : 0xFF6E675F;
                int c = Draw.lerp(0xFF000000, base, Mth.clamp(shade, .2f, 1));
                g.fill((int) px, (int) py, (int) (px + RX * 2 / 40f) + 1, (int) (py + RY * 2 / 24f) + 1, c);
            }
        }
        // Cracks
        for (float[] c : cracks) {
            Draw.line(g, c[0], c[1], c[2], c[3], 2, 0xFF141210);
            Draw.line(g, c[0] + 1, c[1], (c[0] + c[2]) / 2 + 3, (c[1] + c[3]) / 2 - 2, 1, 0xFF262220);
        }
        // Weak points and their rings
        for (Point p : points) {
            if (time < p.born()) {
                continue;
            }
            float r = ring(p);
            float glow = .6f + .4f * Mth.sin(time * 14);
            Draw.disc(g, p.x(), p.y(), INNER + 2, Draw.argb(120, ore));
            Draw.disc(g, p.x(), p.y(), INNER - 2, Draw.argb((int) (200 + 55 * glow), 0xFFFFF0C0));
            // The star of the crack
            for (int k = 0; k < 4; k++) {
                float a = k * Mth.HALF_PI + .4f;
                Draw.line(g, p.x(), p.y(), p.x() + Mth.cos(a) * 9, p.y() + Mth.sin(a) * 9, 1, 0xFF1A1410);
            }
            float err = Math.abs(r - INNER);
            int rc = err < 2.5f ? 0xFFFFE070 : err < 6.5f ? 0xFFB8E0FF : 0xFFFFFFFF;
            Draw.ring(g, p.x(), p.y(), Math.max(1, r), 1.6f, Draw.argb(230, rc));
            Draw.ring(g, p.x(), p.y(), INNER, 1, 0x90FFE070);
        }
        // Chips and dust
        for (Debris d : debris) {
            float t = time - d.born();
            float x = d.x() + d.vx() * t, y = d.y() + d.vy() * t + 160 * t * t;
            int a = (int) (255 * (1 - t / 1.2f));
            g.fill((int) x - 1, (int) y - 1, (int) x + 2, (int) y + 2, Draw.argb(a, d.color()));
        }
        // How it holds: the strikes left and the rock's shaking
        for (int i = 0; i < strikes; i++) {
            int x = AX0 + 8 + i * 9;
            Draw.disc(g, x + 3, AY0 + 9, 3, i < done ? 0xFF5A5048 : ore);
        }
        Component hold = Component.translatable("metier.arcanes_oublies.mining.shaking");
        Draw.text(g, hold, AX1 - 80, AY0 + 5, 0xFFE0D0C0, .7f);
        for (int i = 0; i < maxShake; i++) {
            int x = AX1 - 20 - i * 12;
            g.fill(x, AY0 + 14, x + 9, AY0 + 19, i < shaken ? 0xFFFF5040 : 0x60FFFFFF);
        }
        // The pick, swinging when it strikes
        float swing = swingAt < 0 ? 0 : Mth.clamp((time - swingAt) / .18f, 0, 1);
        float angle = -35 + (swing < 1 ? 70 * Mth.sin(swing * Mth.PI) : 0);
        if (mx > AX0 && mx < AX1 && my > AY0 && my < AY1) {
            Draw.itemRotated(g, tool.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE) : tool, mx + 10, my - 10, 1.6f, angle);
        }
    }
}
