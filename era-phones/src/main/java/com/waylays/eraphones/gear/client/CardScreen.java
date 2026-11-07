package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.GearNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.UUID;

/**
 * An identity card of the time: a pale guilloche background, the photograph (the holder's face), the name, the number,
 * what they look like, a signature; and, when someone shows it, who.
 */
public class CardScreen extends GearScreen {
    private final GearNet.Card card;

    public CardScreen(GearNet.Card card) {
        super("item.eraphones.carte_identite", 300, 190);
        this.card = card;
    }

    private PlayerSkin skin() {
        UUID id;
        try {
            id = UUID.fromString(card.owner());
        } catch (IllegalArgumentException e) {
            return DefaultPlayerSkin.get(new UUID(0, 0));
        }
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        // The card and its guilloche
        PhoneUI.round(g, 2, 4, 296, 184, 12, 0x60000000);
        PhoneUI.gradient(g, 0, 0, 296, 182, 12, 0xFFEAF0E4, 0xFFC9D8CC);
        for (int i = 0; i < 26; i++) {
            float y0 = 20 + i * 6;
            for (int x = 6; x < 290; x += 2) {
                int y = (int) (y0 + Math.sin(x * .06 + i * .7) * 4);
                if (y > 4 && y < 178) {
                    g.fill(x, y, x + 1, y + 1, 0x1A3A6A50);
                }
            }
        }
        // The band at the top
        PhoneUI.gradient(g, 0, 0, 296, 26, 12, 0xFF2E5A8A, 0xFF1E3E66);
        g.fill(0, 16, 296, 26, 0xFF1E3E66);
        PhoneUI.centered(g, tr("gear.eraphones.card_header"), 148, 8, 0xFFF4F0E0, .9f);
        // The photograph
        PhoneUI.round(g, 14, 38, 74, 92, 4, 0xFFFFFFFF);
        g.fill(18, 42, 84, 126, 0xFFB8C4CC);
        PlayerFaceRenderer.draw(g, skin(), 21, 50, 60);
        g.fill(18, 112, 84, 126, 0x30000000);
        // The fields
        int x = 100;
        field(g, x, 40, tr("gear.eraphones.card_name"), card.name().isEmpty() ? "—" : card.name(), 1.1f);
        field(g, x, 68, tr("gear.eraphones.card_number"), card.number().isEmpty() ? "—" : card.number(), .9f);
        PhoneUI.text(g, tr("gear.eraphones.card_desc").toUpperCase(), x, 94, 0xFF5A6A60, .55f);
        String desc = card.description().isEmpty() ? tr("gear.eraphones.card_no_desc") : card.description();
        List<FormattedCharSequence> lines = PhoneUI.wrap(desc, 184, .65f);
        PhoneUI.lines(g, lines.subList(0, Math.min(4, lines.size())), x, 102, 0xFF1A2A20, .65f);
        // The signature, slanting
        g.pose().pushPose();
        g.pose().translate(x + 6, 148, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-6));
        PhoneUI.text(g, Component.literal(card.name()).withStyle(s -> s.withItalic(true)), 0, 0, 0xFF1A2A6A, 1f);
        g.pose().popPose();
        g.fill(x, 160, x + 120, 161, 0x601A2A20);
        PhoneUI.text(g, tr("gear.eraphones.card_signature"), x, 163, 0xFF5A6A60, .5f);
        // The stamp
        PhoneUI.round(g, 228, 128, 52, 40, 20, 0x30A02020);
        PhoneUI.centered(g, "1980", 254, 145, 0x80A02020, .8f);
        if (!card.shownBy().isEmpty()) {
            PhoneUI.centered(g, tr("gear.eraphones.card_shown_by", card.shownBy()), 148, 186, 0xFFE8E8E8, .7f);
        }
    }

    private static void field(GuiGraphics g, int x, int y, String label, String value, float scale) {
        PhoneUI.text(g, label.toUpperCase(), x, y, 0xFF5A6A60, .55f);
        PhoneUI.text(g, PhoneUI.fit(value, 184, scale), x, y + 8, 0xFF101A14, scale);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    protected void pressed(String id) {
    }
}
