package com.waylays.arcanes.spells.echo;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Base of the Echo Magic spells: the school, the config, the caster's gesture. */
public abstract class EchoSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final CastType castType;
    @Nullable
    private final AnimationHolder animation;

    /** {@code animation} null: Iron's own for the cast type. */
    protected EchoSpell(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds, CastType castType, @Nullable AnimationHolder animation) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.ECHO_RESOURCE)
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

    /** The enemy the caster looks at within {@code range}, if any. */
    @Nullable
    public static LivingEntity aimed(Level level, LivingEntity caster, double range) {
        return SpellUtil.aimedEnemy(level, caster, range);
    }

    /** Where the caster's aim meets the ground: under the enemy they look at, else the ground they point at. */
    public static Vec3 aimedSpot(Level level, LivingEntity caster, double range) {
        return SpellUtil.aimedSpot(level, caster, range);
    }
}
