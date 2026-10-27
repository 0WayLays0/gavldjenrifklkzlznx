package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Grasping Roots: thick roots burst out of the ground round the enemy and wind up round it, tightening: it is held fast
 * where it stands and crushed a little every second, until they let go and sink back. Both sides work out the roots
 * from their age; the server holds the prisoner.
 */
public class RootSnare extends SpellEffectEntity {
    public static final int GRIP = 12;
    public static final int RELEASE = 12;
    public static final int ROOTS = 6;

    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(RootSnare.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(RootSnare.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HOLD = SynchedEntityData.defineId(RootSnare.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int prisoner = -1;

    public RootSnare(EntityType<? extends RootSnare> entityType, Level level) {
        super(entityType, level);
    }

    public static RootSnare seize(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, int hold, float damage) {
        RootSnare snare = new RootSnare(ModEntities.ROOT_SNARE.get(), level);
        snare.setOwner(caster);
        snare.spellId = spell.getSpellId();
        snare.damage = damage;
        snare.prisoner = target.getId();
        snare.moveTo(target.getX(), target.getY(), target.getZ(), level.random.nextFloat() * 360, 0);
        snare.entityData.set(DATA_WIDTH, target.getBbWidth() * .5f + .25f);
        snare.entityData.set(DATA_HEIGHT, target.getBbHeight() * .85f);
        snare.entityData.set(DATA_HOLD, hold);
        level.addFreshEntity(snare);
        WoodFx.sprout(level, target.position(), 1.4f);
        WoodFx.chips(level, target.position().add(0, .2, 0), 16, .6);
        return snare;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, .55f);
        builder.define(DATA_HEIGHT, 1.6f);
        builder.define(DATA_HOLD, 60);
    }

    public float getWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public int getHold() {
        return this.entityData.get(DATA_HOLD);
    }

    /** How far the roots have wound up (0 to 1), and back down at the end. */
    public float grown(float age) {
        float up = Mth.clamp(age / GRIP, 0, 1);
        float down = Mth.clamp((age - GRIP - getHold()) / RELEASE, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > GRIP + getHold() + RELEASE) {
            discard();
            return;
        }
        Entity e = level.getEntity(prisoner);
        LivingEntity target = e instanceof LivingEntity living && living.isAlive() ? living : null;
        if (target == null || tickCount < GRIP / 2 || tickCount > GRIP + getHold()) {
            return;
        }
        // Held fast where it stood
        if (target.position().distanceToSqr(position()) > .09) {
            target.teleportTo(getX(), Math.max(getY(), Math.min(target.getY(), getY() + .3)), getZ());
        }
        target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, false));
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell != null && (tickCount - GRIP) % 20 == 0) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, getLivingOwner()));
            WoodFx.chips(level, target.position().add(0, .6, 0), 6, .3);
            WoodFx.sound(level, position(), SoundEvents.WOOD_HIT, 1.2f, .5f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
