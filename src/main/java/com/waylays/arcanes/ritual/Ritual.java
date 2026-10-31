package com.waylays.arcanes.ritual;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/**
 * The group rituals: each needs a number of players kneeling (sneaking) on the stations of its figure, channelling
 * together for a long while. Each participant gives {@link #MANA} mana over the channelling.
 * <p>
 * Each ritual has its own figure and is drawn its own way ({@link Surface}): in chalk on the ground, in chalk on a wall,
 * or prepared at an Altar of Rites with an offering.
 */
public enum Ritual {
    /** A meteor falls where the leader looks. Its figure: a triangle pointing down from the sky. */
    METEOR("meteor", 2, 600, 0xFF7A2E, Figure.TRIANGLE, Surface.GROUND, null, 0),
    /** A great dome shields the place from hostile creatures and their shots. Its figure: a six-pointed star. */
    DOME("dome", 2, 800, 0x7FD8FF, Figure.HEXAGRAM, Surface.GROUND, null, 0),
    /** A storm gathers over the place and strikes hostile creatures with lightning. Its figure: a spiral. */
    STORM("storm", 2, 600, 0x9FA8FF, Figure.SPIRAL, Surface.GROUND, null, 0),
    /** A portal opens in the wall to the anchor the leader marked with their chalk, and one back. Drawn on a wall: an arch. */
    PORTAL("portal", 2, 400, 0xC27BFF, Figure.ARCH, Surface.WALL, null, 0),
    /** The elders' blessing: everyone around is healed, cleansed and strengthened for a long while. Its figure: a rose. */
    BLESSING("blessing", 2, 900, 0xFFE27A, Figure.ROSE, Surface.GROUND, null, 0),
    /** A colossus of stone rises out of the ground and fights for the group. At the altar: a square of menhirs. */
    COLOSSUS("colossus", 2, 1200, 0xB8B0A0, Figure.MENHIRS, Surface.ALTAR, Items.STONE_BRICKS, 16),
    /** A great bridge of amethyst grows out to where the leader looks. At the altar: a path of light. */
    BRIDGE("bridge", 2, 600, 0xB27AFF, Figure.PATH, Surface.ALTAR, Items.AMETHYST_SHARD, 8);

    /** Mana each participant gives, spread over the channelling. */
    public static final float MANA = 75;

    /** The shape a ritual is drawn in. */
    public enum Figure { TRIANGLE, HEXAGRAM, SPIRAL, ARCH, ROSE, MENHIRS, PATH }

    /** How a ritual is begun: chalk on the ground, chalk on a wall, or at an Altar of Rites. */
    public enum Surface { GROUND, WALL, ALTAR }

    public final String id;
    public final int participants;
    public final int channelTicks;
    public final int color;
    public final Figure figure;
    public final Surface surface;
    /** What must be offered at the altar to begin it (null for none), and how many. */
    @Nullable
    public final Item offering;
    public final int offeringCount;

    Ritual(String id, int participants, int channelTicks, int color, Figure figure, Surface surface, @Nullable Item offering, int offeringCount) {
        this.id = id;
        this.participants = participants;
        this.channelTicks = channelTicks;
        this.color = color;
        this.figure = figure;
        this.surface = surface;
        this.offering = offering;
        this.offeringCount = offeringCount;
    }

    public static Ritual byIndex(int index) {
        Ritual[] all = values();
        return all[Mth.clamp(index, 0, all.length - 1)];
    }

    public Ritual next() {
        Ritual[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** The next ritual the chalk can draw (the altar's are begun at the altar). */
    public Ritual nextChalked() {
        Ritual r = next();
        while (r.surface == Surface.ALTAR) {
            r = r.next();
        }
        return r;
    }

    public Component title() {
        return Component.translatable("ritual.arcanes_oublies." + id);
    }

    public Component description() {
        return Component.translatable("ritual.arcanes_oublies." + id + ".desc");
    }
}
