package com.waylays.arcanes.client.music;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The music player: a night-blue window framed in gold. On the left, the library, the favourites and the playlists; in
 * the middle, the tracks of the one chosen (search above them; a star to make one a favourite, a plus to put it in a
 * playlist); below, what plays: its record turning under the tone arm, its title, where it is (the bar can be clicked
 * and dragged), the buttons (shuffle, previous, play or pause, next, repeat), bars dancing to the music, how loud it
 * plays and how far it carries (up to 64 blocks, or for the player's ears only).
 */
public class MusicScreen extends Screen {
    private static final ResourceLocation WINDOW = ArcanesOublies.id("textures/gui/music/window.png");
    private static final ResourceLocation ICONS = ArcanesOublies.id("textures/gui/music/icons.png");
    private static final ResourceLocation RECORD = ArcanesOublies.id("textures/gui/music/record.png");
    private static final ResourceLocation SHINE = ArcanesOublies.id("textures/gui/music/record_shine.png");
    private static final int W = 384, H = 240;

    private static final int GOLD = 0xFFDEB25C, GOLD_LIGHT = 0xFFFFE6A0, TEXT = 0xFFF2ECFF, DIM = 0xFF9A8FB8, FAINT = 0xFF6A6088;
    private static final int VIOLET = 0xFFA06BFF, CYAN = 0xFF6ED2FF;

    /** The icons on their sheet (8 a row). */
    private enum Icon {
        PLAY, PAUSE, PREV, NEXT, SHUFFLE, REPEAT, REPEAT_ONE, STAR, STAR_FULL, PLUS, FOLDER, REFRESH, TRASH, NOTE, SPEAKER, RANGE, SEARCH,
        LIBRARY, CLOSE, PLAYLIST, DISC, MUTED, LOCK, CHECK
    }

    private enum View { LIBRARY, FAVORITES, PLAYLIST }

    // The list's measures
    private static final int LIST_X0 = 110, LIST_X1 = 368, ROWS_Y = 45, ROW = 13, ROWS = 9;
    private static final int SIDE_X0 = 10, SIDE_X1 = 100, PL_Y = 80, PL_ROW = 13, PL_ROWS = 6;
    // The player bar's parts
    private static final int BAR_X0 = 66, BAR_X1 = 264, BAR_Y = 205;
    private static final int SLIDER_X0 = 287, SLIDER_X1 = 351, VOL_Y = 207, RANGE_Y = 221;

    private static View lastView = View.LIBRARY;
    @Nullable
    private static String lastPlaylist;

    private View view = lastView;
    @Nullable
    private String playlist = lastPlaylist;
    private EditBox search;
    private EditBox newPlaylist;
    private int scroll, sideScroll;
    private boolean dragProgress, dragVolume, dragRange;
    private double dragTo;
    /** The track a "put in a playlist" menu is open for, and where. */
    @Nullable
    private MusicLibrary.Track menuFor;
    private int menuX, menuY;
    private float recordAngle;
    private long lastFrame = Util.getMillis();
    private final float[] bars = new float[16];
    @Nullable
    private Component tooltip;

    public MusicScreen() {
        super(Component.translatable("ui.arcanes_oublies.music.title"));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int l = left(), t = top();
        search = new EditBox(font, l + 226, t + 9, 90, 10, Component.translatable("ui.arcanes_oublies.music.search"));
        search.setBordered(false);
        search.setMaxLength(48);
        search.setTextColor(TEXT);
        search.setHint(Component.translatable("ui.arcanes_oublies.music.search").withColor(FAINT));
        search.setResponder(s -> scroll = 0);
        addRenderableWidget(search);
        newPlaylist = new EditBox(font, l + SIDE_X0 + 14, t + PL_Y + 1, 70, 10, Component.translatable("ui.arcanes_oublies.music.new_playlist"));
        newPlaylist.setBordered(false);
        newPlaylist.setMaxLength(32);
        newPlaylist.setTextColor(GOLD_LIGHT);
        newPlaylist.setHint(Component.translatable("ui.arcanes_oublies.music.playlist_name").withColor(FAINT));
        newPlaylist.setVisible(false);
        addRenderableWidget(newPlaylist);
        MusicLibrary.get();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        lastView = view;
        lastPlaylist = playlist;
        super.removed();
    }

    // ---------------------------------------------------------------- what the list shows

    private List<MusicLibrary.Track> tracks() {
        MusicLibrary lib = MusicLibrary.get();
        List<MusicLibrary.Track> base = switch (view) {
            case LIBRARY -> lib.all();
            case FAVORITES -> lib.favorites();
            case PLAYLIST -> playlist != null ? lib.playlist(playlist) : List.of();
        };
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return base;
        }
        List<MusicLibrary.Track> out = new ArrayList<>();
        for (MusicLibrary.Track t : base) {
            if (t.title.toLowerCase(Locale.ROOT).contains(q) || t.artist != null && t.artist.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(t);
            }
        }
        return out;
    }

    private Component viewTitle() {
        return switch (view) {
            case LIBRARY -> Component.translatable("ui.arcanes_oublies.music.library");
            case FAVORITES -> Component.translatable("ui.arcanes_oublies.music.favorites");
            case PLAYLIST -> Component.literal(playlist == null ? "" : playlist);
        };
    }

    // ---------------------------------------------------------------- drawing

    private void icon(GuiGraphics g, Icon icon, int x, int y, int size, int color) {
        int i = icon.ordinal();
        g.setColor((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, (color >>> 24) / 255f);
        g.blit(ICONS, x, y, size, size, (i % 8) * 32, (i / 8) * 32, 32, 32, 256, 96);
        g.setColor(1, 1, 1, 1);
    }

    private static boolean in(double mx, double my, int x0, int y0, int x1, int y1) {
        return mx >= x0 && mx < x1 && my >= y0 && my < y1;
    }

    /** An icon button: brighter under the mouse, its tooltip shown. */
    private void button(GuiGraphics g, Icon icon, int x, int y, int size, int mx, int my, boolean active, Component tip) {
        boolean hover = in(mx, my, x - 1, y - 1, x + size + 1, y + size + 1);
        if (hover) {
            g.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0x30FFFFFF);
            tooltip = tip;
        }
        icon(g, icon, x, y, size, active ? hover ? GOLD_LIGHT : GOLD : hover ? TEXT : DIM);
    }

    private static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private String fit(String s, int w) {
        if (font.width(s) <= w) {
            return s;
        }
        return font.plainSubstrByWidth(s, w - font.width("…")) + "…";
    }

    private void small(GuiGraphics g, String s, float x, float y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(.75f, .75f, 1);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
        super.renderBackground(g, mx, my, partialTick);
        tooltip = null;
        long now = Util.getMillis();
        float dt = Math.min(.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        int l = left(), t = top();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(WINDOW, l, t, W, H, 0, 0, W * 2, H * 2, W * 2, H * 2);
        g.pose().pushPose();
        g.pose().translate(l, t, 0);
        int lx = mx - l, ly = my - t;
        title(g, lx, ly);
        side(g, lx, ly);
        list(g, lx, ly);
        playerBar(g, lx, ly, dt);
        g.pose().popPose();
    }

    private void title(GuiGraphics g, int mx, int my) {
        icon(g, Icon.NOTE, 12, 7, 12, GOLD);
        g.drawString(font, title, 28, 9, GOLD_LIGHT, true);
        // The search field's pane
        g.fill(210, 6, 320, 20, 0x60000000);
        g.renderOutline(210, 6, 110, 14, 0x80DEB25C);
        icon(g, Icon.SEARCH, 213, 8, 10, search.isFocused() ? GOLD_LIGHT : DIM);
        button(g, Icon.FOLDER, 326, 7, 11, mx, my, false, Component.translatable("ui.arcanes_oublies.music.open_folder"));
        button(g, Icon.REFRESH, 342, 7, 11, mx, my, false, Component.translatable("ui.arcanes_oublies.music.refresh"));
        button(g, Icon.CLOSE, 361, 7, 11, mx, my, false, Component.translatable("gui.done"));
    }

    private void side(GuiGraphics g, int mx, int my) {
        MusicLibrary lib = MusicLibrary.get();
        sideEntry(g, mx, my, 32, Icon.LIBRARY, Component.translatable("ui.arcanes_oublies.music.library"), lib.all().size(), view == View.LIBRARY);
        sideEntry(g, mx, my, 47, Icon.STAR_FULL, Component.translatable("ui.arcanes_oublies.music.favorites"), lib.favorites().size(), view == View.FAVORITES);
        // The playlists' heading, and the button to make one
        small(g, Component.translatable("ui.arcanes_oublies.music.playlists").getString().toUpperCase(Locale.ROOT), 14, 67, FAINT);
        g.fill(14, 74, SIDE_X1 - 12, 75, 0x40DEB25C);
        button(g, Icon.PLUS, SIDE_X1 - 10, 64, 9, mx, my, false, Component.translatable("ui.arcanes_oublies.music.new_playlist"));
        List<String> names = lib.playlistNames();
        int y = PL_Y;
        if (newPlaylist.isVisible()) {
            g.fill(SIDE_X0, y - 1, SIDE_X1, y + PL_ROW - 2, 0x40A06BFF);
            icon(g, Icon.PLAYLIST, SIDE_X0 + 2, y + 1, 9, GOLD_LIGHT);
            y += PL_ROW;
        }
        int shown = PL_ROWS - (newPlaylist.isVisible() ? 1 : 0);
        sideScroll = Mth.clamp(sideScroll, 0, Math.max(0, names.size() - shown));
        for (int i = sideScroll; i < names.size() && i < sideScroll + shown; i++, y += PL_ROW) {
            String name = names.get(i);
            boolean chosen = view == View.PLAYLIST && name.equals(playlist);
            boolean hover = in(mx, my, SIDE_X0, y - 1, SIDE_X1, y + PL_ROW - 2);
            if (chosen) {
                g.fillGradient(SIDE_X0, y - 1, SIDE_X1, y + PL_ROW - 2, 0x60A06BFF, 0x30A06BFF);
                g.fill(SIDE_X0, y - 1, SIDE_X0 + 2, y + PL_ROW - 2, GOLD);
            } else if (hover) {
                g.fill(SIDE_X0, y - 1, SIDE_X1, y + PL_ROW - 2, 0x24FFFFFF);
            }
            icon(g, Icon.PLAYLIST, SIDE_X0 + 3, y + 1, 9, chosen ? GOLD_LIGHT : DIM);
            g.drawString(font, fit(name, hover ? 54 : 62), SIDE_X0 + 15, y + 1, chosen ? GOLD_LIGHT : TEXT, false);
            if (hover) {
                button(g, Icon.TRASH, SIDE_X1 - 11, y, 9, mx, my, false, Component.translatable("ui.arcanes_oublies.music.delete_playlist"));
            } else {
                String n = String.valueOf(lib.playlistSize(name));
                small(g, n, SIDE_X1 - 3 - font.width(n) * .75f, y + 2, FAINT);
            }
        }
        if (names.isEmpty() && !newPlaylist.isVisible()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("ui.arcanes_oublies.music.no_playlist"), 112);
            int ly = PL_Y + 2;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                g.pose().pushPose();
                g.pose().translate(SIDE_X0 + 3, ly, 0);
                g.pose().scale(.75f, .75f, 1);
                g.drawString(font, line, 0, 0, FAINT, false);
                g.pose().popPose();
                ly += 7;
            }
        }
        // Who plays music nearby
        var heard = RemoteMusic.heard();
        if (!heard.isEmpty() && minecraft != null && minecraft.level != null) {
            int hy = 160;
            var e = heard.entrySet().iterator().next();
            var who = minecraft.level.getEntity(e.getKey());
            icon(g, Icon.RANGE, SIDE_X0 + 2, hy, 8, CYAN);
            small(g, fit((who != null ? who.getName().getString() + " : " : "") + e.getValue(), 100), SIDE_X0 + 12, hy + 1.5f, CYAN);
        }
    }

    private void sideEntry(GuiGraphics g, int mx, int my, int y, Icon icon, Component name, int count, boolean chosen) {
        boolean hover = in(mx, my, SIDE_X0, y - 2, SIDE_X1, y + 12);
        if (chosen) {
            g.fillGradient(SIDE_X0, y - 2, SIDE_X1, y + 12, 0x60A06BFF, 0x30A06BFF);
            g.fill(SIDE_X0, y - 2, SIDE_X0 + 2, y + 12, GOLD);
        } else if (hover) {
            g.fill(SIDE_X0, y - 2, SIDE_X1, y + 12, 0x24FFFFFF);
        }
        icon(g, icon, SIDE_X0 + 3, y, 10, chosen ? GOLD_LIGHT : hover ? TEXT : DIM);
        g.drawString(font, name, SIDE_X0 + 16, y + 1, chosen ? GOLD_LIGHT : TEXT, false);
        String n = String.valueOf(count);
        small(g, n, SIDE_X1 - 3 - font.width(n) * .75f, y + 2.5f, FAINT);
    }

    private void list(GuiGraphics g, int mx, int my) {
        List<MusicLibrary.Track> tracks = tracks();
        MusicLibrary lib = MusicLibrary.get();
        // The heading: what is shown, how many, how long it all lasts
        Component head = viewTitle();
        g.drawString(font, head, LIST_X0 + 4, 32, GOLD_LIGHT, true);
        long total = 0;
        for (MusicLibrary.Track tr : tracks) {
            total += tr.lengthMs;
        }
        String info = Component.translatable("ui.arcanes_oublies.music.count", tracks.size()).getString() + (total > 0 ? "  ·  " + time(total) : "");
        small(g, info, LIST_X0 + 8 + font.width(head), 33.5f, DIM);
        if (!tracks.isEmpty()) {
            button(g, Icon.PLAY, LIST_X1 - 10, 31, 10, mx, my, true, Component.translatable("ui.arcanes_oublies.music.play_all"));
        }
        g.fill(LIST_X0 + 2, 42, LIST_X1 - 2, 43, 0x50DEB25C);
        scroll = Mth.clamp(scroll, 0, Math.max(0, tracks.size() - ROWS));
        MusicLibrary.Track playing = MusicPlayer.current();
        long t = Util.getMillis();
        for (int i = scroll; i < tracks.size() && i < scroll + ROWS; i++) {
            MusicLibrary.Track tr = tracks.get(i);
            int y = ROWS_Y + (i - scroll) * ROW;
            boolean hover = menuFor == null && in(mx, my, LIST_X0, y - 1, LIST_X1 - 4, y + ROW - 1);
            boolean now = tr == playing;
            if (now) {
                g.fillGradient(LIST_X0, y - 1, LIST_X1 - 4, y + ROW - 1, 0x55A06BFF, 0x2A6ED2FF);
                g.fill(LIST_X0, y - 1, LIST_X0 + 2, y + ROW - 1, GOLD);
            } else if (hover) {
                g.fill(LIST_X0, y - 1, LIST_X1 - 4, y + ROW - 1, 0x20FFFFFF);
            } else if (i % 2 == 1) {
                g.fill(LIST_X0, y - 1, LIST_X1 - 4, y + ROW - 1, 0x0CFFFFFF);
            }
            // Favourite
            boolean fav = lib.favorite(tr);
            boolean starHover = in(mx, my, LIST_X0 + 3, y, LIST_X0 + 13, y + 10);
            icon(g, fav ? Icon.STAR_FULL : Icon.STAR, LIST_X0 + 4, y + 1, 9, fav ? GOLD : starHover ? TEXT : FAINT);
            if (starHover) {
                tooltip = Component.translatable(fav ? "ui.arcanes_oublies.music.unfavorite" : "ui.arcanes_oublies.music.favorite");
            }
            // Its number, or bars dancing if it plays
            if (now && MusicPlayer.playing()) {
                for (int b = 0; b < 3; b++) {
                    float h = 2 + 6 * (.5f + .5f * Mth.sin(t / 120f + b * 2.1f)) * (.3f + .7f * MusicPlayer.level(b * 60L));
                    g.fill(LIST_X0 + 17 + b * 3, (int) (y + 10 - h), LIST_X0 + 19 + b * 3, y + 10, GOLD_LIGHT);
                }
            } else {
                String n = String.valueOf(i + 1);
                small(g, n, LIST_X0 + 26 - font.width(n) * .75f, y + 3, FAINT);
            }
            // Title, and who made it
            int tx = LIST_X0 + 30;
            int maxW = LIST_X1 - 50 - tx;
            String title = fit(tr.title, maxW);
            g.drawString(font, title, tx, y + 2, now ? GOLD_LIGHT : TEXT, false);
            int aw = maxW - font.width(title) - 8;
            if (tr.artist != null && aw > 20) {
                small(g, fit(tr.artist, (int) (aw / .75f)), tx + font.width(title) + 6, y + 3.5f, DIM);
            } else if (tr.builtIn() && aw > 20) {
                icon(g, Icon.DISC, tx + font.width(title) + 5, y + 2, 7, FAINT);
            }
            // Its length
            if (tr.lengthMs > 0) {
                String len = time(tr.lengthMs);
                small(g, len, LIST_X1 - 22 - font.width(len) * .75f, y + 3.5f, DIM);
            }
            // Put in a playlist, or take out of this one
            if (hover || menuFor == tr) {
                if (view == View.PLAYLIST) {
                    button(g, Icon.CLOSE, LIST_X1 - 17, y + 1, 9, mx, my, false, Component.translatable("ui.arcanes_oublies.music.remove_from_playlist"));
                } else {
                    button(g, Icon.PLUS, LIST_X1 - 17, y + 1, 9, mx, my, false, Component.translatable("ui.arcanes_oublies.music.add_to_playlist"));
                }
            }
        }
        // The scroll bar
        if (tracks.size() > ROWS) {
            int h = ROW * ROWS;
            int knob = Math.max(10, h * ROWS / tracks.size());
            int ky = ROWS_Y + (h - knob) * scroll / Math.max(1, tracks.size() - ROWS);
            g.fill(LIST_X1 - 2, ROWS_Y, LIST_X1, ROWS_Y + h, 0x30FFFFFF);
            g.fill(LIST_X1 - 2, ky, LIST_X1, ky + knob, GOLD);
        }
        // Nothing yet: how to add music
        if (tracks.isEmpty() || view == View.LIBRARY && lib.fileCount() == 0 && (search == null || search.getValue().isEmpty())) {
            int y = ROWS_Y + Math.min(tracks.size(), ROWS) * ROW + 8;
            Component hint = tracks.isEmpty() && view != View.LIBRARY ? Component.translatable(view == View.FAVORITES
                    ? "ui.arcanes_oublies.music.no_favorite" : "ui.arcanes_oublies.music.empty_playlist")
                    : Component.translatable("ui.arcanes_oublies.music.how_to_add");
            for (net.minecraft.util.FormattedCharSequence line : font.split(hint, LIST_X1 - LIST_X0 - 30)) {
                g.drawString(font, line, LIST_X0 + 15, y, DIM, false);
                y += 10;
            }
        }
    }

    private void playerBar(GuiGraphics g, int mx, int my, float dt) {
        MusicLibrary lib = MusicLibrary.get();
        MusicLibrary.Track tr = MusicPlayer.current();
        boolean playing = MusicPlayer.playing() && !MusicPlayer.loading();
        // The record, turning while it plays, the light on it still
        if (playing) {
            recordAngle = (recordAngle + dt * 200) % 360;
        }
        int cx = 37, cy = 205;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(recordAngle));
        g.blit(RECORD, -23, -23, 46, 46, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
        g.blit(SHINE, cx - 23, cy - 23, 46, 46, 0, 0, 128, 128, 128, 128);
        // The tone arm: on the record while it plays, lifted off when not
        float arm = playing ? 28 : 8;
        g.pose().pushPose();
        g.pose().translate(58, 184, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(arm + 70));
        g.fill(-1, -1, 22, 1, GOLD);
        g.fill(18, -2, 25, 2, GOLD_LIGHT);
        g.pose().popPose();
        g.fill(55, 181, 61, 187, GOLD);
        g.fill(56, 182, 60, 186, 0xFF3A2A60);
        // The title, scrolling along if too long; who made it below
        String name = tr == null ? Component.translatable("ui.arcanes_oublies.music.nothing").getString() : tr.title;
        int w = font.width(name), room = BAR_X1 - BAR_X0;
        if (w > room) {
            float shift = (Util.getMillis() / 40f) % (w + 40);
            g.enableScissor(left() + BAR_X0, top() + 180, left() + BAR_X1, top() + 192);
            g.drawString(font, name, (int) (BAR_X0 - shift), 182, tr == null ? DIM : GOLD_LIGHT, true);
            g.drawString(font, name, (int) (BAR_X0 - shift + w + 40), 182, tr == null ? DIM : GOLD_LIGHT, true);
            g.disableScissor();
        } else {
            g.drawString(font, name, BAR_X0, 182, tr == null ? DIM : GOLD_LIGHT, true);
        }
        String sub = tr == null ? Component.translatable("ui.arcanes_oublies.music.pick_one").getString()
                : MusicPlayer.loading() ? Component.translatable("ui.arcanes_oublies.music.loading").getString()
                : (tr.artist != null ? tr.artist + "  ·  " : "") + tr.ext.toUpperCase(Locale.ROOT)
                + (MusicPlayer.paused() ? "  ·  " + Component.translatable("ui.arcanes_oublies.music.paused").getString() : "");
        small(g, fit(sub, (int) (room / .75f)), BAR_X0, 193, DIM);
        // Where it is: the bar (clicked or dragged to go elsewhere), the times
        long len = MusicPlayer.lengthMs();
        double pos = dragProgress ? dragTo : MusicPlayer.positionMs();
        float k = len > 0 ? (float) Mth.clamp(pos / len, 0, 1) : 0;
        g.fill(BAR_X0, BAR_Y, BAR_X1, BAR_Y + 3, 0x50FFFFFF);
        int fx = BAR_X0 + (int) ((BAR_X1 - BAR_X0) * k);
        g.fillGradient(BAR_X0, BAR_Y, fx, BAR_Y + 3, VIOLET, CYAN);
        boolean barHover = in(mx, my, BAR_X0 - 2, BAR_Y - 3, BAR_X1 + 2, BAR_Y + 6);
        if (tr != null) {
            int r = barHover || dragProgress ? 3 : 2;
            g.fill(fx - r, BAR_Y + 1 - r, fx + r + 1, BAR_Y + 2 + r, GOLD_LIGHT);
        }
        small(g, time((long) pos), BAR_X0, BAR_Y + 6, DIM);
        String lenS = len > 0 ? time(len) : "--:--";
        small(g, lenS, BAR_X1 - font.width(lenS) * .75f, BAR_Y + 6, DIM);
        // The buttons
        int c = (BAR_X0 + BAR_X1) / 2, by = 216;
        button(g, Icon.SHUFFLE, c - 50, by + 2, 10, mx, my, lib.shuffle, Component.translatable(lib.shuffle ? "ui.arcanes_oublies.music.shuffle_on"
                : "ui.arcanes_oublies.music.shuffle_off"));
        button(g, Icon.PREV, c - 27, by + 2, 10, mx, my, false, Component.translatable("ui.arcanes_oublies.music.previous"));
        boolean mainHover = in(mx, my, c - 8, by - 1, c + 8, by + 15);
        g.fill(c - 8, by - 1, c + 8, by + 15, mainHover ? 0x60DEB25C : 0x40DEB25C);
        g.renderOutline(c - 8, by - 1, 16, 16, GOLD);
        icon(g, MusicPlayer.playing() ? Icon.PAUSE : Icon.PLAY, c - 6, by + 1, 12, mainHover ? 0xFFFFFFFF : GOLD_LIGHT);
        if (mainHover) {
            tooltip = Component.translatable(MusicPlayer.playing() ? "ui.arcanes_oublies.music.pause" : "ui.arcanes_oublies.music.play");
        }
        button(g, Icon.NEXT, c + 17, by + 2, 10, mx, my, false, Component.translatable("ui.arcanes_oublies.music.next"));
        Icon rep = lib.repeat == MusicLibrary.Repeat.ONE ? Icon.REPEAT_ONE : Icon.REPEAT;
        button(g, rep, c + 40, by + 2, 10, mx, my, lib.repeat != MusicLibrary.Repeat.OFF,
                Component.translatable("ui.arcanes_oublies.music.repeat_" + lib.repeat.name().toLowerCase(Locale.ROOT)));
        // Bars dancing to the music
        for (int b = 0; b < bars.length; b++) {
            float level = MusicPlayer.level(b * 25L);
            float wobble = .55f + .45f * Mth.sin(Util.getMillis() / (90f + b * 7) + b * 1.7f);
            float target = playing ? Mth.clamp(level * (.6f + .8f * wobble) * (1.15f - Math.abs(b - 7.5f) / 18f), 0, 1) : 0;
            bars[b] += (target - bars[b]) * Math.min(1, dt * 14);
            int x = 274 + b * 6, h = 1 + (int) (bars[b] * 16);
            g.fillGradient(x, 198 - h, x + 4, 198, CYAN, VIOLET);
            g.fill(x, 197 - h, x + 4, 198 - h, GOLD_LIGHT);
        }
        // How loud, how far
        icon(g, lib.volume <= 0 ? Icon.MUTED : Icon.SPEAKER, 273, VOL_Y - 4, 10, DIM);
        slider(g, mx, my, VOL_Y, dragVolume ? (float) dragTo : lib.volume);
        String vol = Math.round((dragVolume ? dragTo : lib.volume) * 100) + "%";
        small(g, vol, 356, VOL_Y - 2, DIM);
        if (in(mx, my, 272, VOL_Y - 5, 372, VOL_Y + 5)) {
            tooltip = Component.translatable("ui.arcanes_oublies.music.volume");
        }
        int range = dragRange ? MusicSoundInstance.RANGES[(int) Math.round(dragTo)] : lib.range;
        int step = 0;
        for (int i = 0; i < MusicSoundInstance.RANGES.length; i++) {
            if (MusicSoundInstance.RANGES[i] == range) {
                step = i;
            }
        }
        icon(g, range <= 0 ? Icon.LOCK : Icon.RANGE, 273, RANGE_Y - 4, 10, range <= 0 ? GOLD : DIM);
        slider(g, mx, my, RANGE_Y, step / (float) (MusicSoundInstance.RANGES.length - 1));
        for (int i = 0; i < MusicSoundInstance.RANGES.length; i++) {
            int x = SLIDER_X0 + (SLIDER_X1 - SLIDER_X0) * i / (MusicSoundInstance.RANGES.length - 1);
            g.fill(x, RANGE_Y + 2, x + 1, RANGE_Y + 4, 0x60FFFFFF);
        }
        String rl = range <= 0 ? Component.translatable("ui.arcanes_oublies.music.range_self").getString()
                : Component.translatable("ui.arcanes_oublies.music.blocks", range).getString();
        small(g, rl, 356, RANGE_Y - 2, range <= 0 ? GOLD : DIM);
        if (in(mx, my, 272, RANGE_Y - 5, 372, RANGE_Y + 5)) {
            tooltip = Component.translatable(range <= 0 ? "ui.arcanes_oublies.music.range_self_tip" : "ui.arcanes_oublies.music.range_tip", range);
        }
    }

    private void slider(GuiGraphics g, int mx, int my, int y, float value) {
        g.fill(SLIDER_X0, y - 1, SLIDER_X1, y + 1, 0x50FFFFFF);
        int x = SLIDER_X0 + (int) ((SLIDER_X1 - SLIDER_X0) * Mth.clamp(value, 0, 1));
        g.fillGradient(SLIDER_X0, y - 1, x, y + 1, VIOLET, CYAN);
        boolean hover = in(mx, my, SLIDER_X0 - 3, y - 4, SLIDER_X1 + 3, y + 4);
        int r = hover ? 3 : 2;
        g.fill(x - r, y - r, x + r + 1, y + r + 1, GOLD_LIGHT);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        int l = left(), t = top();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        if (menuFor != null) {
            playlistMenu(g, mx, my);
        }
        g.pose().popPose();
        if (tooltip != null && menuFor == null) {
            g.renderTooltip(font, tooltip, mx, my);
        }
    }

    /** The menu of playlists a track can be put in, and a way to make a new one with it. */
    private void playlistMenu(GuiGraphics g, int mx, int my) {
        List<String> names = MusicLibrary.get().playlistNames();
        int w = 110, h = 14 + names.size() * 12 + 4;
        int x = menuX, y = menuY;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFFDEB25C);
        g.fillGradient(x, y, x + w, y + h, 0xF0241642, 0xF00E0A22);
        int ry = y + 3;
        for (String name : names) {
            boolean hover = in(mx, my, x, ry - 1, x + w, ry + 11);
            if (hover) {
                g.fill(x + 1, ry - 1, x + w - 1, ry + 11, 0x40A06BFF);
            }
            boolean has = MusicLibrary.get().inPlaylist(name, menuFor);
            icon(g, has ? Icon.CHECK : Icon.PLAYLIST, x + 3, ry + 1, 8, has ? GOLD : DIM);
            g.drawString(font, fit(name, w - 18), x + 14, ry + 1, hover ? GOLD_LIGHT : TEXT, false);
            ry += 12;
        }
        boolean hover = in(mx, my, x, ry - 1, x + w, ry + 11);
        if (hover) {
            g.fill(x + 1, ry - 1, x + w - 1, ry + 11, 0x40A06BFF);
        }
        icon(g, Icon.PLUS, x + 3, ry + 1, 8, GOLD);
        g.drawString(font, Component.translatable("ui.arcanes_oublies.music.new_playlist"), x + 14, ry + 1, hover ? GOLD_LIGHT : GOLD, false);
    }

    // ---------------------------------------------------------------- input

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
    }

    @Override
    public boolean mouseClicked(double gx, double gy, int button) {
        double mx = gx - left(), my = gy - top();
        if (menuFor != null) {
            menuClick(gx, gy);
            return true;
        }
        if (super.mouseClicked(gx, gy, button)) {
            return true;
        }
        search.setFocused(in(mx, my, 210, 6, 320, 20));
        if (search.isFocused()) {
            setFocused(search);
            return true;
        }
        if (newPlaylist.isVisible() && !in(mx, my, SIDE_X0, PL_Y - 1, SIDE_X1, PL_Y + PL_ROW)) {
            finishPlaylist();
        }
        MusicLibrary lib = MusicLibrary.get();
        // Title bar
        if (in(mx, my, 325, 6, 338, 19)) {
            click();
            MusicLibrary.openFolder();
            return true;
        }
        if (in(mx, my, 341, 6, 354, 19)) {
            click();
            lib.scan();
            return true;
        }
        if (in(mx, my, 360, 6, 373, 19)) {
            click();
            onClose();
            return true;
        }
        // The side list
        if (in(mx, my, SIDE_X0, 30, SIDE_X1, 44)) {
            show(View.LIBRARY, null);
            return true;
        }
        if (in(mx, my, SIDE_X0, 45, SIDE_X1, 59)) {
            show(View.FAVORITES, null);
            return true;
        }
        if (in(mx, my, SIDE_X1 - 11, 63, SIDE_X1, 74)) {
            click();
            startPlaylist();
            return true;
        }
        List<String> names = lib.playlistNames();
        int y = PL_Y + (newPlaylist.isVisible() ? PL_ROW : 0);
        int shown = PL_ROWS - (newPlaylist.isVisible() ? 1 : 0);
        for (int i = sideScroll; i < names.size() && i < sideScroll + shown; i++, y += PL_ROW) {
            if (in(mx, my, SIDE_X0, y - 1, SIDE_X1, y + PL_ROW - 2)) {
                String name = names.get(i);
                if (in(mx, my, SIDE_X1 - 12, y - 1, SIDE_X1, y + PL_ROW - 2)) {
                    click();
                    lib.deletePlaylist(name);
                    if (name.equals(playlist)) {
                        show(View.LIBRARY, null);
                    }
                } else {
                    show(View.PLAYLIST, name);
                }
                return true;
            }
        }
        // The list
        List<MusicLibrary.Track> tracks = tracks();
        if (!tracks.isEmpty() && in(mx, my, LIST_X1 - 11, 30, LIST_X1, 42)) {
            click();
            MusicLibrary.Track first = lib.shuffle ? tracks.get(new java.util.Random().nextInt(tracks.size())) : tracks.get(0);
            MusicPlayer.play(first, tracks);
            return true;
        }
        for (int i = scroll; i < tracks.size() && i < scroll + ROWS; i++) {
            int ry = ROWS_Y + (i - scroll) * ROW;
            if (!in(mx, my, LIST_X0, ry - 1, LIST_X1 - 4, ry + ROW - 1)) {
                continue;
            }
            MusicLibrary.Track tr = tracks.get(i);
            if (in(mx, my, LIST_X0 + 3, ry, LIST_X0 + 13, ry + 10)) {
                click();
                lib.toggleFavorite(tr);
            } else if (in(mx, my, LIST_X1 - 18, ry, LIST_X1 - 7, ry + 11)) {
                click();
                if (view == View.PLAYLIST && playlist != null) {
                    lib.removeFromPlaylist(playlist, tr);
                } else {
                    menuFor = tr;
                    menuX = (int) Math.min(gx, width - 114);
                    menuY = (int) Math.min(gy + 4, height - 20 - lib.playlistNames().size() * 12);
                }
            } else {
                click();
                if (tr == MusicPlayer.current() && !MusicPlayer.loading()) {
                    MusicPlayer.togglePause();
                } else {
                    MusicPlayer.play(tr, tracks);
                }
            }
            return true;
        }
        // The player bar
        int c = (BAR_X0 + BAR_X1) / 2, by = 216;
        if (in(mx, my, BAR_X0 - 2, BAR_Y - 3, BAR_X1 + 2, BAR_Y + 6) && MusicPlayer.current() != null) {
            dragProgress = true;
            dragTo = progressAt(mx);
            return true;
        }
        if (in(mx, my, c - 51, by, c - 39, by + 14)) {
            click();
            MusicPlayer.setShuffle(!lib.shuffle);
            return true;
        }
        if (in(mx, my, c - 28, by, c - 16, by + 14)) {
            click();
            MusicPlayer.previous();
            return true;
        }
        if (in(mx, my, c - 8, by - 1, c + 8, by + 15)) {
            click();
            if (MusicPlayer.current() == null) {
                if (!tracks.isEmpty()) {
                    MusicPlayer.play(tracks.get(0), tracks);
                }
            } else {
                MusicPlayer.togglePause();
            }
            return true;
        }
        if (in(mx, my, c + 16, by, c + 28, by + 14)) {
            click();
            MusicPlayer.next();
            return true;
        }
        if (in(mx, my, c + 39, by, c + 51, by + 14)) {
            click();
            MusicLibrary.Repeat[] all = MusicLibrary.Repeat.values();
            MusicPlayer.setRepeat(all[(lib.repeat.ordinal() + 1) % all.length]);
            return true;
        }
        if (in(mx, my, SLIDER_X0 - 4, VOL_Y - 5, SLIDER_X1 + 4, VOL_Y + 5)) {
            dragVolume = true;
            dragTo = sliderAt(mx);
            MusicPlayer.setVolume((float) dragTo);
            return true;
        }
        if (in(mx, my, SLIDER_X0 - 4, RANGE_Y - 5, SLIDER_X1 + 4, RANGE_Y + 5)) {
            dragRange = true;
            dragTo = Math.round(sliderAt(mx) * (MusicSoundInstance.RANGES.length - 1));
            return true;
        }
        return false;
    }

    private void menuClick(double gx, double gy) {
        MusicLibrary lib = MusicLibrary.get();
        List<String> names = lib.playlistNames();
        int w = 110;
        int ry = menuY + 3;
        MusicLibrary.Track tr = menuFor;
        menuFor = null;
        if (tr == null || !in(gx, gy, menuX, menuY, menuX + w, menuY + 18 + names.size() * 12)) {
            return;
        }
        for (String name : names) {
            if (in(gx, gy, menuX, ry - 1, menuX + w, ry + 11)) {
                click();
                if (lib.inPlaylist(name, tr)) {
                    lib.removeFromPlaylist(name, tr);
                } else {
                    lib.addToPlaylist(name, tr);
                }
                return;
            }
            ry += 12;
        }
        if (in(gx, gy, menuX, ry - 1, menuX + w, ry + 11)) {
            click();
            String made = lib.createPlaylist(Component.translatable("ui.arcanes_oublies.music.default_playlist").getString());
            lib.addToPlaylist(made, tr);
        }
    }

    private double progressAt(double mx) {
        return Mth.clamp((mx - BAR_X0) / (BAR_X1 - BAR_X0), 0, 1) * MusicPlayer.lengthMs();
    }

    private double sliderAt(double mx) {
        return Mth.clamp((mx - SLIDER_X0) / (double) (SLIDER_X1 - SLIDER_X0), 0, 1);
    }

    @Override
    public boolean mouseDragged(double gx, double gy, int button, double dx, double dy) {
        double mx = gx - left();
        if (dragProgress) {
            dragTo = progressAt(mx);
            return true;
        }
        if (dragVolume) {
            dragTo = sliderAt(mx);
            MusicPlayer.setVolume((float) dragTo);
            return true;
        }
        if (dragRange) {
            dragTo = Math.round(sliderAt(mx) * (MusicSoundInstance.RANGES.length - 1));
            return true;
        }
        return super.mouseDragged(gx, gy, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double gx, double gy, int button) {
        if (dragProgress) {
            dragProgress = false;
            MusicPlayer.seek(dragTo);
            return true;
        }
        if (dragVolume) {
            dragVolume = false;
            MusicPlayer.volumeSettled();
            return true;
        }
        if (dragRange) {
            dragRange = false;
            click();
            MusicPlayer.setRange(MusicSoundInstance.RANGES[(int) Mth.clamp(Math.round(dragTo), 0, MusicSoundInstance.RANGES.length - 1)]);
            return true;
        }
        return super.mouseReleased(gx, gy, button);
    }

    @Override
    public boolean mouseScrolled(double gx, double gy, double scrollX, double scrollY) {
        double mx = gx - left(), my = gy - top();
        int d = scrollY > 0 ? -1 : scrollY < 0 ? 1 : 0;
        if (in(mx, my, LIST_X0, ROWS_Y, LIST_X1, ROWS_Y + ROW * ROWS)) {
            scroll += d;
            return true;
        }
        if (in(mx, my, SIDE_X0, PL_Y, SIDE_X1, PL_Y + PL_ROW * PL_ROWS)) {
            sideScroll += d;
            return true;
        }
        if (in(mx, my, 272, VOL_Y - 6, 372, VOL_Y + 6)) {
            MusicPlayer.setVolume(MusicLibrary.get().volume - d * .05f);
            MusicPlayer.volumeSettled();
            return true;
        }
        return super.mouseScrolled(gx, gy, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (menuFor != null && key == GLFW.GLFW_KEY_ESCAPE) {
            menuFor = null;
            return true;
        }
        if (newPlaylist.isVisible() && newPlaylist.isFocused()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                finishPlaylist();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                newPlaylist.setValue("");
                newPlaylist.setVisible(false);
                return true;
            }
        }
        if (!search.isFocused() && !newPlaylist.isFocused() && key == GLFW.GLFW_KEY_SPACE) {
            MusicPlayer.togglePause();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void show(View v, @Nullable String name) {
        click();
        view = v;
        playlist = name;
        scroll = 0;
    }

    private void startPlaylist() {
        newPlaylist.setVisible(true);
        newPlaylist.setValue("");
        setFocused(newPlaylist);
        newPlaylist.setFocused(true);
        search.setFocused(false);
    }

    private void finishPlaylist() {
        String name = newPlaylist.getValue().trim();
        newPlaylist.setVisible(false);
        newPlaylist.setFocused(false);
        if (!name.isEmpty()) {
            String made = MusicLibrary.get().createPlaylist(name);
            show(View.PLAYLIST, made);
        }
    }
}
