package com.waylays.arcanes.client.model;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaScarab;
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
 * Scarab: a broad belly under two domed wing-cases parted by a seam, the shield of its thorax in front, a small head with
 * a rake of teeth, clubbed antennae and eyes of light, six splayed legs. Its legs scuttle in two alternating triplets;
 * when it is off the ground its wing-cases lift and spread and its wings, folded under them, unfold and beat.
 */
public class ManaScarabModel extends HierarchicalModel<ManaScarab> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_scarab"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart elytronLeft, elytronRight, wingLeft, wingRight;
    private final ModelPart[] legs;

    public ManaScarabModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.elytronLeft = root.getChild("elytron_left");
        this.elytronRight = root.getChild("elytron_right");
        this.wingLeft = root.getChild("wing_left");
        this.wingRight = root.getChild("wing_right");
        this.legs = new ModelPart[]{root.getChild("leg_r0"), root.getChild("leg_l0"), root.getChild("leg_r1"), root.getChild("leg_l1"),
                root.getChild("leg_r2"), root.getChild("leg_l2")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3F, -1.5F, -3.6F, 6F, 2.5F, 7.4F), PartPose.offset(0F, 21F, 0F));
        root.addOrReplaceChild("thorax", CubeListBuilder.create()
                .texOffs(0, 10).addBox(-3.2F, -2.1F, -2.6F, 6.4F, 2.6F, 3F)
                .texOffs(0, 16).addBox(-2.6F, -2.6F, -2.2F, 5.2F, 0.6F, 2.4F), PartPose.offset(0F, 20.4F, -3.4F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(20, 10).addBox(-2F, -1.2F, -2.2F, 4F, 2F, 2.2F)
                .texOffs(20, 15).addBox(-2.7F, -0.5F, -3F, 5.4F, 0.7F, 0.9F)
                .texOffs(34, 15).addBox(-2.7F, -0.5F, -3.5F, 0.8F, 0.7F, 0.5F)
                .texOffs(34, 15).addBox(-1.2F, -0.5F, -3.6F, 0.8F, 0.7F, 0.6F)
                .texOffs(34, 15).addBox(0.4F, -0.5F, -3.6F, 0.8F, 0.7F, 0.6F)
                .texOffs(34, 15).addBox(1.9F, -0.5F, -3.5F, 0.8F, 0.7F, 0.5F)
                .texOffs(60, 60).addBox(-2.3F, -1F, -1.6F, 0.6F, 0.8F, 0.9F)
                .texOffs(60, 60).addBox(1.7F, -1F, -1.6F, 0.6F, 0.8F, 0.9F)
                .texOffs(40, 10).addBox(-1.9F, 0.1F, -2.6F, 0.3F, 0.3F, 1.4F)
                .texOffs(40, 10).addBox(1.6F, 0.1F, -2.6F, 0.3F, 0.3F, 1.4F), PartPose.offset(0F, 21F, -6F));
        root.addOrReplaceChild("elytron_left", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3.4F, -0.3F, 0F, 3.4F, 2.6F, 7.6F)
                .texOffs(0, 34).addBox(-3F, -1F, 0.4F, 2.8F, 0.8F, 6.6F)
                .texOffs(16, 34).addBox(-2.3F, -1.4F, 1.2F, 2F, 0.5F, 4.8F), PartPose.offset(0F, 19.3F, -3.3F));
        root.addOrReplaceChild("elytron_right", CubeListBuilder.create()
                .texOffs(22, 24).addBox(0F, -0.3F, 0F, 3.4F, 2.6F, 7.6F)
                .texOffs(24, 34).addBox(0.2F, -1F, 0.4F, 2.8F, 0.8F, 6.6F)
                .texOffs(40, 34).addBox(0.3F, -1.4F, 1.2F, 2F, 0.5F, 4.8F), PartPose.offset(0F, 19.3F, -3.3F));
        root.addOrReplaceChild("wing_left", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-3.2F, -0.1F, 0F, 3.4F, 0.2F, 8F), PartPose.offset(-0.5F, 19.4F, -2.8F));
        root.addOrReplaceChild("wing_right", CubeListBuilder.create()
                .texOffs(16, 44).addBox(-0.2F, -0.1F, 0F, 3.4F, 0.2F, 8F), PartPose.offset(0.5F, 19.4F, -2.8F));
        root.addOrReplaceChild("leg_r0", CubeListBuilder.create()
                .texOffs(32, 0).addBox(0F, -0.4F, -0.4F, 3.3F, 0.8F, 0.8F), PartPose.offsetAndRotation(2.8F, 21.6F, -3F, 0F, 0.45F, 0.95F));
        root.addOrReplaceChild("leg_l0", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-3.3F, -0.4F, -0.4F, 3.3F, 0.8F, 0.8F), PartPose.offsetAndRotation(-2.8F, 21.6F, -3F, 0F, -0.45F, -0.95F));
        root.addOrReplaceChild("leg_r1", CubeListBuilder.create()
                .texOffs(32, 0).addBox(0F, -0.4F, -0.4F, 3.3F, 0.8F, 0.8F), PartPose.offsetAndRotation(2.8F, 21.6F, 0F, 0F, 0F, 0.95F));
        root.addOrReplaceChild("leg_l1", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-3.3F, -0.4F, -0.4F, 3.3F, 0.8F, 0.8F), PartPose.offsetAndRotation(-2.8F, 21.6F, 0F, 0F, 0F, -0.95F));
        root.addOrReplaceChild("leg_r2", CubeListBuilder.create()
                .texOffs(32, 0).addBox(0F, -0.4F, -0.4F, 3.3F, 0.8F, 0.8F), PartPose.offsetAndRotation(2.8F, 21.6F, 3F, 0F, -0.45F, 0.95F));
        root.addOrReplaceChild("leg_l2", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-3.3F, -0.4F, -0.4F, 3.3F, 0.8F, 0.8F), PartPose.offsetAndRotation(-2.8F, 21.6F, 3F, 0F, 0.45F, -0.95F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaScarab entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * .5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * .3F;
        boolean flying = !entity.onGround() && !entity.isInWater();
        wingLeft.visible = wingRight.visible = flying;
        if (flying) {
            float beat = Mth.sin(ageInTicks * 2.6F) * .45F;
            elytronLeft.xRot = .45F;
            elytronLeft.yRot = .35F;
            elytronLeft.zRot = -.55F;
            elytronRight.xRot = .45F;
            elytronRight.yRot = -.35F;
            elytronRight.zRot = .55F;
            wingLeft.xRot = .15F;
            wingLeft.yRot = .9F;
            wingLeft.zRot = -.25F + beat;
            wingRight.xRot = .15F;
            wingRight.yRot = -.9F;
            wingRight.zRot = .25F - beat;
            for (ModelPart leg : legs) {
                leg.zRot *= 1.35F;
            }
        } else {
            // Two triplets in turn: front and back of one side with the middle of the other
            float run = limbSwing * 2.2F;
            for (int i = 0; i < legs.length; i++) {
                boolean right = i % 2 == 0;
                int pair = i / 2;
                boolean first = (pair == 1) != right;
                float phase = first ? 0 : Mth.PI;
                legs[i].yRot += Mth.cos(run + phase) * .55F * limbSwingAmount;
                legs[i].zRot += (right ? -1 : 1) * Math.max(0, Mth.sin(run + phase)) * .35F * limbSwingAmount;
            }
            // The wing-cases breathe, the antennae twitch
            float breath = Mth.sin(ageInTicks * .12F) * .025F;
            elytronLeft.zRot = -breath;
            elytronRight.zRot = breath;
            head.xRot += Mth.sin(ageInTicks * .3F) * .04F;
        }
        if (attackTime > 0) {
            head.xRot += Mth.sin(attackTime * Mth.PI) * .45F;
        }
    }
}
