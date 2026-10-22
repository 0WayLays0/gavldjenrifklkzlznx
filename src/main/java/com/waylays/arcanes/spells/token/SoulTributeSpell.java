package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenMote;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

/**
 * Soul Tribute: the caster pulls a soul token out of each enemy around (it hurts them); the tokens fly to the caster
 * and heal them as they arrive.
 */
@AutoSpellConfig
public class SoulTributeSpell extends TokenSpell {
    public static final float RADIUS = 10;
    public static final int MAX_VICTIMS = 8;

    public SoulTributeSpell() {
        super("soul_tribute", SpellRarity.EPIC, 5, 25, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .7f;
    }

    public float heal(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .35f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.heal_per_token", Utils.stringTruncation(heal(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            float damage = damage(spellLevel, entity);
            float heal = heal(spellLevel, entity);
            List<LivingEntity> victims = SpellUtil.enemiesAround(level, TokenFx.chest(entity), RADIUS, entity, entity::hasLineOfSight);
            victims.sort(Comparator.comparingDouble(e -> e.distanceToSqr(entity)));
            for (LivingEntity e : victims.subList(0, Math.min(MAX_VICTIMS, victims.size()))) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(this, entity, entity));
                TokenFx.burst(level, TokenFx.chest(e), 8, .4);
                TokenMote.send(serverLevel, TokenFx.chest(e), entity, heal);
            }
            // Whatever the sound's kind (a holder or not), playSound takes it
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 1.2f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
