package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.Stations;
import com.waylays.eraphones.gear.block.RadioBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.waylays.eraphones.compat.PacketDistributor;

/**
 * A table radio's face: walnut, the speaker cloth, the printed dial lit warm when it is on (a red needle, dragged,
 * scrolled or moved by the tuning knob), a magic eye that glows green when a station comes in, and three bakelite
 * knobs (power, volume, tuning) that turn with what they set: click the left or right half of a knob to turn it.
 */
public class RadioScreen extends GearScreen {
    private static final ResourceLocation FACE = EraPhones.id("textures/gui/gear/radio.png"), KNOB = EraPhones.id("textures/gui/gear/knob.png");
    private static final int DX = 134, DY = 22, DW = 148, DH = 60;
    private final BlockPos pos;
    private boolean dragging;

    public RadioScreen(BlockPos pos) {
        super("block.eraphones.radio", 300, 170);
        this.pos = pos;
        keys.add(new Key("power", 137, 113, 30, 30));
        keys.add(new Key("vol-", 191, 115, 13, 26));
        keys.add(new Key("vol+", 204, 115, 13, 26));
        keys.add(new Key("f-", 243, 115, 13, 26));
        keys.add(new Key("f+", 256, 115, 13, 26));
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

    private static float fx(int freq) {
        return DX + 8 + (freq - Stations.MIN) / (float) (Stations.MAX - Stations.MIN) * (DW - 16);
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        RadioBlockEntity r = radio();
        if (r == null) {
            return;
        }
        boolean on = r.on();
        int freq = r.frequency();
        panel(g, FACE, 0, 0, 300, 170);
        // The dial: lit warm from behind when on, its needle
        if (on) {
            PhoneUI.round(g, DX, DY, DW, DH, 5, 0x30FFB040);
            PhoneUI.glow(g, DX + DW / 2f, DY + DH / 2f, DW * 1.1f, 0x26FFC060);
        }
        float nx = fx(freq);
        g.fill((int) nx + 1, DY + 5, (int) nx + 2, DY + DH - 5, 0x40000000);
        g.fill((int) nx, DY + 4, (int) nx + 1, DY + DH - 4, 0xFFC81E10);
        PhoneUI.centered(g, Stations.format(freq) + " MHz", DX + DW / 2f, DY + 44, on ? 0xFF4A2410 : 0xFF7A6448, .9f);
        // The magic eye
        boolean station = on && GearClient.onAir(freq);
        if (on) {
            int eye = station ? 0xFF70FF90 : 0xFF2A7A3A;
            PhoneUI.round(g, 278, 124, 8, 8, 4, eye);
            if (station) {
                PhoneUI.glow(g, 282, 128, 34, 0x6070FF90);
            }
        }
        // The knobs, turned to what they set
        sprite(g, KNOB, 152, 128, 30, 80, on ? 45 : -45);
        sprite(g, KNOB, 204, 128, 26, 80, -135 + r.volume() * 27);
        sprite(g, KNOB, 256, 128, 26, 80, -135 + (freq - Stations.MIN) / (float) (Stations.MAX - Stations.MIN) * 270);
        PhoneUI.centered(g, tr("gear.eraphones.power"), 152, 148, 0xFFF0DCB0, .55f);
        PhoneUI.centered(g, tr("gear.eraphones.volume"), 204, 148, 0xFFF0DCB0, .55f);
        PhoneUI.centered(g, tr("gear.eraphones.tuning"), 256, 148, 0xFFF0DCB0, .55f);
        for (Key k : keys) {
            if (hover(k) && k.id().length() > 2 && !k.id().equals("power")) {
                boolean minus = k.id().endsWith("-");
                PhoneUI.centered(g, minus ? "−" : "+", minus ? k.x() - 4 : k.x() + k.w() + 4, k.y() + 9, 0xFFFFE8B0, .9f);
            }
        }
        if (hover(key("power"))) {
            PhoneUI.round(g, 137, 113, 30, 30, 15, 0x20FFFFFF);
        }
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
            case "f-" -> f -= 1;
            case "f+" -> f += 1;
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
