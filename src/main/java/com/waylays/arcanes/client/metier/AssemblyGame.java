package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Crafting, "à l'établi": the recipe is laid out on a 3 x 3 grid, as at a crafting table. One slot after the other
 * lights up: take the right ingredient from the tray and set it there before the light runs out (quick is better).
 * The wrong ingredient, or too slow, is a mistake; three spoil the work. When the grid is full, the press comes down:
 * strike as its needle crosses the gold mark to bind it all.
 */
public class AssemblyGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_assembly"), SLOT = Draw.tex("slot");
    private static final float GX = 64, GY = 60, S = 38, GAP = 4;
    private static final float RX = 262, RY = 121, RS = 54;
    private static final float TX = 352, TY0 = 62;

    private final List<ItemStack> kinds = new ArrayList<>();
    private final int[] pattern = new int[9];
    private final ItemStack[] placed = new ItemStack[9];
    private final float[] placedAt = new float[9];
    private final List<Integer> order = new ArrayList<>();
    private final int[] left;
    private final float stepTime;
    private final boolean hideGhosts;
    private final Draw.Bits bits = new Draw.Bits();
    private int step, mistakes, held = -1;
    private float stepStart, total, wrongAt = -9;
    private int wrongSlot = -1;
    // The press
    private boolean pressing;
    private float pressStart, pressQ = -1, pressAt = -9;
    private final float zone;

    public AssemblyGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                        Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF8A6438);
        this.stepTime = (float) (3.4 / difficulty);
        this.hideGhosts = difficulty > 1.25;
        this.zone = (float) (.08 / Math.sqrt(difficulty));
        List<ItemStack> in = new ArrayList<>();
        for (ItemStack s : inputs) {
            if (!s.isEmpty()) {
                in.add(s);
            }
        }
        if (in.isEmpty()) {
            in.add(new ItemStack(Items.OAK_PLANKS, 4));
        }
        // How many slots each ingredient takes (nine at most, at least one each)
        int sum = 0;
        for (ItemStack s : in) {
            sum += s.getCount();
        }
        int cells = Math.min(9, sum);
        int[] share = new int[in.size()];
        int given = 0;
        for (int i = 0; i < in.size(); i++) {
            share[i] = Math.max(1, Math.round(in.get(i).getCount() * cells / (float) sum));
            given += share[i];
        }
        while (given > cells) {
            int big = 0;
            for (int i = 1; i < share.length; i++) {
                if (share[i] > share[big]) big = i;
            }
            share[big]--;
            given--;
        }
        for (ItemStack s : in) {
            kinds.add(s.copyWithCount(1));
        }
        left = share.clone();
        // Where they go: a lone piece in the middle (or at the foot of a short column), the rest round it
        java.util.Arrays.fill(pattern, -1);
        int lone = -1;
        for (int i = 0; i < share.length; i++) {
            if (share[i] == 1 && in.size() > 1 && (lone < 0)) {
                lone = i;
            }
        }
        int[] spots;
        if (cells <= 3) {
            spots = new int[] {1, 4, 7};
        } else if (lone >= 0 && cells % 2 == 1) {
            spots = new int[] {4, 1, 3, 5, 7, 0, 2, 6, 8};
        } else {
            spots = new int[] {1, 3, 5, 7, 0, 2, 6, 8, 4};
        }
        int k = 0;
        if (lone >= 0) {
            int spot = cells <= 3 ? 7 : spots[0];
            pattern[spot] = lone;
            k = cells <= 3 ? 0 : 1;
        }
        for (int i = 0; i < share.length; i++) {
            int n = share[i] - (i == lone ? 1 : 0);
            for (int j = 0; j < n && k < spots.length; ) {
                if (pattern[spots[k]] < 0) {
                    pattern[spots[k]] = i;
                    j++;
                }
                k++;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (pattern[i] >= 0) {
                order.add(i);
            }
        }
        Collections.shuffle(order, random);
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return pressing ? List.of(control("click", "assembly.ctl_press")) : List.of(control("click", "assembly.ctl_take"), control("click", "assembly.ctl_place"));
    }

    @Override
    protected float liveScore() {
        float cells = order.isEmpty() ? 0 : total / order.size();
        float base = cells * (1 - .12f * mistakes);
        return Mth.clamp(.75f * base + .25f * (pressQ >= 0 ? pressQ : cells * .6f), 0, 1);
    }

    private float slotX(int i) {
        return GX + (i % 3) * (S + GAP);
    }

    private float slotY(int i) {
        return GY + (i / 3) * (S + GAP);
    }

    private int current() {
        return step < order.size() ? order.get(step) : -1;
    }

    private float tileY(int i) {
        return TY0 + i * 42;
    }

    @Override
    protected void update(float dt) {
        if (pressing) {
            if (pressQ < 0 && time - pressStart > 6) {
                pressQ = 0;
                finish(liveScore(), false, null);
            }
            return;
        }
        if (current() >= 0 && time - stepStart > stepTime) {
            mistake(current(), Component.translatable("metier.arcanes_oublies.assembly.too_slow"));
            int lost = current();
            placed[lost] = ItemStack.EMPTY;
            left[pattern[lost]] = Math.max(0, left[pattern[lost]] - 1);
            next(0);
        }
        if (mistakes >= 3) {
            finish(liveScore(), true, Component.translatable("metier.arcanes_oublies.assembly.spoiled"));
        }
    }

    private void mistake(int slot, Component why) {
        mistakes++;
        shake(5);
        flash(why, 0xFFFF7060);
        sound(SoundEvents.WOOD_BREAK, .7f, .7f);
        wrongAt = time;
        wrongSlot = slot;
    }

    private void next(float q) {
        total += q;
        step++;
        stepStart = time;
        if (step >= order.size() && mistakes < 3) {
            pressing = true;
            pressStart = time;
            held = -1;
            flash(Component.translatable("metier.arcanes_oublies.assembly.press_now"), 0xFFFFE6A0);
            sound(SoundEvents.PISTON_EXTEND, 1.2f, .5f);
        }
    }

    /** Where the press's needle is (0 to 1, back and forth). */
    private float needle() {
        float t = ((time - pressStart) * (float) (.8 * difficulty)) % 1;
        return t < .5f ? t * 2 : 2 - t * 2;
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (pressing) {
            if (pressQ >= 0) {
                return;
            }
            float d = Math.abs(needle() - .5f);
            pressQ = d < zone * .5f ? 1 : d < zone ? .75f : d < zone * 2.5f ? .4f : .1f;
            pressAt = time;
            shake(6);
            sound(SoundEvents.ANVIL_LAND, 1.4f, .4f);
            sound(SoundEvents.WOOD_PLACE, .8f, 1);
            flash(Component.translatable(pressQ >= 1 ? "metier.arcanes_oublies.assembly.fits" : pressQ >= .75f ? "metier.arcanes_oublies.assembly.good"
                    : "metier.arcanes_oublies.assembly.badly"), pressQ >= 1 ? 0xFFFFE070 : pressQ >= .75f ? 0xFFB8E0FF : 0xFFFFA060);
            bits.sparks(random, time, RX, RY, 18, 120, 0xFFFFE0A0);
            finish(liveScore(), false, null);
            return;
        }
        // The tray: take an ingredient
        for (int i = 0; i < kinds.size(); i++) {
            if (Math.abs(mx - (TX + 17)) < 19 && Math.abs(my - (tileY(i) + 17)) < 19) {
                held = i;
                sound(SoundEvents.WOOD_HIT, 1.4f, .45f);
                return;
            }
        }
        // The grid: set it down
        for (int i = 0; i < 9; i++) {
            if (mx >= slotX(i) && mx < slotX(i) + S && my >= slotY(i) && my < slotY(i) + S) {
                if (held < 0 || i != current()) {
                    return;
                }
                if (pattern[i] != held) {
                    mistake(i, Component.translatable("metier.arcanes_oublies.assembly.wrong_piece"));
                    return;
                }
                float f = (time - stepStart) / stepTime;
                float q = f < .4f ? 1 : f < .75f ? .8f : .55f;
                placed[i] = kinds.get(held);
                placedAt[i] = time;
                left[held] = Math.max(0, left[held] - 1);
                sound(SoundEvents.WOOD_PLACE, 1.1f + q * .3f, .8f);
                bits.sparks(random, time, slotX(i) + S / 2, slotY(i) + S / 2, q >= 1 ? 10 : 4, 70, 0xFFFFE6A0);
                if (left[held] == 0) {
                    held = -1;
                }
                next(q);
                return;
            }
        }
        held = -1;
    }

    private void slotPicture(GuiGraphics g, float x, float y, float s) {
        Draw.picture(g, SLOT, x - 2, y - 2, x + s + 2, y + s + 2, 72, 72, 0xFFFFFFFF);
    }

    /** {@code stack} faded into its slot (a ghost of what goes there). */
    private void ghost(GuiGraphics g, ItemStack stack, float cx, float cy, float size, int alpha) {
        Draw.itemAt(g, stack, cx, cy, size);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        Draw.rect(g, cx - size / 2 - 1, cy - size / 2 - 1, cx + size / 2 + 1, cy + size / 2 + 1, Draw.argb(255 - alpha, 0xFF221810));
        g.pose().popPose();
    }

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        // Warm light over the work
        Draw.glow(g, GX + 60, GY + 60, 120, 0x30FFC070);
        // The grid
        Draw.shadow(g, GX + 61, GY + 66, 170, 160, 150);
        int cur = current();
        float stepLeft = cur >= 0 ? 1 - (time - stepStart) / stepTime : 0;
        for (int i = 0; i < 9; i++) {
            float x = slotX(i), y = slotY(i), cx = x + S / 2, cy = y + S / 2;
            boolean lit = i == cur;
            if (lit) {
                float pulse = .6f + .4f * Mth.sin(time * 9);
                int c = stepLeft > .4f ? 0xFFFFD070 : Draw.lerp(0xFFFF5030, 0xFFFFD070, stepLeft / .4f);
                Draw.glow(g, cx, cy, S * .95f, Draw.argb((int) (120 * pulse), c));
            }
            slotPicture(g, x, y, S);
            if (placed[i] != null) {
                if (placed[i].isEmpty()) {
                    Draw.line(g, x + 10, y + 10, x + S - 10, y + S - 10, 2, 0xA0802A20);
                    Draw.line(g, x + S - 10, y + 10, x + 10, y + S - 10, 2, 0xA0802A20);
                } else {
                    float pop = Draw.back(Math.min(1, (time - placedAt[i]) * 5));
                    Draw.itemAt(g, placed[i], cx, cy, 26 * pop);
                }
            } else if (pattern[i] >= 0 && (!hideGhosts || time < 2.2f)) {
                ghost(g, kinds.get(pattern[i]), cx, cy, 24, lit ? 130 : 60);
            }
            if (lit) {
                int c = stepLeft > .4f ? 0xFFFFE6A0 : 0xFFFF7050;
                Draw.rect(g, x - 2, y - 2, x + S + 2, y - 1, c);
                Draw.rect(g, x - 2, y + S + 1, x + S + 2, y + S + 2, c);
                Draw.rect(g, x - 2, y - 2, x - 1, y + S + 2, c);
                Draw.rect(g, x + S + 1, y - 2, x + S + 2, y + S + 2, c);
                // The light running out, along the slot's foot
                Draw.pill(g, x + 3, y + S + 4, x + 3 + (S - 6) * Mth.clamp(stepLeft, 0, 1), y + S + 7, c);
            }
            if (i == wrongSlot && time - wrongAt < .5f) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 220);
                Draw.rect(g, x, y, x + S, y + S, Draw.argb((int) (160 * (1 - (time - wrongAt) / .5f)), 0xFFFF3020));
                g.pose().popPose();
            }
        }
        // The arrow to what is made
        float ax0 = GX + 3 * S + 2 * GAP + 10, ax1 = RX - RS / 2 - 10;
        Draw.stroke(g, ax0, RY, ax1, RY, 4, 0xC0F0DCB0);
        Draw.stroke(g, ax1, RY, ax1 - 8, RY - 7, 4, 0xC0F0DCB0);
        Draw.stroke(g, ax1, RY, ax1 - 8, RY + 7, 4, 0xC0F0DCB0);
        // What is made: faint, filling in as the grid does, whole once pressed
        float made = order.isEmpty() ? 0 : (float) step / order.size();
        Draw.glow(g, RX, RY, RS * (.8f + .5f * made), Draw.argb((int) (40 + 100 * made), 0xFFFFD890));
        slotPicture(g, RX - RS / 2, RY - RS / 2, RS);
        if (pressQ >= 0) {
            float pop = Draw.back(Math.min(1, (time - pressAt) * 4));
            Draw.itemAt(g, subject, RX, RY, 40 * pop);
        } else {
            ghost(g, subject, RX, RY, 38, (int) (50 + 140 * made));
        }
        // The press: a needle running to and fro, a gold mark in the middle
        if (pressing) {
            float bx0 = RX - 60, bx1 = RX + 60, by = RY + RS / 2 + 16;
            Draw.pill(g, bx0 - 2, by - 2, bx1 + 2, by + 10, 0xE0140E0A);
            Draw.pill(g, bx0, by, bx1, by + 8, 0xFF3A2A1C);
            float zc = (bx0 + bx1) / 2, zw = (bx1 - bx0) * zone;
            Draw.rect(g, zc - zw * 2.5f, by, zc + zw * 2.5f, by + 8, 0x60FFD070);
            Draw.rect(g, zc - zw, by, zc + zw, by + 8, 0xD0FFD070);
            Draw.glow(g, zc, by + 4, 14, 0x60FFD070);
            float nx = bx0 + (bx1 - bx0) * (pressQ >= 0 ? .5f : needle());
            Draw.glow(g, nx, by + 4, 9, 0xA0FFFFFF);
            Draw.rect(g, nx - 1, by - 5, nx + 1, by + 13, 0xFFFFFFFF);
        }
        // The tray: the ingredients, and how many are still to place
        Draw.panel(g, TX - 8, TY0 - 18, TX + 44, TY0 + Math.max(1, kinds.size()) * 42 - 4, 0xFF5A3A20);
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.assembly.tray"), TX + 18, TY0 - 13, 0xFFF0DCB8, .75f);
        for (int i = 0; i < kinds.size(); i++) {
            float x = TX, y = tileY(i);
            boolean over = Math.abs(mx - (x + 17)) < 19 && Math.abs(my - (y + 17)) < 19;
            if (i == held) {
                Draw.glow(g, x + 17, y + 17, 26, 0x70FFE0A0);
            } else if (over) {
                Draw.glow(g, x + 17, y + 17, 22, 0x40FFFFFF);
            }
            slotPicture(g, x, y, 34);
            Draw.itemAt(g, kinds.get(i), x + 17, y + 17, i == held ? 22 : 26);
            g.pose().pushPose();
            g.pose().translate(0, 0, 250);
            Component n = Component.literal("×" + left[i]);
            Draw.text(g, n, x + 33 - Draw.width(n, .85f), y + 25, left[i] == 0 ? 0xFF8A7A6A : 0xFFFFFFFF, .85f);
            g.pose().popPose();
        }
        // Mistakes
        for (int i = 0; i < 3; i++) {
            float x = GX + i * 14 + 6, y = GY + 3 * S + 2 * GAP + 16;
            Draw.disc(g, x, y, 5, i < mistakes ? 0xFFC0402A : 0x40000000);
            if (i < mistakes) {
                Draw.line(g, x - 2.5f, y - 2.5f, x + 2.5f, y + 2.5f, 1.4f, 0xFFFFE0D0);
                Draw.line(g, x + 2.5f, y - 2.5f, x - 2.5f, y + 2.5f, 1.4f, 0xFFFFE0D0);
            }
        }
        bits.draw(g, time);
        // The ingredient in hand follows the mouse
        if (held >= 0 && inArea(mx, my)) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            Draw.shadow(g, mx + 3, my + 12, 26, 10, 110);
            Draw.itemAt(g, kinds.get(held), mx, my, 28);
            g.pose().popPose();
        }
    }
}
