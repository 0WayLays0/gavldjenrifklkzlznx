package com.waylays.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.cinematic.studio.Emote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Locale;

/** On-screen information (countdown, REC, free camera) and the camera paths drawn in the world. */
public final class StudioHud {
    private StudioHud() {}

    public static void onGuiOverlay(RenderGuiOverlayEvent.Pre e) {
        // The free camera keeps the screen clean: only the chat and our own lines
        if (StudioCamera.free() && e.getOverlay().id().getNamespace().equals("minecraft") && !e.getOverlay().id().getPath().equals("chat_panel")) {
            e.setCanceled(true);
        }
    }

    public static void onGui(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        var font = mc.font;
        var play = ClientStudio.play();
        if (ClientStudio.amRecording()) {
            if (play.countdown() > 0) {
                int n = (play.countdown() + 19) / 20;
                PoseStack ps = g.pose();
                ps.pushPose();
                ps.translate(w / 2f, h / 2f - 30, 0);
                ps.scale(5f, 5f, 1f);
                String digit = String.valueOf(n);
                g.drawString(font, digit, -font.width(digit) / 2, -4, 0xFFFF5050, false);
                ps.popPose();
                g.drawCenteredString(font, Component.translatable("cinematic.hud.get_ready"), w / 2, h / 2 + 20, 0xFFFFFF);
            } else if (play.playing()) {
                boolean blink = (mc.level != null && mc.level.getGameTime() / 10 % 2 == 0);
                g.drawString(font, Component.literal(blink ? "● " : "  ").append(Component.translatable("cinematic.hud.rec",
                        TimelineSlider.time(ClientStudio.recordedTicks()))), 8, 8, 0xFFFF4040);
                g.drawString(font, Component.translatable("cinematic.hud.emote", Component.translatable(Emote.byId(ClientStudio.emote).key())), 8, 20, 0xFFFFFF);
                g.drawString(font, Component.translatable("cinematic.hud.rec_hint"), 8, 32, 0xB0B0B0);
            }
        } else if (ClientStudio.isRecording()) {
            g.drawString(font, Component.translatable("cinematic.hud.someone_recording"), 8, 8, 0xFFFF8080);
        } else if (ClientStudio.serverPlaying() && !mc.options.hideGui) {
            g.drawString(font, Component.literal("▶ " + ClientStudio.play().scene() + "  " + TimelineSlider.time(ClientStudio.sceneTime(0))), 8, 8, 0xFF80FF80);
        }
        if (StudioCamera.free()) {
            CameraTrack t = CameraTrack.forScene(ClientStudio.activeScene());
            CameraTrack.Shot s = t.selectedShot();
            int y = h - 70;
            g.fill(4, y - 6, Math.min(w - 4, 330), h - 4, 0x90000000);
            g.drawString(font, Component.translatable("cinematic.hud.freecam", String.format(Locale.ROOT, "%.1f", StudioCamera.speed()),
                    String.format(Locale.ROOT, "%.0f", StudioCamera.freeFov()), String.format(Locale.ROOT, "%.0f", StudioCamera.freeRoll())), 8, y, 0xFFFFFF);
            g.drawString(font, Component.translatable("cinematic.hud.cursor", ClientStudio.selectedScene.isEmpty() ? "-" : ClientStudio.selectedScene,
                    TimelineSlider.time(ClientStudio.cursor)), 8, y + 13, 0xFFE070);
            g.drawString(font, s == null ? Component.translatable("cinematic.hud.no_shot")
                    : Component.translatable("cinematic.hud.shot", t.selected + 1, Component.translatable(s.type.key()), s.keys.size()), 8, y + 26, 0xD0D0D0);
            g.drawString(font, Component.translatable("cinematic.hud.freecam_keys"), 8, y + 42, 0xA0A0A0);
            g.drawString(font, Component.translatable("cinematic.hud.freecam_keys2"), 8, y + 54, 0xA0A0A0);
        }
    }

    /** While flying the free camera, the shots of the scene are drawn: the selected one in yellow, the others grey. */
    public static void onLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !StudioCamera.free()) return;
        CameraTrack t = CameraTrack.forScene(ClientStudio.activeScene());
        if (t.shots.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i < t.shots.size(); i++) {
            CameraTrack.Shot s = t.shots.get(i);
            boolean sel = i == t.selected;
            int r = sel ? 255 : 150, g = sel ? 210 : 150, b = sel ? 60 : 150;
            for (CameraTrack.Key k : s.keys) {
                Vec3 p = new Vec3(k.x, k.y, k.z);
                float yr = (float) Math.toRadians(k.yaw), pr = (float) Math.toRadians(k.pitch);
                Vec3 dir = new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
                line(vc, m, n, p.add(-0.15, 0, 0), p.add(0.15, 0, 0), r, g, b);
                line(vc, m, n, p.add(0, -0.15, 0), p.add(0, 0.15, 0), r, g, b);
                line(vc, m, n, p.add(0, 0, -0.15), p.add(0, 0, 0.15), r, g, b);
                line(vc, m, n, p, p.add(dir.scale(0.8)), 255, 80, 80);
            }
            if (s.type == CameraTrack.Type.PATH && s.keys.size() > 1) {
                int from = s.keys.get(0).tick, to = s.keys.get(s.keys.size() - 1).tick;
                Vec3 prev = null;
                CameraTrack one = new CameraTrack();
                one.shots.add(s);
                for (int k = 0; k <= 48; k++) {
                    float time = from + (to - from) * k / 48f;
                    CameraTrack.View v = one.evaluate(time);
                    if (v == null) break;
                    if (prev != null) line(vc, m, n, prev, v.pos, r, g, b);
                    prev = v.pos;
                }
            }
        }
        buffers.endBatch(RenderType.lines());
        ps.popPose();
    }

    private static void line(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 a, Vec3 b, int r, int g, int bl) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-6) return;
        float nx = (float) (d.x / len), ny = (float) (d.y / len), nz = (float) (d.z / len);
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, 255).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, 255).normal(n, nx, ny, nz).endVertex();
    }
}
