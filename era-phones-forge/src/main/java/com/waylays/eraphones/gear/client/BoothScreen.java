package com.waylays.eraphones.gear.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneHud;
import com.waylays.eraphones.gear.BoothNet;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A phone booth's payphone, held up close: its steel face, the green LCD saying where the call stands, the chrome
 * keypad (clicked, or typed on the keyboard), the coin slot, the handset on its cradle (lifted while a call goes on);
 * beside it, while talking, what is said both ways and a line to say something; otherwise the booth's own number.
 */
public class BoothScreen extends Screen {
    private static final ResourceLocation PANEL = EraPhones.id("textures/gui/gear/publiphone.png");
    private static final ResourceLocation PIECES = EraPhones.id("textures/gui/gear/publiphone_pieces.png");
    private static final int PW = 230, PH = 250, SIDE = 180, GAP = 10;
    private static final int KX = 56, KY = 74, KW = 34, KH = 22, KGX = 8, KGY = 6;
    private static final int BY = 184, BW = 56, BH = 20, CALL_X = 56, HANG_X = 118;
    private static final String KEYS = "123456789*0#";
    private static final int LCD = 0xFF9CF5A6, LCD_DIM = 0xFF5FA868, LCD_SHADOW = 0xFF1A3A1E;

    private final BlockPos pos;
    private final String number;
    private String dialed = "", said = "";
    private String flash = "";
    private long flashUntil, pressedAt;
    private int pressed = -1;
    private float scale = 1;
    private int ox, oy;

    public BoothScreen(BlockPos pos, String number) {
        super(Component.translatable("block.eraphones.cabine_telephonique"));
        this.pos = pos;
        this.number = number;
    }

    public static void open(BoothNet.Open p) {
        // Picked up to answer, with voice chat: just talk, the call is shown at the top of the screen
        if (p.answered() && PhoneClient.voiceChat()) {
            return;
        }
        Minecraft.getInstance().setScreen(new BoothScreen(p.pos(), p.number()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int total = PW + GAP + SIDE;
        scale = Math.min(1f, Math.min((width - 16) / (float) total, (height - 16) / (float) PH));
        ox = (int) ((width - total * scale) / 2);
        oy = (int) ((height - PH * scale) / 2);
    }

    // ---------------------------------------------------------------- where the call stands

    /** This booth's call state: 0 none (or over), 1 calling, 3 talking. */
    private int state() {
        return number.equals(PhoneClient.callMine) ? PhoneClient.callState : 0;
    }

    private boolean justOver() {
        return number.equals(PhoneClient.callMine) && PhoneClient.callState == 0 && System.currentTimeMillis() - PhoneClient.callEndedAt < 3000;
    }

    private boolean inCall() {
        return state() == 1 || state() == 3;
    }

    private static Item coin() {
        return BoothNet.coin();
    }

    private static int coins() {
        Player p = Minecraft.getInstance().player;
        if (p == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(coin())) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    private void say(String key) {
        flash = Component.translatable(key).getString();
        flashUntil = System.currentTimeMillis() + 2200;
    }

    // ---------------------------------------------------------------- what the user does

    private void press(int k) {
        pressed = k;
        pressedAt = System.currentTimeMillis();
        click(GearRegistry.BOOTH_KEY.get(), .9f + (k % 3) * .06f + (k / 3) * .03f);
        if (!inCall() && dialed.length() < 12) {
            dialed += KEYS.charAt(k);
        }
    }

    private void call() {
        if (inCall()) {
            return;
        }
        if (dialed.isEmpty()) {
            say("booth.eraphones.dial_first");
            return;
        }
        Player p = Minecraft.getInstance().player;
        if (p != null && !p.getAbilities().instabuild && coins() <= 0) {
            say("booth.eraphones.no_coin");
            return;
        }
        PhoneClient.send(new BoothNet.Dial(pos, dialed));
        dialed = "";
    }

    private void hangUp() {
        if (inCall()) {
            PhoneClient.hangUp();
        }
        dialed = "";
        said = "";
        click(net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, 1.8f);
    }

    private static void click(net.minecraft.sounds.SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, .6f));
    }

    private double ux(double mx) {
        return (mx - ox) / scale;
    }

    private double uy(double my) {
        return (my - oy) / scale;
    }

    private static boolean in(double x, double y, int x0, int y0, int w, int h) {
        return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double x = ux(mx), y = uy(my);
        for (int k = 0; k < 12; k++) {
            if (in(x, y, KX + (k % 3) * (KW + KGX), KY + (k / 3) * (KH + KGY), KW, KH)) {
                press(k);
                return true;
            }
        }
        if (in(x, y, CALL_X, BY, BW, BH)) {
            call();
            return true;
        }
        if (in(x, y, HANG_X, BY, BW, BH) || in(x, y, 8, 70, 36, 136)) {
            hangUp();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (state() == 3) {
                if (!said.isBlank()) {
                    PhoneClient.speak(said);
                }
                said = "";
            } else {
                call();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (state() == 3) {
                said = said.isEmpty() ? said : said.substring(0, said.length() - 1);
            } else if (!dialed.isEmpty()) {
                dialed = dialed.substring(0, dialed.length() - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            hangUp();
            return true;
        }
        // Typing goes to what is said while talking; the inventory key must not close the payphone then
        if (state() == 3 && minecraft != null && minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(key, scan))) {
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (state() == 3) {
            if (c >= ' ' && said.length() < 200) {
                said += c;
            }
            return true;
        }
        int k = KEYS.indexOf(c);
        if (k >= 0) {
            press(k);
            return true;
        }
        return super.charTyped(c, mods);
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x88000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        long now = System.currentTimeMillis();
        double x = ux(mx), y = uy(my);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(scale, scale, 1);
        g.blit(PANEL, 0, 0, PW, PH, 0, 0, PW * 2, PH * 2, 460, 500);
        lcd(g, now);
        // The keys: up, pressed (for a moment after a click), lit under the pointer
        for (int k = 0; k < 12; k++) {
            int kx = KX + (k % 3) * (KW + KGX), ky = KY + (k / 3) * (KH + KGY);
            boolean down = k == pressed && now - pressedAt < 140;
            g.blit(PIECES, kx, ky + (down ? 1 : 0), KW, KH, 128, down ? 96 : 0, KW * 2, KH * 2, 256, 512);
            if (in(x, y, kx, ky, KW, KH)) {
                g.fill(kx + 1, ky + 1, kx + KW - 1, ky + KH - 1, 0x22FFFFFF);
            }
            String s = String.valueOf(KEYS.charAt(k));
            g.drawString(font, s, kx + (KW - font.width(s)) / 2, ky + (KH - 8) / 2 + (down ? 2 : 1), 0xFF26282E, false);
        }
        // The green and red buttons
        button(g, CALL_X, 192, "booth.eraphones.call", in(x, y, CALL_X, BY, BW, BH), !inCall() && !dialed.isEmpty());
        button(g, HANG_X, 272, "booth.eraphones.hang_up", in(x, y, HANG_X, BY, BW, BH), true);
        // The handset: on its cradle, or lifted off it while a call goes on
        if (inCall()) {
            g.pose().pushPose();
            g.pose().translate(-26, 84, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-14));
            g.blit(PIECES, 0, 0, 32, 120, 0, 0, 64, 240, 256, 512);
            g.pose().popPose();
        } else {
            g.blit(PIECES, 10, 78, 32, 120, 0, 0, 64, 240, 256, 512);
        }
        side(g, now);
        g.pose().popPose();
    }

    private void button(GuiGraphics g, int bx, int v, String key, boolean hover, boolean ready) {
        g.blit(PIECES, bx, BY, BW, BH, 128, v, BW * 2, BH * 2, 256, 512);
        if (hover) {
            g.fill(bx + 2, BY + 2, bx + BW - 2, BY + BH - 2, 0x26FFFFFF);
        }
        String s = Component.translatable(key).getString();
        g.drawString(font, s, bx + (BW - font.width(s)) / 2, BY + 6, ready ? 0xFFFFFFFF : 0xB0FFFFFF, true);
    }

    /** The LCD: two lines, glowing green. */
    private void lcd(GuiGraphics g, long now) {
        int st = state();
        String top, bottom;
        if (now < flashUntil) {
            top = flash;
            bottom = dialed;
        } else if (st == 1) {
            top = Component.translatable("booth.eraphones.calling").getString() + ".".repeat((int) (now / 400 % 4));
            bottom = PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
        } else if (st == 3) {
            top = Component.translatable("booth.eraphones.talking").getString() + "  " + PhoneHud.timer(now - PhoneClient.callSince);
            bottom = PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
        } else if (justOver()) {
            top = Component.translatable("phone.eraphones.call_end." + PhoneClient.callReason).getString();
            bottom = PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
        } else if (dialed.isEmpty()) {
            top = Component.translatable("booth.eraphones.insert").getString();
            bottom = now / 500 % 2 == 0 ? "_" : "";
        } else {
            top = Component.translatable("booth.eraphones.number").getString();
            bottom = dialed + (now / 500 % 2 == 0 ? "_" : "");
        }
        glow(g, top.toUpperCase(), 28, 25, 1f, LCD_DIM);
        glow(g, bottom, 28, 38, 1.6f, LCD);
    }

    private void glow(GuiGraphics g, String s, int x, int y, float k, int color) {
        float max = 176 / k;
        while (font.width(s) > max && s.length() > 1) {
            s = s.substring(1);
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(k, k, 1);
        g.drawString(font, s, 1, 1, LCD_SHADOW, false);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Beside the payphone: what is said while talking, else the booth's card. */
    private void side(GuiGraphics g, long now) {
        int sx = PW + GAP, w = SIDE;
        g.fill(sx, 0, sx + w, PH, 0xE0121418);
        g.fill(sx, 0, sx + w, 1, 0xFF3A3E46);
        g.fill(sx, PH - 1, sx + w, PH, 0xFF3A3E46);
        g.fill(sx, 0, sx + 1, PH, 0xFF3A3E46);
        g.fill(sx + w - 1, 0, sx + w, PH, 0xFF3A3E46);
        int tx = sx + 10;
        if (state() == 3) {
            g.drawString(font, Component.translatable("booth.eraphones.conversation"), tx, 10, 0xFFE8E0C8, false);
            g.fill(tx, 22, sx + w - 10, 23, 0xFF3A3E46);
            if (PhoneClient.voiceChat()) {
                g.drawString(font, Component.translatable("phone.eraphones.voice_on"), tx, PH - 46, LCD, false);
            }
            // The lines, newest at the bottom, wrapped
            List<FormattedCharSequence> rows = new ArrayList<>();
            List<Integer> colours = new ArrayList<>();
            for (PhoneClient.Said s : PhoneClient.SAID) {
                for (FormattedCharSequence r : font.split(Component.literal((s.mine() ? "> " : "< ") + s.text()), w - 20)) {
                    rows.add(r);
                    colours.add(s.mine() ? 0xFFDADADA : LCD);
                }
            }
            int maxRows = (PH - 70) / 10;
            int from = Math.max(0, rows.size() - maxRows);
            for (int i = from; i < rows.size(); i++) {
                g.drawString(font, rows.get(i), tx, 30 + (i - from) * 10, colours.get(i), false);
            }
            // The line being said
            g.fill(tx - 2, PH - 34, sx + w - 8, PH - 14, 0xFF0A0B0D);
            String line = said.isEmpty() ? "" : said;
            while (font.width(line) > w - 26 && line.length() > 1) {
                line = line.substring(1);
            }
            if (said.isEmpty()) {
                g.drawString(font, Component.translatable("booth.eraphones.say_hint"), tx + 2, PH - 28, 0xFF6A6E78, false);
            } else {
                g.drawString(font, line + (now / 500 % 2 == 0 ? "_" : ""), tx + 2, PH - 28, 0xFFFFFFFF, false);
            }
            return;
        }
        g.drawString(font, Component.translatable("block.eraphones.cabine_telephonique"), tx, 10, 0xFFE8E0C8, false);
        g.fill(tx, 22, sx + w - 10, 23, 0xFF3A3E46);
        g.drawString(font, Component.translatable("booth.eraphones.its_number"), tx, 32, 0xFF8A8E98, false);
        g.pose().pushPose();
        g.pose().translate(tx, 44, 0);
        g.pose().scale(1.6f, 1.6f, 1);
        g.drawString(font, number, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
        int yy = 70;
        for (FormattedCharSequence r : font.split(Component.translatable("booth.eraphones.help", BoothNet.PRICE), w - 20)) {
            g.drawString(font, r, tx, yy, 0xFFB8BCC6, false);
            yy += 10;
        }
        int c = coins();
        Player p = Minecraft.getInstance().player;
        boolean free = p != null && p.getAbilities().instabuild;
        yy += 8;
        g.drawString(font, free ? Component.translatable("booth.eraphones.free") : Component.translatable("booth.eraphones.coins", c), tx, yy,
                free || c > 0 ? 0xFF9CF5A6 : 0xFFFF8A8A, false);
        float pulse = .5f + .5f * Mth.sin(now / 300f);
        if (!inCall() && !free && c == 0) {
            g.drawString(font, Component.translatable("booth.eraphones.no_coin"), tx, yy + 12, 0xFF000000 | (int) (150 + 100 * pulse) << 16 | 0x6A6A, false);
        }
    }
}
