package com.waylays.cinematic.studio;

/** Gestures an actor can perform on top of its recorded movement (the poses are drawn by the client). */
public enum Emote {
    NONE, WAVE, POINT, ARMS_CROSSED, HANDS_UP, FACEPALM, SALUTE, THINK, CLAP, SIT, DANCE, KNEEL, SHRUG;

    public String key() {
        return "cinematic.emote." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Emote byId(int id) {
        Emote[] all = values();
        return id >= 0 && id < all.length ? all[id] : NONE;
    }
}
