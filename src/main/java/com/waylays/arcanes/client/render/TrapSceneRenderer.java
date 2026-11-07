package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.trap.TrapScene;
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
 * Trap Magic's traps: a circle of teeth and runes drawn on the ground in the trap's colour, faint while it waits and
 * breathing slowly; small thorns or spikes rising from it; when it springs, it flares, its teeth snap up, a ring runs
 * out. The plague and the leech draw a thread to their victim; the cursed web spreads strands and rings over the
 * ground, flaring each time it curses.
 */
public class TrapSceneRenderer extends EntityRenderer<TrapScene> {
    public static final ResourceLocation SIGIL = ArcanesOublies.id("textures/entity/trap/sigil.png");
    public static final ResourceLocation WEB = ArcanesOublies.id("textures/entity/trap/web.png");

    public TrapSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TrapScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        int color = e.color();
        int life = e.getLife();
        float since = e.sinceStruck(partialTicks);
        float flare = since >= 0 && since < 20 ? 1 - since / 20f : 0;
        float appear = BossMesh.ramp(age, 0, TrapScene.ARM);
        float fade = 1 - BossMesh.ramp(age, life - 15, life);
        float size = e.getKind() == TrapScene.Kind.WEB ? e.getSize() : e.getKind() == TrapScene.Kind.MINE ? .9f : 1.3f;
        // Faint while it waits (an enemy may not see it), bright when it springs
        float alpha = (.28f + .12f * Mth.sin(age * .08f) + .7f * flare) * appear * fade;
        if (e.getKind() == TrapScene.Kind.WEB) {
            web(e, poseStack, buffer, view, age, color, size, alpha, flare, appear * fade, light);
        } else {
            VertexConsumer c = BossMesh.light(buffer, SIGIL);
            BossMesh.plate(c, poseStack.last(), new Vector3f(0, .03f, 0), new Vector3f((float) Math.cos(age * .01f), 0, (float) Math.sin(age * .01f)),
                    new Vector3f((float) -Math.sin(age * .01f), 0, (float) Math.cos(age * .01f)), size * 2, BossMesh.glow(color, alpha), light);
            // Teeth: thorns or spikes round it, low while it waits, snapping up when it springs
            int teeth = e.getKind() == TrapScene.Kind.MINE ? 0 : 8;
            float rise = .12f + .5f * (e.isSprung() ? Math.min(1, (since + 2) / 3f) * Math.max(0, 1 - since / 30f) : 0);
            VertexConsumer spikes = BossMesh.solid(buffer, ClockMesh.STEEL);
            for (int k = 0; k < teeth; k++) {
                float a = k * Mth.TWO_PI / teeth + .2f;
                float r = size * .78f;
                Vector3f base = new Vector3f(Mth.cos(a) * r, 0, Mth.sin(a) * r);
                Vector3f dir = new Vector3f(-Mth.cos(a) * .35f, 1, -Mth.sin(a) * .35f).normalize();
                BossMesh.horn(spikes, poseStack.last(), base, dir, new Vector3f(), rise * appear * fade, .045f, 5, 3, 0xFF808088, BossMesh.mix(0xFF707078, color | 0xFF000000, .5f),
                        light);
            }
            if (flare > 0) {
                ClockMesh.ring(poseStack.last(), buffer, .05f, size * (1 + 2 * (1 - flare)) * .8f, size * (1 + 2 * (1 - flare)), color, flare, light);
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .4f, 0), 2.5f * size, color, flare * .8f, light);
            }
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .08f, 0), .7f, color, alpha * .8f, light);
            // The plague's and the leech's thread to their victim
            Entity v = e.victim();
            if (v != null && e.isSprung() && (e.getKind() == TrapScene.Kind.PLAGUE || e.getKind() == TrapScene.Kind.LEECH)) {
                Vector3f to = new Vector3f((float) (v.getX() - e.getX()), (float) (v.getY() - e.getY()) + v.getBbHeight() * .5f, (float) (v.getZ() - e.getZ()));
                VertexConsumer beam = BossMesh.light(buffer, WEB);
                Vector3f prev = new Vector3f(0, .1f, 0);
                for (int i = 1; i <= 10; i++) {
                    float t = i / 10f;
                    Vector3f p = new Vector3f(0, .1f, 0).lerp(to, t).add(0, Mth.sin(t * Mth.PI) * .6f + .05f * Mth.sin(age * .5f + i), 0);
                    RitualMesh.shaft(beam, poseStack.last(), view.eye(), prev, p, .12f, BossMesh.glow(color, .8f * fade), light);
                    prev = p;
                }
                ClockMesh.glow(poseStack, buffer, view, to, 1.2f, color, .5f * fade, light);
            }
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void web(TrapScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int color, float radius, float alpha,
                            float flare, float shown, int light) {
        if (shown <= .01f) {
            return;
        }
        VertexConsumer strand = BossMesh.light(buffer, WEB);
        float r = radius * shown;
        int spokes = 12;
        float a0 = age * .003f;
        int c = BossMesh.glow(color, .55f + .45f * flare);
        // Spokes from the middle out
        for (int k = 0; k < spokes; k++) {
            float a = a0 + k * Mth.TWO_PI / spokes;
            RitualMesh.shaft(strand, poseStack.last(), new Vector3f(0, 50, 0), new Vector3f(0, .05f, 0), new Vector3f(Mth.cos(a) * r, .05f, Mth.sin(a) * r), .18f, c,
                    light);
        }
        // Rings of strands between them, sagging a little
        for (int ring = 1; ring <= 5; ring++) {
            float rr = r * ring / 5f;
            for (int k = 0; k < spokes; k++) {
                float a = a0 + k * Mth.TWO_PI / spokes, b = a0 + (k + 1) * Mth.TWO_PI / spokes;
                float sag = .85f + .15f * ((ring + k) % 2);
                Vector3f p = new Vector3f(Mth.cos(a) * rr, .05f, Mth.sin(a) * rr);
                Vector3f q = new Vector3f(Mth.cos(b) * rr, .05f, Mth.sin(b) * rr);
                Vector3f m = new Vector3f(p).add(q).mul(.5f * sag);
                m.y = .05f;
                RitualMesh.shaft(strand, poseStack.last(), new Vector3f(0, 50, 0), p, m, .12f, c, light);
                RitualMesh.shaft(strand, poseStack.last(), new Vector3f(0, 50, 0), m, q, .12f, c, light);
            }
        }
        BossMesh.plate(BossMesh.light(buffer, SIGIL), poseStack.last(), new Vector3f(0, .06f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), 3.5f,
                BossMesh.glow(0x9CFF50, .5f + .5f * flare), light);
        ClockMesh.ring(poseStack.last(), buffer, .05f, r - .5f, r, color, .5f * shown, light);
        if (flare > 0) {
            ClockMesh.ring(poseStack.last(), buffer, .07f, r * (1 - flare) * .9f, r * (1 - flare), 0x9CFF50, flare, light);
        }
    }

    @Override
    public boolean shouldRender(TrapScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TrapScene e) {
        return SIGIL;
    }
}
