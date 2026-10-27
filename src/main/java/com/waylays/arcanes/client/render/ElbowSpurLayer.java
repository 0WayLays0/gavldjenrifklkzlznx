package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * The Elbow Blade's bone: while a body holds a Bone Blade, a spur of bone juts out behind the elbow of that arm,
 * curving back like a horn, where the blade split out of it.
 */
public class ElbowSpurLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public ElbowSpurLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        boolean main = entity.getMainHandItem().is(ModItems.BONE_BLADE.get());
        boolean off = entity.getOffhandItem().is(ModItems.BONE_BLADE.get());
        if (!main && !off) {
            return;
        }
        HumanoidArm arm = main ? entity.getMainArm() : entity.getMainArm().getOpposite();
        ModelPart part = arm == HumanoidArm.RIGHT ? getParentModel().rightArm : getParentModel().leftArm;
        float side = arm == HumanoidArm.RIGHT ? -1 : 1;
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        // The arm's own space: sixteenths of a block, Y down, the back of the arm towards +Z
        float u = 1 / 16f;
        Vector3f elbow = new Vector3f(side * u, 4.5f * u, 1.2f * u);
        BoneRender.spike(bone, p, elbow, new Vector3f(side * 1.4f * u, .5f * u, 11f * u), 1.5f * u, new Vector3f(0, 1.6f * u, 0), packedLight, BoneRender.WHITE);
        BoneRender.spike(bone, p, new Vector3f(side * u, 6.5f * u, 1.6f * u), new Vector3f(side * 1.8f * u, 5f * u, 6.5f * u), .8f * u, new Vector3f(),
                packedLight, BoneRender.WHITE);
        poseStack.popPose();
    }
}
