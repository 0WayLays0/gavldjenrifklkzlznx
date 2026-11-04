package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

/**
 * An Errethys creature: the model drawn in translucent sky-blue mana, with a second additive pass that makes its
 * veins of light glow from within. The mana skin has no fixed layout, so it fits any model; skins "with eyes" add
 * a bright corner that the addon's own models map their eyes to. A creature of another mana (the red salamander) brings its
 * own skin and glow.
 */
public class ManaCreatureRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    public static final ResourceLocation SKIN = ArcanesOublies.id("textures/entity/mana_skin.png");
    public static final ResourceLocation SKIN_EYES = ArcanesOublies.id("textures/entity/mana_skin_eyes.png");
    public static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/mana_glow.png");
    public static final ResourceLocation GLOW_EYES = ArcanesOublies.id("textures/entity/mana_glow_eyes.png");

    private final ResourceLocation skin;
    private final float scale;

    /** The red mana of the salamander, with eyes: its skin and its glow. */
    public static final ResourceLocation RED_SKIN_EYES = ArcanesOublies.id("textures/entity/mana_skin_red_eyes.png");
    public static final ResourceLocation RED_GLOW_EYES = ArcanesOublies.id("textures/entity/mana_glow_red_eyes.png");

    public ManaCreatureRenderer(EntityRendererProvider.Context context, M model, float shadow, float scale, boolean eyes) {
        this(context, model, shadow, scale, eyes ? SKIN_EYES : SKIN, eyes ? GLOW_EYES : GLOW);
    }

    /** A creature of another mana: its own {@code skin} and {@code glow}. */
    public ManaCreatureRenderer(EntityRendererProvider.Context context, M model, float shadow, float scale, ResourceLocation skin, ResourceLocation glow) {
        super(context, model, shadow * scale);
        this.skin = skin;
        this.scale = scale;
        addLayer(glowLayer(this, glow));
    }

    /** The additive pass that lights the creature from within. */
    static <E extends Mob, N extends EntityModel<E>> EyesLayer<E, N> glowLayer(RenderLayerParent<E, N> parent, ResourceLocation glow) {
        RenderType type = RenderType.eyes(glow);
        return new EyesLayer<>(parent) {
            @Override
            public RenderType renderType() {
                return type;
            }
        };
    }

    /** See-through body, whatever the model asks for. */
    @Nullable
    static RenderType translucent(ResourceLocation skin, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (bodyVisible || translucent) {
            return RenderType.entityTranslucent(skin);
        }
        return glowing ? RenderType.outline(skin) : null;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return translucent(skin, bodyVisible, translucent, glowing);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTicks) {
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return skin;
    }
}
