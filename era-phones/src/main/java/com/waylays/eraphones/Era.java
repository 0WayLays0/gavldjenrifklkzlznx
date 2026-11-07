package com.waylays.eraphones;

/**
 * The four phones, oldest first. Each drains its battery at its own pace (per second of call, per second the screen
 * is on, in thousandths of a full charge), and can or cannot send messages and take photographs.
 */
public enum Era {
    /** 1985: a brick with a long aerial, a red LED display, ten memories; calls only; its battery lasts minutes. */
    BRICK("radiotelephone", 1985, 2.5f, 0.4f, false, false, true),
    /** 1997: a GSM handset with a stub aerial and a green backlit screen; calls and 160-character messages. */
    GSM("gsm", 1997, 0.6f, 0.1f, true, false, false),
    /** 2004: a flip phone with a colour screen, polyphonic rings and a small camera. */
    FLIP("clapet", 2004, 0.8f, 0.25f, true, true, false),
    /** 2026: a smartphone. */
    SMART("smartphone", 2026, 1.0f, 0.5f, true, true, false);

    public final String item;
    public final int year;
    public final float callDrain, screenDrain;
    public final boolean sms, camera, weakIndoors;

    Era(String item, int year, float callDrain, float screenDrain, boolean sms, boolean camera, boolean weakIndoors) {
        this.item = item;
        this.year = year;
        this.callDrain = callDrain;
        this.screenDrain = screenDrain;
        this.sms = sms;
        this.camera = camera;
        this.weakIndoors = weakIndoors;
    }

    public static Era byId(int id) {
        Era[] all = values();
        return all[Math.floorMod(id, all.length)];
    }
}
