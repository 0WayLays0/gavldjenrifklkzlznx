package com.waylays.arcanes.client.render;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldenSentinel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Golden Sentinel: a humanoid in full golden armor with a golden sword and glowing eyes.
 */
public class GoldenSentinelRenderer extends HumanoidMobRenderer<GoldenSentinel, HumanoidModel<GoldenSentinel>> {
    private static final ResourceLocation TEXTURE = ArcanesOublies.id("textures/entity/golden_sentinel.png");
    private static final RenderType EYES = RenderType.eyes(ArcanesOublies.id("textures/entity/golden_sentinel_eyes.png"));

    public GoldenSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), .5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(GoldenSentinel entity) {
        return TEXTURE;
    }
}
