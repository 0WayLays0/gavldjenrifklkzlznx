package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Storm of Blades: blades of wind wheeling round the caster for a while, moving with them; each enemy a blade passes
 * through is cut (each blade at most every half second).
 */
public class BladeStorm extends SpellEffectEntity {
    public static final float RADIUS = 3;
    public static final int LIFE = 200;
    public static final int FADE = 12;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(BladeStorm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(BladeStorm.class, EntityDataSerializers.INT);

    private final Int2IntOpenHashMap lastHit = new Int2IntOpenHashMap();
    @Nullable
    private String spellId;

    public BladeStorm(EntityType<? extends BladeStorm> entityType, Level level) {
        super(entityType, level);
    }

    public static BladeStorm raise(Level level, LivingEntity caster, AbstractSpell spell, int count, float damage) {
        for (BladeStorm old : level.getEntitiesOfClass(BladeStorm.class, caster.getBoundingBox().inflate(2))) {
            if (old.follows() == caster) {
                old.discard();
            }
        }
        BladeStorm storm = new BladeStorm(ModEntities.BLADE_STORM.get(), level);
        storm.setOwner(caster);
        storm.spellId = spell.getSpellId();
        storm.damage = damage;
        storm.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        storm.entityData.set(DATA_OWNER, caster.getId());
        storm.entityData.set(DATA_COUNT, count);
        level.addFreshEntity(storm);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1.6f, 1.3f);
        return storm;
    }

    /** Where blade {@code k} of {@code count} is at {@code age}, from the caster's feet (as BladeStormModel draws it). */
    public static Vec3 blade(int k, int count, float age) {
        float a = age * .22f + k * Mth.TWO_PI / count;
        return new Vec3(Mth.cos(a) * RADIUS, 1.1f + .35f * Mth.sin(age * .12f + k * 1.7f), Mth.sin(a) * RADIUS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_COUNT, 6);
    }

    @Nullable
    public Entity follows() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public int getCount() {
        return this.entityData.get(DATA_COUNT);
    }

    public float alive(float age) {
        return Mth.clamp(age / 8, 0, 1) * (1 - Mth.clamp((age - LIFE) / FADE, 0, 1));
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = follows();
        if (owner != null && tickCount < LIFE) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > LIFE + FADE || owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (tickCount > LIFE) {
            return;
        }
        if (tickCount % 12 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .6f, 1.5f);
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        int count = getCount();
        for (int k = 0; k < count; k++) {
            Vec3 b = position().add(blade(k, count, tickCount));
            for (LivingEntity e : SpellUtil.enemiesAround(level, b, 1.3, caster, x -> true)) {
                int key = e.getId() * 31 + k;
                if (spell == null || tickCount - lastHit.getOrDefault(key, -100) < 10) {
                    continue;
                }
                lastHit.put(key, tickCount);
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }
}
