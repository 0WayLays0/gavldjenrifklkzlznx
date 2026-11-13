package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The frame every trade's minigame is played in: a title bar (what is worked, with what, how hard), the play area, a
 * quality gauge with the partial and perfect marks, and the countdown, the result and what came of it. A minigame
 * started from the editor is a trial: nothing is sent, nothing given.
 */
public abstract class MinigameScreen extends Screen {
    protected static final int W = 340, H = 226;
    /** The play area, in the window. */
    protected static final int AX0 = 10, AY0 = 36, AX1 = 330, AY1 = 196;

    private enum State {INTRO, PLAY, WAITING, RESULT}

    @Nullable
    protected final MetierNet.Start start;
    protected final double difficulty;
    protected final Random random;
    protected final ItemStack subject, tool;
    protected final List<ItemStack> inputs;
    private final Component heading, subheading;
    private final float perfectAt, criticalBelow;
    private final int theme;

    private State state = State.INTRO;
    private long stateAt = Util.getMillis();
    private long lastFrame = Util.getMillis();
    protected float time;
    private float finalScore;
    private boolean finalCritical;
    @Nullable
    private Component reason;
    @Nullable
    private MetierNet.Outcome outcome;
    private int practiceOutcome = -1;
    @Nullable
    private Component stopped;
    private long escapeAt = -10000;
    protected int left, top;
    private float shake;
    /** A word shown over the play area for a moment (Net !, Raté…). */
    @Nullable
    private Component flash;
    private int flashColor;
    private long flashAt;
    /** The screen to go back to (the editor, after a trial). */
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

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
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
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- for the games

    /** Advances the game by {@code dt} seconds. */
    protected abstract void update(float dt);

    /** Draws the play area (window coordinates; the mouse too). */
    protected abstract void drawGame(GuiGraphics g, int mx, int my);

    /** The rules, shown during the countdown. */
    protected abstract List<Component> rules();

    /** The score so far (0 to 1), for the gauge. */
    protected abstract float liveScore();

    protected void onMouseDown(double mx, double my, int button) {
    }

    protected void onMouseUp(double mx, double my, int button) {
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
        finalCritical = critical;
        reason = why;
        setState(State.WAITING);
        if (start != null) {
            PacketDistributor.sendToServer(new MetierNet.Result(start.session(), finalScore, critical));
        } else {
            practiceOutcome = critical || finalScore < criticalBelow ? 0 : finalScore >= perfectAt ? 2 : 1;
            setState(State.RESULT);
        }
        sound(critical ? net.minecraft.sounds.SoundEvents.ITEM_BREAK : net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), critical ? .8f : 1.2f, .6f);
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

    /** 1 at difficulty 1, more as it gets harder: {@code base * difficulty^power}. */
    protected double hard(double base, double power) {
        return base * Math.pow(difficulty, power);
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
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        float dt = Math.min(.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (state == State.INTRO && now - stateAt > 2600) {
            setState(State.PLAY);
        }
        if (state == State.PLAY) {
            time += dt;
            update(dt);
        }
        shake = Math.max(0, shake - dt * 18);
        float sx = shake > 0 ? (random.nextFloat() - .5f) * shake : 0, sy = shake > 0 ? (random.nextFloat() - .5f) * shake : 0;
        g.pose().pushPose();
        g.pose().translate(left + sx, top + sy, 0);
        int mx = mouseX - left, my = mouseY - top;
        drawWindow(g);
        g.enableScissor(left + AX0, top + AY0, left + AX1, top + AY1);
        drawGame(g, mx, my);
        g.disableScissor();
        drawGauge(g);
        if (flash != null && now - flashAt < 700) {
            float t = (now - flashAt) / 700f;
            Draw.centered(g, flash, W / 2f, AY0 + 8 - t * 6, Draw.argb((int) (255 * (1 - t * t)), flashColor), 1.6f);
        }
        if (state == State.INTRO) {
            drawIntro(g, now);
        } else if (state == State.WAITING || state == State.RESULT) {
            drawResult(g, now);
        }
        if (state == State.PLAY && now - escapeAt < 2500) {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.give_up"), W / 2f, AY1 - 12, 0xFFFF7070, 1);
        }
        g.pose().popPose();
    }

    private void drawWindow(GuiGraphics g) {
        g.fill(-3, -2, W + 4, H + 5, 0x70000000);
        g.fillGradient(0, 0, W, H, Draw.argb(250, Draw.lerp(theme, 0xFF000000, .55f)), Draw.argb(250, Draw.lerp(theme, 0xFF000000, .82f)));
        g.renderOutline(0, 0, W, H, Draw.lerp(theme, 0xFFFFFFFF, .25f));
        g.renderOutline(1, 1, W - 2, H - 2, Draw.argb(120, theme));
        g.fillGradient(2, 2, W - 2, 30, Draw.argb(200, theme), Draw.argb(40, theme));
        Draw.item(g, subject, 8, 7, 1.1f);
        Draw.text(g, heading, 30, 6, 0xFFFFF4E0, 1.1f);
        Draw.text(g, subheading, 30, 18, 0xFFD0C8B8, .75f);
        if (!tool.isEmpty()) {
            Draw.item(g, tool, W - 26, 7, 1);
        }
        // Difficulty, as stars
        int stars = (int) Mth.clamp(Math.round(difficulty * 2), 1, 8);
        Component diff = Component.translatable("metier.arcanes_oublies.game.difficulty").append(" ")
                .append(Component.literal("★".repeat(stars)).withStyle(difficulty > 1.25 ? ChatFormatting.RED : difficulty < .9 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        Draw.text(g, diff, W - 34 - font.width(diff) * .75f, 12, 0xFFE0D8C8, .75f);
        Draw.frame(g, AX0 - 1, AY0 - 1, AX1 + 1, AY1 + 1, 0x50000000, Draw.argb(160, theme));
    }

    private void drawGauge(GuiGraphics g) {
        int x0 = 60, x1 = W - 60, y = AY1 + 9;
        float s = state == State.PLAY || state == State.INTRO ? liveScore() : finalScore;
        Draw.text(g, Component.translatable("metier.arcanes_oublies.game.quality"), 10, y - 2, 0xFFD0C8B8, .75f);
        g.fill(x0, y, x1, y + 8, 0xA0000000);
        int fillTo = x0 + (int) ((x1 - x0) * Mth.clamp(s, 0, 1));
        int color = s >= perfectAt ? 0xFFFFD25A : s >= criticalBelow ? 0xFF8FD4FF : 0xFFFF6060;
        g.fillGradient(x0, y, fillTo, y + 8, color, Draw.lerp(color, 0xFF000000, .4f));
        g.renderOutline(x0 - 1, y - 1, x1 - x0 + 2, 10, 0xFF8A8070);
        int pc = x0 + (int) ((x1 - x0) * perfectAt), cc = x0 + (int) ((x1 - x0) * criticalBelow);
        g.fill(pc, y - 2, pc + 1, y + 10, 0xFFFFD25A);
        g.fill(cc, y - 2, cc + 1, y + 10, 0xFFFF6060);
        Component label = s >= perfectAt ? Component.translatable("metier.arcanes_oublies.outcome.perfect")
                : s >= criticalBelow ? Component.translatable("metier.arcanes_oublies.outcome.partial") : Component.translatable("metier.arcanes_oublies.outcome.critical");
        Draw.text(g, label, x1 + 6, y - 1, color, .75f);
    }

    private void drawIntro(GuiGraphics g, long now) {
        float t = (now - stateAt) / 2600f;
        g.fill(AX0, AY0, AX1, AY1, 0xC0000000);
        int y = AY0 + 14;
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.how"), W / 2f, y, 0xFFFFE6A0, 1.2f);
        y += 18;
        for (Component line : rules()) {
            for (var seq : font.split(line, AX1 - AX0 - 30)) {
                g.drawString(font, seq, AX0 + 15, y, 0xFFF0EAD8, true);
                y += 10;
            }
            y += 2;
        }
        int count = 3 - (int) (t * 3);
        Draw.centered(g, Component.literal(count > 0 ? String.valueOf(count) : "!"), W / 2f, AY1 - 30, 0xFFFFFFFF, 2.2f);
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.esc"), W / 2f, AY1 - 10, 0xFFA09888, .75f);
    }

    private void drawResult(GuiGraphics g, long now) {
        float t = Math.min(1, (now - stateAt) / 350f);
        g.fill(AX0, AY0, AX1, AY1, Draw.argb((int) (190 * t), 0));
        int cy = AY0 + 30;
        if (stopped != null) {
            Draw.centered(g, stopped, W / 2f, cy + 20, 0xFFFF9090, 1.1f);
            return;
        }
        if (state == State.WAITING) {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.waiting"), W / 2f, cy + 20, 0xFFE0D8C8, 1);
            return;
        }
        int o = outcome != null ? outcome.outcome() : practiceOutcome;
        MetierDefs.Outcome oc = MetierDefs.Outcome.values()[Mth.clamp(o, 0, 2)];
        int color = switch (oc) {
            case PERFECT -> 0xFFFFD25A;
            case PARTIAL -> 0xFFB8E0FF;
            case CRITICAL -> 0xFFFF5050;
        };
        float pop = 1 + .35f * (1 - t);
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.outcome." + oc.name().toLowerCase(java.util.Locale.ROOT) + ".big"), W / 2f,
                cy - 4, color, 2.2f * pop);
        if (reason != null) {
            Draw.centered(g, reason, W / 2f, cy + 22, 0xFFFFB0A0, .9f);
        }
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.score", Math.round(finalScore * 100)), W / 2f, cy + 34, 0xFFD8D0C0, .85f);
        if (outcome != null) {
            List<ItemStack> items = outcome.items();
            int n = items.size();
            int x = W / 2 - n * 12;
            for (ItemStack s : items) {
                Draw.item(g, s, x + 4, cy + 50, 1.25f);
                g.pose().pushPose();
                g.pose().translate(0, 0, 300);
                g.drawString(font, "×" + s.getCount(), x + 8, cy + 72, 0xFFFFFFFF, true);
                g.pose().popPose();
                x += 24;
            }
            if (n == 0) {
                Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.nothing"), W / 2f, cy + 56, 0xFFA09888, 1);
            }
            if (outcome.durability() > 0) {
                Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.wear", outcome.durability()), W / 2f, cy + 86, 0xFFFF9070, .85f);
            }
        } else {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.trial"), W / 2f, cy + 56, 0xFFA0D8FF, 1);
        }
        Draw.centered(g, Component.translatable("metier.arcanes_oublies.game.close"), W / 2f, AY1 - 12, 0xFFA09888, .75f);
        if (now - stateAt > 6000) {
            onClose();
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (state == State.RESULT && Util.getMillis() - stateAt > 600) {
            onClose();
            return true;
        }
        if (state == State.PLAY) {
            onMouseDown(mouseX - left, mouseY - top, button);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (state == State.PLAY) {
            onMouseUp(mouseX - left, mouseY - top, button);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (state == State.PLAY) {
            onScroll(mouseX - left, mouseY - top, scrollY);
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
