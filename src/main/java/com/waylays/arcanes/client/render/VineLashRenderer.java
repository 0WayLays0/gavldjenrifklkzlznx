package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.VineLash;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Vine Lash: a thorny green vine from the caster's hand, whipping in waves as it shoots out and straightening once it
 * holds, leaves along it; wound three times round the enemy it caught, or ending in a claw of three roots dug into the
 * block it hooked; drawn back into the hand at the end.
 */
public class VineLashRenderer extends EntityRenderer<VineLash> {
    private static final int POINTS = 22;

    public VineLashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VineLash lash, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = lash.tickCount + partialTicks;
        float out = lash.extended(age);
        Entity owner = lash.lasher();
        if (out <= .01f || owner == null) {
            return;
        }
        Vec3 origin = new Vec3(Mth.lerp(partialTicks, lash.xo, lash.getX()), Mth.lerp(partialTicks, lash.yo, lash.getY()), Mth.lerp(partialTicks, lash.zo, lash.getZ()));
        Vec3 ownerAt = owner.getPosition(partialTicks);
        float yaw = (owner instanceof LivingEntity living ? Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot) : owner.getYRot()) * Mth.DEG_TO_RAD;
        // The right hand
        Vec3 hand = ownerAt.add(-.38 * Mth.cos(yaw) - .35 * Mth.sin(yaw), owner.getBbHeight() * .72, -.38 * Mth.sin(yaw) + .35 * Mth.cos(yaw)).subtract(origin);
        Entity target = lash.target();
        Vec3 end;
        if (lash.getMode() == VineLash.Mode.ENEMY && target != null) {
            end = target.getPosition(partialTicks).add(0, target.getBbHeight() * .55, 0).subtract(origin);
        } else {
            end = lash.anchor().subtract(origin);
        }
        Vec3 span = end.subtract(hand);
        double length = span.length();
        if (length < .2) {
            return;
        }
        Vec3 dir = span.scale(1 / length);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(dir).normalize();
        // Whipping in waves on the way out, sagging a little once it holds
        boolean holding = age >= VineLash.REACH && age < lash.letGo();
        float wave = holding ? .05f : .45f * (1 - out * .5f);
        int seed = lash.getId() * 71;
        Vector3f[] pts = new Vector3f[POINTS];
        for (int i = 0; i < POINTS; i++) {
            float u = (float) i / (POINTS - 1) * out;
            double s = Math.sin(u * 14 - age * 1.3f) * wave * Mth.sin(u / Math.max(out, .01f) * Mth.PI);
            double sag = holding ? -.35 * Math.sin(u * Math.PI) * Math.min(1, length / 10) : 0;
            Vec3 at = hand.add(span.scale(u)).add(side.scale(s)).add(up.scale(s * .4 + sag));
            pts[i] = new Vector3f((float) at.x, (float) at.y, (float) at.z);
        }
        VertexConsumer vine = WoodRender.vine(buffer);
        PoseStack.Pose p = poseStack.last();
        WoodRender.thornyVine(vine, p, pts, .07f, light, seed);
        Vector3f tip = pts[POINTS - 1];
        if (out >= .98f && lash.getMode() == VineLash.Mode.ENEMY && target != null) {
            // Wound round the body
            float r = target.getBbWidth() * .62f + .08f;
            float h = target.getBbHeight();
            Vec3 base = target.getPosition(partialTicks).subtract(origin);
            Vector3f[] coil = new Vector3f[30];
            for (int i = 0; i < coil.length; i++) {
                float u = (float) i / (coil.length - 1);
                float a = u * Mth.TWO_PI * 3 + seed;
                coil[i] = new Vector3f((float) base.x + Mth.cos(a) * r, (float) base.y + h * (.25f + .5f * u), (float) base.z + Mth.sin(a) * r);
            }
            WoodRender.thornyVine(vine, p, coil, .06f, light, seed + 5);
        } else if (out >= .98f && lash.getMode() == VineLash.Mode.GRAPPLE) {
            // A claw of three roots dug in
            for (int k = 0; k < 3; k++) {
                float a = k * Mth.TWO_PI / 3 + seed;
                Vec3 o = side.scale(Mth.cos(a) * .4).add(up.scale(Mth.sin(a) * .4));
                Vector3f mid = new Vector3f(tip).add((float) (o.x + dir.x * .2), (float) (o.y + dir.y * .2), (float) (o.z + dir.z * .2));
                Vector3f point = new Vector3f(tip).add((float) (o.x * .5 + dir.x * .55), (float) (o.y * .5 + dir.y * .55), (float) (o.z * .5 + dir.z * .55));
                BoneRender.curve(WoodRender.bark(buffer), poseStack.last(), new Vector3f[]{tip, mid, point}, .09f, .02f, 5, light, BoneRender.WHITE);
            }
            vine = null;
        }
        // Leaves along it
        VertexConsumer leaf = WoodRender.leafBuffer(buffer);
        for (int i = 2; i < POINTS - 1; i += 3) {
            Vector3f at = pts[i];
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(Axis.YP.rotation(WoodFx.hash(seed + i) * Mth.TWO_PI));
            poseStack.mulPose(Axis.XP.rotation(.8f + .2f * Mth.sin(age * .3f + i)));
            poseStack.translate(0, .1f, 0);
            WoodRender.leaf(leaf, poseStack.last(), .24f, WoodRender.leafColour(seed + i), light);
            poseStack.popPose();
        }
        super.render(lash, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(VineLash lash, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(VineLash lash) {
        return WoodRender.VINE;
    }
}
