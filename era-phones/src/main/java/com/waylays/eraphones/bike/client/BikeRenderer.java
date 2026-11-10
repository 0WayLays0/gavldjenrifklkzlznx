package com.waylays.eraphones.bike.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.bike.BikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The bicycle drawn from its parts: the frame, the two wheels turning with the ground covered, the cranks turning
 * and the pedals staying level under the feet, the fork and bars turning into the turns about the slanted head tube,
 * the whole leaning into the turns.
 */
public class BikeRenderer extends EntityRenderer<BikeEntity> {
    public static final ModelResourceLocation FRAME = part("velo_cadre"), WHEEL = part("velo_roue"), CRANK = part("velo_pedalier"),
            PEDAL = part("velo_pedale"), STEER = part("velo_guidon");
    /** Where the parts turn (model sixteenths: bike units moved by 8 across and 8 forward). */
    private static final float REAR_Y = 5.3f, REAR_Z = -.5f, FRONT_Y = 5.3f, FRONT_Z = 15.36f, BB_Y = 4.8f, BB_Z = 6.5f, HEAD_Y = 11f,
            HEAD_Z = 13f, HEAD_TILT = 22.5f, CRANK_R = 2.6f, PEDAL_X = 2.45f;

    public BikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = .55f;
    }

    static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(EraPhones.id("block/" + name));
    }

    @Override
    public ResourceLocation getTextureLocation(BikeEntity bike) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(BikeEntity bike, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        float spin = Mth.lerp(pt, bike.spinO, bike.spin), crank = Mth.lerp(pt, bike.crankO, bike.crank);
        float steer = Mth.lerp(pt, bike.steerO, bike.steer), lean = Mth.lerp(pt, bike.leanO, bike.lean);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(pt, bike.yRotO, bike.getYRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(lean));
        pose.translate(-.5, 0, -.5);
        draw(FRAME, pose, vc, light);
        // The rear wheel
        pose.pushPose();
        turn(pose, REAR_Y, REAR_Z, spin);
        draw(WHEEL, pose, vc, light);
        pose.popPose();
        // The cranks, and the pedals level at their ends
        pose.pushPose();
        turn(pose, BB_Y, BB_Z, crank);
        draw(CRANK, pose, vc, light);
        pose.popPose();
        for (int side = -1; side <= 1; side += 2) {
            // The drive side's arm (at -x) points up when the cranks are at rest, the other down
            float a = crank + (side < 0 ? 0 : Mth.PI);
            pose.pushPose();
            pose.translate((8 + side * PEDAL_X) / 16, (BB_Y + CRANK_R * Mth.cos(a)) / 16, (BB_Z + CRANK_R * Mth.sin(a)) / 16);
            pose.translate(-.5, -.5, -.5);
            draw(PEDAL, pose, vc, light);
            pose.popPose();
        }
        // The steering, about the head tube leaning back; the front wheel with it
        pose.pushPose();
        pose.translate(8 / 16f, HEAD_Y / 16, HEAD_Z / 16);
        pose.mulPose(Axis.XP.rotationDegrees(-HEAD_TILT));
        pose.mulPose(Axis.YP.rotationDegrees(-steer));
        pose.mulPose(Axis.XP.rotationDegrees(HEAD_TILT));
        pose.translate(-8 / 16f, -HEAD_Y / 16, -HEAD_Z / 16);
        draw(STEER, pose, vc, light);
        turn(pose, FRONT_Y, FRONT_Z, spin);
        draw(WHEEL, pose, vc, light);
        pose.popPose();
        pose.popPose();
        super.render(bike, yaw, pt, pose, buffers, light);
    }

    /** Moves to a part drawn round (8, 8, 8) turning about the x axis at (y, z). */
    private static void turn(PoseStack pose, float y, float z, float angle) {
        pose.translate(.5, y / 16, z / 16);
        pose.mulPose(Axis.XP.rotation(angle));
        pose.translate(-.5, -.5, -.5);
    }

    private static void draw(ModelResourceLocation id, PoseStack pose, VertexConsumer vc, int light) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), vc, null, model, 1, 1, 1, light,
                OverlayTexture.NO_OVERLAY);
    }
}
