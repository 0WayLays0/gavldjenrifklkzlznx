package com.waylays.arcanes.client.model;

import com.waylays.arcanes.entity.errethys.ManaFalcon;
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
 * Falcon, big enough to ride: a broad body with a deep chest, a fierce head with a hooked beak and a heavy brow, two
 * long wings in two parts (the tips trailing a beat behind the beats) and a fanned tail; talons tucked up under it.
 * It beats its wings hard when climbing or cruising and holds them spread when gliding or diving; its body follows
 * the pitch of its flight.
 */
public class ManaFalconModel extends HierarchicalModel<ManaFalcon> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_falcon"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart tipLeft;
    private final ModelPart tipRight;
    private final ModelPart tail;

    public ManaFalconModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.wingLeft = body.getChild("wing_left");
        this.wingRight = body.getChild("wing_right");
        this.tipLeft = wingLeft.getChild("tip");
        this.tipRight = wingRight.getChild("tip");
        this.tail = body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -3F, -7F, 7, 6.5F, 15)
                .texOffs(0, 22).addBox(-3F, -2.2F, -9F, 6, 6, 3)
                .texOffs(18, 22).addBox(-2.2F, -4.4F, -10F, 4.4F, 4, 4)
                .texOffs(36, 22).addBox(-2.6F, 3.3F, -.5F, 1.6F, 2.2F, 1.6F)
                .texOffs(36, 22).addBox(1F, 3.3F, -.5F, 1.6F, 2.2F, 1.6F), PartPose.offset(0, 15F, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-2.5F, -3F, -4.5F, 5, 4.5F, 5)
                .texOffs(20, 32).addBox(-1F, -1.6F, -7F, 2, 2.1F, 2.6F)
                .texOffs(30, 32).addBox(-.75F, .3F, -7.4F, 1.5F, 1.4F, 1)
                .texOffs(36, 32).addBox(-2.8F, -3.3F, -4.7F, 5.6F, .9F, 2.6F)
                .texOffs(60, 60).addBox(-2.65F, -2.1F, -3.4F, .3F, 1, 1.1F)
                .texOffs(60, 60).addBox(2.35F, -2.1F, -3.4F, .3F, 1, 1.1F), PartPose.offset(0, -3.6F, -9.6F));
        for (int side = -1; side <= 1; side += 2) {
            CubeListBuilder inner = side > 0
                    ? CubeListBuilder.create().texOffs(0, 42).addBox(0, -.6F, -3F, 12, 1.2F, 9)
                    : CubeListBuilder.create().texOffs(0, 42).addBox(-12, -.6F, -3F, 12, 1.2F, 9);
            PartDefinition wing = body.addOrReplaceChild(side > 0 ? "wing_left" : "wing_right", inner, PartPose.offset(side * 3.3F, -2.2F, -3.5F));
            CubeListBuilder outer = side > 0
                    ? CubeListBuilder.create().texOffs(0, 52).addBox(0, -.4F, -2.5F, 11, .8F, 8).texOffs(38, 52).addBox(7.5F, -.3F, 5.5F, 3.5F, .6F, 3)
                    : CubeListBuilder.create().texOffs(0, 52).addBox(-11, -.4F, -2.5F, 11, .8F, 8).texOffs(38, 52).addBox(-11F, -.3F, 5.5F, 3.5F, .6F, 3);
            wing.addOrReplaceChild("tip", outer, PartPose.offset(side * 12F, 0, 0));
        }
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.2F, -.4F, 0, 6.4F, .8F, 7)
                .texOffs(40, 10).addBox(-2.2F, -.3F, 6.5F, 4.4F, .6F, 2), PartPose.offsetAndRotation(0, -1.5F, 7.5F, -.1F, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaFalcon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Its body follows the pitch of its flight
        body.xRot = headPitch * Mth.DEG_TO_RAD * .8F;
        double climb = entity.getY() - entity.yo;
        double speed = Math.sqrt((entity.getX() - entity.xo) * (entity.getX() - entity.xo) + (entity.getZ() - entity.zo) * (entity.getZ() - entity.zo));
        // Hard beats climbing or cruising slowly, wings held out and still gliding or diving fast
        float beat = climb > -.05 && speed < .6 ? 1 : climb > -.15 ? .35F : .08F;
        float wave = Mth.sin(ageInTicks * (.45F + beat * .2F));
        float wing = -.1F + wave * .75F * beat;
        float lag = Mth.sin(ageInTicks * (.45F + beat * .2F) - .9F) * .5F * beat;
        wingLeft.zRot = -wing;
        wingRight.zRot = wing;
        tipLeft.zRot = -lag;
        tipRight.zRot = lag;
        // Wings swept back a little at speed
        float sweep = (float) Math.min(.35, speed * .4);
        wingLeft.yRot = -sweep;
        wingRight.yRot = sweep;
        tail.xRot = -.1F - body.xRot * .3F + Mth.sin(ageInTicks * .1F) * .04F;
        head.xRot = -body.xRot * .6F;
        head.yRot = Mth.sin(ageInTicks * .05F) * .15F;
    }
}
