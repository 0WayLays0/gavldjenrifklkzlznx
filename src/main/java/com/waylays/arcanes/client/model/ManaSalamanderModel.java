package com.waylays.arcanes.client.model;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaSalamander;
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
 * Salamander: a long low body with a crest of little flame-fins down its back, a broad flat head with a round snout and
 * bulging eyes, four short legs splayed out to the sides with spread toes, and a long tail in three tapering parts
 * finned along the top. It walks with the side-to-side wriggle of a lizard, its tail sweeping behind.
 */
public class ManaSalamanderModel extends HierarchicalModel<ManaSalamander> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_salamander"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs;
    private final ModelPart[] tail;

    public ManaSalamanderModel(ModelPart root) {
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
        CubeListBuilder body = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.6F, -1.8F, -6F, 5.2F, 3.4F, 12)
                .texOffs(0, 16).addBox(-2.2F, 1.2F, -5F, 4.4F, .8F, 10);
        // The crest of flame-fins down its back, tallest over the shoulders
        for (int i = 0; i < 6; i++) {
            float h = 1.9F - Math.abs(i - 1.5F) * .25F;
            body.texOffs(48, 40).addBox(-.3F, -1.8F - h, -5F + i * 1.9F, .6F, h, 1.3F);
        }
        root.addOrReplaceChild("body", body, PartPose.offset(0, 20.4F, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-2.8F, -1.9F, -4.8F, 5.6F, 3.1F, 5)
                .texOffs(22, 28).addBox(-2.2F, -1.2F, -6.4F, 4.4F, 2.2F, 1.8F)
                .texOffs(0, 38).addBox(-2.2F, .9F, -6F, 4.4F, .6F, 5)
                // Little fins behind the eyes, and the crest begun on its crown
                .texOffs(48, 48).addBox(-3.3F, -2.6F, -1.6F, .5F, 1.6F, 1.4F)
                .texOffs(48, 48).addBox(2.8F, -2.6F, -1.6F, .5F, 1.6F, 1.4F)
                .texOffs(48, 40).addBox(-.3F, -2.9F, -2.2F, .6F, 1.1F, 2)
                // Bulging eyes
                .texOffs(60, 60).addBox(-3F, -2.5F, -4.2F, 1.4F, 1.4F, 1.4F)
                .texOffs(60, 60).addBox(1.6F, -2.5F, -4.2F, 1.4F, 1.4F, 1.4F), PartPose.offset(0, 20.2F, -6F));
        float[][] hips = {{2.4F, -4F}, {-2.4F, -4F}, {2.4F, 4F}, {-2.4F, 4F}};
        for (int i = 0; i < 4; i++) {
            float s = hips[i][0] > 0 ? 1 : -1;
            CubeListBuilder leg = CubeListBuilder.create()
                    .texOffs(40, 0).addBox(s > 0 ? 0 : -2.5F, -.8F, -.8F, 2.5F, 1.6F, 1.6F)
                    .texOffs(40, 6).addBox(s > 0 ? 1.7F : -3.1F, 0, -.75F, 1.4F, 3.3F, 1.5F)
                    .texOffs(40, 12).addBox(s > 0 ? 1.3F : -3.6F, 2.7F, -1.9F, 2.3F, .6F, 2.4F);
            root.addOrReplaceChild("leg" + i, leg, PartPose.offset(hips[i][0], 20.9F, hips[i][1]));
        }
        PartDefinition t0 = root.addOrReplaceChild("tail0", CubeListBuilder.create()
                .texOffs(0, 46).addBox(-1.8F, -1.5F, 0, 3.6F, 3F, 5.6F)
                .texOffs(48, 40).addBox(-.25F, -2.8F, 1F, .5F, 1.3F, 3.5F), PartPose.offset(0, 20.5F, 5.6F));
        PartDefinition t1 = t0.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(20, 46).addBox(-1.3F, -1.1F, 0, 2.6F, 2.2F, 5.2F)
                .texOffs(48, 40).addBox(-.2F, -2.1F, .8F, .4F, 1F, 3.4F), PartPose.offset(0, .2F, 5.3F));
        t1.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(36, 46).addBox(-.75F, -.7F, 0, 1.5F, 1.4F, 5.4F)
                .texOffs(48, 40).addBox(-.15F, -1.4F, .6F, .3F, .7F, 4.2F), PartPose.offset(0, .2F, 5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaSalamander entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float phase = limbSwing * 1.4F;
        float walk = Math.min(1, limbSwingAmount * 1.4F);
        // The lizard's wriggle: the body swings side to side, the head turned back against it
        float wriggle = Mth.sin(phase) * .2F * walk;
        body.yRot = wriggle;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD - wriggle * .8F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * .6F;
        // Splayed legs: diagonal pairs sweep forward and back along the ground, lifting as they come forward
        for (int i = 0; i < 4; i++) {
            float s = i % 2 == 0 ? 1 : -1;
            float pair = (i == 0 || i == 3) ? 1 : -1;
            float sweep = Mth.cos(phase) * pair;
            legs[i].yRot = -s * sweep * .65F * walk;
            legs[i].zRot = -s * Math.max(0, Mth.sin(phase) * pair) * .35F * walk;
        }
        // The tail sweeps in a wave running down it; at rest it curls slowly
        float idle = 1 - walk;
        for (int i = 0; i < tail.length; i++) {
            tail[i].yRot = -Mth.sin(phase - (i + 1) * .9F) * (.25F + .08F * i) * walk + Mth.sin(ageInTicks * .07F - i * .7F) * .14F * idle;
        }
        tail[0].xRot = -.08F;
    }
}
