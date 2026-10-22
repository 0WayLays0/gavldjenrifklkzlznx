package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.gem.DiamondShell;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Diamond Shell: twelve cut diamond plates round the caster, each facing out, the shell turning slowly, sparkling
 * in every colour; they close in from afar when it is cast.
 */
public class DiamondShellRenderer extends EntityRenderer<DiamondShell> {
    public DiamondShellRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DiamondShell shell, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = shell.tickCount + partialTicks;
        Entity caster = shell.getCaster();
        Vec3 base = shell.getPosition(partialTicks);
        Vec3 c = caster != null ? caster.getPosition(partialTicks).add(0, DiamondShell.middle(caster), 0).subtract(base) : new Vec3(0, 1, 0);
        float close = Mth.clamp(age / 10, 0, 1);
        float r = DiamondShell.RADIUS * (1 + 1.6f * (1 - close) * (1 - close));
        float fade = Mth.clamp((shell.getLife() - age) / 15, 0, 1);
        int plates = shell.getPlates();
        for (int i = 0; i < DiamondShell.PLATES; i++) {
            if ((plates & (1 << i)) == 0) {
                continue;
            }
            Vec3 f = DiamondShell.facing(i, age);
            poseStack.pushPose();
            poseStack.translate(c.x + f.x * r, c.y + f.y * r, c.z + f.z * r);
            // The plate's table faces out
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), f.toVector3f()));
            poseStack.scale(1, .45f, 1);
            GemRender.gem(poseStack, buffer, GemRender.Cut.BRILLIANT, Gem.DIAMOND.color, .62f * close, .45f * (.5f + .5f * fade), age * 2 + i * 13);
            poseStack.popPose();
        }
        for (int i = 0; i < DiamondShell.PLATES; i++) {
            if ((plates & (1 << i)) != 0) {
                Vec3 f = DiamondShell.facing(i, age);
                poseStack.pushPose();
                poseStack.translate(c.x + f.x * r * 1.04, c.y + f.y * r * 1.04, c.z + f.z * r * 1.04);
                GemRender.sparkles(poseStack, buffer, GemRender.Cut.BRILLIANT, Gem.DIAMOND.color, .6f, age * 2, true, i * 7 + shell.getId());
                poseStack.popPose();
            }
        }
        GemRender.glow(poseStack, buffer, c, Gem.DIAMOND.color, r * 1.3f, .12f * close * fade);
        super.render(shell, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DiamondShell shell) {
        return GemRender.FACET;
    }
}
