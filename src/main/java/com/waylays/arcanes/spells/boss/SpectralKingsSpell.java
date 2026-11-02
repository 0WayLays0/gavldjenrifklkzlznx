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
 * Spectral Kings: four phantom kings in black armour rise round the spot aimed at, at the four winds, each kneeling at
 * the end of a long strip through the middle; one after another they rise and charge through it, cutting down what
 * stands in their way.
 */
@AutoSpellConfig
public class SpectralKingsSpell extends BossSpell {
    public static final float LENGTH = 15, WIDTH = 2.2f;
    /** Ticks between one king's charge and the next. */
    public static final int STEP = 7;
    public static final double RANGE = 24;

    public SpectralKingsSpell() {
        super("spectral_kings", SpellRarity.EPIC, 3, 30, BossAnimations.PRISON);
        this.manaCostPerLevel = 25;
        this.baseSpellPower = 13;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(LENGTH, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ZOMBIE_VILLAGER_CONVERTED);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            List<LivingEntity> near = foes(level, entity, entity.position(), RANGE, 1);
            Vec3 center = near.isEmpty() ? aimedGround(level, entity, RANGE) : SpellUtil.groundAt(level, near.get(0).position(), 4, 1);
            float yaw0 = facing(entity) + 45;
            float damage = getDamage(spellLevel, entity);
            for (int k = 0; k < 4; k++) {
                float yaw = yaw0 + k * 90;
                Vec3 dir = direction(yaw);
                Vec3 start = SpellUtil.groundAt(level, center.subtract(dir.scale(LENGTH * .5)), 4, 1);
                BossTelegraph t = BossTelegraph.line(level, entity, this, spellLevel, start, yaw, LENGTH, WIDTH).style(BossTelegraph.Style.VOID).delayed(k * STEP)
                        .damage(damage).part("charge").spawn();
                int fire = t.getFireTick();
                BossProp.scene(level, BossProp.Kind.KING, BossTelegraph.Style.VOID, 1, now(level), fire + 16, start, start, start.add(dir.scale(LENGTH)), fire - now(level), yaw);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 f = t.forward();
        Vec3 side = new Vec3(f.z, 0, -f.x);
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0));
                Vec3 d = target.position().subtract(t.position());
                double s = Math.signum(d.dot(side));
                knockAway(target, target.position().subtract(side.scale(s == 0 ? 1 : s)), .8, .35);
            }
        }
        Vec3 c = t.center();
        SpellUtil.particles(level, ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + .8, c.z, 30, f.x * LENGTH * .3 + .3, .5, f.z * LENGTH * .3 + .3, .04);
        SpellUtil.particles(level, BossFx.dust(t.getStyle().zone, 1.6f), c.x, c.y + .6, c.z, 24, f.x * LENGTH * .3 + .3, .4, f.z * LENGTH * .3 + .3, .02);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 2f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, .5f);
    }
}
