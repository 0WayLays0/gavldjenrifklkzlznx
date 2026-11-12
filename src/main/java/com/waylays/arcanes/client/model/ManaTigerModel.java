package com.waylays.arcanes.client.model;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaTiger;
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
 * Tiger: a long, heavy body over a deep chest, a broad head with cheek ruffs, round ears, a strong muzzle and fangs of
 * light, thick legs on wide paws (haunches behind, shoulders in front) and a long tail carried low that curls up at its
 * end. It walks with a heavy, rolling gait, its head low; in the air it stretches out, forepaws reaching forward and
 * hind legs thrown back; it swipes with a forepaw when it strikes.
 */
public class ManaTigerModel extends HierarchicalModel<ManaTiger> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_tiger"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs;
    private final ModelPart[] tail;

    public ManaTigerModel(ModelPart root) {
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
                .texOffs(0, 0).addBox(-4F, -3.5F, -8F, 8F, 7F, 16F)
                .texOffs(0, 24).addBox(-4.5F, -4.2F, -10F, 9F, 8F, 5F)
                .texOffs(30, 24).addBox(-3.6F, 2.6F, -6F, 7.2F, 1.6F, 11F), PartPose.offset(0F, 12F, 1F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(30, 38).addBox(-3.5F, -3.6F, -5F, 7F, 6.6F, 5.5F)
                .texOffs(0, 50).addBox(-2.3F, -0.4F, -8F, 4.6F, 3.2F, 3F)
                .texOffs(14, 50).addBox(-1.1F, -0.8F, -8.4F, 2.2F, 1.2F, 0.8F)
                .texOffs(0, 56).addBox(-1.9F, 2.7F, -7.6F, 3.8F, 1.3F, 3.2F)
                .texOffs(48, 50).addBox(-4.4F, -0.6F, -3.8F, 1.2F, 4.2F, 3.6F)
                .texOffs(48, 50).addBox(3.2F, -0.6F, -3.8F, 1.2F, 4.2F, 3.6F)
                .texOffs(56, 38).addBox(-3.4F, -5.4F, -2.2F, 2.2F, 2F, 1F)
                .texOffs(56, 38).addBox(1.2F, -5.4F, -2.2F, 2.2F, 2F, 1F)
                .texOffs(60, 60).addBox(-2.7F, -1.7F, -5.15F, 1.5F, 0.9F, 0.3F)
                .texOffs(60, 60).addBox(1.2F, -1.7F, -5.15F, 1.5F, 0.9F, 0.3F)
                .texOffs(60, 60).addBox(-1.5F, 2.2F, -7.9F, 0.5F, 1.1F, 0.4F)
                .texOffs(60, 60).addBox(1F, 2.2F, -7.9F, 0.5F, 1.1F, 0.4F), PartPose.offset(0F, 9.5F, -9.5F));
        root.addOrReplaceChild("leg0", CubeListBuilder.create()
                .texOffs(12, 44).addBox(-1.8F, -1.5F, -1.9F, 3.6F, 4F, 3.8F)
                .texOffs(0, 38).addBox(-1.5F, 0F, -1.5F, 3F, 7.5F, 3F)
                .texOffs(12, 38).addBox(-1.8F, 7.4F, -2.6F, 3.6F, 1.6F, 3.8F), PartPose.offset(2.7F, 15F, -6.5F));
        root.addOrReplaceChild("leg1", CubeListBuilder.create()
                .texOffs(12, 44).addBox(-1.8F, -1.5F, -1.9F, 3.6F, 4F, 3.8F)
                .texOffs(0, 38).addBox(-1.5F, 0F, -1.5F, 3F, 7.5F, 3F)
                .texOffs(12, 38).addBox(-1.8F, 7.4F, -2.6F, 3.6F, 1.6F, 3.8F), PartPose.offset(-2.7F, 15F, -6.5F));
        root.addOrReplaceChild("leg2", CubeListBuilder.create()
                .texOffs(12, 44).addBox(-1.9F, -2F, -2.2F, 3.8F, 5F, 4.2F)
                .texOffs(0, 38).addBox(-1.5F, 0F, -1.5F, 3F, 7.5F, 3F)
                .texOffs(12, 38).addBox(-1.8F, 7.4F, -2.6F, 3.6F, 1.6F, 3.8F), PartPose.offset(2.6F, 15F, 7F));
        root.addOrReplaceChild("leg3", CubeListBuilder.create()
                .texOffs(12, 44).addBox(-1.9F, -2F, -2.2F, 3.8F, 5F, 4.2F)
                .texOffs(0, 38).addBox(-1.5F, 0F, -1.5F, 3F, 7.5F, 3F)
                .texOffs(12, 38).addBox(-1.8F, 7.4F, -2.6F, 3.6F, 1.6F, 3.8F), PartPose.offset(-2.6F, 15F, 7F));
        PartDefinition tail0 = root.addOrReplaceChild("tail0", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-1F, -1F, 0F, 2F, 2F, 6F), PartPose.offsetAndRotation(0F, 10F, 8.6F, -1F, 0F, 0F));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(16, 60).addBox(-0.9F, -0.9F, 0F, 1.8F, 1.8F, 5.5F), PartPose.offsetAndRotation(0F, 0F, 5.6F, 0.55F, 0F, 0F));
        tail1.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(30, 60).addBox(-0.8F, -0.8F, 0F, 1.6F, 1.6F, 4F), PartPose.offsetAndRotation(0F, 0F, 5.1F, 0.65F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaTiger entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * .8F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * .6F + .08F;
        boolean airborne = !entity.onGround() && !entity.isInWater();
        if (airborne) {
            // The pounce: stretched out, forepaws reaching, hind legs thrown back, the tail streaming
            body.xRot = -.18F;
            head.xRot -= .15F;
            legs[0].xRot = legs[1].xRot = -1.15F;
            legs[2].xRot = legs[3].xRot = .95F;
            tail[0].xRot = -.35F;
            tail[1].xRot = .1F;
            tail[2].xRot = .15F;
        } else {
            // A heavy, rolling walk: the legs of a side move nearly together, the shoulders and the head sway
            float speed = .9F;
            float a = Mth.cos(limbSwing * speed) * 1.0F * limbSwingAmount;
            float b = Mth.cos(limbSwing * speed + Mth.PI) * 1.0F * limbSwingAmount;
            legs[0].xRot = a;
            legs[1].xRot = b;
            legs[2].xRot = Mth.cos(limbSwing * speed + .6F) * limbSwingAmount;
            legs[3].xRot = Mth.cos(limbSwing * speed + .6F + Mth.PI) * limbSwingAmount;
            float sway = Mth.sin(limbSwing * speed) * .06F * limbSwingAmount;
            body.zRot = sway;
            head.zRot = -sway * .8F;
            float bob = Mth.sin(limbSwing * speed * 2) * .35F * limbSwingAmount;
            body.y += bob;
            head.y += bob;
            // The tail: a slow sway, its tip flicking
            tail[0].yRot = Mth.sin(ageInTicks * .07F) * .25F;
            tail[1].yRot = Mth.sin(ageInTicks * .07F - .7F) * .2F;
            tail[2].xRot += Mth.sin(ageInTicks * .21F) * .2F;
            tail[2].yRot = Mth.sin(ageInTicks * .11F - 1.3F) * .35F;
            // Breathing
            body.yScale = 1 + Mth.sin(ageInTicks * .1F) * .015F;
        }
        // A swipe of the right forepaw when it strikes
        if (attackTime > 0) {
            float s = Mth.sin(attackTime * Mth.PI);
            legs[0].xRot = -1.6F * s;
            legs[0].zRot = -.25F * s;
            head.xRot += .25F * s;
        }
    }
}
