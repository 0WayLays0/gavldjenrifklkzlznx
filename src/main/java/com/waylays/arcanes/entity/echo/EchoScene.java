package com.waylays.arcanes.entity.echo;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.echo.EchoFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What Echo Magic leaves in the world after its cast: a shriek's rings, the echo left behind by an Echo Step, a patch of
 * sculk tendrils, a sculk sensor listening, resonance bursting, a sonic boom, the Dome of Silence, a shrieker of the
 * Deep Choir, the Requiem. Each is a kind of this one entity: its renderer draws the kind's model, and it does the
 * kind's work as it ticks.
 */
public class EchoScene extends SpellEffectEntity {
    public enum Kind { SHRIEK, STEP, TENDRILS, SENSOR, BURST, BOOM, DOME, SHRIEKER, REQUIEM }

    /** Echo Step: the echo bursts this long after its caster stepped away. */
    public static final int STEP_DELAY = 30;
    /** Sculk Tendrils lash this often (ticks). */
    public static final int LASH = 10;
    /** A sculk sensor hears whatever moves this near, and strikes no more often than {@link #SENSOR_REST} ticks. */
    public static final double SENSOR_RANGE = 7;
    public static final int SENSOR_REST = 30;
    /** A shrieker of the Deep Choir strikes the nearest foe this near, every {@link #SHRIEK_EVERY} ticks. */
    public static final double SHRIEK_RANGE = 16;
    public static final int SHRIEK_EVERY = 25;
    /** The Requiem's walls of sound: the first leaves this long after it begins, the next ones {@link #REQUIEM_STEP} apart. */
    public static final int REQUIEM_FIRST = 30;
    public static final int REQUIEM_STEP = 12;
    public static final int REQUIEM_WAVES = 5;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(EchoScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(EchoScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(EchoScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(EchoScene.class, EntityDataSerializers.VECTOR3);
    /** Where it last struck (relative to it) and when (game tick), for a sensor or a shrieker. */
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(EchoScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(EchoScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ARG = SynchedEntityData.defineId(EchoScene.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;
    /** For the Requiem: how much each level of Resonance bursts for at its end. */
    private float burst;

    public EchoScene(EntityType<? extends EchoScene> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its length, radius or power), lasting
     * {@code life} ticks, striking for {@code damage}.
     */
    public static EchoScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                  float damage) {
        EchoScene s = new EchoScene(ModEntities.ECHO_SCENE.get(), level);
        s.setOwner(caster);
        s.spellId = spell.getSpellId();
        s.damage = damage;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_SIZE, size);
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_DIR, dir);
        s.entityData.set(DATA_STRUCK, -1000);
        s.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(s);
        return s;
    }

    /** A sight only (a shriek's rings, a burst, a boom): it does nothing but show, for as long as its model lasts. */
    public static EchoScene visual(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, float arg) {
        int life = switch (kind) {
            case SHRIEK -> 16;
            case BURST -> 14;
            case BOOM -> 16;
            default -> 20;
        };
        EchoScene s = spawn(level, caster, spell, kind, at, dir, size, life, 0);
        s.entityData.set(DATA_ARG, arg);
        return s;
    }

    public EchoScene arg(float arg) {
        this.entityData.set(DATA_ARG, arg);
        return this;
    }

    public EchoScene burst(float perLevel) {
        this.burst = perLevel;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_DIR, new Vector3f(0, 0, 1));
        builder.define(DATA_TARGET, new Vector3f());
        builder.define(DATA_STRUCK, -1000);
        builder.define(DATA_ARG, 0f);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public Vector3f getDir() {
        return new Vector3f(this.entityData.get(DATA_DIR));
    }

    public Vector3f getTarget() {
        return new Vector3f(this.entityData.get(DATA_TARGET));
    }

    public float getArg() {
        return this.entityData.get(DATA_ARG);
    }

    /** Ticks since it last struck (a sensor, a shrieker); large if it never did. */
    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private void struck(LivingEntity target) {
        Vec3 d = target.position().add(0, target.getBbHeight() * .5, 0).subtract(position());
        this.entityData.set(DATA_TARGET, d.toVector3f());
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife()) {
            discard();
            return;
        }
        AbstractSpell spell = spell();
        LivingEntity caster = getLivingOwner();
        if (spell == null) {
            return;
        }
        switch (getKind()) {
            case STEP -> {
                if (tickCount == STEP_DELAY) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), getSize(), caster, x -> true)) {
                        EchoFx.hit(spell, this, caster, e, damage, 1);
                    }
                    EchoFx.pulse(level, position().add(0, 1, 0), 30, getSize() * .4);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, .8f, 1.6f);
                }
            }
            case TENDRILS -> {
                if (tickCount >= 8 && tickCount <= getLife() - 12 && tickCount % LASH == 0) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position(), getSize() + .5, caster, x -> Math.abs(x.getY() - getY()) < 2)) {
                        e.invulnerableTime = 0;
                        if (EchoFx.hit(spell, this, caster, e, damage, 1)) {
                            e.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, LASH + 4, 0));
                        }
                    }
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 1.2f, .7f);
                }
            }
            case SENSOR -> {
                if (sinceStruck(0) >= SENSOR_REST && tickCount > 8) {
                    LivingEntity heard = null;
                    double best = Double.MAX_VALUE;
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position(), SENSOR_RANGE, caster, x -> true)) {
                        // It hears what moves: walking, falling, fighting
                        boolean moving = e.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || Math.abs(e.getY() - e.yo) > .01 || e.swinging;
                        double d = e.distanceToSqr(this);
                        if (moving && d < best) {
                            best = d;
                            heard = e;
                        }
                    }
                    if (heard != null) {
                        heard.invulnerableTime = 0;
                        EchoFx.hit(spell, this, caster, heard, damage, 1);
                        struck(heard);
                        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.6f, 1);
                        level.playSound(null, heard.getX(), heard.getY(), heard.getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.2f, .7f);
                    }
                }
            }
            case DOME -> {
                if (tickCount % 5 == 0) {
                    float r = getSize();
                    AABB box = getBoundingBox().inflate(r);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && x.distanceToSqr(this) <= r * r)) {
                        if (SpellUtil.isAlly(caster, e)) {
                            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12, 0, false, false, true));
                        } else {
                            e.addEffect(new MobEffectInstance(ModEffects.SILENCE, 12, 0, false, false, true));
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 0, false, false, true));
                            if (tickCount % 20 == 0) {
                                EchoFx.resonate(e, 1);
                            }
                        }
                    }
                    // No shot carries through the silence: what enemies loose inside it falls short
                    for (Projectile shot : level.getEntitiesOfClass(Projectile.class, box, x -> x.distanceToSqr(this) <= r * r)) {
                        Entity by = shot.getOwner();
                        if (by != null && (by == caster || SpellUtil.isAlly(caster, by))) {
                            continue;
                        }
                        shot.setDeltaMovement(shot.getDeltaMovement().scale(.2));
                        shot.hurtMarked = true;
                    }
                }
            }
            case SHRIEKER -> {
                int t = tickCount - (int) getArg();
                if (t > 0 && t % SHRIEK_EVERY == 0 && tickCount < getLife() - 12) {
                    LivingEntity foe = nearest(level, caster, SHRIEK_RANGE);
                    if (foe != null) {
                        foe.invulnerableTime = 0;
                        EchoFx.hit(spell, this, caster, foe, damage, 2);
                        Vec3 away = foe.position().subtract(position()).multiply(1, 0, 1);
                        if (away.lengthSqr() > 1.0E-4) {
                            foe.setDeltaMovement(foe.getDeltaMovement().add(away.normalize().scale(.5)).add(0, .2, 0));
                            foe.hurtMarked = true;
                        }
                        struck(foe);
                        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.6f, 1.1f);
                        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.3f);
                    }
                }
            }
            case REQUIEM -> requiem(level, spell, caster);
            default -> {
            }
        }
    }

    @Nullable
    private LivingEntity nearest(ServerLevel level, @Nullable LivingEntity caster, double range) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), range, caster, x -> true)) {
            double d = e.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private void requiem(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        int t = tickCount - REQUIEM_FIRST;
        if (t < 0 || t % REQUIEM_STEP != 0 || t / REQUIEM_STEP >= REQUIEM_WAVES) {
            if (tickCount % 10 == 0) {
                EchoFx.pulse(level, position().add(0, 5.5, 0), 6, .8);
            }
            return;
        }
        int wave = t / REQUIEM_STEP;
        List<LivingEntity> caught = SpellUtil.enemiesAround(level, position(), getSize(), caster, x -> Math.abs(x.getY() - getY()) < 5);
        for (LivingEntity e : caught) {
            e.invulnerableTime = 0;
            EchoFx.hit(spell, this, caster, e, damage, 1);
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 3f, .7f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2f, .6f + .1f * wave);
        SpellUtil.cameraShake(level, position(), 10, getSize() + 6);
        if (wave == REQUIEM_WAVES - 1) {
            // The last beat: every echo still ringing bursts, and the field falls silent
            for (LivingEntity e : SpellUtil.enemiesAround(level, position(), getSize() + 2, caster, x -> Math.abs(x.getY() - getY()) < 6)) {
                EchoFx.burst(level, spell, this, caster, e, burst);
                e.addEffect(new MobEffectInstance(ModEffects.SILENCE, 80, 0));
                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 3f, .8f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (spellId != null) {
            tag.putString("Spell", spellId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spellId = tag.contains("Spell") ? tag.getString("Spell") : null;
    }
}
