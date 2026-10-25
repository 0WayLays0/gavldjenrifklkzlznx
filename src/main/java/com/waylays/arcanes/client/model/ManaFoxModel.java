package com.waylays.arcanes.client.model;

import com.waylays.arcanes.entity.errethys.ManaFox;
import com.waylays.arcanes.ArcanesOublies;
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
 * Fox: a slim body with a ruff of fur at the chest, a narrow head with a long pointed snout, cheek tufts and two
 * big pointed ears, slender legs and a great bushy tail in three parts that sways and streams out when it runs.
 */
public class ManaFoxModel extends HierarchicalModel<ManaFox> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_fox"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs;
    private final ModelPart[] tail;

    public ManaFoxModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.legs = new ModelPart[]{root.getChild("leg0"), root.getChild("leg1"), root.getChild("leg2"), root.getChild("leg3")};
        ModelPart t0 = root.getChild("tail0");
        ModelPart t1 = t0.getChild("tail1");
        this.tail = new ModelPart[]{t0, t1, t1.getChild("tail2")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3F, -2.5F, -5F, 6, 5, 10)
                .texOffs(0, 16).addBox(-3.4F, -2.9F, -5.6F, 6.8F, 5.8F, 3), PartPose.offset(0, 16.5F, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 26).addBox(-3.5F, -3F, -4.5F, 7, 6, 5)
                .texOffs(24, 26).addBox(-1.5F, .2F, -7.5F, 3, 2.3F, 3)
                .texOffs(36, 26).addBox(-.6F, .1F, -7.9F, 1.2F, .9F, .5F)
                .texOffs(0, 38).addBox(-3.4F, -6.2F, -2.2F, 2.4F, 3.4F, 1)
                .texOffs(0, 38).addBox(1F, -6.2F, -2.2F, 2.4F, 3.4F, 1)
                .texOffs(8, 38).addBox(-4.1F, -.5F, -3F, 1, 2.6F, 2.6F)
                .texOffs(8, 38).addBox(3.1F, -.5F, -3F, 1, 2.6F, 2.6F)
                .texOffs(60, 60).addBox(-2.7F, -1.3F, -4.65F, 1.3F, .9F, .3F)
                .texOffs(60, 60).addBox(1.4F, -1.3F, -4.65F, 1.3F, .9F, .3F), PartPose.offset(0, 15F, -5F));
        float[][] legPos = {{1.6F, -3.3F}, {-1.6F, -3.3F}, {1.6F, 3.4F}, {-1.6F, 3.4F}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(40, 0).addBox(-.9F, 0, -.9F, 1.8F, 5, 1.8F), PartPose.offset(legPos[i][0], 19F, legPos[i][1]));
        }
        PartDefinition t0 = root.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(40, 10).addBox(-1.6F, -1.6F, 0, 3.2F, 3.2F, 4), PartPose.offsetAndRotation(0, 15.2F, 5F, -.5F, 0, 0));
        PartDefinition t1 = t0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(40, 20).addBox(-2.2F, -2.2F, 0, 4.4F, 4.4F, 4.5F), PartPose.offsetAndRotation(0, 0, 3.6F, .15F, 0, 0));
        t1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(40, 32).addBox(-1.5F, -1.5F, 0, 3, 3, 3.2F), PartPose.offsetAndRotation(0, 0, 4.2F, .15F, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaFox entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // A light trot: diagonal pairs
        float stride = Mth.cos(limbSwing * 1.6F);
        legs[0].xRot = stride * 1.2F * limbSwingAmount;
        legs[3].xRot = stride * 1.2F * limbSwingAmount;
        legs[1].xRot = -stride * 1.2F * limbSwingAmount;
        legs[2].xRot = -stride * 1.2F * limbSwingAmount;
        float bob = Mth.abs(Mth.sin(limbSwing * 1.6F)) * -.5F * limbSwingAmount;
        body.y = 16.5F + bob;
        head.y = 15F + bob;
        // The brush sways at rest and streams out behind when it runs
        tail[0].xRot = -.5F + limbSwingAmount * .35F;
        for (int i = 0; i < tail.length; i++) {
            tail[i].yRot = Mth.sin(ageInTicks * .18F - i * .8F) * (.22F - .12F * limbSwingAmount);
        }
    }
}
