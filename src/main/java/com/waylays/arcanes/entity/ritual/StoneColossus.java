package com.waylays.arcanes.entity.ritual;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Stone Colossus: called up by the ritual, stones tear themselves out of the ground all round and fly together into
 * a giant of rough stone, mossy and cracked, a light of amethyst burning in its chest and its eyes. It follows the
 * ritual's leader and fights every hostile creature near: its fists come down together and the ground shakes round them,
 * throwing back all that stands there. When its time is up it falls apart, its stones tumbling back to the ground.
 * It has no name over its head.
 */
public class StoneColossus extends PathfinderMob {
    public static final int ASSEMBLE = 60;
    public static final int CRUMBLE = 36;
    public static final int SLAM_TICKS = 16;
    public static final int LIFE = 20 * 60 * 3;

    // Game times (so a client that starts watching it late sees it as it is, not assembling again)
    private static final EntityDataAccessor<Long> DATA_BORN = SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_SLAM = SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_END = SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.LONG);

    private final Set<UUID> allies = new HashSet<>();
    @Nullable
    private UUID leader;
    private int life = LIFE;

    public StoneColossus(EntityType<? extends StoneColossus> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.ARMOR, 16)
                .add(Attributes.MOVEMENT_SPEED, .26)
                .add(Attributes.ATTACK_DAMAGE, 22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.STEP_HEIGHT, 1.6)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    /** Raises a colossus at {@code at} for the ritual's participants ({@code allies}), led by {@code leader}. */
    public static StoneColossus raise(ServerLevel level, Vec3 at, @Nullable UUID leader, Set<UUID> allies, float power) {
        StoneColossus colossus = new StoneColossus(ModEntities.STONE_COLOSSUS.get(), level);
        colossus.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        colossus.leader = leader;
        colossus.allies.addAll(allies);
        colossus.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300 * power);
        colossus.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(22 * power);
        colossus.setHealth(colossus.getMaxHealth());
        colossus.setPersistenceRequired();
        colossus.entityData.set(DATA_BORN, level.getGameTime());
        level.addFreshEntity(colossus);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2, .6f);
        return colossus;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BORN, Long.MIN_VALUE / 2);
        builder.define(DATA_SLAM, Long.MIN_VALUE / 2);
        builder.define(DATA_END, -1L);
    }

    private long now() {
        return level().getGameTime();
    }

    /** Ticks since it was raised. */
    public float age(float partialTicks) {
        return now() - this.entityData.get(DATA_BORN) + partialTicks;
    }

    /** Ticks since its fists last came down. */
    public float sinceSlam(float partialTicks) {
        return Math.min(10_000, now() - this.entityData.get(DATA_SLAM) + partialTicks);
    }

    /** Ticks since it began to fall apart, or -1. */
    public float sinceEnd(float partialTicks) {
        long end = this.entityData.get(DATA_END);
        return end < 0 ? -1 : now() - end + partialTicks;
    }

    public boolean assembling() {
        return age(0) < ASSEMBLE;
    }

    public boolean crumbling() {
        return this.entityData.get(DATA_END) >= 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new FollowLeaderGoal());
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .6));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                e -> e instanceof Enemy && !allies.contains(e.getUUID())));
    }

    @Nullable
    private Player leader() {
        return leader == null ? null : level().getPlayerByUUID(leader);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (assembling() || crumbling()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            setTarget(null);
        }
        long age = (long) age(0);
        if (assembling() && age % 3 == 0) {
            dust(level, 1.6, 12);
            if (age % 9 == 0) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2, .5f + random.nextFloat() * .3f);
            }
        }
        if (age == ASSEMBLE) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 2.5f, .4f);
            SpellUtil.cameraShake(level, position(), 12, 24);
        }
        if (crumbling()) {
            long since = (long) sinceEnd(0);
            if (since % 4 == 0) {
                dust(level, 2.2, 16);
                level.playSound(null, getX(), getY() + 3, getZ(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2, .4f + random.nextFloat() * .3f);
            }
            if (since > CRUMBLE) {
                discard();
            }
            return;
        }
        if (--life <= 0) {
            this.entityData.set(DATA_END, now());
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.HOSTILE, 2.5f, .4f);
        }
        long sinceSlam = (long) sinceSlam(0);
        if (sinceSlam == SLAM_TICKS / 2) {
            shake(level);
        }
        if (sinceSlam < SLAM_TICKS) {
            getNavigation().stop();
        }
    }

    /** Stone dust and chips round its feet. */
    private void dust(ServerLevel level, double spread, int count) {
        BlockState under = level.getBlockState(BlockPos.containing(position()).below());
        BlockState chips = under.isAir() ? Blocks.STONE.defaultBlockState() : under;
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, chips), getX(), getY() + .3, getZ(), count, spread, .3, spread, .15);
        SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + .2, getZ(), count / 4, spread, .1, spread, .02);
    }

    /** Its fists come down together before it: everything standing there is struck and thrown back, the ground shakes. */
    private void shake(ServerLevel level) {
        Vec3 front = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(2.6));
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(front, front).inflate(3.2, 2, 3.2))) {
            if (e == this || !e.isAlive() || e instanceof Player || allies.contains(e.getUUID()) || !(e instanceof Enemy)) {
                continue;
            }
            e.hurt(damageSources().mobAttack(this), damage);
            Vec3 away = e.position().subtract(front).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(away.scale(1.1 * resist).add(0, .6 * resist, 0));
            e.hurtMarked = true;
        }
        BlockState under = level.getBlockState(BlockPos.containing(front).below());
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, under.isAir() ? Blocks.STONE.defaultBlockState() : under),
                front.x, front.y + .2, front.z, 60, 2, .2, 2, .3);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, front.x, front.y + .4, front.z, 3, 1, .2, 1, 0);
        level.playSound(null, front.x, front.y, front.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, .5f);
        level.playSound(null, front.x, front.y, front.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2f, .4f);
        SpellUtil.cameraShake(level, front, 8, 16);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (assembling() || crumbling() || sinceSlam(0) < SLAM_TICKS) {
            return false;
        }
        this.entityData.set(DATA_SLAM, now());
        getLookControl().setLookAt(target);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (assembling() || crumbling()) {
            return false;
        }
        Entity by = source.getEntity();
        if (by != null && (by instanceof Player || allies.contains(by.getUUID()))) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.4f, .5f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ColossusLife", life);
        tag.putLong("Born", this.entityData.get(DATA_BORN));
        if (leader != null) {
            tag.putUUID("Leader", leader);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.getInt("ColossusLife");
        this.entityData.set(DATA_BORN, tag.contains("Born") ? tag.getLong("Born") : level().getGameTime() - ASSEMBLE);
        if (tag.hasUUID("Leader")) {
            leader = tag.getUUID("Leader");
        }
        if (leader != null) {
            allies.add(leader);
        }
    }

    /** Keeps near the ritual's leader when there is nothing to fight. */
    private class FollowLeaderGoal extends Goal {
        FollowLeaderGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = leader();
            return p != null && getTarget() == null && !assembling() && !crumbling() && distanceToSqr(p) > 8 * 8 && distanceToSqr(p) < 64 * 64;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = leader();
            return p != null && getTarget() == null && distanceToSqr(p) > 4 * 4;
        }

        @Override
        public void tick() {
            Player p = leader();
            if (p != null) {
                getLookControl().setLookAt(p, 10, getMaxHeadXRot());
                getNavigation().moveTo(p, 1);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
