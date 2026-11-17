package com.waylays.arcanes.entity.acid;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A spell of Acid Magic at work, drawn by {@code AcidSceneRenderer}. Acid eats armour: what it touches is sundered,
 * and the armour worn wears away.
 * <ul>
 * <li>{@link Kind#SPRAY}, Acid Spray: a cone of acid sprayed ahead, eating at all it splashes.</li>
 * <li>{@link Kind#POOL}, Corrosive Pool: a puddle of acid that fizzes and fumes, eating feet and armour.</li>
 * <li>{@link Kind#RAIN}, Acid Rain: a sickly cloud over the spot rains acid that sizzles where it falls.</li>
 * <li>{@link Kind#DISSOLVE}, Dissolve: a bubble of acid closes round an enemy, eats its armour, then melts it in a
 * splash.</li>
 * <li>{@link Kind#TIDE}, Corroding Tide: a wave of acid rolls out in a ring round the caster, corroding everything it
 * passes over.</li>
 * </ul>
 * No acid spell summons anything.
 */
public class AcidScene extends SpellEffectEntity {
    public enum Kind { SPRAY, POOL, RAIN, DISSOLVE, TIDE }

    /** Acid Spray: its reach, its half angle (radians), how long it sprays. */
    public static final float SPRAY_REACH = 6.5f, SPRAY_HALF = .55f;
    public static final int SPRAY_TIME = 10;
    /** Corrosive Pool: its radius, how long it lasts. */
    public static final float POOL_R = 2.8f;
    public static final int POOL_TIME = 120;
    /** Acid Rain: the cloud's radius and height, how long it rains. */
    public static final float RAIN_R = 3.5f, RAIN_HEIGHT = 6;
    public static final int RAIN_TIME = 140;
    /** Dissolve: how long the bubble eats, when it melts. */
    public static final int DISSOLVE_TIME = 70;
    /** Corroding Tide: its reach, how long the wave takes to roll out. */
    public static final float TIDE_R = 12;
    public static final int TIDE_ROLL = 40, TIDE_TIME = 60;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(AcidScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(AcidScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(AcidScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR = SynchedEntityData.defineId(AcidScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(AcidScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_VICTIM = SynchedEntityData.defineId(AcidScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(AcidScene.class, EntityDataSerializers.INT);

    @Nullable
    private AbstractSpell spell;
    private float power = 1;
    private int strikes, eaten;
    private final Set<Integer> hit = new HashSet<>();
    private final Map<Integer, Integer> lastHurt = new HashMap<>();

    public AcidScene(EntityType<? extends AcidScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir} and {@code pitch} (degrees), on {@code victim} (Dissolve). */
    public static AcidScene spawn(Level level, @Nullable LivingEntity caster, @Nullable AbstractSpell spell, Kind kind, Vec3 at, float dir, float pitch,
                                  @Nullable LivingEntity victim, int life, float power) {
        AcidScene s = new AcidScene(ModEntities.ACID_SCENE.get(), level);
        s.setOwner(caster);
        s.spell = spell;
        s.power = power;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_DIR, dir);
        s.entityData.set(DATA_PITCH, pitch);
        s.entityData.set(DATA_VICTIM, victim == null ? -1 : victim.getId());
        float size = switch (kind) {
            case SPRAY -> SPRAY_REACH;
            case POOL -> POOL_R;
            case RAIN -> RAIN_R;
            case DISSOLVE -> victim == null ? 1 : victim.getBbWidth() * .6f + .4f;
            case TIDE -> TIDE_R;
        };
        s.entityData.set(DATA_SIZE, size);
        s.moveTo(at.x, at.y, at.z, dir, 0);
        level.addFreshEntity(s);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_DIR, 0f);
        builder.define(DATA_PITCH, 0f);
        builder.define(DATA_VICTIM, -1);
        builder.define(DATA_STRUCK, -1000);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    /** The way it faces: a unit vector (the spray's aim, pitch included). */
    public Vec3 aim() {
        return Vec3.directionFromRotation(this.entityData.get(DATA_PITCH), this.entityData.get(DATA_DIR));
    }

    @Nullable
    public Entity victim() {
        int id = this.entityData.get(DATA_VICTIM);
        return id >= 0 ? level().getEntity(id) : null;
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    public int strikes() {
        return strikes;
    }

    /** How much armour durability the scene has eaten (read by the tests). */
    public int eaten() {
        return eaten;
    }

    private void struck() {
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private List<LivingEntity> enemies(double radius, Vec3 at) {
        return SpellUtil.enemiesAround(level(), at, radius, getLivingOwner(), x -> true);
    }

    private boolean hurt(LivingEntity e, float amount) {
        LivingEntity caster = getLivingOwner();
        boolean done = spell != null ? DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster))
                : e.hurt(level().damageSources().magic(), amount);
        if (done) {
            strikes++;
        }
        return done;
    }

    /** Acid on {@code e}: its armour sundered for {@code seconds}, every piece it wears worn by {@code wear}. */
    private void corrode(LivingEntity e, int seconds, int amplifier, int wear) {
        MobEffectInstance had = e.getEffect(ModEffects.SUNDERED);
        if (had == null || had.getDuration() < seconds * 20 || had.getAmplifier() < amplifier) {
            e.addEffect(new MobEffectInstance(ModEffects.SUNDERED, seconds * 20, amplifier, false, true, true));
        }
        if (wear <= 0) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack armour = e.getItemBySlot(slot);
            if (!armour.isEmpty() && armour.isDamageableItem()) {
                armour.hurtAndBreak(wear, e, slot);
                eaten += wear;
            }
        }
    }

    private void fizz(double x, double y, double z, int count, double spread) {
        SpellUtil.particles(level(), new DustParticleOptions(new Vector3f(.72f, 1, .16f), 1.1f), x, y, z, count, spread, spread * .5, spread, .02);
        SpellUtil.particles(level(), ParticleTypes.SMOKE, x, y + .2, z, Math.max(1, count / 3), spread * .5, .1, spread * .5, .02);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > getLife()) {
            discard();
            return;
        }
        switch (getKind()) {
            case SPRAY -> spray();
            case POOL -> pool();
            case RAIN -> rain();
            case DISSOLVE -> dissolve();
            case TIDE -> tide();
        }
    }

    // ---------------------------------------------------------------- Acid Spray

    private void spray() {
        Entity caster = getOwner();
        if (caster != null && tickCount <= SPRAY_TIME) {
            setPos(caster.getX(), caster.getEyeY() - .3, caster.getZ());
        }
        if (tickCount == 1) {
            sound(SoundEvents.BREWING_STAND_BREW, 1.6f, .6f);
            sound(SoundEvents.SLIME_SQUISH, 1.2f, .7f);
        }
        if (tickCount > SPRAY_TIME || tickCount % 2 != 0) {
            return;
        }
        Vec3 d = aim();
        for (LivingEntity e : enemies(getSize() + 1, position())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(position());
            double along = to.dot(d);
            if (along <= 0 || along > getSize() || Math.acos(Mth.clamp(to.normalize().dot(d), -1, 1)) > SPRAY_HALF + .15) {
                continue;
            }
            if (hit.add(e.getId())) {
                hurt(e, power * .8f);
                struck();
            }
            corrode(e, 5, 0, 2);
            fizz(e.getX(), e.getY() + 1, e.getZ(), 4, .3);
        }
    }

    // ---------------------------------------------------------------- Corrosive Pool

    private void pool() {
        if (tickCount == 1) {
            sound(SoundEvents.BUCKET_EMPTY, 1.6f, .7f);
            sound(SoundEvents.FIRE_EXTINGUISH, 1, .6f);
        }
        if (tickCount % 6 == 0) {
            float a = level().random.nextFloat() * Mth.TWO_PI, rr = getSize() * Mth.sqrt(level().random.nextFloat());
            fizz(getX() + Mth.cos(a) * rr, getY() + .1, getZ() + Mth.sin(a) * rr, 3, .2);
        }
        if (tickCount % 10 != 0 || tickCount > getLife() - 15) {
            return;
        }
        for (LivingEntity e : enemies(getSize(), position().add(0, .5, 0))) {
            if (Math.abs(e.getY() - getY()) > 1.5) {
                continue;
            }
            corrode(e, 3, 0, 3);
            Integer last = lastHurt.get(e.getId());
            if (last == null || tickCount - last >= 20) {
                lastHurt.put(e.getId(), tickCount);
                hurt(e, power * .3f);
                struck();
                sound(SoundEvents.FIRE_EXTINGUISH, .6f, 1.4f);
            }
        }
    }

    // ---------------------------------------------------------------- Acid Rain

    private void rain() {
        if (tickCount == 1) {
            sound(SoundEvents.WEATHER_RAIN, 1.6f, .7f);
        }
        if (tickCount % 3 == 0) {
            float a = level().random.nextFloat() * Mth.TWO_PI, rr = getSize() * Mth.sqrt(level().random.nextFloat());
            SpellUtil.particles(level(), ParticleTypes.FALLING_DRIPSTONE_WATER, getX() + Mth.cos(a) * rr, getY() + RAIN_HEIGHT - .5, getZ() + Mth.sin(a) * rr, 2,
                    .2, 0, .2, 0);
        }
        if (tickCount % 10 != 0 || tickCount < 15 || tickCount > getLife() - 15) {
            return;
        }
        for (LivingEntity e : enemies(getSize() + .5, position().add(0, 1, 0))) {
            if (e.getY() > getY() + RAIN_HEIGHT || Math.hypot(e.getX() - getX(), e.getZ() - getZ()) > getSize()) {
                continue;
            }
            hurt(e, power * .25f);
            corrode(e, 3, 0, 1);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true, true));
            fizz(e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 3, .3);
            struck();
        }
        if (tickCount % 20 == 0) {
            sound(SoundEvents.FIRE_EXTINGUISH, .5f, 1.6f);
        }
    }

    // ---------------------------------------------------------------- Dissolve

    private void dissolve() {
        Entity v = victim();
        if (!(v instanceof LivingEntity e) || !e.isAlive()) {
            if (tickCount < getLife() - 12) {
                this.entityData.set(DATA_LIFE, tickCount + 12);
            }
            return;
        }
        setPos(e.getX(), e.getY(), e.getZ());
        if (tickCount == 1) {
            sound(SoundEvents.SLIME_SQUISH, 1.6f, .5f);
            sound(SoundEvents.BREWING_STAND_BREW, 1.2f, .5f);
        }
        int melt = getLife() - 12;
        if (tickCount < melt && tickCount % 20 == 0) {
            hurt(e, power * .3f);
            corrode(e, 4, 1, 12);
            fizz(e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 6, .4);
            sound(SoundEvents.FIRE_EXTINGUISH, .8f, 1.2f);
        }
        if (tickCount == melt) {
            struck();
            hurt(e, power * .9f);
            corrode(e, 6, 1, 8);
            fizz(e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 30, .8);
            sound(SoundEvents.SLIME_BLOCK_BREAK, 2, .5f);
            sound(SoundEvents.LAVA_EXTINGUISH, 1.4f, .8f);
        }
    }

    // ---------------------------------------------------------------- Corroding Tide

    private void tide() {
        if (tickCount == 1) {
            sound(SoundEvents.GENERIC_SPLASH, 2, .5f);
            sound(SoundEvents.ELDER_GUARDIAN_CURSE, .8f, 1.8f);
        }
        if (tickCount > TIDE_ROLL) {
            return;
        }
        float front = getSize() * tickCount / (float) TIDE_ROLL;
        for (LivingEntity e : enemies(front + 1, position().add(0, 1, 0))) {
            double d = Math.hypot(e.getX() - getX(), e.getZ() - getZ());
            if (d > front || d < front - 2.5 || !hit.add(e.getId())) {
                continue;
            }
            hurt(e, power * 2);
            corrode(e, 8, 1, 20);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, true, true));
            Vec3 push = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(.8);
            e.setDeltaMovement(push.x, .3, push.z);
            e.hurtMarked = true;
            fizz(e.getX(), e.getY() + 1, e.getZ(), 10, .5);
            struck();
        }
        if (tickCount % 4 == 0) {
            for (int k = 0; k < 10; k++) {
                float a = k * Mth.TWO_PI / 10 + tickCount;
                fizz(getX() + Mth.cos(a) * front, getY() + .3, getZ() + Mth.sin(a) * front, 2, .3);
            }
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
