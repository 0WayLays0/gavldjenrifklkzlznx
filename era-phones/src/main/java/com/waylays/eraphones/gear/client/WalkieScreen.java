package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.Gear;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.WalkieItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A walkie-talkie's face: its speaker grille, a green display (channel, battery, on or off), a red power key and two
 * big keys to change the channel. Talking into it is Simple Voice Chat's own talk key, with the walkie-talkie in hand.
 */
public class WalkieScreen extends GearScreen {
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
        // The aerial, the body
        PhoneUI.gradient(g, 84, 0, 12, 34, 5, 0xFF2A2C2A, 0xFF121412);
        PhoneUI.gradient(g, 6, 26, 108, 200, 18, 0xFF4A5240, 0xFF262B22);
        PhoneUI.round(g, 6, 26, 108, 3, 2, 0x30FFFFFF);
        // The knob on top and the clip
        PhoneUI.gradient(g, 20, 20, 22, 10, 4, 0xFF303030, 0xFF101010);
        // The speaker grille
        PhoneUI.round(g, 16, 38, 88, 44, 8, 0xFF1A1E18);
        for (int i = 0; i < 7; i++) {
            PhoneUI.round(g, 22, 42 + i * 5 + 1, 76, 2, 1, 0xFF0A0C0A);
        }
        // The display
        int lcd = on ? 0xFF9CC07A : 0xFF4A5A40;
        PhoneUI.round(g, 16, 92, 88, 46, 6, 0xFF101410);
        PhoneUI.gradient(g, 20, 96, 80, 38, 4, PhoneUI.mix(lcd, 0xFFFFFFFF, .1f), PhoneUI.mix(lcd, 0xFF000000, .15f));
        if (on) {
            int ink = 0xFF1C2A14;
            PhoneUI.text(g, "CH", 26, 104, ink, .9f);
            PhoneUI.right(g, String.format("%02d", Gear.channel(s)), 94, 100, ink, 2.4f);
            int bars = (int) Math.ceil(Gear.battery(s) / 250f);
            for (int k = 0; k < 4; k++) {
                g.fill(26 + k * 5, 124, 29 + k * 5, 129, k < bars ? ink : 0x301C2A14);
            }
            PhoneUI.text(g, "ON", 50, 123, ink, .7f);
        }
        // The keys
        button(g, key("power"), on ? 0xFFC03030 : 0xFF702020, "⏻", 0xFFFFFFFF);
        button(g, key("up"), 0xFF3A3E36, "▲", 0xFFE0E0D0);
        button(g, key("down"), 0xFF3A3E36, "▼", 0xFFE0E0D0);
        PhoneUI.centered(g, tr("gear.eraphones.push_to_talk"), 60, 208, 0xFFB8C0A8, .55f);
    }

    private static String tr(String key, Object... args) {
        return net.minecraft.network.chat.Component.translatable(key, args).getString();
    }

    @Override
    protected void pressed(String id) {
        ItemStack s = stack();
        if (s.isEmpty()) {
            return;
        }
        boolean on = s.getOrDefault(com.waylays.eraphones.registry.GearRegistry.POWER.get(), false);
        int ch = Gear.channel(s);
        switch (id) {
            case "power" -> on = !on;
            case "up" -> ch = ch % 16 + 1;
            case "down" -> ch = (ch + 14) % 16 + 1;
            default -> {
            }
        }
        // Shown at once, confirmed by the server
        s.set(com.waylays.eraphones.registry.GearRegistry.POWER.get(), on);
        s.set(com.waylays.eraphones.registry.GearRegistry.CHANNEL.get(), ch);
        PacketDistributor.sendToServer(new GearNet.Walkie(hand == InteractionHand.OFF_HAND, on, ch));
    }
}
