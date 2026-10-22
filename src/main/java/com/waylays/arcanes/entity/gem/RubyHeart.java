package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Burning Heart: a ruby thrown spinning. It sets itself into the first enemy it strikes (or into the ground) and
 * beats like a heart, three times, each beat hotter, scorching all around; on the fourth it bursts in molten shards.
 */
public class RubyHeart extends SpellProjectile {
    public static final int BEAT_TICKS = 18;
    public static final int BEATS = 3;
    public static final float BEAT_RADIUS = 2.5f;
    public static final float BLAST_RADIUS = 4.5f;

    private static final EntityDataAccessor<Integer> DATA_HOST = SynchedEntityData.defineId(RubyHeart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SET_AT = SynchedEntityData.defineId(RubyHeart.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public RubyHeart(EntityType<? extends RubyHeart> entityType, Level level) {
        super(entityType, level);
    }

    public static RubyHeart hurl(Level level, LivingEntity owner, AbstractSpell spell, Vec3 from, Vec3 velocity, float damage) {
        RubyHeart heart = new RubyHeart(ModEntities.RUBY_HEART.get(), level);
        heart.setOwner(owner);
        heart.spellId = spell.getSpellId();
        heart.setDamage(damage);
        heart.setPos(from.x, from.y - heart.getBbHeight() * .5, from.z);
        heart.setDeltaMovement(velocity);
        level.addFreshEntity(heart);
        return heart;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HOST, -1);
        builder.define(DATA_SET_AT, -1);
    }

    /** The enemy it is set in (entity id), or -1. */
    public int getHost() {
        return this.entityData.get(DATA_HOST);
    }

    /** The tick it set itself in something, or -1 while it flies. */
    public int getSetAt() {
        return this.entityData.get(DATA_SET_AT);
    }

    /** How hot it glows (0 to 1): a beat at a time, white hot before it bursts. */
    public float heat(float partialTicks) {
        int set = getSetAt();
        if (set < 0) {
            return .3f;
        }
        return Mth.clamp((tickCount + partialTicks - set) / (BEAT_TICKS * (BEATS + 1)), 0, 1);
    }

    @Override
    protected double gravity() {
        return .03;
    }

    @Override
    protected int lifetime() {
        return 200;
    }

    @Override
    public void tick() {
        int set = getSetAt();
        if (set < 0) {
            super.tick();
            return;
        }
        // Set in: it follows its host and beats
        Entity host = level().getEntity(getHost());
        if (host != null && host.isAlive()) {
            setPos(host.getX(), host.getY() + host.getBbHeight() * .6 - getBbHeight() * .5, host.getZ());
        }
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int since = tickCount - set;
        if (since > 0 && since % BEAT_TICKS == 0 && since / BEAT_TICKS <= BEATS) {
            beat(level, since / BEAT_TICKS);
        }
        if (since >= BEAT_TICKS * (BEATS + 1)) {
            burst(level);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            this.entityData.set(DATA_HOST, living.getId());
        } else {
            Vec3 at = hit.getLocation();
            setPos(at.x, at.y, at.z);
        }
        this.entityData.set(DATA_SET_AT, tickCount);
        setDeltaMovement(Vec3.ZERO);
        Vec3 at = position();
        GemFx.sound(level(), at, SoundEvents.AMETHYST_BLOCK_HIT, 1.2f, .6f);
        GemFx.glitter(level(), at, Gem.RUBY, 10, .3);
    }

    /** A beat: the heat scorches all around, hotter each time. */
    private void beat(ServerLevel level, int n) {
        Vec3 at = position().add(0, getBbHeight() * .5, 0);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, at, BEAT_RADIUS, getOwner(), x -> true)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * (.2f + .1f * n), SpellUtil.damageSource(spell, this, getOwner()));
                e.igniteForSeconds(2 + n);
            }
        }
        SpellUtil.particles(level, ParticleTypes.FLAME, at.x, at.y, at.z, 6 + 4 * n, .3, .3, .3, .06);
        GemFx.glitter(level, at, Gem.RUBY, 8 + 4 * n, .5);
        GemFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, .6f + .2f * n);
        GemFx.sound(level, at, SoundEvents.BLAZE_SHOOT, .6f, 1.4f + .15f * n);
    }

    /** It bursts in molten shards. */
    private void burst(ServerLevel level) {
        Vec3 at = position().add(0, getBbHeight() * .5, 0);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        for (LivingEntity e : SpellUtil.enemiesAround(level, at, BLAST_RADIUS, getOwner(), x -> true)) {
            float falloff = (float) Mth.clamp(1 - .5 * e.position().distanceTo(at) / BLAST_RADIUS, .5, 1);
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * falloff, SpellUtil.damageSource(spell, this, getOwner()));
            }
            e.igniteForSeconds(4);
            Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(e.getDeltaMovement().add(away.scale(.9 * resist)).add(0, .45 * resist, 0));
            e.hurtMarked = true;
        }
        GemFx.shatter(level, at, Gem.RUBY, 2.4f, 18);
        SpellUtil.particles(level, ParticleTypes.FLAME, at.x, at.y, at.z, 50, 1, .6, 1, .12);
        SpellUtil.particles(level, ParticleTypes.LAVA, at.x, at.y, at.z, 12, .6, .3, .6, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3f, 1.1f);
        SpellUtil.cameraShake(level, at, 10, 14);
        discard();
    }

    @Override
    protected void onExpire() {
        if (level() instanceof ServerLevel level) {
            burst(level);
        }
    }

    @Override
    protected void clientTick() {
        Vec3 p = position().add(0, getBbHeight() * .5, 0);
        level().addParticle(Gem.RUBY.dust, p.x, p.y, p.z, 0, 0, 0);
        if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, 0, .01, 0);
        }
    }
}
