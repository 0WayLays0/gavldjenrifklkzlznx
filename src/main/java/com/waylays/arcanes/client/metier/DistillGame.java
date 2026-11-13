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
 * Distilling, "l'alambic": three phases. First bring the still up to heat and hold it in the band (the flame is set
 * with the wheel, W/S or by dragging the valve; the heat follows it slowly). Then add each ingredient when the mixture,
 * whose colour drifts, matches the ingredient's swatch (click). Last, keep the heat in the band while the drops
 * condense: a drop that falls in the band is pure, one that falls outside is cloudy. Too hot too long and the still
 * bursts.
 */
public class DistillGame extends MinigameScreen {
    private static final int DROPS = 12;
    private static final float HEAT_HOLD = 2.5f;
    private static final float THX = 302, THY0 = 50, THY1 = 186;
    private static final float VX0 = 40, VX1 = 120, VY = 182;

    private enum Phase {HEAT, INGREDIENTS, CONDENSE}

    private record Bubble(float x, float born, float speed) {}

    private record Drop(float born, boolean pure) {}

    private final float lo, hi;
    private final float[] targets;
    private final List<ItemStack> added = new ArrayList<>();
    private Phase phase = Phase.HEAT;
    private float flame = .3f, temp = .15f, hold, holdTotal, over, hue, hueSpeed, ingredientsQ, lastAdd = -9;
    private int ingredient, pure, fallen;
    private float nextDrop;
    private boolean dragging;
    private final List<Bubble> bubbles = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();

    public DistillGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                       Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF6A4A9A);
        float half = (float) (.11 / Math.sqrt(difficulty));
        this.lo = .62f - half;
        this.hi = .62f + half;
        int n = Math.max(1, Math.min(4, inputs.isEmpty() ? 2 : inputs.size()));
        targets = new float[n];
        for (int i = 0; i < n; i++) {
            targets[i] = random.nextFloat();
        }
        hue = random.nextFloat();
        hueSpeed = (float) (.16 * Math.pow(difficulty, .7));
    }

    @Override
    protected List<Component> rules() {
        return List.of(Component.translatable("metier.arcanes_oublies.distill.rule1"), Component.translatable("metier.arcanes_oublies.distill.rule2"),
                Component.translatable("metier.arcanes_oublies.distill.rule3"));
    }

    @Override
    protected float liveScore() {
        float heatPart = Math.min(1, holdTotal / HEAT_HOLD);
        float ingPart = targets.length == 0 ? 1 : ingredientsQ / targets.length;
        float dropPart = pure / (float) DROPS;
        return Mth.clamp(.15f * heatPart + .35f * ingPart + .5f * dropPart, 0, 1);
    }

    private boolean inBand() {
        return temp >= lo && temp <= hi;
    }

    @Override
    protected void update(float dt) {
        // The heat follows the flame, slowly; the still loses heat all the time
        float target = flame * 1.05f;
        temp += (target - temp) * dt * .55f * (float) Math.pow(difficulty, .25);
        temp = Mth.clamp(temp, 0, 1.1f);
        if (temp > .95f) {
            over += dt;
            shake(over * 2);
            if (over > 1.2f) {
                sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, .5f);
                finish(liveScore() * .4f, true, Component.translatable("metier.arcanes_oublies.distill.burst"));
                return;
            }
        } else {
            over = Math.max(0, over - dt);
        }
        if (temp > .45f && random.nextFloat() < dt * (4 + temp * 14)) {
            bubbles.add(new Bubble(-18 + random.nextFloat() * 36, time, 20 + random.nextFloat() * 25 * temp));
        }
        bubbles.removeIf(b -> time - b.born() > 1.3f);
        hue = (hue + hueSpeed * dt * (1 + .4f * Mth.sin(time * 1.7f))) % 1;
        switch (phase) {
            case HEAT -> {
                if (inBand()) {
                    hold += dt;
                    holdTotal += dt;
                    if (hold >= HEAT_HOLD) {
                        phase = Phase.INGREDIENTS;
                        flash(Component.translatable("metier.arcanes_oublies.distill.add_now"), 0xFFD8B8FF);
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
                    drops.add(new Drop(time, p));
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
                drops.removeIf(d -> time - d.born() > 1.2f);
            }
        }
    }

    /** How far apart two hues are (0 to .5). */
    private static float hueGap(float a, float b) {
        float d = Math.abs(a - b) % 1;
        return Math.min(d, 1 - d);
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (my > VY - 12 && my < VY + 12 && mx > VX0 - 10 && mx < VX1 + 10) {
            dragging = true;
            flame = Mth.clamp((float) ((mx - VX0) / (VX1 - VX0)), 0, 1);
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
        if (q <= 0) {
            shake(4);
        }
        if (ingredient < inputs.size()) {
            added.add(inputs.get(ingredient));
        }
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && playing()) {
            flame = Mth.clamp((float) ((mouseX - left - VX0) / (VX1 - VX0)), 0, 1);
        }
        return true;
    }

    @Override
    protected void onScroll(double mx, double my, double dy) {
        flame = Mth.clamp(flame + (float) dy * .05f, 0, 1);
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) {
            flame = Mth.clamp(flame + .08f, 0, 1);
        } else if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) {
            flame = Mth.clamp(flame - .08f, 0, 1);
        }
    }

    @Override
    protected void drawGame(GuiGraphics g, int mx, int my) {
        g.fillGradient(AX0, AY0, AX1, AY1, 0xFF201830, 0xFF0E0A16);
        // Shelves in the back
        for (int i = 0; i < 3; i++) {
            g.fill(AX0 + 190, AY0 + 30 + i * 40, AX0 + 270, AY0 + 33 + i * 40, 0xFF3A2A20);
        }
        // The burner
        float fx = 80, fy = 160;
        int flames = (int) (3 + flame * 6);
        for (int i = 0; i < flames; i++) {
            float h = 4 + flame * 14 * (.7f + .3f * Mth.sin(time * 12 + i * 2));
            float x = fx - flames * 2.5f + i * 5;
            g.fillGradient((int) x, (int) (fy - h), (int) x + 3, (int) fy, 0xC0FFE070, 0xFF3060FF);
        }
        g.fill((int) fx - 20, (int) fy, (int) fx + 20, (int) fy + 6, 0xFF4A4A52);
        // The flask, the mixture's colour drifting
        int mix = phase == Phase.HEAT ? Draw.lerp(0xFF6A7A9A, Draw.hsv(hue, .7f, .9f), Math.min(1, temp)) : Draw.hsv(hue, .75f, .95f);
        Draw.disc(g, fx, 122, 26, 0xFFB8C8D8);
        Draw.disc(g, fx, 122, 24, 0xFF1A1A28);
        float level = 6 + Math.min(1, added.size() / (float) Math.max(1, targets.length)) * 8;
        for (int dy = (int) (122 - level); dy <= 146; dy++) {
            float w = (float) Math.sqrt(Math.max(0, 24 * 24 - (dy - 122) * (dy - 122)));
            g.fill((int) (fx - w), dy, (int) (fx + w), dy + 1, Draw.argb(220, mix));
        }
        for (Bubble b : bubbles) {
            float t = time - b.born();
            float y = 140 - t * b.speed();
            if (y > 122 - level) {
                Draw.disc(g, fx + b.x() * .6f, y, 1.5f, 0xA0FFFFFF);
            }
        }
        g.fill((int) fx - 5, 70, (int) fx + 5, 98, 0xFFB8C8D8);
        // The coil to the receiving flask
        Draw.line(g, fx, 72, 180, 72, 3, 0xFFC08040);
        Draw.line(g, 180, 72, 200, 110, 3, 0xFFC08040);
        g.fill(195, 140, 225, 170, 0xFFB8C8D8);
        g.fill(197, 142, 223, 170, 0xFF1A1A28);
        int filled = Math.min(26, pure * 2 + (fallen - pure));
        g.fill(197, 170 - filled, 223, 170, Draw.lerp(0xFF9AA0A8, Draw.hsv(.8f, .5f, 1), pure / (float) Math.max(1, fallen)));
        for (Drop d : drops) {
            float t = time - d.born();
            Draw.disc(g, 202, 112 + t * 40, 2, d.pure() ? 0xFFE0D0FF : 0xFF8A8A90);
        }
        Draw.item(g, subject, 200, 120, 1);
        // The ingredients waiting, with their swatches
        if (phase == Phase.INGREDIENTS || phase == Phase.HEAT) {
            for (int i = 0; i < targets.length; i++) {
                int x = 140 + i * 30, y = 96;
                boolean done = i < ingredient, current = i == ingredient && phase == Phase.INGREDIENTS;
                Draw.frame(g, x - 2, y - 2, x + 22, y + 30, current ? 0xA0302040 : 0x60000000, current ? 0xFFFFE070 : 0xFF6A5A7A);
                if (i < inputs.size()) {
                    Draw.item(g, inputs.get(i), x + 2, y, 1);
                }
                g.fill(x + 2, y + 18, x + 18, y + 26, done ? 0x60FFFFFF : Draw.hsv(targets[i], .75f, .95f));
            }
            if (phase == Phase.INGREDIENTS) {
                Draw.centered(g, Component.translatable("metier.arcanes_oublies.distill.match"), 170, 84, 0xFFE0D0FF, .75f);
                // The mixture's colour next to the target's
                Draw.disc(g, 125, 110, 7, mix);
            }
        }
        // The valve
        g.fill((int) VX0, (int) VY - 1, (int) VX1, (int) VY + 2, 0xFF5A5A62);
        float vx = VX0 + (VX1 - VX0) * flame;
        Draw.disc(g, vx, VY, 5, dragging ? 0xFFFFE070 : 0xFFD0A040);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.distill.flame"), VX0, VY - 14, 0xFFD8C8E8, .7f);
        // The thermometer
        g.fill((int) THX - 1, (int) THY0 - 1, (int) THX + 13, (int) THY1 + 1, 0xFF100A18);
        g.fillGradient((int) THX, (int) THY0, (int) THX + 12, (int) THY1, 0xFFC03030, 0xFF3050A0);
        int yl = (int) (THY1 - lo / 1.1f * (THY1 - THY0)), yh = (int) (THY1 - hi / 1.1f * (THY1 - THY0));
        g.renderOutline((int) THX - 3, yh, 18, yl - yh, 0xFF70FF70);
        int yr = (int) (THY1 - .95f / 1.1f * (THY1 - THY0));
        g.fill((int) THX - 3, yr, (int) THX + 15, yr + 1, 0xFFFF3030);
        int yt = (int) (THY1 - temp / 1.1f * (THY1 - THY0));
        g.fill((int) THX - 5, yt - 1, (int) THX + 17, yt + 2, 0xFFFFFFFF);
        // The phase and how it goes
        Component phaseText = switch (phase) {
            case HEAT -> Component.translatable("metier.arcanes_oublies.distill.phase_heat");
            case INGREDIENTS -> Component.translatable("metier.arcanes_oublies.distill.phase_ingredients", ingredient + 1, targets.length);
            case CONDENSE -> Component.translatable("metier.arcanes_oublies.distill.phase_condense", fallen, DROPS);
        };
        Draw.text(g, phaseText, AX0 + 8, AY0 + 6, 0xFFE8D8FF, .85f);
        if (phase == Phase.HEAT) {
            g.fill(AX0 + 8, AY0 + 18, AX0 + 108, AY0 + 22, 0x80000000);
            g.fill(AX0 + 8, AY0 + 18, AX0 + 8 + (int) (100 * Math.min(1, hold / HEAT_HOLD)), AY0 + 22, 0xFF90E070);
        } else if (phase == Phase.CONDENSE) {
            Draw.text(g, Component.translatable("metier.arcanes_oublies.distill.pure", pure), AX0 + 8, AY0 + 18, 0xFFD8C8FF, .75f);
        }
        if (over > 0) {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.distill.too_hot"), 165, AY0 + 30, Draw.argb((int) (155 + 100 * Mth.sin(time * 20)), 0xFF5040), 1);
        }
    }
}
