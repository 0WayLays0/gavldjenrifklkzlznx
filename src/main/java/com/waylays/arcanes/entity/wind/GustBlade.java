package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** A Gust Blade: a crescent of cutting wind flung flat, cutting through up to {@link #PIERCE} enemies, thrusting each back. */
public class GustBlade extends SpellProjectile {
    public static final int PIERCE = 3;
    public static final float SPEED = 2.1f;

    private final IntOpenHashSet struck = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public GustBlade(EntityType<? extends GustBlade> entityType, Level level) {
        super(entityType, level);
    }

    public static GustBlade fling(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        GustBlade blade = new GustBlade(ModEntities.GUST_BLADE.get(), level);
        blade.setOwner(caster);
        blade.spellId = spell.getSpellId();
        blade.setDamage(damage);
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(.8)).subtract(0, .3, 0);
        blade.setPos(from.x, from.y - blade.getBbHeight() * .5, from.z);
        blade.shoot(look, SPEED);
        level.addFreshEntity(blade);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.1f, 1.3f);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .9f, 1.4f);
        return blade;
    }

    @Override
    protected int lifetime() {
        return 26;
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
                Vec3 push = getDeltaMovement().normalize().scale(.55);
                WindFx.throwBy(e, new Vec3(push.x, .2, push.z));
            }
            SpellUtil.particles(level, ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 1, 0, 0, 0, 0);
            if (struck.size() < PIERCE) {
                return;
            }
        }
        WindFx.gust(level, hit.getLocation(), 8, .4);
        discard();
    }

    @Override
    protected void onExpire() {
        if (level() instanceof ServerLevel level) {
            WindFx.gust(level, position(), 4, .3);
        }
    }
}
