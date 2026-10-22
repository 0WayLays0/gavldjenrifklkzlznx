package com.waylays.arcanes.client.model;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaPiranha;
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
 * Piranha: a deep, narrow, rounded body with a hump on the back and a full belly, a blunt head, the lower jaw jutting
 * out past the upper one, both lined with sharp teeth; a dorsal and an anal fin, two pectoral fins and a forked tail.
 * The jaw hangs a little open and snaps shut on each bite; the tail beats from side to side. Eyes and teeth use the
 * bright corner of the mana skin, so they shine.
 */
public class ManaPiranhaModel extends HierarchicalModel<ManaPiranha> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_piranha"), "main");

    private final ModelPart root;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart fin;
    private final ModelPart[] pectorals;

    public ManaPiranhaModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.jaw = body.getChild("jaw");
        this.tail = body.getChild("tail");
        this.fin = tail.getChild("fin");
        this.pectorals = new ModelPart[]{body.getChild("pectoral_left"), body.getChild("pectoral_right")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The body: deep and narrow, rounded by a hump and a belly; the head blunt and a little narrower
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -3.5F, -4F, 3, 7, 8)
                .texOffs(0, 16).addBox(-1.2F, -4.6F, -2.5F, 2.4F, 1.2F, 5)
                .texOffs(0, 23).addBox(-1.2F, 3.4F, -3F, 2.4F, 1.3F, 5.5F)
                .texOffs(22, 0).addBox(-1.3F, -2.8F, -6.6F, 2.6F, 5, 2.7F)
                .texOffs(22, 8).addBox(-1.1F, -2F, -7.4F, 2.2F, 2.6F, .9F)
                // Eyes, high on the head
                .texOffs(60, 60).addBox(-1.45F, -1.9F, -6.1F, .3F, .9F, .9F)
                .texOffs(60, 60).addBox(1.15F, -1.9F, -6.1F, .3F, .9F, .9F)
                // Upper teeth, along the front edge of the head
                .texOffs(60, 60).addBox(-.95F, .55F, -7.3F, .45F, .8F, .45F)
                .texOffs(60, 60).addBox(-.25F, .55F, -7.35F, .45F, .9F, .45F)
                .texOffs(60, 60).addBox(.5F, .55F, -7.3F, .45F, .8F, .45F)
                .texOffs(60, 60).addBox(-1.05F, .55F, -6.6F, .4F, .7F, .4F)
                .texOffs(60, 60).addBox(.65F, .55F, -6.6F, .4F, .7F, .4F), PartPose.offset(0, 19, 0));
        // The lower jaw, hinged under the eyes, jutting past the upper one, its teeth pointing up
        body.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-1.35F, 0, -3.6F, 2.7F, 1.5F, 3.8F)
                .texOffs(60, 60).addBox(-1.1F, -.85F, -3.45F, .45F, .9F, .45F)
                .texOffs(60, 60).addBox(-.4F, -.95F, -3.55F, .45F, 1F, .45F)
                .texOffs(60, 60).addBox(.35F, -.95F, -3.55F, .45F, 1F, .45F)
                .texOffs(60, 60).addBox(.8F, -.85F, -3.45F, .45F, .9F, .45F)
                .texOffs(60, 60).addBox(-1.2F, -.7F, -2.6F, .4F, .75F, .4F)
                .texOffs(60, 60).addBox(.8F, -.7F, -2.6F, .4F, .75F, .4F), PartPose.offset(0, 1.3F, -4.3F));
        // Fins: the dorsal swept back, the anal fin under the tail end, a pectoral on each side
        body.addOrReplaceChild("dorsal", CubeListBuilder.create().texOffs(36, 8).addBox(-.25F, -3F, -1.5F, .5F, 3, 4),
                PartPose.offsetAndRotation(0, -4.2F, 0, -.35F, 0, 0));
        body.addOrReplaceChild("anal", CubeListBuilder.create().texOffs(36, 16).addBox(-.25F, 0, -1, .5F, 2.2F, 3),
                PartPose.offsetAndRotation(0, 4F, 1.5F, .4F, 0, 0));
        body.addOrReplaceChild("pectoral_left", CubeListBuilder.create().texOffs(46, 16).addBox(0, 0, 0, .3F, 1.6F, 2.2F),
                PartPose.offsetAndRotation(1.5F, 1.2F, -3F, .5F, .3F, 0));
        body.addOrReplaceChild("pectoral_right", CubeListBuilder.create().texOffs(46, 16).addBox(-.3F, 0, 0, .3F, 1.6F, 2.2F),
                PartPose.offsetAndRotation(-1.5F, 1.2F, -3F, .5F, -.3F, 0));
        // The tail: a narrow stalk, then the forked fin
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 31).addBox(-.8F, -1.6F, 0, 1.6F, 3.2F, 2.4F),
                PartPose.offset(0, 0, 3.8F));
        tail.addOrReplaceChild("fin", CubeListBuilder.create()
                .texOffs(12, 31).addBox(-.25F, -1F, 0, .5F, 2, 1.2F)
                .texOffs(20, 31).addBox(-.25F, -4.2F, .6F, .5F, 3.4F, 2.6F)
                .texOffs(20, 38).addBox(-.25F, .8F, .6F, .5F, 3.4F, 2.6F), PartPose.offset(0, 0, 2.2F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaPiranha entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float swim = entity.isInWater() ? 1 : 1.8f;
        float beat = Mth.sin(ageInTicks * (.55f + .25f * entity.getFrenzy()) * swim);
        tail.yRot = beat * .45f * swim;
        fin.yRot = Mth.sin(ageInTicks * (.55f + .25f * entity.getFrenzy()) * swim - .9f) * .5f * swim;
        for (int i = 0; i < 2; i++) {
            pectorals[i].yRot = (i == 0 ? 1 : -1) * (.3f + .25f * Mth.sin(ageInTicks * .4f + i * Mth.PI));
        }
        // The jaw hangs a little open, gaping wider in a frenzy, and snaps on each bite
        float chomp = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
        jaw.xRot = .12f + .05f * entity.getFrenzy() + .06f * Mth.sin(ageInTicks * .3f) + chomp * .9f;
    }
}
