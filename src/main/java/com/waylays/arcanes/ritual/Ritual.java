package com.waylays.arcanes.ritual;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The group rituals: each needs a number of players kneeling (sneaking) on the stations of a chalk circle, channelling
 * together for a long while. Each participant gives {@link #MANA} mana over the channelling.
 */
public enum Ritual {
    /** A meteor falls where the leader looks. */
    METEOR("meteor", 2, 600, 0xFF7A2E),
    /** A great dome shields the place from hostile creatures and their shots. */
    DOME("dome", 2, 800, 0x7FD8FF),
    /** A storm gathers over the place and strikes hostile creatures with lightning. */
    STORM("storm", 2, 600, 0x9FA8FF),
    /** A portal opens to the anchor the leader marked with their chalk, and one back. */
    PORTAL("portal", 2, 400, 0xC27BFF),
    /** The elders' blessing: everyone around is healed, cleansed and strengthened for a long while. */
    BLESSING("blessing", 2, 900, 0xFFE27A),
    /** A colossus of stone wakes and fights for the group. */
    COLOSSUS("colossus", 2, 1200, 0xB8B0A0);

    /** Mana each participant gives, spread over the channelling. */
    public static final float MANA = 75;

    public final String id;
    public final int participants;
    public final int channelTicks;
    public final int color;

    Ritual(String id, int participants, int channelTicks, int color) {
        this.id = id;
        this.participants = participants;
        this.channelTicks = channelTicks;
        this.color = color;
    }

    public static Ritual byIndex(int index) {
        Ritual[] all = values();
        return all[Mth.clamp(index, 0, all.length - 1)];
    }

    public Ritual next() {
        Ritual[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public Component title() {
        return Component.translatable("ritual.arcanes_oublies." + id);
    }

    public Component description() {
        return Component.translatable("ritual.arcanes_oublies." + id + ".desc");
    }
}
