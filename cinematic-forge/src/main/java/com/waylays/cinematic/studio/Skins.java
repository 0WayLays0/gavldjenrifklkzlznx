package com.waylays.cinematic.studio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** Checks shared by the client (before sending) and the server (before storing): a PNG, square, a multiple of 64. */
public final class Skins {
    public static final int MAX_BYTES = 12 * 1024 * 1024;
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{40}");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    private Skins() {}

    public static boolean validHash(String s) {
        return s != null && HASH.matcher(s).matches();
    }

    public static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Width and height from the PNG header, or null when it is not a PNG. */
    public static int[] size(byte[] data) {
        if (data.length < 24) return null;
        for (int i = 0; i < PNG.length; i++) if (data[i] != PNG[i]) return null;
        if (!"IHDR".equals(new String(data, 12, 4, StandardCharsets.US_ASCII))) return null;
        int w = (data[16] & 0xFF) << 24 | (data[17] & 0xFF) << 16 | (data[18] & 0xFF) << 8 | (data[19] & 0xFF);
        int h = (data[20] & 0xFF) << 24 | (data[21] & 0xFF) << 16 | (data[22] & 0xFF) << 8 | (data[23] & 0xFF);
        return new int[] {w, h};
    }

    /** A translation key describing why this file cannot be a skin, or null when it can. */
    public static String problem(byte[] data, int maxSide) {
        if (data.length > MAX_BYTES) return "cinematic.skin.too_heavy";
        int[] s = size(data);
        if (s == null) return "cinematic.skin.not_png";
        if (s[0] == s[1] * 2) return "cinematic.skin.legacy";
        if (s[0] != s[1] || s[0] % 64 != 0) return "cinematic.skin.not_square";
        if (s[0] > maxSide) return "cinematic.skin.too_big";
        return null;
    }
}
