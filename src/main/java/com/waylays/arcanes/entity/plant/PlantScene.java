package com.waylays.arcanes.entity.plant;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A spell of Plant Magic at work, drawn by {@code PlantSceneRenderer}:
 * <ul>
 * <li>{@link Kind#BLOOM}, Thorn Bloom: a bud swells from the ground, opens into a great flower and flings its thorns
 * all round, making those they pierce bleed.</li>
 * <li>{@link Kind#POLLEN}, Pollen Cloud: a cloud of golden pollen; enemies in it sneeze and weaken, the caster and their
 * allies heal.</li>
 * <li>{@link Kind#SEEDS}, Seed Volley: seeds flung ahead in a fan plant themselves, sprout into puffballs and burst into
 * poisonous spores.</li>
 * <li>{@link Kind#SUN}, Photosynthesis: a shaft of sunlight falls on the caster, leaves whirling up in it; the caster
 * and their allies close by heal, twice as much under the open sky by day.</li>
 * <li>{@link Kind#MAW}, Verdant Maw: a huge carnivorous flower bursts from the ground under the enemy, its jaws snapping
 * shut; it chews and holds those inside, feeding its caster.</li>
 * </ul>
 * No plant spell summons anything: the flowers are the spells themselves, and they wither once done.
 */
public class PlantScene extends SpellEffectEntity {
    public enum Kind { BLOOM, POLLEN, SEEDS, SUN, MAW }

    /** Thorn Bloom: the bud's growing, when it opens, how far its thorns fly. */
    public static final int BLOOM_OPEN = 24, BLOOM_TIME = 60;
    public static final float BLOOM_R = 4.5f;
    /** Pollen Cloud: its radius, how long it hangs. */
    public static final float POLLEN_R = 4.5f;
    public static final int POLLEN_TIME = 160;
    /** Seed Volley: how many seeds, their flight, when they burst, how far and wide. */
    public static final int SEEDS = 7, SEED_FLIGHT = 10, SEED_BURST = 34, SEEDS_TIME = 50;
    public static final float SEED_RANGE = 7, SEED_R = 1.8f;
    /** Photosynthesis: how long it shines, how far it reaches. */
    public static final int SUN_TIME = 200;
    public static final float SUN_R = 3.5f;
    /** Verdant Maw: when the jaws close, how long it chews, its radius. */
    public static final int MAW_SNAP = 20, MAW_TIME = 80;
    public static final float MAW_R = 2.4f;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(PlantScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(PlantScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(PlantScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR = SynchedEntityData.defineId(PlantScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(PlantScene.class, EntityDataSerializers.INT);

    @Nullable
    private AbstractSpell spell;
    private float power = 1;
    private int strikes;
    private float healed;
    private final Set<Integer> held = new HashSet<>();

    public PlantScene(EntityType<? extends PlantScene> entityType, Level level) {
        super(entityType, level);
    }

    public static PlantScene spawn(Level level, @Nullable LivingEntity caster, @Nullable AbstractSpell spell, Kind kind, Vec3 at, float dir, int life, float power) {
        PlantScene s = new PlantScene(ModEntities.PLANT_SCENE.get(), level);
        s.setOwner(caster);
        s.spell = spell;
        s.power = power;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_DIR, dir);
        s.entityData.set(DATA_SIZE, switch (kind) {
            case BLOOM -> BLOOM_R;
            case POLLEN -> POLLEN_R;
            case SEEDS -> SEED_RANGE;
            case SUN -> SUN_R;
            case MAW -> MAW_R;
        });
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

    public float getDir() {
        return this.entityData.get(DATA_DIR);
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    public int strikes() {
        return strikes;
    }

    public float healed() {
        return healed;
    }

    /**
     * Where the {@code i}th seed of a volley lands, from the scene's spot (on the ground, the scene's height): a fan
     * ahead, the same on both sides.
     */
    public Vec3 seedSpot(int i) {
        float yaw = getDir() * Mth.DEG_TO_RAD + (i - (SEEDS - 1) / 2f) * .16f;
        float reach = getSize() * (.7f + .3f * ((i * 37 % 7) / 6f));
        return new Vec3(-Mth.sin(yaw) * reach, 0, Mth.cos(yaw) * reach);
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

    /** The caster and those friendly to them, round {@code at}. */
    private List<LivingEntity> friends(double radius, Vec3 at) {
        LivingEntity caster = getLivingOwner();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
            if (e.isAlive() && e.distanceToSqr(at) <= radius * radius && (e == caster || caster != null && SpellUtil.isAlly(caster, e))) {
                out.add(e);
            }
        }
        return out;
    }

    private boolean hurt(LivingEntity e, float amount) {
        LivingEntity caster = getLivingOwner();
        boolean done = spell != null ? DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster))
                : e.hurt(level().damageSources().cactus(), amount);
        if (done) {
            strikes++;
        }
        return done;
    }

    private void heal(LivingEntity e, float amount) {
        float before = e.getHealth();
        e.heal(amount);
        healed += e.getHealth() - before;
    }

    private void effect(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amplifier) {
        MobEffectInstance had = e.getEffect(effect);
        if (had == null || had.getDuration() < ticks || had.getAmplifier() < amplifier) {
            e.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, true, true));
        }
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
            case BLOOM -> bloom();
            case POLLEN -> pollen();
            case SEEDS -> seeds();
            case SUN -> sun();
            case MAW -> maw();
        }
    }

    // ---------------------------------------------------------------- Thorn Bloom

    private void bloom() {
        if (tickCount == 1) {
            sound(SoundEvents.BIG_DRIPLEAF_TILT_UP, 1.4f, .6f);
        }
        if (tickCount < BLOOM_OPEN && tickCount % 6 == 0) {
            SpellUtil.particles(level(), ParticleTypes.HAPPY_VILLAGER, getX(), getY() + .5, getZ(), 4, .4, .3, .4, .02);
        }
        if (tickCount == BLOOM_OPEN) {
            struck();
            sound(SoundEvents.SPORE_BLOSSOM_BREAK, 2, .7f);
            sound(SoundEvents.SWEET_BERRY_BUSH_BREAK, 2, .6f);
            for (LivingEntity e : enemies(getSize(), position().add(0, .8, 0))) {
                hurt(e, power * .9f);
                effect(e, ModEffects.BLEEDING, 80, 0);
            }
            SpellUtil.particles(level(), ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1, getZ(), 30, getSize() * .4, .4, getSize() * .4, .05);
        }
    }

    // ---------------------------------------------------------------- Pollen Cloud

    private void pollen() {
        if (tickCount == 1) {
            sound(SoundEvents.AZALEA_LEAVES_PLACE, 1.6f, .8f);
            sound(SoundEvents.BEEHIVE_WORK, 1.2f, 1.2f);
        }
        if (tickCount % 5 == 0) {
            SpellUtil.particles(level(), ParticleTypes.FALLING_SPORE_BLOSSOM, getX(), getY() + 2.5, getZ(), 6, getSize() * .6, .5, getSize() * .6, 0);
        }
        if (tickCount % 20 != 10 || tickCount > getLife() - 10) {
            return;
        }
        Vec3 c = position().add(0, 1, 0);
        for (LivingEntity e : enemies(getSize(), c)) {
            effect(e, MobEffects.WEAKNESS, 40, 0);
            effect(e, MobEffects.CONFUSION, 80, 0);
            hurt(e, power * .2f);
            SpellUtil.particles(level(), ParticleTypes.SNEEZE, e.getX(), e.getEyeY(), e.getZ(), 4, .2, .1, .2, .02);
        }
        for (LivingEntity f : friends(getSize(), c)) {
            effect(f, MobEffects.REGENERATION, 40, 0);
            heal(f, power * .15f);
        }
    }

    // ---------------------------------------------------------------- Seed Volley

    private void seeds() {
        if (tickCount == 1) {
            sound(SoundEvents.CROP_PLANTED, 1.6f, .8f);
        }
        if (tickCount == SEED_FLIGHT) {
            sound(SoundEvents.CAVE_VINES_PLACE, 1.4f, 1);
        }
        if (tickCount != SEED_BURST) {
            return;
        }
        struck();
        sound(SoundEvents.SNIFFER_EGG_CRACK, 2, 1.4f);
        sound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.4f, .8f);
        Set<Integer> hit = new HashSet<>();
        for (int i = 0; i < SEEDS; i++) {
            Vec3 at = position().add(seedSpot(i));
            SpellUtil.particles(level(), ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + .4, at.z, 12, .5, .3, .5, .02);
            for (LivingEntity e : enemies(SEED_R, at.add(0, .5, 0))) {
                if (hit.add(e.getId())) {
                    hurt(e, power * .6f);
                }
                effect(e, MobEffects.POISON, 80, 0);
            }
        }
    }

    // ---------------------------------------------------------------- Photosynthesis

    private boolean underSun() {
        return level().isDay() && level().canSeeSky(blockPosition().above());
    }

    private void sun() {
        Entity caster = getOwner();
        if (!(caster instanceof LivingEntity c) || !c.isAlive()) {
            discard();
            return;
        }
        setPos(c.getX(), c.getY(), c.getZ());
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_ACTIVATE, 1.2f, 1.6f);
        }
        if (tickCount % 20 != 0) {
            return;
        }
        float amount = power * .4f * (underSun() ? 2 : 1);
        for (LivingEntity f : friends(getSize(), position().add(0, 1, 0))) {
            heal(f, amount);
            SpellUtil.particles(level(), ParticleTypes.HAPPY_VILLAGER, f.getX(), f.getY() + 1, f.getZ(), 5, .3, .4, .3, .02);
        }
        sound(SoundEvents.AMETHYST_BLOCK_CHIME, .8f, 1.6f);
    }

    // ---------------------------------------------------------------- Verdant Maw

    private void maw() {
        float r = getSize();
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.ROOTED_DIRT_BREAK, 2, .5f);
            sound(SoundEvents.BIG_DRIPLEAF_TILT_DOWN, 2, .5f);
        }
        if (tickCount < MAW_SNAP) {
            if (tickCount % 4 == 0) {
                SpellUtil.particles(level(), ParticleTypes.HAPPY_VILLAGER, c.x, c.y + .3, c.z, 6, r * .5, .2, r * .5, .02);
            }
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (tickCount == MAW_SNAP) {
            struck();
            sound(SoundEvents.EVOKER_FANGS_ATTACK, 2, .5f);
            sound(SoundEvents.SNIFFER_EAT, 2, .6f);
            SpellUtil.cameraShake(level(), c, 10, 8);
            float dealt = 0;
            for (LivingEntity e : enemies(r, c.add(0, 1, 0))) {
                float before = e.getHealth();
                hurt(e, power * 2.6f);
                dealt += Math.max(0, before - e.getHealth());
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                held.add(e.getId());
            }
            if (caster != null && dealt > 0) {
                heal(caster, dealt * .3f);
            }
            return;
        }
        // Chewing: those it caught are held at its heart and hurt each second, feeding its caster
        if (tickCount < getLife() - 15) {
            for (int id : held) {
                if (level().getEntity(id) instanceof LivingEntity e && e.isAlive()) {
                    Vec3 back = c.subtract(e.position()).multiply(1, 0, 1);
                    if (back.length() > .3) {
                        Vec3 pull = back.normalize().scale(.25);
                        e.setDeltaMovement(pull.x, Math.min(e.getDeltaMovement().y, 0), pull.z);
                        e.hurtMarked = true;
                    }
                    if (tickCount % 20 == 0) {
                        float before = e.getHealth();
                        hurt(e, power * .3f);
                        if (caster != null) {
                            heal(caster, Math.max(0, before - e.getHealth()) * .3f);
                        }
                    }
                }
            }
            if (tickCount % 10 == 0) {
                sound(SoundEvents.SNIFFER_EAT, 1.2f, .5f + level().random.nextFloat() * .2f);
            }
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
