package com.waylays.arcanes.entity.boss;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.boss.BossFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Annihilation Beam: a rune circle standing upright at the caster's side of the warning strip, charging while the
 * warning lasts, then a beam of energy along the strip for two seconds, hurting whatever is in it every few ticks.
 * The beam does not follow anyone: it burns the strip that was marked.
 */
public class AnnihilationBeam extends SpellEffectEntity {
    public static final int DURATION = 40;
    public static final int PULSE = 4;
    /** Height of the beam above the ground. */
    public static final double HEIGHT = 1.3;

    private static final EntityDataAccessor<Float> DATA_LENGTH = SynchedEntityData.defineId(AnnihilationBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(AnnihilationBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(AnnihilationBeam.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FIRE = SynchedEntityData.defineId(AnnihilationBeam.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public AnnihilationBeam(EntityType<? extends AnnihilationBeam> entityType, Level level) {
        super(entityType, level);
    }

    public static AnnihilationBeam spawn(Level level, LivingEntity caster, AbstractSpell spell, Vec3 ground, float yaw, float length, float width,
                                         int fireTick, float pulseDamage) {
        AnnihilationBeam beam = new AnnihilationBeam(ModEntities.ANNIHILATION_BEAM.get(), level);
        beam.setOwner(caster);
        beam.spellId = spell.getSpellId();
        beam.damage = pulseDamage;
        beam.moveTo(ground.x, ground.y, ground.z, yaw, 0);
        beam.entityData.set(DATA_LENGTH, length);
        beam.entityData.set(DATA_WIDTH, width);
        beam.entityData.set(DATA_START, (int) level.getGameTime());
        beam.entityData.set(DATA_FIRE, fireTick);
        level.addFreshEntity(beam);
        return beam;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTH, 10f);
        builder.define(DATA_WIDTH, 2f);
        builder.define(DATA_START, 0);
        builder.define(DATA_FIRE, 0);
    }

    public float getLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public float getWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    /** 0 to 1 while it charges. */
    public float charge(float partialTicks) {
        int start = this.entityData.get(DATA_START);
        int fire = this.entityData.get(DATA_FIRE);
        float now = (int) level().getGameTime() + partialTicks;
        return fire <= start ? 1 : Mth.clamp((now - start) / (fire - start), 0, 1);
    }

    /** Ticks since the beam fired (negative while it charges). */
    public float firing(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_FIRE);
    }

    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        float firing = firing(0);
        if (!(level() instanceof ServerLevel level)) {
            if (firing < 0) {
                // Energy drawn into the circle
                Vec3 c = position().add(0, HEIGHT, 0);
                double a = random.nextDouble() * Math.PI * 2;
                double r = 2.5 + random.nextDouble();
                Vec3 side = new Vec3(forward().z, 0, -forward().x);
                Vec3 from = c.add(side.scale(Math.cos(a) * r)).add(0, Math.sin(a) * r, 0);
                Vec3 v = c.subtract(from).scale(.12);
                level().addParticle(ParticleTypes.END_ROD, from.x, from.y, from.z, v.x, v.y, v.z);
            }
            return;
        }
        if (firing == 0) {
            Vec3 c = position();
            level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, .7f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, .5f);
            SpellUtil.cameraShake(level, c.add(forward().scale(getLength() * .5)), DURATION, getLength() * .6f);
        }
        if (firing >= 0 && firing < DURATION) {
            if (firing % PULSE == 0) {
                burn(level);
            }
            if (firing % 10 == 0) {
                Vec3 c = position();
                level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 3f, .6f);
            }
        }
        if (firing > DURATION + 8 || tickCount > 1200) {
            discard();
        }
    }

    /** One pulse of the beam: everything in it (but the caster's side) is hurt. */
    private void burn(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        Vec3 f = forward();
        float length = getLength();
        float half = getWidth() * .5f;
        AABB box = new AABB(getX() - length - half, getY() - 1, getZ() - length - half, getX() + length + half, getY() + HEIGHT + 2.5, getZ() + length + half);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!e.isAlive() || e.isSpectator() || SpellUtil.isAlly(getOwner(), e)) {
                continue;
            }
            Vec3 d = e.position().subtract(position());
            double along = d.x * f.x + d.z * f.z;
            double across = d.x * f.z - d.z * f.x;
            if (along < 0 || along > length || Math.abs(across) > half + e.getBbWidth() * .5) {
                continue;
            }
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, getOwner()));
        }
        for (float d = 1; d < length; d += 1.5f) {
            Vec3 p = position().add(f.scale(d)).add(0, HEIGHT, 0);
            SpellUtil.particles(level, BossFx.dust(0xFF4050, 1.6f), p.x, p.y, p.z, 1, .25, .25, .25, 0);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
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
