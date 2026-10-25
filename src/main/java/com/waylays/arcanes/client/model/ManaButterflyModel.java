package com.waylays.arcanes.client.model;

import com.waylays.arcanes.entity.errethys.ManaButterfly;
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
 * Butterfly: a slender body and a small head with two long curling antennae, and four broad wings (a big fore wing
 * and a round hind wing each side) beating fast while it flutters, the hind wings a beat behind.
 */
public class ManaButterflyModel extends HierarchicalModel<ManaButterfly> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_butterfly"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart foreLeft;
    private final ModelPart foreRight;
    private final ModelPart hindLeft;
    private final ModelPart hindRight;
    private final ModelPart[] antennae;

    public ManaButterflyModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.foreLeft = body.getChild("fore_left");
        this.foreRight = body.getChild("fore_right");
        this.hindLeft = body.getChild("hind_left");
        this.hindRight = body.getChild("hind_right");
        this.antennae = new ModelPart[]{body.getChild("antenna_left"), body.getChild("antenna_right")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-.6F, -.6F, -2.2F, 1.2F, 1.2F, 5)
                .texOffs(0, 8).addBox(-.8F, -.8F, -3.4F, 1.6F, 1.6F, 1.3F)
                .texOffs(60, 60).addBox(-.95F, -.6F, -3.45F, .4F, .55F, .5F)
                .texOffs(60, 60).addBox(.55F, -.6F, -3.45F, .4F, .55F, .5F), PartPose.offset(0, 19.5F, 0));
        body.addOrReplaceChild("antenna_left", CubeListBuilder.create().texOffs(10, 0).addBox(-.12F, -.12F, -3.2F, .25F, .25F, 3.2F)
                .texOffs(10, 4).addBox(-.25F, -.25F, -3.5F, .5F, .5F, .5F), PartPose.offsetAndRotation(.4F, -.6F, -3.2F, -.7F, -.35F, 0));
        body.addOrReplaceChild("antenna_right", CubeListBuilder.create().texOffs(10, 0).addBox(-.12F, -.12F, -3.2F, .25F, .25F, 3.2F)
                .texOffs(10, 4).addBox(-.25F, -.25F, -3.5F, .5F, .5F, .5F), PartPose.offsetAndRotation(-.4F, -.6F, -3.2F, -.7F, .35F, 0));
        body.addOrReplaceChild("fore_left", CubeListBuilder.create().texOffs(0, 20).addBox(0, -.05F, -4.6F, 7.5F, .1F, 5.2F), PartPose.offset(.6F, -.3F, -.6F));
        body.addOrReplaceChild("fore_right", CubeListBuilder.create().texOffs(0, 20).addBox(-7.5F, -.05F, -4.6F, 7.5F, .1F, 5.2F), PartPose.offset(-.6F, -.3F, -.6F));
        body.addOrReplaceChild("hind_left", CubeListBuilder.create().texOffs(0, 30).addBox(0, -.05F, 0, 5.2F, .1F, 4.6F), PartPose.offset(.6F, -.3F, -.2F));
        body.addOrReplaceChild("hind_right", CubeListBuilder.create().texOffs(0, 30).addBox(-5.2F, -.05F, 0, 5.2F, .1F, 4.6F), PartPose.offset(-.6F, -.3F, -.2F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaButterfly entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Fast wing beats, from nearly closed above its back to spread flat; the hind wings follow a beat behind
        float fore = .35F + .85F * (.5F + .5F * Mth.cos(ageInTicks * 1.3F));
        float hind = .3F + .8F * (.5F + .5F * Mth.cos(ageInTicks * 1.3F - .6F));
        foreLeft.zRot = -fore;
        foreRight.zRot = fore;
        hindLeft.zRot = -hind;
        hindRight.zRot = hind;
        // It bobs a little with each beat and rocks in the air
        body.y = 19.5F + Mth.sin(ageInTicks * 1.3F) * .5F;
        body.xRot = -.15F + Mth.sin(ageInTicks * .2F) * .08F;
        for (int i = 0; i < antennae.length; i++) {
            antennae[i].xRot = -.7F + Mth.sin(ageInTicks * .25F + i) * .1F;
        }
    }
}
