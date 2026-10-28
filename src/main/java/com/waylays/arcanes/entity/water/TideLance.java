package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A Tide Lance: a spear of water, three streams twisting round its core, flung from the hand. It runs through up to
 * {@link #PIERCE} enemies (soaked and thrown back a little) and bursts in a crown of water where it ends.
 */
public class TideLance extends SpellProjectile {
    public static final int PIERCE = 3;
    public static final float SPEED = 1.9f;

    private final IntOpenHashSet struck = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public TideLance(EntityType<? extends TideLance> entityType, Level level) {
        super(entityType, level);
    }

    public static TideLance fling(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        TideLance lance = new TideLance(ModEntities.TIDE_LANCE.get(), level);
        lance.setOwner(caster);
        lance.spellId = spell.getSpellId();
        lance.setDamage(damage);
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(.9)).subtract(0, .25, 0);
        lance.setPos(from.x, from.y - lance.getBbHeight() * .5, from.z);
        lance.setDeltaMovement(look.scale(SPEED));
        lance.setYRot((float) (Mth.atan2(look.x, look.z) * Mth.RAD_TO_DEG));
        lance.setXRot((float) (Mth.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG));
        lance.yRotO = lance.getYRot();
        lance.xRotO = lance.getXRot();
        level.addFreshEntity(lance);
        WaterFx.sound(level, from, SoundEvents.DROWNED_SHOOT, 1.2f, .8f);
        WaterFx.sound(level, from, SoundEvents.BUCKET_EMPTY, .8f, 1.3f);
        return lance;
    }

    @Override
    protected double gravity() {
        return .01;
    }

    @Override
    protected int lifetime() {
        return 50;
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
    protected void clientTick() {
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.FALLING_WATER, getX(), getY() + getBbHeight() * .5, getZ(), 0, 0, 0);
        }
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
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 push = getDeltaMovement().normalize().scale(.45 * resist);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x, .15 * resist, push.z));
                e.hurtMarked = true;
                WaterFx.soak(e, 60);
            }
            WaterFx.splash(level, e.getBoundingBox().getCenter(), 12, .3);
            WaterFx.sound(level, e.position(), SoundEvents.PLAYER_SPLASH, 1, 1.2f);
            if (struck.size() < PIERCE) {
                return;
            }
        }
        Vec3 at = hit.getLocation();
        SplashCrown.at(level, at.subtract(0, hit.getType() == HitResult.Type.BLOCK ? 0 : .5, 0), 1.1f);
        WaterFx.splash(level, at, 20, .5);
        WaterFx.crash(level, at, 1);
        discard();
    }
}
