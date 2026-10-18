package com.waylays.arcanes.client.render;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.camel.Camel;

import javax.annotation.Nullable;

/**
 * Mana dromedary: Minecraft's camel, saddle and all, drawn in translucent sky-blue mana that glows from within.
 */
public class ManaCamelRenderer extends CamelRenderer {
    public ManaCamelRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.CAMEL);
        addLayer(ManaCreatureRenderer.glowLayer(this, ManaCreatureRenderer.GLOW));
    }

    @Nullable
    @Override
    protected RenderType getRenderType(Camel entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return ManaCreatureRenderer.translucent(ManaCreatureRenderer.SKIN, bodyVisible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(Camel entity) {
        return ManaCreatureRenderer.SKIN;
    }
}
