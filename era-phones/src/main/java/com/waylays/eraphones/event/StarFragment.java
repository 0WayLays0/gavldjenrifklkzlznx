package com.waylays.eraphones.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A fragment of the star the clown was born of. Too heavy for one: the first to lift it is told they are not strong
 * enough alone and waits for a second; then two to four players carry it together, standing round it, their hands
 * on it, slowly (less slowly the more they are). Each pushes it where they walk: all the same way, it goes; pulling
 * against each other, it barely moves and they are asked to work together. A game master in creative lifts it alone.
 * It glows, lights what is round it, and keeps away whoever bears the tag "clown".
 */
public class StarFragment extends Entity {
    /** Walking pace (blocks a tick) by number of bearers: one (a game master), two, three, four. */
    private static final float[] PACE = {0, .2f, .052f, .072f, .092f};
    /** How far from its middle each bearer stands, by number of bearers. */
    private static final float[] REACH = {.5f, .5f, .52f, .55f, .58f};
    /** How high its middle is carried, and floats when set down (blocks above its foot). */
    public static final float HELD_AT = 1.0f, RESTS_AT = .62f;
    /** Ticks a first bearer waits for a second; how near the second must find them; how far the clown is kept away. */
    private static final int WAIT = 600;
    private static final double WAIT_NEAR = 5, KEEP_AWAY = 7;
    /** Where each bearer last pushed it, and when (game time). */
    private static final Map<UUID, Push> PUSHES = new ConcurrentHashMap<>();

    private record Push(float x, float z, long at) {}

    private UUID waiting;
    private long waitUntil;
    private boolean dropping;
    private BlockPos light;
    private int lastNag;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot;
    // Seen on each side, for drawing: how far the bearers have walked (their steps), how much they are walking
    public float walk, walkO, stride, strideO;

    public StarFragment(EntityType<? extends StarFragment> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // The light it left when its chunk was put away: taken back as soon as it moves on
        if (tag.contains("Light")) {
            light = BlockPos.of(tag.getLong("Light"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (light != null) {
            tag.putLong("Light", light.asLong());
        }
    }

    public static float reach(int bearers) {
        return REACH[Mth.clamp(bearers, 0, 4)];
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

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(EventRegistry.FRAGMENT_ITEM.get());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().size() < 4;
    }

    // ---------------------------------------------------------------- lifting it

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer p)) {
            return InteractionResult.PASS;
        }
        if (isVehicle()) {
            // Another pair of hands
            if (getPassengers().size() >= 4) {
                p.displayClientMessage(Component.translatable("event.eraphones.fragment.full").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            if (p.startRiding(this)) {
                tellBearers(Component.translatable("event.eraphones.fragment.joins", com.waylays.eraphones.rp.RpService.name(p)));
                lift();
            }
            return InteractionResult.CONSUME;
        }
        if (p.isCreative()) {
            p.startRiding(this);
            lift();
            return InteractionResult.CONSUME;
        }
        ServerPlayer first = waiting == null ? null : p.server.getPlayerList().getPlayer(waiting);
        if (first != null && first != p && !first.isPassenger() && level().getGameTime() < waitUntil && first.level() == level()
                && first.distanceTo(this) < WAIT_NEAR) {
            // The second pair of hands: together they lift it
            waiting = null;
            first.startRiding(this);
            p.startRiding(this);
            tellBearers(Component.translatable("event.eraphones.fragment.lifted"));
            lift();
            return InteractionResult.CONSUME;
        }
        waiting = p.getUUID();
        waitUntil = level().getGameTime() + WAIT;
        p.sendSystemMessage(Component.translatable("event.eraphones.fragment.too_heavy").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, .8f, .6f);
        return InteractionResult.CONSUME;
    }

    private void lift() {
        level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.2f, .55f);
        level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, .35f, 1.6f);
    }

    private void tellBearers(Component c) {
        for (Entity e : getPassengers()) {
            if (e instanceof ServerPlayer sp) {
                sp.displayClientMessage(c.copy().withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        PUSHES.remove(passenger.getUUID());
        if (!level().isClientSide && !dropping && isVehicle() && !strongEnough()) {
            // One alone cannot hold it: it is set down
            dropping = true;
            tellBearers(Component.translatable("event.eraphones.fragment.set_down"));
            ejectPassengers();
            dropping = false;
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, 1, .5f);
        }
    }

    /** Two bearers or more, or a game master in creative. */
    private boolean strongEnough() {
        List<Entity> all = getPassengers();
        if (all.size() >= 2) {
            return true;
        }
        return all.size() == 1 && all.get(0) instanceof Player p && p.isCreative();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return passenger.position();
    }

    // ---------------------------------------------------------------- where the bearers stand

    /** Where bearer {@code i} of {@code n} stands round the fragment (the formation turns with its heading). */
    public float standAngle(int i, int n) {
        return getYRot() + (n == 4 ? 45 : 0) + 360f * i / Math.max(1, n);
    }

    private Vec3 standAt(int i, int n, double x, double y, double z, float yaw) {
        float a = (yaw + (n == 4 ? 45 : 0) + 360f * i / Math.max(1, n)) * Mth.DEG_TO_RAD;
        float r = reach(n);
        return new Vec3(x - Mth.sin(a) * r, y, z + Mth.cos(a) * r);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        List<Entity> all = getPassengers();
        int i = all.indexOf(passenger), n = all.size();
        if (i < 0) {
            return;
        }
        Vec3 at = standAt(i, n, getX(), getY(), getZ(), getYRot());
        move.accept(passenger, at.x, at.y, at.z);
        if (passenger instanceof LivingEntity l) {
            // Facing the fragment
            l.setYBodyRot(standAngle(i, n) + 180);
        }
    }

    // ---------------------------------------------------------------- moving it

    /** {@code p} pushes the fragment they carry toward (x, z). */
    public static void push(ServerPlayer p, float x, float z) {
        if (p.getVehicle() instanceof StarFragment && Float.isFinite(x) && Float.isFinite(z)) {
            float len = Mth.sqrt(x * x + z * z);
            if (len > 1) {
                x /= len;
                z /= len;
            }
            PUSHES.put(p.getUUID(), new Push(x, z, p.level().getGameTime()));
        }
    }

    @Override
    public void tick() {
        double x0 = getX(), z0 = getZ();
        super.tick();
        if (level().isClientSide) {
            tickLerp();
            animate(x0, z0);
            sparkle();
            return;
        }
        if (isVehicle() && !strongEnough()) {
            dropping = true;
            ejectPassengers();
            dropping = false;
        }
        Vec3 v = getDeltaMovement();
        double vy = onGround() ? -.04 : Math.max(-1, (v.y - .06) * .98);
        double vx = 0, vz = 0;
        if (isVehicle()) {
            Vec3 want = wanted();
            vx = v.x + (want.x - v.x) * .25;
            vz = v.z + (want.z - v.z) * .25;
        } else {
            vx = v.x * .5;
            vz = v.z * .5;
        }
        setDeltaMovement(vx, vy, vz);
        step();
        turn();
        keepAway();
        glow();
        tickWaiting();
    }

    /** Where the bearers together take it: their pushes summed; pulling apart, almost nowhere (and they are told). */
    private Vec3 wanted() {
        List<Entity> all = getPassengers();
        int n = all.size();
        long now = level().getGameTime();
        double sx = 0, sz = 0, sum = 0;
        for (Entity e : all) {
            Push p = PUSHES.get(e.getUUID());
            if (p != null && now - p.at < 12) {
                sx += p.x;
                sz += p.z;
                sum += Math.sqrt(p.x * p.x + p.z * p.z);
            }
        }
        double s = Math.sqrt(sx * sx + sz * sz);
        if (sum < .2) {
            return Vec3.ZERO;
        }
        float pace = PACE[Mth.clamp(n, 1, 4)];
        if (n >= 2 && s / sum < .45) {
            // Against each other
            if (tickCount - lastNag > 50) {
                lastNag = tickCount;
                tellBearers(Component.translatable("event.eraphones.fragment.together"));
            }
            return new Vec3(sx / Math.max(1, sum), 0, sz / Math.max(1, sum)).scale(pace * .15);
        }
        // Those not pushing are carried along, but slow it
        double effort = n == 1 ? 1 : Math.min(1, s / (n * .6));
        return new Vec3(sx / s, 0, sz / s).scale(pace * (.45 + .55 * effort));
    }

    /** Moves it, as long as none of its bearers would walk into a wall. */
    private void step() {
        Vec3 v = getDeltaMovement();
        Vec3 before = position();
        move(MoverType.SELF, v);
        if (isVehicle() && !bearersFit(getYRot())) {
            // Try along each axis alone
            setPos(before);
            move(MoverType.SELF, new Vec3(v.x, v.y, 0));
            if (!bearersFit(getYRot())) {
                setPos(before);
                move(MoverType.SELF, new Vec3(0, v.y, v.z));
                if (!bearersFit(getYRot())) {
                    setPos(before);
                    move(MoverType.SELF, new Vec3(0, v.y, 0));
                    setDeltaMovement(0, getDeltaMovement().y, 0);
                }
            }
        }
    }

    /** Whether each bearer would stand clear of the blocks (their legs may step up what the fragment steps up). */
    private boolean bearersFit(float yaw) {
        List<Entity> all = getPassengers();
        int n = all.size();
        for (int i = 0; i < n; i++) {
            Vec3 at = standAt(i, n, getX(), getY(), getZ(), yaw);
            AABB box = new AABB(at.x - .28, at.y + .6, at.z - .28, at.x + .28, at.y + 1.75, at.z + .28);
            if (!level().noCollision(all.get(i), box)) {
                return false;
            }
        }
        return true;
    }

    /** The formation turns slowly toward where it goes (its bearers' order kept: the nearest way round). */
    private void turn() {
        Vec3 v = getDeltaMovement();
        int n = getPassengers().size();
        if (n < 1 || v.horizontalDistanceSqr() < 1e-4) {
            return;
        }
        float go = (float) (Mth.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
        float sector = 360f / n;
        float diff = Mth.wrapDegrees(go - getYRot());
        diff -= Math.round(diff / sector) * sector;
        float t = Mth.clamp(diff, -2.5f, 2.5f);
        if (Math.abs(t) > .05f && bearersFit(getYRot() + t)) {
            setYRot(getYRot() + t);
        }
    }

    /** Whoever bears the tag "clown" cannot come near it: pushed back, told so. */
    private void keepAway() {
        if (tickCount % 2 != 0) {
            return;
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(KEEP_AWAY), pl -> pl.getTags().contains("clown"))) {
            if (p.getVehicle() == this) {
                continue;
            }
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
            double d = away.length();
            if (d > KEEP_AWAY) {
                continue;
            }
            away = d < 1e-3 ? new Vec3(1, 0, 0) : away.scale(1 / d);
            double force = .25 + .5 * (1 - d / KEEP_AWAY);
            p.setDeltaMovement(away.x * force, Math.max(p.getDeltaMovement().y, .1), away.z * force);
            p.hurtMarked = true;
            if (p instanceof ServerPlayer sp && tickCount % 40 == 0) {
                sp.displayClientMessage(Component.translatable("event.eraphones.fragment.repelled").withStyle(ChatFormatting.DARK_RED), true);
            }
        }
    }

    /** A soft light where it is (an invisible light block, moved along with it, never put in place of anything). */
    private void glow() {
        if (tickCount % 4 != 0) {
            return;
        }
        BlockPos at = BlockPos.containing(getX(), getY() + (isVehicle() ? HELD_AT : RESTS_AT), getZ());
        if (at.equals(light)) {
            return;
        }
        clearLight();
        if (level().getBlockState(at).isAir()) {
            level().setBlock(at, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 11), 3);
            light = at;
        }
    }

    private void clearLight() {
        if (light != null && level().getBlockState(light).is(Blocks.LIGHT)) {
            level().setBlock(light, Blocks.AIR.defaultBlockState(), 3);
        }
        light = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            clearLight();
        }
        super.remove(reason);
    }

    private void tickWaiting() {
        if (waiting == null) {
            return;
        }
        ServerPlayer first = ((net.minecraft.server.level.ServerLevel) level()).getServer().getPlayerList().getPlayer(waiting);
        if (first == null || level().getGameTime() >= waitUntil || first.level() != level() || first.distanceTo(this) > WAIT_NEAR || first.isPassenger()) {
            if (first != null && !first.isPassenger()) {
                first.displayClientMessage(Component.translatable("event.eraphones.fragment.gave_up").withStyle(ChatFormatting.GRAY), true);
            }
            waiting = null;
            return;
        }
        if (tickCount % 20 == 0) {
            first.displayClientMessage(Component.translatable("event.eraphones.fragment.waiting").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved() && source.getEntity() instanceof Player p && p.isCreative() && !isVehicle()) {
            discard();
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- on the players' side

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 4;
    }

    private void tickLerp() {
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, getXRot());
            lerpSteps--;
        }
    }

    private void animate(double x0, double z0) {
        walkO = walk;
        strideO = stride;
        double moved = Math.sqrt((getX() - x0) * (getX() - x0) + (getZ() - z0) * (getZ() - z0));
        // Short, heavy steps: a stride of about 0.6 block
        walk += (float) (moved / .6 * Mth.PI);
        stride += ((float) Math.min(1, moved / .05) - stride) * .3f;
    }

    /** A little golden dust drifting off it now and then, a bright mote rising more rarely. */
    private void sparkle() {
        if (random.nextInt(3) == 0) {
            double h = getY() + (isVehicle() ? HELD_AT : RESTS_AT);
            Vector3f c = random.nextBoolean() ? new Vector3f(1f, .86f, .5f) : new Vector3f(.8f, .85f, 1f);
            level().addParticle(new DustParticleOptions(c, .45f), getX() + (random.nextDouble() - .5) * .5, h + (random.nextDouble() - .5) * .5,
                    getZ() + (random.nextDouble() - .5) * .5, 0, .01, 0);
        }
        if (random.nextInt(14) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - .5) * .3, getY() + (isVehicle() ? HELD_AT : RESTS_AT),
                    getZ() + (random.nextDouble() - .5) * .3, 0, .015, 0);
        }
    }
}
