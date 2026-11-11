package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.NoteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.waylays.eraphones.compat.PacketDistributor;

/**
 * Writing on a sheet of paper: the page on the left, as it will look, changing as one writes; on the right its title,
 * its text, the paper (five), the ink (five), by hand or typed; then kept as a draft, or signed (it can then only be
 * read).
 */
public class NoteEditScreen extends Screen {
    private final InteractionHand hand;
    private EditBox title;
    private MultiLineEditBox text;
    private int style, ink;
    private boolean typed;
    private float pageScale, pageX, pageY;
    private int panelX, panelW, inkY;

    public NoteEditScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("item.eraphones.feuille"));
        this.hand = hand;
        NotePage.Data d = NotePage.Data.of(stack);
        this.style = d.style();
        this.ink = d.ink();
        this.typed = d.typed();
        this.initialTitle = d.title();
        this.initialText = d.text();
    }

    private final String initialTitle, initialText;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    @Override
    protected void init() {
        String keepTitle = title == null ? initialTitle : title.getValue();
        String keepText = text == null ? initialText : text.getValue();
        pageScale = Math.min((height - 30f) / NotePage.H, (width * .48f) / NotePage.W);
        panelW = Math.min(230, (int) (width - NotePage.W * pageScale - 50));
        float total = NotePage.W * pageScale + 20 + panelW;
        pageX = (width - total) / 2f;
        pageY = (height - NotePage.H * pageScale) / 2f;
        panelX = (int) (pageX + NotePage.W * pageScale + 20);
        int y = (int) pageY + 4;
        title = new EditBox(font, panelX, y, panelW, 18, tr("note.eraphones.title"));
        title.setMaxLength(GearNet.NOTE_TITLE);
        title.setHint(tr("note.eraphones.title"));
        title.setValue(keepTitle);
        addRenderableWidget(title);
        y += 24;
        int textH = Math.max(60, Math.min(150, height - y - 128));
        text = new MultiLineEditBox(font, panelX, y, panelW, textH, tr("note.eraphones.text"), tr("note.eraphones.text"));
        text.setCharacterLimit(GearNet.NOTE_TEXT);
        text.setValue(keepText);
        addRenderableWidget(text);
        // Room under the box for its character count
        y += textH + 14;
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> style = Math.floorMod(style - 1, NotePage.STYLES.length))
                .bounds(panelX, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> style = Math.floorMod(style + 1, NotePage.STYLES.length))
                .bounds(panelX + panelW - 20, y, 20, 20).build());
        y += 24;
        inkY = y;
        y += 22;
        addRenderableWidget(Button.builder(handLabel(), b -> {
            typed = !typed;
            b.setMessage(handLabel());
        }).bounds(panelX, y, panelW, 20).build());
        y += 24;
        int half = (panelW - 4) / 2;
        addRenderableWidget(Button.builder(tr("note.eraphones.keep"), b -> send(false)).bounds(panelX, y, half, 20).build());
        addRenderableWidget(Button.builder(tr("note.eraphones.sign"), b -> send(true)).bounds(panelX + half + 4, y, half, 20).build());
        setInitialFocus(text);
    }

    private Component handLabel() {
        return tr(typed ? "note.eraphones.typed" : "note.eraphones.handwritten");
    }

    private void send(boolean sign) {
        ItemStack held = Minecraft.getInstance().player.getItemInHand(hand);
        if (held.getItem() instanceof NoteItem && !NoteItem.signed(held)) {
            PacketDistributor.sendToServer(new GearNet.Note(hand == InteractionHand.OFF_HAND, title.getValue(), text.getValue(), style, ink, typed, sign));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        // The page as it will look
        g.pose().pushPose();
        g.pose().translate(pageX, pageY, 0);
        g.pose().scale(pageScale, pageScale, 1);
        NotePage.draw(g, 0, 0, new NotePage.Data(title.getValue(), text.getValue(), style, ink, typed, ""), Integer.MAX_VALUE);
        g.pose().popPose();
        // The paper's name between its arrows, the inks
        int sy = inkY - 24;
        PhoneUI.centered(g, tr("note.eraphones.style." + NotePage.STYLES[style]).getString(), panelX + panelW / 2f, sy + 6, 0xFFFFFFFF, 1);
        int n = NotePage.INKS.length, size = 16, gap = (panelW - n * size) / (n - 1);
        for (int i = 0; i < n; i++) {
            int x = panelX + i * (size + gap);
            boolean on = i == ink;
            PhoneUI.round(g, x - 2, inkY - 2, size + 4, size + 4, 6, on ? 0xFFFFFFFF : 0x40FFFFFF);
            PhoneUI.round(g, x, inkY, size, size, 5, NotePage.INKS[i]);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int n = NotePage.INKS.length, size = 16, gap = (panelW - n * size) / (n - 1);
        for (int i = 0; i < n; i++) {
            int x = panelX + i * (size + gap);
            if (mx >= x && mx < x + size && my >= inkY && my < inkY + size) {
                ink = i;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
