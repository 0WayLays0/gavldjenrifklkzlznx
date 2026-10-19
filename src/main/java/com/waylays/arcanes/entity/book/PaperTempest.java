package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A tornado of paper (Paper Tempest): a funnel of pages taller than a house that its caster steers with their eyes. It
 * drags the enemies in and spins them round, slashes them twice a second and blinds them, and swallows the arrows
 * and small projectiles that fly into it. When it blows out it flings everything away.
 */
public class PaperTempest extends SpellEffectEntity {
    public static final float HEIGHT = 7f;
    public static final int FORM_TICKS = 15;
    public static final int FADE_TICKS = 12;
    public static final int CUT_INTERVAL = 10;
    /** How fast the caster can move it, in blocks per tick. */
    public static final double STEER_SPEED = .2;
    public static final double STEER_RANGE = 32;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(PaperTempest.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(PaperTempest.class, EntityDataSerializers.INT);

    public PaperTempest(EntityType<? extends PaperTempest> entityType, Level level) {
        super(entityType, level);
    }

    public PaperTempest(Level level, LivingEntity owner, Vec3 at, float radius, int duration, float damage) {
        this(ModEntities.PAPER_TEMPEST.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.entityData.set(DATA_RADIUS, radius);
        this.entityData.set(DATA_DURATION, duration);
        moveTo(at.x, at.y, at.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 4f);
        this.entityData.define(DATA_DURATION, 120);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        if (tickCount >= getDuration()) {
            blowOut();
            return;
        }
        steer();
        if (tickCount == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.5f, .7f);
        }
        if (tickCount % 30 == 1) {
            level().playSound(null, getX(), getY() + 2, getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.2f, .55f);
        }
        if (tickCount % 4 == 0) {
            level().playSound(null, getX(), getY() + 2, getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.5f, .6f + random.nextFloat() * .8f);
        }
        if (tickCount < FORM_TICKS / 2) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        float radius = getRadius();
        Vec3 center = position();
        AABB area = new AABB(center.x - radius - 2, center.y - 1, center.z - radius - 2, center.x + radius + 2, center.y + HEIGHT, center.z + radius + 2);
        boolean cut = tickCount % CUT_INTERVAL == 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && !SpellUtil.isAlly(owner, e))) {
            Vec3 offset = new Vec3(e.getX() - center.x, 0, e.getZ() - center.z);
            double distance = offset.length();
            if (distance > radius + 2) {
                continue;
            }
            whirl(e, offset, distance, radius);
            if (cut && distance <= radius) {
                // Many small cuts: each one goes through hurt immunity
                e.invulnerableTime = 0;
                if (DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.PAPER_TEMPEST.get(), this, owner))) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
                    Vec3 c = e.getBoundingBox().getCenter();
                    SpellUtil.particles(level(), ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, .3, .3, .3, 0);
                    SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 3, .3, .4, .3, .2);
                }
            }
        }
        // Arrows and other small projectiles get caught in the wind and spin with the pages
        for (Projectile projectile : level().getEntitiesOfClass(Projectile.class, area.deflate(2, 0, 2))) {
            Entity shooter = projectile.getOwner();
            if (shooter != null && (shooter == owner || SpellUtil.isAlly(owner, shooter))) {
                continue;
            }
            Vec3 offset = new Vec3(projectile.getX() - center.x, 0, projectile.getZ() - center.z);
            if (offset.lengthSqr() > radius * radius) {
                continue;
            }
            Vec3 tangent = new Vec3(-offset.z, 0, offset.x).normalize();
            projectile.setDeltaMovement(tangent.scale(.5).add(0, .06, 0));
            projectile.hasImpulse = true;
        }
    }

    /** Drawn in, spun round the funnel and lifted a little off the ground. */
    private void whirl(LivingEntity e, Vec3 offset, double distance, float radius) {
        if (distance < 1.0E-3) {
            offset = new Vec3(1, 0, 0);
            distance = 1;
        }
        Vec3 out = offset.scale(1 / distance);
        Vec3 tangent = new Vec3(-out.z, 0, out.x);
        double inward = distance > radius * .45 ? .07 : -.02;
        double lift = e.getY() - getY() < 2.2 ? .07 : -.02;
        Vec3 push = tangent.scale(.09).subtract(out.scale(inward)).add(0, lift, 0);
        Vec3 v = e.getDeltaMovement();
        e.setDeltaMovement(v.x * .8 + push.x, Math.max(v.y, -.1) * .8 + push.y, v.z * .8 + push.z);
        e.hurtMarked = true;
        e.fallDistance = 0;
    }

    /** The caster steers it with their eyes; a mob sends it after its target. */
    private void steer() {
        LivingEntity owner = getLivingOwner();
        if (owner == null || owner.distanceToSqr(this) > STEER_RANGE * STEER_RANGE * 2.25) {
            return;
        }
        Vec3 wanted = null;
        if (owner instanceof Player) {
            wanted = Utils.getTargetBlock(level(), owner, ClipContext.Fluid.NONE, STEER_RANGE).getLocation();
        } else if (owner instanceof Mob mob && mob.getTarget() != null) {
            wanted = mob.getTarget().position();
        }
        if (wanted == null) {
            return;
        }
        Vec3 flat = new Vec3(wanted.x - getX(), 0, wanted.z - getZ());
        double distance = flat.length();
        if (distance < .3) {
            return;
        }
        Vec3 step = flat.scale(Math.min(STEER_SPEED, distance) / distance);
        // Hugs the ground: climbs steps and slopes, drops into hollows
        Vec3 next = SpellUtil.groundAt(level(), position().add(step).add(0, .6, 0), 4, 1);
        setPos(next.x, next.y, next.z);
    }

    /** The storm blows out: everything inside is flung away under a last flurry of pages. */
    private void blowOut() {
        LivingEntity owner = getLivingOwner();
        float radius = getRadius();
        Vec3 center = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level(), center.add(0, 1, 0), radius + 1, owner, x -> true)) {
            Vec3 away = new Vec3(e.getX() - center.x, 0, e.getZ() - center.z);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage * 2, SpellUtil.damageSource(ModSpells.PAPER_TEMPEST.get(), this, owner));
            e.setDeltaMovement(away.scale(1.3).add(0, .55, 0));
            e.hurtMarked = true;
        }
        Vec3 c = center.add(0, HEIGHT * .4, 0);
        SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 70, radius * .5, HEIGHT * .3, radius * .5, .45);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, c.x, c.y, c.z, 80, radius * .4, HEIGHT * .3, radius * .4, 1.5);
        SpellUtil.particles(level(), new BlastwaveParticleOptions(BookFx.VIOLET, radius * 2.2f), center.x, center.y + .2, center.z, 1, 0, 0, 0, 0);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2f, .7f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 2f, .6f);
        discard();
    }

    private void clientParticles() {
        float age = tickCount;
        float radius = getRadius();
        float grown = Mth.clamp(age / FORM_TICKS, 0f, 1f);
        // Scraps and glyphs racing round the funnel, and dust swept off the ground at its foot
        for (int i = 0; i < 3; i++) {
            double h = random.nextDouble() * HEIGHT * grown;
            double r = radius * (.35 + .65 * h / HEIGHT) * (.8 + random.nextDouble() * .3);
            double a = random.nextDouble() * Mth.TWO_PI;
            double sx = -Math.sin(a) * .5;
            double sz = Math.cos(a) * .5;
            level().addParticle(i == 0 ? BookFx.paper() : ParticleTypes.ENCHANT, getX() + Math.cos(a) * r, getY() + h, getZ() + Math.sin(a) * r, sx, .08, sz);
        }
        if (tickCount % 2 == 0) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double r = radius * .4;
            level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * r, getY() + .1, getZ() + Math.sin(a) * r, -Math.sin(a) * .25, .02, Math.cos(a) * .25);
            level().addParticle(BookFx.VIOLET_DUST, getX() + Math.cos(a + 2) * radius * .7, getY() + random.nextDouble() * HEIGHT * grown, getZ() + Math.sin(a + 2) * radius * .7, 0, 0, 0);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float r = getRadius() * 1.6f;
        return new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + HEIGHT + 2, getZ() + r);
    }
}
