package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Orbital Strike: a ring of light locks onto the nearest enemy's ground (or the spot aimed at), a mark from far above;
 * then a pillar of white fire as wide as the ring falls from the sky onto it, and a shock races out round it.
 */
@AutoSpellConfig
public class OrbitalStrikeSpell extends BossSpell {
    public static final float RADIUS = 3.5f, SHOCK = 7.5f;
    public static final double RANGE = 30;
    /** Ticks longer than other marks: it is a blow worth dodging. */
    public static final int EXTRA = 10;

    public OrbitalStrikeSpell() {
        super("orbital_strike", SpellRarity.EPIC, 3, 40, BossAnimations.BEAM);
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 26;
        this.spellPowerPerLevel = 6;
        this.castTime = 0;
        this.baseManaCost = 160;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            List<LivingEntity> near = foes(level, entity, entity.position(), RANGE, 1);
            Vec3 center = near.isEmpty() ? aimedGround(level, entity, RANGE) : SpellUtil.groundAt(level, near.get(0).position(), 4, 1);
            float damage = getDamage(spellLevel, entity);
            BossTelegraph strike = BossTelegraph.circle(level, entity, this, spellLevel, center, RADIUS).style(BossTelegraph.Style.STAR).delayed(EXTRA).damage(damage)
                    .part("strike").spawn();
            BossTelegraph.ring(level, entity, this, spellLevel, center, RADIUS, SHOCK).style(BossTelegraph.Style.STAR).delayed(EXTRA + 6).damage(damage * .35f)
                    .part("shock").spawn();
            int fire = strike.getFireTick();
            BossProp.scene(level, BossProp.Kind.ORBITAL, BossTelegraph.Style.STAR, RADIUS, now(level), fire + 26, center, center.add(0, 40, 0), center, fire - now(level), 0);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        boolean strike = "strike".equals(t.getPart());
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                if (strike) {
                    target.igniteForSeconds(5);
                    target.setDeltaMovement(target.getDeltaMovement().add(0, .7, 0));
                    target.hurtMarked = true;
                } else {
                    knockAway(target, c, .9, .4);
                }
            }
        }
        BossFx.shockwave(level, c, t.getSize() * 2.1f, t.getStyle().zone);
        if (strike) {
            SpellUtil.particles(level, ParticleTypes.FLASH, c.x, c.y + 1, c.z, 2, 0, 0, 0, 0);
            SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 60, RADIUS * .4, 2, RADIUS * .4, .2);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3f, .7f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, .5f);
            SpellUtil.cameraShake(level, c, 20, 24);
        }
    }
}
