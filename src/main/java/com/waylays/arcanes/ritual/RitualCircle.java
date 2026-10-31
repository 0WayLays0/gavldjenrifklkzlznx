package com.waylays.arcanes.ritual;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A ritual circle drawn in chalk. It draws itself slowly, then waits for its participants: one kneeling (sneaking) on
 * each of its stations. Once all are there the channelling begins, long and slow: the runes light one by one, beams
 * reach from the participants to the middle, a pillar of light rises and a ring opens in the sky; each participant
 * gives mana every second. If a station stays empty (someone stands up or walks off) or someone runs out of mana, the
 * ritual breaks and lashes back at them. If it holds to the end, the ritual is cast.
 * <p>
 * Players kneeling inside the circle but off the stations join the chorus: each makes the ritual a fifth stronger (up
 * to twice as strong). A creature with the {@code arcanes_oublies_ritualist} tag (given by the staff with /tag) may
 * stand on a station in a player's place, for scenes with characters of the server.
 */
public class RitualCircle extends SpellEffectEntity {
    public enum State { DRAWING, WAITING, CHANNELING, DONE, FAILED }

    public static final float RADIUS = 3.5f;
    /** Ticks to draw the circle, to wait for the participants at most, of leeway when a station empties. */
    public static final int DRAW = 60;
    public static final int WAIT_LIMIT = 20 * 180;
    public static final int GRACE = 40;
    /** Ticks the circle lingers after the ritual is cast, or after it breaks. */
    public static final int AFTERGLOW = 80;
    public static final String RITUALIST_TAG = "arcanes_oublies_ritualist";
    public static final int MAX_CHORUS = 5;

    private static final EntityDataAccessor<Integer> DATA_RITUAL = SynchedEntityData.defineId(RitualCircle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(RitualCircle.class, EntityDataSerializers.INT);
    /** The game time it entered its current state (so a client that starts watching late sees it as it is). */
    private static final EntityDataAccessor<Long> DATA_SINCE = SynchedEntityData.defineId(RitualCircle.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_OCCUPIED = SynchedEntityData.defineId(RitualCircle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHORUS = SynchedEntityData.defineId(RitualCircle.class, EntityDataSerializers.INT);
    /** The way the wall it is drawn on faces (a horizontal direction), or -1 for a figure drawn on the ground. */
    private static final EntityDataAccessor<Integer> DATA_WALL = SynchedEntityData.defineId(RitualCircle.class, EntityDataSerializers.INT);

    @Nullable
    private Vec3 anchor;
    @Nullable
    private ResourceLocation anchorLevel;
    private int emptyTicks;

    public RitualCircle(EntityType<? extends RitualCircle> entityType, Level level) {
        super(entityType, level);
    }

    public static RitualCircle draw(ServerLevel level, @Nullable LivingEntity leader, Ritual ritual, Vec3 at, float yaw,
                                    @Nullable Vec3 anchor, @Nullable ResourceLocation anchorLevel) {
        return draw(level, leader, ritual, at, yaw, null, anchor, anchorLevel);
    }

    /**
     * Draws a ritual's figure at {@code at}: on the ground, or (with {@code wall}) upright on the face of a wall that
     * faces {@code wall}, {@code at} then being the foot of the wall's face.
     */
    public static RitualCircle draw(ServerLevel level, @Nullable LivingEntity leader, Ritual ritual, Vec3 at, float yaw, @Nullable Direction wall,
                                    @Nullable Vec3 anchor, @Nullable ResourceLocation anchorLevel) {
        RitualCircle circle = new RitualCircle(ModEntities.RITUAL_CIRCLE.get(), level);
        if (wall != null) {
            circle.entityData.set(DATA_WALL, wall.get2DDataValue());
            yaw = wall.toYRot();
        }
        circle.setOwner(leader);
        circle.moveTo(at.x, at.y, at.z, yaw, 0);
        circle.anchor = anchor;
        circle.anchorLevel = anchorLevel;
        circle.entityData.set(DATA_RITUAL, ritual.ordinal());
        circle.entityData.set(DATA_SINCE, level.getGameTime());
        level.addFreshEntity(circle);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.CALCITE_PLACE, SoundSource.PLAYERS, 1.2f, .7f);
        return circle;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RITUAL, 0);
        builder.define(DATA_STATE, 0);
        builder.define(DATA_SINCE, 0L);
        builder.define(DATA_OCCUPIED, 0);
        builder.define(DATA_CHORUS, 0);
        builder.define(DATA_WALL, -1);
    }

    public Ritual getRitual() {
        return Ritual.byIndex(this.entityData.get(DATA_RITUAL));
    }

    public State getState() {
        State[] all = State.values();
        return all[Mth.clamp(this.entityData.get(DATA_STATE), 0, all.length - 1)];
    }

    /** Ticks since the circle entered its current state. */
    public float sinceState(float partialTicks) {
        return level().getGameTime() - this.entityData.get(DATA_SINCE) + partialTicks;
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, state.ordinal());
        this.entityData.set(DATA_SINCE, level().getGameTime());
    }

    /** Which stations have someone on them (one bit per station). */
    public int getOccupied() {
        return this.entityData.get(DATA_OCCUPIED);
    }

    public int getChorus() {
        return this.entityData.get(DATA_CHORUS);
    }

    /** How far the circle is drawn (0 to 1). */
    public float drawn(float partialTicks) {
        return getState() == State.DRAWING ? Mth.clamp(sinceState(partialTicks) / DRAW, 0, 1) : 1;
    }

    /** How far the channelling has gone (0 to 1); 1 once cast. */
    public float progress(float partialTicks) {
        return switch (getState()) {
            case CHANNELING -> Mth.clamp(sinceState(partialTicks) / getRitual().channelTicks, 0, 1);
            case DONE -> 1;
            default -> 0;
        };
    }

    /** The way the wall its figure is drawn on faces, or null if it lies on the ground. */
    @Nullable
    public Direction getWall() {
        int wall = this.entityData.get(DATA_WALL);
        return wall < 0 ? null : Direction.from2DDataValue(wall);
    }

    /**
     * Where station {@code i} is, relative to the middle: round the figure on the ground, or in a row before the wall
     * for a figure drawn on one.
     */
    public Vec3 station(int i) {
        int n = getRitual().participants;
        Direction wall = getWall();
        if (wall != null) {
            Vec3 out = Vec3.atLowerCornerOf(wall.getNormal());
            Vec3 along = Vec3.atLowerCornerOf(wall.getClockWise().getNormal());
            return out.scale(2.4).add(along.scale((i - (n - 1) * .5) * 3));
        }
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / n;
        return new Vec3(Mth.cos(a) * RADIUS, 0, Mth.sin(a) * RADIUS);
    }

    /** How much stronger the chorus makes it. */
    public float power() {
        return 1 + .2f * Math.min(MAX_CHORUS, getChorus());
    }

    @Nullable
    public Vec3 getAnchor() {
        return anchor;
    }

    @Nullable
    public ResourceLocation getAnchorLevel() {
        return anchorLevel;
    }

    private static boolean kneeling(LivingEntity e) {
        if (!e.isAlive() || e.isSpectator()) {
            return false;
        }
        return e instanceof Player player ? player.isShiftKeyDown() : e.getTags().contains(RITUALIST_TAG);
    }

    /** Who kneels on each station (null where it is empty). */
    private LivingEntity[] onStations(ServerLevel level) {
        int n = getRitual().participants;
        LivingEntity[] out = new LivingEntity[n];
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(RADIUS + 3, 3, RADIUS + 3),
                RitualCircle::kneeling);
        for (int i = 0; i < n; i++) {
            Vec3 s = position().add(station(i));
            double best = 1.2 * 1.2;
            for (LivingEntity e : near) {
                double d = e.distanceToSqr(s.x, e.getY(), s.z);
                if (d < best && Math.abs(e.getY() - s.y) < 1.6 && !contains(out, e)) {
                    best = d;
                    out[i] = e;
                }
            }
        }
        return out;
    }

    private static boolean contains(LivingEntity[] all, LivingEntity e) {
        for (LivingEntity x : all) {
            if (x == e) {
                return true;
            }
        }
        return false;
    }

    /** The players kneeling inside the circle but off its stations. */
    private List<Player> chorus(ServerLevel level, LivingEntity[] stations) {
        List<Player> out = new ArrayList<>();
        Vec3 middle = getWall() == null ? position() : position().add(Vec3.atLowerCornerOf(getWall().getNormal()).scale(2.4));
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(middle, middle).inflate(RADIUS, 2, RADIUS))) {
            if (kneeling(p) && !contains(stations, p) && p.distanceToSqr(middle.x, p.getY(), middle.z) < RADIUS * RADIUS) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public void tick() {
        super.tick();
        ServerLevel level = level() instanceof ServerLevel server ? server : null;
        if (level == null) {
            return;
        }
        float since = sinceState(0);
        switch (getState()) {
            case DRAWING -> {
                if (tickCount % 6 == 0) {
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.CALCITE_STEP, SoundSource.PLAYERS, .8f, .8f + random.nextFloat() * .3f);
                }
                if (since >= DRAW) {
                    setState(State.WAITING);
                    sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, .6f);
                }
            }
            case WAITING -> waiting(level, since);
            case CHANNELING -> channel(level, since);
            case DONE, FAILED -> {
                if (since > AFTERGLOW) {
                    discard();
                }
            }
        }
    }

    private void waiting(ServerLevel level, float since) {
        LivingEntity[] stations = onStations(level);
        updateOccupied(stations);
        if (since > WAIT_LIMIT) {
            setState(State.FAILED);
            return;
        }
        for (LivingEntity e : stations) {
            if (e == null) {
                return;
            }
        }
        if (getRitual() == Ritual.PORTAL && anchor == null) {
            return;
        }
        setState(State.CHANNELING);
        emptyTicks = 0;
        sound(level, SoundEvents.BEACON_ACTIVATE, 2f, .6f);
        tell(stations, Component.translatable("ui.arcanes_oublies.ritual_begins", getRitual().title(), getRitual().channelTicks / 20)
                .withStyle(s -> s.withColor(getRitual().color)));
    }

    private void channel(ServerLevel level, float since) {
        Ritual ritual = getRitual();
        LivingEntity[] stations = onStations(level);
        updateOccupied(stations);
        List<Player> chorus = chorus(level, stations);
        this.entityData.set(DATA_CHORUS, chorus.size());
        boolean full = true;
        for (LivingEntity e : stations) {
            full &= e != null;
        }
        emptyTicks = full ? 0 : emptyTicks + 1;
        if (emptyTicks > GRACE) {
            fail(level, stations, Component.translatable("ui.arcanes_oublies.ritual_broken"));
            return;
        }
        // Every second each participant gives their share of mana
        if ((int) since % 20 == 0 && since > 0) {
            float share = Ritual.MANA / (ritual.channelTicks / 20f);
            for (LivingEntity e : stations) {
                if (e instanceof Player player && !player.isCreative()) {
                    MagicData data = MagicData.getPlayerMagicData(player);
                    if (data.getMana() < share) {
                        fail(level, stations, Component.translatable("ui.arcanes_oublies.ritual_no_mana", player.getDisplayName()));
                        return;
                    }
                    data.addMana(-share);
                }
            }
        }
        float p = since / ritual.channelTicks;
        // The chant: a deep hum that rises as it builds, and a chime for each rune lit
        if ((int) since % 40 == 0) {
            sound(level, SoundEvents.BEACON_AMBIENT, 1.6f, .5f + p * .9f);
        }
        int runes = 12;
        if ((int) since % Math.max(1, ritual.channelTicks / (runes * 3)) == 0 && p < .35f) {
            sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, .6f + p * 2);
        }
        if (p > .35f && (int) since % 10 == 0) {
            for (int i = 0; i < ritual.participants; i++) {
                Vec3 s = position().add(station(i)).add(0, 1, 0);
                SpellUtil.particles(level, ParticleTypes.END_ROD, s.x, s.y, s.z, 2, .2, .4, .2, .01);
            }
        }
        if (p > .8f && (int) since % 5 == 0) {
            SpellUtil.particles(level, ParticleTypes.END_ROD, getX(), getY() + 2 + random.nextDouble() * 18, getZ(), 3, .4, 1, .4, .05);
        }
        if (since >= ritual.channelTicks) {
            setState(State.DONE);
            Set<UUID> allies = new HashSet<>();
            for (LivingEntity e : stations) {
                if (e != null) {
                    allies.add(e.getUUID());
                }
            }
            for (Player player : chorus) {
                allies.add(player.getUUID());
            }
            if (ownerUUID != null) {
                allies.add(ownerUUID);
            }
            sound(level, SoundEvents.BEACON_POWER_SELECT, 2.5f, .7f);
            sound(level, SoundEvents.TOTEM_USE, 1.5f, .8f);
            SpellUtil.cameraShake(level, position(), 20, 30);
            RitualEffects.manifest(level, this, allies, power());
            tell(stations, Component.translatable("ui.arcanes_oublies.ritual_done", ritual.title()).withStyle(s -> s.withColor(ritual.color)));
        }
    }

    private void updateOccupied(LivingEntity[] stations) {
        int bits = 0;
        for (int i = 0; i < stations.length; i++) {
            if (stations[i] != null) {
                bits |= 1 << i;
            }
        }
        this.entityData.set(DATA_OCCUPIED, bits);
    }

    /** The ritual breaks: the gathered power lashes back at those kneeling. */
    private void fail(ServerLevel level, LivingEntity[] stations, Component why) {
        setState(State.FAILED);
        for (LivingEntity e : stations) {
            if (e != null) {
                e.hurt(level.damageSources().magic(), 4);
                Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                e.setDeltaMovement(away.scale(.8).add(0, .4, 0));
                e.hurtMarked = true;
            }
        }
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, getX(), getY() + .5, getZ(), 6, RADIUS * .5, .3, RADIUS * .5, 0);
        SpellUtil.particles(level, ParticleTypes.SMOKE, getX(), getY() + .3, getZ(), 40, RADIUS * .6, .2, RADIUS * .6, .05);
        sound(level, SoundEvents.BEACON_DEACTIVATE, 2f, .5f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, .7f);
        tell(stations, why.copy().withStyle(ChatFormatting.RED));
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void tell(LivingEntity[] stations, Component message) {
        for (LivingEntity e : stations) {
            if (e instanceof Player player) {
                player.displayClientMessage(message, true);
            }
        }
        Entity leader = getOwner();
        if (leader instanceof Player player && !contains(stations, player)) {
            player.displayClientMessage(message, true);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
