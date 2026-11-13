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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Crafting, "le plan": the blueprint shows, step by step, the outline of the next piece to fit, turned the way it must
 * go. Below it, three pieces cut from the ingredients, each lying turned some way: take the right one (click), turn it
 * (right click, the wheel or R) until it matches the outline, and set it on the plan (click). The right piece the right
 * way, quickly, is a clean fit; turned wrong it holds badly; the wrong piece, or the time out, is a mistake, and three
 * mistakes spoil the work. The made thing shows through the plan as it comes together.
 */
public class AssemblyGame extends MinigameScreen {
    private static final int[][][] SHAPES = {
            {{0, 0}, {1, 0}, {2, 0}, {1, 1}},
            {{0, 0}, {0, 1}, {0, 2}, {1, 2}},
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{0, 0}, {1, 0}, {2, 0}, {0, 1}, {2, 1}},
            {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}},
            {{0, 0}, {0, 1}, {1, 1}, {1, 2}, {2, 2}},
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {1, 1}},
            {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}},
    };
    private static final int[] COLORS = {0xFFC08850, 0xFF8AA0B8, 0xFFB86A50, 0xFF7AA868, 0xFFC8B070, 0xFF9A80C0};
    private static final int CELL = 9;
    private static final float PLAN_X = 170, PLAN_Y = 82;

    private final int steps;
    private final float stepTime;
    private int step, mistakes;
    private float stepStart;
    private float total;
    // The step's target and its offers
    private int targetShape, targetTurn;
    private final int[] offerShape = new int[3], offerTurn = new int[3], offerColor = new int[3];
    private int held = -1, heldTurn;
    private final List<int[]> placed = new ArrayList<>();
    private float blink = -1;

    public AssemblyGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                        Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF3A6EA8);
        this.steps = 4 + (int) Math.round(difficulty * 1.5);
        this.stepTime = (float) (8.0 / difficulty);
        newStep();
    }

    @Override
    protected List<Component> rules() {
        return List.of(Component.translatable("metier.arcanes_oublies.assembly.rule1"), Component.translatable("metier.arcanes_oublies.assembly.rule2"),
                Component.translatable("metier.arcanes_oublies.assembly.rule3"));
    }

    @Override
    protected float liveScore() {
        return total / steps;
    }

    private void newStep() {
        targetShape = random.nextInt(SHAPES.length);
        targetTurn = random.nextInt(4);
        int right = random.nextInt(3);
        Set<Integer> used = new HashSet<>();
        used.add(targetShape);
        for (int i = 0; i < 3; i++) {
            if (i == right) {
                offerShape[i] = targetShape;
            } else {
                int s;
                do {
                    s = random.nextInt(SHAPES.length);
                } while (used.contains(s));
                used.add(s);
                offerShape[i] = s;
            }
            offerTurn[i] = random.nextInt(4);
            offerColor[i] = random.nextInt(COLORS.length);
        }
        held = -1;
        stepStart = time;
    }

    /** The cells of shape {@code s} turned {@code turn} quarters, moved to start at 0, 0. */
    private static int[][] cells(int s, int turn) {
        int[][] src = SHAPES[s];
        int[][] out = new int[src.length][2];
        for (int i = 0; i < src.length; i++) {
            int x = src[i][0], y = src[i][1];
            for (int t = 0; t < turn; t++) {
                int nx = -y;
                y = x;
                x = nx;
            }
            out[i][0] = x;
            out[i][1] = y;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for (int[] c : out) {
            minX = Math.min(minX, c[0]);
            minY = Math.min(minY, c[1]);
        }
        for (int[] c : out) {
            c[0] -= minX;
            c[1] -= minY;
        }
        return out;
    }

    private static Set<Long> key(int[][] cells) {
        Set<Long> k = new HashSet<>();
        for (int[] c : cells) {
            k.add((long) c[0] << 32 | (c[1] & 0xFFFFFFFFL));
        }
        return k;
    }

    private static boolean sameWay(int s, int turnA, int turnB) {
        return key(cells(s, turnA)).equals(key(cells(s, turnB)));
    }

    @Override
    protected void update(float dt) {
        if (time - stepStart > stepTime) {
            mistake(Component.translatable("metier.arcanes_oublies.assembly.too_slow"));
            next(0);
        }
        if (mistakes >= 3) {
            finish(liveScore(), true, Component.translatable("metier.arcanes_oublies.assembly.spoiled"));
        }
    }

    private void mistake(Component why) {
        mistakes++;
        shake(5);
        flash(why, 0xFFFF7060);
        sound(SoundEvents.WOOD_BREAK, .7f, .7f);
        blink = time;
    }

    private void next(float q) {
        total += q;
        step++;
        if (step >= steps) {
            finish(liveScore(), mistakes >= 3, null);
        } else {
            newStep();
        }
    }

    private float offerX(int i) {
        return 70 + i * 100;
    }

    private static final float OFFER_Y = 160;

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (button == 1) {
            turn();
            return;
        }
        if (held < 0) {
            for (int i = 0; i < 3; i++) {
                if (Math.abs(mx - offerX(i)) < 30 && Math.abs(my - OFFER_Y) < 22) {
                    held = i;
                    heldTurn = offerTurn[i];
                    sound(SoundEvents.WOOD_HIT, 1.3f, .5f);
                    return;
                }
            }
            return;
        }
        // On the plan: set it down
        if (Math.abs(mx - PLAN_X) < 60 && Math.abs(my - PLAN_Y) < 42) {
            if (offerShape[held] != targetShape) {
                mistake(Component.translatable("metier.arcanes_oublies.assembly.wrong_piece"));
                held = -1;
                return;
            }
            float q;
            if (sameWay(targetShape, heldTurn, targetTurn)) {
                q = time - stepStart < stepTime * .5f ? 1 : .8f;
                flash(Component.translatable("metier.arcanes_oublies.assembly.fits"), 0xFFFFE070);
                sound(SoundEvents.WOOD_PLACE, 1.2f, .9f);
            } else {
                q = .4f;
                flash(Component.translatable("metier.arcanes_oublies.assembly.badly"), 0xFFFFA060);
                sound(SoundEvents.WOOD_PLACE, .7f, .9f);
            }
            placed.add(new int[] {targetShape, targetTurn, offerColor[held]});
            next(q);
        } else {
            held = -1;
        }
    }

    private void turn() {
        if (held >= 0) {
            heldTurn = (heldTurn + 1) % 4;
            sound(SoundEvents.WOOD_HIT, 1.6f, .3f);
        }
    }

    @Override
    protected void onScroll(double mx, double my, double dy) {
        if (held >= 0) {
            heldTurn = (heldTurn + (dy > 0 ? 1 : 3)) % 4;
        }
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_R) {
            turn();
        }
    }

    private void drawShape(GuiGraphics g, int s, int turn, float cx, float cy, int fill, int edge, boolean outline) {
        int[][] cs = cells(s, turn);
        int w = 0, h = 0;
        for (int[] c : cs) {
            w = Math.max(w, c[0] + 1);
            h = Math.max(h, c[1] + 1);
        }
        float x0 = cx - w * CELL / 2f, y0 = cy - h * CELL / 2f;
        for (int[] c : cs) {
            int x = (int) (x0 + c[0] * CELL), y = (int) (y0 + c[1] * CELL);
            if (outline) {
                g.renderOutline(x, y, CELL, CELL, edge);
            } else {
                g.fill(x, y, x + CELL, y + CELL, fill);
                g.fill(x, y, x + CELL, y + 1, Draw.lerp(fill, 0xFFFFFFFF, .35f));
                g.renderOutline(x, y, CELL, CELL, edge);
            }
        }
    }

    @Override
    protected void drawGame(GuiGraphics g, int mx, int my) {
        // The workbench
        g.fillGradient(AX0, AY0, AX1, AY1, 0xFF6A4A2E, 0xFF4A3220);
        for (int y = AY0 + 6; y < AY1; y += 14) {
            g.fill(AX0, y, AX1, y + 1, 0x30000000);
        }
        // The blueprint
        int bx0 = (int) PLAN_X - 70, by0 = (int) PLAN_Y - 44, bx1 = (int) PLAN_X + 70, by1 = (int) PLAN_Y + 44;
        g.fill(bx0, by0, bx1, by1, 0xFF1E4A84);
        for (int x = bx0; x < bx1; x += 7) {
            g.fill(x, by0, x + 1, by1, 0x30FFFFFF);
        }
        for (int y = by0; y < by1; y += 7) {
            g.fill(bx0, y, bx1, y + 1, 0x30FFFFFF);
        }
        g.renderOutline(bx0, by0, bx1 - bx0, by1 - by0, 0xFFD8E8FF);
        // What is made, showing through as it comes together
        float made = (float) step / steps;
        g.setColor(1, 1, 1, .15f + .7f * made);
        Draw.item(g, subject, PLAN_X - 24, PLAN_Y - 24, 3);
        g.setColor(1, 1, 1, 1);
        // The outline to fit, blinking red after a mistake
        boolean red = blink >= 0 && time - blink < .4f;
        int pulse = (int) (180 + 70 * Mth.sin(time * 6));
        drawShape(g, targetShape, targetTurn, PLAN_X, PLAN_Y, 0, Draw.argb(pulse, red ? 0xFFFF5050 : 0xFFFFFFFF), true);
        // The steps
        for (int i = 0; i < steps; i++) {
            int x = bx0 + 4 + i * 8;
            g.fill(x, by1 - 7, x + 6, by1 - 3, i < step ? 0xFFFFE070 : 0x50FFFFFF);
        }
        // The time for this step
        float left = 1 - (time - stepStart) / stepTime;
        g.fill(bx0, by1 + 3, bx0 + (int) ((bx1 - bx0) * Mth.clamp(left, 0, 1)), by1 + 6, left > .5f ? 0xFF90D070 : left > .25f ? 0xFFE0C050 : 0xFFE05040);
        // Mistakes
        for (int i = 0; i < 3; i++) {
            Draw.text(g, Component.literal("✘"), AX1 - 40 + i * 11, AY0 + 6, i < mistakes ? 0xFFFF5040 : 0x50FFFFFF, 1);
        }
        // The ingredients on the bench
        for (int i = 0; i < Math.min(inputs.size(), 5); i++) {
            Draw.item(g, inputs.get(i), AX0 + 8 + i * 18, AY0 + 6, 1);
        }
        // The three pieces
        for (int i = 0; i < 3; i++) {
            if (i == held) {
                continue;
            }
            boolean hover = Math.abs(mx - offerX(i)) < 30 && Math.abs(my - OFFER_Y) < 22;
            g.fill((int) offerX(i) - 32, (int) OFFER_Y - 24, (int) offerX(i) + 32, (int) OFFER_Y + 24, hover ? 0x40FFFFFF : 0x20000000);
            drawShape(g, offerShape[i], offerTurn[i], offerX(i), OFFER_Y, COLORS[offerColor[i]], 0xFF2A1A10, false);
        }
        // The piece in hand follows the mouse
        if (held >= 0) {
            drawShape(g, offerShape[held], heldTurn, mx, my, Draw.argb(220, COLORS[offerColor[held]]), 0xFFFFFFFF, false);
            Draw.text(g, Component.translatable("metier.arcanes_oublies.assembly.turn_hint"), AX0 + 8, AY1 - 12, 0xFFF0E0C8, .7f);
        }
    }
}
