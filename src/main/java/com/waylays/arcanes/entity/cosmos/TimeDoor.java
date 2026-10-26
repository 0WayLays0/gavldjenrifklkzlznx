package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import com.waylays.arcanes.spells.cosmos.PocketUniverse;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A door of the Time Variance Authority: a tall frame of orange light standing on the ground, hazy light rising inside
 * it. It opens from a line of light and closes the same way.
 * <ul>
 * <li>{@link Mode#ENTRY}: opened by the Pocket Universe; a moment later it takes its caster (and those crouching by
 * them) into their pocket universe, then closes.</li>
 * <li>{@link Mode#EXIT}: stands on each island of the pocket universe for good; walking into it takes you back where
 * you came from.</li>
 * <li>{@link Mode#GATE_IN} and {@link Mode#GATE_OUT}: the pair of the Time Door spell. Whatever goes into the first
 * (its caster, a monster pushed into it, an arrow) comes out of the second, where the caster stood five seconds
 * before, carried on at the same speed.</li>
 * </ul>
 */
public class TimeDoor extends SpellEffectEntity {
    public static final float WIDTH = 1.5f;
    public static final float HEIGHT = 2.7f;
    public static final int OPEN_TICKS = 10;
    public static final int CLOSE_TICKS = 10;
    /** When the entry door takes its travellers. */
    public static final int TAKE_TICK = 20;
    public static final int ENTRY_LIFE = TAKE_TICK + CLOSE_TICKS + 4;
    /** Ticks before something that went through a gate can go through again. */
    private static final int GATE_COOLDOWN = 30;

    public enum Mode { ENTRY, EXIT, GATE_IN, GATE_OUT }

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(TimeDoor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TimeDoor.class, EntityDataSerializers.INT);

    private final List<UUID> guests = new ArrayList<>();
    private int spellLevel = 1;
    @Nullable
    private UUID partner;
    private final Map<UUID, Long> crossed = new HashMap<>();

    public TimeDoor(EntityType<? extends TimeDoor> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, Mode.GATE_IN.ordinal());
        builder.define(DATA_LIFE, 200);
    }

    public Mode getMode() {
        return Mode.values()[Mth.clamp(this.entityData.get(DATA_MODE), 0, Mode.values().length - 1)];
    }

    /** Ticks it stays (it closes over the last ones); negative: for good. */
    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How open it is at {@code age}: 0 a line of light, 1 wide open. */
    public float openness(float age) {
        float open = Mth.clamp(age / OPEN_TICKS, 0, 1);
        int life = getLife();
        if (life >= 0) {
            open = Math.min(open, Mth.clamp((life - age) / CLOSE_TICKS, 0, 1));
        }
        return 1 - (1 - open) * (1 - open);
    }

    private static TimeDoor create(Level level, Mode mode, Vec3 at, float yRot, int life, @Nullable Entity owner) {
        TimeDoor door = new TimeDoor(ModEntities.TIME_DOOR.get(), level);
        door.setOwner(owner);
        door.moveTo(at.x, at.y, at.z, yRot, 0);
        door.entityData.set(DATA_MODE, mode.ordinal());
        door.entityData.set(DATA_LIFE, life);
        return door;
    }

    /** The door into the pocket universe, opening in front of {@code caster}; {@code guests} go along. */
    public static TimeDoor entry(Level level, LivingEntity caster, Vec3 at, int spellLevel, List<UUID> guests) {
        TimeDoor door = create(level, Mode.ENTRY, at, caster.getYRot() + 180, ENTRY_LIFE, caster);
        door.spellLevel = spellLevel;
        door.guests.addAll(guests);
        level.addFreshEntity(door);
        CosmosFx.fold(level, at.add(0, 1.3, 0), 1.2f, .9f);
        return door;
    }

    /** The pair of doors of the Time Door spell: in front of the caster, and where they stood {@code back} ago. */
    public static TimeDoor gate(Level level, LivingEntity caster, Vec3 in, float inYRot, Vec3 out, float outYRot, int life) {
        TimeDoor exit = create(level, Mode.GATE_OUT, out, outYRot, life, caster);
        TimeDoor entrance = create(level, Mode.GATE_IN, in, inYRot, life, caster);
        entrance.partner = exit.getUUID();
        exit.partner = entrance.getUUID();
        level.addFreshEntity(exit);
        level.addFreshEntity(entrance);
        CosmosFx.fold(level, in.add(0, 1.3, 0), 1.2f, 1f);
        CosmosFx.fold(level, out.add(0, 1.3, 0), 1.2f, .8f);
        return entrance;
    }

    /** Sets the way out of an island (the island round {@code centre}) if it is not already standing there. */
    public static void standAtIsland(ServerLevel pocket, BlockPos centre) {
        Vec3 at = new Vec3(centre.getX() + .5, PocketUniverse.SURFACE + 1, centre.getZ() - 1.5);
        pocket.getChunk(BlockPos.containing(at));
        if (!pocket.getEntitiesOfClass(TimeDoor.class, new AABB(at, at).inflate(4), d -> d.getMode() == Mode.EXIT).isEmpty()) {
            return;
        }
        pocket.addFreshEntity(create(pocket, Mode.EXIT, at, 0, -1, null));
    }

    /** Which way the door faces (out of its front). */
    public Vec3 facing() {
        return Vec3.directionFromRotation(0, getYRot());
    }

    /** Whether {@code entity} is in the doorway: within its width, close to its plane, between its foot and its top. */
    public boolean inDoorway(Entity entity) {
        Vec3 d = entity.getBoundingBox().getCenter().subtract(position());
        Vec3 f = facing();
        double across = d.x * f.z - d.z * f.x;
        double through = d.x * f.x + d.z * f.z;
        return Math.abs(across) < WIDTH * .5 && Math.abs(through) < .55 && d.y > -.3 && d.y < HEIGHT;
    }

    @Override
    public void tick() {
        super.tick();
        int life = getLife();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0 && openness(tickCount) > .3f) {
                Vec3 f = facing();
                double across = (random.nextDouble() - .5) * WIDTH;
                level().addParticle(CosmosFx.TIME, getX() + f.z * across, getY() + random.nextDouble() * HEIGHT, getZ() - f.x * across, 0, .02, 0);
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (life >= 0 && tickCount >= life) {
            discard();
            return;
        }
        switch (getMode()) {
            case ENTRY -> {
                if (tickCount == TAKE_TICK) {
                    take(level);
                }
            }
            case EXIT -> {
                if (tickCount % 2 == 0) {
                    for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(1.5), this::inDoorway)) {
                        PocketUniverse.leave(player);
                    }
                }
            }
            case GATE_IN -> {
                if (openness(tickCount) > .6f) {
                    pass(level);
                }
            }
            default -> {
            }
        }
    }

    private void take(ServerLevel level) {
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayer caster) || !caster.isAlive()) {
            return;
        }
        Vec3 here = position().add(0, 1, 0);
        CosmosFx.time(level, here, 40, .8);
        for (UUID id : guests) {
            if (level.getEntity(id) instanceof ServerPlayer guest && guest.isAlive() && guest.distanceTo(caster) < 6) {
                PocketUniverse.enter(guest, caster, spellLevel);
            }
        }
        PocketUniverse.enter(caster, caster, spellLevel);
    }

    /** Sends what is in the doorway out of the other door. */
    private void pass(ServerLevel level) {
        if (partner == null || !(level.getEntity(partner) instanceof TimeDoor out)) {
            return;
        }
        long now = level.getGameTime();
        crossed.values().removeIf(t -> now - t > GATE_COOLDOWN);
        List<Entity> going = level.getEntities(this, getBoundingBox().inflate(1.5), e -> e.isAlive() && !crossed.containsKey(e.getUUID())
                && !e.isPassenger() && !e.isVehicle() && passes(e) && inDoorway(e));
        for (Entity e : going) {
            crossed.put(e.getUUID(), now);
            out.crossed.put(e.getUUID(), now);
            Vec3 f = out.facing();
            Vec3 v = e.getDeltaMovement();
            double speed = Math.max(.25, Math.sqrt(v.x * v.x + v.z * v.z));
            Vec3 to = out.position().add(f.scale(.9));
            Vec3 from = e.position();
            e.teleportTo(level, to.x, to.y + .05, to.z, Set.of(), out.getYRot(), e.getXRot());
            e.setDeltaMovement(f.x * speed, v.y, f.z * speed);
            e.hurtMarked = true;
            e.resetFallDistance();
            CosmosFx.time(level, from.add(0, e.getBbHeight() * .5, 0), 16, .4);
            CosmosFx.time(level, to.add(0, e.getBbHeight() * .5, 0), 16, .4);
            CosmosFx.sound(level, from, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, .7f, 1.3f);
            CosmosFx.sound(level, to, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, .7f, 1.1f);
        }
    }

    private static boolean passes(Entity e) {
        return e instanceof LivingEntity || e instanceof Projectile || e instanceof ItemEntity || e instanceof FallingBlockEntity || e instanceof ExperienceOrb;
    }

    /** The way out of an island stands for good: anti-magic only closes the passing doors. */
    @Override
    public void onAntiMagic(io.redspace.ironsspellbooks.api.magic.MagicData playerMagicData) {
        if (getMode() != Mode.EXIT) {
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return getMode() == Mode.EXIT;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mode", getMode().ordinal());
        tag.putInt("Life", getLife());
        tag.putInt("SpellLevel", spellLevel);
        if (partner != null) {
            tag.putUUID("Partner", partner);
        }
        ListTag list = new ListTag();
        for (UUID id : guests) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("Guests", list);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_MODE, tag.getInt("Mode"));
        this.entityData.set(DATA_LIFE, tag.getInt("Life"));
        spellLevel = tag.getInt("SpellLevel");
        partner = tag.hasUUID("Partner") ? tag.getUUID("Partner") : null;
        guests.clear();
        for (Tag t : tag.getList("Guests", Tag.TAG_INT_ARRAY)) {
            guests.add(NbtUtils.loadUUID(t));
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
