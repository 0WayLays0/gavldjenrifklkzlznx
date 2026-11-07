package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.client.SnakeGame;
import com.waylays.eraphones.client.smart.PhoneApp;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Serpent, the old game made new: a neon snake on a dark grid, steered with the arrows (or a swipe of the pad). */
public class SerpentApp extends PhoneApp {
    private static final int CW = 24, CH = 34, CELL = 6;
    private SnakeGame game;

    public SerpentApp() {
        super("serpent");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF06121A, 0xFF02060A};
    }

    @Override
    public void open() {
        game = new SnakeGame(CW, CH, 140);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        long now = System.currentTimeMillis();
        if (game.update(now)) {
            phone.sound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(), 1.8f);
        }
        PhoneSettings s = PhoneSettings.get();
        if (game.over && game.score > s.snakeBest) {
            s.snakeBest = game.score;
            s.save();
        }
        int gx = (X0 + X1) / 2 - CW * CELL / 2, gy = Y0 + 14;
        PhoneUI.text(g, Component.translatable("phone.eraphones.score", game.score).getString(), X0 + 10, Y0 + 2, 0xFF5CF0C0, .65f);
        PhoneUI.right(g, Component.translatable("phone.eraphones.best", s.snakeBest).getString(), X1 - 10, Y0 + 2, PhoneUI.DIM, .65f);
        PhoneUI.round(g, gx - 3, gy - 3, CW * CELL + 6, CH * CELL + 6, 6, 0x3324F0C0);
        g.fill(gx, gy, gx + CW * CELL, gy + CH * CELL, 0xFF03080C);
        for (int x = 0; x <= CW; x++) {
            g.fill(gx + x * CELL, gy, gx + x * CELL + 1, gy + CH * CELL, 0x0E5CF0C0);
        }
        for (int y = 0; y <= CH; y++) {
            g.fill(gx, gy + y * CELL, gx + CW * CELL, gy + y * CELL + 1, 0x0E5CF0C0);
        }
        if (game.food != null) {
            float fx = gx + game.food[0] * CELL + CELL / 2f, fy = gy + game.food[1] * CELL + CELL / 2f;
            PhoneUI.glow(g, fx, fy, 18, 0xAAFF4A8A);
            PhoneUI.round(g, (int) fx - 2, (int) fy - 2, 5, 5, 2, 0xFFFF6AA0);
        }
        int i = 0, n = game.body.size();
        for (int[] p : game.body) {
            int c = PhoneUI.mix(0xFF7CFFE0, 0xFF1A8CFF, i / (float) Math.max(1, n));
            PhoneUI.round(g, gx + p[0] * CELL, gy + p[1] * CELL, CELL, CELL, 2, game.over ? PhoneUI.mix(c, 0xFF404040, .6f) : c);
            i++;
        }
        if (game.over) {
            PhoneUI.round(g, X0 + 20, gy + CH * CELL / 2 - 16, X1 - X0 - 40, 32, 8, 0xE0000000);
            PhoneUI.centered(g, Component.translatable("phone.eraphones.game_over").getString(), (X0 + X1) / 2f, gy + CH * CELL / 2f - 10, 0xFFFF6AA0, .9f);
            PhoneUI.centered(g, Component.translatable("phone.eraphones.play_again").getString(), (X0 + X1) / 2f, gy + CH * CELL / 2f + 3, PhoneUI.DIM, .55f);
        }
        // A pad to steer with the mouse
        int py = gy + CH * CELL + 8;
        String[] arrows = {"◀", "▲", "▼", "▶"};
        for (int k = 0; k < 4; k++) {
            int x = (X0 + X1) / 2 - 62 + k * 32;
            boolean h = PhoneUI.in(mx, my, x, py, 28, 16);
            PhoneUI.round(g, x, py, 28, 16, 8, h ? 0x445CF0C0 : 0x225CF0C0);
            PhoneUI.centered(g, arrows[k], x + 14, py + 4, 0xFF7CFFE0, .8f);
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (game.over) {
            game = new SnakeGame(CW, CH, 140);
            return true;
        }
        int py = Y0 + 14 + CH * CELL + 8;
        for (int k = 0; k < 4; k++) {
            int x = (X0 + X1) / 2 - 62 + k * 32;
            if (PhoneUI.in(mx, my, x, py, 28, 16)) {
                steer(k);
            }
        }
        return true;
    }

    private void steer(int k) {
        switch (k) {
            case 0 -> game.turn(-1, 0);
            case 1 -> game.turn(0, -1);
            case 2 -> game.turn(0, 1);
            default -> game.turn(1, 0);
        }
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> steer(0);
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> steer(1);
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> steer(2);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> steer(3);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (game.over) {
                    game = new SnakeGame(CW, CH, 140);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
