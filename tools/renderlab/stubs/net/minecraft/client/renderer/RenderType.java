package net.minecraft.client.renderer;

import net.minecraft.resources.ResourceLocation;

/**
 * Render lab stand-in for Minecraft's RenderType: only its name (how it blends) and its texture. {@code blend} is
 * "alpha" (see-through, two-sided unless culled), "add" (additive light), or "solid"/"cutout".
 */
public class RenderType {
    public final String name;
    public final ResourceLocation texture;
    public final String blend;
    public final boolean cull;

    private RenderType(String name, ResourceLocation texture, String blend, boolean cull) {
        this.name = name;
        this.texture = texture;
        this.blend = blend;
        this.cull = cull;
    }

    public static RenderType entityTranslucent(ResourceLocation tex) {
        return new RenderType("entity_translucent", tex, "alpha", false);
    }

    public static RenderType entityTranslucent(ResourceLocation tex, boolean outline) {
        return entityTranslucent(tex);
    }

    public static RenderType entityTranslucentCull(ResourceLocation tex) {
        return new RenderType("entity_translucent_cull", tex, "alpha", true);
    }

    public static RenderType itemEntityTranslucentCull(ResourceLocation tex) {
        return new RenderType("item_entity_translucent_cull", tex, "alpha", true);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation tex) {
        return new RenderType("entity_translucent_emissive", tex, "alpha", false);
    }

    public static RenderType eyes(ResourceLocation tex) {
        return new RenderType("eyes", tex, "add", true);
    }

    public static RenderType energySwirl(ResourceLocation tex, float u, float v) {
        return new RenderType("energy_swirl", tex, "add", false);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation tex) {
        return new RenderType("entity_cutout_no_cull", tex, "cutout", false);
    }

    public static RenderType entityCutout(ResourceLocation tex) {
        return new RenderType("entity_cutout", tex, "cutout", true);
    }

    public static RenderType entitySolid(ResourceLocation tex) {
        return new RenderType("entity_solid", tex, "solid", true);
    }

    public static RenderType lightning() {
        return new RenderType("lightning", null, "add", false);
    }
}
