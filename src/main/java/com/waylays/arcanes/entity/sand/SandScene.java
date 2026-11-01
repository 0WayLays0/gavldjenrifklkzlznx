package com.waylays.arcanes.entity.sand;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.sand.SandFx;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * What Sand Magic leaves in the world after its cast: a swarm of scarabs, a hand of sand, a veil of sand round its
 * caster, an hourglass, a serpent of sand, the lens of the sun, a fall of sand, a djinn, the simoom. Each is a kind of
 * this one entity: its renderer draws the kind's model, and it does the kind's work as it ticks.
 */
public class SandScene extends SpellEffectEntity {
    public enum Kind { SWARM, GRASP, VEIL, HOURGLASS, SERPENT, LENS, SANDFALL, DJINN, SIMOOM }

    /** The scarabs fly this fast (blocks a tick), seek prey this far off and bite every {@link #BITE_EVERY} ticks. */
    public static final double SWARM_SPEED = .38, SWARM_RANGE = 18;
    public static final int BITE_EVERY = 10;
    /** The hand of sand rises for this long, and crushes this long before its end. */
    public static final int GRASP_RISE = 10, GRASP_CRUSH = 12;
    /** The hourglass hangs this high over its middle. */
    public static final float HOURGLASS_HEIGHT = 3.2f;
    /** The serpent slides this fast, seeks prey this far off, bites this many times, no oftener than every {@link #SERPENT_REST} ticks. */
    public static final double SERPENT_SPEED = .34, SERPENT_RANGE = 20;
    public static final int SERPENT_BITES = 3, SERPENT_REST = 18;
    /** The lens hangs this high over the spot it burns, which slides toward the nearest foe this far round it, this fast. */
    public static final float LENS_HEIGHT = 7;
    public static final double LENS_RANGE = 12, LENS_SPEED = .2;
    public static final int BURN_EVERY = 10;
    /** The sand falls from this high, every {@link #POUR_EVERY} ticks it buries what is under it. */
    public static final float SANDFALL_HEIGHT = 10;
    public static final int POUR_EVERY = 10;
    /** The djinn looses a bolt of sand at the nearest foe this far off, every {@link #DJINN_EVERY} ticks. */
    public static final double DJINN_RANGE = 16;
    public static final int DJINN_EVERY = 30;
    /** The simoom crawls toward where its caster looks, this fast. */
    public static final double SIMOOM_SPEED = .16;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.VECTOR3);
    /** Where it last struck (relative to it) and when (game tick): a bite, a bolt, a turned shot. */
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    /** What it follows or holds (a veil or a djinn its caster, a hand its victim). */
    private int bound = -1;
    private int bites;
    private int lastBite = -1000;

    public SandScene(EntityType<? extends SandScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its radius or reach), lasting {@code life} ticks, striking for {@code damage}. */
    public static SandScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                  float damage) {
        SandScene s = new SandScene(ModEntities.SAND_SCENE.get(), level);
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

    /** Makes it follow or hold {@code e} (a veil or a djinn its caster, a hand its victim). */
    public SandScene follow(Entity e) {
        this.bound = e.getId();
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

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    /** Server: how many times the serpent has bitten. */
    public int bites() {
        return bites;
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private boolean strike(AbstractSpell spell, @Nullable LivingEntity caster, LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        return DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster));
    }

    /** Shows the clients where it struck, and when. */
    private void struck(Vec3 at) {
        this.entityData.set(DATA_TARGET, at.subtract(position()).toVector3f());
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
    }

    @Nullable
    private LivingEntity nearest(ServerLevel level, Vec3 from, double range, @Nullable LivingEntity caster) {
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, from, range, caster, x -> true);
        return foes.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
    }

    /** {@code pos} slid {@code step} toward {@code to} over the ground (kept on it, never through a wall). */
    private Vec3 slide(ServerLevel level, Vec3 to, double step) {
        Vec3 d = to.subtract(position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1.0E-4) {
            return position();
        }
        Vec3 next = position().add(d.normalize().scale(Math.min(step, d.length())));
        Vec3 ground = SpellUtil.groundAt(level, next, 2, 1);
        return Math.abs(ground.y - next.y) <= 2 ? ground : position();
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
        AbstractSpell spell = spell();
        LivingEntity caster = getLivingOwner();
        if (spell == null) {
            return;
        }
        float size = getSize();
        switch (getKind()) {
            case SWARM -> swarm(level, spell, caster, size);
            case GRASP -> {
                // Holding its victim if it has one (it follows it), else rising where it was called
                Entity held = bound >= 0 ? level.getEntity(bound) : null;
                if (held instanceof LivingEntity victim && victim.isAlive()) {
                    setPos(victim.getX(), victim.getY(), victim.getZ());
                    if (tickCount < getLife() - GRASP_CRUSH + 4) {
                        victim.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, 5, 0, false, false, true));
                        victim.setDeltaMovement(victim.getDeltaMovement().multiply(0, 1, 0));
                        victim.hurtMarked = true;
                    }
                    if (tickCount == GRASP_RISE) {
                        strike(spell, caster, victim, damage * .3f);
                    }
                } else if (bound >= 0 && tickCount < getLife() - GRASP_CRUSH) {
                    // Its victim is gone: the hand closes on nothing
                    this.entityData.set(DATA_LIFE, tickCount + GRASP_CRUSH);
                }
                if (tickCount == GRASP_RISE) {
                    SandFx.sand(level, position().add(0, .5, 0), 24, .7);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 2f, .5f);
                }
                if (tickCount == getLife() - GRASP_CRUSH) {
                    // The fist closes on whatever is in it
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size, caster, x -> Math.abs(x.getY() - getY()) < 2.5)) {
                        strike(spell, caster, e, damage);
                    }
                    SandFx.sand(level, position().add(0, 1, 0), 40, .8);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 2f, .5f);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, .6f, .5f);
                }
            }
            case VEIL -> {
                Entity follow = bound >= 0 ? level.getEntity(bound) : null;
                if (follow != null && follow.isAlive()) {
                    setPos(follow.getX(), follow.getY(), follow.getZ());
                }
                if (follow instanceof LivingEntity wearer && tickCount % 10 == 0) {
                    wearer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 0, false, false, true));
                }
                if (WindFx.deflect(level, getBoundingBox().inflate(size, 2, size).move(0, 1, 0), position().add(0, 1, 0), caster, .5) > 0) {
                    struck(position().add(0, 1.2, 0));
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_HIT, SoundSource.PLAYERS, 1.5f, 1.2f);
                }
                if (tickCount % 10 == 0) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size + .6, caster, x -> true)) {
                        strike(spell, caster, e, damage);
                    }
                }
                if (tickCount % 16 == 0) {
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_STEP, SoundSource.PLAYERS, 1.2f, .7f);
                }
            }
            case HOURGLASS -> {
                int shatter = getLife() - 12;
                float run = Mth.clamp((float) tickCount / shatter, 0, 1);
                if (tickCount < shatter && tickCount % 5 == 0) {
                    // The more sand has run, the slower they are
                    int amp = Math.min(3, (int) (run * 4));
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size, caster, x -> Math.abs(x.getY() - getY()) < 3)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, amp, false, false, true));
                        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 12, amp, false, false, true));
                    }
                }
                if (tickCount % 20 == 0 && tickCount < shatter) {
                    level.playSound(null, getX(), getY() + HOURGLASS_HEIGHT, getZ(), SoundEvents.SAND_STEP, SoundSource.PLAYERS, .8f, 1.6f);
                }
                if (tickCount == shatter) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size, caster, x -> Math.abs(x.getY() - getY()) < 3)) {
                        strike(spell, caster, e, damage);
                    }
                    SandFx.glass(level, position().add(0, HOURGLASS_HEIGHT, 0), 50, 1.2);
                    SandFx.sand(level, position().add(0, 1, 0), 40, size * .5);
                    level.playSound(null, getX(), getY() + HOURGLASS_HEIGHT, getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.5f, .6f);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.2f, .5f);
                }
            }
            case SERPENT -> serpent(level, spell, caster, size);
            case LENS -> {
                if (tickCount % 2 == 0 && tickCount < getLife() - 10) {
                    LivingEntity prey = nearest(level, position(), LENS_RANGE, caster);
                    if (prey != null) {
                        setPos(slide(level, prey.position(), LENS_SPEED * 2));
                    }
                }
                if (tickCount > 12 && tickCount < getLife() - 10 && tickCount % BURN_EVERY == 0) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size, caster, x -> Math.abs(x.getY() - getY()) < 2.5)) {
                        if (strike(spell, caster, e, damage)) {
                            e.igniteForSeconds(3);
                        }
                    }
                    SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.FLAME, getX(), getY() + .2, getZ(), 8, size * .4, .1, size * .4, .02);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1.5f, 1.4f);
                }
            }
            case SANDFALL -> {
                if (tickCount > 6 && tickCount < getLife() - 14 && tickCount % POUR_EVERY == 0) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size, caster, x -> Math.abs(x.getY() - getY()) < 3)) {
                        if (strike(spell, caster, e, damage)) {
                            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false, true));
                            e.setDeltaMovement(e.getDeltaMovement().multiply(.4, 0, .4).add(0, -.3, 0));
                            e.hurtMarked = true;
                        }
                    }
                    SandFx.sand(level, position().add(0, .5, 0), 20, size * .6);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_FALL, SoundSource.PLAYERS, 2f, .6f);
                }
            }
            case DJINN -> {
                Entity follow = bound >= 0 ? level.getEntity(bound) : null;
                if (follow != null && follow.isAlive()) {
                    // Hovering at its master's shoulder, a little behind
                    float yaw = follow.getYRot() * Mth.DEG_TO_RAD;
                    Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw)).scale(1.4);
                    Vec3 side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(1.3);
                    setPos(follow.position().add(back).add(side));
                    // It faces the way its master looks
                    Vec3 look = follow.getLookAngle().multiply(1, 0, 1);
                    if (look.lengthSqr() > 1.0E-4) {
                        this.entityData.set(DATA_DIR, look.normalize().toVector3f());
                    }
                }
                if (tickCount > 14 && tickCount < getLife() - 10 && tickCount % DJINN_EVERY == 0) {
                    Vec3 from = position().add(0, 2.4, 0);
                    LivingEntity prey = nearest(level, from, DJINN_RANGE, caster);
                    if (prey != null && level.clip(new ClipContext(from, prey.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                            .getType() == HitResult.Type.MISS) {
                        strike(spell, caster, prey, damage);
                        prey.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, true));
                        Vec3 hit = prey.position().add(0, prey.getBbHeight() * .5, 0);
                        struck(hit);
                        SandFx.sand(level, hit, 16, .3);
                        level.playSound(null, from.x, from.y, from.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, 1.4f);
                        level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, .6f, 1.6f);
                    }
                }
            }
            case SIMOOM -> simoom(level, spell, caster, size);
        }
    }

    private void swarm(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float size) {
        LivingEntity prey = nearest(level, position(), SWARM_RANGE, caster);
        if (prey != null && tickCount < getLife() - 10) {
            Vec3 to = prey.position().add(0, prey.getBbHeight() * .6, 0).subtract(0, .5, 0);
            Vec3 d = to.subtract(position());
            if (d.lengthSqr() > .04) {
                setPos(position().add(d.normalize().scale(Math.min(SWARM_SPEED, d.length()))));
            }
            if (tickCount % BITE_EVERY == 0 && position().distanceTo(to) < size + .8) {
                if (strike(spell, caster, prey, damage)) {
                    struck(prey.position().add(0, prey.getBbHeight() * .6, 0));
                }
            }
        }
        if (tickCount % 8 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEEHIVE_WORK, SoundSource.PLAYERS, .8f, .6f);
        }
    }

    private void serpent(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float size) {
        if (tickCount >= getLife() - 12 || bites >= SERPENT_BITES) {
            if (bites >= SERPENT_BITES && tickCount < getLife() - 12) {
                this.entityData.set(DATA_LIFE, tickCount + 12);
            }
            return;
        }
        LivingEntity prey = nearest(level, position(), SERPENT_RANGE, caster);
        Vector3f d = getDir();
        Vec3 heading = new Vec3(d.x, 0, d.z);
        if (prey != null) {
            Vec3 to = prey.position().subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 1.0E-4) {
                // It turns toward its prey, a little each tick
                Vec3 want = to.normalize();
                heading = heading.lengthSqr() < 1.0E-4 ? want : heading.normalize().scale(.8).add(want.scale(.2)).normalize();
                this.entityData.set(DATA_DIR, heading.toVector3f());
            }
            if (to.length() < 1.6 + size && tickCount - lastBite >= SERPENT_REST) {
                lastBite = tickCount;
                bites++;
                if (strike(spell, caster, prey, damage)) {
                    prey.setDeltaMovement(prey.getDeltaMovement().add(heading.scale(.4)).add(0, .45, 0));
                    prey.hurtMarked = true;
                }
                struck(prey.position().add(0, prey.getBbHeight() * .6, 0));
                SandFx.sand(level, prey.position().add(0, .5, 0), 24, .5);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.4f, .7f);
                return;
            }
        }
        if (heading.lengthSqr() > 1.0E-4) {
            setPos(slide(level, position().add(heading.normalize().scale(4)), SERPENT_SPEED));
        }
        if (tickCount % 4 == 0) {
            SandFx.sand(level, position().add(0, .1, 0), 4, .4);
        }
        if (tickCount % 12 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_STEP, SoundSource.PLAYERS, 1.2f, .5f);
        }
    }

    private void simoom(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        if (caster != null && caster.isAlive() && tickCount < getLife() - 16 && tickCount % 2 == 0) {
            // It crawls toward the ground its caster looks at
            Vec3 aim = SpellUtil.aimedGround(level, caster, 32);
            setPos(slide(level, aim, SIMOOM_SPEED * 2));
        }
        if (tickCount < getLife() - 16) {
            Vec3 c = position();
            for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 2, 0), radius + 2, caster, x -> x.getY() < c.y + 12)) {
                // Drawn in and whirled round, lifted off its feet
                Vec3 in = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
                double dist = in.length();
                if (dist > .5) {
                    Vec3 round = new Vec3(-in.z, 0, in.x).scale(1 / dist);
                    e.setDeltaMovement(e.getDeltaMovement().scale(.6).add(in.scale(.1 / dist)).add(round.scale(.3)).add(0, dist < radius ? .12 : 0, 0));
                    e.hurtMarked = true;
                }
                if (tickCount % 10 == 0 && strike(spell, caster, e, damage)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
                }
            }
            if (tickCount % 4 == 0) {
                SandFx.sand(level, c.add(0, .3, 0), 10, radius * .6);
            }
            if (tickCount % 20 == 0) {
                level.playSound(null, c.x, c.y, c.z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 2.5f, .5f);
            }
            // Now and then a bolt of lightning in the storm leaves a scar of glass
            if (tickCount % 25 == 12) {
                float a = random.nextFloat() * Mth.TWO_PI;
                struck(c.add(Mth.cos(a) * radius * .7, 0, Mth.sin(a) * radius * .7));
                level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, .8f, 1.6f);
            }
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
