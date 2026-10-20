package com.waylays.arcanes.entity.boss;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * The Void Orb: a great sphere of darkness rolling slowly along its marked path. Touching it hurts, and it drags
 * whatever is close a little toward it; at the end of its path it waits for its collapse and its burst (both marked
 * on the ground, see the spell), then vanishes.
 */
public class VoidOrb extends SpellEffectEntity {
    public static final float RADIUS = 1.4f;
    public static final double PULL_RANGE = 4.5;
    /** Ticks between two contact hits on the same target. */
    public static final int CONTACT_COOLDOWN = 10;

    private static final EntityDataAccessor<Vector3f> DATA_FROM = SynchedEntityData.defineId(VoidOrb.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_TO = SynchedEntityData.defineId(VoidOrb.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(VoidOrb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ARRIVE = SynchedEntityData.defineId(VoidOrb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(VoidOrb.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private final Map<UUID, Long> lastHit = new HashMap<>();

    public VoidOrb(EntityType<? extends VoidOrb> entityType, Level level) {
        super(entityType, level);
    }

    /** An orb rolling from {@code from} at game tick {@code start} to {@code to} at {@code arrive}, gone at {@code end}. */
    public static VoidOrb spawn(Level level, LivingEntity caster, AbstractSpell spell, Vec3 from, Vec3 to, int start, int arrive, int end, float contactDamage) {
        VoidOrb orb = new VoidOrb(ModEntities.VOID_ORB.get(), level);
        orb.setOwner(caster);
        orb.spellId = spell.getSpellId();
        orb.damage = contactDamage;
        orb.entityData.set(DATA_FROM, from.toVector3f());
        orb.entityData.set(DATA_TO, to.toVector3f());
        orb.entityData.set(DATA_START, start);
        orb.entityData.set(DATA_ARRIVE, arrive);
        orb.entityData.set(DATA_END, end);
        orb.setPos(from);
        level.addFreshEntity(orb);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2f, .4f);
        return orb;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FROM, new Vector3f());
        builder.define(DATA_TO, new Vector3f());
        builder.define(DATA_START, 0);
        builder.define(DATA_ARRIVE, 0);
        builder.define(DATA_END, 0);
    }

    /** Ticks it has existed, on the game clock. */
    public float age(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_START);
    }

    /** Ticks left before it vanishes. */
    public float left(float partialTicks) {
        return this.entityData.get(DATA_END) - ((int) level().getGameTime() + partialTicks);
    }

    private Vec3 positionAt(float partialTicks) {
        int start = this.entityData.get(DATA_START);
        int arrive = this.entityData.get(DATA_ARRIVE);
        float f = arrive <= start ? 1 : Mth.clamp(age(partialTicks) / (arrive - start), 0, 1);
        return new Vec3(this.entityData.get(DATA_FROM)).lerp(new Vec3(this.entityData.get(DATA_TO)), f);
    }

    @Override
    public void tick() {
        super.tick();
        setPos(positionAt(0));
        if (!(level() instanceof ServerLevel level)) {
            for (int i = 0; i < 2; i++) {
                Vec3 p = position().add((random.nextDouble() - .5) * 5, (random.nextDouble() - .5) * 5, (random.nextDouble() - .5) * 5);
                Vec3 v = position().subtract(p).scale(.1);
                level().addParticle(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, v.x, v.y, v.z);
            }
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Vec3 c = position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PULL_RANGE))) {
            if (!e.isAlive() || e.isSpectator() || SpellUtil.isAlly(getOwner(), e)) {
                continue;
            }
            Vec3 toOrb = c.subtract(e.getBoundingBox().getCenter());
            double dist = toOrb.length();
            if (dist < RADIUS + e.getBbWidth() * .5 + .3) {
                long now = level.getGameTime();
                Long last = lastHit.get(e.getUUID());
                if (spell != null && (last == null || now - last >= CONTACT_COOLDOWN)) {
                    lastHit.put(e.getUUID(), now);
                    DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, getOwner()));
                }
            } else if (dist < PULL_RANGE && dist > 1.0E-3) {
                e.setDeltaMovement(e.getDeltaMovement().add(toOrb.scale(.035 / dist)));
                e.hurtMarked = true;
            }
        }
        if (tickCount % 20 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 1.5f, .5f);
        }
        if (left(0) <= 0 || tickCount > 1200) {
            discard();
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
