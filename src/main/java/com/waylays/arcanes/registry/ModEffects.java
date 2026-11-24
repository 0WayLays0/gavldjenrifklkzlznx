package com.waylays.arcanes.registry;

import com.waylays.arcanes.effect.ResonanceEffect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.BlastLeapEffect;
import com.waylays.arcanes.effect.BleedingEffect;
import com.waylays.arcanes.effect.EntangledEffect;
import com.waylays.arcanes.effect.GildedArmorEffect;
import com.waylays.arcanes.effect.GlyphFrozenEffect;
import com.waylays.arcanes.effect.GlyphMarkEffect;
import com.waylays.arcanes.effect.LowGravityEffect;
import com.waylays.arcanes.effect.MidasCurseEffect;
import com.waylays.arcanes.effect.ParchmentAegisEffect;
import com.waylays.arcanes.effect.PetrifiedEffect;
import com.waylays.arcanes.effect.PinnedEffect;
import com.waylays.arcanes.effect.ReactiveShieldEffect;
import com.waylays.arcanes.effect.RuneEdgeEffect;
import com.waylays.arcanes.effect.RustedEffect;
import com.waylays.arcanes.effect.SilenceEffect;
import com.waylays.arcanes.effect.SunderedEffect;
import com.waylays.arcanes.effect.VolatileBrandEffect;
import com.waylays.arcanes.effect.WeightlessEffect;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.List;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ArcanesOublies.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> MIDAS_CURSE = EFFECTS.register("midas_curse", MidasCurseEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> GILDED_ARMOR = EFFECTS.register("gilded_armor", GildedArmorEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> VOLATILE_BRAND = EFFECTS.register("volatile_brand", VolatileBrandEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLAST_LEAP = EFFECTS.register("blast_leap", BlastLeapEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> REACTIVE_SHIELD = EFFECTS.register("reactive_shield", ReactiveShieldEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> SILENCE = EFFECTS.register("silence", SilenceEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PARCHMENT_AEGIS = EFFECTS.register("parchment_aegis", ParchmentAegisEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> SUNDERED = EFFECTS.register("sundered", SunderedEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PINNED = EFFECTS.register("pinned", PinnedEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PETRIFIED = EFFECTS.register("petrified", PetrifiedEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> RUNE_EDGE = EFFECTS.register("rune_edge", RuneEdgeEffect::new);
    /** Glyph Magic: an element's glyph inscribed on a creature, one effect per element (in {@link GlyphElement} order). */
    public static final List<DeferredHolder<MobEffect, MobEffect>> GLYPH_MARKS = Arrays.stream(GlyphElement.values())
            .map(element -> EFFECTS.register("glyph_" + element.id, () -> (MobEffect) new GlyphMarkEffect(element))).toList();
    public static final DeferredHolder<MobEffect, MobEffect> GLYPH_FROZEN = EFFECTS.register("glyph_frozen", GlyphFrozenEffect::new);
    /** Moss, Copper and Rust: rusted armour (protects less, wears away), and vines holding a creature in place. */
    public static final DeferredHolder<MobEffect, MobEffect> RUSTED = EFFECTS.register("rusted", RustedEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> ENTANGLED = EFFECTS.register("entangled", EntangledEffect::new);
    // Cosmos
    public static final DeferredHolder<MobEffect, MobEffect> WEIGHTLESS = EFFECTS.register("weightless", WeightlessEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> LOW_GRAVITY = EFFECTS.register("low_gravity", LowGravityEffect::new);
    // Stellar Fire: how hot its caster burns

    // Echo Magic: the sound still ringing in a creature, waiting to burst
    public static final DeferredHolder<MobEffect, MobEffect> RESONANCE = EFFECTS.register("resonance", ResonanceEffect::new);
    // The Gale: the air's weight bowing those under it
    public static final DeferredHolder<MobEffect, MobEffect> CRUSHING_GRAVITY = EFFECTS.register("crushing_gravity",
            com.waylays.arcanes.effect.CrushingGravityEffect::new);
    /** Walking on water (/marchesurleau, for the staff). */
    public static final DeferredHolder<MobEffect, MobEffect> WATER_WALKING = EFFECTS.register("water_walking",
            com.waylays.arcanes.effect.WaterWalkingEffect::new);

    private ModEffects() {}
}
