package com.waylays.arcanes.entity.mend;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * What Mending Magic lays on the one ally it is cast on, following them: a thread of light from the caster that heals
 * while it holds, a healing that echoes, a bud that blooms when they are hurt, a wisp that answers each wound, a cocoon,
 * the promise of a second life; and the flash of the spells that act at once. Its renderer draws the kind; it does the
 * kind's work as it ticks (see also {@link MendEvents}).
 */
public class MendScene extends SpellEffectEntity {
    public enum Kind { PULSE, THREAD, ECHO, BUD, WISP, COCOON, SECOND }

    /** Lifethread: how far the thread reaches before it breaks, how often it heals. */
    public static final double THREAD_REACH = 16;
    public static final int THREAD_EVERY = 10;
    /** Echoing healing: when the echoes come. */
    public static final int ECHO_1 = 40, ECHO_2 = 80;
    /** Mending bud: below which share of their health it blooms. */
    public static final float BUD_AT = .5f;
    /** Guardian wisp: the share of a wound it heals, after how long. */
    public static final float WISP_SHARE = .5f;
    public static final int WISP_DELAY = 10;
    /** Cocoon: how long it holds. */
    public static final int COCOON_TIME = 60;
    /** Burst lingering after a kind is done (for its renderer). */
    public static final int LINGER = 24;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(MendScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(MendScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOUND = SynchedEntityData.defineId(MendScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SOURCE = SynchedEntityData.defineId(MendScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLASH = SynchedEntityData.defineId(MendScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DONE = SynchedEntityData.defineId(MendScene.class, EntityDataSerializers.BOOLEAN);

    /** Wisp: heals due, as {tick, amount}. */
    private final List<float[]> due = new ArrayList<>();
    private float healed;

    public MendScene(EntityType<? extends MendScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} on {@code ally}, from {@code caster}, lasting {@code life} ticks, healing {@code amount}. */
    public static MendScene spawn(Level level, @Nullable LivingEntity caster, LivingEntity ally, Kind kind, int life, float amount) {
        MendScene s = new MendScene(ModEntities.MEND_SCENE.get(), level);
        s.setOwner(caster);
        s.damage = amount;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_BOUND, ally.getId());
        s.entityData.set(DATA_SOURCE, caster == null ? -1 : caster.getId());
        s.moveTo(ally.getX(), ally.getY(), ally.getZ(), 0, 0);
        level.addFreshEntity(s);
        MendEvents.track(s, ally, kind);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_BOUND, -1);
        builder.define(DATA_SOURCE, -1);
        builder.define(DATA_FLASH, -1000);
        builder.define(DATA_DONE, false);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public boolean isDone() {
        return this.entityData.get(DATA_DONE);
    }

    /** Ticks since it last healed (or bloomed, or answered a wound), by the level's clock. */
    public float sinceFlash(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_FLASH);
    }

    @Nullable
    public Entity ally() {
        int id = this.entityData.get(DATA_BOUND);
        return id >= 0 ? level().getEntity(id) : null;
    }

    /** The caster (for the thread's other end). */
    @Nullable
    public Entity source() {
        int id = this.entityData.get(DATA_SOURCE);
        return id >= 0 ? level().getEntity(id) : null;
    }

    /** How much it has healed so far. */
    public float healed() {
        return healed;
    }

    private void flash() {
        this.entityData.set(DATA_FLASH, (int) level().getGameTime());
    }

    /** Ends it, leaving {@code linger} ticks for its last flash. */
    public void finish(int linger) {
        if (isDone()) {
            return;
        }
        this.entityData.set(DATA_DONE, true);
        this.entityData.set(DATA_LIFE, tickCount + linger);
    }

    /** Heals the ally by {@code amount}, with a flash, a chime and hearts. */
    public void heal(LivingEntity ally, float amount, float pitch) {
        if (amount <= 0) {
            return;
        }
        float before = ally.getHealth();
        ally.heal(amount);
        healed += ally.getHealth() - before;
        flash();
        SpellUtil.particles(level(), ParticleTypes.HEART, ally.getX(), ally.getY() + ally.getBbHeight() + .2, ally.getZ(), 2, .3, .1, .3, 0);
        SpellUtil.particles(level(), ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + ally.getBbHeight() * .5, ally.getZ(), 8, .4, .5, .4, 0);
        sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1, pitch);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A wound the ally took (wisp): half of it is healed a moment later. */
    void wounded(float amount) {
        due.add(new float[] {tickCount + WISP_DELAY, amount * WISP_SHARE});
    }

    /** The ally would have died (second life): they live on. */
    void spare(LivingEntity ally) {
        ally.setHealth(ally.getMaxHealth() * .5f);
        ally.removeAllEffects();
        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, true, true));
        ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, true, true));
        healed += ally.getMaxHealth() * .5f;
        flash();
        SpellUtil.particles(level(), ParticleTypes.TOTEM_OF_UNDYING, ally.getX(), ally.getY() + 1, ally.getZ(), 60, .6, .8, .6, .5);
        sound(SoundEvents.TOTEM_USE, 1, 1.2f);
        sound(SoundEvents.BEACON_POWER_SELECT, 1.2f, 1.6f);
        finish(40);
    }

    @Override
    public void tick() {
        Entity bound = ally();
        if (bound != null && bound.isAlive()) {
            setPos(bound.getX(), bound.getY(), bound.getZ());
        }
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > getLife()) {
            discard();
            return;
        }
        if (!(bound instanceof LivingEntity ally) || !ally.isAlive()) {
            if (!isDone()) {
                finish(10);
            }
            return;
        }
        if (isDone()) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        switch (getKind()) {
            case PULSE -> {
            }
            case THREAD -> {
                if (caster == null || !caster.isAlive() || caster.distanceToSqr(ally) > THREAD_REACH * THREAD_REACH) {
                    sound(SoundEvents.CHAIN_BREAK, .8f, 1.6f);
                    finish(10);
                    return;
                }
                if (tickCount % THREAD_EVERY == 0) {
                    heal(ally, damage * THREAD_EVERY / 20f, 1.4f + .02f * (tickCount / THREAD_EVERY));
                }
            }
            case ECHO -> {
                if (tickCount == 1) {
                    heal(ally, damage, 1.2f);
                } else if (tickCount == ECHO_1) {
                    heal(ally, damage * .5f, 1.5f);
                } else if (tickCount == ECHO_2) {
                    heal(ally, damage * .25f, 1.8f);
                    finish(LINGER);
                }
            }
            case BUD -> {
                if (ally.getHealth() < ally.getMaxHealth() * BUD_AT) {
                    heal(ally, damage, 1.0f);
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
                    sound(SoundEvents.AZALEA_LEAVES_BREAK, 1.2f, .8f);
                    finish(LINGER);
                }
            }
            case WISP -> {
                for (var it = due.iterator(); it.hasNext(); ) {
                    float[] d = it.next();
                    if (tickCount >= d[0]) {
                        heal(ally, d[1], 1.7f);
                        it.remove();
                    }
                }
            }
            case COCOON -> {
                ally.setDeltaMovement(0, Math.min(0, ally.getDeltaMovement().y), 0);
                ally.hurtMarked = true;
                if (tickCount % 10 == 0) {
                    heal(ally, damage * 10 / COCOON_TIME, 1.1f + tickCount * .005f);
                }
                if (tickCount >= COCOON_TIME) {
                    sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1, 1.2f);
                    finish(LINGER);
                }
            }
            case SECOND -> {
                if (tickCount % 40 == 0) {
                    SpellUtil.particles(level(), ParticleTypes.END_ROD, ally.getX(), ally.getY() + ally.getBbHeight() + .4, ally.getZ(), 2, .2, .05, .2, .01);
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        MendEvents.untrack(this);
        super.remove(reason);
    }
}
