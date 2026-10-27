package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Gashadokuro: a giant skeleton rises out of the ground behind the caster, waist-deep, a skull as big as a man,
 * eyes burning crimson, and roars. For its time it smashes its fists down on the caster's enemies, one arm then the
 * other, each blow shaking the ground all round; then it sinks back. Both sides work out its pose from its age and the
 * blow it is striking (synced); the server picks the targets.
 */
public class BoneColossus extends SpellEffectEntity {
    public static final int RISE = 40;
    public static final int SINK = 36;
    public static final int SLAM_EVERY = 30;
    public static final int RAISE = 14;
    public static final int IMPACT = 20;
    public static final int RETURN = 30;
    public static final float SHOULDER_X = 3.3f;
    public static final float SHOULDER_Y = 7f;
    public static final float ARM = 8f;
    /** How far behind the caster it rises (the camera behind a player stays clear of its ribs). */
    public static final float BEHIND = 7;
    public static final float SLAM_RADIUS = 4f;
    public static final float HUNT = 24;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BoneColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(BoneColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ARM = SynchedEntityData.defineId(BoneColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TX = SynchedEntityData.defineId(BoneColossus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TZ = SynchedEntityData.defineId(BoneColossus.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;
    private int slams;

    public BoneColossus(EntityType<? extends BoneColossus> entityType, Level level) {
        super(entityType, level);
    }

    public static BoneColossus raise(Level level, LivingEntity caster, AbstractSpell spell, int life, float damage) {
        BoneColossus colossus = new BoneColossus(ModEntities.BONE_COLOSSUS.get(), level);
        colossus.setOwner(caster);
        colossus.spellId = spell.getSpellId();
        colossus.damage = damage;
        Vec3 forward = Vec3.directionFromRotation(0, caster.getYRot());
        Vec3 at = SpellUtil.groundAt(level, caster.position().subtract(forward.scale(BEHIND)), 4, 1);
        colossus.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        colossus.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(colossus);
        return colossus;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_SLAM, -1000);
        builder.define(DATA_ARM, 0);
        builder.define(DATA_TX, 0f);
        builder.define(DATA_TZ, 6f);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The tick the current blow began (long ago if none). */
    public int slamStart() {
        return this.entityData.get(DATA_SLAM);
    }

    /** The arm striking: -1 its right (local -X), 1 its left. */
    public int slamSide() {
        return this.entityData.get(DATA_ARM) == 0 ? -1 : 1;
    }

    /** Where the blow lands, in its own frame (+Z ahead). */
    public Vec3 slamTarget() {
        return new Vec3(this.entityData.get(DATA_TX), 0, this.entityData.get(DATA_TZ));
    }

    /** How far out of the ground it stands (0 buried to 1 risen). */
    public float risen(float age) {
        float up = Mth.clamp(age / RISE, 0, 1);
        float down = Mth.clamp((age - getLife()) / SINK, 0, 1);
        return (1 - (1 - up) * (1 - up) * (1 - up)) * (1 - down * down);
    }

    public Vec3 toWorld(Vec3 local) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float c = Mth.cos(yaw), s = Mth.sin(yaw);
        return new Vec3(local.x * c - local.z * s, local.y, local.x * s + local.z * c);
    }

    public Vec3 toLocal(Vec3 world) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float c = Mth.cos(yaw), s = Mth.sin(yaw);
        return new Vec3(world.x * c + world.z * s, world.y, -world.x * s + world.z * c);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int life = getLife();
        if (tickCount > life + SINK) {
            discard();
            return;
        }
        if (tickCount == 1) {
            BoneFx.erupt(level, position(), 2.5f);
            SpellUtil.cameraShake(level, position(), RISE, 30);
        }
        if (tickCount < RISE && tickCount % 4 == 0) {
            for (int i = 0; i < 6; i++) {
                float a = level.random.nextFloat() * Mth.TWO_PI;
                BoneFx.splinters(level, position().add(Mth.cos(a) * 3, .3, Mth.sin(a) * 3), 6, .5);
            }
            BoneFx.sound(level, position(), SoundEvents.BONE_BLOCK_BREAK, 2f, .4f);
        }
        if (tickCount == RISE - 6) {
            level.playSound(null, getX(), getY() + 8, getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3f, .45f);
            level.playSound(null, getX(), getY() + 8, getZ(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.PLAYERS, 3f, .4f);
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (tickCount >= RISE && tickCount < life - RETURN && (tickCount - RISE) % SLAM_EVERY == 0) {
            aim(level, caster);
        }
        if (tickCount == slamStart() + RAISE) {
            BoneFx.sound(level, position().add(0, 10, 0), SoundEvents.PLAYER_ATTACK_SWEEP, 2f, .4f);
        }
        if (tickCount == slamStart() + IMPACT && spell != null) {
            Vec3 at = position().add(toWorld(slamTarget()));
            at = SpellUtil.groundAt(level, at, 5, 1);
            for (LivingEntity e : SpellUtil.enemiesAround(level, at.add(0, 1, 0), SLAM_RADIUS, caster, x -> true)) {
                double d = e.position().distanceTo(at);
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * (float) Mth.clamp(1.15 - .35 * d / SLAM_RADIUS, .6, 1), SpellUtil.damageSource(spell, this, caster));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.scale(.8 * resist)).add(0, .75 * Math.max(.4, resist), 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            BoneFx.splinters(level, at.add(0, .4, 0), 50, 1.6);
            BoneFx.marrow(level, at.add(0, .6, 0), 30, 1.4);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.4f, .5f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.6f, .4f);
            SpellUtil.cameraShake(level, at, 16, 26);
        }
    }

    /** Picks the next blow: the enemy ahead nearest the caster, struck by the arm on its side. */
    private void aim(ServerLevel level, @Nullable LivingEntity caster) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), HUNT, caster, x -> true)) {
            Vec3 local = toLocal(e.position().subtract(position()));
            if (local.z < 1.5) {
                continue;
            }
            double d = caster == null ? local.lengthSqr() : e.distanceToSqr(caster);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        if (best == null) {
            return;
        }
        Vec3 local = toLocal(best.position().subtract(position()));
        int side = Math.abs(local.x) < .5 ? (slams % 2 == 0 ? -1 : 1) : (local.x < 0 ? -1 : 1);
        // Within the arm's reach of its shoulder
        Vec3 fromShoulder = new Vec3(local.x - side * SHOULDER_X, 0, local.z);
        double reach = ARM * 2 - .6;
        double flat = Math.sqrt(reach * reach - SHOULDER_Y * SHOULDER_Y * .4);
        if (fromShoulder.length() > flat) {
            fromShoulder = fromShoulder.normalize().scale(flat);
        }
        this.entityData.set(DATA_TX, (float) (side * SHOULDER_X + fromShoulder.x));
        this.entityData.set(DATA_TZ, (float) fromShoulder.z);
        this.entityData.set(DATA_ARM, side < 0 ? 0 : 1);
        this.entityData.set(DATA_SLAM, tickCount);
        slams++;
        BoneFx.rattle(level, position().add(0, 8, 0), 2);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
