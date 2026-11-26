package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * The see-through render types the spells are drawn with. The game's own see-through entity type marks the depth of
 * every pixel it draws, faint or not: a veil, a ribbon, a bubble drawn first then hid all that lay behind it and was
 * drawn after (glows, the spell's other parts, a creature inside a bubble), which vanished. These draw the same, but
 * mark nothing: whatever is behind them still shows through. Their quads are sorted far to near before drawing.
 */
public abstract class SpellRenderTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> VEILS = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> FLOOR_VEILS = new HashMap<>();

    private SpellRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup,
                             Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    /** See-through, from both sides, hiding nothing behind it. */
    public static RenderType veil(ResourceLocation texture) {
        return VEILS.computeIfAbsent(texture, t -> make(ArcanesOublies.MODID + "_veil", t, NO_CULL));
    }

    /** See-through from the side its faces turn to only (things lying on the ground, a shell seen from outside), hiding nothing behind it. */
    public static RenderType veilCull(ResourceLocation texture) {
        return FLOOR_VEILS.computeIfAbsent(texture, t -> make(ArcanesOublies.MODID + "_veil_cull", t, CULL));
    }

    private static RenderType make(String name, ResourceLocation texture, CullStateShard cull) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(cull)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, state);
    }
}
