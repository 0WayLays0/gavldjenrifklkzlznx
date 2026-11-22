package com.waylays.cinematic.studio;

/** The six limbs a pose key can turn. */
public enum BodyPart {
    HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;

    public String key() {
        return "cinematic.part." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
