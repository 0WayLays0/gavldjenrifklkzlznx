package com.waylays.arcanes.client.model;

import com.waylays.arcanes.entity.errethys.ManaKangaroo;
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
 * Kangaroo, as tall as a player: a deep body leaning forward with a pouch, a head with a long muzzle and tall ears,
 * two short arms held up in a boxer's guard with big round fists, powerful hind legs on long feet, and a thick
 * two-part tail resting on the ground behind. It bounds on its hind legs (both together, the body lifting and the
 * tail swinging), punches straight with its right, and in a fury throws left-right combinations.
 */
public class ManaKangarooModel extends HierarchicalModel<ManaKangaroo> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_kangaroo"), "main");
    private static final float BODY_Y = 14F;
    private static final float LEAN = .3F;
    private static final float GUARD = -1F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    private final ModelPart[] tail;

    public ManaKangarooModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.armLeft = body.getChild("arm_left");
        this.armRight = body.getChild("arm_right");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
        ModelPart t0 = root.getChild("tail0");
        this.tail = new ModelPart[]{t0, t0.getChild("tail1")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -9F, -3F, 7, 10, 6.5F)
                .texOffs(28, 0).addBox(-2.6F, -5.5F, -3.7F, 5.2F, 4.8F, 1), PartPose.offsetAndRotation(0, BODY_Y, 1.5F, LEAN, 0, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-2.5F, -4.5F, -3F, 5, 4.5F, 5.5F)
                .texOffs(22, 18).addBox(-1.6F, -3F, -6.3F, 3.2F, 2.6F, 3.5F)
                .texOffs(36, 18).addBox(-.7F, -3.1F, -6.6F, 1.4F, 1, .4F)
                .texOffs(0, 30).addBox(-2.5F, -9.6F, -.4F, 1.5F, 5.3F, 1.2F)
                .texOffs(0, 30).addBox(1F, -9.6F, -.4F, 1.5F, 5.3F, 1.2F)
                .texOffs(60, 60).addBox(-2.3F, -3.6F, -3.15F, 1, .8F, .3F)
                .texOffs(60, 60).addBox(1.3F, -3.6F, -3.15F, 1, .8F, .3F), PartPose.offsetAndRotation(0, -9F, -1F, -LEAN, 0, 0));
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition arm = body.addOrReplaceChild(side > 0 ? "arm_left" : "arm_right", CubeListBuilder.create()
                    .texOffs(8, 30).addBox(-.8F, 0, -.8F, 1.6F, 4.5F, 1.6F), PartPose.offsetAndRotation(side * 2.9F, -7F, -2.2F, GUARD, 0, 0));
            arm.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(16, 30).addBox(-1.3F, -.3F, -1.3F, 2.6F, 2.6F, 2.6F), PartPose.offset(0, 4.5F, 0));
        }
        for (int side = -1; side <= 1; side += 2) {
            root.addOrReplaceChild(side > 0 ? "leg_left" : "leg_right", CubeListBuilder.create()
                    .texOffs(0, 40).addBox(-1.8F, 0, -3F, 3.6F, 6.5F, 6)
                    .texOffs(20, 40).addBox(-1.1F, 5.5F, .5F, 2.2F, 3, 2)
                    .texOffs(30, 40).addBox(-1.3F, 8F, -6F, 2.6F, 2, 8.5F), PartPose.offset(side * 3F, BODY_Y, 2F));
        }
        PartDefinition t0 = root.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(44, 0).addBox(-1.6F, -1.6F, 0, 3.2F, 3.2F, 8), PartPose.offsetAndRotation(0, 14.5F, 4.5F, -.6F, 0, 0));
        t0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(44, 12).addBox(-1.1F, -1.1F, 0, 2.2F, 2.2F, 7), PartPose.offsetAndRotation(0, 0, 7.6F, -.25F, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaKangaroo entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTicks = ageInTicks - entity.tickCount;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = -LEAN + headPitch * Mth.DEG_TO_RAD;
        // Bounding: both hind legs together, the whole body lifting off the ground, the tail swinging as a counterweight
        float hop = ManaKangaroo.hop(limbSwing) * Math.min(1, limbSwingAmount * 1.5F);
        float kick = Mth.cos(limbSwing * .6F * 2) * limbSwingAmount;
        float lift = -hop * 3.5F;
        body.y = BODY_Y + lift;
        body.xRot = LEAN + hop * .2F;
        legLeft.y = legRight.y = BODY_Y + lift;
        legLeft.xRot = legRight.xRot = kick * .55F;
        tail[0].y = 14.5F + lift;
        tail[0].xRot = -.6F + hop * .35F;
        tail[1].xRot = -.25F + hop * .2F;
        // At rest it breathes, its guard up
        float breath = Mth.sin(ageInTicks * .08F) * .04F;
        armLeft.xRot = GUARD + breath;
        armRight.xRot = GUARD - breath;
        armLeft.zRot = -.15F;
        armRight.zRot = .15F;
        float punch = entity.punchProgress(partialTicks);
        if (punch > 0) {
            // A straight right: the arm shoots forward and snaps back, the body leaning into it
            float reach = Mth.sin(punch * Mth.PI);
            armRight.xRot = GUARD - reach * 1F;
            armRight.zRot = .15F - reach * .25F;
            body.xRot += reach * .15F;
        }
        float fury = entity.furyTime(partialTicks);
        if (fury >= 0) {
            // A fury of punches: left, right, left... each arm firing in turn
            float beat = Mth.sin(fury * 1.4F);
            armLeft.xRot = GUARD - Math.max(0, beat) * 1F;
            armRight.xRot = GUARD - Math.max(0, -beat) * 1F;
            body.yRot = beat * .15F;
            body.xRot += .1F;
        } else {
            body.yRot = 0;
        }
    }
}
