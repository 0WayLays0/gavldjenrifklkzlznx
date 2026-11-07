package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.Gear;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.WalkieItem;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A walkie-talkie's face: its rubber body, the speaker grille, a green display lit when it is on (channel, battery),
 * the red power key and two big keys to change the channel. Talking into it is Simple Voice Chat's own talk key,
 * with the walkie-talkie in hand.
 */
public class WalkieScreen extends GearScreen {
    private static final ResourceLocation FACE = EraPhones.id("textures/gui/gear/walkie.png");
    private final InteractionHand hand;

    public WalkieScreen(InteractionHand hand) {
        super("item.eraphones.talkie_walkie", 120, 230);
        this.hand = hand;
        keys.add(new Key("power", 18, 160, 24, 24));
        keys.add(new Key("up", 52, 150, 50, 22));
        keys.add(new Key("down", 52, 176, 50, 22));
    }

    private ItemStack stack() {
        ItemStack s = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getItemInHand(hand);
        return s.getItem() instanceof WalkieItem ? s : ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        if (stack().isEmpty()) {
            onClose();
        }
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        ItemStack s = stack();
        boolean on = !s.isEmpty() && Gear.on(s);
        panel(g, FACE, 0, 0, 120, 230);
        if (on) {
            // The backlight, then the figures
            PhoneUI.round(g, 20, 96, 80, 38, 4, 0x70C8F070);
            PhoneUI.glow(g, 60, 115, 90, 0x30C8F070);
            int ink = 0xFF16240E;
            PhoneUI.text(g, "CH", 26, 104, ink, .9f);
            PhoneUI.right(g, String.format("%02d", Gear.channel(s)), 95, 100, 0x3016240E, 2.6f);
            PhoneUI.right(g, String.format("%02d", Gear.channel(s)), 94, 99, ink, 2.6f);
            int bars = (int) Math.ceil(Gear.battery(s) / 250f);
            for (int k = 0; k < 4; k++) {
                g.fill(26 + k * 5, 124, 29 + k * 5, 129, k < bars ? ink : 0x3016240E);
            }
            PhoneUI.text(g, "ON", 50, 123, ink, .7f);
            PhoneUI.glow(g, 30, 172, 26, 0x50FF4030);
        }
        hoverLight(g);
        PhoneUI.centered(g, tr("gear.eraphones.push_to_talk"), 60, 210, 0xFFC8D0B4, .6f);
    }

    private static String tr(String key) {
        return net.minecraft.network.chat.Component.translatable(key).getString();
    }

    @Override
    protected void pressed(String id) {
        ItemStack s = stack();
        if (s.isEmpty()) {
            return;
        }
        boolean on = s.getOrDefault(GearRegistry.POWER.get(), false);
        int ch = Gear.channel(s);
        switch (id) {
            case "power" -> on = !on;
            case "up" -> ch = ch % 16 + 1;
            case "down" -> ch = (ch + 14) % 16 + 1;
            default -> {
            }
        }
        // Shown at once, confirmed by the server
        s.set(GearRegistry.POWER.get(), on);
        s.set(GearRegistry.CHANNEL.get(), ch);
        PacketDistributor.sendToServer(new GearNet.Walkie(hand == InteractionHand.OFF_HAND, on, ch));
    }
}
