package com.waylays.cinematic.client;

import com.waylays.cinematic.studio.BodyPart;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** A clickable figure seen from the front (the actor's right is on your left): each joint is a block to pick. */
final class Mannequin {
    private final Map<BodyPart, int[]> rects = new EnumMap<>(BodyPart.class);
    private final int x, y, w, h;

    Mannequin(int areaX, int areaY, int areaW, int areaH) {
        int u = Math.max(2, Math.min((areaW - 4) / 18, (areaH - 4) / 35));
        w = 18 * u;
        h = 35 * u;
        x = areaX + (areaW - w) / 2;
        y = areaY + Math.max(0, (areaH - h) / 2);
        put(BodyPart.HEAD, 5, 0, 8, 8, u);
        put(BodyPart.BODY, 5, 9, 8, 12, u);
        put(BodyPart.RIGHT_ARM, 0, 9, 4, 6, u);
        put(BodyPart.RIGHT_FOREARM, 0, 16, 4, 6, u);
        put(BodyPart.LEFT_ARM, 14, 9, 4, 6, u);
        put(BodyPart.LEFT_FOREARM, 14, 16, 4, 6, u);
        put(BodyPart.RIGHT_LEG, 5, 22, 4, 6, u);
        put(BodyPart.RIGHT_SHIN, 5, 29, 4, 6, u);
        put(BodyPart.LEFT_LEG, 9, 22, 4, 6, u);
        put(BodyPart.LEFT_SHIN, 9, 29, 4, 6, u);
    }

    private void put(BodyPart p, int px, int py, int pw, int ph, int u) {
        rects.put(p, new int[] {x + px * u, y + py * u, pw * u, ph * u});
    }

    int bottom() {
        return y + h;
    }

    BodyPart at(double mx, double my) {
        for (Map.Entry<BodyPart, int[]> e : rects.entrySet()) {
            int[] r = e.getValue();
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) return e.getKey();
        }
        return null;
    }

    void render(GuiGraphics g, Font font, BodyPart selected, Set<BodyPart> withKeys, int mx, int my) {
        BodyPart hover = at(mx, my);
        for (Map.Entry<BodyPart, int[]> e : rects.entrySet()) {
            int[] r = e.getValue();
            BodyPart p = e.getKey();
            int fill = p == selected ? 0xFFFFC23D : p == hover ? 0xFFB8B8B8 : 0xFF707070;
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
            g.renderOutline(r[0], r[1], r[2], r[3], p == selected ? 0xFFFFFFFF : 0xFF202020);
            if (withKeys.contains(p)) {
                // A small blue diamond: this joint has keys
                int cx = r[0] + r[2] / 2, cy = r[1] + r[3] / 2;
                g.fill(cx - 1, cy - 2, cx + 2, cy + 3, 0xFF3FA9FF);
                g.fill(cx - 2, cy - 1, cx + 3, cy + 2, 0xFF3FA9FF);
            }
        }
    }
}
