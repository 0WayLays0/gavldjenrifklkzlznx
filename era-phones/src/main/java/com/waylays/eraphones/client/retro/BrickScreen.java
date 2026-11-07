package com.waylays.eraphones.client.retro;

import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

/**
 * The 1985 radiotelephone: a heavy beige brick with a long aerial, eight red figures of light behind smoked glass,
 * three lamps (in use, no service, battery), and big rubber keys: SND to call (with nothing typed, the last number
 * again), END, CLR, STO and RCL with a digit to keep and recall ten numbers, FCN # to show one's own number, FCN * or
 * VOL for a silent ring, PWR to put it away. What is said during a call shows beside it.
 */
public class BrickScreen extends RetroScreen {
    private static final int[] MASK = new int[128];

    static {
        String[] map = {"0:63", "1:6", "2:91", "3:79", "4:102", "5:109", "6:125", "7:7", "8:127", "9:111", "-:64", " :0", "A:119", "b:124", "C:57",
                "c:88", "d:94", "E:121", "F:113", "H:118", "h:116", "L:56", "n:84", "o:92", "P:115", "r:80", "S:109", "t:120", "U:62", "u:28", "y:110",
                "_:8", "I:6", "i:4", "O:63", "=:72"};
        for (String m : map) {
            MASK[m.charAt(0)] = Integer.parseInt(m.substring(2));
        }
    }

    private String typed = "";
    private String last = "";
    /** A key waiting for its digit: "sto", "rcl" or "fcn". */
    private String pending = "";
    private String flash = "";
    private long flashAt;

    public BrickScreen() {
        super("radiotelephone", 120, 380);
        int beige = 0xFF5C5650, light = 0xFFF0E8DC;
        String[] top = {"snd", "clr", "end", "rcl", "sto", "fcn"};
        String[] topLabels = {"SND", "CLR", "END", "RCL", "STO", "FCN"};
        int[] topColors = {0xFF3E7A46, beige, 0xFF9A3A32, beige, beige, 0xFF6A5A30};
        for (int i = 0; i < 6; i++) {
            keys.add(new Key(top[i], 18 + i % 3 * 32, 170 + i / 3 * 22, 26, 17, topLabels[i], "", topColors[i], light));
        }
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
        String[] subs = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PRS", "TUV", "WXY", "", "OPER", ""};
        for (int i = 0; i < 12; i++) {
            keys.add(new Key(digits[i], 18 + i % 3 * 32, 214 + i / 3 * 22, 26, 17, digits[i], subs[i], 0xFF3A3632, light));
        }
        keys.add(new Key("pwr", 18, 306, 26, 15, "PWR", "", 0xFF2A2622, 0xFFE07060));
        keys.add(new Key("vol", 50, 306, 26, 15, "VOL", "", 0xFF2A2622, light));
    }

    @Override
    protected void opened() {
        PhoneClient.beep(SoundEvents.NOTE_BLOCK_BIT.value(), .8f, .5f);
    }

    @Override
    protected int[] screenBox() {
        return new int[] {16, 124, 104, 150};
    }

    private void flash(String text) {
        flash = text;
        flashAt = System.currentTimeMillis();
    }

    /** What the eight figures show now. */
    private String display(long now) {
        if (now - flashAt < 1500) {
            return flash;
        }
        int st = PhoneClient.callState;
        if (st == 2) {
            if (Numbers.hidden(PhoneClient.callNumber)) {
                return now % 600 < 300 ? "--------" : "        ";
            }
            return now % 1600 < 800 ? "CALL" : PhoneClient.callNumber;
        }
        if (st == 1) {
            return now % 700 < 450 ? "CALL" : "";
        }
        if (st == 3) {
            long s = (now - PhoneClient.callSince) / 1000;
            return String.format("%d.%02d", s / 60, s % 60);
        }
        if (PhoneClient.inCall()) {
            return switch (PhoneClient.callReason) {
                case "busy", "declined" -> "bUSy";
                case "unreachable", "no_answer" -> "no AnS";
                case "no_network" -> "no Srv";
                case "battery" -> "bAtt";
                case "lost" -> "LoSt";
                case "unassigned", "bad_number" -> "Err";
                default -> "End";
            };
        }
        if (!pending.isEmpty()) {
            return (pending.equals("sto") ? "Sto" : pending.equals("rcl") ? "rcL" : "Fcn") + (now % 600 < 300 ? " -" : "  ");
        }
        if (!typed.isEmpty()) {
            return typed.length() == 7 ? typed.substring(0, 3) + "-" + typed.substring(3) : typed;
        }
        int missed = PhoneClient.badge("telephone");
        if (missed > 0) {
            return "CALL " + Math.min(9, missed);
        }
        return now % 1000 < 500 ? "-" : "";
    }

    @Override
    protected void screen(GuiGraphics g, int mx, int my, long now) {
        int[] b = screenBox();
        g.fill(b[0], b[1], b[2], b[3], 0xFF160404);
        g.fillGradient(b[0], b[1], b[2], b[1] + 8, 0x30FF8060, 0x00FF8060);
        String text = display(now);
        // Right-aligned on eight figures; a dot lights the figure before it
        int[] cells = new int[8];
        boolean[] dots = new boolean[8];
        int n = 7;
        for (int i = text.length() - 1; i >= 0 && n >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.') {
                dots[n] = true;
                continue;
            }
            cells[n--] = c < 128 ? MASK[c] : 0;
        }
        for (int k = 0; k < 8; k++) {
            digit(g, b[0] + 4 + k * 10.5f, b[1] + 5, cells[k], dots[k]);
        }
        // The three lamps under the glass
        lamp(g, 20, "IN USE", PhoneClient.callState != 0, now);
        lamp(g, 50, "NO SVC", PhoneClient.bars() == 0, now);
        lamp(g, 80, "BATT", PhoneClient.battery() < 200, now);
    }

    private void lamp(GuiGraphics g, int x, String label, boolean on, long now) {
        boolean lit = on && (now % 1000 < 700 || !label.equals("BATT"));
        PhoneUI.round(g, x, 154, 5, 3, 1, lit ? 0xFFFF4A2A : 0xFF3A1A14);
        if (lit) {
            PhoneUI.glow(g, x + 2.5f, 155.5f, 10, 0x88FF4A2A);
        }
        PhoneUI.text(g, label, x + 7, 154, 0xFFB8AC9C, .35f);
    }

    /** One figure of seven segments (and its dot), {@code mask} the segments lit; the unlit ones faintly there. */
    private static void digit(GuiGraphics g, float x, float y, int mask, boolean dot) {
        float sw = 6, sh = 7, t = 1.3f;
        // a b c d e f g: top, top right, bottom right, bottom, bottom left, top left, middle
        float[][] seg = {{x + 1, y, sw, t}, {x + sw + .6f, y + .8f, t, sh - .6f}, {x + sw + .6f, y + sh + 1.2f, t, sh - .6f}, {x + 1, y + 2 * sh + 1.4f, sw, t},
                {x - .2f, y + sh + 1.2f, t, sh - .6f}, {x - .2f, y + .8f, t, sh - .6f}, {x + 1, y + sh + .4f, sw, t}};
        for (int s = 0; s < 7; s++) {
            boolean on = (mask >> s & 1) != 0;
            fill(g, seg[s], on ? 0xFFFF4430 : 0x24FF4430);
            if (on) {
                fill(g, new float[] {seg[s][0] - .4f, seg[s][1] - .4f, seg[s][2] + .8f, seg[s][3] + .8f}, 0x40FF6040);
            }
        }
        fill(g, new float[] {x + sw + 2.2f, y + 2 * sh + 1.4f, 1.2f, 1.2f}, dot ? 0xFFFF4430 : 0x24FF4430);
    }

    private static void fill(GuiGraphics g, float[] r, int color) {
        g.pose().pushPose();
        g.pose().translate(r[0], r[1], 0);
        g.pose().scale(r[2] / 10f, r[3] / 10f, 1);
        g.fill(0, 0, 10, 10, color);
        g.pose().popPose();
    }

    @Override
    protected void press(String id) {
        if (id.length() == 1 && Character.isDigit(id.charAt(0)) && !pending.isEmpty()) {
            memory(id);
            return;
        }
        switch (id) {
            case "snd", "call", "ok" -> {
                if (PhoneClient.callState != 0) {
                    return;
                }
                String n = typed.isEmpty() ? last : typed;
                if (Numbers.normalize(n) == null) {
                    flash("Err");
                    return;
                }
                last = n;
                PhoneClient.dial(n);
                typed = "";
            }
            case "clr", "back" -> {
                if (!pending.isEmpty()) {
                    pending = "";
                } else if (!typed.isEmpty()) {
                    typed = typed.substring(0, typed.length() - 1);
                }
            }
            case "end" -> {
                typed = "";
                pending = "";
            }
            case "sto", "rcl", "fcn" -> pending = id;
            case "pwr" -> onClose();
            case "vol" -> silent();
            case "*", "#" -> {
                if (pending.equals("fcn")) {
                    memory(id);
                }
            }
            default -> {
                if (id.length() == 1 && Character.isDigit(id.charAt(0)) && typed.length() < 7) {
                    typed += id;
                }
            }
        }
    }

    private void silent() {
        PhoneClient.silent = !PhoneClient.silent;
        PhoneClient.ask(PhoneClient.silent ? 7 : 8, "");
        flash(PhoneClient.silent ? "SIL on" : "SIL oFF");
    }

    /** STO, RCL or FCN followed by a key. */
    private void memory(String key) {
        String what = pending;
        pending = "";
        switch (what) {
            case "sto" -> {
                String n = Numbers.normalize(typed);
                if (n == null) {
                    flash("Err");
                    return;
                }
                PhoneClient.send(new PhoneNet.ContactEdit("#" + key, n, false));
                flash("Sto " + key);
                typed = "";
            }
            case "rcl" -> {
                for (PhoneNet.ContactView c : PhoneClient.contacts) {
                    if (c.name().equals("#" + key)) {
                        typed = c.number().replace("-", "");
                        flash("rcL " + key);
                        return;
                    }
                }
                flash("Err");
            }
            case "fcn" -> {
                if (key.equals("#")) {
                    flash(PhoneClient.number);
                    flashAt += 1500;
                } else if (key.equals("*")) {
                    silent();
                }
            }
            default -> {
            }
        }
    }
}
