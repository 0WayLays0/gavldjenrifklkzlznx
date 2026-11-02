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
 * Lances of Judgment: five long strips are marked side by side ahead of the caster, and giant lances of light fall out
 * of the sky onto them one after another, the middle one first, then outward.
 */
@AutoSpellConfig
public class LancesOfJudgmentSpell extends BossSpell {
    public static final int LINES = 5;
    public static final float LENGTH = 18, WIDTH = 1.8f, SPACING = 3;
    /** Ticks between one pair of lances and the next. */
    public static final int STEP = 5;

    public LancesOfJudgmentSpell() {
        super("lances_of_judgment", SpellRarity.RARE, 5, 20, BossAnimations.BLADE_CALL);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 70;
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
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float yaw = facing(entity);
            Vec3 fwd = direction(yaw), side = new Vec3(fwd.z, 0, -fwd.x);
            Vec3 base = feet(entity).add(fwd.scale(1.5));
            float damage = getDamage(spellLevel, entity);
            for (int k = 0; k < LINES; k++) {
                int off = k - LINES / 2;
                Vec3 start = SpellUtil.groundAt(level, base.add(side.scale(off * SPACING)), 4, 1);
                BossTelegraph t = BossTelegraph.line(level, entity, this, spellLevel, start, yaw, LENGTH, WIDTH).style(BossTelegraph.Style.HOLY)
                        .delayed(Math.abs(off) * STEP).damage(damage).part("lance").spawn();
                int fire = t.getFireTick();
                BossProp.scene(level, BossProp.Kind.LANCE, BossTelegraph.Style.HOLY, WIDTH, now(level), fire + 18, start, start.add(0, 16, 0), start.add(fwd.scale(LENGTH)),
                        fire - now(level), yaw);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 f = t.forward();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                target.setDeltaMovement(target.getDeltaMovement().add(0, .4, 0));
                target.hurtMarked = true;
            }
        }
        Vec3 c = t.center();
        SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y + .4, c.z, 30, f.x * LENGTH * .3 + .3, .3, f.z * LENGTH * .3 + .3, .05);
        SpellUtil.particles(level, BossFx.dust(t.getStyle().zone, 1.4f), c.x, c.y + .3, c.z, 20, f.x * LENGTH * .3 + .3, .2, f.z * LENGTH * .3 + .3, .02);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.6f, 1.4f);
        SpellUtil.cameraShake(level, c, 6, 10);
    }
}
