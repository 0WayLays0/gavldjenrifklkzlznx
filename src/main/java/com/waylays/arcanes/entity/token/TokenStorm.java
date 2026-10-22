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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Token Storm: a whirlwind of soul tokens around the caster, moving with them. Every half second the tokens slash
 * the enemies inside, and shots from enemies that fly into it are knocked out of the air.
 */
public class TokenStorm extends SpellEffectEntity {
    public static final float RADIUS = 3.5f;
    public static final float HEIGHT = 3;
    public static final int PULSE_TICKS = 10;
    public static final int GROW_TICKS = 10;
    public static final int FADE_TICKS = 12;
    /** How many tokens whirl, for the renderer. */
    public static final int TOKENS = 24;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(TokenStorm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TokenStorm.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public TokenStorm(EntityType<? extends TokenStorm> entityType, Level level) {
        super(entityType, level);
    }

    /** Raises the storm around {@code caster}, replacing the one it had. */
    public static TokenStorm raise(Level level, LivingEntity caster, AbstractSpell spell, int life, float damage) {
        for (TokenStorm old : level.getEntitiesOfClass(TokenStorm.class, caster.getBoundingBox().inflate(4), s -> caster.getUUID().equals(s.getOwnerUUID()))) {
            old.discard();
        }
        TokenStorm storm = new TokenStorm(ModEntities.TOKEN_STORM.get(), level);
        storm.setOwner(caster);
        storm.spellId = spell.getSpellId();
        storm.damage = damage;
        storm.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        storm.entityData.set(DATA_OWNER, caster.getId());
        storm.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(storm);
        return storm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_LIFE, 160);
    }

    @Nullable
    public Entity getBearer() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** 0 to 1: gathers, whirls, then the tokens scatter at the end. */
    public float strength(float partialTicks) {
        float age = tickCount + partialTicks;
        return Mth.clamp(age / GROW_TICKS, 0, 1) * Mth.clamp(1 - (age - getLife()) / FADE_TICKS, 0, 1);
    }

    private boolean inside(Entity e) {
        return e.distanceToSqr(getX(), e.getY(), getZ()) <= RADIUS * RADIUS && e.getY() > getY() - 1 && e.getY() < getY() + HEIGHT;
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
        if (bearer == null || !bearer.isAlive() || tickCount > getLife() + FADE_TICKS) {
            discard();
            return;
        }
        if (tickCount > getLife()) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Entity owner = getOwner();
        AABB box = getBoundingBox().inflate(RADIUS, HEIGHT, RADIUS);
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, box, p -> !(p instanceof TokenBolt) && inside(p)
                && p.getOwner() != null && !SpellUtil.isAlly(owner, p.getOwner()))) {
            TokenFx.impact(level, shot.position(), .4f);
            shot.discard();
        }
        if (tickCount % PULSE_TICKS != 0) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        boolean struck = false;
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), RADIUS + 2, owner, this::inside)) {
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, owner));
            TokenFx.burst(level, TokenFx.chest(e), 4, .3);
            struck = true;
        }
        TokenFx.sound(level, position(), struck ? SoundEvents.CHAIN_HIT : SoundEvents.CHAIN_STEP, .7f, 1.5f + random.nextFloat() * .3f);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
