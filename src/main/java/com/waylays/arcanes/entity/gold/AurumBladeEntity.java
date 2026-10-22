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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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
 * Aurum Blade: a giant golden sword, forged in the sky from molten gold poured out of the sun vault, that plunges
 * into the ground at the strike point. The entity sits on the ground there; the renderer draws the sword. Timeline
 * (ticks): forged and hovering until {@link #PLUNGE_START}, impact at {@link #STRIKE_TICK} (the nearest enemy takes the
 * full blow and two coats of gold, the others less and one coat), the planted blade rings out an aftershock at
 * {@link #RING_TICK}, and melts into coins at {@link #END_TICK}.
 */
public class AurumBladeEntity extends Entity implements AntiMagicSusceptible {
    public static final int PLUNGE_START = 14;
    public static final int STRIKE_TICK = 18;
    public static final int RING_TICK = 30;
    public static final int END_TICK = 46;
    public static final float HOVER_HEIGHT = 7f;
    /** Share of the blow the aftershock deals, a little further out. */
    public static final float AFTERSHOCK = .35f;

    private static final EntityDataAccessor<Float> DATA_HOVER = SynchedEntityData.defineId(AurumBladeEntity.class, EntityDataSerializers.FLOAT);

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

    /** Height of the sword's point over the ground while it is forged (lower under a roof). */
    public float getHover() {
        return this.entityData.get(DATA_HOVER);
    }

    public void setHover(float hover) {
        this.entityData.set(DATA_HOVER, hover);
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
        float hover = getHover();
        if (tickCount == 1) {
            GoldSigil.vault(level(), position().add(0, hover + 7.6, 0), 2.4f, PLUNGE_START + 8);
            GoldFx.sigil(level(), position(), AurumBladeSpell.RADIUS, RING_TICK);
            level().playSound(null, getX(), getY() + hover, getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.5f, 1.6f);
            level().playSound(null, getX(), getY() + hover, getZ(), SoundEvents.LAVA_POP, SoundSource.NEUTRAL, 2f, .6f);
        }
        if (tickCount == 6 || tickCount == 10) {
            level().playSound(null, getX(), getY() + hover, getZ(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, .8f, 1.4f + tickCount * .02f);
        }
        if (tickCount == PLUNGE_START) {
            level().playSound(null, getX(), getY() + hover, getZ(), SoundEvents.TRIDENT_THROW, SoundSource.NEUTRAL, 2f, .7f);
        }
        if (tickCount == STRIKE_TICK) {
            strike();
        }
        if (tickCount == RING_TICK) {
            aftershock();
        }
        if (tickCount >= END_TICK) {
            melt();
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
                GoldFx.gild(e, e == closest ? 2 : 1);
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
        GoldFx.coins(level(), at.add(0, .6, 0), 8, .4f);
    }

    /** The planted blade rings like a bell: a second, weaker shock runs out along the ground. */
    private void aftershock() {
        Entity owner = getOwner();
        Vec3 at = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level(), at.add(0, 1, 0), AurumBladeSpell.RADIUS + 1.2, owner, x -> true)) {
            if (DamageSources.applyDamage(e, damage * AFTERSHOCK, SpellUtil.damageSource(ModSpells.AURUM_BLADE.get(), this, owner))) {
                GoldFx.gild(e, 1);
            }
        }
        SpellUtil.particles(level(), GoldFx.PALE_DUST, at.x, at.y + .2, at.z, 30, 2.2, .05, 2.2, .02);
        SpellUtil.cameraShake(level(), at, 5, 10);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 2.5f, .9f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 1.5f, 1.2f);
    }

    /** Its time up, the blade melts away into coins from point to pommel. */
    private void melt() {
        Vec3 at = position();
        GoldFx.coins(level(), at.add(0, 1, 0), 10, .3f);
        GoldFx.coins(level(), at.add(0, 2.6, 0), 10, .35f);
        GoldFx.shatterBurst(level(), at.x, at.y + 2, at.z, 1.2f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 2f, .9f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.NEUTRAL, 2f, .7f);
        discard();
    }

    private void clientParticles() {
        Level level = level();
        float hover = getHover();
        if (tickCount < PLUNGE_START) {
            // Molten gold poured from the vault, splashing off the sword as it takes shape
            double y = getY() + hover + 7.4;
            level.addParticle(GoldFx.MOLTEN_DUST, getX() + (random.nextFloat() - .5f) * .3f, y, getZ() + (random.nextFloat() - .5f) * .3f, 0, -.5, 0);
            double angle = random.nextDouble() * Math.PI * 2;
            level.addParticle(GoldFx.sparks(), getX() + Math.cos(angle) * .4, getY() + hover + 3 + random.nextDouble() * 3, getZ() + Math.sin(angle) * .4,
                    Math.cos(angle) * .15, .05, Math.sin(angle) * .15);
        } else if (tickCount < STRIKE_TICK) {
            double y = getY() + hover * (1 - (tickCount - PLUNGE_START) / (float) (STRIKE_TICK - PLUNGE_START));
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
        return getBoundingBox().inflate(4, 0, 4).expandTowards(0, getHover() + 9, 0);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HOVER, HOVER_HEIGHT);
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
        tag.putFloat("Hover", getHover());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        damage = tag.getFloat("Damage");
        if (tag.contains("Hover")) {
            setHover(tag.getFloat("Hover"));
        }
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }
}
