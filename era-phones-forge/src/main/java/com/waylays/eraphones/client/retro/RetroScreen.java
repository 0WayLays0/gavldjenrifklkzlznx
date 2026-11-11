package com.waylays.eraphones.client.retro;

import com.mojang.blaze3d.platform.InputConstants;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneHud;
import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * An older phone held up at the right of the screen: its body (a texture drawn at three pixels a unit), its screen,
 * and its keys, which are clicked or played from the keyboard (digits, Enter for OK, Backspace for C, the arrows; any
 * letter typed goes into the text being written, and on the keys themselves letters come by pressing again, as they
 * did). While a call is going on, what is said shows beside the phone, with a line to speak into.
 */
public abstract class RetroScreen extends Screen {
    /** A key: its place on the body, its label (and the letters under it), its look. */
    public record Key(String id, float x, float y, float w, float h, String label, String sub, int color, int text) {}

    protected final int bodyW, bodyH;
    protected final ResourceLocation body;
    protected final List<Key> keys = new ArrayList<>();
    protected float px, py, scale = 1;
    private long openedAt;
    private boolean leaving;
    private long leftAt;
    private String pressed = "";
    private long pressedAt;
    /** The line being said into the call. */
    protected String speech = "";
    // Letters by pressing a key again
    private String tapKey = "";
    private int tapCount;
    private long tapAt;

    protected RetroScreen(String texture, int bodyW, int bodyH) {
        super(Component.translatable("phone.eraphones.name"));
        this.body = EraPhones.id("textures/gui/phone/" + texture + ".png");
        this.bodyW = bodyW;
        this.bodyH = bodyH;
    }

    @Override
    protected void init() {
        if (openedAt == 0) {
            openedAt = System.currentTimeMillis();
            opened();
        }
    }

    /** Opened (the first time it is drawn). */
    protected void opened() {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- what each phone does

    /** The screen's place on the body: x0, y0, x1, y1. */
    protected abstract int[] screenBox();

    /** Draws the screen's content, in body coordinates. */
    protected abstract void screen(GuiGraphics g, int mx, int my, long now);

    /** A key was pressed (by its id: the digits, "*", "#", "ok", "back", "up", "down", "left", "right", "call", "end", ...). */
    protected abstract void press(String id);

    /** A character typed while writing; false when nothing is being written. */
    protected boolean write(char c) {
        return false;
    }

    /** The characters a key gives when pressed again and again while writing. */
    protected static String letters(String key) {
        return switch (key) {
            case "1" -> ".,?!1'-";
            case "2" -> "abc2àâç";
            case "3" -> "def3éèê";
            case "4" -> "ghi4î";
            case "5" -> "jkl5";
            case "6" -> "mno6ô";
            case "7" -> "pqrs7";
            case "8" -> "tuv8ù";
            case "9" -> "wxyz9";
            case "0" -> " 0";
            default -> "";
        };
    }

    /**
     * A digit key pressed while writing: the next letter of that key if pressed again soon, else its first letter.
     * Returns the text with it.
     */
    protected String tap(String text, String key, int max) {
        String l = letters(key);
        if (l.isEmpty()) {
            return text;
        }
        long now = System.currentTimeMillis();
        if (key.equals(tapKey) && now - tapAt < 900 && !text.isEmpty()) {
            tapCount = (tapCount + 1) % l.length();
            text = text.substring(0, text.length() - 1);
        } else {
            tapCount = 0;
        }
        tapKey = key;
        tapAt = now;
        return text.length() < max ? text + l.charAt(tapCount) : text;
    }

    /** Whether a letter is still being chosen on a key (it shows underlined). */
    protected boolean tapping() {
        return System.currentTimeMillis() - tapAt < 900;
    }

    protected void endTap() {
        tapKey = "";
    }

    // ---------------------------------------------------------------- placing, drawing

    private void place() {
        scale = Math.min(1.5f, (height - 12f) / bodyH);
        float t = (System.currentTimeMillis() - openedAt) / 380f;
        float in = leaving ? 1 - PhoneUI.ease((System.currentTimeMillis() - leftAt) / 260f) : PhoneUI.spring(t);
        px = width - bodyW * scale - Math.max(10, width * .06f);
        py = (height - bodyH * scale) / 2f + (1 - in) * (height * .9f);
    }

    protected double lx(double mx) {
        return (mx - px) / scale;
    }

    protected double ly(double my) {
        return (my - py) / scale;
    }

    /** Scissors to a rectangle given in body coordinates. */
    public void clip(GuiGraphics g, float x0, float y0, float x1, float y1) {
        g.pose().pushPose();
        g.pose().setIdentity();
        g.enableScissor(Mth.floor(px + x0 * scale), Mth.floor(py + y0 * scale), Mth.ceil(px + x1 * scale), Mth.ceil(py + y1 * scale));
        g.pose().popPose();
    }

        public void renderBackground(GuiGraphics g, int mx, int my, float partialTicks) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        if (leaving && System.currentTimeMillis() - leftAt > 260) {
            PhoneClient.closed();
            super.onClose();
            return;
        }
        place();
        long now = System.currentTimeMillis();
        int mx = (int) Math.floor(lx(mouseX)), my = (int) Math.floor(ly(mouseY));
        PhoneUI.glow(g, px + bodyW * scale / 2, py + bodyH * scale / 2, bodyH * scale * 1.2f, 0x40101010);
        callPanel(g, now);
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        g.pose().scale(scale, scale, 1);
        PhoneUI.blend();
        g.blit(body, 0, 0, bodyW, bodyH, 0, 0, bodyW * 3, bodyH * 3, bodyW * 3, bodyH * 3);
        int[] box = screenBox();
        clip(g, box[0], box[1], box[2], box[3]);
        if (PhoneClient.battery() <= 0) {
            g.fill(box[0], box[1], box[2], box[3], 0xFF0A0C0A);
        } else {
            screen(g, mx, my, now);
            if (PhoneClient.haunted()) {
                haunt(g, box, now);
            }
        }
        g.disableScissor();
        for (Key k : keys) {
            drawKey(g, k, mx, my, now);
        }
        g.pose().popPose();
    }

    protected void drawKey(GuiGraphics g, Key k, int mx, int my, long now) {
        boolean down = k.id.equals(pressed) && now - pressedAt < 140;
        boolean hover = PhoneUI.in(mx, my, k.x, k.y, k.w, k.h);
        int r = (int) Math.min(k.h / 2, 5);
        int c = k.color;
        int top = PhoneUI.mix(c, 0xFFFFFFFF, down ? .05f : hover ? .28f : .18f), bottom = PhoneUI.mix(c, 0xFF000000, down ? .1f : .3f);
        PhoneUI.round(g, (int) k.x, (int) k.y + 1, (int) k.w, (int) k.h, r, 0x66000000);
        PhoneUI.gradient(g, (int) k.x, (int) k.y + (down ? 1 : 0), (int) k.w, (int) k.h, r, top, bottom);
        float ty = k.y + (down ? 1 : 0);
        if (k.sub.isEmpty()) {
            PhoneUI.centered(g, k.label, k.x + k.w / 2f, ty + (k.h - 6.5f) / 2f, k.text, .8f);
        } else {
            PhoneUI.centered(g, k.label, k.x + k.w / 2f, ty + 2, k.text, .8f);
            PhoneUI.centered(g, k.sub, k.x + k.w / 2f, ty + k.h - 6, PhoneUI.alpha(k.text, .6f), .4f);
        }
    }

    /** The screen goes wrong: lines of static, the picture torn, a red balloon drifting up behind it. */
    protected void haunt(GuiGraphics g, int[] b, long now) {
        java.util.Random r = new java.util.Random(now / 60);
        int w = b[2] - b[0], h = b[3] - b[1];
        float left = Math.min(1, (PhoneClient.hauntUntil - now) / 1000f);
        for (int k = 0; k < 50; k++) {
            int x = b[0] + r.nextInt(w), y = b[1] + r.nextInt(h);
            g.fill(x, y, x + 1 + r.nextInt(4), y + 1, PhoneUI.alpha(r.nextBoolean() ? 0xFFFFFFFF : 0xFF000000, .55f * left));
        }
        float t = (now % 5000) / 5000f;
        float bx = (b[0] + b[2]) / 2f + Mth.sin(now / 600f) * w * .2f, by = b[3] + 10 - t * (h + 30);
        PhoneUI.blend();
        g.blit(EraPhones.id("textures/gui/phone/balloon.png"), (int) bx - 6, (int) by - 8, 12, 24, 0, 0, 32, 64, 32, 64);
        if (!PhoneClient.hauntText.isEmpty() && now % 900 < 600) {
            PhoneUI.paragraph(g, PhoneClient.hauntText, (b[0] + b[2]) / 2f, (b[1] + b[3]) / 2f - 6, w - 8, PhoneUI.alpha(0xFFFF2020, left), .6f);
        }
    }

    /** Beside the phone during a call: what each end said, and the line being spoken. */
    private void callPanel(GuiGraphics g, long now) {
        if (PhoneClient.callState != 3) {
            return;
        }
        int w = Math.min(220, (int) (px - 24));
        if (w < 80) {
            return;
        }
        int x = (int) px - w - 12, bottom = height - 30;
        int y = bottom - 18;
        PhoneUI.round(g, x, y, w, 16, 6, 0xC0000000);
        String shown = speech.isEmpty() ? Component.translatable("phone.eraphones.speak").getString() : speech + (now % 1000 < 500 ? "_" : "");
        PhoneUI.text(g, PhoneUI.fit(shown, w - 12, .75f), x + 6, y + 4, speech.isEmpty() ? 0xFF8A8A8A : 0xFFFFFFFF, .75f);
        y -= 4;
        String other = PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
        for (int i = PhoneClient.SAID.size() - 1; i >= 0 && y > 40; i--) {
            PhoneClient.Said s = PhoneClient.SAID.get(i);
            String who = s.mine() ? Component.translatable("phone.eraphones.you").getString() : other;
            var lines = PhoneUI.wrap(who + " : " + s.text(), w - 12, .7f);
            int h = lines.size() * 7 + 6;
            y -= h + 3;
            PhoneUI.round(g, x, y, w, h, 5, s.mine() ? 0x90203040 : 0x90000000);
            PhoneUI.lines(g, lines, x + 6, y + 3, s.mine() ? 0xFFB8C8E0 : 0xFFFFFFFF, .7f);
        }
        PhoneUI.text(g, "✆ " + PhoneHud.timer(now - PhoneClient.callSince), x, 26, 0xFF9AE6B4, .8f);
    }

    // ---------------------------------------------------------------- input

    protected void click(String id) {
        pressed = id;
        pressedAt = System.currentTimeMillis();
        beep(id);
        // A call coming in, or going on: the green key answers, the red one hangs up, whatever the screen shows
        if (PhoneClient.callState == 2 && (id.equals("call") || id.equals("snd") || id.equals("ok"))) {
            PhoneClient.answer(true);
            return;
        }
        if (PhoneClient.callState != 0 && id.equals("end")) {
            if (PhoneClient.callState == 2) {
                PhoneClient.answer(false);
            } else {
                PhoneClient.hangUp();
            }
            return;
        }
        if (PhoneClient.battery() <= 0) {
            return;
        }
        press(id);
    }

    /** The key's tone: higher for the higher digits. */
    protected void beep(String id) {
        if (!PhoneSettings.get().sounds) {
            return;
        }
        int d = "123456789*0#".indexOf(id);
        float pitch = d >= 0 ? 1.3f + d * .05f : 1.1f;
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, .25f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = lx(mouseX), my = ly(mouseY);
        for (Key k : keys) {
            if (PhoneUI.in(mx, my, k.x, k.y, k.w, k.h)) {
                click(k.id);
                return true;
            }
        }
        if (PhoneUI.in(mx, my, 0, 0, bodyW, bodyH)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (PhoneClient.callState == 3) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                PhoneClient.speak(speech);
                speech = "";
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!speech.isEmpty()) {
                    speech = speech.substring(0, speech.length() - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_END) {
                click("end");
                return true;
            }
            return true;
        }
        if (PhoneClient.OPEN.isActiveAndMatches(InputConstants.getKey(key, scan)) && !writing()) {
            onClose();
            return true;
        }
        String id = switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> "ok";
            case GLFW.GLFW_KEY_BACKSPACE -> "back";
            case GLFW.GLFW_KEY_UP -> "up";
            case GLFW.GLFW_KEY_DOWN -> "down";
            case GLFW.GLFW_KEY_LEFT -> "left";
            case GLFW.GLFW_KEY_RIGHT -> "right";
            case GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_HOME -> "call";
            case GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_END, GLFW.GLFW_KEY_DELETE -> "end";
            default -> null;
        };
        if (id != null) {
            click(id);
            return true;
        }
        return true;
    }

    /** Whether text is being written (letters typed go to it, and the phone key does not close the phone). */
    protected boolean writing() {
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (PhoneClient.callState == 3) {
            if (speech.length() < 200 && c >= ' ') {
                speech += c;
            }
            return true;
        }
        if (writing() && write(c)) {
            return true;
        }
        if (Character.isDigit(c) || c == '*' || c == '#') {
            click(String.valueOf(c));
        }
        return true;
    }

    @Override
    public void onClose() {
        if (!leaving) {
            leaving = true;
            leftAt = System.currentTimeMillis();
        }
    }

    // ---------------------------------------------------------------- helpers for the screens

    /** The reason a call ended, as the phone says it. */
    protected static String endReason() {
        return Component.translatable("phone.eraphones.call_end." + PhoneClient.callReason).getString();
    }

    protected static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}
