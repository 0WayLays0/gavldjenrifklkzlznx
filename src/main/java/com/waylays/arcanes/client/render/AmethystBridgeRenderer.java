package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.ritual.AmethystBridge;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Amethyst Bridge's light (its blocks are real): a bright knot of light where it is growing, motes running out
 * along it to there, a shimmer over the deck while it stands, a scatter of light where it shatters.
 */
public class AmethystBridgeRenderer extends EntityRenderer<AmethystBridge> {
    private static final int COLOR = 0xB27AFF;

    public AmethystBridgeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AmethystBridge bridge, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, bridge, partialTicks);
        Vec3 origin = bridge.position();
        float age = bridge.tickCount + partialTicks;
        int n = bridge.length();
        int laid = bridge.laid(), gone = bridge.shattered();
        VertexConsumer glow = RitualMesh.light(buffer, RitualMesh.GLOW);
        if (laid < n && gone == 0) {
            Vector3f front = at(bridge, origin, laid + partialTicks, 1.4f);
            RitualMesh.billboard(glow, poseStack, view, front, 3.2f, 0, -1, WaterMesh.glow(COLOR, .9f), packedLight);
            RitualMesh.billboard(glow, poseStack, view, front, 1.2f, 0, -1, WaterMesh.glow(0xFFFFFF, .9f), packedLight);
        }
        if (gone > 0 && gone < n) {
            Vector3f edge = at(bridge, origin, n - gone, 1);
            RitualMesh.billboard(glow, poseStack, view, edge, 2.4f, 0, -1, WaterMesh.glow(COLOR, .7f), packedLight);
        }
        VertexConsumer motes = RitualMesh.light(buffer, RitualMesh.MOTE);
        int standing = Math.max(0, Math.min(laid, n - gone));
        for (int k = 0; k < 24 && standing > 1; k++) {
            float t = (age * (laid < n ? .03f : .008f) + WaterMesh.hash(k * 7 + bridge.getId())) % 1;
            float side = (WaterMesh.hash(k * 13 + bridge.getId()) - .5f) * 2.6f;
            Vector3f m = at(bridge, origin, t * standing, 1.2f + .6f * Mth.sin(age * .1f + k));
            Vec3 across = new Vec3(-bridge.along().z, 0, bridge.along().x).scale(side);
            m.add((float) across.x, 0, (float) across.z);
            RitualMesh.billboard(motes, poseStack, view, m, .35f, 0, -1, WaterMesh.glow(k % 3 == 0 ? 0xFFFFFF : COLOR, .8f * Mth.sin(t * Mth.PI)), packedLight);
        }
        super.render(bridge, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Slice {@code i}'s middle, lifted {@code lift}, from the entity's place. */
    private static Vector3f at(AmethystBridge bridge, Vec3 origin, float i, float lift) {
        Vec3 s = bridge.slice(i).subtract(origin);
        return new Vector3f((float) s.x, (float) s.y + lift, (float) s.z);
    }

    @Override
    public boolean shouldRender(AmethystBridge bridge, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(AmethystBridge bridge) {
        return RitualMesh.GLOW;
    }
}
