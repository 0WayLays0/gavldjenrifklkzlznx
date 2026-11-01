package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import com.waylays.arcanes.spells.gold.GoldfallSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Goldfall: a giant ingot of gold, two blocks long. It hangs white-hot under the sun vault in the sky for a moment,
 * drops point first, spinning, and drives itself into the ground: everything around is crushed and thrown
 * back. It stays planted there, cooling, then melts away into a spray of coins.
 */
public class GoldfallIngot extends SpellProjectile {
    private static final EntityDataAccessor<Integer> DATA_WAIT = SynchedEntityData.defineId(GoldfallIngot.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PLANTED = SynchedEntityData.defineId(GoldfallIngot.class, EntityDataSerializers.INT);

    /** Length of the ingot, in blocks. */
    public static final float SIZE = 2;
    /** Ticks it stays planted before melting into coins. */
    public static final int PLANTED_TICKS = 50;
    public static final float DROP_SPEED = .4f;

    public GoldfallIngot(EntityType<? extends GoldfallIngot> entityType, Level level) {
        super(entityType, level);
    }

    public GoldfallIngot(Level level, LivingEntity caster) {
        this(ModEntities.GOLDFALL_INGOT.get(), level);
        setOwner(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WAIT, 0);
        builder.define(DATA_PLANTED, -1);
    }

    /** Ticks it hangs in the vault before it drops (0: it falls at once, with the speed it is given). */
    public void setWait(int ticks) {
        this.entityData.set(DATA_WAIT, ticks);
    }

    public int getWait() {
        return this.entityData.get(DATA_WAIT);
    }

    public boolean isPlanted() {
        return this.entityData.get(DATA_PLANTED) >= 0;
    }

    /** The tick it struck the ground. */
    public int plantedAt() {
        return this.entityData.get(DATA_PLANTED);
    }

    public boolean isFalling() {
        return tickCount >= getWait() && !isPlanted();
    }

    @Override
    protected double gravity() {
        return isFalling() ? .06 : 0;
    }

    @Override
    protected int lifetime() {
        return getWait() + 10 * 20;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return isFalling() && super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    public void tick() {
        if (tickCount < getWait() || isPlanted()) {
            setDeltaMovement(Vec3.ZERO);
        } else if (tickCount == getWait() && getWait() > 0 && !level().isClientSide) {
            setDeltaMovement(0, -DROP_SPEED, 0);
            hasImpulse = true;
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, .5f, 1.8f);
        }
        super.tick();
        if (!isRemoved() && isPlanted() && !level().isClientSide && tickCount - plantedAt() >= PLANTED_TICKS) {
            melt();
        }
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (isPlanted()) {
            return;
        }
        // Struck on someone's head or on the ground, the ingot plants itself in the ground below
        Vec3 at = hit.getLocation();
        HitResult ground = level().clip(new ClipContext(at.add(0, .5, 0), at.subtract(0, 6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 foot = ground.getType() == HitResult.Type.MISS ? at : ground.getLocation();
        setPos(foot.x, foot.y, foot.z);
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
        this.entityData.set(DATA_PLANTED, tickCount);

        Level level = level();
        Vec3 centre = foot.add(0, .5, 0);
        for (LivingEntity victim : SpellUtil.enemiesAround(level, centre, GoldfallSpell.IMPACT_RADIUS, getOwner(), e -> true)) {
            DamageSources.applyDamage(victim, getDamage(), SpellUtil.damageSource(ModSpells.GOLDFALL.get(), this, getOwner()));
            Vec3 away = victim.position().subtract(foot).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            victim.push(away.x * .6, .35, away.z * .6);
            victim.hurtMarked = true;
        }
        GoldFx.shatterBurst(level, centre.x, centre.y, centre.z, 1.4f);
        GoldFx.coins(level, centre, 5, .35f);
        SpellUtil.particles(level, GoldFx.DEEP_DUST, foot.x, foot.y + .2, foot.z, 30, 1.4, .1, 1.4, .02);
        SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, foot.x, foot.y + .3, foot.z, 6, .8, .1, .8, .01);
        SpellUtil.cameraShake(level, foot, 10, 12);
        level.playSound(null, foot.x, foot.y, foot.z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.6f, .6f + random.nextFloat() * .2f);
        level.playSound(null, foot.x, foot.y, foot.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 2f, .7f);
        level.playSound(null, foot.x, foot.y, foot.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, .6f, 1.5f);
    }

    /** Its time up, the cooled ingot melts away into coins. */
    private void melt() {
        Vec3 top = position().add(0, .9, 0);
        GoldFx.coins(level(), top, 10, .35f);
        GoldFx.shatterBurst(level(), top.x, top.y, top.z, .7f);
        level().playSound(null, top.x, top.y, top.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.2f, 1.3f);
        level().playSound(null, top.x, top.y, top.z, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.NEUTRAL, 1.5f, .8f);
        discard();
    }

    /** Client side: white-hot drips under the vault, a comet tail of molten gold as it falls, steam as it cools. */
    @Override
    protected void clientTick() {
        Level level = level();
        if (isPlanted()) {
            int since = tickCount - plantedAt();
            if (since < 20 && random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.WHITE_SMOKE, getX() + (random.nextFloat() - .5f) * .6f, getY() + 1.4, getZ() + (random.nextFloat() - .5f) * .6f, 0, .05, 0);
            } else if (random.nextInt(5) == 0) {
                level.addParticle(GoldFx.PALE_DUST, getX() + (random.nextFloat() - .5f) * .7f, getY() + random.nextFloat() * 1.4, getZ() + (random.nextFloat() - .5f) * .7f, 0, .01, 0);
            }
            return;
        }
        double cy = getY() + getBbHeight() * .5;
        if (!isFalling()) {
            if (random.nextInt(3) == 0) {
                level.addParticle(GoldFx.MOLTEN_DUST, getX() + (random.nextFloat() - .5f) * .3f, cy - SIZE * .5, getZ() + (random.nextFloat() - .5f) * .3f, 0, -.05, 0);
            }
            return;
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(GoldFx.DEEP_DUST, getX() + (random.nextFloat() - .5f) * .6f, cy + random.nextFloat() * 1.2f, getZ() + (random.nextFloat() - .5f) * .6f, 0, .02, 0);
        }
        level.addParticle(GoldFx.MOLTEN_DUST, getX(), cy + 1, getZ(), 0, 0, 0);
        if (random.nextInt(2) == 0) {
            level.addParticle(GoldFx.sparks(), getX(), cy, getZ(), (random.nextFloat() - .5f) * .2f, .1f, (random.nextFloat() - .5f) * .2f);
        }
    }

    /** The ingot stands two blocks tall and trails molten gold well above itself. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1, 1, 1).expandTowards(0, 4, 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Wait", getWait());
        tag.putInt("Planted", plantedAt());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setWait(tag.getInt("Wait"));
        this.entityData.set(DATA_PLANTED, tag.contains("Planted") ? tag.getInt("Planted") : -1);
    }
}
