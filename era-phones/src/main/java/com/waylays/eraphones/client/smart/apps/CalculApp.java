package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.math.MathContext;

/** Calculette: a calculator of round keys, the number large above them; the keyboard works too. */
public class CalculApp extends PhoneApp {
    private static final String[][] KEYS = {{"AC", "±", "%", "÷"}, {"7", "8", "9", "×"}, {"4", "5", "6", "−"}, {"1", "2", "3", "+"}, {"0", "", ".", "="}};
    private String shown = "0";
    private double held;
    private String op;
    private boolean fresh = true;

    public CalculApp() {
        super("calcul");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF000000, 0xFF000000};
    }

    private int size() {
        return (X1 - X0 - 20) / 4;
    }

    private int keysTop() {
        return Y1 - 5 * (size() + 4) - 2;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        PhoneUI.text(g, "‹", X0 + 6, TOP + 3, PhoneUI.ACCENT, 1.6f);
        float scale = shown.length() > 9 ? 1.6f : 2.6f;
        PhoneUI.right(g, shown, X1 - 10, keysTop() - 10 - 9 * scale, PhoneUI.WHITE, scale);
        if (op != null) {
            PhoneUI.right(g, fmt(held) + " " + op, X1 - 10, keysTop() - 22 - 9 * scale, PhoneUI.FAINT, .7f);
        }
        int s = size();
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 4; c++) {
                String k = KEYS[r][c];
                if (k.isEmpty()) {
                    continue;
                }
                int w = k.equals("0") ? s * 2 + 4 : s;
                int x = X0 + 8 + c * (s + 4), y = keysTop() + r * (s + 4);
                boolean hover = PhoneUI.in(mx, my, x, y, w, s);
                int col = c == 3 ? (k.equals(op) && fresh ? 0xFFFFFFFF : 0xFFFF9F0A) : r == 0 ? 0xFFA5A5A5 : 0xFF333333;
                PhoneUI.round(g, x, y, w, s, s / 2, hover ? PhoneUI.mix(col, 0xFFFFFFFF, .25f) : col);
                int tc = c == 3 && k.equals(op) && fresh ? 0xFFFF9F0A : r == 0 && c < 3 ? 0xFF000000 : 0xFFFFFFFF;
                PhoneUI.centered(g, k, k.equals("0") ? x + s / 2f : x + w / 2f, y + s / 2f - 5, tc, 1.2f);
            }
        }
    }

    private void press(String k) {
        switch (k) {
            case "AC" -> {
                shown = "0";
                op = null;
                held = 0;
                fresh = true;
            }
            case "±" -> shown = shown.startsWith("-") ? shown.substring(1) : shown.equals("0") ? shown : "-" + shown;
            case "%" -> shown = fmt(value() / 100);
            case "÷", "×", "−", "+" -> {
                if (op != null && !fresh) {
                    shown = fmt(apply(held, value(), op));
                }
                held = value();
                op = k;
                fresh = true;
            }
            case "=" -> {
                if (op != null) {
                    shown = fmt(apply(held, value(), op));
                    op = null;
                    fresh = true;
                }
            }
            case "." -> {
                if (fresh) {
                    shown = "0.";
                    fresh = false;
                } else if (!shown.contains(".")) {
                    shown += ".";
                }
            }
            default -> {
                if (fresh || shown.equals("0")) {
                    shown = k;
                    fresh = false;
                } else if (shown.length() < 14) {
                    shown += k;
                }
            }
        }
    }

    private double value() {
        try {
            return Double.parseDouble(shown);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double apply(double a, double b, String op) {
        return switch (op) {
            case "÷" -> b == 0 ? Double.NaN : a / b;
            case "×" -> a * b;
            case "−" -> a - b;
            default -> a + b;
        };
    }

    private static String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "Erreur";
        }
        String s = new BigDecimal(v).round(new MathContext(12)).stripTrailingZeros().toPlainString();
        return s.equals("-0") ? "0" : s;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        int s = size();
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 4; c++) {
                String k = KEYS[r][c];
                int w = k.equals("0") ? s * 2 + 4 : s;
                int x = X0 + 8 + c * (s + 4), y = keysTop() + r * (s + 4);
                if (!k.isEmpty() && PhoneUI.in(mx, my, x, y, w, s)) {
                    click();
                    press(k);
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean typed(char c, int mods) {
        String k = switch (c) {
            case '/' -> "÷";
            case '*', 'x' -> "×";
            case '-' -> "−";
            case '+' -> "+";
            case '=' -> "=";
            case '.', ',' -> ".";
            case '%' -> "%";
            default -> Character.isDigit(c) ? String.valueOf(c) : null;
        };
        if (k != null) {
            press(k);
            return true;
        }
        return false;
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            press("=");
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            press("AC");
            return true;
        }
        return false;
    }
}
