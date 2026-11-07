package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.client.phone.PhoneUI;
import com.waylays.arcanes.network.PhonePackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Banque d'Émeraude: an account of emeralds kept by the server: emeralds carried are put in, taken out, or sent to
 * another player (who has them even if away); a card of the account, its history.
 */
public class BanqueApp extends PhoneApp {
    private EditBox amount;
    private EditBox to;
    private int mode = -1;

    public BanqueApp() {
        super("banque");
    }

    @Override
    public void open() {
        PhoneClient.seen(id);
        amount = field(X0 + 14, Y0 + 98, 50, 6, "phone.arcanes_oublies.amount");
        to = field(X0 + 76, Y0 + 98, X1 - X0 - 90, PhonePackets.NAME, "phone.arcanes_oublies.to");
        amount.setFilter(s -> s.chars().allMatch(Character::isDigit));
        mode = -1;
        PhoneClient.ask(0, "");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF0C1C16, 0xFF06100C};
    }

    private int carried() {
        if (mc.player == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(Items.EMERALD)) {
                n += s.getCount();
            }
        }
        return n;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        // The card
        int cx = X0 + 8, cy = Y0 + 2, cw = X1 - X0 - 16, ch = 60;
        PhoneUI.round(g, cx, cy, cw, ch, 10, 0xFF0E8A5A);
        PhoneUI.round(g, cx, cy + ch / 2, cw, ch / 2, 10, 0xFF07563A);
        PhoneUI.round(g, cx, cy + ch / 2 - 6, cw, 12, 2, 0xFF0B7049);
        PhoneUI.glow(g, cx + cw - 20, cy + 14, 50, 0x5560FFB0);
        PhoneUI.text(g, "ÉMERAUDE · ARCANES", cx + 8, cy + 7, 0xDDFFFFFF, .5f);
        PhoneUI.round(g, cx + 8, cy + 16, 14, 10, 2, 0xFFE8C46A);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.balance").getString(), cx + 8, cy + 32, 0xCCFFFFFF, .55f);
        PhoneUI.text(g, PhoneClient.balance + " ◆", cx + 8, cy + 40, PhoneUI.WHITE, 1.4f);
        PhoneUI.right(g, mc.player != null ? mc.player.getGameProfile().getName() : "", cx + cw - 8, cy + ch - 10, 0xCCFFFFFF, .55f);
        PhoneUI.icon(g, "banque", cx + cw - 22, cy + 6, 14);
        // Carried, and the three actions
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.carried", carried()).getString(), X0 + 10, Y0 + 68, PhoneUI.DIM, .6f);
        int bw = (X1 - X0 - 24) / 3;
        String[] labels = {"deposit", "withdraw", "send"};
        int[] colors = {0xFF1E9A64, 0xFF2E6AC8, 0xFF8A5CFF};
        for (int k = 0; k < 3; k++) {
            int c = mode == k ? PhoneUI.mix(colors[k], 0xFFFFFFFF, .25f) : colors[k];
            button(g, X0 + 8 + k * (bw + 4), Y0 + 78, bw, 14, Component.translatable("phone.arcanes_oublies." + labels[k]).getString(), c, mx, my);
        }
        if (mode >= 0) {
            drawField(g, amount, mx, my, pt);
            if (mode == 2) {
                drawField(g, to, mx, my, pt);
            }
            button(g, X1 - 46, Y0 + 113, 38, 13, "OK", 0xFF1E9A64, mx, my);
        }
        // The history
        int y = Y0 + (mode >= 0 ? 132 : 100);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.history").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        for (PhonePackets.TxnView t : PhoneClient.history) {
            boolean in = t.amount() > 0;
            PhoneUI.round(g, X0 + 8, y, 14, 14, 7, in ? 0x3360E0A0 : 0x33FF6070);
            PhoneUI.centered(g, in ? "↓" : "↑", X0 + 15, y + 3, in ? 0xFF60E0A0 : 0xFFFF6070, .7f);
            String label = Component.translatable("phone.arcanes_oublies.txn." + t.kind(), t.other()).getString();
            PhoneUI.text(g, PhoneUI.fit(label, 80, .6f), X0 + 26, y + 2, PhoneUI.WHITE, .6f);
            PhoneUI.text(g, PhoneUI.ago(t.time()), X0 + 26, y + 9, PhoneUI.FAINT, .5f);
            PhoneUI.right(g, (in ? "+" : "") + t.amount() + " ◆", X1 - 10, y + 4, in ? 0xFF60E0A0 : 0xFFFF8090, .65f);
            y += 18;
            if (y > Y1 - 16) {
                break;
            }
        }
    }

    private void confirm() {
        int n;
        try {
            n = Integer.parseInt(amount.getValue());
        } catch (NumberFormatException e) {
            return;
        }
        if (n > 0 && mode >= 0) {
            PacketDistributor.sendToServer(new PhonePackets.Bank(mode, n, mode == 2 ? to.getValue().strip() : ""));
            amount.setValue("");
            click();
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        int bw = (X1 - X0 - 24) / 3;
        for (int k = 0; k < 3; k++) {
            if (PhoneUI.in(mx, my, X0 + 8 + k * (bw + 4), Y0 + 78, bw, 14)) {
                click();
                mode = mode == k ? -1 : k;
                amount.setFocused(mode >= 0);
                return true;
            }
        }
        if (mode >= 0) {
            if (PhoneUI.in(mx, my, X1 - 46, Y0 + 113, 38, 13)) {
                confirm();
                return true;
            }
            if (!clickField(amount, mx, my, button) && mode == 2) {
                clickField(to, mx, my, button);
            } else {
                to.setFocused(false);
            }
        }
        return true;
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (mode >= 0 && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            confirm();
            return true;
        }
        return key != GLFW.GLFW_KEY_ESCAPE && (feed(amount, key, scan, mods) || feed(to, key, scan, mods));
    }

    @Override
    public boolean typed(char c, int mods) {
        return feed(amount, c, mods) || feed(to, c, mods);
    }
}
