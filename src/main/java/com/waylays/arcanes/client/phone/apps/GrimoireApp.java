package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.client.phone.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** Grimoire: the player at a glance: their health, their mana, their level, armour and hunger, the effects on them, where they are. */
public class GrimoireApp extends PhoneApp {
    public GrimoireApp() {
        super("grimoire");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF241040, 0xFF0C0618};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        Player p = mc.player;
        if (p == null) {
            return;
        }
        float cx = (X0 + X1) / 2f;
        PhoneUI.glow(g, cx, Y0 + 20, 70, 0x66B98CFF);
        PhoneUI.face(g, p.getUUID(), (int) cx - 14, Y0 + 6, 28);
        PhoneUI.centered(g, p.getGameProfile().getName(), cx, Y0 + 40, PhoneUI.WHITE, .85f);
        PhoneUI.centered(g, Component.translatable("phone.arcanes_oublies.level", p.experienceLevel).getString(), cx, Y0 + 50, PhoneUI.GOLD, .6f);
        int y = Y0 + 62;
        y = bar(g, y, Component.translatable("phone.arcanes_oublies.health").getString(), PhoneClient.healthFraction(),
                (int) p.getHealth() + " / " + (int) p.getMaxHealth(), 0xFFFF4A6A);
        y = bar(g, y, Component.translatable("phone.arcanes_oublies.mana").getString(), PhoneClient.manaFraction(),
                Math.round(PhoneClient.manaFraction() * 100) + " %", 0xFF5CB4FF);
        y = bar(g, y, Component.translatable("phone.arcanes_oublies.experience").getString(), p.experienceProgress, "", 0xFF7CE07A);
        y = bar(g, y, Component.translatable("phone.arcanes_oublies.hunger").getString(), p.getFoodData().getFoodLevel() / 20f,
                p.getFoodData().getFoodLevel() + " / 20", 0xFFE0A050);
        y = bar(g, y, Component.translatable("phone.arcanes_oublies.armor").getString(), Math.min(1, p.getArmorValue() / 20f), String.valueOf(p.getArmorValue()),
                0xFFB8C0D0);
        y += 2;
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.effects").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        if (p.getActiveEffects().isEmpty()) {
            PhoneUI.text(g, "—", X0 + 12, y, PhoneUI.DIM, .6f);
            y += 9;
        }
        for (MobEffectInstance e : p.getActiveEffects()) {
            int color = e.getEffect().value().getColor() | 0xFF000000;
            PhoneUI.round(g, X0 + 10, y + 1, 5, 5, 2, color);
            String name = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
            PhoneUI.text(g, PhoneUI.fit(name, 90, .6f), X0 + 18, y, PhoneUI.WHITE, .6f);
            PhoneUI.right(g, e.isInfiniteDuration() ? "∞" : e.getDuration() / 20 / 60 + ":" + String.format("%02d", e.getDuration() / 20 % 60), X1 - 10, y,
                    PhoneUI.DIM, .6f);
            y += 9;
            if (y > Y1 - 14) {
                break;
            }
        }
        PhoneUI.centered(g, p.level().dimension().location().getPath() + " · " + p.blockPosition().toShortString(), cx, Y1 - 8, PhoneUI.FAINT, .5f);
    }

    private int bar(GuiGraphics g, int y, String label, float f, String value, int color) {
        PhoneUI.text(g, label, X0 + 10, y, PhoneUI.DIM, .55f);
        PhoneUI.right(g, value, X1 - 10, y, PhoneUI.WHITE, .55f);
        PhoneUI.round(g, X0 + 10, y + 7, X1 - X0 - 20, 5, 2, 0x33FFFFFF);
        PhoneUI.round(g, X0 + 10, y + 7, Math.max(3, (int) ((X1 - X0 - 20) * Math.max(0, Math.min(1, f)))), 5, 2, color);
        return y + 16;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
        }
        return true;
    }
}
