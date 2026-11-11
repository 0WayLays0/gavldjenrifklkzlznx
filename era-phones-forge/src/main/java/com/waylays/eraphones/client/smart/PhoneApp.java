package com.waylays.eraphones.client.smart;

import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * An app of the smartphone: it draws inside the phone's screen (phone coordinates), and is given the clicks, the keys
 * and the scroll while it is open. Its header holds its title and the way back.
 */
public abstract class PhoneApp {
    /** The app's content area: below the status bar and the header, above the home bar. */
    public static final int X0 = SmartScreen.SX, X1 = SmartScreen.SX + SmartScreen.SW, TOP = SmartScreen.SY + 18, Y0 = TOP + 20,
            Y1 = SmartScreen.SY + SmartScreen.SH - 12;

    public final String id;
    protected SmartScreen phone;
    protected final Minecraft mc = Minecraft.getInstance();
    protected float scroll;

    protected PhoneApp(String id) {
        this.id = id;
    }

    void attach(SmartScreen phone) {
        this.phone = phone;
    }

    public Component title() {
        return Component.translatable("phone.eraphones.app." + id);
    }

    /** Opened (again). */
    public void open() {
    }

    public void tick() {
    }

    /** Draws the app; {@code mx}, {@code my} in phone coordinates. */
    public abstract void render(GuiGraphics g, int mx, int my, float partialTicks);

    public boolean click(double mx, double my, int button) {
        return false;
    }

    public boolean scrolled(double mx, double my, double dy) {
        scroll = Math.max(0, scroll - (float) dy * 14);
        return true;
    }

    public boolean key(int key, int scan, int mods) {
        return false;
    }

    public boolean typed(char c, int mods) {
        return false;
    }

    /** Back within the app (a conversation to the list...); false when the app should close. */
    public boolean back() {
        return false;
    }

    /** Whether the world shows through the screen instead of the app's background (the camera). */
    public boolean seeThrough() {
        return false;
    }

    /** The app's background colour (top, bottom). */
    public int[] background() {
        return new int[] {0xFF15111F, 0xFF0B0912};
    }

    /** The header: a back chevron, the title, a button on the right if {@code action} is given. */
    protected void header(GuiGraphics g, Component title, int mx, int my, String action) {
        boolean hb = PhoneUI.in(mx, my, X0 + 2, TOP, 30, 18);
        PhoneUI.text(g, "‹", X0 + 6, TOP + 3, hb ? PhoneUI.WHITE : PhoneUI.ACCENT, 1.6f);
        PhoneUI.centered(g, title.getString(), (X0 + X1) / 2f, TOP + 6, PhoneUI.WHITE, 1f);
        if (action != null) {
            boolean ha = PhoneUI.in(mx, my, X1 - 26, TOP, 24, 18);
            PhoneUI.right(g, action, X1 - 8, TOP + 5, ha ? PhoneUI.WHITE : PhoneUI.ACCENT, 1.2f);
        }
        g.fill(X0 + 6, TOP + 18, X1 - 6, TOP + 19, 0x22FFFFFF);
    }

    protected boolean headerBack(double mx, double my) {
        return PhoneUI.in(mx, my, X0 + 2, TOP, 30, 18);
    }

    protected boolean headerAction(double mx, double my) {
        return PhoneUI.in(mx, my, X1 - 26, TOP, 24, 18);
    }

    /** A text field placed in the phone's coordinates (drawn and fed by the app). */
    protected EditBox field(int x, int y, int w, int max, String hint) {
        EditBox box = new EditBox(mc.font, x, y, w, 12, Component.empty());
        box.setBordered(false);
        box.setMaxLength(max);
        box.setTextColor(PhoneUI.WHITE);
        box.setHint(Component.translatable(hint).withStyle(st -> st.withColor((0xFF8A86A0) & 0xFFFFFF)));
        return box;
    }

    /** Draws a field on a glass pill. */
    protected void drawField(GuiGraphics g, EditBox box, int mx, int my, float pt) {
        PhoneUI.round(g, box.getX() - 5, box.getY() - 3, box.getWidth() + 10, 14, 7, box.isFocused() ? 0x55FFFFFF : 0x33FFFFFF);
        box.render(g, mx, my, pt);
    }

    /** Clicks a field: focuses it if hit (unfocuses otherwise). */
    protected boolean clickField(EditBox box, double mx, double my, int button) {
        boolean hit = PhoneUI.in(mx, my, box.getX() - 5, box.getY() - 3, box.getWidth() + 10, 14);
        box.setFocused(hit);
        if (hit) {
            box.mouseClicked(mx, my, button);
        }
        return hit;
    }

    protected boolean feed(EditBox box, int key, int scan, int mods) {
        return box.isFocused() && box.keyPressed(key, scan, mods);
    }

    protected boolean feed(EditBox box, char c, int mods) {
        return box.isFocused() && box.charTyped(c, mods);
    }

    /** A pill button; returns whether the mouse is over it. */
    protected boolean button(GuiGraphics g, int x, int y, int w, int h, String label, int color, int mx, int my) {
        boolean hover = PhoneUI.in(mx, my, x, y, w, h);
        PhoneUI.round(g, x, y, w, h, h / 2, hover ? PhoneUI.mix(color, 0xFFFFFFFF, .2f) : color);
        PhoneUI.centered(g, label, x + w / 2f, y + (h - 7) / 2f, PhoneUI.WHITE, .85f);
        return hover;
    }

    /** Clips drawing to the content area (from {@code y0}). */
    protected void clip(GuiGraphics g, int y0, int y1) {
        phone.clip(g, X0, y0, X1, y1);
    }

    protected void unclip(GuiGraphics g) {
        g.disableScissor();
    }

    protected void click() {
        phone.clickSound();
    }
}
