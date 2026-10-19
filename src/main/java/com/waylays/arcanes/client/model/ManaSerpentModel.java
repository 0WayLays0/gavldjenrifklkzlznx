package com.waylays.arcanes.client.model;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaSerpent;
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
 * Serpent: a flat head with a flicking forked tongue and a body of {@link #SEGMENTS} chained segments, thinner toward
 * the tail. A wave runs down the chain so the body curves in an S as it slithers; it rears its head to strike.
 */
public class ManaSerpentModel extends HierarchicalModel<ManaSerpent> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanesOublies.id("mana_serpent"), "main");
    public static final int SEGMENTS = 9;
    private static final float SEGMENT_LENGTH = 3;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart tongue;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];

    public ManaSerpentModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.tongue = head.getChild("tongue");
        ModelPart part = root.getChild("segment0");
        segments[0] = part;
        for (int i = 1; i < SEGMENTS; i++) {
            part = part.getChild("segment" + i);
            segments[i] = part;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The model is centred on the entity: head forward (-Z), the body trailing behind
        float front = -9;
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.6F, -1.4F, -4F, 3.2F, 2.2F, 4)
                .texOffs(0, 7).addBox(-1.1F, -1F, -5.2F, 2.2F, 1.6F, 1.4F)
                .texOffs(60, 60).addBox(-1.75F, -1.5F, -3.4F, .6F, .6F, .9F)
                .texOffs(60, 60).addBox(1.15F, -1.5F, -3.4F, .6F, .6F, .9F), PartPose.offset(0, 22.9F, front));
        head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(16, 0).addBox(-.3F, 0, -2.4F, .6F, .1F, 2.4F), PartPose.offset(0, .2F, -5.1F));
        PartDefinition parent = root;
        for (int i = 0; i < SEGMENTS; i++) {
            float w = Mth.lerp(i / (float) (SEGMENTS - 1), 2.6F, .7F);
            float h = Mth.lerp(i / (float) (SEGMENTS - 1), 2F, .7F);
            PartPose pose = i == 0 ? PartPose.offset(0, 22.9F, front) : PartPose.offset(0, 0, SEGMENT_LENGTH);
            // Resting on the ground, each segment a little longer than the gap so the joints never open up
            parent = parent.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(0, 12 + i * 4)
                    .addBox(-w / 2, 1 - h, -.3F, w, h, SEGMENT_LENGTH + .6F), pose);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ManaSerpent entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // The wave runs down the body with time and, faster, with the ground covered (limbSwing only grows, so
        // the wave never jumps when the serpent speeds up or stops); it widens while it moves
        float width = .2f + .22f * Math.min(1, limbSwingAmount);
        float phase = ageInTicks * .26f + limbSwing * 1.5f;
        for (int i = 0; i < SEGMENTS; i++) {
            segments[i].yRot = Mth.sin(phase - i * .85f) * width * (i == 0 ? .6f : 1f);
        }
        // Head reared a little, swaying against the body; lunges when it strikes
        float strike = Mth.sin(attackTime * Mth.PI);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * .6f - segments[0].yRot * .5f;
        head.xRot = -.18f - strike * .5f + headPitch * Mth.DEG_TO_RAD * .3f;
        head.y = 22.9f - strike * 1.5f - .4f;
        head.z = -9 - strike * 2f;
        // Tongue flicks in and out every second or so
        tongue.visible = (ageInTicks % 22) < 6;
        tongue.zScale = 1 + Mth.sin(ageInTicks * 1.4f) * .3f;
    }
}
