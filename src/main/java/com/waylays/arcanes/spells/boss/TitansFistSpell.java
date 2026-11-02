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
 * Titan's Fist: a colossal gauntlet of iron and gold plunges out of the sky onto the spot aimed at, and the ground
 * quakes out from where it lands in two rings: the blow itself, then the quake, wider and weaker.
 */
@AutoSpellConfig
public class TitansFistSpell extends BossSpell {
    public static final float RADIUS = 4;
    public static final float[] QUAKE = {4, 8, 12};
    public static final double RANGE = 26;

    public TitansFistSpell() {
        super("titans_fist", SpellRarity.EPIC, 3, 40, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 35;
        this.baseSpellPower = 22;
        this.spellPowerPerLevel = 5;
        this.castTime = 0;
        this.baseManaCost = 150;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.quake_damage", Utils.stringTruncation(getDamage(spellLevel, caster) * .45f, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ANVIL_PLACE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = aimedGround(level, entity, RANGE);
            float damage = getDamage(spellLevel, entity);
            BossTelegraph fist = BossTelegraph.circle(level, entity, this, spellLevel, center, RADIUS).style(BossTelegraph.Style.EARTH).damage(damage).part("fist").spawn();
            for (int i = 0; i < 2; i++) {
                BossTelegraph.ring(level, entity, this, spellLevel, center, QUAKE[i], QUAKE[i + 1]).style(BossTelegraph.Style.EARTH).delayed(8 + 8 * i)
                        .damage(damage * (i == 0 ? .45f : .3f)).part("quake").spawn();
            }
            int fire = fist.getFireTick();
            BossProp.scene(level, BossProp.Kind.FIST, BossTelegraph.Style.EARTH, RADIUS, now(level), fire + 40, center, center.add(0, 26, 0), center, fire - now(level),
                    facing(entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        boolean fist = "fist".equals(t.getPart());
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                if (fist) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0, .9, 0));
                    target.hurtMarked = true;
                } else {
                    knockAway(target, c, .8, .5);
                }
            }
        }
        BossFx.shockwave(level, c, t.getSize() * 2.1f, t.getStyle().zone);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, c.x, c.y + .5, c.z, fist ? 6 : 3, t.getSize() * .4, .3, t.getSize() * .4, 0);
        SpellUtil.particles(level, BossFx.dust(0x9A7A5A, 2.2f), c.x, c.y + .4, c.z, 40, t.getSize() * .7, .3, t.getSize() * .7, .05);
        level.playSound(null, c.x, c.y, c.z, fist ? SoundEvents.ANVIL_LAND : SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, fist ? .4f : .6f);
        SpellUtil.cameraShake(level, c, fist ? 20 : 10, t.getSize() + 10);
    }
}
