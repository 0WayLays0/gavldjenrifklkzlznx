package com.waylays.eraphones.client.smart;

import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneHud;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The call, over the whole screen: who it is (their picture, their name), how it stands (ringing, calling, the time
 * spoken, why it ended); answer or refuse when it rings; while talking, what each end has said, a line to speak into
 * and the red button to hang up.
 */
public class CallOverlay extends PhoneApp {
    private static final int SX = SmartScreen.SX, SY = SmartScreen.SY, SW = SmartScreen.SW, SH = SmartScreen.SH;
    private EditBox line;

    public CallOverlay() {
        super("appel");
    }

    private EditBox line() {
        if (line == null) {
            line = field(SX + 14, SY + SH - 76, SW - 50, 200, "phone.eraphones.speak");
        }
        return line;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        int st = PhoneClient.callState;
        boolean hidden = Numbers.hidden(PhoneClient.callNumber);
        PhoneUI.gradient(g, SX, SY, SW, SH, SmartScreen.SR, hidden ? 0xF8200808 : 0xF81A2436, 0xF8060810);
        float cx = SX + SW / 2f;
        // The picture, a soft halo pulsing while it rings
        if (st == 1 || st == 2) {
            float p = (now % 1500) / 1500f;
            PhoneUI.glow(g, cx, SY + 70, 60 + p * 50, PhoneUI.alpha(st == 2 ? 0xFF5CE08A : 0xFF6A8CFF, (1 - p) * .5f));
        }
        String name = PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
        if (hidden) {
            PhoneUI.round(g, (int) cx - 24, SY + 46, 48, 48, 24, 0xFF101010);
            PhoneUI.centered(g, "?", cx, SY + 60, 0xFFFF3030, 2.2f);
        } else {
            PhoneUI.avatar(g, name, PhoneClient.callNumber, (int) cx - 24, SY + 46, 48);
        }
        PhoneUI.centered(g, PhoneUI.fit(name, SW - 20, 1.1f), cx, SY + 102, PhoneUI.WHITE, 1.1f);
        if (!hidden && !name.equals(PhoneClient.callNumber)) {
            PhoneUI.centered(g, PhoneClient.callNumber, cx, SY + 115, PhoneUI.DIM, .6f);
        }
        String status = switch (st) {
            case 1 -> Component.translatable("phone.eraphones.calling").getString() + ".".repeat((int) (now / 400 % 4));
            case 2 -> Component.translatable("phone.eraphones.incoming").getString();
            case 3 -> PhoneHud.timer(now - PhoneClient.callSince);
            default -> Component.translatable("phone.eraphones.call_end." + PhoneClient.callReason).getString();
        };
        PhoneUI.centered(g, status, cx, SY + 126, st == 0 ? 0xFFFF8A8A : st == 3 ? 0xFF9AE6B4 : PhoneUI.DIM, .75f);
        if (st == 2) {
            roundButton(g, SX + 40, SY + SH - 56, 0xFFE0453A, "✕", mx, my);
            float bob = Mth.sin(now / 160f) * 1.5f;
            roundButton(g, SX + SW - 40, SY + SH - 56 + bob, 0xFF2EB85C, "✆", mx, my);
            PhoneUI.centered(g, Component.translatable("phone.eraphones.decline").getString(), SX + 40, SY + SH - 36, PhoneUI.DIM, .55f);
            PhoneUI.centered(g, Component.translatable("phone.eraphones.accept").getString(), SX + SW - 40, SY + SH - 36, PhoneUI.DIM, .55f);
        } else if (st == 1) {
            roundButton(g, cx, SY + SH - 50, 0xFFE0453A, "✆", mx, my);
        } else if (st == 3) {
            if (PhoneClient.voiceChat()) {
                PhoneUI.centered(g, Component.translatable("phone.eraphones.voice_on").getString(), cx, SY + 136, 0xFF9AE6B4, .55f);
            }
            subtitles(g, SY + 146, SY + SH - 86);
            drawField(g, line(), mx, my, pt);
            boolean hs = PhoneUI.in(mx, my, SX + SW - 32, SY + SH - 80, 20, 14);
            PhoneUI.round(g, SX + SW - 32, SY + SH - 80, 20, 14, 7, hs ? 0xFF5CB4FF : 0xFF2E7AD0);
            PhoneUI.centered(g, "↑", SX + SW - 22, SY + SH - 77, PhoneUI.WHITE, .8f);
            roundButton(g, cx, SY + SH - 38, 0xFFE0453A, "✆", mx, my);
        }
    }

    private void subtitles(GuiGraphics g, int top, int bottom) {
        int y = bottom;
        List<PhoneClient.Said> said = PhoneClient.SAID;
        phone.clip(g, SX, top, SX + SW, bottom);
        for (int i = said.size() - 1; i >= 0 && y > top; i--) {
            PhoneClient.Said s = said.get(i);
            List<FormattedCharSequence> lines = PhoneUI.wrap(s.text(), 100, .65f);
            int h = lines.size() * 7 + 6;
            int w = 0;
            for (FormattedCharSequence l : lines) {
                w = Math.max(w, Math.round(mc.font.width(l) * .65f));
            }
            w += 10;
            y -= h + 3;
            int x = s.mine() ? SX + SW - 10 - w : SX + 10;
            PhoneUI.round(g, x, y, w, h, 6, s.mine() ? 0xFF2E7AD0 : 0x44FFFFFF);
            PhoneUI.lines(g, lines, x + 5, y + 3, PhoneUI.WHITE, .65f);
        }
        g.disableScissor();
    }

    private void roundButton(GuiGraphics g, float cx, float cy, int color, String glyph, int mx, int my) {
        boolean hover = PhoneUI.in(mx, my, cx - 14, cy - 14, 28, 28);
        PhoneUI.round(g, (int) cx - 14, (int) cy - 14, 28, 28, 14, hover ? PhoneUI.mix(color, 0xFFFFFFFF, .2f) : color);
        PhoneUI.centered(g, glyph, cx, cy - 5, PhoneUI.WHITE, 1.2f);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        int st = PhoneClient.callState;
        if (st == 2) {
            if (PhoneUI.in(mx, my, SX + 26, SY + SH - 70, 28, 28)) {
                PhoneClient.answer(false);
            } else if (PhoneUI.in(mx, my, SX + SW - 54, SY + SH - 70, 28, 28)) {
                PhoneClient.answer(true);
            }
        } else if (st == 1) {
            if (PhoneUI.in(mx, my, SX + SW / 2f - 14, SY + SH - 64, 28, 28)) {
                PhoneClient.hangUp();
            }
        } else if (st == 3) {
            if (PhoneUI.in(mx, my, SX + SW / 2f - 14, SY + SH - 52, 28, 28)) {
                PhoneClient.hangUp();
            } else if (PhoneUI.in(mx, my, SX + SW - 32, SY + SH - 80, 20, 14)) {
                say();
            } else {
                clickField(line(), mx, my, button);
            }
        }
        return true;
    }

    private void say() {
        String t = line().getValue();
        if (!t.isBlank()) {
            PhoneClient.speak(t);
            line().setValue("");
        }
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (PhoneClient.callState != 3) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            say();
            line().setFocused(true);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        if (!line().isFocused()) {
            line().setFocused(true);
        }
        return feed(line(), key, scan, mods) || key != GLFW.GLFW_KEY_ESCAPE && line().isFocused();
    }

    @Override
    public boolean typed(char c, int mods) {
        if (PhoneClient.callState != 3) {
            return false;
        }
        line().setFocused(true);
        return feed(line(), c, mods);
    }
}
