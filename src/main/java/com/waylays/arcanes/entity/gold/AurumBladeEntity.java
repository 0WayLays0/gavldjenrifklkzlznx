package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.AurumBladeSpell;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Aurum Blade: a giant spectral golden sword. The entity sits on the ground at the strike point; the renderer draws
 * the sword hovering above it, then plunging. Timeline (ticks): hover until {@link #STRIKE_TICK}, impact, stuck in the
 * ground, dissolves at {@link #END_TICK}.
 */
public class AurumBladeEntity extends Entity implements AntiMagicSusceptible {
    public static final int PLUNGE_START = 14;
    public static final int STRIKE_TICK = 18;
    public static final int END_TICK = 46;
    public static final float HOVER_HEIGHT = 7f;

    private float damage;
    @Nullable
    private UUID ownerUUID;

    public AurumBladeEntity(EntityType<? extends AurumBladeEntity> entityType, Level level) {
        super(entityType, level);
    }

    public AurumBladeEntity(Level level, LivingEntity owner, float damage) {
        this(ModEntities.AURUM_BLADE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        setYRot(owner.getYRot());
    }

    @Nullable
    private Entity getOwner() {
        return ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        if (tickCount == 1) {
            level().playSound(null, getX(), getY() + HOVER_HEIGHT, getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.5f, 1.6f);
        }
        if (tickCount == PLUNGE_START) {
            level().playSound(null, getX(), getY() + HOVER_HEIGHT, getZ(), SoundEvents.TRIDENT_THROW, SoundSource.NEUTRAL, 2f, .7f);
        }
        if (tickCount == STRIKE_TICK) {
            strike();
        }
        if (tickCount >= END_TICK) {
            SpellUtil.particles(level(), GoldFx.GOLD_DUST, getX(), getY() + 1.5, getZ(), 30, .3, 1.2, .3, .02);
            discard();
        }
    }

    private void strike() {
        Entity owner = getOwner();
        Vec3 at = position();
        LivingEntity closest = null;
        for (LivingEntity e : SpellUtil.enemiesAround(level(), at.add(0, 1, 0), AurumBladeSpell.RADIUS, owner, x -> true)) {
            if (closest == null || e.distanceToSqr(at) < closest.distanceToSqr(at)) {
                closest = e;
            }
        }
        for (LivingEntity e : SpellUtil.enemiesAround(level(), at.add(0, 1, 0), AurumBladeSpell.RADIUS, owner, x -> true)) {
            float amount = e == closest ? damage : damage * AurumBladeSpell.SPLASH_RATIO;
            if (DamageSources.applyDamage(e, amount, SpellUtil.damageSource(ModSpells.AURUM_BLADE.get(), this, owner))) {
                Vec3 push = e.position().subtract(at).multiply(1, 0, 1);
                push = push.lengthSqr() < 1.0E-4 ? Vec3.ZERO : push.normalize().scale(.6);
                e.push(push.x, .35, push.z);
                e.hurtMarked = true;
            }
        }
        GoldFx.shatterBurst(level(), at.x, at.y + .3, at.z, 1.8f);
        SpellUtil.particles(level(), GoldFx.DEEP_DUST, at.x, at.y + .1, at.z, 40, 1.6, .05, 1.6, .03);
        SpellUtil.cameraShake(level(), at, 10, 12);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 2f, .5f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, .8f, 1.3f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 2f, .6f);
    }

    private void clientParticles() {
        Level level = level();
        if (tickCount < PLUNGE_START) {
            // Gathering light where the sword forms
            double y = getY() + HOVER_HEIGHT + random.nextDouble() * 2;
            level.addParticle(GoldFx.PALE_DUST, getX() + (random.nextFloat() - .5f) * 2, y, getZ() + (random.nextFloat() - .5f) * 2, 0, -.05, 0);
            // Target marker on the ground
            double angle = random.nextDouble() * Math.PI * 2;
            level.addParticle(GoldFx.GOLD_DUST, getX() + Math.cos(angle) * 1.3, getY() + .1, getZ() + Math.sin(angle) * 1.3, 0, 0, 0);
        } else if (tickCount < STRIKE_TICK) {
            double y = getY() + HOVER_HEIGHT * (1 - (tickCount - PLUNGE_START) / (float) (STRIKE_TICK - PLUNGE_START));
            for (int i = 0; i < 4; i++) {
                level.addParticle(GoldFx.sparks(), getX() + (random.nextFloat() - .5f) * .4f, y + random.nextFloat() * 3, getZ() + (random.nextFloat() - .5f) * .4f, 0, .2, 0);
            }
        } else if (tickCount % 3 == 0) {
            level.addParticle(GoldFx.PALE_DUST, getX() + (random.nextFloat() - .5f) * .5f, getY() + random.nextFloat() * 3, getZ() + (random.nextFloat() - .5f) * .5f, 0, .03, 0);
        }
    }

    /** The sword towers above the anchor: keep rendering while any of it is on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3, 0, 3).expandTowards(0, HOVER_HEIGHT + 6, 0);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
        tag.putFloat("Damage", damage);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        damage = tag.getFloat("Damage");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }
}
