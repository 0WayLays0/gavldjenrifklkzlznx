package com.waylays.arcanes.spells.wind;

import com.waylays.arcanes.entity.wind.UpdraftGust;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Updraft: a column of wind bursts up under the caster and carries them high into the air, letting them drift down
 * slowly after; enemies close by are struck and thrown up and away.
 */
@AutoSpellConfig
public class UpdraftSpell extends WindSpell {
    public static final float RADIUS = 3.5f;

    public UpdraftSpell() {
        super("updraft", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    public static float lift(int spellLevel) {
        return 1 + .06f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float v = lift(spellLevel);
        return List.of(
                Component.translatable("ui.arcanes_oublies.lift_height", Utils.stringTruncation(v * v * 6.2f, 1)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x * .5, lift(spellLevel), v.z * .5);
        entity.hurtMarked = true;
        entity.fallDistance = 0;
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
            float damage = getPower(spellLevel, entity);
            for (LivingEntity e : SpellUtil.enemiesAround(level, entity.position(), RADIUS, entity, x -> true)) {
                e.invulnerableTime = 0;
                io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(e, damage, SpellUtil.damageSource(this, entity, entity));
                Vec3 out = e.position().subtract(entity.position()).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                WindFx.throwBy(e, out.scale(.9).add(0, .7, 0));
            }
            UpdraftGust.at(level, entity.position(), 7);
            WindFx.dust(level, entity.position(), 20, 1.5);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.5f, .9f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
