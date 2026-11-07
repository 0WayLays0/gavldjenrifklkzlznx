package com.waylays.eraphones.client.retro;

import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneHud;
import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.client.SnakeGame;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * A phone of menus (the 1997 GSM, the 2004 flip phone): the standby screen (a number typed there is dialled with the
 * green key), the menu, and its screens: messages (received, read, written letter by letter), the directory (add,
 * call, write, erase), the call log, Serpent, the calculator, the ringing (normal or silent), one's own number. Each
 * phone draws them in its own way; the call, when there is one, takes the screen.
 */
public abstract class MenuPhone extends RetroScreen {
    protected static final String IDLE = "idle", MENU = "menu", MESSAGES = "messages", INBOX = "inbox", THREAD = "thread", TO = "to", TEXT = "text",
            CONTACTS = "contacts", CONTACT = "contact", ADD_NAME = "add_name", ADD_NUMBER = "add_number", CALLS = "calls", SNAKE = "snake", CALC = "calc",
            RING = "ring", NUMBER = "number";

    protected String view = IDLE;
    protected int sel, menuSel, scroll;
    protected String dial = "", to = "", text = "", name = "", number = "", with = "";
    protected SnakeGame snake;
    private String flash = "";
    private long flashAt;
    // The calculator
    protected String calc = "0";
    private double calcAcc;
    private char calcOp;
    private boolean calcFresh = true;
    // Where the screen is this frame
    protected int x0, y0, w, h;

    protected MenuPhone(String texture, int bodyW, int bodyH) {
        super(texture, bodyW, bodyH);
    }

    // ---------------------------------------------------------------- what the phone draws in its own way

    /** The menu's entries (ids). */
    protected abstract String[] menu();

    protected abstract void background(GuiGraphics g, long now);

    protected abstract void idle(GuiGraphics g, long now);

    protected abstract void menuScreen(GuiGraphics g, long now);

    protected abstract void header(GuiGraphics g, String title);

    protected abstract void list(GuiGraphics g, List<String> items, List<String> right, int selected, int top);

    protected abstract void softkeys(GuiGraphics g, String left, String right);

    protected abstract void paragraph(GuiGraphics g, List<String> lines, int top);

    protected abstract void callScreen(GuiGraphics g, long now);

    protected abstract void snakeScreen(GuiGraphics g, long now);

    protected abstract float textScale();

    protected abstract int fg();

    /** A screen of the phone's own (camera, gallery, themes...), drawn; false when the view is not one. */
    protected boolean extraScreen(GuiGraphics g, long now) {
        return false;
    }

    /** A key on a screen of the phone's own; false when the view is not one. */
    protected boolean extraPress(String id) {
        return false;
    }

    /** Opens an entry of the menu that is the phone's own; false when it is not one. */
    protected boolean openExtra(String id) {
        return false;
    }

    protected int lineHeight() {
        return Math.round(10 * textScale());
    }

    protected int rows() {
        return Math.max(1, (h - lineHeight() * 2 - 6) / lineHeight());
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void screen(GuiGraphics g, int mx, int my, long now) {
        int[] b = screenBox();
        x0 = b[0];
        y0 = b[1];
        w = b[2] - b[0];
        h = b[3] - b[1];
        background(g, now);
        if (PhoneClient.inCall()) {
            callScreen(g, now);
        } else {
            draw(g, now);
        }
        if (now - flashAt < 1600 && !flash.isEmpty()) {
            int fw = Math.min(w - 8, PhoneUI.width(flash, textScale()) + 8);
            int fx = x0 + w / 2 - fw / 2, fy = y0 + h / 2 - 7;
            g.fill(fx - 1, fy - 1, fx + fw + 1, fy + 15, fg());
            flashBox(g, fx, fy, fw);
            PhoneUI.centered(g, PhoneUI.fit(flash, fw - 4, textScale()), x0 + w / 2f, fy + 4, fg(), textScale());
        }
    }

    protected void flashBox(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 14, 0xFFFFFFFF);
    }

    protected void flash(String key, Object... args) {
        flash = tr(key, args);
        flashAt = System.currentTimeMillis();
    }

    private void draw(GuiGraphics g, long now) {
        switch (view) {
            case IDLE -> idle(g, now);
            case "dial" -> {
                String shown = dial.length() > 3 ? dial.substring(0, 3) + "-" + dial.substring(3) : dial;
                float big = textScale() * 1.6f;
                PhoneUI.right(g, PhoneUI.fit(shown, w - 6, big), x0 + w - 4, y0 + h / 2f - 8, fg(), big);
                String n = Numbers.normalize(dial);
                if (n != null && !PhoneClient.name(n).equals(n)) {
                    PhoneUI.right(g, PhoneUI.fit(PhoneClient.name(n), w - 6, textScale()), x0 + w - 4, y0 + h / 2f + 6, fg(), textScale());
                }
                softkeys(g, tr("phone.eraphones.call"), tr("phone.eraphones.erase"));
            }
            case MENU -> menuScreen(g, now);
            case MESSAGES -> {
                header(g, tr("phone.eraphones.app.messages"));
                int unread = PhoneClient.badge("messages");
                list(g, List.of(tr("phone.eraphones.write"), tr("phone.eraphones.inbox")), List.of("", unread > 0 ? String.valueOf(unread) : ""), sel, 0);
                softkeys(g, tr("phone.eraphones.select"), tr("phone.eraphones.back"));
            }
            case INBOX -> {
                header(g, tr("phone.eraphones.inbox"));
                List<String> items = new ArrayList<>(), right = new ArrayList<>();
                for (PhoneNet.Conv c : PhoneClient.inbox) {
                    items.add((c.unread() > 0 ? "✉ " : "") + PhoneClient.name(c.number(), c.display()));
                    right.add(c.unread() > 0 ? String.valueOf(c.unread()) : "");
                }
                if (items.isEmpty()) {
                    paragraph(g, List.of(tr("phone.eraphones.empty")), 0);
                } else {
                    list(g, items, right, sel, scroll);
                }
                softkeys(g, tr("phone.eraphones.read"), tr("phone.eraphones.back"));
            }
            case THREAD -> {
                header(g, PhoneClient.name(with));
                List<String> lines = new ArrayList<>();
                PhoneNet.Thread t = PhoneClient.thread;
                if (t != null && t.number().equals(with)) {
                    for (PhoneNet.Line l : t.lines()) {
                        for (FormattedCharSequence s : PhoneUI.wrap((l.mine() ? "> " : "< ") + l.text(), w - 6, textScale())) {
                            StringBuilder sb = new StringBuilder();
                            s.accept((i, style, cp) -> {
                                sb.appendCodePoint(cp);
                                return true;
                            });
                            lines.add(sb.toString());
                        }
                    }
                }
                int max = Math.max(0, lines.size() - rows());
                scroll = Math.max(0, Math.min(scroll, max));
                int from = Math.max(0, max - scroll);
                paragraph(g, lines.subList(from, Math.min(lines.size(), from + rows())), 0);
                softkeys(g, Numbers.hidden(with) ? "" : tr("phone.eraphones.reply"), tr("phone.eraphones.back"));
            }
            case TO -> {
                header(g, tr("phone.eraphones.to_number"));
                paragraph(g, List.of(to + cursor(now)), 0);
                softkeys(g, tr("phone.eraphones.ok"), tr("phone.eraphones.erase"));
            }
            case TEXT -> {
                int max = PhoneClient.era == com.waylays.eraphones.Era.GSM ? PhoneNet.SMS_GSM : 200;
                header(g, (tapping() ? "abc " : "Abc ") + text.length() + "/" + max);
                List<String> lines = new ArrayList<>();
                for (FormattedCharSequence s : PhoneUI.wrap(text + cursor(now), w - 6, textScale())) {
                    StringBuilder sb = new StringBuilder();
                    s.accept((i, style, cp) -> {
                        sb.appendCodePoint(cp);
                        return true;
                    });
                    lines.add(sb.toString());
                }
                paragraph(g, lines.subList(Math.max(0, lines.size() - rows()), lines.size()), 0);
                softkeys(g, tr("phone.eraphones.send"), tr("phone.eraphones.erase"));
            }
            case CONTACTS -> {
                header(g, tr("phone.eraphones.directory"));
                List<String> items = new ArrayList<>(), right = new ArrayList<>();
                items.add("+ " + tr("phone.eraphones.add"));
                right.add("");
                for (PhoneNet.ContactView c : PhoneClient.contacts) {
                    items.add(c.name());
                    right.add("");
                }
                list(g, items, right, sel, scroll);
                softkeys(g, tr("phone.eraphones.select"), tr("phone.eraphones.back"));
            }
            case CONTACT -> {
                PhoneNet.ContactView c = contact();
                header(g, c == null ? "" : c.name());
                list(g, List.of(tr("phone.eraphones.call"), tr("phone.eraphones.write"), tr("phone.eraphones.remove")), List.of("", "", ""), sel, 0);
                if (c != null) {
                    PhoneUI.text(g, c.number(), x0 + 4, y0 + h - lineHeight() * 2 - 2, fg(), textScale() * .9f);
                }
                softkeys(g, tr("phone.eraphones.select"), tr("phone.eraphones.back"));
            }
            case ADD_NAME -> {
                header(g, tr("phone.eraphones.contact_name"));
                paragraph(g, List.of(name + cursor(now)), 0);
                softkeys(g, tr("phone.eraphones.ok"), tr("phone.eraphones.erase"));
            }
            case ADD_NUMBER -> {
                header(g, tr("phone.eraphones.contact_number"));
                paragraph(g, List.of(name, number + cursor(now)), 0);
                softkeys(g, tr("phone.eraphones.save"), tr("phone.eraphones.erase"));
            }
            case CALLS -> {
                header(g, tr("phone.eraphones.recents"));
                List<String> items = new ArrayList<>(), right = new ArrayList<>();
                for (PhoneNet.CallView c : PhoneClient.calls) {
                    String arrow = c.kind() == 0 ? "↗" : c.kind() == 2 ? "!" : "↙";
                    items.add(arrow + " " + PhoneClient.name(c.number(), c.display()));
                    right.add(c.seconds() > 0 ? PhoneHud.timer(c.seconds() * 1000L) : "");
                }
                if (items.isEmpty()) {
                    paragraph(g, List.of(tr("phone.eraphones.empty")), 0);
                } else {
                    list(g, items, right, sel, scroll);
                }
                softkeys(g, tr("phone.eraphones.call"), tr("phone.eraphones.back"));
            }
            case SNAKE -> snakeScreen(g, now);
            case CALC -> {
                header(g, tr("phone.eraphones.app.calcul"));
                PhoneUI.right(g, (calcOp != 0 ? calcOp + " " : "") + calc, x0 + w - 4, y0 + h / 2f - 6, fg(), textScale() * 1.4f);
                PhoneUI.text(g, "* = + − × ÷   # = ,", x0 + 4, y0 + h - lineHeight() * 2 - 2, fg(), textScale() * .75f);
                softkeys(g, "=", tr("phone.eraphones.erase"));
            }
            case RING -> {
                header(g, tr("phone.eraphones.ringing"));
                list(g, List.of(tr("phone.eraphones.profile.normal"), tr("phone.eraphones.profile.silent")),
                        List.of(PhoneClient.silent ? "" : "✓", PhoneClient.silent ? "✓" : ""), sel, 0);
                softkeys(g, tr("phone.eraphones.select"), tr("phone.eraphones.back"));
            }
            case NUMBER -> {
                header(g, tr("phone.eraphones.my_number"));
                paragraph(g, List.of("", PhoneClient.number), 0);
                softkeys(g, "", tr("phone.eraphones.back"));
            }
            default -> {
                if (!extraScreen(g, now)) {
                    view = IDLE;
                }
            }
        }
    }

    protected String cursor(long now) {
        return now % 1000 < 500 ? "_" : " ";
    }

    // ---------------------------------------------------------------- keys

    @Override
    protected boolean writing() {
        return !PhoneClient.inCall() && (view.equals(TEXT) || view.equals(ADD_NAME));
    }

    @Override
    protected boolean write(char c) {
        if (view.equals(TEXT)) {
            int max = PhoneClient.era == com.waylays.eraphones.Era.GSM ? PhoneNet.SMS_GSM : 200;
            if (text.length() < max && c >= ' ') {
                text += c;
            }
            endTap();
            return true;
        }
        if (view.equals(ADD_NAME)) {
            if (name.length() < 24 && c >= ' ') {
                name += c;
            }
            endTap();
            return true;
        }
        return false;
    }

    protected void go(String v) {
        view = v;
        sel = 0;
        scroll = 0;
    }

    private PhoneNet.ContactView contact() {
        int i = sel - 1;
        List<PhoneNet.ContactView> c = PhoneClient.contacts;
        return i >= 0 && i < c.size() ? c.get(i) : chosen;
    }

    private PhoneNet.ContactView chosen;

    protected void open(String id) {
        switch (id) {
            case "messages" -> go(MESSAGES);
            case "contacts" -> {
                go(CONTACTS);
                PhoneClient.ask(10, "");
            }
            case "calls" -> {
                go(CALLS);
                PhoneClient.seen("telephone");
                PhoneClient.ask(9, "");
            }
            case "snake" -> {
                go(SNAKE);
                snake = new SnakeGame(snakeW(), snakeH(), 160);
            }
            case "calc" -> {
                go(CALC);
                calc = "0";
                calcOp = 0;
                calcFresh = true;
            }
            case "ring" -> go(RING);
            case "number" -> go(NUMBER);
            default -> openExtra(id);
        }
    }

    protected int snakeW() {
        return 40;
    }

    protected int snakeH() {
        return 24;
    }

    protected int listSize() {
        return switch (view) {
            case MESSAGES -> 2;
            case INBOX -> PhoneClient.inbox.size();
            case CONTACTS -> PhoneClient.contacts.size() + 1;
            case CONTACT -> 3;
            case CALLS -> PhoneClient.calls.size();
            case RING -> 2;
            default -> 0;
        };
    }

    private void move(int d) {
        int n = listSize();
        if (n == 0) {
            return;
        }
        sel = Math.floorMod(sel + d, n);
        int rows = rows();
        if (sel < scroll) {
            scroll = sel;
        } else if (sel >= scroll + rows) {
            scroll = sel - rows + 1;
        }
    }

    @Override
    protected void press(String id) {
        if (PhoneClient.inCall()) {
            return;
        }
        if (extraPress(id)) {
            return;
        }
        boolean digit = id.length() == 1 && "0123456789*#".contains(id);
        switch (view) {
            case IDLE -> {
                if (digit) {
                    dial = id;
                    view = "dial";
                } else if (id.equals("ok") || id.equals("left")) {
                    go(MENU);
                    sel = menuSel;
                } else if (id.equals("right")) {
                    open("contacts");
                } else if (id.equals("call")) {
                    open("calls");
                } else if (id.equals("down") || id.equals("up")) {
                    open("contacts");
                }
            }
            case "dial" -> {
                if (digit && dial.length() < 12) {
                    dial += id;
                } else if (id.equals("back")) {
                    dial = dial.isEmpty() ? "" : dial.substring(0, dial.length() - 1);
                    if (dial.isEmpty()) {
                        go(IDLE);
                    }
                } else if (id.equals("call") || id.equals("ok")) {
                    PhoneClient.dial(dial);
                    dial = "";
                    go(IDLE);
                } else if (id.equals("end")) {
                    dial = "";
                    go(IDLE);
                }
            }
            case MENU -> {
                String[] m = menu();
                if (id.equals("up") || id.equals("left")) {
                    sel = Math.floorMod(sel - 1, m.length);
                } else if (id.equals("down") || id.equals("right")) {
                    sel = Math.floorMod(sel + 1, m.length);
                } else if (id.equals("ok")) {
                    menuSel = sel;
                    open(m[sel]);
                } else if (digit && !id.equals("*") && !id.equals("#")) {
                    int k = Integer.parseInt(id) - 1;
                    if (k >= 0 && k < m.length) {
                        menuSel = k;
                        open(m[k]);
                    }
                } else if (id.equals("back") || id.equals("end")) {
                    go(IDLE);
                }
            }
            case MESSAGES -> {
                if (id.equals("up")) {
                    move(-1);
                } else if (id.equals("down")) {
                    move(1);
                } else if (id.equals("ok")) {
                    if (sel == 0) {
                        to = "";
                        go(TO);
                    } else {
                        go(INBOX);
                        PhoneClient.ask(11, "");
                    }
                } else if (id.equals("back")) {
                    go(MENU);
                    sel = menuSel;
                }
            }
            case INBOX -> {
                if (id.equals("up")) {
                    move(-1);
                } else if (id.equals("down")) {
                    move(1);
                } else if (id.equals("ok") && sel < PhoneClient.inbox.size()) {
                    with = PhoneClient.inbox.get(sel).number();
                    PhoneClient.thread = null;
                    PhoneClient.ask(1, with);
                    go(THREAD);
                } else if (id.equals("back")) {
                    go(MESSAGES);
                    sel = 1;
                }
            }
            case THREAD -> {
                if (id.equals("up")) {
                    scroll++;
                } else if (id.equals("down")) {
                    scroll = Math.max(0, scroll - 1);
                } else if (id.equals("ok") && !Numbers.hidden(with)) {
                    to = with;
                    text = "";
                    go(TEXT);
                } else if (id.equals("call") && !Numbers.hidden(with)) {
                    PhoneClient.dial(with);
                } else if (id.equals("back")) {
                    go(INBOX);
                    PhoneClient.ask(11, "");
                }
            }
            case TO -> {
                if (digit && !id.equals("*") && !id.equals("#") && to.length() < 8) {
                    to += id;
                } else if (id.equals("back")) {
                    if (to.isEmpty()) {
                        go(MESSAGES);
                    } else {
                        to = to.substring(0, to.length() - 1);
                    }
                } else if (id.equals("ok")) {
                    String n = Numbers.normalize(to);
                    if (n == null) {
                        flash("phone.eraphones.bad_number_short");
                    } else {
                        to = n;
                        text = "";
                        go(TEXT);
                    }
                }
            }
            case TEXT -> {
                int max = PhoneClient.era == com.waylays.eraphones.Era.GSM ? PhoneNet.SMS_GSM : 200;
                if (digit && !id.equals("*") && !id.equals("#")) {
                    text = tap(text, id, max);
                } else if (id.equals("#")) {
                    text = text.length() < max ? text + " " : text;
                    endTap();
                } else if (id.equals("back")) {
                    if (text.isEmpty()) {
                        go(MESSAGES);
                    } else {
                        text = text.substring(0, text.length() - 1);
                    }
                    endTap();
                } else if (id.equals("ok") && !text.isBlank()) {
                    PhoneClient.send(new PhoneNet.Sms(to, text));
                    flash("phone.eraphones.sent");
                    text = "";
                    go(MESSAGES);
                }
            }
            case CONTACTS -> {
                if (id.equals("up")) {
                    move(-1);
                } else if (id.equals("down")) {
                    move(1);
                } else if (id.equals("ok")) {
                    if (sel == 0) {
                        name = "";
                        number = "";
                        go(ADD_NAME);
                    } else {
                        chosen = contact();
                        go(CONTACT);
                    }
                } else if (id.equals("call") && sel > 0) {
                    PhoneNet.ContactView c = contact();
                    if (c != null) {
                        PhoneClient.dial(c.number());
                    }
                } else if (id.equals("back")) {
                    go(MENU);
                    sel = menuSel;
                }
            }
            case CONTACT -> {
                PhoneNet.ContactView c = chosen;
                if (id.equals("up")) {
                    move(-1);
                } else if (id.equals("down")) {
                    move(1);
                } else if ((id.equals("ok") || id.equals("call")) && c != null) {
                    if (sel == 0 || id.equals("call")) {
                        PhoneClient.dial(c.number());
                    } else if (sel == 1) {
                        to = c.number();
                        text = "";
                        go(TEXT);
                    } else {
                        PhoneClient.send(new PhoneNet.ContactEdit(c.name(), c.number(), true));
                        flash("phone.eraphones.removed");
                        go(CONTACTS);
                    }
                } else if (id.equals("back")) {
                    go(CONTACTS);
                }
            }
            case ADD_NAME -> {
                if (digit && !id.equals("*") && !id.equals("#")) {
                    name = tap(name, id, 24);
                } else if (id.equals("#")) {
                    name = name + " ";
                    endTap();
                } else if (id.equals("back")) {
                    if (name.isEmpty()) {
                        go(CONTACTS);
                    } else {
                        name = name.substring(0, name.length() - 1);
                    }
                    endTap();
                } else if (id.equals("ok") && !name.isBlank()) {
                    number = "";
                    view = ADD_NUMBER;
                }
            }
            case ADD_NUMBER -> {
                if (digit && !id.equals("*") && !id.equals("#") && number.length() < 8) {
                    number += id;
                } else if (id.equals("back")) {
                    if (number.isEmpty()) {
                        view = ADD_NAME;
                    } else {
                        number = number.substring(0, number.length() - 1);
                    }
                } else if (id.equals("ok")) {
                    String n = Numbers.normalize(number);
                    if (n == null) {
                        flash("phone.eraphones.bad_number_short");
                    } else {
                        PhoneClient.send(new PhoneNet.ContactEdit(name.strip(), n, false));
                        flash("phone.eraphones.saved");
                        go(CONTACTS);
                    }
                }
            }
            case CALLS -> {
                if (id.equals("up")) {
                    move(-1);
                } else if (id.equals("down")) {
                    move(1);
                } else if ((id.equals("ok") || id.equals("call")) && sel < PhoneClient.calls.size()) {
                    String n = PhoneClient.calls.get(sel).number();
                    if (!Numbers.hidden(n)) {
                        PhoneClient.dial(n);
                    }
                } else if (id.equals("back")) {
                    go(MENU);
                    sel = menuSel;
                }
            }
            case SNAKE -> {
                switch (id) {
                    case "2", "up" -> snake.turn(0, -1);
                    case "8", "down" -> snake.turn(0, 1);
                    case "4", "left" -> snake.turn(-1, 0);
                    case "6", "right" -> snake.turn(1, 0);
                    case "ok", "5" -> {
                        if (snake.over) {
                            snake = new SnakeGame(snakeW(), snakeH(), 160);
                        }
                    }
                    case "back" -> {
                        go(MENU);
                        sel = menuSel;
                    }
                    default -> {
                    }
                }
            }
            case CALC -> calcKey(id);
            case RING -> {
                if (id.equals("up")) {
                    move(-1);
                } else if (id.equals("down")) {
                    move(1);
                } else if (id.equals("ok")) {
                    PhoneClient.silent = sel == 1;
                    PhoneClient.ask(PhoneClient.silent ? 7 : 8, "");
                    flash(PhoneClient.silent ? "phone.eraphones.profile.silent" : "phone.eraphones.profile.normal");
                } else if (id.equals("back")) {
                    go(MENU);
                    sel = menuSel;
                }
            }
            case NUMBER -> {
                if (id.equals("back") || id.equals("ok")) {
                    go(MENU);
                    sel = menuSel;
                }
            }
            default -> {
            }
        }
        if (id.equals("end") && !view.equals(IDLE)) {
            dial = "";
            go(IDLE);
        }
    }

    private void calcKey(String id) {
        if (id.length() == 1 && Character.isDigit(id.charAt(0))) {
            calc = calcFresh || calc.equals("0") ? id : calc.length() < 10 ? calc + id : calc;
            calcFresh = false;
        } else if (id.equals("#")) {
            if (!calc.contains(".")) {
                calc = (calcFresh ? "0" : calc) + ".";
                calcFresh = false;
            }
        } else if (id.equals("*")) {
            if (!calcFresh) {
                compute();
            }
            calcOp = switch (calcOp) {
                case '+' -> '−';
                case '−' -> '×';
                case '×' -> '÷';
                default -> '+';
            };
            calcFresh = true;
        } else if (id.equals("ok")) {
            compute();
            calcOp = 0;
            calcFresh = true;
        } else if (id.equals("back")) {
            if (calcFresh || calc.length() <= 1) {
                if (calc.equals("0") && calcOp == 0) {
                    go(MENU);
                    sel = menuSel;
                }
                calc = "0";
                calcOp = 0;
                calcAcc = 0;
                calcFresh = true;
            } else {
                calc = calc.substring(0, calc.length() - 1);
            }
        }
    }

    private void compute() {
        double v;
        try {
            v = Double.parseDouble(calc);
        } catch (NumberFormatException e) {
            v = 0;
        }
        double r = switch (calcOp) {
            case '+' -> calcAcc + v;
            case '−' -> calcAcc - v;
            case '×' -> calcAcc * v;
            case '÷' -> v == 0 ? Double.NaN : calcAcc / v;
            default -> v;
        };
        calcAcc = r;
        if (Double.isNaN(r) || Double.isInfinite(r)) {
            calc = "E";
        } else if (r == Math.rint(r) && Math.abs(r) < 1e10) {
            calc = String.valueOf((long) r);
        } else {
            calc = String.format(java.util.Locale.ROOT, "%.6g", r).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }

    /** Keeps the best score of Serpent. */
    protected void snakeScore() {
        PhoneSettings s = PhoneSettings.get();
        if (snake != null && snake.over && snake.score > s.snakeBest) {
            s.snakeBest = snake.score;
            s.save();
        }
    }
}
