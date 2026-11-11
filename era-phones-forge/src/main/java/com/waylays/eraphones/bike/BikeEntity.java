package com.waylays.eraphones.bike;

import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A bicycle of the 1980s. Used, it is ridden; forward pedals (faster and faster, up to a good speed), back brakes, the
 * bike goes where its rider looks, leaning into the turns; jump rings the bell. It climbs a block's step, coasts when
 * not pedalled, slows in water. Hit, it is picked up. Driven on the rider's own side, as boats are.
 */
public class BikeEntity extends Entity {
    /** The four bicycles: the red chopper, the blue racer, the green town bike, the chrome BMX (their item names). */
    public static final String[] KINDS = {"velo", "velo_course", "velo_ville", "velo_bmx"};
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(BikeEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    /** Top speed (blocks a tick: about 30 km/h) and how fast it gets there, how hard it brakes. */
    public static final float MAX = .42f, PEDAL = .011f, BRAKE = .03f;
    /** The wheel's radius and the gearing from cranks to wheel (blocks; turns). */
    public static final float WHEEL_R = 5.3f / 16, GEAR = 2.4f;
    private float speed;
    private int bellWait;
    // On each side, for drawing: the wheels' and the cranks' turn (radians), the steering and the lean (degrees)
    public float spin, spinO, crank, crankO, steer, steerO, lean, leanO;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot;

    public BikeEntity(EntityType<? extends BikeEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, 0);
    }

    public int kind() {
        return Mth.clamp(entityData.get(KIND), 0, KINDS.length - 1);
    }

    public void setKind(int kind) {
        entityData.set(KIND, Mth.clamp(kind, 0, KINDS.length - 1));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(tag.getInt("Kind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind());
    }

    private ItemStack item() {
        return new ItemStack(GearRegistry.BIKE_ITEMS.get(kind()).get());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        // The rider's hips on the long seat, the feet on the pedals: their feet .29 above the ground
        Vec3 a = new Vec3(0, .29, -.36).yRot(-getYRot() * Mth.DEG_TO_RAD);
        move.accept(passenger, getX() + a.x, getY() + a.y, getZ() + a.z);
        if (passenger instanceof LivingEntity l) {
            l.setYBodyRot(getYRot());
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player p) {
            if (!p.getAbilities().instabuild) {
                spawnAtLocation(item());
            }
            discard();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return item();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 6;
    }

    private void lerpStep() {
        double k = 1.0 / lerpSteps;
        setPos(getX() + (lerpX - getX()) * k, getY() + (lerpY - getY()) * k, getZ() + (lerpZ - getZ()) * k);
        setYRot(getYRot() + net.minecraft.util.Mth.wrapDegrees((float) lerpYRot - getYRot()) * (float) k);
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpStep();
            lerpSteps--;
        }
    }

    @Override
    public void tick() {
        double x0 = getX(), z0 = getZ();
        float yaw0 = getYRot();
        super.tick();
        tickLerp();
        LivingEntity rider = getControllingPassenger();
        if (isControlledByLocalInstance()) {
            drive(rider);
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision) {
                speed *= .3f;
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        checkInsideBlocks();
        if (!level().isClientSide) {
            ringBell(rider);
        }
        animate(x0, z0, yaw0);
    }

    /** The rider's legs and eyes: pedal, brake, go where they look. */
    private void drive(@Nullable LivingEntity rider) {
        float forward = rider == null ? 0 : rider.zza;
        if (forward > 0) {
            speed += PEDAL * forward * (1 - speed / MAX * .6f);
        } else if (forward < 0) {
            speed = speed > 0 ? Math.max(0, speed - BRAKE) : Math.max(-.06f, speed - .006f);
        } else {
            speed *= onGround() ? .985f : .995f;
        }
        if (isInWater()) {
            speed *= .85f;
        }
        speed = Mth.clamp(speed, -.06f, MAX);
        if (Math.abs(speed) < .002f && forward == 0) {
            speed = 0;
        }
        if (rider != null) {
            // Toward where the rider looks, more slowly the faster the bike goes; strafing turns it as well
            float want = Mth.wrapDegrees(rider.getYRot() - getYRot());
            float rate = 9f - 5f * Math.min(1, Math.abs(speed) / MAX);
            float turn = Mth.clamp(want, -rate, rate) - rider.xxa * rate * .5f;
            setYRot(getYRot() + turn);
        }
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        double dy = getDeltaMovement().y;
        dy = onGround() ? -.04 : (dy - .08) * .98;
        setDeltaMovement(dir.x * speed, dy, dir.z * speed);
    }

    private void ringBell(@Nullable LivingEntity rider) {
        if (bellWait > 0) {
            bellWait--;
        }
        if (rider != null && ((com.waylays.eraphones.mixin.LivingEntityAccessor) rider).eraphones$jumping() && bellWait == 0) {
            bellWait = 14;
            level().playSound(null, getX(), getY() + 1, getZ(), GearRegistry.BIKE_BELL.get(), SoundSource.NEUTRAL, .9f, .97f + random.nextFloat() * .06f);
        }
    }

    /** The wheels turn with the ground covered, the cranks with them going forward; the front steers into turns. */
    private void animate(double x0, double z0, float yaw0) {
        spinO = spin;
        crankO = crank;
        steerO = steer;
        leanO = lean;
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        double moved = (getX() - x0) * dir.x + (getZ() - z0) * dir.z;
        spin += (float) (moved / WHEEL_R);
        if (moved > 0.001) {
            crank += (float) (moved / WHEEL_R / GEAR);
        }
        float turning = Mth.wrapDegrees(getYRot() - yaw0);
        steer += (Mth.clamp(turning * 3.5f, -32, 32) - steer) * .35f;
        float fast = (float) Math.min(1, Math.abs(moved) / MAX);
        lean += (Mth.clamp(turning * 2.2f * fast, -24, 24) - lean) * .25f;
    }

    public float speed() {
        return speed;
    }
}
