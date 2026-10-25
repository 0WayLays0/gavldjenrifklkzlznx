package com.waylays.arcanes.client.model;

import com.waylays.arcanes.entity.errethys.ManaWolf;
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
 * Wolf: a long body behind a heavy mane over the shoulders, a broad head with a strong muzzle and upright ears, long
 * legs and a two-part tail carried low that lifts when it runs.
 */
public class ManaWolfModel extends HierarchicalModel<ManaWolf> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_wolf"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs;
    private final ModelPart[] tail;

    public ManaWolfModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.legs = new ModelPart[]{root.getChild("leg0"), root.getChild("leg1"), root.getChild("leg2"), root.getChild("leg3")};
        ModelPart t0 = root.getChild("tail0");
        this.tail = new ModelPart[]{t0, t0.getChild("tail1")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -3F, -5F, 7, 6, 11)
                .texOffs(0, 18).addBox(-4.5F, -4.3F, -7.5F, 9, 8.3F, 6), PartPose.offset(0, 14.5F, 1F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-3.5F, -3.5F, -5F, 7, 6.5F, 5.5F)
                .texOffs(26, 34).addBox(-1.8F, -.2F, -9F, 3.6F, 3, 4.2F)
                .texOffs(42, 34).addBox(-.8F, -.4F, -9.3F, 1.6F, 1, .5F)
                .texOffs(0, 48).addBox(-3.3F, -6.6F, -2.6F, 2.2F, 3.3F, 1.2F)
                .texOffs(0, 48).addBox(1.1F, -6.6F, -2.6F, 2.2F, 3.3F, 1.2F)
                .texOffs(10, 48).addBox(-4.2F, -1F, -3.5F, 1, 3.5F, 3)
                .texOffs(10, 48).addBox(3.2F, -1F, -3.5F, 1, 3.5F, 3)
                .texOffs(60, 60).addBox(-2.8F, -1.7F, -5.15F, 1.4F, .9F, .3F)
                .texOffs(60, 60).addBox(1.4F, -1.7F, -5.15F, 1.4F, .9F, .3F), PartPose.offset(0, 12.5F, -7F));
        float[][] legPos = {{2.3F, -4.6F}, {-2.3F, -4.6F}, {2.3F, 5F}, {-2.3F, 5F}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(44, 0).addBox(-1.2F, 0, -1.2F, 2.4F, 6.5F, 2.4F), PartPose.offset(legPos[i][0], 17.5F, legPos[i][1]));
        }
        PartDefinition t0 = root.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(44, 12).addBox(-1.3F, -1.3F, 0, 2.6F, 2.6F, 6), PartPose.offsetAndRotation(0, 12.5F, 6.5F, -.7F, 0, 0));
        t0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(44, 22).addBox(-1.1F, -1.1F, 0, 2.2F, 2.2F, 4.5F), PartPose.offsetAndRotation(0, 0, 5.6F, .3F, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaWolf entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // A loping gallop: front pair then back pair
        float stride = Mth.cos(limbSwing * 1.2F);
        legs[0].xRot = stride * 1.1F * limbSwingAmount;
        legs[1].xRot = Mth.cos(limbSwing * 1.2F + .5F) * 1.1F * limbSwingAmount;
        legs[2].xRot = -stride * 1.1F * limbSwingAmount;
        legs[3].xRot = -Mth.cos(limbSwing * 1.2F + .5F) * 1.1F * limbSwingAmount;
        float bob = Mth.sin(limbSwing * 1.2F) * -.6F * limbSwingAmount;
        body.y = 14.5F + bob;
        head.y = 12.5F + bob;
        tail[0].xRot = -.7F + limbSwingAmount * .45F;
        tail[0].yRot = Mth.sin(ageInTicks * .15F) * .15F;
        tail[1].yRot = Mth.sin(ageInTicks * .15F - .8F) * .2F;
    }
}
