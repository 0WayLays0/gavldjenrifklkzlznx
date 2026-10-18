package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import com.waylays.arcanes.spells.book.PaperCranesSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A glowing origami crane. It flutters up and out from the caster for {@link #LAUNCH_TICKS} ticks, then dives at its
 * target (or the nearest enemy), gathering speed, and bursts into paper confetti on whatever it hits, hurting the
 * enemies right around too.
 */
public class PaperCrane extends SpellProjectile {
    public static final int LAUNCH_TICKS = 12;
    public static final float SPLASH_RADIUS = 1.8f;

    private int targetId = -1;

    public PaperCrane(EntityType<? extends PaperCrane> entityType, Level level) {
        super(entityType, level);
    }

    public PaperCrane(Level level, LivingEntity owner, float damage, @Nullable LivingEntity target) {
        this(ModEntities.PAPER_CRANE.get(), level);
        setOwner(owner);
        setDamage(damage);
        this.targetId = target == null ? -1 : target.getId();
    }

    @Override
    protected int lifetime() {
        return 100;
    }

    @Nullable
    private LivingEntity target() {
        if (targetId >= 0 && level().getEntity(targetId) instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        targetId = -1;
        // Lost its prey: the nearest enemy around will do (looked for a few times a second)
        if (tickCount % 5 != 0) {
            return null;
        }
        Entity owner = getOwner();
        LivingEntity best = null;
        double bestDist = PaperCranesSpell.SEEK_RADIUS * PaperCranesSpell.SEEK_RADIUS;
        for (LivingEntity e : SpellUtil.enemiesAround(level(), position(), PaperCranesSpell.SEEK_RADIUS, owner, x -> !(x instanceof ArmorStand))) {
            double d = e.distanceToSqr(this);
            if (d < bestDist && e.hasLineOfSight(this)) {
                best = e;
                bestDist = d;
            }
        }
        if (best != null) {
            targetId = best.getId();
        }
        return best;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && tickCount > LAUNCH_TICKS) {
            LivingEntity target = target();
            Vec3 motion = getDeltaMovement();
            float t = Mth.clamp((tickCount - LAUNCH_TICKS) / 25f, 0f, 1f);
            double speed = Mth.lerp(t, .55, 1.15);
            if (target != null) {
                Vec3 to = target.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed);
                double turn = Mth.lerp(t, .12, .3);
                setDeltaMovement(motion.scale(1 - turn).add(to.scale(turn)).normalize().scale(speed));
            } else {
                setDeltaMovement(motion.normalize().scale(speed));
            }
            hasImpulse = true;
        } else if (!level().isClientSide) {
            // Fluttering up: slowing down while it unfolds its wings
            setDeltaMovement(getDeltaMovement().scale(.9).add(0, .015, 0));
            hasImpulse = true;
        }
        super.tick();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void clientTick() {
        if (tickCount % 2 == 0) {
            Vec3 p = position();
            level().addParticle(BookFx.GILT_DUST, p.x, p.y, p.z, 0, 0, 0);
        }
        if (random.nextInt(3) == 0) {
            Vec3 p = position();
            level().addParticle(ParticleTypes.ENCHANT, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    @Override
    protected void onImpact(HitResult hit) {
        Entity owner = getOwner();
        Vec3 at = hit.getLocation();
        LivingEntity struck = null;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            struck = living;
            at = living.getBoundingBox().getCenter();
            living.invulnerableTime = 0;
            DamageSources.applyDamage(living, damage, SpellUtil.damageSource(ModSpells.PAPER_CRANES.get(), this, owner));
        }
        final LivingEntity direct = struck;
        for (LivingEntity e : SpellUtil.enemiesAround(level(), at, SPLASH_RADIUS, owner, x -> x != direct)) {
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage * .4f, SpellUtil.damageSource(ModSpells.PAPER_CRANES.get(), this, owner));
        }
        confetti(at);
        discard();
    }

    @Override
    protected void onExpire() {
        confetti(position());
    }

    private void confetti(Vec3 at) {
        SpellUtil.particles(level(), BookFx.paper(), at.x, at.y, at.z, 14, .2, .2, .2, .25);
        SpellUtil.particles(level(), BookFx.VIOLET_DUST, at.x, at.y, at.z, 10, .4, .4, .4, 0);
        SpellUtil.particles(level(), BookFx.GILT_DUST, at.x, at.y, at.z, 10, .4, .4, .4, 0);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, at.x, at.y, at.z, 20, .2, .2, .2, 1.2);
        SpellUtil.particles(level(), ParticleTypes.FIREWORK, at.x, at.y, at.z, 6, .1, .1, .1, .12);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, .8f, 1.6f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, 1.3f);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
