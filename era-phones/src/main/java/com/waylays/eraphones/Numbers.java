package com.waylays.eraphones;

import javax.annotation.Nullable;

/** Phone numbers: seven digits written "555-0142". The hidden caller's number is {@link #HIDDEN}. */
public final class Numbers {
    /** The number of whoever calls or writes without showing it (only game masters can). */
    public static final String HIDDEN = "###";
    public static final int MAX_LENGTH = 16;

    private Numbers() {}

    /** {@code raw} written the standard way ("5550142", "555 0142", "0142" all give "555-0142"), or null. */
    @Nullable
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        String d = digits.toString();
        if (d.length() == 4) {
            d = "555" + d;
        }
        if (d.length() != 7) {
            return null;
        }
        return d.substring(0, 3) + "-" + d.substring(3);
    }

    public static boolean hidden(String number) {
        return HIDDEN.equals(number);
    }
}
