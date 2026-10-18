package com.waylays.arcanes.client.model;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaRat;
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
 * Rat: a low body, a pointed head with round ears and a snout, four short legs and a long three-part tail that sways
 * and trails. Eyes use the bright corner of the mana skin.
 */
public class ManaRatModel extends HierarchicalModel<ManaRat> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_rat"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs;
    private final ModelPart[] tail;

    public ManaRatModel(ModelPart root) {
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
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -2F, -3.5F, 4, 3.5F, 7), PartPose.offset(0, 21, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-1.5F, -1.5F, -3F, 3, 3, 3)
                .texOffs(12, 12).addBox(-1F, -.5F, -5F, 2, 1.8F, 2)
                .texOffs(0, 18).addBox(-1.9F, -2.8F, -1.2F, 1.4F, 1.4F, .4F)
                .texOffs(0, 18).addBox(.5F, -2.8F, -1.2F, 1.4F, 1.4F, .4F)
                .texOffs(60, 60).addBox(-1.3F, -1.1F, -3.15F, .7F, .7F, .3F)
                .texOffs(60, 60).addBox(.6F, -1.1F, -3.15F, .7F, .7F, .3F), PartPose.offset(0, 20.5F, -3.5F));
        float[][] legPos = {{1.3F, -2F}, {-1.3F, -2F}, {1.3F, 2.2F}, {-1.3F, 2.2F}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(24, 0).addBox(-.5F, 0, -.5F, 1, 2.2F, 1), PartPose.offset(legPos[i][0], 21.8F, legPos[i][1]));
        }
        PartDefinition t0 = root.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(24, 4).addBox(-.5F, -.5F, 0, 1, 1, 3), PartPose.offsetAndRotation(0, 20.5F, 3.5F, -.25F, 0, 0));
        PartDefinition t1 = t0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(24, 9).addBox(-.4F, -.4F, 0, .8F, .8F, 3), PartPose.offsetAndRotation(0, 0, 3, .2F, 0, 0));
        t1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(24, 14).addBox(-.3F, -.3F, 0, .6F, .6F, 3), PartPose.offsetAndRotation(0, 0, 3, .15F, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaRat entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD + Mth.sin(ageInTicks * .9f) * .04f;
        // Scurrying: quick diagonal gait
        float stride = Mth.cos(limbSwing * 2.2f);
        legs[0].xRot = stride * 1.3f * limbSwingAmount;
        legs[3].xRot = stride * 1.3f * limbSwingAmount;
        legs[1].xRot = -stride * 1.3f * limbSwingAmount;
        legs[2].xRot = -stride * 1.3f * limbSwingAmount;
        body.y = 21 + Mth.abs(Mth.sin(limbSwing * 2.2f)) * -.4f * limbSwingAmount;
        // The tail swishes from side to side, the tip lagging behind
        for (int i = 0; i < tail.length; i++) {
            tail[i].yRot = Mth.sin(ageInTicks * .25f - i * .9f) * (.25f + .2f * limbSwingAmount);
        }
    }
}
