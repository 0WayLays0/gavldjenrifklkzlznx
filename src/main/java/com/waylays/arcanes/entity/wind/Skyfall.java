package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
 * Skyfall: a whirling updraft seizes the enemy the caster looks at and carries it up {@link #HEIGHT} blocks; then a
 * downburst slams it back to the ground: struck as it is hurled down, then the fall.
 */
public class Skyfall extends SpellEffectEntity {
    public static final int LIFT = 30;
    public static final int SLAM = 14;
    public static final float HEIGHT = 9;

    private static final EntityDataAccessor<Integer> DATA_VICTIM = SynchedEntityData.defineId(Skyfall.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public Skyfall(EntityType<? extends Skyfall> entityType, Level level) {
        super(entityType, level);
    }

    public static Skyfall seize(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity victim, float damage) {
        Skyfall s = new Skyfall(ModEntities.SKYFALL.get(), level);
        s.setOwner(caster);
        s.spellId = spell.getSpellId();
        s.damage = damage;
        s.moveTo(victim.getX(), victim.getY(), victim.getZ(), 0, 0);
        s.entityData.set(DATA_VICTIM, victim.getId());
        level.addFreshEntity(s);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 2, .7f);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VICTIM, -1);
    }

    @Nullable
    public Entity victim() {
        return level().getEntity(this.entityData.get(DATA_VICTIM));
    }

    /** How far up it has been carried (0 to 1), and how far through the downburst (below 0 before it). */
    public float lift(float age) {
        return Mth.clamp(age / LIFT, 0, 1);
    }

    public float slam(float age) {
        return age < LIFT ? -1 : Mth.clamp((age - LIFT) / SLAM, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity v = victim();
        if (tickCount > LIFT + SLAM || !(v instanceof LivingEntity victim) || !victim.isAlive()) {
            discard();
            return;
        }
        if (tickCount < LIFT) {
            // Carried up, turning, held over the spot
            double goal = getY() + HEIGHT;
            double k = WindFx.give(victim);
            double vy = Mth.clamp((goal - victim.getY()) * .18, -.2, .7) * Math.max(.3, k);
            Vec3 back = new Vec3(getX() - victim.getX(), 0, getZ() - victim.getZ()).scale(.2);
            victim.setDeltaMovement(back.x, vy, back.z);
            victim.hurtMarked = true;
            victim.fallDistance = 0;
            if (tickCount % 6 == 0) {
                WindFx.gust(level, victim.position(), 3, .6);
            }
            return;
        }
        if (tickCount == LIFT) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null) {
                victim.invulnerableTime = 0;
                DamageSources.applyDamage(victim, damage, SpellUtil.damageSource(spell, this, getOwner()));
            }
            victim.setDeltaMovement(0, -2.6, 0);
            victim.hurtMarked = true;
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 2, .5f);
        }
        if (tickCount == LIFT + 4) {
            WindFx.dust(level, position(), 30, 2.5);
            WindFx.gust(level, position().add(0, .5, 0), 16, 2);
            SpellUtil.cameraShake(level, position(), 8, 14);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
