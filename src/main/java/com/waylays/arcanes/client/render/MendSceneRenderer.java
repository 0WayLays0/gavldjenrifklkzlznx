package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.melody.MelodyMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.mend.MendScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * Mending Magic on its ally: soft green and gold light. A wavering thread of gold from the caster's hand; rings of light
 * rising round the ally at each healing; a bud of rose light over their head that bursts into petals of light; a wisp
 * circling them; a cocoon of turning bands of light; a halo of gold over their head.
 */
public class MendSceneRenderer extends EntityRenderer<MendScene> {
    static final int MINT = 0x7CFFB0, GOLD = 0xFFD678, GOLD_LIGHT = 0xFFF4C8, ROSE = 0xFF7896, WHITE = 0xFFFFFF;

    public MendSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MendScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        Entity ally = e.ally();
        float h = ally != null ? ally.getBbHeight() : 1.8f;
        float w = ally != null ? ally.getBbWidth() : .6f;
        float since = e.sinceFlash(partialTicks);
        int life = e.getLife();
        float fade = 1 - BossMesh.ramp(age, life - 10, life);
        // A flash at each healing: rings rising round the ally, a glow
        if (since >= 0 && since < 20) {
            float t = since / 20f;
            for (int k = 0; k < 3; k++) {
                float y = (t + k * .25f) % 1 * h * 1.1f;
                ClockMesh.ring(poseStack.last(), buffer, y, w * .9f, w * 1.1f, k == 1 ? GOLD : MINT, (1 - t) * .9f, light);
            }
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, h * .55f, 0), 2.4f * (1 + t), MINT, (1 - t) * .7f, light);
            MelodyMesh.glints(poseStack, buffer, view, new Vector3f(), w * 1.6f, h * 1.3f, 10, age * 2, 1 - t, seed, light);
        }
        switch (e.getKind()) {
            case PULSE -> {
                float t = Math.min(1, age / 24f);
                for (int k = 0; k < 4; k++) {
                    float y = (t * 1.4f - k * .18f);
                    if (y > 0 && y < 1.2f) {
                        ClockMesh.ring(poseStack.last(), buffer, y * h, w * .8f, w * 1.05f, k % 2 == 0 ? MINT : GOLD_LIGHT, (1 - t), light);
                    }
                }
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, h * .55f, 0), 3f, WHITE, (1 - t) * .6f, light);
                MelodyMesh.glints(poseStack, buffer, view, new Vector3f(), w * 1.8f, h * 1.4f, 14, age * 2, 1 - t, seed, light);
            }
            case THREAD -> {
                Entity from = e.source();
                if (from != null && !e.isDone()) {
                    Vector3f a = new Vector3f((float) (from.getX() - e.getX()), (float) (from.getY() - e.getY()) + from.getBbHeight() * .65f, (float) (from.getZ() - e.getZ()));
                    Vector3f b = new Vector3f(0, h * .6f, 0);
                    VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                    int n = 16;
                    Vector3f prev = a;
                    Vector3f side = BossMesh.across(new Vector3f(b).sub(a).normalize());
                    for (int i = 1; i <= n; i++) {
                        float t = (float) i / n;
                        Vector3f p = new Vector3f(a).lerp(b, t).add(new Vector3f(side).mul(.18f * Mth.sin(t * 9 - age * .4f) * Mth.sin(t * Mth.PI)))
                                .add(0, .25f * Mth.sin(t * Mth.PI), 0);
                        RitualMesh.shaft(beam, poseStack.last(), view.eye(), prev, p, .14f, BossMesh.glow(i % 2 == 0 ? GOLD_LIGHT : GOLD, .9f * fade), light);
                        prev = p;
                    }
                    ClockMesh.glow(poseStack, buffer, view, a, .8f, GOLD, .7f * fade, light);
                }
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, h * .6f, 0), 1.6f, MINT, .4f * fade, light);
            }
            case ECHO -> {
                // A soft ring at their feet between the echoes
                ClockMesh.ring(poseStack.last(), buffer, .05f, w, w * 1.6f, MINT, .4f * fade, light);
            }
            case BUD -> {
                Vector3f top = new Vector3f(0, h + .45f + .08f * Mth.sin(age * .1f), 0);
                if (!e.isDone()) {
                    float pulse = .8f + .2f * Mth.sin(age * .15f);
                    BossMesh.sphere(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), top, .14f * pulse, 6, 10, 0, 0, 1, BossMesh.glow(ROSE, .8f), light);
                    for (int k = 0; k < 3; k++) {
                        float a = k * Mth.TWO_PI / 3 + age * .03f;
                        Vector3f leaf = new Vector3f(top).add(Mth.cos(a) * .16f, -.12f, Mth.sin(a) * .16f);
                        ClockMesh.glow(poseStack, buffer, view, leaf, .3f, MINT, .7f, light);
                    }
                    ClockMesh.glow(poseStack, buffer, view, top, .9f, ROSE, .45f * pulse, light);
                } else {
                    // The bloom: petals of light flung out
                    float t = Math.min(1, since / 20f);
                    for (int k = 0; k < 8; k++) {
                        float a = k * Mth.TWO_PI / 8;
                        Vector3f p = new Vector3f(top).add(Mth.cos(a) * t * 1.4f, -t * .6f, Mth.sin(a) * t * 1.4f);
                        ClockMesh.glow(poseStack, buffer, view, p, .5f, k % 2 == 0 ? ROSE : GOLD_LIGHT, 1 - t, light);
                    }
                }
            }
            case WISP -> {
                float a = age * .12f;
                Vector3f p = new Vector3f(Mth.cos(a) * (w + .5f), h * .7f + .25f * Mth.sin(age * .07f), Mth.sin(a) * (w + .5f));
                ClockMesh.glow(poseStack, buffer, view, p, .7f, WHITE, .95f * fade, light);
                ClockMesh.glow(poseStack, buffer, view, p, 1.5f, MINT, .6f * fade, light);
                for (int k = 1; k <= 6; k++) {
                    float b = a - k * .12f;
                    Vector3f q = new Vector3f(Mth.cos(b) * (w + .5f), h * .7f + .25f * Mth.sin((age - k * 1.5f) * .07f), Mth.sin(b) * (w + .5f));
                    ClockMesh.glow(poseStack, buffer, view, q, .5f - k * .05f, MINT, (.5f - k * .07f) * fade, light);
                }
            }
            case COCOON -> {
                float grow = Math.min(1, age / 8f) * fade;
                VertexConsumer c = BossMesh.light(buffer, BossMesh.GLOW);
                BossMesh.sphere(c, poseStack.last(), new Vector3f(0, h * .5f, 0), Math.max(w, h * .5f) * 1.15f * grow, 10, 20, 0, 0, 1,
                        BossMesh.glow(MINT, .25f), light);
                for (int k = 0; k < 5; k++) {
                    float y = h * (.1f + .2f * k);
                    float r = Math.max(w, h * .5f) * 1.2f * grow * Mth.sin(Mth.PI * (.12f + .19f * k));
                    float turn = age * (k % 2 == 0 ? .08f : -.06f);
                    BossMesh.annulus(c, poseStack.last(), new Vector3f(0, y, 0), new Vector3f(Mth.cos(turn), .25f * Mth.sin(turn + k), Mth.sin(turn)).normalize(),
                            new Vector3f(-Mth.sin(turn), 0, Mth.cos(turn)), r * .92f, r, 40, 1, 0, 0, BossMesh.glow(k % 2 == 0 ? GOLD_LIGHT : MINT, 0),
                            BossMesh.glow(k % 2 == 0 ? GOLD_LIGHT : MINT, .8f), light, true);
                }
            }
            case SECOND -> {
                if (!e.isDone()) {
                    float y = h + .3f + .05f * Mth.sin(age * .08f);
                    BossMesh.annulus(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), new Vector3f(0, y, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1),
                            .26f, .36f, 40, 1, 0, 0, BossMesh.glow(GOLD_LIGHT, .9f), BossMesh.glow(GOLD, .9f), light, true);
                    ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, y, 0), 1.1f, GOLD, .35f * fade, light);
                } else {
                    float t = Math.min(1, since / 40f);
                    ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, h * .6f, 0), 6 * (.4f + t), GOLD_LIGHT, (1 - t), light);
                    for (int k = 0; k < 3; k++) {
                        ClockMesh.ring(poseStack.last(), buffer, .05f + k * .02f, 0, (1 + k) * 2.5f * t, k == 0 ? GOLD : MINT, (1 - t), light);
                    }
                }
            }
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(MendScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(MendScene e) {
        return BossMesh.GLOW;
    }
}
