package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.glyph.GlyphSign;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.boss.BossScheduler;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Base of the Glyph Magic spells: the school, the config and the caster's gesture, and the writing itself. A glyph
 * spell first writes its element's glyph in the air before the caster, stroke after stroke, and goes off when the
 * glyph is finished.
 */
public abstract class GlyphSpell extends AbstractSpell {
    /** Ticks it takes to write a glyph in the air. */
    public static final int DRAW_TICKS = 6;
    /** How far before the caster's eyes the glyph is written, and its size. */
    public static final double WRITE_DISTANCE = 1.5;
    public static final float WRITE_SIZE = .75f;

    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final CastType castType;
    @Nullable
    private final AnimationHolder animation;

    /** {@code animation} null: Iron's own for the cast type. */
    protected GlyphSpell(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds, CastType castType, @Nullable AnimationHolder animation) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.GLYPH_RESOURCE)
                .setMaxLevel(maxLevel)
                .setCooldownSeconds(cooldownSeconds)
                .build();
        this.castType = castType;
        this.animation = animation;
    }

    @Override
    public CastType getCastType() {
        return castType;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return animation != null ? animation : super.getCastStartAnimation();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return animation != null ? AnimationHolder.pass() : super.getCastFinishAnimation();
    }

    public float getPower(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    /** Where the caster writes: before their eyes, a little low. */
    public static Vec3 writingSpot(LivingEntity caster) {
        return caster.getEyePosition().add(caster.getLookAngle().scale(WRITE_DISTANCE)).add(0, -.2, 0);
    }

    /**
     * Writes the glyph of {@code element} in the air before the caster, then runs {@code then} once it is written
     * (if the caster is still standing). Returns where it is written.
     */
    protected static Vec3 writeThen(Level level, LivingEntity caster, GlyphElement element, Consumer<ServerLevel> then) {
        Vec3 at = writingSpot(caster);
        GlyphSign.write(level, element, at, caster.getYRot(), caster.getXRot(), WRITE_SIZE, 0, DRAW_TICKS, false);
        GlyphFx.writing(level, element, at);
        if (level instanceof ServerLevel serverLevel) {
            BossScheduler.after(serverLevel, DRAW_TICKS, l -> {
                if (caster.isAlive() && !caster.isRemoved()) {
                    then.accept(l);
                }
            });
        }
        return at;
    }
}
