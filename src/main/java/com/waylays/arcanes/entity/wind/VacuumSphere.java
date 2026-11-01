package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Vacuum Sphere: the air is drawn out of a place: for a moment every enemy round it is dragged in toward its heart,
 * then it bursts, striking them and leaving them winded (slowed and weakened).
 */
public class VacuumSphere extends SpellEffectEntity {
    public static final int PULL = 30;
    public static final int BURST = 12;
    public static final float RADIUS = 7;
    public static final float HEART = 1.4f;

    @Nullable
    private String spellId;

    public VacuumSphere(EntityType<? extends VacuumSphere> entityType, Level level) {
        super(entityType, level);
    }

    public static VacuumSphere open(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float damage) {
        VacuumSphere v = new VacuumSphere(ModEntities.VACUUM_SPHERE.get(), level);
        v.setOwner(caster);
        v.spellId = spell.getSpellId();
        v.damage = damage;
        v.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(v);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 2, .6f);
        return v;
    }

    public Vec3 heart() {
        return position().add(0, HEART, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > PULL + BURST) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        Vec3 c = heart();
        if (tickCount < PULL) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, c, RADIUS, caster, x -> true)) {
                Vec3 in = c.subtract(e.position().add(0, e.getBbHeight() * .5, 0));
                double d = in.length();
                if (d > .6) {
                    double k = WindFx.give(e);
                    e.setDeltaMovement(e.getDeltaMovement().scale(.6).add(in.scale(.07 * k * Math.min(1.6, 4 / d))));
                    e.hurtMarked = true;
                }
            }
            if (tickCount % 6 == 0) {
                WindFx.dust(level, position(), 4, RADIUS * .5);
            }
            return;
        }
        if (tickCount == PULL) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            for (LivingEntity e : SpellUtil.enemiesAround(level, c, RADIUS * .6, caster, x -> true)) {
                if (spell != null) {
                    e.invulnerableTime = 0;
                    DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                }
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                WindFx.throwBy(e, out.scale(.6).add(0, .35, 0));
            }
            WindFx.gust(level, c, 24, 1.5);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 2, .5f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, .8f, 1.6f);
            SpellUtil.cameraShake(level, c, 8, 16);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
