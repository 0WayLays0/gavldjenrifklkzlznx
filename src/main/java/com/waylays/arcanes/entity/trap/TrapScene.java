package com.waylays.arcanes.entity.trap;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A trap of Trap Magic, drawn on the ground where the caster aimed: faint while it waits, it springs on the first enemy
 * to step in and lays its curses on them. No trap stuns, roots or holds: they only weaken (poison, slowness, weakness,
 * bleeding, rust, withering, blindness...). Some act longer: the echo trap springs again and again, each time harder;
 * the plague carrier passes its sickness on; the leech feeds its caster; the cursed web lays a new curse every two
 * seconds on all inside it.
 */
public class TrapScene extends SpellEffectEntity {
    public enum Kind { SNARE, HEX, BLEED, SOOT, RUST, ECHO, PLAGUE, LEECH, MINE, WEB }

    /** How long a trap takes to arm, how near an enemy must step, how long the trap waits. */
    public static final int ARM = 10;
    public static final double REACH = 1.4;
    /** Soot mine: how far its cloud reaches. */
    public static final double SOOT_REACH = 3.5;
    /** Echo trap: how many times it springs, how long between two on the same enemy, its strongest. */
    public static final int ECHO_CHARGES = 5, ECHO_REST = 30, ECHO_MAX = 2;
    /** Plague carrier: how long the victim carries it, how far it passes on. */
    public static final int PLAGUE_TIME = 160;
    public static final double PLAGUE_REACH = 3.5;
    /** Leech: how long it drinks, how often. */
    public static final int LEECH_TIME = 120;
    /** Cursed web: how often it lays a curse. */
    public static final int WEB_EVERY = 40;
    /** The curses of the minefield's mines and the web, in turn. */
    public static final int CURSES = 7;
    public static final int[] COLORS = {0x96FF5A, 0x8CA0FF, 0xA05AFF, 0x5A4050, 0xE0E0F0, 0xC81E28, 0xBE6428};

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(TrapScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TrapScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(TrapScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TrapScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(TrapScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VICTIM = SynchedEntityData.defineId(TrapScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SPRUNG = SynchedEntityData.defineId(TrapScene.class, EntityDataSerializers.BOOLEAN);

    /** Seconds (in ticks) each power multiplies a curse's length by. */
    private float power = 1;
    private int charges = ECHO_CHARGES;
    private final Map<Integer, int[]> echoes = new HashMap<>();
    private int sprungAt = -1;

    public TrapScene(EntityType<? extends TrapScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A trap of {@code kind} at {@code at}, {@code size} its reach (the web's radius), waiting {@code life} ticks; {@code power} scales its curses. */
    public static TrapScene spawn(Level level, @Nullable LivingEntity caster, Kind kind, Vec3 at, float size, int life, float power, int variant) {
        TrapScene s = new TrapScene(ModEntities.TRAP_SCENE.get(), level);
        s.setOwner(caster);
        s.power = power;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_SIZE, size);
        s.entityData.set(DATA_VARIANT, variant);
        s.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(s);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_STRUCK, -1000);
        builder.define(DATA_VICTIM, -1);
        builder.define(DATA_SPRUNG, false);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public boolean isSprung() {
        return this.entityData.get(DATA_SPRUNG);
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    /** The enemy it last sprang on (for the leech's and the plague's threads). */
    @Nullable
    public Entity victim() {
        int id = this.entityData.get(DATA_VICTIM);
        return id >= 0 ? level().getEntity(id) : null;
    }

    /** The trap's colour: its kind's, or the curse it lays for a mine. */
    public int color() {
        return switch (getKind()) {
            case SNARE -> 0x6EDC3C;
            case HEX -> 0xA05AFF;
            case BLEED -> 0xD22832;
            case SOOT -> 0x7A7488;
            case RUST -> 0xC86E2E;
            case ECHO -> 0xB48CFF;
            case PLAGUE -> 0x9CFF50;
            case LEECH -> 0xE0283C;
            case MINE -> COLORS[Math.floorMod(getVariant(), CURSES)];
            case WEB -> 0xC8A0FF;
        };
    }

    private int ticks(float seconds) {
        return Math.round(seconds * 20 * power);
    }

    private void curse(LivingEntity e, Holder<MobEffect> effect, float seconds, int amplifier) {
        MobEffectInstance had = e.getEffect(effect);
        int duration = ticks(seconds);
        if (had == null || had.getAmplifier() < amplifier || had.getDuration() < duration) {
            e.addEffect(new MobEffectInstance(effect, duration, Math.max(amplifier, had == null ? 0 : had.getAmplifier()), false, true, true));
        }
    }

    /** The {@code n}th of the curses the mines and the web lay in turn. */
    private void curseNumber(LivingEntity e, int n, float seconds) {
        switch (Math.floorMod(n, CURSES)) {
            case 0 -> curse(e, MobEffects.POISON, seconds, 0);
            case 1 -> curse(e, MobEffects.MOVEMENT_SLOWDOWN, seconds, 1);
            case 2 -> curse(e, MobEffects.WEAKNESS, seconds, 1);
            case 3 -> curse(e, MobEffects.WITHER, seconds, 0);
            case 4 -> {
                curse(e, MobEffects.GLOWING, seconds * 2, 0);
                curse(e, MobEffects.BLINDNESS, seconds * .6f, 0);
            }
            case 5 -> curse(e, ModEffects.BLEEDING, seconds, 0);
            default -> curse(e, ModEffects.SUNDERED, seconds, 0);
        }
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void spring(LivingEntity e) {
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
        this.entityData.set(DATA_VICTIM, e.getId());
        this.entityData.set(DATA_SPRUNG, true);
        int c = color();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 1.2f);
        SpellUtil.particles(level(), dust, getX(), getY() + .4, getZ(), 24, .6, .3, .6, .05);
        SpellUtil.particles(level(), ParticleTypes.SMOKE, getX(), getY() + .2, getZ(), 8, .4, .1, .4, .02);
        sound(SoundEvents.TRIPWIRE_CLICK_ON, 1, .8f);
        sound(SoundEvents.CHAIN_PLACE, .8f, 1.4f);
    }

    private List<LivingEntity> enemies(ServerLevel level, double radius, @Nullable LivingEntity caster) {
        double y = getY();
        return SpellUtil.enemiesAround(level, position(), radius, caster, x -> Math.abs(x.getY() - y) < 2.5);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount > getLife()) {
            discard();
            return;
        }
        if (tickCount < ARM) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        Kind kind = getKind();
        if (kind == Kind.WEB) {
            web(level, caster);
            return;
        }
        if (isSprung()) {
            after(level, caster, kind);
            if (kind != Kind.ECHO) {
                return;
            }
        }
        List<LivingEntity> near = enemies(level, REACH, caster);
        if (near.isEmpty()) {
            return;
        }
        LivingEntity e = near.get(0);
        switch (kind) {
            case SNARE -> {
                spring(e);
                curse(e, MobEffects.POISON, 6, 0);
                curse(e, MobEffects.MOVEMENT_SLOWDOWN, 6, 0);
                sound(SoundEvents.SWEET_BERRY_BUSH_BREAK, 1, .8f);
                end(30);
            }
            case HEX -> {
                spring(e);
                curse(e, MobEffects.WEAKNESS, 8, 1);
                curse(e, MobEffects.DIG_SLOWDOWN, 8, 1);
                sound(SoundEvents.EVOKER_CAST_SPELL, 1, 1.2f);
                end(30);
            }
            case BLEED -> {
                spring(e);
                curse(e, ModEffects.BLEEDING, 8, 0);
                sound(SoundEvents.TRIDENT_HIT, 1, .7f);
                end(30);
            }
            case SOOT -> {
                spring(e);
                for (LivingEntity x : enemies(level, SOOT_REACH, caster)) {
                    curse(x, MobEffects.BLINDNESS, 5, 0);
                    curse(x, MobEffects.DARKNESS, 6, 0);
                    curse(x, MobEffects.GLOWING, 10, 0);
                }
                SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, getX(), getY() + .8, getZ(), 50, 1.4, .8, 1.4, .02);
                sound(SoundEvents.FIRE_EXTINGUISH, 1.2f, .6f);
                end(40);
            }
            case RUST -> {
                spring(e);
                curse(e, ModEffects.RUSTED, 10, 0);
                curse(e, ModEffects.SUNDERED, 10, 0);
                sound(SoundEvents.IRON_DOOR_CLOSE, 1, .7f);
                end(30);
            }
            case ECHO -> echo(level, caster, near);
            case PLAGUE -> {
                spring(e);
                curse(e, MobEffects.POISON, 8, 0);
                curse(e, MobEffects.HUNGER, 8, 1);
                sound(SoundEvents.ZOMBIE_VILLAGER_CURE, .6f, 1.6f);
                sprungAt = tickCount;
                this.entityData.set(DATA_LIFE, tickCount + PLAGUE_TIME + 10);
            }
            case LEECH -> {
                spring(e);
                curse(e, MobEffects.WITHER, LEECH_TIME / 20f, 0);
                sound(SoundEvents.HONEY_BLOCK_SLIDE, 1.2f, .6f);
                sprungAt = tickCount;
                this.entityData.set(DATA_LIFE, tickCount + LEECH_TIME + 10);
            }
            case MINE -> {
                spring(e);
                curseNumber(e, getVariant(), 7);
                sound(SoundEvents.FIREWORK_ROCKET_BLAST, .5f, 1.6f);
                end(30);
            }
            default -> {
            }
        }
    }

    private void end(int linger) {
        this.entityData.set(DATA_LIFE, tickCount + linger);
    }

    /** The echo trap: it springs on each enemy again after a rest, its curse a step stronger each time, until its charges are spent. */
    private void echo(ServerLevel level, @Nullable LivingEntity caster, List<LivingEntity> near) {
        for (LivingEntity e : near) {
            int[] seen = echoes.computeIfAbsent(e.getId(), k -> new int[] {-1000, -1});
            if (tickCount - seen[0] < ECHO_REST || charges <= 0) {
                continue;
            }
            seen[0] = tickCount;
            seen[1] = Math.min(ECHO_MAX, seen[1] + 1);
            spring(e);
            curse(e, MobEffects.MOVEMENT_SLOWDOWN, 6, seen[1]);
            curse(e, MobEffects.WEAKNESS, 6, seen[1]);
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1, .8f + seen[1] * .25f);
            charges--;
            if (charges <= 0) {
                end(30);
            }
        }
    }

    /** After it sprang: the plague passing on, the leech drinking. */
    private void after(ServerLevel level, @Nullable LivingEntity caster, Kind kind) {
        if (sprungAt < 0 || !(victim() instanceof LivingEntity v) || !v.isAlive()) {
            return;
        }
        if (kind == Kind.PLAGUE && (tickCount - sprungAt) % 20 == 0) {
            for (LivingEntity x : SpellUtil.enemiesAround(level, v.position(), PLAGUE_REACH, caster, o -> o != v)) {
                curse(x, MobEffects.POISON, 5, 0);
                curse(x, MobEffects.HUNGER, 5, 0);
                SpellUtil.particles(level, ParticleTypes.SNEEZE, x.getX(), x.getY() + 1, x.getZ(), 6, .3, .3, .3, .02);
            }
            SpellUtil.particles(level, ParticleTypes.SNEEZE, v.getX(), v.getY() + 1.2, v.getZ(), 10, .5, .4, .5, .03);
        }
        if (kind == Kind.LEECH && (tickCount - sprungAt) % 20 == 0 && caster != null && caster.isAlive() && v.hasEffect(MobEffects.WITHER)) {
            caster.heal(1 + power * .5f);
            SpellUtil.particles(level, ParticleTypes.DAMAGE_INDICATOR, v.getX(), v.getY() + 1, v.getZ(), 3, .2, .2, .2, .05);
        }
    }

    /** The cursed web: every two seconds, a new curse on each enemy inside it, in turn. */
    private void web(ServerLevel level, @Nullable LivingEntity caster) {
        if (tickCount == ARM) {
            sound(SoundEvents.SPIDER_AMBIENT, 1.5f, .5f);
            sound(SoundEvents.EVOKER_PREPARE_WOLOLO, 1.2f, .7f);
        }
        if ((tickCount - ARM) % WEB_EVERY != 0) {
            return;
        }
        int round = (tickCount - ARM) / WEB_EVERY;
        boolean any = false;
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), getSize(), caster, x -> Math.abs(x.getY() - getY()) < 4)) {
            curseNumber(e, round + e.getId(), 4);
            SpellUtil.particles(level, ParticleTypes.WITCH, e.getX(), e.getY() + 1, e.getZ(), 8, .3, .5, .3, .02);
            any = true;
        }
        if (any) {
            this.entityData.set(DATA_STRUCK, (int) level.getGameTime());
            sound(SoundEvents.EVOKER_CAST_SPELL, 1.2f, .6f + (round % 4) * .1f);
        }
    }
}
