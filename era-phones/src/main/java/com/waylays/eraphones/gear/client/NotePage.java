package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A sheet of paper drawn (200 x 260 units): its paper, its title, its text written on the lines (by hand, slanting, or
 * typed), in its ink, and who signed it. Used to read a sheet and, as it is written, to see it.
 */
public final class NotePage {
    public static final int W = 200, H = 260;
    public static final int[] INKS = {0xFF1A1A24, 0xFF1C3A8A, 0xFF9A1C1C, 0xFF1C6A3A, 0xFF5E5E68};
    public static final String[] STYLES = {"carnet", "lettre", "parchemin", "postit", "dechiree"};

    /** Where each paper is written: left, right, the title's line, the first line, the gap between lines, the last line. */
    private static final float[][] AREA = {{34, 188, 30, 44, 10, 250}, {22, 178, 22, 46, 9.5f, 240}, {24, 176, 24, 44, 9.5f, 238},
            {22, 178, 34, 52, 9.5f, 192}, {34, 188, 26, 44, 10, 250}};

    public record Data(String title, String text, int style, int ink, boolean typed, String author) {
        public static Data of(ItemStack stack) {
            return new Data(stack.getOrDefault(GearRegistry.NOTE_TITLE.get(), ""), stack.getOrDefault(GearRegistry.NOTE_TEXT.get(), ""),
                    Math.floorMod(stack.getOrDefault(GearRegistry.NOTE_STYLE.get(), 0), STYLES.length),
                    Math.floorMod(stack.getOrDefault(GearRegistry.NOTE_INK.get(), 0), INKS.length), stack.getOrDefault(GearRegistry.NOTE_TYPED.get(), false),
                    stack.getOrDefault(GearRegistry.NOTE_AUTHOR.get(), ""));
        }
    }

    private NotePage() {}

    private static ResourceLocation paper(int style) {
        return EraPhones.id("textures/gui/gear/note_" + style + ".png");
    }

    /** The text in lines that fit {@code width} (units at {@code scale}), its own line breaks kept. */
    public static List<FormattedCharSequence> lines(String text, float width, float scale, boolean typed) {
        Font font = Minecraft.getInstance().font;
        Style style = typed ? Style.EMPTY : Style.EMPTY.withItalic(true);
        List<FormattedCharSequence> out = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) {
                out.add(FormattedCharSequence.EMPTY);
                continue;
            }
            out.addAll(font.split(Component.literal(para).withStyle(style), Math.max(10, (int) (width / scale))));
        }
        return out;
    }

    /**
     * Draws the page at ({@code x}, {@code y}) in the current units, {@code scroll} lines down; returns how many lines
     * the text has past what fits (0 when it all fits).
     */
    public static int draw(GuiGraphics g, float x, float y, Data d, int scroll) {
        float[] a = AREA[d.style];
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        PhoneUI.blend();
        g.blit(paper(d.style), 0, 0, W, H, 0, 0, W * 2, H * 2, W * 2, H * 2);
        int ink = INKS[d.ink];
        float textScale = .82f;
        Font font = Minecraft.getInstance().font;
        Style style = d.typed ? Style.EMPTY : Style.EMPTY.withItalic(true);
        if (!d.title.isBlank()) {
            float ts = 1.05f;
            Component title = Component.literal(PhoneUI.fit(d.title, (a[1] - a[0]) / ts, 1)).withStyle(style.withBold(true));
            g.pose().pushPose();
            g.pose().translate((a[0] + a[1]) / 2f - font.width(title) * ts / 2f, a[2] - 8 * ts, 0);
            g.pose().scale(ts, ts, 1);
            g.drawString(font, title, 0, 0, ink, false);
            g.pose().popPose();
        }
        List<FormattedCharSequence> lines = lines(d.text, a[1] - a[0], textScale, d.typed);
        int fits = (int) ((a[5] - a[3]) / a[4]) + 1 - (d.author.isEmpty() ? 0 : 2);
        int from = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - fits)));
        for (int i = 0; i < fits && from + i < lines.size(); i++) {
            float ly = a[3] + i * a[4] - 8 * textScale + (d.typed ? 0 : -.5f);
            g.pose().pushPose();
            g.pose().translate(a[0] + (d.typed ? 0 : (i % 3) * .3f), ly, 0);
            g.pose().scale(textScale, textScale, 1);
            g.drawString(font, lines.get(from + i), 0, 0, ink, false);
            g.pose().popPose();
        }
        if (!d.author.isEmpty()) {
            Component sig = Component.literal("— " + d.author).withStyle(Style.EMPTY.withItalic(true));
            float sy = a[5] - a[4] * .2f - 8 * .9f;
            g.pose().pushPose();
            g.pose().translate(a[1] - font.width(sig) * .9f - 2, sy, 0);
            g.pose().scale(.9f, .9f, 1);
            g.drawString(font, sig, 0, 0, ink, false);
            g.pose().popPose();
        }
        g.pose().popPose();
        return Math.max(0, lines.size() - fits);
    }
}
