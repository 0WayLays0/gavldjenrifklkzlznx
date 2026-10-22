package com.waylays.arcanes.client.gold;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.GoldCoatPacket;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Gold drawn on any body's own model: polished gold from the feet up to the height the gold has reached, a soft edge
 * where it climbs (glowing like molten gold while it is still climbing), and a sheen of light sliding over it. Gilded
 * Armor wears it as a thinner, brighter shell over the whole body.
 */
public class GoldCoatLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public static final ResourceLocation SKIN = ArcanesOublies.id("textures/entity/gold/gold_skin.png");
    public static final ResourceLocation SHEEN = ArcanesOublies.id("textures/entity/gold/gold_sheen.png");
    /** The height over which the gold fades in at its edge, as a part of the body's. */
    private static final float EDGE = .08f;

    public GoldCoatLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        GoldCoatPacket state = ClientGoldCoat.get(entity);
        if (state == null || entity.isInvisible()) {
            return;
        }
        float height = ClientGoldCoat.height(state, entity, partialTick);
        boolean armor = state.armor();
        if (height <= .001f && !armor) {
            return;
        }
        // The body's feet in the space the model's vertices come out in (world axes, from the camera; the model's
        // own space is flipped and offset: its feet are 1.501 below its origin), and its height in the world
        Vector3f feet = poseStack.last().pose().transformPosition(0, 1.501f, 0, new Vector3f());
        float bodyHeight = Math.max(.1f, entity.getBbHeight());
        M model = getParentModel();
        if (height > .001f) {
            float reach = Math.min(1.1f, height) + (height >= 1 ? EDGE : 0);
            model.renderToBuffer(poseStack, new HeightMask(buffer.getBuffer(RenderType.entityTranslucent(SKIN)), feet.y, bodyHeight, reach, EDGE, false, false),
                    packedLight, OverlayTexture.NO_OVERLAY, 0xF2FFFFFF);
            float scroll = ageInTicks * .01f;
            model.renderToBuffer(poseStack, new HeightMask(buffer.getBuffer(RenderType.energySwirl(SHEEN, scroll, scroll * .6f)), feet.y, bodyHeight, reach, EDGE, false, true),
                    packedLight, OverlayTexture.NO_OVERLAY, 0xFF8A6A30);
            if (height < 1) {
                // The climbing edge glows like molten gold
                float pulse = .7f + .3f * Mth.sin(ageInTicks * .3f);
                model.renderToBuffer(poseStack, new HeightMask(buffer.getBuffer(RenderType.eyes(SKIN)), feet.y, bodyHeight, reach, EDGE * 1.6f, true, true),
                        packedLight, OverlayTexture.NO_OVERLAY, (int) (pulse * 255) << 24 | 0xFF9A30);
            }
        }
        if (armor) {
            // Liquid gold flows up from the feet, then hardens into a thin polished shell with light sliding over it
            float flow = ClientGoldCoat.armor(entity, partialTick);
            float reach = flow + (flow >= 1 ? EDGE : 0);
            float scroll = ageInTicks * .02f;
            model.renderToBuffer(poseStack, new HeightMask(buffer.getBuffer(RenderType.entityTranslucent(SKIN)), feet.y, bodyHeight, reach, EDGE, false, false),
                    packedLight, OverlayTexture.NO_OVERLAY, 0x80FFFFFF);
            model.renderToBuffer(poseStack, new HeightMask(buffer.getBuffer(RenderType.energySwirl(SHEEN, scroll, -scroll)), feet.y, bodyHeight, reach, EDGE, false, true),
                    packedLight, OverlayTexture.NO_OVERLAY, 0xFFB08A3C);
            if (flow < 1) {
                model.renderToBuffer(poseStack, new HeightMask(buffer.getBuffer(RenderType.eyes(SKIN)), feet.y, bodyHeight, reach, EDGE * 1.6f, true, true),
                        packedLight, OverlayTexture.NO_OVERLAY, 0xFFFF9A30);
            }
        }
    }

    /**
     * Passes a model's vertices through, fading each one's alpha by its height on the body: whole up to {@code reach}
     * of the body's height, fading out over {@code edge} above it; or ({@code band}) only a band of light at the edge.
     * Additive render types (energy swirl, eyes) ignore alpha: for them ({@code additive}) the colour is faded instead.
     */
    private static final class HeightMask implements VertexConsumer {
        private final VertexConsumer out;
        private final float feetY;
        private final float bodyHeight;
        private final float reach;
        private final float edge;
        private final boolean band;
        private final boolean additive;
        private float y;

        HeightMask(VertexConsumer out, float feetY, float bodyHeight, float reach, float edge, boolean band, boolean additive) {
            this.out = out;
            this.feetY = feetY;
            this.bodyHeight = bodyHeight;
            this.reach = reach;
            this.edge = edge;
            this.band = band;
            this.additive = additive;
        }

        private float mask() {
            float h = (y - feetY) / bodyHeight;
            if (band) {
                return Mth.clamp(1 - Math.abs(h - reach) / edge, 0, 1);
            }
            return Mth.clamp((reach - h) / edge, 0, 1);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.y = y;
            out.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            float mask = mask();
            if (additive) {
                out.setColor((int) (red * mask), (int) (green * mask), (int) (blue * mask), (int) (alpha * mask));
            } else {
                out.setColor(red, green, blue, (int) (alpha * mask));
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            out.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            out.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            out.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            out.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }
}
