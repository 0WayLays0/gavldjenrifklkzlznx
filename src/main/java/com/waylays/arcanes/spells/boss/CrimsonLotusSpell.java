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
 * Crimson Lotus: a great lotus of crimson glass blooms on the spot aimed at: its heart bursts first, then its six
 * petals open outward, each slicing through a wedge of ground (each marked beforehand), and those they cut bleed.
 */
@AutoSpellConfig
public class CrimsonLotusSpell extends BossSpell {
    public static final float HEART = 2.6f, RADIUS = 7.5f;
    public static final int PETALS = 6;
    public static final double RANGE = 22;

    public CrimsonLotusSpell() {
        super("crimson_lotus", SpellRarity.RARE, 5, 18, BossAnimations.GROUND_SLAM);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 70;
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
        return Optional.of(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = aimedGround(level, entity, RANGE);
            float damage = getDamage(spellLevel, entity);
            BossTelegraph heart = BossTelegraph.circle(level, entity, this, spellLevel, center, HEART).style(BossTelegraph.Style.CRIMSON).damage(damage).part("heart").spawn();
            float yaw0 = facing(entity);
            for (int k = 0; k < PETALS; k++) {
                BossTelegraph.cone(level, entity, this, spellLevel, center, yaw0 + k * 360f / PETALS, RADIUS, 18).style(BossTelegraph.Style.CRIMSON).delayed(10)
                        .damage(damage * .8f).part("petal").spawn();
            }
            int fire = heart.getFireTick();
            BossProp.scene(level, BossProp.Kind.LOTUS, BossTelegraph.Style.CRIMSON, RADIUS, now(level), fire + 34, center, center, center, fire - now(level), yaw0);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        boolean heart = "heart".equals(t.getPart());
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                if (heart) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0, .7, 0));
                    target.hurtMarked = true;
                } else {
                    knockAway(target, c, .7, .35);
                }
            }
        }
        Vec3 at = heart ? c : c.add(t.forward().scale(RADIUS * .6));
        SpellUtil.particles(level, BossFx.dust(t.getStyle().zone, 1.6f), at.x, at.y + .5, at.z, heart ? 40 : 14, heart ? 1.2 : 1.6, .5, heart ? 1.2 : 1.6, .05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, heart ? 2.5f : 1.2f, heart ? .6f : 1.1f);
        if (heart) {
            BossFx.shockwave(level, c, HEART * 2.4f, t.getStyle().zone);
        }
    }
}
