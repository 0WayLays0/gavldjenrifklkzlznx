package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.SilenceSeal;
import com.waylays.arcanes.registry.ModEffects;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Seal of Silence: a sealed rune is stamped on the target, which cannot cast spells for a while (see
 * {@link com.waylays.arcanes.effect.SilenceEffect}).
 */
@AutoSpellConfig
public class SealOfSilenceSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("seal_of_silence");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public SealOfSilenceSpell() {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel), 1)));
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
        return Optional.of(SoundEvents.EVOKER_PREPARE_WOLOLO);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 20, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                seal(level, entity, target, getDuration(spellLevel));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Silences {@code target} and puts the seal on it. */
    public static void seal(Level level, LivingEntity caster, LivingEntity target, int duration) {
        target.addEffect(new MobEffectInstance(ModEffects.SILENCE, duration, 0, false, true, true), caster);
        // A fresh seal replaces the old one
        for (SilenceSeal old : level.getEntitiesOfClass(SilenceSeal.class, target.getBoundingBox().inflate(2), s -> s.getTarget() == target)) {
            old.discard();
        }
        level.addFreshEntity(new SilenceSeal(level, caster, target));
        // A thread of glyphs from the caster's hand to the victim
        Vec3 from = caster.getEyePosition().add(caster.getLookAngle().scale(.8));
        Vec3 to = target.getEyePosition();
        Vec3 delta = to.subtract(from);
        int steps = (int) (delta.length() * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
            SpellUtil.particles(level, i % 2 == 0 ? BookFx.VIOLET_DUST : ParticleTypes.ENCHANT, p.x, p.y, p.z, 1, .05, .05, .05, 0);
        }
    }

    /** 3 s at level 1, +1 s per level. */
    public int getDuration(int spellLevel) {
        return 40 + spellLevel * 20;
    }
}
