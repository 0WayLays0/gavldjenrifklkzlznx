package com.waylays.cinematic.client;

import com.waylays.cinematic.CinematicMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The actor's model: the player's, with each arm cut at the elbow and each leg at the knee (the lower half is a child
 * that can bend). The skin's layout is unchanged: each half reads its half of the limb's texture.
 */
public final class ActorLayers {
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(new ResourceLocation(CinematicMod.MOD_ID, "actor"), "main");
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(new ResourceLocation(CinematicMod.MOD_ID, "actor_slim"), "main");

    private ActorLayers() {}

    public static LayerDefinition create(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE, over = new CubeDeformation(0.25f);
        int aw = slim ? 3 : 4;
        float ay = slim ? 2.5f : 2f;
        limb(root, "right_arm", "right_forearm", 40, 16, slim ? -2 : -3, -2, aw, 6, 4, none, -5f, ay);
        limb(root, "right_sleeve", "right_sleeve_lower", 40, 32, slim ? -2 : -3, -2, aw, 6, 4, over, -5f, ay);
        limb(root, "left_arm", "left_forearm", 32, 48, -1, -2, aw, 6, 4, none, 5f, ay);
        limb(root, "left_sleeve", "left_sleeve_lower", 48, 48, -1, -2, aw, 6, 4, over, 5f, ay);
        limb(root, "right_leg", "right_shin", 0, 16, -2, 0, 4, 6, 6, none, -1.9f, 12f);
        limb(root, "right_pants", "right_pants_lower", 0, 32, -2, 0, 4, 6, 6, over, -1.9f, 12f);
        limb(root, "left_leg", "left_shin", 16, 48, -2, 0, 4, 6, 6, none, 1.9f, 12f);
        limb(root, "left_pants", "left_pants_lower", 0, 48, -2, 0, 4, 6, 6, over, 1.9f, 12f);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Replaces a 12-high limb by its upper half (top at {@code top}) and a lower half hanging from the joint.
     * {@code joint} is where the lower half starts, below the limb's pivot.
     */
    private static void limb(PartDefinition root, String name, String lower, int u, int v, float x0, float top, int w, int h, int joint,
                             CubeDeformation def, float px, float py) {
        PartDefinition upper = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x0, top, -2f, w, h, 4, def),
                PartPose.offset(px, py, 0f));
        // The side faces of a box start 4 pixels below its texture offset: the lower half reads 6 pixels further down
        upper.addOrReplaceChild(lower, CubeListBuilder.create().texOffs(u, v + 6).addBox(x0, 0f, -2f, w, 6, 4, def),
                PartPose.offset(0f, joint, 0f));
    }
}
