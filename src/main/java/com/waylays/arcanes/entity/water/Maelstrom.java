package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Maelstrom: the ground where the caster points turns to a whirlpool, a bowl of churning water turning ever faster
 * towards its heart. Enemies in it are swept round and drawn in along a spiral; the nearer the heart the harder they are
 * battered, and at its heart they are dragged under and run out of air.
 */
public class Maelstrom extends SpellEffectEntity {
    public static final float RADIUS = 5.5f;
    public static final int OPEN = 16;
    public static final int CLOSE = 16;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(Maelstrom.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public Maelstrom(EntityType<? extends Maelstrom> entityType, Level level) {
        super(entityType, level);
    }

    public static Maelstrom open(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, int life, float damage) {
        Maelstrom whirl = new Maelstrom(ModEntities.MAELSTROM.get(), level);
        whirl.setOwner(caster);
        whirl.spellId = spell.getSpellId();
        whirl.damage = damage;
        whirl.moveTo(at.x, at.y, at.z, 0, 0);
        whirl.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(whirl);
        WaterFx.crash(level, at, 1.4f);
        return whirl;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 140);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How wide open it is (0 to 1). */
    public float opened(float age) {
        float in = Mth.clamp(age / OPEN, 0, 1);
        float out = Mth.clamp((age - getLife()) / CLOSE, 0, 1);
        return (1 - (1 - in) * (1 - in)) * (1 - out);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife() + CLOSE) {
            discard();
            return;
        }
        if (tickCount % 20 == 0) {
            WaterFx.sound(level, position(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.6f, .7f);
        }
        if (tickCount % 2 == 0) {
            float a = tickCount * .5f;
            SpellUtil.particles(level, ParticleTypes.FISHING, getX() + Mth.cos(a) * RADIUS * .8, getY() + .3, getZ() + Mth.sin(a) * RADIUS * .8, 4, .4, 0, .4, .05);
            WaterFx.bubbles(level, position().add(0, .2, 0), 2, .4);
        }
        if (tickCount >= getLife() || tickCount < OPEN / 2) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), RADIUS + 1.5, caster, x -> Math.abs(x.getY() - getY()) < 2.5)) {
            Vec3 to = new Vec3(getX() - e.getX(), 0, getZ() - e.getZ());
            double d = to.length();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            Vec3 in = d < 1.0E-3 ? Vec3.ZERO : to.scale(1 / d);
            Vec3 round = new Vec3(-in.z, 0, in.x);
            double close = 1 - Mth.clamp(d / RADIUS, 0, 1);
            Vec3 v = e.getDeltaMovement().scale(.6).add(in.scale((.06 + .1 * close) * resist)).add(round.scale((.14 + .22 * close) * resist));
            e.setDeltaMovement(v.x, Math.min(v.y, close > .75 ? -.08 : v.y), v.z);
            e.hurtMarked = true;
            WaterFx.soak(e, 30);
            if (close > .75 && !e.canBreatheUnderwater()) {
                e.setAirSupply(Math.max(-20, e.getAirSupply() - 8));
            }
            if (spell != null && tickCount % 10 == 0) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * (float) (.5 + close), SpellUtil.damageSource(spell, this, caster));
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
