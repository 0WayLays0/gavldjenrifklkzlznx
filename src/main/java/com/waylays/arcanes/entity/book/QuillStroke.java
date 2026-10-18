package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Quill of Fate: a giant spectral quill writes a line of ink along the ground from the entity's position along its
 * yaw; the writing glows, then erupts into a row of rune pillars that hurt and fling up every enemy standing on it.
 */
public class QuillStroke extends SpellEffectEntity {
    public static final int WRITE_TICKS = 16;
    public static final int ERUPT_TICK = 26;
    public static final int END_TICK = 44;
    public static final float PILLAR_SPACING = 1.3f;
    /** How far from the line an enemy is still caught. */
    public static final float HALF_WIDTH = 1.25f;

    private static final EntityDataAccessor<Float> DATA_LENGTH = SynchedEntityData.defineId(QuillStroke.class, EntityDataSerializers.FLOAT);

    public QuillStroke(EntityType<? extends QuillStroke> entityType, Level level) {
        super(entityType, level);
    }

    public QuillStroke(Level level, LivingEntity owner, float damage, float length) {
        this(ModEntities.QUILL_STROKE.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.entityData.set(DATA_LENGTH, length);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LENGTH, 10f);
    }

    public float getLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public Vec3 direction() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** How much of the line is written at {@code age} ticks, 0 to 1. */
    public static float progress(float age) {
        float t = Mth.clamp(age / WRITE_TICKS, 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    public int pillarCount() {
        return Math.max(1, (int) (getLength() / PILLAR_SPACING) + 1);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 dir = direction();
        if (level().isClientSide) {
            if (tickCount <= WRITE_TICKS) {
                // Ink spattering from the nib
                Vec3 tip = position().add(dir.scale(getLength() * progress(tickCount)));
                level().addParticle(BookFx.INK_DUST, tip.x + (random.nextFloat() - .5f) * .3, tip.y + .1, tip.z + (random.nextFloat() - .5f) * .3, 0, .02, 0);
                level().addParticle(ParticleTypes.ENCHANT, tip.x, tip.y + .4, tip.z, 0, .3, 0);
            } else if (tickCount < ERUPT_TICK) {
                for (int i = 0; i < 2; i++) {
                    Vec3 p = position().add(dir.scale(getLength() * random.nextFloat()));
                    level().addParticle(BookFx.VIOLET_DUST, p.x, p.y + .1, p.z, 0, .05, 0);
                }
            }
            return;
        }
        if (tickCount <= WRITE_TICKS && tickCount % 3 == 1) {
            Vec3 tip = position().add(dir.scale(getLength() * progress(tickCount)));
            level().playSound(null, tip.x, tip.y, tip.z, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 1.2f, .8f + random.nextFloat() * .3f);
        }
        if (tickCount == WRITE_TICKS + 2) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, .6f);
        }
        if (tickCount == ERUPT_TICK) {
            erupt(dir);
        }
        if (tickCount >= END_TICK) {
            discard();
        }
    }

    private void erupt(Vec3 dir) {
        Entity owner = getOwner();
        Vec3 start = position();
        float length = getLength();
        AABB box = new AABB(start, start.add(dir.scale(length))).inflate(HALF_WIDTH + 1, 0, HALF_WIDTH + 1).expandTowards(0, 3.5, 0).move(0, -1.5, 0);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !SpellUtil.isAlly(owner, e))) {
            Vec3 d = e.position().subtract(start);
            double along = Mth.clamp(d.dot(dir), 0, length);
            Vec3 nearest = start.add(dir.scale(along));
            double off = Math.hypot(e.getX() - nearest.x, e.getZ() - nearest.z);
            if (off > HALF_WIDTH + e.getBbWidth() * .5) {
                continue;
            }
            e.invulnerableTime = 0;
            if (DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.QUILL_OF_FATE.get(), this, owner))) {
                Vec3 m = e.getDeltaMovement();
                e.setDeltaMovement(m.x * .3, .95, m.z * .3);
                e.hurtMarked = true;
            }
        }
        for (int i = 0; i < pillarCount(); i++) {
            Vec3 p = start.add(dir.scale(Math.min(length, i * PILLAR_SPACING)));
            SpellUtil.particles(level(), ParticleTypes.ENCHANT, p.x, p.y + 1, p.z, 16, .2, .8, .2, 1.2);
            SpellUtil.particles(level(), BookFx.VIOLET_DUST, p.x, p.y + 1.2, p.z, 6, .2, 1, .2, 0);
            SpellUtil.particles(level(), BookFx.paper(), p.x, p.y + .5, p.z, 2, .2, .3, .2, .15);
        }
        Vec3 mid = start.add(dir.scale(length * .5));
        level().playSound(null, mid.x, mid.y, mid.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 2f, .7f);
        level().playSound(null, mid.x, mid.y, mid.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 2f, .6f);
        level().playSound(null, mid.x, mid.y, mid.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.2f);
        SpellUtil.cameraShake(level(), mid, 8, length * .5f + 6);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 end = position().add(direction().scale(getLength()));
        return new AABB(position(), end).inflate(2, 4, 2);
    }
}
