package com.waylays.arcanes.client.model;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaCrow;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Crow: a sleek body with a fanned tail, a head with a long pointed beak, broad wings and thin legs. In the air it
 * beats its wings while climbing or cruising, holds them out flat to glide when coming down, tucks its legs back
 * and tilts with its climb; on the ground it folds its wings along its back and stands chest up.
 */
public class ManaCrowModel extends HierarchicalModel<ManaCrow> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_crow"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ManaCrowModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.tail = body.getChild("tail");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -3F, 3, 3, 6), PartPose.offset(0, 18, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 10).addBox(-1.25F, -2F, -2.5F, 2.5F, 2.5F, 2.5F)
                .texOffs(10, 10).addBox(-.5F, -1.1F, -4.6F, 1, 1, 2.2F)
                .texOffs(60, 60).addBox(-1.35F, -1.5F, -2F, .4F, .6F, .6F)
                .texOffs(60, 60).addBox(.95F, -1.5F, -2F, .4F, .6F, .6F), PartPose.offset(0, -1.2F, -2.6F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 16).addBox(0, -.3F, 0, 6, .6F, 4.2F), PartPose.offset(1.4F, -1.2F, -1.8F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 22).addBox(-6, -.3F, 0, 6, .6F, 4.2F), PartPose.offset(-1.4F, -1.2F, -1.8F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 28).addBox(-1.6F, -.2F, 0, 3.2F, .4F, 3.8F), PartPose.offset(0, -.6F, 2.8F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 0).addBox(-.25F, 0, -.25F, .5F, 4.7F, .5F), PartPose.offset(.7F, 19.3F, .5F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 0).addBox(-.25F, 0, -.25F, .5F, 4.7F, .5F), PartPose.offset(-.7F, 19.3F, .5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaCrow entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        if (entity.onGround()) {
            // Perched: wings folded along the back, chest up, a little hop in the step
            body.xRot = -.35f;
            leftWing.yRot = -1.45f;
            rightWing.yRot = 1.45f;
            leftWing.zRot = .25f;
            rightWing.zRot = -.25f;
            tail.xRot = .35f;
            leftLeg.xRot = Mth.cos(limbSwing * 1.8f) * limbSwingAmount;
            rightLeg.xRot = -Mth.cos(limbSwing * 1.8f) * limbSwingAmount;
            head.xRot += Mth.sin(ageInTicks * .3f) * .08f;
            return;
        }
        float climb = (float) (entity.getY() - entity.yo);
        boolean gliding = climb < -.04f;
        body.xRot = Mth.clamp(-climb * 2.5f, -.5f, .5f);
        leftWing.yRot = 0;
        rightWing.yRot = 0;
        float flap = gliding ? -.12f + Mth.sin(ageInTicks * .2f) * .05f : Mth.sin(ageInTicks * 1.15f) * .9f + .1f;
        leftWing.zRot = flap;
        rightWing.zRot = -flap;
        tail.xRot = .1f + Mth.sin(ageInTicks * .3f) * .05f;
        leftLeg.xRot = 1.3f;
        rightLeg.xRot = 1.3f;
    }
}
