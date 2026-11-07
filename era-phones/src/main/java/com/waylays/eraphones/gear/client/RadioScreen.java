package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.Stations;
import com.waylays.eraphones.gear.block.RadioBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A table radio's face: the speaker cloth on the left, the lit dial on the right (88 to 108, a red needle, dragged or
 * moved by the keys), a magic eye that glows when a station comes in, the power and volume knobs.
 */
public class RadioScreen extends GearScreen {
    private static final int DX = 132, DY = 22, DW = 150, DH = 64;
    private final BlockPos pos;
    private boolean dragging;

    public RadioScreen(BlockPos pos) {
        super("block.eraphones.radio", 300, 170);
        this.pos = pos;
        keys.add(new Key("power", 136, 120, 30, 30));
        keys.add(new Key("vol-", 180, 126, 20, 18));
        keys.add(new Key("vol+", 204, 126, 20, 18));
        keys.add(new Key("f--", 232, 98, 24, 16));
        keys.add(new Key("f-", 232, 118, 24, 16));
        keys.add(new Key("f+", 258, 118, 24, 16));
        keys.add(new Key("f++", 258, 98, 24, 16));
    }

    private RadioBlockEntity radio() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof RadioBlockEntity r ? r : null;
    }

    @Override
    public void tick() {
        RadioBlockEntity r = radio();
        if (r == null || Minecraft.getInstance().player.distanceToSqr(pos.getCenter()) > 64) {
            onClose();
        }
    }

    private int fx(int freq) {
        return DX + 8 + Math.round((freq - Stations.MIN) / (float) (Stations.MAX - Stations.MIN) * (DW - 16));
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        RadioBlockEntity r = radio();
        if (r == null) {
            return;
        }
        boolean on = r.on();
        int freq = r.frequency();
        wood(g, 0, 0, 300, 170, 16);
        PhoneUI.round(g, 6, 6, 288, 158, 12, 0x22000000);
        // The speaker cloth
        PhoneUI.round(g, 14, 16, 106, 138, 10, 0xFF2A1A10);
        PhoneUI.gradient(g, 18, 20, 98, 130, 8, 0xFFB8A27A, 0xFF8A7454);
        for (int y = 22; y < 148; y += 3) {
            g.fill(20, y, 114, y + 1, 0x22000000);
        }
        for (int x = 20; x < 114; x += 3) {
            g.fill(x, 22, x + 1, 148, 0x14000000);
        }
        PhoneUI.centered(g, "VOXA", 67, 136, 0xFF3A2414, .8f);
        // The dial
        PhoneUI.round(g, DX - 4, DY - 4, DW + 8, DH + 8, 8, 0xFF2A1A10);
        int glass = on ? 0xFFF6DC9A : 0xFF6E6250;
        PhoneUI.gradient(g, DX, DY, DW, DH, 6, PhoneUI.mix(glass, 0xFFFFFFFF, .2f), PhoneUI.mix(glass, 0xFF000000, .2f));
        for (int f = Stations.MIN; f <= Stations.MAX; f += 5) {
            int x = fx(f);
            boolean major = f % 20 == 0;
            g.fill(x, DY + 14, x + 1, DY + (major ? 26 : 20), 0xFF3A2A1A);
            if (major) {
                PhoneUI.centered(g, String.valueOf(f / 10), x + .5f, DY + 30, 0xFF3A2A1A, .6f);
            }
        }
        PhoneUI.text(g, "FM", DX + 6, DY + 4, 0xFF5A3A1A, .6f);
        PhoneUI.right(g, "MHz", DX + DW - 6, DY + 4, 0xFF5A3A1A, .6f);
        PhoneUI.centered(g, Stations.format(freq), DX + DW / 2f, DY + 44, 0xFF3A1A0A, 1.2f);
        int nx = fx(freq);
        g.fill(nx, DY + 6, nx + 1, DY + DH - 6, 0xFFD02010);
        // The magic eye
        boolean station = on && GearClient.onAir(freq);
        int eye = station ? 0xFF60FF80 : on ? 0xFF1E6A2A : 0xFF14301A;
        PhoneUI.round(g, DX + DW - 26, DY + DH + 12, 18, 18, 9, 0xFF101810);
        PhoneUI.round(g, DX + DW - 23, DY + DH + 15, 12, 12, 6, eye);
        if (station) {
            PhoneUI.glow(g, DX + DW - 17, DY + DH + 21, 30, 0x5060FF80);
        }
        // The knobs
        knob(g, key("power"), on ? 0xFF3A2414 : 0xFF2A1A10, on ? "I" : "O");
        PhoneUI.centered(g, tr("gear.eraphones.power"), 151, 152, 0xFFE8D0A8, .55f);
        button(g, key("vol-"), 0xFF3A2414, "−", 0xFFF0D8B0);
        button(g, key("vol+"), 0xFF3A2414, "+", 0xFFF0D8B0);
        for (int k = 0; k < 10; k++) {
            g.fill(180 + k * 4 + 1, 148, 180 + k * 4 + 3, 152, k < r.volume() ? 0xFFF0C070 : 0x40F0C070);
        }
        PhoneUI.centered(g, tr("gear.eraphones.volume"), 202, 116, 0xFFE8D0A8, .55f);
        button(g, key("f--"), 0xFF3A2414, "«", 0xFFF0D8B0);
        button(g, key("f-"), 0xFF3A2414, "‹", 0xFFF0D8B0);
        button(g, key("f+"), 0xFF3A2414, "›", 0xFFF0D8B0);
        button(g, key("f++"), 0xFF3A2414, "»", 0xFFF0D8B0);
    }

    private void knob(GuiGraphics g, Key k, int color, String label) {
        PhoneUI.round(g, k.x() - 2, k.y() - 1, k.w() + 4, k.h() + 4, (k.w() + 4) / 2, 0x80000000);
        button(g, k, color, label, 0xFFF0D8B0);
    }

    private static String tr(String key) {
        return net.minecraft.network.chat.Component.translatable(key).getString();
    }

    private void send(boolean on, int freq, int vol) {
        PacketDistributor.sendToServer(new GearNet.Radio(pos, on, Math.max(Stations.MIN, Math.min(Stations.MAX, freq)), Math.max(0, Math.min(10, vol))));
    }

    @Override
    protected void pressed(String id) {
        RadioBlockEntity r = radio();
        if (r == null) {
            return;
        }
        boolean on = r.on();
        int f = r.frequency(), v = r.volume();
        switch (id) {
            case "power" -> on = !on;
            case "vol-" -> v--;
            case "vol+" -> v++;
            case "f--" -> f -= 10;
            case "f-" -> f -= 1;
            case "f+" -> f += 1;
            case "f++" -> f += 10;
            default -> {
            }
        }
        send(on, f, v);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double lx = (mx - px) / scale, ly = (my - py) / scale;
        if (lx >= DX && lx < DX + DW && ly >= DY && ly < DY + DH) {
            dragging = true;
            tune(lx);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            tune((mx - px) / scale);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        RadioBlockEntity r = radio();
        if (r != null) {
            send(r.on(), r.frequency() + (sy > 0 ? 1 : -1), r.volume());
        }
        return true;
    }

    private void tune(double lx) {
        RadioBlockEntity r = radio();
        if (r == null) {
            return;
        }
        int f = Stations.MIN + (int) Math.round((lx - DX - 8) / (DW - 16) * (Stations.MAX - Stations.MIN));
        if (f != r.frequency()) {
            send(r.on(), f, r.volume());
        }
    }
}
