package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.CassetteItem;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.gear.block.VcrBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A video recorder's face: the cassette flap (the cassette's label showing through when one is in), a blue display
 * (what the deck does, the tape counter), and its row of keys: eject, rewind, play, pause, stop, fast forward.
 */
public class VcrScreen extends GearScreen {
    private static final String[] ROW = {"eject", "rewind", "play", "pause", "stop", "forward"};
    private static final String[] GLYPH = {"⏏", "◀◀", "▶", "❚❚", "■", "▶▶"};
    private final BlockPos pos;

    public VcrScreen(BlockPos pos) {
        super("block.eraphones.magnetoscope", 320, 130);
        this.pos = pos;
        for (int i = 0; i < ROW.length; i++) {
            keys.add(new Key(ROW[i], 152 + i * 27, 92, 24, 18));
        }
        keys.add(new Key("insert", 22, 92, 104, 18));
    }

    private VcrBlockEntity vcr() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof VcrBlockEntity v ? v : null;
    }

    @Override
    public void tick() {
        if (vcr() == null || Minecraft.getInstance().player.distanceToSqr(pos.getCenter()) > 64) {
            onClose();
        }
    }

    private static boolean holdingTape() {
        var p = Minecraft.getInstance().player;
        return p != null && (p.getMainHandItem().getItem() instanceof CassetteItem || p.getOffhandItem().getItem() instanceof CassetteItem);
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        VcrBlockEntity v = vcr();
        if (v == null) {
            return;
        }
        metal(g, 0, 10, 320, 110, 10, 0xFFD8DADE, 0xFF8E9298);
        PhoneUI.round(g, 0, 112, 320, 8, 4, 0xFF2A2C30);
        // The cassette flap
        ItemStack tape = v.cassette();
        PhoneUI.round(g, 16, 26, 116, 54, 6, 0xFF1A1C20);
        PhoneUI.gradient(g, 20, 30, 108, 46, 4, 0xFF34363C, 0xFF16181C);
        if (!tape.isEmpty()) {
            PhoneUI.round(g, 26, 40, 96, 26, 3, 0xFF0E0E10);
            PhoneUI.gradient(g, 30, 44, 88, 18, 2, 0xFFF4EEDC, 0xFFD8D0B8);
            g.fill(30, 48, 118, 49, 0x40C03020);
            String label = tape.getOrDefault(GearRegistry.TAPE_LABEL.get(), "");
            PhoneUI.centered(g, PhoneUI.fit(label, 84, .7f), 74, 51, 0xFF2A2420, .7f);
        } else {
            PhoneUI.centered(g, tr("gear.eraphones.vcr_slot"), 74, 50, 0x90FFFFFF, .6f);
        }
        // The display
        PhoneUI.round(g, 148, 26, 160, 54, 6, 0xFF0A0C10);
        PhoneUI.gradient(g, 152, 30, 152, 46, 4, 0xFF0C1824, 0xFF060A10);
        String mode = v.mode();
        int vfd = 0xFF6AE0FF;
        String icon = switch (mode) {
            case "PLAY" -> "▶";
            case "PAUSE" -> "❚❚";
            case "REWIND" -> "◀◀";
            case "FORWARD" -> "▶▶";
            default -> tape.isEmpty() ? "" : "■";
        };
        boolean blink = mode.equals("PAUSE") && now % 1000 < 500;
        if (!blink) {
            PhoneUI.text(g, icon, 162, 44, vfd, 1.4f);
        }
        String counter = tape.isEmpty() ? "--:--" : Tape.clock(v.position());
        PhoneUI.right(g, counter, 296, 40, vfd, 2.2f);
        PhoneUI.glow(g, 228, 53, 120, 0x146AE0FF);
        PhoneUI.text(g, tape.isEmpty() ? tr("gear.eraphones.vcr_no_cassette") : tr("gear.eraphones.vcr_mode." + mode.toLowerCase()), 162, 66, 0xC06AE0FF, .55f);
        // The keys
        for (int i = 0; i < ROW.length; i++) {
            Key k = key(ROW[i]);
            boolean lit = ROW[i].equals(mode.toLowerCase());
            button(g, k, lit ? 0xFF4A6A8A : 0xFF3A3C42, GLYPH[i], lit ? 0xFFBFF0FF : 0xFFE8E8EC);
        }
        if (tape.isEmpty() && holdingTape()) {
            button(g, key("insert"), 0xFF5A6A48, tr("gear.eraphones.vcr_insert"), 0xFFFFFFFF);
        }
        PhoneUI.text(g, "VOXA  VHS", 16, 14, 0xFF4A4C52, .7f);
        PhoneUI.right(g, "HQ", 308, 14, 0xFF4A4C52, .7f);
    }

    private static String tr(String key) {
        return net.minecraft.network.chat.Component.translatable(key).getString();
    }

    @Override
    protected void pressed(String id) {
        VcrBlockEntity v = vcr();
        if (v == null || (id.equals("insert") && (!v.cassette().isEmpty() || !holdingTape()))) {
            return;
        }
        PacketDistributor.sendToServer(new GearNet.Vcr(pos, id));
    }
}
