package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.GearNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * An identity card of the time, laminated: a guilloche, the band and its seal, the photograph (the holder's face over
 * their shoulders), the name, the number, the description, a signature, the reading strip at the bottom; a gleam
 * passing over it now and then; and, when someone shows it, who.
 */
public class CardScreen extends GearScreen {
    private static final ResourceLocation FACE = EraPhones.id("textures/gui/gear/card.png"), SHEEN = EraPhones.id("textures/gui/gear/sheen.png");
    private final GearNet.Card card;
    private final long opened = System.currentTimeMillis();

    public CardScreen(GearNet.Card card) {
        super("item.eraphones.carte_identite", 300, 196);
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

    /** The two lines of the reading strip, made of the name and the number. */
    private String[] strip() {
        String name = card.name().toUpperCase(Locale.ROOT).replaceAll("[^A-Z ]", "");
        String[] parts = name.trim().split("\\s+");
        String last = parts.length > 1 ? parts[parts.length - 1] : parts[0];
        String first = parts.length > 1 ? String.join("<", java.util.Arrays.copyOf(parts, parts.length - 1)) : "";
        String l1 = ("IDVOX" + last + "<<" + first + "<".repeat(40)).substring(0, 40);
        String digits = card.number().replaceAll("[^0-9]", "");
        String l2 = (digits + "<<" + "1980" + "<<" + "VOXA" + "<".repeat(40)).substring(0, 40);
        return new String[] {l1, l2};
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        panel(g, FACE, 0, 0, 300, 190);
        PhoneUI.text(g, tr("gear.eraphones.card_header"), 14, 9, 0xFFF4F0E0, 1f);
        PhoneUI.text(g, "VOXA", 14, 18, 0xA0D8E4F0, .55f);
        // The photograph: shoulders, then the face
        g.fillGradient(24, 108, 78, 128, 0xFF3A4458, 0xFF242A38);
        PlayerFaceRenderer.draw(g, skin(), 25, 50, 52);
        g.fill(18, 40, 84, 44, 0x10FFFFFF);
        // The fields
        int x = 100;
        label(g, x, 38, tr("gear.eraphones.card_name"));
        PhoneUI.text(g, PhoneUI.fit(card.name().isEmpty() ? "—" : card.name(), 180, 1.15f), x, 44, 0xFF101A14, 1.15f);
        label(g, x, 66, tr("gear.eraphones.card_number"));
        PhoneUI.text(g, card.number().isEmpty() ? "—" : card.number(), x, 72, 0xFF101A14, 1f);
        label(g, x, 90, tr("gear.eraphones.card_desc"));
        String desc = card.description().isEmpty() ? tr("gear.eraphones.card_no_desc") : card.description();
        List<FormattedCharSequence> lines = PhoneUI.wrap(desc, 120, .6f);
        PhoneUI.lines(g, lines.subList(0, Math.min(5, lines.size())), x, 98, 0xFF1A2A20, .6f);
        // The signature, slanting, in ink
        g.pose().pushPose();
        g.pose().translate(x + 6, 146, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-5));
        PhoneUI.text(g, Component.literal(card.name()).withStyle(st -> st.withItalic(true)), 0, 0, 0xFF1A2A7A, 1.05f);
        g.pose().popPose();
        PhoneUI.text(g, tr("gear.eraphones.card_signature"), x, 162, 0xFF5A6A60, .5f);
        // The reading strip
        String[] mrz = strip();
        PhoneUI.text(g, mrz[0], 10, 168, 0xFF2A302C, .62f);
        PhoneUI.text(g, mrz[1], 10, 175, 0xFF2A302C, .62f);
        // The gleam across the laminate
        float t = ((now - opened) % 5000) / 5000f;
        clip(g, 0, 0, 296, 182);
        g.pose().pushPose();
        g.pose().translate(-120 + t * 560, -10, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(18));
        panel(g, SHEEN, 0, 0, 80, 200);
        g.pose().popPose();
        g.disableScissor();
        if (!card.shownBy().isEmpty()) {
            PhoneUI.centered(g, tr("gear.eraphones.card_shown_by", card.shownBy()), 148, 188, 0xFFF0F0F0, .7f);
        }
    }

    private static void label(GuiGraphics g, int x, int y, String text) {
        PhoneUI.text(g, text.toUpperCase(), x, y, 0xFF4E6A5C, .5f);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    protected void pressed(String id) {
    }
}
