package com.waylays.cinematic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** The post-processing chain: depth of field (2 passes) then the film grade (shaders/post/cinematic.json). */
public final class FilmLook {
    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation CHAIN = new ResourceLocation(CinematicMod.MOD_ID, "shaders/post/cinematic.json");

    private static PostChain chain;
    private static final List<EffectInstance> effects = new ArrayList<>();
    private static boolean failed;
    private static int width = -1, height = -1;

    private static long lastNanos;
    private static float clock;
    private static float bars, dof;
    private static float fade, fadeSpeed;

    private FilmLook() {}

    public static void fadeFromBlack(float seconds) {
        fade = 1f;
        fadeSpeed = 1f / seconds;
    }

    /** A brief dip to black, used on cuts. */
    public static void dip() {
        fade = Math.max(fade, 0.85f);
        fadeSpeed = 4f;
    }

    public static void close() {
        if (chain != null) chain.close();
        chain = null;
        effects.clear();
        failed = false;
        width = height = -1;
    }

    private static boolean ensureChain(Minecraft mc) {
        if (chain != null) return true;
        if (failed) return false;
        try {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN);
            effects.clear();
            // PostChain keeps its passes in a private list: find it by content, independent of the obfuscated name
            for (Field f : PostChain.class.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                List<?> list = (List<?>) f.get(chain);
                if (!list.isEmpty() && list.get(0) instanceof PostPass) {
                    for (Object p : list) effects.add(((PostPass) p).getEffect());
                }
            }
            return true;
        } catch (Exception e) {
            LOG.error("Cinematic: could not load the film shaders, the look is disabled", e);
            failed = true;
            chain = null;
            return false;
        }
    }

    static void render(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min((now - lastNanos) / 1.0e9f, 0.1f);
        lastNanos = now;
        clock = (clock + dt) % 3600f;
        if (fade > 0f) fade = Math.max(0f, fade - fadeSpeed * dt);

        boolean shots = CameraDirector.active() || com.waylays.cinematic.client.StudioCamera.filmActive();
        boolean on = CinematicClient.lookOn() || shots;
        float barsTarget = on && (shots || CinematicConfig.LETTERBOX.get()) ? 1f : 0f;
        float dofTarget = on && CinematicClient.dofOn() ? 1f : 0f;
        bars = approach(bars, barsTarget, dt * 1.6f);
        dof = approach(dof, dofTarget, dt * 2.5f);
        if (!on && bars <= 0f && fade <= 0f) return;
        if (!ensureChain(mc)) return;

        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w != width || h != height) {
            chain.resize(w, h);
            width = w;
            height = h;
        }
        float look = on ? 1f : 0f;
        float focus = com.waylays.cinematic.client.StudioCamera.focus() >= 0 ? com.waylays.cinematic.client.StudioCamera.focus() : CameraDirector.focusDistance();
        for (EffectInstance fx : effects) {
            fx.safeGetUniform("Clock").set(clock);
            fx.safeGetUniform("Bars").set(smooth(bars));
            fx.safeGetUniform("Fade").set(fade);
            fx.safeGetUniform("Grade").set(look * CinematicConfig.GRADE.get().floatValue());
            fx.safeGetUniform("Grain").set(look * CinematicConfig.GRAIN.get().floatValue());
            fx.safeGetUniform("Vignette").set(look * CinematicConfig.VIGNETTE.get().floatValue());
            fx.safeGetUniform("Aberration").set(look * CinematicConfig.ABERRATION.get().floatValue());
            fx.safeGetUniform("Bloom").set(look * CinematicConfig.BLOOM.get().floatValue());
            fx.safeGetUniform("DofStrength").set(smooth(dof) * CinematicConfig.DOF_STRENGTH.get().floatValue());
            fx.safeGetUniform("FocusDepth").set(focus);
            fx.safeGetUniform("FarPlane").set(mc.gameRenderer.getDepthFar());
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(partialTick);
        mc.getMainRenderTarget().bindWrite(false);
        RenderSystem.enableDepthTest();
    }

    private static float approach(float v, float target, float step) {
        return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }
}
