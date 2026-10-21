package com.waylays.arcanes.effect;

import com.waylays.arcanes.spells.glyph.GlyphElement;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * An element's glyph inscribed on a creature by Glyph Magic. It does nothing by itself: a glyph of another element
 * striking it sets off a reaction (see {@link com.waylays.arcanes.spells.glyph.GlyphReactions}).
 */
public class GlyphMarkEffect extends MobEffect {
    public final GlyphElement element;

    public GlyphMarkEffect(GlyphElement element) {
        super(MobEffectCategory.HARMFUL, element.color);
        this.element = element;
    }
}
