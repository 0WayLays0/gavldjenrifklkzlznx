package com.waylays.arcanes.spells.wind;

import com.waylays.arcanes.entity.wind.GaleBurst;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Gale: a blast of wind from the hand: every enemy in a cone before the caster is struck and hurled back, shots in its
 * way are blown aside, and the caster's own flames are put out.
 */
@AutoSpellConfig
public class GaleSpell extends WindSpell {
    public static final float RANGE = 8;

    public GaleSpell() {
        super("gale", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    public static float push(int spellLevel) {
        return 1.3f + .08f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.knockback", Utils.stringTruncation(push(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 from = entity.getEyePosition().subtract(0, .3, 0);
        if (!level.isClientSide) {
            float damage = getPower(spellLevel, entity);
            for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(look.scale(RANGE * .5)), RANGE * .6, entity, x -> true)) {
                Vec3 to = e.position().add(0, e.getBbHeight() * .5, 0).subtract(from);
                double d = to.length();
                if (d > RANGE || d < 1.0E-3 || to.scale(1 / d).dot(look) < .72) {
                    continue;
                }
                e.invulnerableTime = 0;
                io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(e, damage, SpellUtil.damageSource(this, entity, entity));
                Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
                WindFx.throwBy(e, flat.scale(push(spellLevel) * (1 - .4 * d / RANGE)).add(0, .45, 0));
            }
            WindFx.deflect(level, new net.minecraft.world.phys.AABB(from, from).inflate(RANGE * .6).move(look.scale(RANGE * .5)), from, entity, 1.2);
            entity.clearFire();
            GaleBurst.at(level, from.add(look.scale(.6)), look, RANGE);
            level.playSound(null, from.x, from.y, from.z, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.4f, .8f);
            level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, .8f, 1.6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
