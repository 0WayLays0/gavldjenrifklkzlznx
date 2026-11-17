package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The frame every trade's minigame is played in, scaled to the screen: a header (what is worked, with what, how hard),
 * the painted scene the game is drawn on, a quality gauge with the partial and perfect marks, the keys along the
 * bottom; a countdown before, the result after. A minigame started from the editor is a trial: nothing is sent, nothing
 * given.
 */
public abstract class MinigameScreen extends Screen {
    /** The window's size in its own units (it is scaled to fit the screen). */
    protected static final int W = 420, H = 272;
    /** The play area, in the window. */
    protected static final int AX0 = 12, AY0 = 46, AX1 = 408, AY1 = 226;
    private static final long INTRO = 2000;

    private enum State {INTRO, PLAY, WAITING, RESULT}

    /** A key and what it does, shown along the bottom of the scene. */
    protected record Control(Component key, Component label) {}

    @Nullable
    protected final MetierNet.Start start;
    protected final double difficulty;
    protected final Random random;
    protected final ItemStack subject, tool;
    protected final List<ItemStack> inputs;
    private final Component heading, subheading;
    private final float perfectAt, criticalBelow;
    protected final int theme;

    private State state = State.INTRO;
    private long stateAt = Util.getMillis();
    private long lastFrame = Util.getMillis();
    protected float time;
    private float finalScore, shownScore;
    @Nullable
    private Component reason;
    @Nullable
    private MetierNet.Outcome outcome;
    private int practiceOutcome = -1;
    @Nullable
    private Component stopped;
    private long escapeAt = -10000;
    /** Where the window is on screen, and its scale. */
    protected float left, top, k = 1;
    private float shake;
    @Nullable
    private Component flash;
    private int flashColor;
    private long flashAt;
    private final Draw.Bits confetti = new Draw.Bits();
    private float resultClock;
    @Nullable
    private Screen returnTo;

    protected MinigameScreen(@Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject, ItemStack tool, List<ItemStack> inputs,
                             Component heading, Component subheading, float perfectAt, float criticalBelow, int theme) {
        super(heading);
        this.start = start;
        this.difficulty = difficulty;
        this.random = new Random(seed);
        this.subject = subject;
        this.tool = tool;
        this.inputs = inputs;
        this.heading = heading;
        this.subheading = subheading;
        this.perfectAt = perfectAt;
        this.criticalBelow = criticalBelow;
        this.theme = theme;
    }

    public MinigameScreen returnTo(@Nullable Screen screen) {
        returnTo = screen;
        return this;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(returnTo);
    }

    @Override
    protected void init() {
        k = Mth.clamp(Math.min((width - 16) / (float) W, (height - 12) / (float) H), .5f, 3.2f);
        left = (width - W * k) / 2;
        top = (height - H * k) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- for the games

    /** Advances the game by {@code dt} seconds. */
    protected abstract void update(float dt);

    /** The painted scene behind the game (784 x 360). */
    protected abstract ResourceLocation scene();

    /** Draws the game over its scene (window units; the mouse too). */
    protected abstract void drawGame(GuiGraphics g, float mx, float my);

    /** The keys, shown along the bottom of the scene. */
    protected abstract List<Control> controls();

    /** The score so far (0 to 1), for the gauge. */
    protected abstract float liveScore();

    protected static Control control(String key, String label) {
        return new Control(Component.translatable("metier.arcanes_oublies.key." + key), Component.translatable("metier.arcanes_oublies." + label));
    }

    protected void onMouseDown(double mx, double my, int button) {
    }

    protected void onMouseUp(double mx, double my, int button) {
    }

    protected void onMouseDrag(double mx, double my, int button) {
    }

    protected void onKeyDown(int key) {
    }

    protected void onKeyUp(int key) {
    }

    protected void onScroll(double mx, double my, double dy) {
    }

    protected boolean playing() {
        return state == State.PLAY;
    }

    /** The game is over: {@code score} 0 to 1, or a critical failure ({@code why}). */
    protected void finish(float score, boolean critical, @Nullable Component why) {
        if (state != State.PLAY) {
            return;
        }
        finalScore = Mth.clamp(score, 0, 1);
        reason = why;
        setState(State.WAITING);
        if (start != null) {
            PacketDistributor.sendToServer(new MetierNet.Result(start.session(), finalScore, critical));
        } else {
            practiceOutcome = critical || finalScore < criticalBelow ? 0 : finalScore >= perfectAt ? 2 : 1;
            setState(State.RESULT);
        }
        sound(critical ? SoundEvents.ITEM_BREAK : SoundEvents.NOTE_BLOCK_BELL.value(), critical ? .8f : 1.2f, .6f);
    }

    protected void flash(Component text, int color) {
        flash = text;
        flashColor = color;
        flashAt = Util.getMillis();
    }

    protected void shake(float amount) {
        shake = Math.max(shake, amount);
    }

    protected void sound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    protected boolean inArea(double mx, double my) {
        return mx >= AX0 && mx < AX1 && my >= AY0 && my < AY1;
    }

    // ---------------------------------------------------------------- from the server

    void outcome(MetierNet.Outcome o) {
        outcome = o;
        setState(State.RESULT);
    }

    void stopped(Component why) {
        stopped = why;
        setState(State.RESULT);
    }

    /** Whether the server has said what came of it (or called it off). */
    public boolean settled() {
        return outcome != null || stopped != null;
    }

    /** The outcome's ordinal ({@link MetierDefs.Outcome}), or -1. */
    public int outcomeOrdinal() {
        return outcome == null ? -1 : outcome.outcome();
    }

    public int session() {
        return start == null ? -1 : start.session();
    }

    private void setState(State s) {
        state = s;
        stateAt = Util.getMillis();
        resultClock = 0;
    }

    private int resultIndex() {
        return Mth.clamp(outcome != null ? outcome.outcome() : practiceOutcome, 0, 2);
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x90000000, 0xB8000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        float dt = Math.min(.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (state == State.INTRO && now - stateAt > INTRO) {
            setState(State.PLAY);
            sound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f, .5f);
        }
        if (state == State.PLAY) {
            time += dt;
            update(dt);
        }
        if (state == State.RESULT) {
            resultClock += dt;
        }
        shownScore = Mth.lerp(Math.min(1, dt * 8), shownScore, state == State.PLAY || state == State.INTRO ? liveScore() : finalScore);
        shake = Math.max(0, shake - dt * 22);
        float sx = shake > 0 ? (random.nextFloat() - .5f) * shake : 0, sy = shake > 0 ? (random.nextFloat() - .5f) * shake : 0;
        float mx = (mouseX - left) / k, my = (mouseY - top) / k;
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(k, k, 1);
        drawWindow(g);
        g.enableScissor((int) (left + AX0 * k), (int) (top + AY0 * k), (int) Math.ceil(left + AX1 * k), (int) Math.ceil(top + AY1 * k));
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0);
        Draw.picture(g, scene(), AX0 - 4, AY0 - 2, AX1 + 4, AY1 + 2, 784, 360, 0xFFFFFFFF);
        drawGame(g, mx - sx, my - sy);
        g.pose().popPose();
        // Inner shadow along the scene's edges
        Draw.vgrad(g, AX0, AY0, AX1, AY0 + 10, 0x70000000, 0x00000000);
        Draw.vgrad(g, AX0, AY1 - 8, AX1, AY1, 0x00000000, 0x60000000);
        if (state == State.PLAY) {
            drawControls(g);
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (flash != null && now - flashAt < 800) {
            float t = (now - flashAt) / 800f;
            float pop = .8f + .4f * Draw.back(Math.min(1, t * 5));
            int a = (int) (255 * (1 - Math.max(0, t - .5f) * 2));
            Draw.title(g, flash, (AX0 + AX1) / 2f, AY0 + 14 - t * 6, Draw.argb(a, flashColor), 1.5f * pop);
        }
        if (state == State.INTRO) {
            drawIntro(g, now);
        } else if (state == State.WAITING || state == State.RESULT) {
            drawResult(g, now, dt);
        }
        if (state == State.PLAY && now - escapeAt < 2500) {
            Draw.title(g, Component.translatable("metier.arcanes_oublies.game.give_up"), (AX0 + AX1) / 2f, AY1 - 34, 0xFFFF8070, 1);
        }
        g.pose().popPose();
        g.disableScissor();
        drawGauge(g);
        g.pose().popPose();
    }

    private void drawWindow(GuiGraphics g) {
        Draw.panel(g, 0, 0, W, H, theme);
        // Header: the theme's colour fading out, the subject, the title, the tool, the difficulty
        Draw.vgrad(g, 1, 1, W - 1, 42, Draw.argb(140, theme), Draw.argb(0, theme));
        Draw.glow(g, 24, 22, 24, Draw.argb(80, Draw.lerp(theme, 0xFFFFFFFF, .4f)));
        Draw.itemAt(g, subject, 24, 22, 26);
        Draw.text(g, heading, 46, 10, 0xFFFFF6E6, 1.35f);
        Draw.text(g, subheading, 46, 27, Draw.lerp(theme, 0xFFFFFFFF, .7f), .85f);
        float x = W - 14;
        if (!tool.isEmpty()) {
            Draw.itemAt(g, tool, x - 11, 22, 22);
            x -= 30;
        }
        int pips = (int) Mth.clamp(Math.round(difficulty * 2), 1, 6);
        int pipColor = difficulty > 1.25 ? 0xFFFF6A50 : difficulty < .9 ? 0xFF8AE07A : 0xFFFFC850;
        for (int i = 0; i < 6; i++) {
            float px = x - (5 - i) * 9 - 4;
            Draw.disc(g, px, 22, 3.2f, i < pips ? pipColor : 0x38FFFFFF);
        }
        Draw.rect(g, AX0 - 2, AY0 - 2, AX1 + 2, AY1 + 2, Draw.argb(220, Draw.lerp(theme, 0, .7f)));
    }

    private void drawControls(GuiGraphics g) {
        List<Control> list = controls();
        if (list.isEmpty()) {
            return;
        }
        int alpha = time < 5 ? 235 : 140;
        float total = 0;
        for (Control c : list) {
            total += Math.max(14, Draw.width(c.key(), .8f) + 8) + 4 + Draw.width(c.label(), .85f) + 12;
        }
        float x = (AX0 + AX1) / 2f - total / 2, y = AY1 - 16;
        Draw.vgrad(g, AX0, AY1 - 28, AX1, AY1, 0x00000000, Draw.argb(alpha / 2, 0));
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        for (Control c : list) {
            x += Draw.keycap(g, c.key(), c.label(), x, y, alpha);
        }
        g.pose().popPose();
    }

    private void drawGauge(GuiGraphics g) {
        float x0 = 76, x1 = W - 92, y = AY1 + 20;
        float s = shownScore;
        int color = s >= perfectAt ? 0xFFFFD25A : s >= criticalBelow ? 0xFF8FD4FF : 0xFFFF6A5A;
        Draw.text(g, Component.translatable("metier.arcanes_oublies.game.quality"), 14, y - 1, 0xFFD8CEBC, .95f);
        Draw.pill(g, x0 - 2, y - 2, x1 + 2, y + 10, 0xFF0C0A08);
        Draw.pill(g, x0, y, x1, y + 8, 0xFF2A2420);
        float fill = x0 + (x1 - x0) * Mth.clamp(s, 0, 1);
        if (fill > x0 + 1) {
            Draw.pill(g, x0, y, fill, y + 8, color);
            Draw.pill(g, x0 + 2, y + 1, Math.max(x0 + 4, fill - 2), y + 3.5f, Draw.argb(100, 0xFFFFFFFF));
            Draw.glow(g, fill, y + 4, 10, Draw.argb(110, color));
        }
        float pc = x0 + (x1 - x0) * perfectAt, cc = x0 + (x1 - x0) * criticalBelow;
        Draw.rect(g, pc - .5f, y - 3, pc + .5f, y + 11, 0xFFFFD25A);
        Draw.rect(g, cc - .5f, y - 3, cc + .5f, y + 11, 0xFFFF6A5A);
        Draw.star(g, pc, y - 4, 6, 0xFFFFE070);
        Component label = Component.translatable("metier.arcanes_oublies.outcome." + (s >= perfectAt ? "perfect" : s >= criticalBelow ? "partial" : "critical"));
        Draw.text(g, label, x1 + 10, y, color, .95f);
    }

    private void drawIntro(GuiGraphics g, long now) {
        float t = (now - stateAt) / (float) INTRO;
        Draw.rect(g, AX0, AY0, AX1, AY1, Draw.argb((int) (150 * (1 - Math.max(0, t - .85f) / .15f)), 0));
        float cx = (AX0 + AX1) / 2f, cy = (AY0 + AY1) / 2f + 6;
        int n = Math.max(1, 3 - (int) (t * 3));
        float local = (t * 3) % 1;
        float pop = Draw.back(Math.min(1, local * 3));
        Draw.glow(g, cx, cy, 70 * pop, Draw.argb((int) (90 * (1 - local)), theme));
        Draw.ring(g, cx, cy, 26 + 34 * local, Draw.argb((int) (200 * (1 - local)), Draw.lerp(theme, 0xFFFFFFFF, .5f)), true);
        Draw.centered(g, Component.literal(String.valueOf(n)), cx, cy - 13 * pop, Draw.argb((int) (255 * (1 - local * .6f)), 0xFFFFFFFF), 3.2f * pop);
        Draw.title(g, heading, cx, AY0 + 20, 0xFFFFF0D8, 1.3f);
    }

    private void drawResult(GuiGraphics g, long now, float dt) {
        float t = Draw.ease((now - stateAt) / 400f);
        Draw.rect(g, AX0, AY0, AX1, AY1, Draw.argb((int) (175 * t), 0));
        float cx = (AX0 + AX1) / 2f, cy = (AY0 + AY1) / 2f;
        if (stopped != null) {
            Draw.title(g, stopped, cx, cy - 6, 0xFFFF9A8A, 1.3f);
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.close"), cx, AY1 - 14, 0xFFA8A090, .8f);
            return;
        }
        if (state == State.WAITING) {
            for (int i = 0; i < 3; i++) {
                float a = (now / 300f + i * .33f) % 1;
                Draw.disc(g, cx - 12 + i * 12, cy, 3 + 2 * Mth.sin(a * Mth.PI), 0xC0FFFFFF);
            }
            return;
        }
        MetierDefs.Outcome oc = MetierDefs.Outcome.values()[resultIndex()];
        int color = switch (oc) {
            case PERFECT -> 0xFFFFD25A;
            case PARTIAL -> 0xFF9AD8FF;
            case CRITICAL -> 0xFFFF5A4A;
        };
        float pop = Draw.back(Math.min(1, (now - stateAt) / 450f));
        Draw.glow(g, cx, cy - 40, 120 * pop, Draw.argb(oc == MetierDefs.Outcome.CRITICAL ? 60 : 100, color));
        if (oc == MetierDefs.Outcome.PERFECT) {
            if (random.nextFloat() < dt * 30) {
                confetti.add(resultClock, cx + (random.nextFloat() - .5f) * 300, AY0 + 6, (random.nextFloat() - .5f) * 30, 20 + random.nextFloat() * 30,
                        3f, 2.5f + random.nextFloat() * 2, Draw.hsv(.07f + random.nextFloat() * .1f, .65f, 1), 30, false);
            }
            for (int i = 0; i < 5; i++) {
                float a = resultClock * .7f + i * 1.2566f;
                Draw.star(g, cx + Mth.cos(a) * 118, cy - 40 + Mth.sin(a) * 24, 7 + 3 * Mth.sin(resultClock * 5 + i), 0xB0FFF0A0);
            }
        }
        confetti.draw(g, resultClock);
        Draw.title(g, Component.translatable("metier.arcanes_oublies.outcome." + oc.name().toLowerCase(java.util.Locale.ROOT) + ".big"), cx, cy - 54, color,
                2.4f * pop);
        float y = cy - 22;
        if (reason != null) {
            Draw.centered(g, reason, cx, y, 0xFFFFC0B0, 1);
            y += 13;
        }
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.score", Math.round(finalScore * 100)), cx, y, 0xFFE0D8C8, .9f);
        y += 16;
        if (outcome != null) {
            List<ItemStack> items = outcome.items();
            float x = cx - items.size() * 17;
            for (int i = 0; i < items.size(); i++) {
                ItemStack s = items.get(i);
                float it = Draw.back(Mth.clamp((now - stateAt - 250 - i * 120) / 350f, 0, 1));
                float ix = x + i * 34 + 17;
                Draw.glow(g, ix, y + 14, 20 * it, Draw.argb(90, color));
                Draw.itemAt(g, s, ix, y + 14, 26 * it);
                if (it > .5f) {
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 250);
                    Draw.text(g, Component.literal("×" + s.getCount()), ix + 4, y + 22, 0xFFFFFFFF, .9f);
                    g.pose().popPose();
                }
            }
            if (items.isEmpty()) {
                Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.nothing"), cx, y + 10, 0xFFA8A090, 1);
            }
            if (outcome.durability() > 0) {
                Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.wear", outcome.durability()), cx, y + 38, 0xFFFF9A7A, .9f);
            } else if (outcome.durability() < 0) {
                // The grindstone: durability given back
                Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.repaired", -outcome.durability()), cx, y + 38, 0xFF8AE07A, .9f);
            }
        } else {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.trial"), cx, y + 8, 0xFFA8DCFF, 1);
        }
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.close"), cx, AY1 - 14, 0xFFA8A090, .8f);
        if (now - stateAt > 7000) {
            onClose();
        }
    }

    // ---------------------------------------------------------------- input

    private double ux(double mouseX) {
        return (mouseX - left) / k;
    }

    private double uy(double mouseY) {
        return (mouseY - top) / k;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (state == State.RESULT && Util.getMillis() - stateAt > 700) {
            onClose();
            return true;
        }
        if (state == State.PLAY) {
            onMouseDown(ux(mouseX), uy(mouseY), button);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (state == State.PLAY) {
            onMouseUp(ux(mouseX), uy(mouseY), button);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (state == State.PLAY) {
            onMouseDrag(ux(mouseX), uy(mouseY), button);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (state == State.PLAY) {
            onScroll(ux(mouseX), uy(mouseY), scrollY);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (state == State.PLAY || state == State.INTRO) {
                long now = Util.getMillis();
                if (now - escapeAt < 2500) {
                    if (state == State.INTRO) {
                        setState(State.PLAY);
                    }
                    finish(0, true, Component.translatable("metier.arcanes_oublies.game.abandoned"));
                } else {
                    escapeAt = now;
                }
                return true;
            }
            if (state == State.RESULT) {
                onClose();
            }
            return true;
        }
        if (state == State.PLAY) {
            onKeyDown(key);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (state == State.PLAY) {
            onKeyUp(key);
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
