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
 * Forging, "battre le fer": the metal cools all the time; work the bellows (hold the right button or space) to keep it
 * at forging heat (the gauge's band). Strike (left click) when the hammer's mark crosses the anvil's beat: on the beat
 * and hot, the piece takes shape; struck cold, it cracks (three cracks and it is lost); left too hot too long, it burns.
 * Once shaped, quench it: it cools fast, and must go in the water (click) at cherry red.
 */
public class ForgeGame extends MinigameScreen {
    private static final float THERMO_X = 300, TY0 = 48, TY1 = 184;

    private record Spark(float x, float y, float vx, float vy, float born) {}

    private final float lo, hi, period, zone;
    private float heat = .6f, progress, strikesQ, burn, lastStrike = -1, quenchStart = -1, quench = -1;
    private int strikes, cracks;
    private boolean bellows;
    private float bellowsAnim;
    private final List<Spark> sparks = new ArrayList<>();

    public ForgeGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                     Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFFA8442A);
        float half = (float) (.15 / Math.sqrt(difficulty));
        this.lo = .7f - half;
        this.hi = .7f + half;
        this.period = (float) (1.15 / difficulty);
        this.zone = (float) (.17 / Math.sqrt(difficulty));
    }

    @Override
    protected List<Component> rules() {
        return List.of(Component.translatable("metier.arcanes_oublies.forge.rule1"), Component.translatable("metier.arcanes_oublies.forge.rule2"),
                Component.translatable("metier.arcanes_oublies.forge.rule3"));
    }

    private float avgStrike() {
        return strikes == 0 ? 0 : strikesQ / strikes;
    }

    @Override
    protected float liveScore() {
        float shaped = Math.min(1, progress);
        float q = .7f * avgStrike() * shaped + .3f * (quench >= 0 ? quench : shaped * .6f);
        return Mth.clamp(q - Math.min(.2f, burn * .05f), 0, 1);
    }

    /** Where the hammer's mark is on its bar (0 to 1, back and forth). */
    private float mark() {
        float t = (time % period) / period;
        return t < .5f ? t * 2 : 2 - t * 2;
    }

    @Override
    protected void update(float dt) {
        bellowsAnim = Mth.lerp(dt * 10, bellowsAnim, bellows ? 1 : 0);
        if (quenchStart >= 0) {
            // Quenching: it cools by itself, fast
            heat = Math.max(0, 1 - (time - quenchStart) / 2.8f);
            if (heat <= 0) {
                quench = 0;
                flash(Component.translatable("metier.arcanes_oublies.forge.too_cold"), 0xFFFF7060);
                finish(liveScore(), false, null);
            }
            return;
        }
        heat += (bellows ? .3f : 0) * dt - .075f * (float) (1 + .2 * (difficulty - 1)) * dt;
        heat = Mth.clamp(heat, 0, 1.15f);
        if (heat > 1) {
            burn += dt;
            if (burn > 1.6f) {
                finish(liveScore() * .5f, true, Component.translatable("metier.arcanes_oublies.forge.burnt"));
            }
        }
        sparks.removeIf(s -> time - s.born() > .8f);
        if (cracks >= 3) {
            finish(liveScore() * .5f, true, Component.translatable("metier.arcanes_oublies.forge.cracked"));
        } else if (progress >= 1 && quenchStart < 0) {
            quenchStart = time;
            heat = 1;
            flash(Component.translatable("metier.arcanes_oublies.forge.quench_now"), 0xFFB8E0FF);
        }
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (button == 1) {
            bellows = true;
            return;
        }
        if (quenchStart >= 0) {
            // Into the water
            float target = .5f, err = Math.abs(heat - target);
            quench = Mth.clamp(1 - err * 4, 0, 1);
            sound(SoundEvents.FIRE_EXTINGUISH, 1.1f, .8f);
            flash(Component.translatable(quench > .75f ? "metier.arcanes_oublies.forge.quench_good" : "metier.arcanes_oublies.forge.quench_bad"),
                    quench > .75f ? 0xFFFFE070 : 0xFFFFA060);
            finish(liveScore(), false, null);
            return;
        }
        if (time - lastStrike < .22f) {
            return;
        }
        lastStrike = time;
        float d = Math.abs(mark() - .5f);
        float q;
        if (heat < lo - .05f) {
            cracks++;
            q = 0;
            shake(6);
            flash(Component.translatable("metier.arcanes_oublies.forge.cold_crack"), 0xFFFF6050);
            sound(SoundEvents.ANVIL_LAND, .6f, .6f);
        } else {
            q = d < zone * .5f ? 1 : d < zone ? .7f : .3f;
            if (heat > hi + .1f) {
                q *= .5f;
                flash(Component.translatable("metier.arcanes_oublies.forge.too_hot"), 0xFFFFA060);
            } else {
                flash(Component.translatable(q >= 1 ? "metier.arcanes_oublies.forge.true_blow" : q >= .7f ? "metier.arcanes_oublies.forge.good_blow"
                        : "metier.arcanes_oublies.forge.off_beat"), q >= 1 ? 0xFFFFE070 : q >= .7f ? 0xFFB8E0FF : 0xFFFFA060);
            }
            progress += q * .13f;
            shake(q * 4);
            sound(SoundEvents.ANVIL_USE, .9f + q * .4f, .55f);
            for (int i = 0; i < (int) (6 + q * 12); i++) {
                sparks.add(new Spark(165, 112, (random.nextFloat() - .5f) * 160, -30 - random.nextFloat() * 110, time));
            }
        }
        strikes++;
        strikesQ += q;
        heat -= .03f;
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        if (button == 1) {
            bellows = false;
        }
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            bellows = true;
        }
    }

    @Override
    protected void onKeyUp(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            bellows = false;
        }
    }

    @Override
    protected void drawGame(GuiGraphics g, int mx, int my) {
        // The smithy: dark, lit by the forge
        g.fillGradient(AX0, AY0, AX1, AY1, 0xFF2A1A14, 0xFF140C0A);
        int glow = Draw.argb((int) (60 + 80 * Mth.clamp(heat, 0, 1)), 0xFFFF6A20);
        g.fillGradient(AX0, AY1 - 50, AX1, AY1, 0x00000000, glow);
        // The bellows, squeezing as they blow
        float squeeze = bellowsAnim;
        int bx = AX0 + 20, by = 120;
        g.fill(bx, by - (int) (18 - squeeze * 10), bx + 46, by + (int) (18 - squeeze * 10), 0xFF6A4228);
        g.renderOutline(bx, by - (int) (18 - squeeze * 10), 46, (int) (36 - squeeze * 20), 0xFF2A1A10);
        g.fill(bx + 46, by - 3, bx + 66, by + 3, 0xFF4A4A50);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.forge.bellows"), bx, by + 24, 0xFFE0C8A8, .7f);
        if (bellows && random.nextInt(2) == 0) {
            sparks.add(new Spark(bx + 70, by, 40 + random.nextFloat() * 30, (random.nextFloat() - .5f) * 30, time));
        }
        // The anvil
        g.fill(115, 124, 215, 134, 0xFF3A3A42);
        g.fill(130, 134, 200, 150, 0xFF2E2E36);
        g.fill(118, 150, 212, 158, 0xFF3A3A42);
        g.fill(115, 124, 215, 125, 0xFF6A6A74);
        // The piece: a bar becoming a blade, coloured by its heat
        float shaped = Math.min(1, progress);
        int col = Draw.heat(heat * .95f);
        int len = (int) (50 + shaped * 40), thick = (int) (12 - shaped * 7);
        g.fill(165 - len / 2, 122 - thick, 165 + len / 2, 122, col);
        g.fill(165 + len / 2, 122 - thick + 2, 165 + len / 2 + (int) (shaped * 10), 122, col);
        g.fill(165 - len / 2, 122 - thick, 165 + len / 2, 122 - thick + 1, Draw.lerp(col, 0xFFFFFFFF, .3f));
        if (shaped > .2f) {
            Draw.item(g, subject, 158, 78, 1);
        }
        // Sparks
        for (Spark s : sparks) {
            float t = time - s.born();
            float x = s.x() + s.vx() * t, y = s.y() + s.vy() * t + 220 * t * t;
            g.fill((int) x, (int) y, (int) x + 2, (int) y + 2, Draw.argb((int) (255 * (1 - t / .8f)), 0xFFFFC060));
        }
        // The thermometer: the forging band, the burning line
        g.fill((int) THERMO_X - 1, (int) TY0 - 1, (int) THERMO_X + 13, (int) TY1 + 1, 0xFF1A1010);
        for (int y = (int) TY0; y < TY1; y++) {
            float v = 1.15f * (TY1 - y) / (TY1 - TY0);
            g.fill((int) THERMO_X, y, (int) THERMO_X + 12, y + 1, Draw.heat(v * .9f));
        }
        int yl = (int) (TY1 - lo / 1.15f * (TY1 - TY0)), yh = (int) (TY1 - hi / 1.15f * (TY1 - TY0)), yb = (int) (TY1 - 1 / 1.15f * (TY1 - TY0));
        g.renderOutline((int) THERMO_X - 3, yh, 18, yl - yh, 0xFF70FF70);
        g.fill((int) THERMO_X - 3, yb, (int) THERMO_X + 15, yb + 1, 0xFFFF3030);
        int yh2 = (int) (TY1 - Mth.clamp(heat, 0, 1.15f) / 1.15f * (TY1 - TY0));
        g.fill((int) THERMO_X - 5, yh2 - 1, (int) THERMO_X + 17, yh2 + 2, 0xFFFFFFFF);
        if (quenchStart >= 0) {
            int yq = (int) (TY1 - .5f / 1.15f * (TY1 - TY0));
            g.fill((int) THERMO_X - 7, yq - 2, (int) THERMO_X + 19, yq + 2, 0xA0B8E0FF);
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.forge.quench_hint"), 165, 52, 0xFFB8E0FF, .9f);
            g.fill(120, 168, 210, 186, 0xFF2A4A6A);
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.forge.water"), 165, 174, 0xFFD8F0FF, .7f);
        } else {
            // The beat bar: the hammer's mark and the anvil's beat
            int x0 = 100, x1 = 230, y = 176;
            g.fill(x0, y, x1, y + 8, 0xA0000000);
            int zc = (x0 + x1) / 2, zw = (int) ((x1 - x0) * zone);
            g.fill(zc - zw, y, zc + zw, y + 8, 0x8090D070);
            g.fill(zc - zw / 2, y, zc + zw / 2, y + 8, 0xC0FFE070);
            int m = x0 + (int) ((x1 - x0) * mark());
            g.fill(m - 1, y - 3, m + 2, y + 11, 0xFFFFFFFF);
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.forge.beat"), (x0 + x1) / 2f, y - 10, 0xFFE0C8A8, .7f);
        }
        // Progress and cracks
        g.fill(AX0 + 10, AY0 + 8, AX0 + 110, AY0 + 13, 0x80000000);
        g.fill(AX0 + 10, AY0 + 8, AX0 + 10 + (int) (100 * shaped), AY0 + 13, 0xFFFFA040);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.forge.shape"), AX0 + 10, AY0 + 16, 0xFFE0C8A8, .7f);
        for (int i = 0; i < 3; i++) {
            Draw.text(g, Component.literal("⚡"), AX1 - 70 + i * 11, AY0 + 6, i < cracks ? 0xFFFF5040 : 0x40FFFFFF, 1);
        }
        // The hammer
        if (mx > AX0 && mx < AX1 && my > AY0 && my < AY1 && quenchStart < 0) {
            float swing = lastStrike < 0 ? 1 : Mth.clamp((time - lastStrike) / .16f, 0, 1);
            Draw.itemRotated(g, tool.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.MACE) : tool, mx + 8, my - 8, 1.5f,
                    -45 + 60 * Mth.sin(swing * Mth.PI));
        }
    }
}
