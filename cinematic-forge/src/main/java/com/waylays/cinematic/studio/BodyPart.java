package com.waylays.cinematic.studio;

/**
 * The joints a pose key can turn. Stored by position: new joints are only ever added at the end, so takes made with
 * fewer joints still read.
 */
public enum BodyPart {
    HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, RIGHT_FOREARM, LEFT_FOREARM, RIGHT_SHIN, LEFT_SHIN;

    public String key() {
        return "cinematic.part." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
