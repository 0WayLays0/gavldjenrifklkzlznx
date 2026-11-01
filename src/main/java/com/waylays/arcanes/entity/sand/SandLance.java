package com.waylays.arcanes.entity.sand;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.sand.SandFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A Sand Lance: a spear of sand packed hard and spinning, flung straight; it drills through up to {@link #PIERCE}
 * enemies, slowing each, and bursts in sand where it stops.
 */
public class SandLance extends SpellProjectile {
    public static final int PIERCE = 3;
    public static final float SPEED = 2.1f;

    private final IntOpenHashSet struck = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public SandLance(EntityType<? extends SandLance> entityType, Level level) {
        super(entityType, level);
    }

    public static SandLance fling(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        SandLance lance = new SandLance(ModEntities.SAND_LANCE.get(), level);
        lance.setOwner(caster);
        lance.spellId = spell.getSpellId();
        lance.setDamage(damage);
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(.8)).subtract(0, .25, 0);
        lance.setPos(from.x, from.y - lance.getBbHeight() * .5, from.z);
        lance.shoot(look, SPEED);
        level.addFreshEntity(lance);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, .7f);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.2f, .8f);
        return lance;
    }

    @Override
    protected int lifetime() {
        return 26;
    }

    @Override
    protected double gravity() {
        return 0;
    }

    @Override
    protected boolean piercesEntities() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !struck.contains(target.getId()) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity e) {
            struck.add(e.getId());
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, getOwner()));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
            }
            SandFx.sand(level, e.position().add(0, e.getBbHeight() * .5, 0), 12, .3);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SAND_HIT, SoundSource.PLAYERS, 1.4f, .8f);
            if (struck.size() < PIERCE) {
                return;
            }
        }
        SandFx.sand(level, hit.getLocation(), 24, .4);
        level.playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, .6f);
        discard();
    }

    @Override
    protected void onExpire() {
        if (level() instanceof ServerLevel level) {
            SandFx.sand(level, position(), 12, .3);
        }
    }
}
