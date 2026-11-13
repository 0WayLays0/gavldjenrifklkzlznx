package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Forging, "battre le fer": the metal cools all the time; work the bellows (hold the right button or space) to keep it
 * at forging heat (the gauge's green band). Strike (left click) as the mark crosses the anvil's beat: on the beat and
 * hot, the piece takes shape; struck cold it cracks (three cracks and it is lost); left too hot too long, it burns.
 * Once shaped, quench it: it cools fast and must go into the water at cherry red.
 */
public class ForgeGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_forge"), ANVIL = Draw.tex("anvil"), BELLOWS = Draw.tex("bellows");
    private static final float AVX = 236, AVY = 178, AVW = 156, AVH = 88;
    private static final float FACE = AVY - AVH / 2 + 16;
    private static final float HX0 = 40, HX1 = 116, HY = 202;
    private static final float TX = 380, TY0 = 66, TY1 = 200, MAX = 1.15f;

    private final float lo, hi, period, zone;
    private float heat = .6f, progress, strikesQ, burn, lastStrike = -1, quenchStart = -1, quench = -1, quenchAt = -1;
    private int strikes, cracks;
    private boolean bellows;
    private float bellowsAnim;
    private final Draw.Fire fire;
    private final Draw.Bits bits = new Draw.Bits();

    public ForgeGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                     Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFFB0502A);
        float half = (float) (.15 / Math.sqrt(difficulty));
        this.lo = .7f - half;
        this.hi = .7f + half;
        this.period = (float) (1.15 / difficulty);
        this.zone = (float) (.17 / Math.sqrt(difficulty));
        this.fire = new Draw.Fire(random, false);
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return quenchStart >= 0 ? List.of(control("click", "forge.ctl_quench"))
                : List.of(control("right_hold", "forge.ctl_bellows"), control("click", "forge.ctl_strike"));
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

    private float mark() {
        float t = (time % period) / period;
        return t < .5f ? t * 2 : 2 - t * 2;
    }

    @Override
    protected void update(float dt) {
        bellowsAnim = Mth.lerp(Math.min(1, dt * 10), bellowsAnim, bellows ? 1 : 0);
        fire.update(time, dt, HX0, HX1, HY, Mth.clamp(.25f + .75f * bellowsAnim, 0, 1));
        if (quenchStart >= 0) {
            if (quench < 0) {
                heat = Math.max(0, 1 - (time - quenchStart) / 2.8f);
                if (heat <= 0) {
                    quench = 0;
                    flash(Component.translatable("metier.arcanes_oublies.forge.too_cold"), 0xFFFF7060);
                    finish(liveScore(), false, null);
                }
            }
            return;
        }
        heat += (bellows ? .3f : 0) * dt - .075f * (float) (1 + .2 * (difficulty - 1)) * dt;
        heat = Mth.clamp(heat, 0, MAX);
        if (heat > 1) {
            burn += dt;
            if (random.nextFloat() < dt * 20) {
                bits.add(time, AVX + (random.nextFloat() - .5f) * 50, FACE - 4, (random.nextFloat() - .5f) * 10, -20, 1, 10, 0xFF302820, -8, false);
            }
            if (burn > 1.6f) {
                finish(liveScore() * .5f, true, Component.translatable("metier.arcanes_oublies.forge.burnt"));
            }
        }
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
            if (quench >= 0) {
                return;
            }
            float err = Math.abs(heat - .5f);
            quench = Mth.clamp(1 - err * 4, 0, 1);
            quenchAt = time;
            sound(SoundEvents.FIRE_EXTINGUISH, 1.1f, .8f);
            sound(SoundEvents.GENERIC_SPLASH, 1.2f, .5f);
            for (int i = 0; i < 18; i++) {
                bits.add(time, 318 + (random.nextFloat() - .5f) * 40, 190, (random.nextFloat() - .5f) * 30, -30 - random.nextFloat() * 40, 1.4f,
                        10 + random.nextFloat() * 10, 0xFFE8F0F8, -20, false);
            }
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
            shake(7);
            flash(Component.translatable("metier.arcanes_oublies.forge.cold_crack"), 0xFFFF6050);
            sound(SoundEvents.ANVIL_LAND, .6f, .6f);
            bits.chips(random, time, AVX, FACE, 4, 0xFF4A4A50);
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
            shake(2 + q * 4);
            sound(SoundEvents.ANVIL_USE, .9f + q * .4f, .55f);
            bits.sparks(random, time, AVX + (random.nextFloat() - .5f) * 20, FACE - 6, (int) (10 + q * 22), 150 + q * 60, 0xFFFFB040);
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

    private float gaugeY(float v) {
        return TY1 - v / MAX * (TY1 - TY0);
    }

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        float power = Mth.clamp(.25f + .75f * bellowsAnim, 0, 1);
        // The hearth's fire and the light it throws
        fire.draw(g, time, HX0, HX1, HY, power);
        Draw.glow(g, (HX0 + HX1) / 2, HY - 30, 120 + 40 * power, Draw.argb((int) (30 + 40 * power), 0xFFFF7020));
        // The bellows, squeezing as they blow, their nozzle in the fire
        float squeeze = 1 - .35f * bellowsAnim;
        g.pose().pushPose();
        g.pose().translate(164, 192, 0);
        g.pose().scale(-1, squeeze, 1);
        Draw.picture(g, BELLOWS, -40, -27.5f, 40, 27.5f, 160, 110, 0xFFFFFFFF);
        g.pose().popPose();
        if (bellowsAnim > .3f && random.nextFloat() < .5f) {
            Draw.glow(g, 120, 192, 10, Draw.argb(60, 0xFFFFC080));
        }
        // The water trough, once the piece is to be quenched
        if (quenchStart >= 0) {
            float in = Draw.ease((time - quenchStart) * 3);
            float ty = 206 - 20 * in;
            Draw.shadow(g, 318, 208, 80, 14, 120);
            Draw.rect(g, 288, ty, 348, 208, 0xFF4A3420);
            Draw.vgrad(g, 291, ty + 3, 345, ty + 9, 0xFF6AA8D8, 0xFF2A5A88);
            Draw.rect(g, 288, ty, 348, ty + 2, 0xFF6A4A2E);
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.forge.water"), 318, ty - 11, 0xFFD8F0FF, .8f);
        }
        // The anvil, the piece on it, glowing with its heat
        Draw.shadow(g, AVX, AVY + AVH / 2 - 2, AVW * 1.1f, 20, 170);
        Draw.picture(g, ANVIL, AVX - AVW / 2, AVY - AVH / 2, AVX + AVW / 2, AVY + AVH / 2, 300, 170, 0xFFFFFFFF);
        float shaped = Math.min(1, progress);
        float hv = Mth.clamp(heat, 0, 1);
        int col = Draw.heat(hv * .95f);
        float len = 52 + shaped * 34, thick = 9 - shaped * 4.5f;
        float px0 = AVX - len / 2, px1 = AVX + len / 2, py = FACE - thick;
        boolean quenched = quench >= 0;
        if (!quenched) {
            Draw.glow(g, AVX, FACE - thick / 2, len * .7f, Draw.argb((int) (220 * hv * hv), Draw.lerp(col, 0xFFFFFFFF, .2f)));
        }
        Draw.rect(g, px0, py, px1, FACE, quenched ? 0xFF6A6E7A : col);
        Draw.line(g, px1, FACE - thick / 2, px1 + 6 + shaped * 10, FACE - .5f, thick * .9f, quenched ? 0xFF6A6E7A : col);
        Draw.rect(g, px0, py, px1, py + 1, Draw.argb(150, 0xFFFFFFFF));
        // The beat: a mark running to and fro, the anvil's beat in the middle
        if (quenchStart < 0) {
            float bx0 = AVX - 80, bx1 = AVX + 80, by = AY0 + 30;
            Draw.pill(g, bx0 - 2, by - 2, bx1 + 2, by + 10, 0xE0100A08);
            Draw.pill(g, bx0, by, bx1, by + 8, 0xFF2E2420);
            float zc = (bx0 + bx1) / 2, zw = (bx1 - bx0) * zone;
            Draw.rect(g, zc - zw, by, zc + zw, by + 8, 0x7090D070);
            Draw.rect(g, zc - zw / 2, by, zc + zw / 2, by + 8, 0xD0FFD070);
            float m = mark();
            float nx = bx0 + (bx1 - bx0) * m;
            boolean onBeat = Math.abs(m - .5f) < zone * .5f;
            Draw.glow(g, nx, by + 4, onBeat ? 16 : 9, onBeat ? 0xC0FFE070 : 0x80FFFFFF);
            Draw.rect(g, nx - 1.5f, by - 5, nx + 1.5f, by + 13, 0xFFFFFFFF);
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.forge.beat"), zc, by - 12, 0xFFF0DCC0, .8f);
        }
        bits.draw(g, time);
        // The heat gauge: forging band, burning line; the quench mark once it is to be quenched
        Draw.pill(g, TX - 4, TY0 - 4, TX + 18, TY1 + 4, 0xE0100A08);
        for (float y = TY0 + 7; y < TY1 - 7; y += 1) {
            float v = (TY1 - y) / (TY1 - TY0) * MAX;
            Draw.rect(g, TX, y, TX + 14, y + 1.05f, Draw.heat(v * .9f));
        }
        Draw.disc(g, TX + 7, TY0 + 7, 7, Draw.heat(1));
        Draw.disc(g, TX + 7, TY1 - 7, 7, Draw.heat(.02f));
        float yl = gaugeY(lo), yh = gaugeY(hi), yb = gaugeY(1);
        Draw.rect(g, TX - 3, yh, TX + 17, yh + 1, 0xFF90FF90);
        Draw.rect(g, TX - 3, yl - 1, TX + 17, yl, 0xFF90FF90);
        Draw.rect(g, TX - 3, yh, TX - 2, yl, 0xFF90FF90);
        Draw.rect(g, TX + 16, yh, TX + 17, yl, 0xFF90FF90);
        Draw.rect(g, TX - 5, yb - .5f, TX + 19, yb + 1, 0xFFFF3A2A);
        if (quenchStart >= 0) {
            float yq = gaugeY(.5f);
            Draw.glow(g, TX + 7, yq, 16, 0x9080C0FF);
            Draw.pill(g, TX - 7, yq - 2, TX + 21, yq + 2, 0xFFB8E0FF);
        }
        float yt = gaugeY(Mth.clamp(heat, 0, MAX));
        Draw.glow(g, TX + 7, yt, 12, Draw.argb(170, Draw.heat(hv)));
        Draw.pill(g, TX - 6, yt - 2, TX + 20, yt + 2, 0xFFFFFFFF);
        // Shape and cracks
        Draw.text(g, Component.translatable("metier.arcanes_oublies.forge.shape"), AX0 + 12, AY0 + 9, 0xFFF0DCC0, .85f);
        Draw.pill(g, AX0 + 12, AY0 + 21, AX0 + 112, AY0 + 28, 0x60000000);
        if (shaped > .01f) {
            Draw.pill(g, AX0 + 12, AY0 + 21, AX0 + 12 + 100 * shaped, AY0 + 28, 0xFFFFA040);
        }
        for (int i = 0; i < 3; i++) {
            float x = TX - 24 - i * 14, y = AY0 + 16;
            Draw.disc(g, x, y, 5, i < cracks ? 0xFFC0402A : 0x40FFFFFF);
            if (i < cracks) {
                Draw.line(g, x - 1, y - 4, x + 1, y, 1.2f, 0xFF200A06);
                Draw.line(g, x + 1, y, x - 1, y + 4, 1.2f, 0xFF200A06);
            }
        }
        // The hammer
        if (inArea(mx, my) && quenchStart < 0) {
            float swing = lastStrike < 0 ? 1 : Mth.clamp((time - lastStrike) / .16f, 0, 1);
            Draw.itemRotated(g, tool.isEmpty() ? new ItemStack(Items.MACE) : tool, mx + 10, my - 10, 2.1f, -40 + 70 * Mth.sin(swing * Mth.PI));
        } else if (quenchStart >= 0 && quenchAt < 0) {
            Draw.itemRotated(g, new ItemStack(Items.IRON_INGOT), mx, my, 1.4f, 20);
        }
    }
}
