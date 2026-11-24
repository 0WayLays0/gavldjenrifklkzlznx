package com.waylays.arcanes.piege;

import com.waylays.arcanes.rp.arms.Proc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * A trap at work: what /piege set on it, where it is in its round (sprung {@code phase} ticks ago, or at rest), how
 * long before it can be sprung again, and when it last struck each one standing in it.
 */
public class TrapBlockEntity extends BlockEntity {
    private static final Random RANDOM = new Random();
    /** Spikes: up this many ticks after the step, down again at {@link #SPIKES_DOWN}. */
    private static final int SPIKES_UP = 5, SPIKES_DOWN = 32;
    /** Jaws and snare: shut this long. */
    private static final int BEAR_SHUT = 80, SNARE_SHUT = 60;
    /** Fire vent: hisses, then burns until {@link #FIRE_OUT}. */
    private static final int FIRE_ON = 12, FIRE_OUT = 52;
    private static final int GAS_TIME = 100, MINE_FUSE = 20;
    /** Dart trap: how far it sees. */
    private static final int DART_RANGE = 10;

    private TrapSetup setup = TrapSetup.NONE;
    private int phase = -1;
    private int rest;
    private final Map<Integer, Long> lastHit = new HashMap<>();

    public TrapBlockEntity(BlockPos pos, BlockState state) {
        super(Pieges.TYPE.get(), pos, state);
    }

    public TrapSetup setup() {
        return setup;
    }

    private TrapKind kind() {
        return getBlockState().getBlock() instanceof TrapBlock t ? t.kind : TrapKind.SPIKES;
    }

    // ---------------------------------------------------------------- saved, and carried by the item

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setup = tag.contains("piege") ? TrapSetup.CODEC.parse(NbtOps.INSTANCE, tag.get("piege")).result().orElse(TrapSetup.NONE) : TrapSetup.NONE;
        phase = tag.contains("phase") ? tag.getInt("phase") : -1;
        rest = tag.getInt("repos");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!setup.equals(TrapSetup.NONE)) {
            tag.put("piege", Pieges.encode(setup));
        }
        tag.putInt("phase", phase);
        tag.putInt("repos", rest);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        setup = input.getOrDefault(Pieges.SETUP.get(), TrapSetup.NONE);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!setup.equals(TrapSetup.NONE)) {
            components.set(Pieges.SETUP.get(), setup);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("piege");
    }

    // ---------------------------------------------------------------- what sets it off

    private Vec3 centre() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, .2, 0);
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private void setActive(ServerLevel level, BlockState state, boolean active) {
        if (state.getValue(TrapBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TrapBlock.ACTIVE, active), 3);
        }
    }

    /** Whether {@code e} was struck less than {@code every} ticks ago (and if not, marks it struck now). */
    private boolean struckLately(ServerLevel level, LivingEntity e, int every) {
        long now = level.getGameTime();
        Long last = lastHit.get(e.getId());
        if (last != null && now - last < every) {
            return true;
        }
        lastHit.put(e.getId(), now);
        if (lastHit.size() > 32) {
            lastHit.values().removeIf(t -> now - t > 200);
        }
        return false;
    }

    private void strike(ServerLevel level, LivingEntity e) {
        Pieges.strike(level, centre(), setup, e, setup.damageFor(kind()));
    }

    /** {@code e} is in the trap. */
    void touched(ServerLevel level, BlockState state, LivingEntity e) {
        TrapKind kind = kind();
        if (kind.continuous()) {
            int every = kind == TrapKind.SAW || kind == TrapKind.WALL ? 10 : 20;
            if (struckLately(level, e, every)) {
                return;
            }
            switch (kind) {
                case CALTROPS, NAILS -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
                    sound(level, SoundEvents.SWEET_BERRY_BUSH_BREAK, .5f, 1.4f);
                }
                case SAW -> {
                    sound(level, SoundEvents.GRINDSTONE_USE, .7f, 1.6f);
                    level.sendParticles(ParticleTypes.CRIT, e.getX(), worldPosition.getY() + .4, e.getZ(), 6, .2, .1, .2, .2);
                }
                case WALL -> {
                    Direction f = getBlockState().getValue(TrapBlock.FACING);
                    e.knockback(.6, -f.getStepX(), -f.getStepZ());
                    e.hurtMarked = true;
                    sound(level, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, .8f, .8f);
                }
                case TAR -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                    sound(level, SoundEvents.HONEY_BLOCK_SLIDE, .6f, .7f);
                }
                default -> {
                }
            }
            strike(level, e);
            return;
        }
        if (kind == TrapKind.SPIKES && phase >= SPIKES_UP && phase < SPIKES_DOWN) {
            // Walking into the spikes while they are up
            if (!struckLately(level, e, 20)) {
                strike(level, e);
            }
            return;
        }
        if (kind == TrapKind.DARTS || phase >= 0 || rest > 0) {
            return;
        }
        // Set off
        phase = 0;
        setChanged();
        switch (kind) {
            case SPIKES -> sound(level, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, .8f, .6f);
            case BEAR -> {
                setActive(level, state, true);
                sound(level, SoundEvents.IRON_TRAPDOOR_CLOSE, 1.2f, .55f);
                sound(level, SoundEvents.PLAYER_ATTACK_CRIT, 1, .7f);
                level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + .3, e.getZ(), 10, .2, .1, .2, .1);
                strike(level, e);
            }
            case SNARE -> {
                setActive(level, state, true);
                sound(level, SoundEvents.LEASH_KNOT_PLACE, 1.2f, .8f);
                strike(level, e);
            }
            case FIRE -> sound(level, SoundEvents.FIRE_EXTINGUISH, .8f, .6f);
            case GAS -> {
                setActive(level, state, true);
                sound(level, SoundEvents.BREWING_STAND_BREW, 1.2f, .5f);
                sound(level, SoundEvents.FIRE_EXTINGUISH, .7f, .4f);
                gas(level, e);
            }
            case MINE -> {
                setActive(level, state, true);
                sound(level, SoundEvents.TRIPWIRE_CLICK_ON, 1.2f, .6f);
            }
            default -> {
            }
        }
    }

    /** A jaw trap (or a snare) opened by hand. */
    void reset(ServerLevel level, BlockState state) {
        phase = -1;
        rest = 40;
        setActive(level, state, false);
        setChanged();
    }

    private void end(ServerLevel level, BlockState state) {
        phase = -1;
        rest = kind().rest;
        setActive(level, state, false);
        setChanged();
    }

    /** The living within {@code box}, that a trap does not leave alone. */
    private List<LivingEntity> in(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> !Pieges.ignored(e));
    }

    // ---------------------------------------------------------------- its round

    public static void serverTick(Level l, BlockPos pos, BlockState state, TrapBlockEntity trap) {
        if (!(l instanceof ServerLevel level)) {
            return;
        }
        if (trap.rest > 0 && --trap.rest == 0) {
            trap.setChanged();
        }
        TrapKind kind = trap.kind();
        if (kind == TrapKind.DARTS) {
            trap.darts(level, state);
            return;
        }
        if (trap.phase < 0) {
            return;
        }
        int t = ++trap.phase;
        double x = pos.getX() + .5, y = pos.getY(), z = pos.getZ() + .5;
        switch (kind) {
            case SPIKES -> {
                if (t == SPIKES_UP) {
                    trap.setActive(level, state, true);
                    trap.sound(level, SoundEvents.PISTON_EXTEND, .8f, 1.5f);
                    trap.sound(level, SoundEvents.PLAYER_ATTACK_SWEEP, .7f, 1.3f);
                    for (LivingEntity e : trap.in(level, new AABB(pos).expandTowards(0, 1, 0))) {
                        trap.lastHit.put(e.getId(), level.getGameTime());
                        trap.strike(level, e);
                        level.sendParticles(ParticleTypes.CRIT, e.getX(), y + .6, e.getZ(), 8, .25, .3, .25, .1);
                    }
                } else if (t == SPIKES_DOWN) {
                    trap.sound(level, SoundEvents.PISTON_CONTRACT, .6f, 1.6f);
                    trap.end(level, state);
                }
            }
            case BEAR -> {
                if (t == BEAR_SHUT) {
                    trap.sound(level, SoundEvents.IRON_TRAPDOOR_OPEN, 1, .6f);
                    trap.end(level, state);
                }
            }
            case SNARE -> {
                if (t == SNARE_SHUT) {
                    trap.end(level, state);
                }
            }
            case FIRE -> {
                if (t < FIRE_ON) {
                    if (t % 2 == 0) {
                        level.sendParticles(ParticleTypes.SMOKE, x, y + .3, z, 3, .2, .05, .2, .01);
                    }
                } else if (t == FIRE_ON) {
                    trap.setActive(level, state, true);
                    trap.sound(level, SoundEvents.BLAZE_SHOOT, 1, .7f);
                    trap.sound(level, SoundEvents.FIRECHARGE_USE, .8f, .6f);
                }
                if (t >= FIRE_ON && t < FIRE_OUT) {
                    for (double h = .3; h < 3; h += .45) {
                        level.sendParticles(ParticleTypes.FLAME, x, y + h, z, 3, .16 + h * .05, .1, .16 + h * .05, .02);
                    }
                    if (t % 4 == 0) {
                        level.sendParticles(ParticleTypes.LAVA, x, y + .4, z, 1, .2, 0, .2, 0);
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 3, z, 2, .2, .1, .2, .01);
                    }
                    if (t % 8 == 0) {
                        trap.sound(level, SoundEvents.FIRE_AMBIENT, 1, .8f);
                    }
                    if ((t - FIRE_ON) % 10 == 0) {
                        for (LivingEntity e : trap.in(level, new AABB(pos).expandTowards(0, 2.5, 0))) {
                            e.igniteForSeconds(4);
                            trap.strike(level, e);
                        }
                    }
                } else if (t == FIRE_OUT) {
                    trap.end(level, state);
                }
            }
            case GAS -> {
                if (t % 6 == 0) {
                    level.sendParticles(ParticleTypes.SNEEZE, x, y + .3, z, 2, .2, .05, .2, .02);
                }
                if (t == GAS_TIME) {
                    trap.end(level, state);
                }
            }
            case MINE -> {
                if (t % 4 == 0 && t < MINE_FUSE) {
                    level.sendParticles(ParticleTypes.SMOKE, x, y + .2, z, 1, .1, 0, .1, 0);
                }
                if (t == MINE_FUSE) {
                    trap.explode(level, state);
                }
            }
            default -> trap.phase = -1;
        }
    }

    /** The gas vent breathes out its cloud: the effects set on it (each if its chance comes up), or poison. */
    private void gas(ServerLevel level, LivingEntity stepped) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (Proc raw : setup.effects()) {
            Proc p = raw.normalized();
            if (p.special()) {
                continue;
            }
            var effect = Pieges.effect(p.type());
            if (effect != null && RANDOM.nextFloat() * 100 < p.chance()) {
                effects.add(new MobEffectInstance(effect, Math.max(1, p.seconds()) * 20, Math.max(0, p.power() - 1)));
            }
        }
        if (setup.effects().isEmpty()) {
            effects.add(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
        // Its own actions (set alight, freeze...) on whoever stepped on it
        TrapSetup specials = new TrapSetup(setup.damage(), setup.effects().stream().filter(p -> p.normalized().special()).toList());
        Pieges.strike(level, centre(), specials, stepped, setup.damageFor(TrapKind.GAS));
        AreaEffectCloud cloud = new AreaEffectCloud(level, worldPosition.getX() + .5, worldPosition.getY() + .2, worldPosition.getZ() + .5);
        cloud.setRadius(2.5f);
        cloud.setRadiusPerTick(-.01f);
        cloud.setDuration(GAS_TIME);
        cloud.setWaitTime(0);
        cloud.setPotionContents(new PotionContents(Optional.empty(), Optional.of(0x7FBF3F), effects));
        level.addFreshEntity(cloud);
    }

    /** The mine goes off: a blast that spares the blocks, then its own damage and effects on those near. */
    private void explode(ServerLevel level, BlockState state) {
        Vec3 c = Vec3.atBottomCenterOf(worldPosition).add(0, .3, 0);
        level.explode(null, level.damageSources().source(Pieges.DAMAGE), null, c.x, c.y, c.z, 2.2f, false, Level.ExplosionInteraction.NONE);
        float damage = setup.damageFor(TrapKind.MINE);
        for (LivingEntity e : in(level, new AABB(worldPosition).inflate(3))) {
            double d = e.position().distanceTo(c);
            if (d < 3.5) {
                Pieges.strike(level, c, setup, e, damage * (float) (1 - d / 3.5));
            }
        }
        end(level, state);
    }

    /** The dart trap watches the ten blocks before it and shoots at the nearest one it sees there. */
    private void darts(ServerLevel level, BlockState state) {
        if (rest > 0 || level.getGameTime() % 4 != 0) {
            return;
        }
        Direction f = state.getValue(TrapBlock.FACING);
        Vec3 muzzle = Vec3.atCenterOf(worldPosition).add(f.getStepX() * .6, 0, f.getStepZ() * .6);
        AABB look = new AABB(worldPosition.relative(f)).expandTowards(f.getStepX() * (DART_RANGE - 1), 1, f.getStepZ() * (DART_RANGE - 1))
                .inflate(f.getAxis() == Direction.Axis.X ? 0 : .5, 0, f.getAxis() == Direction.Axis.Z ? 0 : .5);
        LivingEntity target = in(level, look).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(muzzle))).orElse(null);
        if (target == null) {
            return;
        }
        Vec3 aim = target.getBoundingBox().getCenter();
        HitResult hit = level.clip(new ClipContext(muzzle, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceToSqr(muzzle) < aim.distanceToSqr(muzzle) - .25) {
            return;
        }
        Arrow dart = new Arrow(level, muzzle.x, muzzle.y, muzzle.z, new ItemStack(Items.ARROW), null);
        Vec3 d = aim.subtract(muzzle);
        float speed = 2.4f;
        dart.shoot(d.x, d.y + d.horizontalDistance() * .03, d.z, speed, .5f);
        dart.setBaseDamage(Math.max(.1, setup.damageFor(TrapKind.DARTS) / speed));
        dart.pickup = AbstractArrow.Pickup.DISALLOWED;
        if (!setup.effects().isEmpty()) {
            dart.getPersistentData().put(Pieges.DART, Pieges.encode(setup));
        }
        level.addFreshEntity(dart);
        sound(level, SoundEvents.DISPENSER_LAUNCH, 1, 1.4f);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, .05, .05, .05, .01);
        rest = TrapKind.DARTS.rest;
        setChanged();
    }
}
