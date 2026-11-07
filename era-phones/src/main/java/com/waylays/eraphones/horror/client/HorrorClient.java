package com.waylays.eraphones.horror.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.horror.HorrorNet;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * What a game master puts before one player's senses, and theirs alone: words whispered in their head, letter by
 * letter; fog closing in; the lights flickering and going out; blood running down; a face rushing at them; the ground
 * shaking; a red balloon floating in the world, bobbing on its string, until it bursts.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class HorrorClient {
    private static final ResourceLocation FACE = EraPhones.id("textures/gui/horror/face.png");
    private static final ResourceLocation BALLOON = EraPhones.id("textures/horror/balloon.png");

    private static final class Active {
        final String kind, text;
        final long start, end;
        final double x, y, z;
        final long seed;
        boolean popped;

        Active(HorrorNet.Effect e, long now) {
            kind = e.kind();
            text = e.text();
            start = now;
            end = now + e.ticks() * 50L;
            x = e.x();
            y = e.y();
            z = e.z();
            seed = now ^ (long) (e.x() * 31 + e.z() * 17);
        }

        float age() {
            return (System.currentTimeMillis() - start) / 1000f;
        }

        float left() {
            return (end - System.currentTimeMillis()) / 1000f;
        }

        /** 0 to 1 coming in over {@code in} seconds, 1 to 0 going over the last {@code out}. */
        float fade(float in, float out) {
            return clamp(Math.min(age() / in, left() / out));
        }
    }

    private static final List<Active> ACTIVE = new ArrayList<>();

    private HorrorClient() {}

    private static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }

    private static SoundEvent sound(String id) {
        return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace(id));
    }

    private static void play(String id, float pitch, float volume) {
        SoundEvent s = sound(id);
        if (s != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, volume));
        }
    }

    public static void effect(HorrorNet.Effect e) {
        long now = System.currentTimeMillis();
        if (e.kind().equals("stop")) {
            ACTIVE.clear();
            return;
        }
        if (!e.kind().equals("ballon")) {
            ACTIVE.removeIf(a -> a.kind.equals(e.kind()));
        }
        ACTIVE.add(new Active(e, now));
        switch (e.kind()) {
            case "murmure" -> play("entity.vex.ambient", .55f, .6f);
            case "brouillard" -> play("ambient.cave", .6f, .7f);
            case "coupure" -> {
                play("block.beacon.deactivate", .6f, 1);
                play("block.lever.click", .5f, .8f);
            }
            case "sang" -> play("entity.warden.heartbeat", 1, 1);
            case "sursaut" -> {
                play("entity.elder_guardian.curse", .9f, 1);
                play("entity.ghast.scream", .6f, .9f);
            }
            case "tremblement" -> play("entity.warden.emerge", .6f, .9f);
            default -> {
            }
        }
    }

    private static Active active(String kind) {
        for (Active a : ACTIVE) {
            if (a.kind.equals(kind)) {
                return a;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        for (Iterator<Active> it = ACTIVE.iterator(); it.hasNext(); ) {
            Active a = it.next();
            if (a.kind.equals("ballon") && !a.popped && now >= a.end) {
                a.popped = true;
                pop(mc, a);
            }
            if (now >= a.end + 400) {
                it.remove();
            }
        }
        // A heart beating under the blood
        Active blood = active("sang");
        if (blood != null && blood.left() > 1 && mc.player != null && mc.player.tickCount % 22 == 0) {
            play("entity.warden.heartbeat", 1, .8f);
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    // ---------------------------------------------------------------- over the screen

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(EraPhones.id("horror"), HorrorClient::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        Active a;
        if ((a = active("sang")) != null) {
            blood(g, a, w, h);
        }
        if ((a = active("coupure")) != null) {
            blackout(g, a, w, h);
        }
        if ((a = active("murmure")) != null) {
            whisper(g, a, w, h);
        }
        if ((a = active("sursaut")) != null) {
            face(g, a, w, h);
        }
    }

    /** A dark frame round the screen, {@code strength} its darkest at the very edge. */
    private static void vignette(GuiGraphics g, int w, int h, int rgb, float strength) {
        int steps = 24;
        int band = Math.max(steps, Math.min(w, h) / 4);
        for (int i = 0; i < steps; i++) {
            float t = 1 - i / (float) steps;
            int alpha = (int) (255 * Math.min(1, strength) * t * t * .85f);
            int c = alpha << 24 | rgb;
            int in = band * i / steps, step = Math.max(1, band * (i + 1) / steps - in);
            // Each strip of the frame once: top and bottom across, the sides between them
            g.fill(in, in, w - in, in + step, c);
            g.fill(in, h - in - step, w - in, h - in, c);
            g.fill(in, in + step, in + step, h - in - step, c);
            g.fill(w - in - step, in + step, w - in, h - in - step, c);
        }
    }

    /** Blood running down from the top of the screen, a red edge beating like a heart. */
    private static void blood(GuiGraphics g, Active a, int w, int h) {
        float f = a.fade(.3f, 1.5f), t = a.age();
        float beat = (float) Math.pow(Math.max(0, Math.sin(t * Math.PI * 2 * 1.1)), 6);
        vignette(g, w, h, 0x7A0000, f * (.7f + .3f * beat));
        g.fillGradient(0, 0, w, (int) (h * .07f), ((int) (200 * f) << 24) | 0x5A0000, 0x005A0000);
        Random r = new Random(a.seed);
        int count = 10 + w / 40;
        for (int i = 0; i < count; i++) {
            float x = r.nextFloat() * w, width = 1.5f + r.nextFloat() * 4, speed = 12 + r.nextFloat() * 45, delay = r.nextFloat() * 2.5f;
            float len = Math.min(h * (.35f + r.nextFloat() * .6f), Math.max(0, t - delay) * speed);
            if (len <= 0) {
                continue;
            }
            int alpha = (int) (215 * f);
            int dark = alpha << 24 | 0x4A0000, bright = alpha << 24 | 0x8A0A0A;
            g.fillGradient((int) x, 0, (int) (x + width), (int) len, dark, bright);
            // The drop at its end, a little wider
            int dy = (int) len;
            g.fill((int) (x - .6f), dy - 2, (int) (x + width + .6f), dy + 1, bright);
            g.fill((int) x, dy + 1, (int) (x + width), dy + 2, bright);
        }
    }

    /** The lights flickering, going out, a flicker now and then in the dark, then coming back. */
    private static void blackout(GuiGraphics g, Active a, int w, int h) {
        float t = a.age(), left = a.left();
        long frame = System.currentTimeMillis() / 70;
        Random r = new Random(frame * 31 + a.seed);
        float dark;
        if (t < 1.4f) {
            dark = r.nextFloat() < .5f + t * .3f ? .95f : 0;
        } else if (left < 1.2f) {
            dark = left <= 0 ? 0 : r.nextFloat() < left * .7f ? .95f : 0;
        } else {
            dark = .95f;
            // Now and then a light stutters back for a moment
            if ((frame % 37) < 2 && r.nextFloat() < .7f) {
                dark = .55f;
            }
        }
        if (dark > 0) {
            g.fill(0, 0, w, h, (int) (255 * dark) << 24);
            vignette(g, w, h, 0x000000, 1);
        }
    }

    /** Words in the head: letter by letter, trembling, in dark red, the screen's edges darkening round them. */
    private static void whisper(GuiGraphics g, Active a, int w, int h) {
        Font font = Minecraft.getInstance().font;
        float f = a.fade(.4f, 1.2f);
        vignette(g, w, h, 0x000000, .55f * f);
        float scale = Math.max(1, Math.min(2, w / 320f));
        List<String> lines = wrap(font, a.text, (int) (w * .7f / scale));
        int shown = (int) Math.max(0, (a.age() - .25f) / .045f);
        int alpha = (int) (255 * f);
        if (alpha < 8) {
            return;
        }
        Random r = new Random(System.currentTimeMillis() / 80);
        float lh = 11 * scale;
        float y = h * .36f - lines.size() * lh / 2;
        int count = 0;
        for (String line : lines) {
            float x = (w - font.width(line) * scale) / 2;
            for (int i = 0; i < line.length(); i++) {
                if (count++ >= shown) {
                    return;
                }
                String ch = String.valueOf(line.charAt(i));
                float jx = (r.nextFloat() - .5f) * 1.2f * scale, jy = (r.nextFloat() - .5f) * 1.2f * scale;
                g.pose().pushPose();
                g.pose().translate(x + jx, y + jy, 0);
                g.pose().scale(scale, scale, 1);
                g.drawString(font, ch, 1, 1, alpha << 24, false);
                g.drawString(font, ch, 0, 0, alpha << 24 | 0xB01414, false);
                g.pose().popPose();
                x += font.width(ch) * scale;
            }
            y += lh;
        }
    }

    private static List<String> wrap(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (font.width(next) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** A face rushing at the screen, shaking, the screen flashing red; words under it if there are any. */
    private static void face(GuiGraphics g, Active a, int w, int h) {
        float t = a.age(), left = a.left();
        float f = clamp(left / .3f);
        if (f <= 0) {
            return;
        }
        float zoom = t < .12f ? 1.7f - t / .12f * .6f : 1.1f + (t - .12f) * .05f;
        Random r = new Random(System.currentTimeMillis() / 40);
        float shake = 7 * f;
        g.fill(0, 0, w, h, (int) (235 * f) << 24);
        int size = (int) (h * .92f * zoom);
        float x = (w - size) / 2f + (r.nextFloat() - .5f) * shake * 2, y = (h - size) / 2f + (r.nextFloat() - .5f) * shake * 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        boolean flash = (System.currentTimeMillis() / 60) % 5 == 0;
        RenderSystem.setShaderColor(1, flash ? .55f : 1, flash ? .55f : 1, f);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 256f, size / 256f, 1);
        g.blit(FACE, 0, 0, 0, 0, 256, 256, 256, 256);
        g.pose().popPose();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        if (t < .1f) {
            g.fill(0, 0, w, h, (int) (200 * (1 - t / .1f)) << 24 | 0xFFFFFF);
        }
        if (!a.text.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            float scale = Math.max(1.5f, Math.min(3, w / 220f));
            String s = a.text.toUpperCase();
            float tx = (w - font.width(s) * scale) / 2 + (r.nextFloat() - .5f) * shake, ty = h * .82f;
            g.pose().pushPose();
            g.pose().translate(tx, ty, 0);
            g.pose().scale(scale, scale, 1);
            int alpha = (int) (255 * f);
            g.drawString(font, s, 1, 1, alpha << 24, false);
            g.drawString(font, s, 0, 0, alpha << 24 | 0xE01010, false);
            g.pose().popPose();
        }
    }

    // ---------------------------------------------------------------- the world

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Active a = active("brouillard");
        if (a == null) {
            return;
        }
        float f = a.fade(3, 3);
        if (f <= 0) {
            return;
        }
        event.setFarPlaneDistance(lerp(event.getFarPlaneDistance(), 5, f));
        event.setNearPlaneDistance(lerp(event.getNearPlaneDistance(), -1, f));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Active a = active("brouillard");
        if (a == null) {
            return;
        }
        float f = a.fade(3, 3);
        event.setRed(lerp(event.getRed(), .30f, f));
        event.setGreen(lerp(event.getGreen(), .32f, f));
        event.setBlue(lerp(event.getBlue(), .31f, f));
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float strength = 0;
        Active a = active("tremblement");
        if (a != null) {
            strength = Math.max(strength, 1.6f * a.fade(.8f, 1.5f));
        }
        a = active("sursaut");
        if (a != null) {
            strength = Math.max(strength, 3 * clamp(a.left() / .3f));
        }
        if (strength <= 0) {
            return;
        }
        double t = System.currentTimeMillis() / 1000.0;
        event.setYaw(event.getYaw() + (float) (Math.sin(t * 37) + Math.sin(t * 23.3)) * .5f * strength);
        event.setPitch(event.getPitch() + (float) (Math.sin(t * 41) + Math.sin(t * 19.7)) * .5f * strength);
        event.setRoll(event.getRoll() + (float) Math.sin(t * 29) * .6f * strength);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Where a balloon is at this moment: bobbing, swaying, rising off in its last seconds. */
    private static Vec3 balloonAt(Active a) {
        float t = a.age(), left = a.left();
        double rise = left < 2.5f ? (2.5f - Math.max(0, left)) * (2.5f - Math.max(0, left)) * .25 : 0;
        return new Vec3(a.x + Math.sin(t * .9) * .12, a.y + Math.sin(t * 1.7) * .08 + rise, a.z + Math.cos(t * .7) * .1);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        boolean any = false;
        for (Active a : ACTIVE) {
            any |= a.kind.equals("ballon") && !a.popped;
        }
        if (!any) {
            return;
        }
        PoseStack ps = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(BALLOON);
        VertexConsumer vc = buffers.getBuffer(type);
        for (Active a : ACTIVE) {
            if (!a.kind.equals("ballon") || a.popped) {
                continue;
            }
            Vec3 at = balloonAt(a);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at));
            int alpha = (int) (255 * clamp(a.age() / .8f));
            ps.pushPose();
            ps.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
            ps.mulPose(camera.rotation());
            // A slight lean, as the string pulls
            ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) Math.sin(a.age() * 1.1) * 6));
            PoseStack.Pose pose = ps.last();
            float hw = .42f, top = .55f, bottom = -1.13f;
            vertex(vc, pose, -hw, top, 0, 0, alpha, light);
            vertex(vc, pose, -hw, bottom, 0, 1, alpha, light);
            vertex(vc, pose, hw, bottom, 1, 1, alpha, light);
            vertex(vc, pose, hw, top, 1, 0, alpha, light);
            ps.popPose();
        }
        buffers.endBatch(type);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int alpha, int light) {
        vc.addVertex(pose, x, y, 0).setColor(255, 255, 255, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
    }

    /** The balloon bursts: a bang, red shreds falling. */
    private static void pop(Minecraft mc, Active a) {
        if (mc.level == null) {
            return;
        }
        Vec3 at = balloonAt(a);
        SoundEvent bang = sound("entity.firework_rocket.blast");
        if (bang != null) {
            mc.level.playLocalSound(at.x, at.y, at.z, bang, SoundSource.MASTER, .7f, 1.7f, false);
        }
        DustParticleOptions red = new DustParticleOptions(new Vector3f(.75f, .04f, .05f), 1.1f);
        Random r = new Random(a.seed);
        for (int i = 0; i < 18; i++) {
            mc.level.addParticle(red, at.x, at.y + .2, at.z, (r.nextFloat() - .5f) * .3f, r.nextFloat() * .15f, (r.nextFloat() - .5f) * .3f);
        }
    }
}
