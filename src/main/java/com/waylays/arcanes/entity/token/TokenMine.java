package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.token.TokenFx;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A trap token lying on the ground. It drops into place, arms after a second (it starts blinking), and bursts when
 * an enemy comes near: the enemies around are hurt and slowed. A caster may have {@link #MAX_PER_CASTER} at once.
 */
public class TokenMine extends SpellEffectEntity {
    public static final int DROP_TICKS = 6;
    public static final int ARM_TICKS = 20;
    public static final int LIFE = 900;
    public static final double TRIGGER = 1.4;
    public static final float BURST = 2.2f;
    public static final int MAX_PER_CASTER = 9;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TokenMine.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public TokenMine(EntityType<? extends TokenMine> entityType, Level level) {
        super(entityType, level);
    }

    public static TokenMine place(ServerLevel level, LivingEntity caster, AbstractSpell spell, Vec3 at, float damage) {
        List<TokenMine> mine = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof TokenMine m && !m.isRemoved() && caster.getUUID().equals(m.getOwnerUUID())) {
                mine.add(m);
            }
        }
        // The oldest go first
        mine.sort(Comparator.comparingInt((TokenMine m) -> m.tickCount).reversed());
        for (int i = 0; i <= mine.size() - MAX_PER_CASTER; i++) {
            mine.get(i).discard();
        }
        TokenMine token = new TokenMine(ModEntities.TOKEN_MINE.get(), level);
        token.setOwner(caster);
        token.spellId = spell.getSpellId();
        token.damage = damage;
        token.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        token.entityData.set(DATA_LIFE, LIFE);
        level.addFreshEntity(token);
        return token;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, LIFE);
    }

    public boolean isArmed() {
        return tickCount >= ARM_TICKS;
    }

    /** How high it still is as it drops into place (0 once down). */
    public float drop(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / DROP_TICKS, 0, 1);
        return (1 - t * t) * 1.2f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > this.entityData.get(DATA_LIFE)) {
            discard();
            return;
        }
        if (tickCount == ARM_TICKS) {
            TokenFx.sound(level(), position(), SoundEvents.TRIPWIRE_CLICK_ON, .6f, 1.6f);
        }
        if (!isArmed() || tickCount % 2 != 0) {
            return;
        }
        Entity owner = getOwner();
        if (!SpellUtil.enemiesAround(level(), position().add(0, .6, 0), TRIGGER + .6, owner,
                e -> e.distanceToSqr(getX(), e.getY(), getZ()) <= TRIGGER * TRIGGER && e.getY() < getY() + 2 && e.getY() > getY() - 1).isEmpty()) {
            burst((ServerLevel) level(), owner);
        }
    }

    private void burst(ServerLevel level, @Nullable Entity owner) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Vec3 at = position().add(0, .3, 0);
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, at, BURST, owner, x -> true)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, owner));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        }
        TokenFx.impact(level, at, BURST);
        TokenFx.sound(level, at, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 1.4f);
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
