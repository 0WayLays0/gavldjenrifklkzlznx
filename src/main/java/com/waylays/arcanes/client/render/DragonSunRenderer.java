package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.DragonSun;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The giant sun the Golden Dragon pours out of: the jewelled sun stone of Eldorado blown up to fill the sky, facing
 * whoever looks at it, in a vast corona, with a white-hot heart where the dragon comes out. It swells into being,
 * blazes, then shrinks to a spark and is gone.
 */
public class DragonSunRenderer extends EntityRenderer<DragonSun> {
    public DragonSunRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DragonSun sun, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sun.tickCount + partialTicks;
        float rise = Mth.clamp(age / 12, 0, 1);
        rise = 1 - (1 - rise) * (1 - rise);
        float fade = Mth.clamp((DragonSun.LIFE - age) / 14, 0, 1);
        float size = Math.max(.02f, rise * fade * (1 + Mth.sin(age * .25f) * .03f));
        float half = sun.getBbHeight() * .5f;
        Vec3 centre = new Vec3(0, half, 0);

        GemRender.glow(poseStack, buffer, centre, GoldRender.GOLD, 26 * size, .5f);
        GemRender.glow(poseStack, buffer, centre, GoldRender.MOLTEN, 13 * size, .8f);

        poseStack.pushPose();
        poseStack.translate(0, half, 0);
        GoldRender.faceCamera(poseStack, sun.getPosition(partialTicks).add(centre));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sun.getYRot() + age * .8f));
        GoldRender.sunStone(poseStack, buffer, DragonSun.RADIUS * size, GoldRender.GOLD, GoldRender.DEEP, age, sun.getId());
        poseStack.popPose();

        // The heart it opens at, white-hot while the dragon comes out: just in front of the disc's face
        Vec3 toEye = this.entityRenderDispatcher.camera.getPosition().subtract(sun.getPosition(partialTicks).add(centre)).normalize();
        GemRender.glow(poseStack, buffer, centre.add(toEye.scale(DragonSun.RADIUS * size * .2f)), 0xFFF4D8, 4.5f * size, (1 - fade * .3f) * Mth.clamp(age / 6, 0, 1));
        super.render(sun, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DragonSun sun) {
        return GoldRender.SIGIL;
    }
}
