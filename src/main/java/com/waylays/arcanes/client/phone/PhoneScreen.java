package com.waylays.arcanes.client.phone;

import com.mojang.blaze3d.platform.InputConstants;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.music.MusicLibrary;
import com.waylays.arcanes.client.music.MusicPlayer;
import com.waylays.arcanes.client.phone.apps.PhoneApps;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Arcanophone, held up at the right of the screen: its body of deep violet titanium, its screen. Locked, it shows
 * the time large over its wallpaper, the waiting notifications, and is swiped up to open; open, the home screen: the
 * status bar (the time of day, the "signal" of the player's mana, the "battery" of their health), the rune island at its
 * top (it grows to show a message as it comes), a great weather widget with the hours to come, the latest photos,
 * twelve apps, a search pill, and a dock of glass with four more. An app opens growing out of its icon.
 */
public class PhoneScreen extends Screen {
    /** The phone texture's size and its screen's place in it (phone coordinates). */
    public static final int FW = 184, FH = 368, SX = 10, SY = 10, SW = 164, SH = 348, SR = 24;
    public static final String[] GRID = {"carte", "galerie", "meteo", "calcul", "notes", "banque", "echos", "grimoire", "horloge", "boussole", "reglages",
            "des"};
    public static final String[] DOCK = {"missives", "contacts", "photo", "musique"};
    private static final String[] WALLPAPERS = {"nebula", "aurora", "abyss", "ember"};
    private static final ResourceLocation RECORD = ArcanesOublies.id("textures/gui/music/record.png");
    /** The finishes of the body (a tint over the titanium). */
    public static final int[] FINISHES = {0xFFFFFFFF, 0xFFFFE0A0, 0xFF9AA0B8, 0xFFFFB0D0};

    private final List<PhoneApp> apps = PhoneApps.all();
    @Nullable
    private PhoneApp app;
    @Nullable
    private PhoneApp closing;
    /** Where the open (or closing) app grew from: its icon's place. */
    private float fromX, fromY;
    private long appAt;
    private long openedAt;
    private boolean leaving;
    private long leftAt;
    private boolean locked = true;
    private long unlockedAt = -1;
    private float dragFrom = -1, drag;
    /** Where the music card was drawn on the lock screen, -1 when none. */
    private int nowPlayingY = -1;
    private boolean camera;

    // Where the phone is on the screen this frame, and how big
    private float px, py, scale = 1;

    public PhoneScreen() {
        super(Component.translatable("phone.arcanes_oublies.name"));
        for (PhoneApp a : apps) {
            a.attach(this);
        }
    }

    /** Opens straight on an app (from a notification). */
    public PhoneScreen(String app) {
        this();
        locked = false;
        unlockedAt = 0;
        open(app, SX + SW / 2f, SY + SH / 2f);
    }

    @Override
    protected void init() {
        if (openedAt == 0) {
            openedAt = System.currentTimeMillis();
            PhoneClient.ask(0, "");
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public PhoneSettings settings() {
        return PhoneSettings.get();
    }

    @Nullable
    public PhoneApp current() {
        return app;
    }

    public void open(String id, float x, float y) {
        for (PhoneApp a : apps) {
            if (a.id.equals(id)) {
                app = a;
                closing = null;
                fromX = x;
                fromY = y;
                appAt = System.currentTimeMillis();
                a.open();
                return;
            }
        }
    }

    public void home() {
        if (app != null) {
            closing = app;
            app = null;
            appAt = System.currentTimeMillis();
        }
    }

    void sound(net.minecraft.sounds.SoundEvent s, float pitch) {
        if (settings().sounds) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, .5f));
        }
    }

    public void clickSound() {
        sound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 1.8f);
    }

    // ---------------------------------------------------------------- coordinates

    private void place(float partialTicks) {
        scale = Math.min(1.35f, (height - 12f) / FH);
        float t = (System.currentTimeMillis() - openedAt) / 380f;
        float in = leaving ? 1 - PhoneUI.ease((System.currentTimeMillis() - leftAt) / 260f) : PhoneUI.spring(t);
        px = width - FW * scale - Math.max(10, width * .06f);
        py = (height - FH * scale) / 2f + (1 - in) * (height * .9f);
    }

    private double lx(double mx) {
        return (mx - px) / scale;
    }

    private double ly(double my) {
        return (my - py) / scale;
    }

    /** Scissors to a rectangle given in phone coordinates. */
    public void clip(GuiGraphics g, float x0, float y0, float x1, float y1) {
        // Given in screen coordinates, with no transform in force (whether or not this version applies the pose to it)
        g.pose().pushPose();
        g.pose().setIdentity();
        g.enableScissor(Mth.floor(px + x0 * scale), Mth.floor(py + y0 * scale), Mth.ceil(px + x1 * scale), Mth.ceil(py + y1 * scale));
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTicks) {
        // No blur: the world stays in view; only a soft shade on the phone's side
        g.fillGradient(width / 2, 0, width, height, 0x00000000, 0x00000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        if (leaving && System.currentTimeMillis() - leftAt > 260) {
            super.onClose();
            return;
        }
        place(partialTicks);
        int mx = (int) Math.floor(lx(mouseX)), my = (int) Math.floor(ly(mouseY));
        long now = System.currentTimeMillis();
        float time = now / 1000f;
        // A violet glow behind the phone
        PhoneUI.glow(g, px + FW * scale / 2, py + FH * scale / 2, FH * scale * 1.3f, 0x553A1866);
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        g.pose().scale(scale, scale, 1);
        boolean seeThrough = app != null && app.seeThrough() && appProgress() >= 1;
        if (!seeThrough) {
            wallpaper(g, mx, my, time);
        }
        if (locked || unlockedAt > 0 && now - unlockedAt < 450) {
            lockScreen(g, mx, my, time);
        }
        if (!locked) {
            float appT = appProgress();
            boolean full = app != null && appT >= 1;
            if (!full) {
                float u = unlockedAt > 0 ? PhoneUI.ease((now - unlockedAt) / 450f) : 1;
                float dim = app != null ? appT : closing != null ? 1 - appT : 0;
                home(g, mx, my, time, u, dim);
            }
            appLayer(g, mx, my, partialTicks, appT);
        }
        statusBar(g, now, seeThrough);
        island(g, mx, my, now);
        // The home bar
        PhoneUI.round(g, SX + SW / 2 - 27, SY + SH - 6, 54, 3, 2, app != null && !app.seeThrough() || locked ? 0xDDFFFFFF : 0xDDFFFFFF);
        // The body over it all
        PhoneUI.blend();
        int f = FINISHES[Math.floorMod(settings().finish, FINISHES.length)];
        g.setColor((f >> 16 & 255) / 255f, (f >> 8 & 255) / 255f, (f & 255) / 255f, 1);
        g.blit(PhoneUI.FRAME, 0, 0, FW, FH, 0, 0, FW * 3, FH * 3, FW * 3, FH * 3);
        g.setColor(1, 1, 1, 1);
        g.pose().popPose();
    }

    private float appProgress() {
        float t = (System.currentTimeMillis() - appAt) / 300f;
        return PhoneUI.ease(t);
    }

    private ResourceLocation wallpaperTexture() {
        return ArcanesOublies.id("textures/gui/phone/wallpaper_" + WALLPAPERS[Math.floorMod(settings().wallpaper, WALLPAPERS.length)] + ".png");
    }

    /**
     * The wallpaper, drawn so it keeps inside the screen's rounded corners (row by row near them), shifted a little
     * with the mouse (parallax), motes of light drifting up over it.
     */
    private void wallpaper(GuiGraphics g, int mx, int my, float time) {
        ResourceLocation tex = wallpaperTexture();
        PhoneUI.blend();
        int tw = SW * 3, th = SH * 3;
        for (int y = 0; y < SH; y++) {
            int inset = 0;
            int dy = y < SR ? SR - y : y >= SH - SR ? y - (SH - SR - 1) : 0;
            if (dy > 0) {
                inset = (int) Math.ceil(SR - Math.sqrt(Math.max(0, SR * SR - (double) (dy - .5) * (dy - .5))));
            }
            int band = dy > 0 ? 1 : 1;
            if (dy == 0) {
                // The straight middle in one piece
                int end = SH - SR;
                g.blit(tex, SX, SY + y, SW, end - y, 0, y * 3, tw, (end - y) * 3, tw, th);
                y = end - 1;
                continue;
            }
            g.blit(tex, SX + inset, SY + y, SW - 2 * inset, band, inset * 3, y * 3, tw - inset * 6, 3, tw, th);
        }
        // Motes of light rising
        int color = switch (Math.floorMod(settings().wallpaper, WALLPAPERS.length)) {
            case 1 -> 0x7790FFC0;
            case 2 -> 0x7790D0FF;
            case 3 -> 0x77FFB060;
            default -> 0x77F0A0FF;
        };
        clip(g, SX, SY, SX + SW, SY + SH);
        float shift = (float) (mx - (SX + SW / 2)) * .015f;
        for (int k = 0; k < 22; k++) {
            float speed = 6 + (k * 37 % 11);
            float y = SY + SH - ((time * speed + k * 53) % (SH + 20)) + 10;
            float x = SX + 8 + (k * 71 % (SW - 16)) + 4 * Mth.sin(time * .8f + k) + shift * (1 + k % 3);
            float s = 3 + (k % 4) * 2;
            PhoneUI.glow(g, x, y, s * 2.2f, PhoneUI.alpha(color, .5f + .5f * Mth.sin(time * 2 + k)));
        }
        g.disableScissor();
    }

    // ---------------------------------------------------------------- lock screen

    private void lockScreen(GuiGraphics g, int mx, int my, float time) {
        long now = System.currentTimeMillis();
        float out = unlockedAt > 0 ? PhoneUI.ease((now - unlockedAt) / 450f) : 0;
        float lift = locked ? Math.max(0, drag) : SH * out;
        float a = 1 - out;
        // Kept inside the screen as it slides away
        clip(g, SX, SY, SX + SW, SY + SH);
        g.pose().pushPose();
        g.pose().translate(0, -lift, 0);
        // The date, the time large, a soft light behind it
        PhoneUI.glow(g, SX + SW / 2f, SY + 64, 150, PhoneUI.alpha(0x30FFFFFF, a));
        String date = PhoneClient.dateLine();
        PhoneUI.centered(g, PhoneUI.fit(date, SW - 20, .8f), SX + SW / 2f, SY + 40, PhoneUI.alpha(0xFFFFFFFF, .85f * a), .8f);
        String clock = PhoneClient.clock();
        float big = 3.4f;
        float w = PhoneUI.width(clock, big);
        PhoneUI.text(g, clock, SX + SW / 2f - w / 2f + 1, SY + 50 + 1, PhoneUI.alpha(0x66000000, a), big);
        PhoneUI.text(g, clock, SX + SW / 2f - w / 2f, SY + 50, PhoneUI.alpha(0xFFFFFFFF, a), big);
        // Four round widgets: mana, health, experience, the weather
        lockWidgets(g, SY + 102, a, time);
        int y = SY + 124;
        // What plays, if anything
        nowPlayingY = -1;
        if (MusicPlayer.current() != null) {
            nowPlayingY = y;
            nowPlaying(g, y, a, mx, my + (int) lift, now);
            y += 46;
        }
        // The waiting notifications, stacked
        for (PhoneClient.Note n : PhoneClient.notes()) {
            if (y + 30 > SY + SH - 52) {
                break;
            }
            PhoneUI.glass(g, SX + 8, y, SW - 16, 30, 10, PhoneUI.alpha(0x88201830, a));
            PhoneUI.icon(g, n.app(), SX + 13, y + 6, 18);
            PhoneUI.text(g, PhoneUI.fit(n.title(), 96, .75f), SX + 36, y + 6, PhoneUI.alpha(0xFFFFFFFF, a), .75f);
            PhoneUI.right(g, PhoneUI.ago(n.time()), SX + SW - 13, y + 6, PhoneUI.alpha(0xFFB8B4C8, a), .6f);
            PhoneUI.text(g, PhoneUI.fit(n.text(), 112, .65f), SX + 36, y + 17, PhoneUI.alpha(0xFFD8D4E8, a), .65f);
            y += 34;
        }
        // The two round buttons and the hint to swipe up
        for (int k = 0; k < 2; k++) {
            int bx = k == 0 ? SX + 22 : SX + SW - 22;
            PhoneUI.round(g, bx - 11, SY + SH - 46, 22, 22, 11, PhoneUI.alpha(0x66000000, a));
            PhoneUI.icon(g, k == 0 ? "horloge" : "photo", bx - 7, SY + SH - 42, 14);
        }
        float pulse = .55f + .45f * Mth.sin(time * 3);
        PhoneUI.centered(g, "⌃", SX + SW / 2f, SY + SH - 27 - 2 * pulse, PhoneUI.alpha(0xFFFFFFFF, pulse * a), .7f);
        PhoneUI.centered(g, Component.translatable("phone.arcanes_oublies.swipe").getString(), SX + SW / 2f, SY + SH - 20,
                PhoneUI.alpha(0xFFFFFFFF, pulse * a), .6f);
        g.pose().popPose();
        g.disableScissor();
    }

    /** The lock screen's round widgets: rings for mana, health and experience (with their value), the sky and its warmth. */
    private void lockWidgets(GuiGraphics g, float cy, float a, float time) {
        var p = minecraft.player;
        float mana = PhoneClient.manaFraction(), hp = PhoneClient.healthFraction(), xp = p == null ? 0 : p.experienceProgress;
        float[] fractions = {mana, hp, xp};
        int[] colors = {0xFFB98CFF, 0xFFFF5C7A, 0xFF7CF08A};
        String[] glyphs = {"✦", "♥", "★"};
        String[] values = {Math.round(mana * 100) + "%", p == null ? "" : String.valueOf(Math.round(p.getHealth())),
                p == null ? "" : String.valueOf(p.experienceLevel)};
        for (int k = 0; k < 4; k++) {
            float cx = SX + SW / 2f + (k - 1.5f) * 34;
            PhoneUI.round(g, (int) cx - 14, (int) cy - 14, 28, 28, 14, PhoneUI.alpha(0x50120A20, a));
            if (k < 3) {
                PhoneUI.ring(g, cx, cy, 11, fractions[k], PhoneUI.alpha(colors[k], a), 32);
                PhoneUI.centered(g, glyphs[k], cx, cy - 7, PhoneUI.alpha(colors[k], a), .55f);
                PhoneUI.centered(g, values[k], cx, cy, PhoneUI.alpha(0xFFFFFFFF, a), .6f);
            } else {
                PhoneClient.Sky sky = PhoneClient.sky();
                weatherGlyph(g, sky.night() ? (sky.rain() ? 4 : 1) : sky.rain() ? (sky.thunder() ? 3 : 2) : 0, cx, cy - 4, 3.5f, time);
                PhoneUI.centered(g, sky.temperature() + "°", cx + 1, cy + 3, PhoneUI.alpha(0xFFFFFFFF, a), .6f);
            }
        }
    }

    /** The card of the music playing: the record turning, the title, where it is, play/pause. */
    private void nowPlaying(GuiGraphics g, int y, float a, int mx, int my, long now) {
        MusicLibrary.Track t = MusicPlayer.current();
        if (t == null) {
            return;
        }
        PhoneUI.glass(g, SX + 8, y, SW - 16, 40, 12, PhoneUI.alpha(0x88201830, a));
        PhoneUI.glow(g, SX + 24, y + 20, 40, PhoneUI.alpha(MusicPlayer.playing() ? 0x66FF5AA0 : 0x22FF5AA0, a));
        g.pose().pushPose();
        g.pose().translate(SX + 24, y + 20, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(MusicPlayer.playing() ? (now % 3000) * .12f : 0));
        PhoneUI.blend();
        g.blit(RECORD, -12, -12, 24, 24, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
        PhoneUI.text(g, PhoneUI.fit(t.title, SW - 80, .7f), SX + 42, y + 7, PhoneUI.alpha(0xFFFFFFFF, a), .7f);
        PhoneUI.text(g, PhoneUI.fit(t.artist == null ? "" : t.artist, SW - 80, .55f), SX + 42, y + 16, PhoneUI.alpha(0xFFB8B4C8, a), .55f);
        int bx = SX + 42, bw = SW - 66;
        float at = Math.min(1, MusicPlayer.positionMs() / (float) Math.max(1, MusicPlayer.lengthMs()));
        PhoneUI.round(g, bx, y + 29, bw, 2, 1, PhoneUI.alpha(0x44FFFFFF, a));
        PhoneUI.round(g, bx, y + 29, Math.max(2, (int) (bw * at)), 2, 1, PhoneUI.alpha(0xFFFF7AD9, a));
        // Play / pause, on the right
        int px = SX + SW - 24, py = y + 9;
        boolean hover = PhoneUI.in(mx, my, px - 2, py - 2, 14, 14);
        PhoneUI.round(g, px - 2, py - 2, 14, 14, 7, PhoneUI.alpha(hover ? 0x44FFFFFF : 0x22FFFFFF, a));
        if (MusicPlayer.playing()) {
            g.fill(px + 2, py + 2, px + 4, py + 8, PhoneUI.alpha(0xFFFFFFFF, a));
            g.fill(px + 6, py + 2, px + 8, py + 8, PhoneUI.alpha(0xFFFFFFFF, a));
        } else {
            PhoneUI.text(g, "▶", px + 2, py + 1, PhoneUI.alpha(0xFFFFFFFF, a), .75f);
        }
    }

    /** Unlocks at once (the client smoke test). */
    public void unlockNow() {
        unlock();
    }

    private void unlock() {
        if (locked) {
            locked = false;
            unlockedAt = System.currentTimeMillis();
            drag = 0;
            sound(SoundEvents.AMETHYST_CLUSTER_STEP, 1.8f);
        }
    }

    // ---------------------------------------------------------------- home screen

    private void home(GuiGraphics g, int mx, int my, float time, float in, float dim) {
        g.pose().pushPose();
        // Unlocking: the home grows in from a little larger; an app opening: it falls back a little
        float s = (1 + .12f * (1 - in)) * (1 - .06f * dim);
        g.pose().translate(SX + SW / 2f, SY + SH / 2f, 0);
        g.pose().scale(s, s, 1);
        g.pose().translate(-(SX + SW / 2f), -(SY + SH / 2f), 0);
        weatherWidget(g, SX + 8, SY + 24, SW - 16, 70, time);
        PhoneUI.centered(g, Component.translatable("phone.arcanes_oublies.app.meteo").getString(), SX + SW / 2f, SY + 96, 0xEEFFFFFF, .55f);
        photosWidget(g, SX + 8, SY + 104, SW - 16, 48, mx, my);
        PhoneUI.centered(g, Component.translatable("phone.arcanes_oublies.app.galerie").getString(), SX + SW / 2f, SY + 154, 0xEEFFFFFF, .55f);
        for (int i = 0; i < 12; i++) {
            int c = i % 4, r = i / 4;
            float x = SX + 11 + c * 37, y = SY + 164 + r * 41;
            appIcon(g, GRID[i], x, y, 30, mx, my, true);
        }
        // A pill to search the apps
        PhoneUI.glass(g, SX + SW / 2 - 30, SY + 289, 60, 12, 6, 0x55FFFFFF);
        PhoneUI.centered(g, "⌕ " + Component.translatable("phone.arcanes_oublies.search").getString(), SX + SW / 2f, SY + 292, 0xEEFFFFFF, .55f);
        // The dock
        PhoneUI.glass(g, SX + 6, SY + SH - 46, SW - 12, 38, 14, 0x55FFFFFF);
        for (int i = 0; i < 4; i++) {
            appIcon(g, DOCK[i], SX + 14 + i * 37, SY + SH - 42, 30, mx, my, false);
        }
        g.pose().popPose();
        if (dim > 0) {
            PhoneUI.round(g, SX, SY, SW, SH, SR, PhoneUI.alpha(0xFF000000, dim * .5f));
        }
    }

    private void appIcon(GuiGraphics g, String id, float x, float y, int size, int mx, int my, boolean label) {
        boolean hover = PhoneUI.in(mx, my, x, y, size, size) && app == null && !locked;
        float s = hover ? 1.06f : 1;
        PhoneUI.round(g, (int) x + 1, (int) y + 2, size, size, 9, 0x40000000);
        PhoneUI.icon(g, id, x - size * (s - 1) / 2, y - size * (s - 1) / 2, size * s);
        int badge = PhoneClient.badge(id);
        if (badge > 0) {
            String b = badge > 99 ? "99+" : String.valueOf(badge);
            int bw = Math.max(10, PhoneUI.width(b, .6f) + 6);
            PhoneUI.round(g, (int) (x + size - bw + 4), (int) y - 3, bw, 10, 5, 0xFFFF3B4E);
            PhoneUI.centered(g, b, x + size - bw / 2f + 4, y - 1, 0xFFFFFFFF, .6f);
        }
        if (label) {
            String name = Component.translatable("phone.arcanes_oublies.app." + id).getString();
            PhoneUI.centered(g, PhoneUI.fit(name, 36, .55f), x + size / 2f + .5f, y + size + 3.5f, 0x88000000, .55f);
            PhoneUI.centered(g, PhoneUI.fit(name, 36, .55f), x + size / 2f, y + size + 3, 0xFFFFFFFF, .55f);
        }
    }

    /** The great weather widget: the place, the temperature, the sky, and the hours to come. */
    private void weatherWidget(GuiGraphics g, int x, int y, int w, int h, float time) {
        PhoneClient.Sky sky = PhoneClient.sky();
        int top = sky.night() ? 0xEE24306A : sky.rain() ? 0xEE4A5A78 : 0xEE3C86D8;
        int bottom = sky.night() ? 0xEE121838 : sky.rain() ? 0xEE2A3448 : 0xEE2A5AA8;
        PhoneUI.gradient(g, x, y, w, h, 14, top, bottom);
        PhoneUI.glow(g, x + w - 14, y + 11, 60, sky.night() ? 0x22C8D8FF : 0x33FFE0A0);
        PhoneUI.text(g, PhoneUI.fit(sky.place() + " ➤", 84, .65f), x + 8, y + 6, 0xFFFFFFFF, .65f);
        PhoneUI.text(g, sky.temperature() + "°", x + 8, y + 15, 0xFFFFFFFF, 2.2f);
        weatherGlyph(g, sky.night() ? (sky.rain() ? 4 : 1) : sky.rain() ? (sky.thunder() ? 3 : 2) : 0, x + w - 14, y + 11, 9, time);
        PhoneUI.right(g, sky.condition(), x + w - 8, y + 21, 0xFFFFFFFF, .62f);
        PhoneUI.right(g, sky.range(), x + w - 8, y + 29, 0xDDFFFFFF, .55f);
        // The six hours to come
        for (int k = 0; k < 6; k++) {
            PhoneClient.Hour hr = sky.hours().get(k);
            float cx = x + 14 + k * (w - 28) / 5f;
            PhoneUI.centered(g, hr.label(), cx, y + h - 26, 0xEEFFFFFF, .5f);
            weatherGlyph(g, hr.glyph(), cx, y + h - 15, 4, time + k);
            PhoneUI.centered(g, hr.temperature() + "°", cx, y + h - 8, 0xFFFFFFFF, .55f);
        }
    }

    /** A sun (0), a moon (1), rain (2), a storm (3), a cloudy night (4). */
    public static void weatherGlyph(GuiGraphics g, int kind, float cx, float cy, float r, float time) {
        switch (kind) {
            case 0 -> {
                PhoneUI.glow(g, cx, cy, r * 4.5f, 0x88FFD050);
                PhoneUI.round(g, (int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2), (int) r, 0xFFFFD84A);
            }
            case 1 -> {
                PhoneUI.glow(g, cx, cy, r * 4, 0x66C8D8FF);
                PhoneUI.round(g, (int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2), (int) r, 0xFFEFF2FF);
                PhoneUI.round(g, (int) (cx - r * .4f), (int) (cy - r * 1.1f), (int) (r * 2), (int) (r * 2), (int) r, 0xFF24306A);
            }
            default -> {
                int cloud = kind == 3 ? 0xFF8A90A8 : 0xFFE6ECF6;
                PhoneUI.round(g, (int) (cx - r * 1.3f), (int) (cy - r * .4f), (int) (r * 2.6f), (int) (r * 1.2f), (int) (r * .6f), cloud);
                PhoneUI.round(g, (int) (cx - r * .7f), (int) (cy - r), (int) (r * 1.4f), (int) (r * 1.4f), (int) (r * .7f), cloud);
                if (kind == 2 || kind == 3) {
                    for (int d = 0; d < 3; d++) {
                        float fall = (time * 3 + d * .33f) % 1;
                        g.fill((int) (cx - r + d * r * .8f), (int) (cy + r * .9f + fall * r), (int) (cx - r + d * r * .8f + 1),
                                (int) (cy + r * 1.4f + fall * r), 0xCC9AD0FF);
                    }
                }
                if (kind == 3) {
                    PhoneUI.glow(g, cx, cy + r, r * 3, PhoneUI.alpha(0xFFFFF0A0, Math.max(0, Mth.sin(time * 7)) * .8f));
                }
            }
        }
    }

    private void photosWidget(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        PhoneUI.glass(g, x, y, w, h, 14, 0xAA1A1622);
        PhoneUI.icon(g, "galerie", x + 7, y + 5, 10);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.photos").getString(), x + 20, y + 7, 0xFFFFFFFF, .6f);
        List<ResourceLocation> thumbs = PhoneClient.recentPhotos(4);
        int tw = (w - 14 - 3 * 4) / 4;
        for (int k = 0; k < 4; k++) {
            int tx = x + 7 + k * (tw + 4), ty = y + 18;
            if (k < thumbs.size()) {
                PhoneUI.round(g, tx - 1, ty - 1, tw + 2, h - 23, 5, 0x55FFFFFF);
                PhoneUI.blend();
                g.blit(thumbs.get(k), tx, ty, tw, h - 25, 0, 0, 128, 96, 128, 96);
            } else {
                int[] tones = {0xFF6A3AA8, 0xFF2A6AB8, 0xFFB84A8A, 0xFF3A9A7A};
                PhoneUI.round(g, tx, ty, tw, h - 25, 5, PhoneUI.alpha(tones[k], .55f));
                PhoneUI.icon(g, "photo", tx + tw / 2f - 5, ty + (h - 25) / 2f - 5, 10);
            }
        }
    }

    // ---------------------------------------------------------------- the open app

    private void appLayer(GuiGraphics g, int mx, int my, float pt, float t) {
        PhoneApp a = app != null ? app : closing;
        if (a == null) {
            return;
        }
        float e = app != null ? t : 1 - t;
        if (app == null && t >= 1) {
            closing = null;
            return;
        }
        // The app's card grows from its icon to the whole screen
        float x0 = Mth.lerp(e, fromX - 15, SX), y0 = Mth.lerp(e, fromY - 15, SY);
        float x1 = Mth.lerp(e, fromX + 15, SX + SW), y1 = Mth.lerp(e, fromY + 15, SY + SH);
        int r = (int) Mth.lerp(e, 9, SR);
        if (!a.seeThrough()) {
            int[] bg = a.background();
            PhoneUI.gradient(g, (int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0), r, bg[0], bg[1]);
        }
        if (e < .35f) {
            PhoneUI.icon(g, a.id, (x0 + x1) / 2 - 12, (y0 + y1) / 2 - 12, 24);
            return;
        }
        clip(g, x0, y0, x1, y1);
        if (app != null) {
            a.render(g, mx, my, pt);
        } else {
            a.render(g, -100, -100, pt);
        }
        g.disableScissor();
    }

    // ---------------------------------------------------------------- status bar, island

    private void statusBar(GuiGraphics g, long now, boolean overWorld) {
        int color = 0xFFFFFFFF;
        if (overWorld) {
            PhoneUI.round(g, SX, SY, SW, 18, 2, 0x55000000);
        }
        PhoneUI.text(g, PhoneClient.clock(), SX + 17, SY + 7, color, .75f);
        // The "signal": the player's mana, in four bars
        float mana = PhoneClient.manaFraction();
        int bx = SX + SW - 44;
        for (int k = 0; k < 4; k++) {
            int h = 2 + k * 1;
            g.fill(bx + k * 3, SY + 12 - h, bx + k * 3 + 2, SY + 12, mana > k / 4f + .01f ? color : 0x55FFFFFF);
        }
        PhoneUI.text(g, "ARC", bx + 13, SY + 7, color, .5f);
        // The "battery": the player's health
        float hp = PhoneClient.healthFraction();
        int bat = SX + SW - 24;
        PhoneUI.round(g, bat, SY + 6, 13, 7, 2, 0x88FFFFFF);
        PhoneUI.round(g, bat + 1, SY + 7, Math.max(1, (int) (11 * hp)), 5, 1, hp < .25f ? 0xFFFF4A5A : hp < .5f ? 0xFFFFD050 : 0xFFFFFFFF);
        g.fill(bat + 13, SY + 8, bat + 14, SY + 11, 0x88FFFFFF);
    }

    /**
     * The rune island: a black pill at the top of the screen, a thin ring of shifting colour round it; when a message
     * (or a post, or emeralds) comes, it springs open to show it, then closes again.
     */
    private void island(GuiGraphics g, int mx, int my, long now) {
        PhoneClient.Note n = PhoneClient.fresh();
        float open = n == null ? 0 : PhoneUI.spring((now - n.shownAt()) / 420f) * (1 - PhoneUI.ease((now - n.shownAt() - 3600) / 300f));
        open = Math.max(0, open);
        float w = Mth.lerp(open, 46, SW - 16), h = Mth.lerp(open, 12, 34);
        float x = SX + SW / 2f - w / 2, y = SY + 4;
        float hue = (now % 4000) / 4000f;
        int ring = PhoneUI.mix(PhoneUI.mix(0xFFB98CFF, 0xFFFF7AD9, Mth.sin(hue * Mth.TWO_PI) * .5f + .5f), 0xFF7CE0FF,
                Mth.sin(hue * Mth.TWO_PI + 2) * .5f + .5f);
        PhoneUI.round(g, (int) x - 1, (int) y - 1, (int) w + 2, (int) h + 2, (int) (h / 2) + 1, PhoneUI.alpha(ring, .55f + .35f * open));
        PhoneUI.round(g, (int) x, (int) y, (int) w, (int) h, (int) (h / 2), 0xFF000000);
        // The camera's lens, a violet glint
        PhoneUI.round(g, (int) (x + w - 10), (int) (y + h / 2 - 2.5f), 5, 5, 2, 0xFF1A1030);
        PhoneUI.glow(g, x + w - 7.5f, y + h / 2, 4, 0x88B98CFF);
        if (n != null && open > .6f) {
            float a = (open - .6f) / .4f;
            PhoneUI.icon(g, n.app(), x + 6, y + 7, 20);
            PhoneUI.text(g, PhoneUI.fit(n.title(), w - 60, .7f), x + 31, y + 8, PhoneUI.alpha(0xFFFFFFFF, a), .7f);
            PhoneUI.text(g, PhoneUI.fit(n.text(), w - 40, .6f), x + 31, y + 19, PhoneUI.alpha(0xFFB8B4C8, a), .6f);
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = lx(mouseX), my = ly(mouseY);
        if (!PhoneUI.in(mx, my, 0, 0, FW, FH)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        PhoneClient.Note n = PhoneClient.fresh();
        if (n != null && PhoneUI.in(mx, my, SX + 8, SY + 4, SW - 16, 34) && !locked) {
            PhoneClient.dismissFresh();
            open(n.app(), SX + SW / 2f, SY + 20);
            return true;
        }
        if (locked) {
            if (PhoneUI.in(mx, my, SX, SY + SH - 52, 44, 34)) {
                unlock();
                open("horloge", SX + 22, SY + SH - 35);
                return true;
            }
            if (PhoneUI.in(mx, my, SX + SW - 44, SY + SH - 52, 44, 34)) {
                unlock();
                open("photo", SX + SW - 22, SY + SH - 35);
                return true;
            }
            if (nowPlayingY >= 0 && PhoneUI.in(mx, my, SX + SW - 26, nowPlayingY + 7, 14, 14)) {
                clickSound();
                MusicPlayer.togglePause();
                return true;
            }
            dragFrom = (float) my;
            return true;
        }
        if (button == 1) {
            back();
            return true;
        }
        if (app != null) {
            if (appProgress() >= 1) {
                if (PhoneUI.in(mx, my, SX + SW / 2f - 30, SY + SH - 10, 60, 10)) {
                    home();
                    return true;
                }
                app.click(mx, my, button);
            }
            return true;
        }
        for (int i = 0; i < 12; i++) {
            float x = SX + 11 + (i % 4) * 37, y = SY + 164 + (i / 4) * 41;
            if (PhoneUI.in(mx, my, x, y, 30, 30)) {
                clickSound();
                open(GRID[i], x + 15, y + 15);
                return true;
            }
        }
        for (int i = 0; i < 4; i++) {
            float x = SX + 14 + i * 37, y = SY + SH - 42;
            if (PhoneUI.in(mx, my, x, y, 30, 30)) {
                clickSound();
                open(DOCK[i], x + 15, y + 15);
                return true;
            }
        }
        if (PhoneUI.in(mx, my, SX + 8, SY + 24, SW - 16, 70)) {
            clickSound();
            open("meteo", SX + SW / 2f, SY + 59);
        } else if (PhoneUI.in(mx, my, SX + 8, SY + 104, SW - 16, 48)) {
            clickSound();
            open("galerie", SX + SW / 2f, SY + 128);
        } else if (PhoneUI.in(mx, my, SX + SW / 2f - 30, SY + 289, 60, 12)) {
            clickSound();
            open("reglages", SX + SW / 2f, SY + 295);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (locked && dragFrom >= 0) {
            drag = Math.max(0, dragFrom - (float) ly(mouseY));
            if (drag > 70) {
                dragFrom = -1;
                unlock();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (locked && dragFrom >= 0) {
            dragFrom = -1;
            if (drag > 25) {
                unlock();
            }
            drag = 0;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double sx, double sy) {
        if (app != null && appProgress() >= 1) {
            return app.scrolled(lx(mouseX), ly(mouseY), sy);
        }
        if (locked && sy < 0) {
            unlock();
        }
        return true;
    }

    private void back() {
        if (app != null) {
            if (!app.back()) {
                home();
            }
        } else if (!locked) {
            locked = true;
            unlockedAt = -1;
        } else {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (app != null && app.key(key, scan, mods)) {
                return true;
            }
            if (app != null) {
                back();
            } else {
                onClose();
            }
            return true;
        }
        if (app != null && appProgress() >= 1 && app.key(key, scan, mods)) {
            return true;
        }
        if (locked && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_UP)) {
            unlock();
            return true;
        }
        if (PhoneClient.OPEN.isActiveAndMatches(InputConstants.getKey(key, scan))) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return app != null && app.typed(c, mods);
    }

    @Override
    public void tick() {
        if (app != null) {
            app.tick();
        }
    }

    @Override
    public void onClose() {
        if (!leaving) {
            leaving = true;
            leftAt = System.currentTimeMillis();
            sound(SoundEvents.AMETHYST_BLOCK_STEP, 1.4f);
        }
    }

    /** The camera: the phone steps away for a moment while the photo is taken. */
    public boolean camera() {
        return camera;
    }

    @Nullable
    public static BlockPos here() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        return p == null ? null : p.blockPosition();
    }
}
