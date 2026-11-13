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
 * Distilling, "l'alambic": first bring the still up to heat and hold it in the band (the flame is set with the wheel,
 * W/S or by dragging the valve; the heat follows slowly). Then add each ingredient when the mixture, whose colour
 * drifts, matches the ingredient's swatch. Last, keep the heat in the band while the drops condense: a drop that falls
 * in the band is pure. Too hot too long and the flask bursts.
 */
public class DistillGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_distill"), FLASK = Draw.tex("flask"), FILL = Draw.tex("flask_fill");
    private static final int DROPS = 12;
    private static final float HEAT_HOLD = 2.5f;
    private static final float FX = 132, FY0 = 64, FW = 84, FH = 105;
    private static final float BURNER_Y = 190;
    private static final float VX = 36, VY0 = 92, VY1 = 184;
    private static final float TX = 386, TY0 = 66, TY1 = 200, MAX = 1.1f;
    private static final float RCX = 262, RCY = 168;

    private enum Phase {HEAT, INGREDIENTS, CONDENSE}

    private record Bubble(float x, float born, float speed, float size) {}

    private final float lo, hi;
    private final float[] targets;
    private Phase phase = Phase.HEAT;
    private float flame = .3f, temp = .15f, hold, holdTotal, over, hue, hueSpeed, ingredientsQ, lastAdd = -9, burstAt = -1;
    private int ingredient, pure, fallen;
    private float nextDrop;
    private boolean dragging;
    private final List<Bubble> bubbles = new ArrayList<>();
    private final List<float[]> drops = new ArrayList<>();
    private final Draw.Fire fire;
    private final Draw.Bits bits = new Draw.Bits();

    public DistillGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                       Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF7A4AA8);
        float half = (float) (.11 / Math.sqrt(difficulty));
        this.lo = .62f - half;
        this.hi = .62f + half;
        int n = Math.max(1, Math.min(4, inputs.isEmpty() ? 2 : inputs.size()));
        targets = new float[n];
        for (int i = 0; i < n; i++) {
            targets[i] = (i * .618f + random.nextFloat() * .2f) % 1;
        }
        hue = random.nextFloat();
        hueSpeed = (float) (.16 * Math.pow(difficulty, .7));
        fire = new Draw.Fire(random, true);
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return switch (phase) {
            case INGREDIENTS -> List.of(control("wheel", "distill.ctl_flame"), control("click", "distill.ctl_add"));
            default -> List.of(control("wheel", "distill.ctl_flame"), control("drag", "distill.ctl_valve"));
        };
    }

    @Override
    protected float liveScore() {
        float heatPart = Math.min(1, holdTotal / HEAT_HOLD);
        float ingPart = targets.length == 0 ? 1 : ingredientsQ / targets.length;
        return Mth.clamp(.15f * heatPart + .35f * ingPart + .5f * pure / (float) DROPS, 0, 1);
    }

    private boolean inBand() {
        return temp >= lo && temp <= hi;
    }

    @Override
    protected void update(float dt) {
        fire.update(time, dt, FX - 12, FX + 12, BURNER_Y, flame);
        temp += (flame * 1.05f - temp) * dt * .55f * (float) Math.pow(difficulty, .25);
        temp = Mth.clamp(temp, 0, MAX);
        if (temp > .95f) {
            over += dt;
            shake(over * 2);
            if (over > 1.2f) {
                burstAt = time;
                sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, .5f);
                sound(SoundEvents.GLASS_BREAK, 1, .8f);
                for (int i = 0; i < 20; i++) {
                    float a = random.nextFloat() * Mth.TWO_PI, v = 60 + random.nextFloat() * 120;
                    bits.add(time, FX, FY0 + 70, Mth.cos(a) * v, Mth.sin(a) * v - 60, 1, 2 + random.nextFloat() * 2, 0xFFD0E8FF, 300, false);
                }
                finish(liveScore() * .4f, true, Component.translatable("metier.arcanes_oublies.distill.burst"));
                return;
            }
        } else {
            over = Math.max(0, over - dt);
        }
        if (temp > .4f && random.nextFloat() < dt * (4 + temp * 16)) {
            bubbles.add(new Bubble((random.nextFloat() - .5f) * 40, time, 14 + random.nextFloat() * 22 * temp, 1 + random.nextFloat() * 2));
        }
        bubbles.removeIf(b -> time - b.born() > 1.6f);
        hue = (hue + hueSpeed * dt * (1 + .4f * Mth.sin(time * 1.7f))) % 1;
        switch (phase) {
            case HEAT -> {
                if (inBand()) {
                    hold += dt;
                    holdTotal += dt;
                    if (hold >= HEAT_HOLD) {
                        phase = Phase.INGREDIENTS;
                        flash(Component.translatable("metier.arcanes_oublies.distill.add_now"), 0xFFE0C8FF);
                        sound(SoundEvents.BREWING_STAND_BREW, 1.2f, .6f);
                    }
                } else {
                    hold = Math.max(0, hold - dt * 2);
                }
            }
            case INGREDIENTS -> {
                if (time > 40) {
                    finish(liveScore(), false, Component.translatable("metier.arcanes_oublies.distill.timeout"));
                }
            }
            case CONDENSE -> {
                if (time >= nextDrop) {
                    boolean p = inBand();
                    drops.add(new float[] {time, p ? 1 : 0});
                    fallen++;
                    if (p) {
                        pure++;
                    }
                    sound(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, p ? 1.4f : .8f, .7f);
                    nextDrop = time + (float) (1.5 / Math.pow(difficulty, .3));
                    if (fallen >= DROPS) {
                        finish(liveScore(), false, null);
                    }
                }
                drops.removeIf(d -> time - d[0] > .9f);
            }
        }
    }

    private static float hueGap(float a, float b) {
        float d = Math.abs(a - b) % 1;
        return Math.min(d, 1 - d);
    }

    private void setFlame(double my) {
        flame = Mth.clamp((float) ((VY1 - my) / (VY1 - VY0)), 0, 1);
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (Math.abs(mx - VX) < 14 && my > VY0 - 8 && my < VY1 + 8) {
            dragging = true;
            setFlame(my);
            return;
        }
        if (phase != Phase.INGREDIENTS || ingredient >= targets.length || time - lastAdd < .4f) {
            return;
        }
        lastAdd = time;
        float gap = hueGap(hue, targets[ingredient]);
        float tol = (float) (.09 / Math.sqrt(difficulty));
        float q = gap < tol * .5f ? 1 : gap < tol ? .7f : gap < tol * 2 ? .35f : 0;
        if (!inBand()) {
            q *= .6f;
        }
        ingredientsQ += q;
        flash(Component.translatable(q >= 1 ? "metier.arcanes_oublies.distill.exact" : q >= .7f ? "metier.arcanes_oublies.distill.close"
                : q > 0 ? "metier.arcanes_oublies.distill.off" : "metier.arcanes_oublies.distill.wrong"), q >= 1 ? 0xFFFFE070 : q >= .7f ? 0xFFB8E0FF
                : q > 0 ? 0xFFFFA060 : 0xFFFF6050);
        sound(SoundEvents.BREWING_STAND_BREW, .8f + q * .6f, .5f);
        sound(SoundEvents.BOTTLE_EMPTY, 1.2f, .5f);
        if (q <= 0) {
            shake(4);
        }
        bits.sparks(random, time, FX, FY0 + 50, 6 + (int) (q * 10), 60, Draw.hsv(hue, .6f, 1));
        ingredient++;
        if (ingredient >= targets.length) {
            phase = Phase.CONDENSE;
            nextDrop = time + 1.2f;
            flash(Component.translatable("metier.arcanes_oublies.distill.condense"), 0xFFB8E0FF);
        }
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        dragging = false;
    }

    @Override
    protected void onMouseDrag(double mx, double my, int button) {
        if (dragging) {
            setFlame(my);
        }
    }

    @Override
    protected void onScroll(double mx, double my, double dy) {
        flame = Mth.clamp(flame + (float) dy * .05f, 0, 1);
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_Z) {
            flame = Mth.clamp(flame + .08f, 0, 1);
        } else if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) {
            flame = Mth.clamp(flame - .08f, 0, 1);
        }
    }

    private float gaugeY(float v) {
        return TY1 - v / MAX * (TY1 - TY0);
    }

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        int mix = phase == Phase.HEAT ? Draw.lerp(0xFF8A9AB8, Draw.hsv(hue, .7f, .95f), Mth.clamp(temp * 1.2f, 0, 1)) : Draw.hsv(hue, .72f, .98f);
        boolean burst = burstAt >= 0;
        // Candle-warm light and the mixture's own glow
        Draw.glow(g, FX, FY0 + 70, 90, Draw.argb((int) (40 + 60 * temp), mix));
        // The burner: a brass dish, a blue-rooted flame
        Draw.shadow(g, FX, BURNER_Y + 6, 70, 12, 160);
        fire.draw(g, time, FX - 12, FX + 12, BURNER_Y, flame);
        Draw.rect(g, FX - 22, BURNER_Y, FX + 22, BURNER_Y + 5, 0xFF8A6A30);
        Draw.rect(g, FX - 22, BURNER_Y, FX + 22, BURNER_Y + 1, 0xFFE0C070);
        Draw.rect(g, FX - 16, BURNER_Y + 5, FX + 16, BURNER_Y + 9, 0xFF5A4420);
        // The stand's legs
        Draw.stroke(g, FX - 30, BURNER_Y + 8, FX - 24, FY0 + FH - 14, 2, 0xFF3A3436);
        Draw.stroke(g, FX + 30, BURNER_Y + 8, FX + 24, FY0 + FH - 14, 2, 0xFF3A3436);
        Draw.stroke(g, FX - 28, FY0 + FH - 14, FX + 28, FY0 + FH - 14, 2, 0xFF4A4446);
        if (!burst) {
            // The liquid, to its level, and its bubbles
            float level = .45f + .25f * Math.min(1, ingredient / (float) targets.length);
            float ly = FY0 + FH - FH * level * .62f;
            g.enableScissor((int) (left + (FX - FW / 2) * k), (int) (top + ly * k), (int) Math.ceil(left + (FX + FW / 2) * k),
                    (int) Math.ceil(top + (FY0 + FH) * k));
            Draw.picture(g, FILL, FX - FW / 2, FY0, FX + FW / 2, FY0 + FH, 160, 200, Draw.argb(230, mix));
            g.disableScissor();
            float wave = Mth.sin(time * 4) * .8f;
            Draw.rect(g, FX - 30 + Math.abs(ly - (FY0 + 66)) * .1f, ly + wave, FX + 30, ly + wave + 1, Draw.argb(150, Draw.lerp(mix, 0xFFFFFFFF, .5f)));
            for (Bubble b : bubbles) {
                float t = time - b.born();
                float y = FY0 + FH - 10 - t * b.speed();
                if (y > ly + 2) {
                    Draw.disc(g, FX + b.x() * (.6f + .4f * (y - FY0) / FH), y, b.size(), 0x90FFFFFF);
                }
            }
            Draw.picture(g, FLASK, FX - FW / 2, FY0, FX + FW / 2, FY0 + FH, 160, 200, 0xFFFFFFFF);
            // Vapour rising from the neck when hot
            if (temp > .5f && random.nextFloat() < .3f) {
                bits.add(time, FX + (random.nextFloat() - .5f) * 6, FY0 + 2, (random.nextFloat() - .5f) * 6, -12, 1.2f, 6, 0xFFE8E0F0, -6, false);
            }
        }
        // The coil running to the receiver
        float nx = FX, ny = FY0 + 4;
        Draw.stroke(g, nx, ny, nx + 6, ny - 8, 3.5f, 0xFFB87A42);
        Draw.stroke(g, nx + 6, ny - 8, RCX - 10, ny - 8, 3.5f, 0xFFB87A42);
        Draw.stroke(g, RCX - 10, ny - 8, RCX - 10, RCY - 62, 3.5f, 0xFFB87A42);
        for (int i = 0; i < 4; i++) {
            float y = RCY - 96 + i * 9;
            Draw.stroke(g, RCX - 18, y, RCX - 2, y + 4, 3, 0xFFC88A50);
            Draw.stroke(g, RCX - 2, y + 4, RCX - 18, y + 9, 3, 0xFF8A5A2A);
        }
        Draw.stroke(g, RCX - 10, RCY - 60, RCX - 10, RCY - 46, 3, 0xFFB87A42);
        Draw.line(g, nx + 6, ny - 9.2f, RCX - 10, ny - 9.2f, 1, 0x80FFE0C0);
        // The receiver, filling with what has dripped
        for (float[] d : drops) {
            float t = (time - d[0]) / .9f;
            float y = RCY - 46 + t * 26;
            Draw.disc(g, RCX - 10, y, 2, d[1] > 0 ? Draw.lerp(Draw.hsv(hue, .4f, 1), 0xFFFFFFFF, .3f) : 0xFF8A8A90);
            Draw.glow(g, RCX - 10, y, 5, d[1] > 0 ? 0x60E0D0FF : 0x20FFFFFF);
        }
        float rw = 40, rh = 50;
        float fill = Math.min(1, (pure + (fallen - pure) * .5f) / DROPS);
        if (fill > 0) {
            float ly = RCY + rh / 2 - rh * .62f * fill;
            g.enableScissor((int) (left + (RCX - 10 - rw / 2) * k), (int) (top + ly * k), (int) Math.ceil(left + (RCX - 10 + rw / 2) * k),
                    (int) Math.ceil(top + (RCY + rh / 2) * k));
            int good = fallen == 0 ? 0xFFE0D0FF : Draw.lerp(0xFF8A8A92, 0xFFD8B8FF, pure / (float) fallen);
            Draw.picture(g, FILL, RCX - 10 - rw / 2, RCY - rh / 2, RCX - 10 + rw / 2, RCY + rh / 2, 160, 200, Draw.argb(230, good));
            g.disableScissor();
        }
        Draw.picture(g, FLASK, RCX - 10 - rw / 2, RCY - rh / 2, RCX - 10 + rw / 2, RCY + rh / 2, 160, 200, 0xFFFFFFFF);
        // The ingredients waiting, each with the colour it must go in at
        float cx0 = 300;
        for (int i = 0; i < targets.length; i++) {
            float y = AY0 + 30 + i * 40;
            boolean done = i < ingredient, current = i == ingredient && phase == Phase.INGREDIENTS;
            Draw.panel(g, cx0, y, cx0 + 60, y + 34, current ? 0xFFB08AE0 : 0xFF4A3A5A);
            if (current) {
                Draw.glow(g, cx0 + 30, y + 17, 40, 0x50C0A0FF);
            }
            if (i < inputs.size()) {
                Draw.itemAt(g, inputs.get(i), cx0 + 16, y + 17, 22);
            }
            int sw = Draw.hsv(targets[i], .75f, .98f);
            Draw.disc(g, cx0 + 44, y + 17, 9, 0xFF141018);
            Draw.disc(g, cx0 + 44, y + 17, 7.5f, done ? 0x50FFFFFF : sw);
            if (done) {
                Draw.stroke(g, cx0 + 40, y + 17, cx0 + 43, y + 21, 2, 0xFF90FF90);
                Draw.stroke(g, cx0 + 43, y + 21, cx0 + 49, y + 12, 2, 0xFF90FF90);
            }
        }
        if (phase == Phase.INGREDIENTS && ingredient < targets.length) {
            // The mixture's colour beside the swatch to match
            float y = AY0 + 30 + ingredient * 40 + 17;
            Draw.disc(g, cx0 - 14, y, 9, 0xFF141018);
            Draw.disc(g, cx0 - 14, y, 7.5f, mix);
            float gap = hueGap(hue, targets[ingredient]);
            if (gap < .09f / Math.sqrt(difficulty)) {
                Draw.glow(g, cx0 - 14, y, 16, 0x90FFFFFF);
            }
        }
        bits.draw(g, time);
        // The valve: a brass lever up and down a slot
        Draw.pill(g, VX - 4, VY0 - 4, VX + 4, VY1 + 4, 0xE0100A08);
        Draw.vgrad(g, VX - 2, VY0, VX + 2, VY1, 0xFFFF8A40, 0xFF3A5AC0);
        float ky = VY1 - (VY1 - VY0) * flame;
        Draw.glow(g, VX, ky, 12, dragging ? 0x90FFE070 : 0x40FFE070);
        Draw.disc(g, VX, ky, 7, 0xFF2A2018);
        Draw.disc(g, VX, ky, 6, dragging ? 0xFFFFD870 : 0xFFD0A040);
        Draw.disc(g, VX - 1.5f, ky - 1.5f, 2, 0xA0FFFFFF);
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.distill.flame"), VX, VY0 - 16, 0xFFE8D8FF, .8f);
        // The heat gauge
        Draw.pill(g, TX - 4, TY0 - 4, TX + 18, TY1 + 4, 0xE0100A08);
        Draw.vgrad(g, TX, TY0 + 7, TX + 14, TY1 - 7, 0xFFE04030, 0xFF3050B0);
        Draw.disc(g, TX + 7, TY0 + 7, 7, 0xFFE04030);
        Draw.disc(g, TX + 7, TY1 - 7, 7, 0xFF3050B0);
        float yl = gaugeY(lo), yh = gaugeY(hi);
        Draw.rect(g, TX, yh, TX + 14, yl, 0x6090FF90);
        Draw.rect(g, TX - 3, yh, TX + 17, yh + 1, 0xFF90FF90);
        Draw.rect(g, TX - 3, yl - 1, TX + 17, yl, 0xFF90FF90);
        float yr = gaugeY(.95f);
        Draw.rect(g, TX - 5, yr - .5f, TX + 19, yr + 1, 0xFFFF3A2A);
        float yt = gaugeY(temp);
        Draw.glow(g, TX + 7, yt, 12, inBand() ? 0xB0A0FFA0 : temp > .95f ? 0xC0FF5040 : 0x90FFFFFF);
        Draw.pill(g, TX - 6, yt - 2, TX + 20, yt + 2, 0xFFFFFFFF);
        // Where we are
        Component phaseText = switch (phase) {
            case HEAT -> Component.translatable("metier.arcanes_oublies.distill.phase_heat");
            case INGREDIENTS -> Component.translatable("metier.arcanes_oublies.distill.phase_ingredients", ingredient + 1, targets.length);
            case CONDENSE -> Component.translatable("metier.arcanes_oublies.distill.phase_condense", fallen, DROPS);
        };
        Draw.text(g, phaseText, AX0 + 62, AY0 + 9, 0xFFF0E4FF, .95f);
        if (phase == Phase.HEAT) {
            Draw.pill(g, AX0 + 62, AY0 + 22, AX0 + 162, AY0 + 28, 0x60000000);
            if (hold > .02f) {
                Draw.pill(g, AX0 + 62, AY0 + 22, AX0 + 62 + 100 * Math.min(1, hold / HEAT_HOLD), AY0 + 28, 0xFF90E070);
            }
        } else if (phase == Phase.CONDENSE) {
            Draw.text(g, Component.translatable("metier.arcanes_oublies.distill.pure", pure), AX0 + 62, AY0 + 22, 0xFFD8C8FF, .85f);
        }
        if (over > 0) {
            Draw.title(g, Component.translatable("metier.arcanes_oublies.distill.too_hot"), (AX0 + AX1) / 2f, AY0 + 40,
                    Draw.argb((int) (170 + 85 * Mth.sin(time * 20)), 0xFFFF5040), 1.2f);
        }
        if (burst) {
            float t = time - burstAt;
            Draw.glow(g, FX, FY0 + 70, 60 + 200 * t, Draw.argb((int) (255 * Math.max(0, 1 - t * 2)), 0xFFFFC080));
        }
    }
}
