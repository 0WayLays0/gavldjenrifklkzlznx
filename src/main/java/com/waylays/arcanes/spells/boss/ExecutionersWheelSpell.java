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
 * The Executioner's Wheel: a great wheel of black iron and burning blades forms in front of the caster, spinning faster
 * and faster, then rolls down a long strip ahead of them, cutting a burning furrow. The strip is marked in four parts
 * that each land as the wheel rolls over them: getting out of its way in time is all it takes.
 */
@AutoSpellConfig
public class ExecutionersWheelSpell extends BossSpell {
    public static final float LENGTH = 26;
    public static final float WIDTH = 4.5f;
    public static final int PARTS = 4;
    /** Ticks the wheel takes to roll over one part of the strip. */
    public static final int PART_TICKS = 5;
    public static final float WHEEL_RADIUS = 2.6f;

    public ExecutionersWheelSpell() {
        super("executioners_wheel", SpellRarity.EPIC, 3, 30, BossAnimations.BLADE_CALL);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 16;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 110;
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
        return Optional.of(SoundEvents.ANVIL_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float yaw = facing(entity);
            Vec3 fwd = direction(yaw);
            Vec3 start = SpellUtil.groundAt(level, entity.position().add(fwd.scale(1.5)), 3, 1);
            float damage = getDamage(spellLevel, entity);
            float part = LENGTH / PARTS;
            int fire = 0;
            for (int k = 0; k < PARTS; k++) {
                Vec3 from = SpellUtil.groundAt(level, start.add(fwd.scale(part * k)), 4, 1);
                BossTelegraph t = BossTelegraph.line(level, entity, this, spellLevel, from, yaw, part, WIDTH)
                        .style(BossTelegraph.Style.CRIMSON).delayed(k * PART_TICKS).damage(damage).part("wheel").spawn();
                if (k == 0) {
                    fire = t.getFireTick();
                }
            }
            // It reaches the middle of each part as that part lands
            int rolling = PARTS * PART_TICKS;
            int rollStart = fire - PART_TICKS / 2;
            BossProp.scene(level, BossProp.Kind.WHEEL, BossTelegraph.Style.CRIMSON, WHEEL_RADIUS, now(level), rollStart + rolling, start, start,
                    start.add(fwd.scale(LENGTH)), rolling, yaw);
            level.playSound(null, start.x, start.y, start.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 f = t.forward();
        Vec3 side = new Vec3(f.z, 0, -f.x);
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.igniteForSeconds(4);
                // Flung aside, off the wheel's way
                Vec3 d = target.position().subtract(t.position());
                Vec3 away = side.scale(Math.signum(d.dot(side)) == 0 ? 1 : Math.signum(d.dot(side)));
                knockAway(target, target.position().subtract(away), .9, .45);
            }
        }
        Vec3 c = t.center();
        SpellUtil.particles(level, ParticleTypes.FLAME, c.x, c.y + .4, c.z, 30, f.x * 2 + .4, .3, f.z * 2 + .4, .06);
        SpellUtil.particles(level, ParticleTypes.LAVA, c.x, c.y + .3, c.z, 8, f.x * 2, .2, f.z * 2, 0);
        SpellUtil.particles(level, BossFx.dust(t.getStyle().zone, 2f), c.x, c.y + .5, c.z, 24, f.x * 2.5 + .5, .4, f.z * 2.5 + .5, .05);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.5f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.6f, .6f);
        SpellUtil.cameraShake(level, c, 8, 10);
    }
}
