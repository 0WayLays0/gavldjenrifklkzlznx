package com.waylays.eraphones.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Serpent: the snake that grows as it eats, on a grid that wraps round at its edges; it dies biting itself. Each phone
 * draws it its own way (dark pixels on green, colours, neon).
 */
public final class SnakeGame {
    public final int w, h;
    public final Deque<int[]> body = new ArrayDeque<>();
    public int[] food;
    public int score;
    public boolean over;
    private int dx = 1, dy, nextDx = 1, nextDy;
    private final Random random = new Random();
    private long last;
    private int every;

    public SnakeGame(int w, int h, int every) {
        this.w = w;
        this.h = h;
        this.every = every;
        for (int i = 0; i < 4; i++) {
            body.addFirst(new int[] {w / 2 - 3 + i, h / 2});
        }
        food();
        last = System.currentTimeMillis();
    }

    private void food() {
        for (int tries = 0; tries < 400; tries++) {
            int[] f = {random.nextInt(w), random.nextInt(h)};
            if (!occupied(f[0], f[1])) {
                food = f;
                return;
            }
        }
    }

    public boolean occupied(int x, int y) {
        for (int[] p : body) {
            if (p[0] == x && p[1] == y) {
                return true;
            }
        }
        return false;
    }

    public void turn(int ndx, int ndy) {
        if (ndx == -dx && ndy == -dy) {
            return;
        }
        nextDx = ndx;
        nextDy = ndy;
    }

    /** Moves on as many steps as the time since the last asks; returns whether it ate. */
    public boolean update(long now) {
        boolean ate = false;
        int steps = 0;
        while (!over && now - last >= every && steps++ < 4) {
            last += every;
            ate |= step();
        }
        if (now - last > every * 4L) {
            last = now;
        }
        return ate;
    }

    private boolean step() {
        dx = nextDx;
        dy = nextDy;
        int[] head = body.peekFirst();
        int nx = Math.floorMod(head[0] + dx, w), ny = Math.floorMod(head[1] + dy, h);
        boolean eat = food != null && nx == food[0] && ny == food[1];
        if (!eat) {
            body.removeLast();
        }
        if (occupied(nx, ny)) {
            over = true;
            return false;
        }
        body.addFirst(new int[] {nx, ny});
        if (eat) {
            score++;
            every = Math.max(55, every - 3);
            food();
        }
        return eat;
    }
}
