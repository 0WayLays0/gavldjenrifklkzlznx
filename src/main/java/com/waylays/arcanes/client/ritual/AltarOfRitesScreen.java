package com.waylays.arcanes.client.ritual;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.AltarRitualPacket;
import com.waylays.arcanes.ritual.Ritual;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The book of rites an Altar of Rites opens: the rituals listed down its left page; the chosen one's page on the
 * right (what it does, how many kneel, how long it is channelled, how it is drawn, its offering) and a button to
 * prepare it round the altar (the altar's rituals) or set it in the Ritual Chalk held (the others).
 */
public class AltarOfRitesScreen extends Screen {
    private static final ResourceLocation PANEL = ArcanesOublies.id("textures/gui/altar_of_rites.png");
    private static final int W = 256, H = 200;
    private static final int INK = 0x3B2414, FADED = 0x6E5434;
    private static final int ROW = 19;

    private final BlockPos pos;
    private int chosen;
    private Button act;

    public AltarOfRitesScreen(BlockPos pos) {
        super(Component.translatable("block.arcanes_oublies.altar_of_rites"));
        this.pos = pos;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        act = addRenderableWidget(Button.builder(Component.empty(), b -> {
            PacketDistributor.sendToServer(new AltarRitualPacket(pos, chosen));
            onClose();
        }).bounds(left() + 140, top() + 172, 104, 20).build());
        refresh();
    }

    private void refresh() {
        Ritual ritual = Ritual.byIndex(chosen);
        act.setMessage(Component.translatable(ritual.surface == Ritual.Surface.ALTAR ? "ui.arcanes_oublies.altar.prepare" : "ui.arcanes_oublies.altar.to_chalk"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = left() + 14, y = top() + 30;
        Ritual[] all = Ritual.values();
        for (int i = 0; i < all.length; i++) {
            if (mouseX >= x && mouseX < x + 74 && mouseY >= y + i * ROW - 3 && mouseY < y + i * ROW + ROW - 3) {
                chosen = i;
                refresh();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int l = left(), t = top();
        g.drawString(font, title, l + (W - font.width(title)) / 2, t + 9, INK, false);
        // The list
        Ritual[] all = Ritual.values();
        for (int i = 0; i < all.length; i++) {
            int y = t + 30 + i * ROW;
            boolean hover = mouseX >= l + 14 && mouseX < l + 88 && mouseY >= y - 3 && mouseY < y + ROW - 3;
            if (i == chosen) {
                g.fill(l + 13, y - 4, l + 89, y + ROW - 5, 0x40000000 | (all[i].color & 0xFFFFFF));
                g.fill(l + 13, y - 4, l + 15, y + ROW - 5, 0xFF000000 | all[i].color);
            } else if (hover) {
                g.fill(l + 13, y - 4, l + 89, y + ROW - 5, 0x20000000);
            }
            List<FormattedCharSequence> name = font.split(all[i].title(), 70);
            int ny = name.size() > 1 ? y - 3 : y + 1;
            for (FormattedCharSequence line : name.subList(0, Math.min(2, name.size()))) {
                g.drawString(font, line, l + 18, ny, i == chosen ? INK : FADED, false);
                ny += 8;
            }
        }
        // The chosen rite's page
        Ritual r = Ritual.byIndex(chosen);
        int x = l + 106, y = t + 28;
        g.drawString(font, r.title(), x, y, 0xFF000000 | darker(r.color), false);
        y += 13;
        for (FormattedCharSequence line : font.split(r.description(), 138)) {
            g.drawString(font, line, x, y, INK, false);
            y += 9;
        }
        y = Math.max(y + 4, t + 118);
        String surface = switch (r.surface) {
            case GROUND -> "ground";
            case WALL -> "wall";
            case ALTAR -> "altar";
        };
        Component[] facts = {
                Component.translatable("ui.arcanes_oublies.altar.participants", r.participants),
                Component.translatable("ui.arcanes_oublies.altar.channel", r.channelTicks / 20),
                Component.translatable("ui.arcanes_oublies.altar.surface." + surface),
                r.offering == null ? Component.translatable("ui.arcanes_oublies.altar.no_offering")
                        : Component.translatable("ui.arcanes_oublies.altar.offering", r.offeringCount, r.offering.getDefaultInstance().getHoverName())};
        for (Component fact : facts) {
            for (FormattedCharSequence line : font.split(fact, 138)) {
                g.drawString(font, line, x, y, FADED, false);
                y += 9;
            }
        }
        if (r.offering != null) {
            g.renderItem(r.offering.getDefaultInstance(), l + 118, t + 174);
        }
    }

    /** A ritual's colour darkened to read on the page. */
    private static int darker(int rgb) {
        return (int) (((rgb >> 16) & 0xFF) * .55f) << 16 | (int) (((rgb >> 8) & 0xFF) * .55f) << 8 | (int) ((rgb & 0xFF) * .55f);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(PANEL, left(), top(), 0, 0, W, H, W, H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
