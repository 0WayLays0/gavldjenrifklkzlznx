package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.VolatileBrandEffect;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Volatile Brand: burns a glowing mark into the target. Every hit it takes charges the mark; the fifth one sets it
 * off (see {@link VolatileBrandEffect}).
 */
@AutoSpellConfig
public class VolatileBrandSpell extends AbstractSpell {
    public static final float RADIUS = 3.5f;
    public static final float FALLBACK_DAMAGE = 12f;

    private final ResourceLocation spellId = ArcanesOublies.id("volatile_brand");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(15)
            .build();

    public VolatileBrandSpell() {
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.charges", VolatileBrandEffect.MAX_CHARGES),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel), 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.FIRECHARGE_USE);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 24, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                VolatileBrandEffect.brand(target, entity, spellLevel, getDuration(spellLevel), getDamage(spellLevel, entity));
                Vec3 from = entity.getEyePosition();
                Vec3 to = target.getBoundingBox().getCenter();
                Vec3 delta = to.subtract(from);
                int steps = (int) (delta.length() * 3);
                for (int i = 0; i <= steps; i++) {
                    Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
                    SpellUtil.particles(level, i % 2 == 0 ? ExplosionFx.FIRE_DUST : ExplosionFx.EMBER_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
                SpellUtil.particles(level, ExplosionFx.sparks(), to.x, to.y, to.z, 14, .3, .4, .3, .3);
                level.playSound(null, to.x, to.y, to.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, .7f);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 10 s at level 1, +1 s per level. */
    public int getDuration(int spellLevel) {
        return 200 + (spellLevel - 1) * 20;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.5f;
    }
}
