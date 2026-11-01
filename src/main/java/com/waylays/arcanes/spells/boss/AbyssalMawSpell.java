package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Abyssal Maw: the ground opens under the foe the caster aims at into a gullet of violet night, and two rows of
 * great fangs rise round it, while the maw slowly draws in whatever is near. When its warning runs out it bites:
 * everything still inside is crushed, dragged to the middle and left slowed and withering.
 */
@AutoSpellConfig
public class AbyssalMawSpell extends BossSpell {
    public static final double RANGE = 28;
    public static final float RADIUS = 6;
    /** How hard it draws in what stands near it, every half second of its warning. */
    public static final double PULL = .09;

    public AbyssalMawSpell() {
        super("abyssal_maw", SpellRarity.EPIC, 3, 35, BossAnimations.GROUND_SLAM);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 16;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 120;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bite_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.WARDEN_DIG);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 c = SpellUtil.aimedSpot(level, entity, RANGE);
            BossTelegraph t = BossTelegraph.circle(level, entity, this, spellLevel, c, RADIUS)
                    .style(BossTelegraph.Style.VOID).damage(getDamage(spellLevel, entity)).part("maw").spawn();
            BossProp.scene(level, BossProp.Kind.MAW, BossTelegraph.Style.VOID, RADIUS, now(level), t.getFireTick(), c, c, c, 0, 0);
            // It draws in what stands near it while it waits
            for (int tick = 10; tick < t.getFireTick() - now(level); tick += 10) {
                BossScheduler.after(server, tick, l -> {
                    if (!t.isRemoved()) {
                        for (LivingEntity e : SpellUtil.enemiesAround(l, c, RADIUS * 1.6, entity, x -> true)) {
                            Vec3 in = c.subtract(e.position()).multiply(1, 0, 1);
                            if (in.lengthSqr() > .25) {
                                e.setDeltaMovement(e.getDeltaMovement().add(in.normalize().scale(PULL)));
                                e.hurtMarked = true;
                            }
                        }
                    }
                });
            }
            level.playSound(null, c.x, c.y, c.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                Vec3 in = c.subtract(target.position()).multiply(1, 0, 1);
                target.setDeltaMovement(in.scale(.25).add(0, .2, 0));
                target.hurtMarked = true;
            }
        }
        SpellUtil.particles(level, ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 80, RADIUS * .4, .8, RADIUS * .4, .1);
        SpellUtil.particles(level, ParticleTypes.SQUID_INK, c.x, c.y + .5, c.z, 30, RADIUS * .3, .3, RADIUS * .3, .05);
        BossFx.shockwave(level, c, RADIUS * 2.4f, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 3f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, .6f);
        SpellUtil.cameraShake(level, c, 16, RADIUS * 3);
    }
}
