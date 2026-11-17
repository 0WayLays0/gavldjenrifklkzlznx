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
import java.util.List;

/**
 * Sharpening at the grindstone: the wheel turns; hold the left button (or space) to press the blade on it. The
 * pressure must stay in the green band, which wanders: in it, the edge comes back and sparks fly; too light, little
 * happens; too hard, the blade heats, and if it turns blue its temper is lost (a failure). The edge whole, it is done.
 */
public class GrindGame extends MinigameScreen {
    private static final ResourceLocation SCENE = Draw.tex("bg_forge");
    /** The wheel's centre and radius, the gauge's place. */
    private static final float WX = 168, WY = 150, WR = 58, GX = 372, GY0 = 66, GY1 = 206;
    private static final float LIMIT = 22;

    private final float half, speed, phase;
    private float pressure, heat, progress, inBand, played, maxHeat, wheel;
    private boolean pressing;
    private float lastSpark;
    private final Draw.Bits bits = new Draw.Bits();

    public GrindGame(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                     Component heading, Component sub, float perfectAt, float criticalBelow) {
        super(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow, 0xFF7A8A9A);
        this.half = (float) (.12 / Math.sqrt(Math.max(.3, difficulty)));
        this.speed = (float) (.9 + .35 * difficulty);
        this.phase = random.nextFloat() * Mth.TWO_PI;
    }

    @Override
    protected ResourceLocation scene() {
        return SCENE;
    }

    @Override
    protected List<Control> controls() {
        return List.of(control("click_hold", "grind.ctl_press"));
    }

    /** Where the band is now (its middle, 0 to 1). */
    private float band() {
        return .5f + .22f * Mth.sin(time * speed + phase) + .08f * Mth.sin(time * speed * 2.7f + phase * 1.7f);
    }

    @Override
    protected float liveScore() {
        float steady = played <= 0 ? 0 : inBand / played;
        return Mth.clamp(steady * (.4f + .6f * Math.min(1, progress)) - maxHeat * .15f, 0, 1);
    }

    @Override
    protected void update(float dt) {
        wheel += dt * (6 + pressure * 3);
        pressure = Mth.clamp(pressure + (pressing ? 1.25f : -1.05f) * dt, 0, 1);
        if (!playing()) {
            return;
        }
        played += dt;
        float c = band();
        boolean in = pressure >= c - half && pressure <= c + half;
        if (in) {
            inBand += dt;
            progress += dt / 7f;
        } else if (pressure > .05f && pressure < c - half) {
            progress += dt / 7f * .2f;
        }
        if (pressure > c + half) {
            heat += (pressure - c - half) * 2.6f * dt;
        } else {
            heat = Math.max(0, heat - .22f * dt);
        }
        maxHeat = Math.max(maxHeat, heat);
        // Sparks where the blade bites
        if (pressure > .08f && time - lastSpark > (in ? .04f : .12f)) {
            lastSpark = time;
            float cx = WX + WR * .72f, cy = WY - WR * .7f;
            int color = heat > .6f ? 0xFF7AA8FF : in ? 0xFFFFC050 : 0xFFFF9030;
            bits.sparks(random, time, cx, cy, in ? 4 : 1, 120 + pressure * 120, color);
            if (in && random.nextFloat() < .15f) {
                sound(SoundEvents.GRINDSTONE_USE, .9f + random.nextFloat() * .3f, .25f);
            }
        }
        if (heat >= 1) {
            finish(liveScore() * .5f, true, Component.translatable("metier.arcanes_oublies.grind.blued"));
        } else if (progress >= 1) {
            flash(Component.translatable("metier.arcanes_oublies.grind.sharp"), 0xFFE8F4FF);
            finish(liveScore(), false, null);
        } else if (played >= LIMIT) {
            finish(liveScore() * .8f, false, Component.translatable("metier.arcanes_oublies.grind.too_long"));
        }
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (button == 0) {
            pressing = true;
        }
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        if (button == 0) {
            pressing = false;
        }
    }

    @Override
    protected void onKeyDown(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            pressing = true;
        }
    }

    @Override
    protected void onKeyUp(int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            pressing = false;
        }
    }

    @Override
    protected void drawGame(GuiGraphics g, float mx, float my) {
        // The frame and the trough of water under the wheel
        Draw.shadow(g, WX, WY + WR + 18, WR * 2.6f, 22, 170);
        Draw.rect(g, WX - WR - 22, WY + WR + 4, WX + WR + 22, WY + WR + 20, 0xFF4A3420);
        Draw.vgrad(g, WX - WR - 18, WY + WR + 6, WX + WR + 18, WY + WR + 12, 0xFF6AA8D8, 0xFF2A5A88);
        Draw.rect(g, WX - 6, WY - 4, WX - 2, WY + WR + 6, 0xFF5A4028);
        Draw.rect(g, WX + 2, WY - 4, WX + 6, WY + WR + 6, 0xFF5A4028);
        // The stone wheel, turning
        Draw.disc(g, WX, WY, WR + 3, 0xFF3A3632);
        Draw.disc(g, WX, WY, WR, 0xFF8E887C);
        Draw.disc(g, WX, WY, WR * .78f, 0xFF9A9488);
        for (int i = 0; i < 10; i++) {
            float a = wheel + i * Mth.TWO_PI / 10;
            Draw.line(g, WX + Mth.cos(a) * WR * .25f, WY + Mth.sin(a) * WR * .25f, WX + Mth.cos(a) * WR * .95f, WY + Mth.sin(a) * WR * .95f, 1.2f,
                    Draw.argb(70, 0xFF3A3632));
        }
        Draw.disc(g, WX, WY, 7, 0xFF5A4028);
        Draw.disc(g, WX, WY, 3, 0xFF2A2018);
        // The blade on the wheel: closer the harder it is pressed
        float push = 22 - pressure * 16;
        float bx = WX + WR * .72f + push * .7f, by = WY - WR * .7f - push * .7f;
        int glow = heat > .05f ? Draw.lerp(0xFFFFA040, 0xFF6A90FF, Mth.clamp(heat, 0, 1)) : 0;
        if (glow != 0) {
            Draw.glow(g, bx, by, 18 + heat * 14, Draw.argb((int) (60 + 120 * Math.min(1, heat)), glow));
        }
        Draw.itemRotated(g, subject, bx + 16, by - 16, 3.2f, -45);
        bits.draw(g, time);

        // The pressure gauge: the band to keep, the needle
        Draw.pill(g, GX - 6, GY0 - 6, GX + 20, GY1 + 6, 0xE0100A08);
        Draw.vgrad(g, GX, GY0, GX + 14, GY1, 0xFF5A2A20, 0xFF20201E);
        float c = band();
        float yHi = Mth.lerp(c + half, GY1, GY0), yLo = Mth.lerp(c - half, GY1, GY0);
        Draw.rect(g, GX, yHi, GX + 14, yLo, 0xB050D070);
        Draw.rect(g, GX - 3, yHi, GX + 17, yHi + 1, 0xFF90FF90);
        Draw.rect(g, GX - 3, yLo - 1, GX + 17, yLo, 0xFF90FF90);
        float yp = Mth.lerp(pressure, GY1, GY0);
        boolean in = pressure >= c - half && pressure <= c + half;
        Draw.glow(g, GX + 7, yp, in ? 12 : 7, in ? 0xC0FFE070 : 0x80FFFFFF);
        Draw.rect(g, GX - 5, yp - 1, GX + 19, yp + 1, 0xFFFFFFFF);
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.grind.pressure"), GX + 7, GY0 - 16, 0xFFF0DCC0, .75f);

        // The edge coming back, and the heat of the blade
        float px0 = 236, px1 = 340, py = 70;
        Draw.text(g, Component.translatable("metier.arcanes_oublies.grind.edge"), px0, py - 11, 0xFFF0DCC0, .8f);
        Draw.pill(g, px0, py, px1, py + 8, 0xFF2E2420);
        Draw.pill(g, px0, py, px0 + (px1 - px0) * Math.min(1, progress), py + 8, 0xFFDCE8F4);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.grind.heat"), px0, py + 16, 0xFFF0DCC0, .8f);
        Draw.pill(g, px0, py + 27, px1, py + 35, 0xFF2E2420);
        Draw.pill(g, px0, py + 27, px0 + (px1 - px0) * Mth.clamp(heat, 0, 1), py + 35, Draw.lerp(0xFFFFA040, 0xFF6A90FF, Mth.clamp(heat, 0, 1)));
    }
}
