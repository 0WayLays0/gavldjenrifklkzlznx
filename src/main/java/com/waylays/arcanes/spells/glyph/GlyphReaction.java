package com.waylays.arcanes.spells.glyph;

import javax.annotation.Nullable;

/**
 * What happens when a glyph strikes an enemy already inscribed with another element. Fifteen pairs, fifteen
 * reactions; those with a {@link #zone} leave a lasting zone where they happened.
 */
public enum GlyphReaction {
    MELT("melt", GlyphElement.FIRE, GlyphElement.FROST, null),
    OVERLOAD("overload", GlyphElement.FIRE, GlyphElement.STORM, null),
    STEAM("steam", GlyphElement.FIRE, GlyphElement.TIDE, Zone.VORTEX),
    FIRESTORM("firestorm", GlyphElement.FIRE, GlyphElement.GALE, Zone.VORTEX),
    MAGMA("magma", GlyphElement.FIRE, GlyphElement.STONE, Zone.POOL),
    SUPERCONDUCT("superconduct", GlyphElement.FROST, GlyphElement.STORM, null),
    FREEZE("freeze", GlyphElement.FROST, GlyphElement.TIDE, null),
    BLIZZARD("blizzard", GlyphElement.FROST, GlyphElement.GALE, Zone.VORTEX),
    SHATTER("shatter", GlyphElement.FROST, GlyphElement.STONE, null),
    ELECTROCUTE("electrocute", GlyphElement.STORM, GlyphElement.TIDE, null),
    THUNDERSTORM("thunderstorm", GlyphElement.STORM, GlyphElement.GALE, Zone.VORTEX),
    MAGNETIZE("magnetize", GlyphElement.STORM, GlyphElement.STONE, null),
    TYPHOON("typhoon", GlyphElement.TIDE, GlyphElement.GALE, Zone.VORTEX),
    MIRE("mire", GlyphElement.TIDE, GlyphElement.STONE, Zone.POOL),
    SANDSTORM("sandstorm", GlyphElement.GALE, GlyphElement.STONE, Zone.VORTEX);

    /** How a lasting reaction is drawn: a pool on the ground, or a whirling column. */
    public enum Zone { POOL, VORTEX }

    public final String id;
    public final GlyphElement a;
    public final GlyphElement b;
    @Nullable
    public final Zone zone;

    GlyphReaction(String id, GlyphElement a, GlyphElement b, @Nullable Zone zone) {
        this.id = id;
        this.a = a;
        this.b = b;
        this.zone = zone;
    }

    /** The reaction of two different elements, in either order. */
    public static GlyphReaction of(GlyphElement x, GlyphElement y) {
        for (GlyphReaction r : values()) {
            if (r.a == x && r.b == y || r.a == y && r.b == x) {
                return r;
            }
        }
        throw new IllegalArgumentException("no reaction between " + x + " and " + y);
    }

    /** The colours of its two elements, mixed. */
    public int color() {
        int r = (((a.color >> 16) & 0xFF) + ((b.color >> 16) & 0xFF)) / 2;
        int g = (((a.color >> 8) & 0xFF) + ((b.color >> 8) & 0xFF)) / 2;
        int bl = ((a.color & 0xFF) + (b.color & 0xFF)) / 2;
        return r << 16 | g << 8 | bl;
    }

    public static GlyphReaction byId(int ordinal) {
        GlyphReaction[] all = values();
        return all[Math.max(0, Math.min(ordinal, all.length - 1))];
    }
}
