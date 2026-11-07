package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.CassetteItem;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.gear.block.VcrBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A video recorder's face: brushed aluminium, the smoked cassette door (the cassette seen through it, its reels turning
 * with the tape: slowly as it plays, fast and the other way as it winds back), a blue display (the counter), its
 * lamps, and its row of keys: eject, rewind, play, pause, stop, fast forward.
 */
public class VcrScreen extends GearScreen {
    private static final ResourceLocation FACE = EraPhones.id("textures/gui/gear/vcr.png"), TAPE = EraPhones.id("textures/gui/gear/vcr_tape.png"),
            REEL = EraPhones.id("textures/gui/gear/reel.png");
    private static final String[] ROW = {"eject", "rewind", "play", "pause", "stop", "forward"};
    private final BlockPos pos;
    private float reelAngle;
    private long last = System.currentTimeMillis();

    public VcrScreen(BlockPos pos) {
        super("block.eraphones.magnetoscope", 320, 130);
        this.pos = pos;
        for (int i = 0; i < ROW.length; i++) {
            keys.add(new Key(ROW[i], 152 + i * 27, 91, 24, 18));
        }
        keys.add(new Key("insert", 22, 91, 104, 18));
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
        String mode = v.mode();
        ItemStack tape = v.cassette();
        float dt = Math.min(.1f, (now - last) / 1000f);
        last = now;
        float speed = switch (mode) {
            case "PLAY" -> 140;
            case "REWIND" -> -1100;
            case "FORWARD" -> 1100;
            default -> 0;
        };
        reelAngle = (reelAngle + speed * dt) % 360;
        panel(g, FACE, 0, 0, 320, 130);
        // The cassette, behind the smoked door
        if (!tape.isEmpty()) {
            panel(g, TAPE, 20, 28, 112, 44);
            String label = tape.getOrDefault(GearRegistry.TAPE_LABEL.get(), "");
            PhoneUI.centered(g, PhoneUI.fit(label, 92, .62f), 76, 33.5f, 0xFF2A2420, .62f);
            sprite(g, REEL, 60, 58.5f, 14, 64, reelAngle);
            sprite(g, REEL, 92, 58.5f, 14, 64, reelAngle);
            g.fill(17, 25, 135, 75, 0x48080A10);
            g.fillGradient(17, 25, 135, 40, 0x18FFFFFF, 0x00FFFFFF);
        }
        // The display
        int vfd = 0xFF7AE8FF;
        String icon = switch (mode) {
            case "PLAY" -> "▶";
            case "PAUSE" -> "❚❚";
            case "REWIND" -> "◀◀";
            case "FORWARD" -> "▶▶";
            default -> tape.isEmpty() ? "" : "■";
        };
        PhoneUI.glow(g, 227, 43, 150, 0x147AE8FF);
        if (!(mode.equals("PAUSE") && now % 1000 < 500)) {
            PhoneUI.text(g, icon, 156, 36, vfd, 1.5f);
        }
        String counter = tape.isEmpty() ? "--:--" : Tape.clock(v.position());
        PhoneUI.right(g, counter, 300, 31, 0x407AE8FF, 2.4f);
        PhoneUI.right(g, counter, 300, 30, vfd, 2.4f);
        PhoneUI.text(g, tape.isEmpty() ? tr("gear.eraphones.vcr_no_cassette") : tr("gear.eraphones.vcr_mode." + mode.toLowerCase()), 156, 52, 0xC07AE8FF, .55f);
        // The lamps under the display
        boolean blink = now % 800 < 400;
        lamp(g, 160, mode.equals("PLAY") || (mode.equals("PAUSE") && blink), 0xFF50FF70);
        lamp(g, 200, mode.equals("REWIND"), 0xFFFFB040);
        lamp(g, 240, mode.equals("FORWARD"), 0xFFFFB040);
        lamp(g, 280, !tape.isEmpty(), 0xFFFF5040);
        // The keys: the mode's own lit blue, the one under the mouse brighter
        for (String id : ROW) {
            if (id.equals(mode.toLowerCase())) {
                lit(g, key(id), 0x405AB0FF);
            }
        }
        if (tape.isEmpty() && holdingTape()) {
            button(g, key("insert"), 0xFF5A6A48, tr("gear.eraphones.vcr_insert"), 0xFFFFFFFF);
        }
        hoverLight(g);
    }

    @Override
    protected boolean active(Key k) {
        VcrBlockEntity v = vcr();
        return !k.id().equals("insert") || (v != null && v.cassette().isEmpty() && holdingTape());
    }

    private static void lamp(GuiGraphics g, int x, boolean on, int color) {
        if (on) {
            PhoneUI.glow(g, x, 71, 14, PhoneUI.alpha(color, .6f));
            PhoneUI.round(g, x - 2, 69, 4, 4, 2, color);
        }
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
