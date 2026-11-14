package com.waylays.arcanes.rp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpConfig;
import com.waylays.arcanes.rp.RpNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The role play on this side: the characters' names (over the heads, in the place of the accounts'), and the bubbles of
 * what is said and done, floating over the head for a few seconds, the newest at the bottom, fading out.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class RpClient {
    private static final long LIFE = 7000, FADE = 800;
    private static final Map<UUID, String> NAMES = new HashMap<>();
    private static final Map<Integer, List<Said>> BUBBLES = new HashMap<>();
    /** The server hides the names over the heads (the default; /pseudos afficher shows them). */
    private static boolean namesHidden = true;

    private record Said(String text, int color, long at) {}

    private RpClient() {}

    public static void names(RpNet.Names p) {
        if (p.replace()) {
            NAMES.clear();
        }
        NAMES.putAll(p.names());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (Player pl : mc.level.players()) {
                pl.refreshDisplayName();
            }
        }
    }

    public static void nameplates(RpNet.Nameplates p) {
        namesHidden = p.hidden();
    }

    public static void bubble(RpNet.Bubble p) {
        List<Said> l = BUBBLES.computeIfAbsent(p.entity(), k -> new ArrayList<>());
        l.add(new Said(p.text(), p.color(), System.currentTimeMillis()));
        while (l.size() > 3) {
            l.remove(0);
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity().level().isClientSide) {
            String n = NAMES.get(event.getEntity().getUUID());
            if (n != null) {
                event.setDisplayname(Component.literal(n));
            }
        }
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player && !nameplates()) {
            event.setCanRender(TriState.FALSE);
        }
    }

    private static boolean nameplates() {
        if (namesHidden) {
            return false;
        }
        try {
            return RpConfig.NAMEPLATES.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    @SubscribeEvent
    public static void onRender(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        List<Said> l = BUBBLES.get(player.getId());
        if (l == null || l.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        l.removeIf(s -> now - s.at > LIFE);
        if (l.isEmpty()) {
            BUBBLES.remove(player.getId());
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(0, player.getBbHeight() + (nameplates() ? 0.62 : 0.4), 0);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(0.025f, -0.025f, 0.025f);
        Matrix4f m = ps.last().pose();
        // Lines from the bottom (newest) up
        float y = 0;
        for (int i = l.size() - 1; i >= 0; i--) {
            Said s = l.get(i);
            long age = now - s.at;
            float a = age > LIFE - FADE ? (LIFE - age) / (float) FADE : Math.min(1, age / 150f);
            int alpha = Math.max(8, (int) (255 * a));
            List<FormattedCharSequence> lines = font.split(Component.literal(s.text), 180);
            y -= lines.size() * 10 + 3;
            float ly = y;
            for (FormattedCharSequence line : lines) {
                float x = -font.width(line) / 2f;
                int bg = (int) (alpha * .62f) << 24 | 0x0A0A12;
                font.drawInBatch(line, x, ly, alpha << 24 | (s.color & 0xFFFFFF), false, m, event.getMultiBufferSource(), Font.DisplayMode.NORMAL, bg,
                        event.getPackedLight());
                ly += 10;
            }
        }
        ps.popPose();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        NAMES.clear();
        BUBBLES.clear();
        namesHidden = true;
    }
}
