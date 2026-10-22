package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.token.TokenFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;

/**
 * Token Aegis: soul tokens turning around their caster. Each blow or shot that reaches the caster from someone is
 * caught by a token instead (no damage), and that token flies back at whoever struck. When none is left, the aegis is
 * gone. Falls, fire and the like are not blows and go through.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class TokenAegis extends SpellEffectEntity {
    public static final int FADE_TICKS = 10;
    public static final float ORBIT = 1.2f;
    public static final double COUNTER_RANGE = 16;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(TokenAegis.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(TokenAegis.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TokenAegis.class, EntityDataSerializers.INT);
    /** Tick a token last caught a blow (it flashes). */
    private static final EntityDataAccessor<Integer> DATA_CAUGHT = SynchedEntityData.defineId(TokenAegis.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public TokenAegis(EntityType<? extends TokenAegis> entityType, Level level) {
        super(entityType, level);
    }

    /** Sets the tokens turning around {@code caster}, replacing those it had. */
    public static TokenAegis raise(Level level, LivingEntity caster, AbstractSpell spell, int tokens, int life, float damage) {
        TokenAegis old = of(level, caster);
        if (old != null) {
            old.discard();
        }
        TokenAegis aegis = new TokenAegis(ModEntities.TOKEN_AEGIS.get(), level);
        aegis.setOwner(caster);
        aegis.spellId = spell.getSpellId();
        aegis.damage = damage;
        aegis.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        aegis.entityData.set(DATA_OWNER, caster.getId());
        aegis.entityData.set(DATA_COUNT, tokens);
        aegis.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(aegis);
        return aegis;
    }

    /** The aegis turning around {@code caster}, if it has tokens left. */
    @Nullable
    public static TokenAegis of(Level level, LivingEntity caster) {
        for (TokenAegis aegis : level.getEntitiesOfClass(TokenAegis.class, caster.getBoundingBox().inflate(4),
                a -> !a.isRemoved() && caster.getUUID().equals(a.getOwnerUUID()) && a.getCount() > 0 && a.tickCount <= a.getLife())) {
            return aegis;
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_COUNT, 3);
        builder.define(DATA_LIFE, 400);
        builder.define(DATA_CAUGHT, -1000);
    }

    @Nullable
    public Entity getBearer() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public int getCount() {
        return this.entityData.get(DATA_COUNT);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public float sinceCaught(float partialTicks) {
        return tickCount + partialTicks - this.entityData.get(DATA_CAUGHT);
    }

    /** Degrees around the bearer of token {@code i} of {@code count}. */
    public float angle(int i, int count, float partialTicks) {
        return (tickCount + partialTicks) * 5 + i * 360f / Math.max(1, count);
    }

    /** 1 while it holds, fading at the end or once the last token is spent. */
    public float strength(float partialTicks) {
        float end = getCount() > 0 ? getLife() : this.entityData.get(DATA_CAUGHT);
        return Mth.clamp(1 - (tickCount + partialTicks - end) / FADE_TICKS, 0, 1) * Mth.clamp((tickCount + partialTicks) / 6, 0, 1);
    }

    /** Where token {@code i} is, relative to the bearer's feet. */
    public Vec3 offset(int i, int count, Entity bearer, float partialTicks) {
        float a = angle(i, count, partialTicks) * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(a) * ORBIT, bearer.getBbHeight() * .55 + .25 * Mth.sin(a * 2), Mth.sin(a) * ORBIT);
    }

    @Override
    public void tick() {
        super.tick();
        Entity bearer = getBearer();
        if (bearer != null) {
            setPos(bearer.getX(), bearer.getY(), bearer.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        boolean spent = getCount() <= 0 && tickCount > this.entityData.get(DATA_CAUGHT) + FADE_TICKS;
        if (bearer == null || !bearer.isAlive() || spent || tickCount > getLife() + FADE_TICKS) {
            discard();
        }
    }

    /** A token catches the blow: no damage, and the token flies back at whoever struck. */
    private void catchBlow(ServerLevel level, LivingEntity bearer, DamageSource source) {
        int count = getCount();
        Vec3 from = bearer.position().add(offset(count - 1, count, bearer, 0));
        this.entityData.set(DATA_COUNT, count - 1);
        this.entityData.set(DATA_CAUGHT, tickCount);
        TokenFx.impact(level, from, .5f);
        TokenFx.sound(level, from, SoundEvents.SHIELD_BLOCK, .8f, 1.5f);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell != null && source.getEntity() instanceof LivingEntity attacker && attacker != bearer && attacker.isAlive()
                && attacker.distanceToSqr(bearer) < COUNTER_RANGE * COUNTER_RANGE) {
            TokenBolt.shoot(level, bearer, spell, TokenBolt.Mode.STRIKE, from, TokenFx.chest(attacker).subtract(from), 1.5f, damage);
        }
    }

    /** Blows from someone (in hand or shot) are caught; falls, fire, magic out of nowhere are not. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity bearer = event.getEntity();
        if (!(bearer.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (direct == null || attacker == bearer || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !(direct instanceof Projectile || direct instanceof LivingEntity || attacker instanceof LivingEntity)) {
            return;
        }
        TokenAegis aegis = of(level, bearer);
        if (aegis != null) {
            event.setCanceled(true);
            aegis.catchBlow(level, bearer, source);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
