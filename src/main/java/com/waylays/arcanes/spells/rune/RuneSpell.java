package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Base of the Rune Magic spells: the school, the config and the caster's gesture. */
public abstract class RuneSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final CastType castType;
    @Nullable
    private final AnimationHolder animation;

    /** {@code animation} null: Iron's own for the cast type. */
    protected RuneSpell(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds, CastType castType, @Nullable AnimationHolder animation) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.RUNE_RESOURCE)
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

    /** The ground under {@code entity}. */
    protected static Vec3 feet(LivingEntity entity) {
        return SpellUtil.groundAt(entity.level(), entity.position(), 3, 1);
    }

    /** The horizontal direction of a yaw. */
    public static Vec3 direction(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    public float getPower(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
