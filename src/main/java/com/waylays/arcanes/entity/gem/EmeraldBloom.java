package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Emerald Bloom: a flower of emeralds opens on the ground, six cut emeralds for petals round a great one floating
 * at its heart. Every second it heals the caster and their allies around, and takes one ailment off each; when it
 * fades its petals burst into green light that leaves them regenerating and shielded.
 */
public class EmeraldBloom extends SpellEffectEntity {
    public static final int PETALS = 6;
    public static final float PETAL_RING = 1.5f;
    public static final int OPEN = 18;
    public static final int HEAL_TICKS = 20;
    public static final float RANGE = 8;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(EmeraldBloom.class, EntityDataSerializers.INT);

    public EmeraldBloom(EntityType<? extends EmeraldBloom> entityType, Level level) {
        super(entityType, level);
    }

    /** {@code heal}: health given each second to each ally around. */
    public static EmeraldBloom open(Level level, LivingEntity caster, Vec3 at, int life, float heal) {
        EmeraldBloom bloom = new EmeraldBloom(ModEntities.EMERALD_BLOOM.get(), level);
        bloom.setOwner(caster);
        bloom.damage = heal;
        bloom.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        bloom.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(bloom);
        GemFx.sigil(level, at, Gem.EMERALD, RANGE * .5f, OPEN + 16);
        GemFx.chime(level, at, 1.4f, 1.1f);
        return bloom;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 160);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How far the flower has opened (0 to 1), and closing again at the end. */
    public float open(float partialTicks) {
        float age = tickCount + partialTicks;
        float t = Mth.clamp(age / OPEN, 0, 1);
        return t * t * (3 - 2 * t) * Mth.clamp((getLife() - age) / 10, 0, 1);
    }

    /** The caster and those on their side, around the flower. */
    private List<LivingEntity> allies(ServerLevel level) {
        LivingEntity owner = getLivingOwner();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(RANGE))) {
            if (e.isAlive() && e.distanceToSqr(position()) <= RANGE * RANGE
                    && (owner == null ? e instanceof Player : SpellUtil.isAlly(owner, e) || e.isAlliedTo(owner))) {
                out.add(e);
            }
        }
        return out;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int life = getLife();
        if (tickCount > life) {
            discard();
            return;
        }
        if (tickCount >= OPEN && (tickCount - OPEN) % HEAL_TICKS == 0 && tickCount < life) {
            for (LivingEntity e : allies(level)) {
                e.heal(damage);
                // One ailment taken off
                for (MobEffectInstance effect : e.getActiveEffects()) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        e.removeEffect(effect.getEffect());
                        break;
                    }
                }
                Vec3 at = e.position().add(0, e.getBbHeight() * .5, 0);
                SpellUtil.particles(level, ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 6, .35, .5, .35, 0);
                GemFx.glitter(level, at, Gem.EMERALD, 6, .4);
            }
            GemFx.chime(level, position(), .8f, 1.3f + random.nextFloat() * .3f);
        }
        if (tickCount == life) {
            for (LivingEntity e : allies(level)) {
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
            }
            for (int i = 0; i < PETALS; i++) {
                float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / PETALS;
                GemFx.shatter(level, position().add(Mth.cos(a) * PETAL_RING, .8, Mth.sin(a) * PETAL_RING), Gem.EMERALD, .8f, 5);
            }
            GemFx.shatter(level, position().add(0, 1.6, 0), Gem.EMERALD, 1.4f, 12);
            GemFx.sound(level, position(), SoundEvents.PLAYER_LEVELUP, .8f, 1.6f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
