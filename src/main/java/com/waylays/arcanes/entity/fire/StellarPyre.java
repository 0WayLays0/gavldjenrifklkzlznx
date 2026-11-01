package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.fire.FireFx;
import com.waylays.arcanes.spells.fire.HeatColors;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Pyre (Stellar Fire's ultimate): a towering vortex of fire of its heat rises round the caster and moves with
 * them, flaring higher six times ({@link #STAGE} ticks apart); every enemy in it burns and is drawn in. The caster
 * stands unburnt in its eye. At the end it bursts in a supernova of the same colour.
 */
public class StellarPyre extends SpellEffectEntity {
    public static final int STAGE = 40;
    public static final int LIFE = STAGE * HeatColors.TIERS;
    public static final int FADE = 16;
    public static final float RADIUS = 6;
    public static final float HEIGHT = 14;
    public static final float NOVA = 10;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(StellarPyre.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(StellarPyre.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public StellarPyre(EntityType<? extends StellarPyre> entityType, Level level) {
        super(entityType, level);
    }

    public static StellarPyre kindle(Level level, LivingEntity caster, AbstractSpell spell, float damage, int tier) {
        StellarPyre pyre = new StellarPyre(ModEntities.STELLAR_PYRE.get(), level);
        pyre.setOwner(caster);
        pyre.spellId = spell.getSpellId();
        pyre.damage = damage;
        pyre.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        pyre.entityData.set(DATA_OWNER, caster.getId());
        pyre.entityData.set(DATA_TIER, HeatColors.clamp(tier));
        level.addFreshEntity(pyre);
        FireFx.sound(level, caster.position(), SoundEvents.BLAZE_SHOOT, 3, .4f);
        FireFx.sound(level, caster.position(), SoundEvents.WITHER_SPAWN, .6f, 1.8f);
        return pyre;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_TIER, 0);
    }

    /** Client and server: the caster it moves with. */
    @Nullable
    public Entity follows() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    /** The heat it burns at. */
    public int getTier() {
        return this.entityData.get(DATA_TIER);
    }

    /** How many times it has flared at {@code age} (0 to 5): it climbs higher with each. */
    public static int stage(float age) {
        return HeatColors.clamp((int) (age / STAGE));
    }

    /** How far into its stage it is at {@code age} (0 to 1). */
    public static float step(float age) {
        return Math.min(age, LIFE - .01f) % STAGE / STAGE;
    }

    public static float grow(float age) {
        return Mth.clamp(age / 20, 0, 1);
    }

    public static float faded(float age) {
        return Mth.clamp((age - LIFE) / FADE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > LIFE + FADE) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster != null && caster.isAlive() && tickCount < LIFE) {
            setPos(caster.getX(), caster.getY(), caster.getZ());
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        int tier = getTier();
        float rising = .55f + .09f * stage(tickCount);
        if (tickCount == LIFE) {
            Supernova.at(level, position(), NOVA * (.7f + .06f * tier), tier);
            for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), NOVA, caster, x -> true)) {
                FireFx.burn(e, damage * 3, spell, this, caster, tier);
            }
            return;
        }
        if (tickCount > LIFE) {
            return;
        }
        if (tickCount % STAGE == 0) {
            FireFx.burst(level, position().add(0, 2, 0), 2.5f, tier);
        }
        if (caster != null && tickCount % 20 == 0) {
            caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
            caster.clearFire();
        }
        if (tickCount % 10 == 0 && tickCount > 10) {
            Vec3 c = position();
            for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, HEIGHT * .4, 0), HEIGHT * .6, caster,
                    x -> Math.hypot(x.getX() - c.x, x.getZ() - c.z) < RADIUS + 1 && x.getY() > c.y - 2 && x.getY() < c.y + HEIGHT)) {
                FireFx.burn(e, damage * rising, spell, this, caster, tier);
                Vec3 in = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
                double d = in.length();
                if (d > 1.5) {
                    double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                    Vec3 round = new Vec3(-in.z, 0, in.x).scale(1 / d);
                    e.setDeltaMovement(e.getDeltaMovement().add(in.scale(.12 * resist / d)).add(round.scale(.25 * resist)).add(0, .08, 0));
                    e.hurtMarked = true;
                }
            }
        }
        if (tickCount % 3 == 0) {
            float a = tickCount * .4f;
            FireFx.flames(level, position().add(Mth.cos(a) * RADIUS * .7, 1 + (tickCount % 12) * .8, Mth.sin(a) * RADIUS * .7), 4, .5, tier);
        }
        if (tickCount % 20 == 0) {
            FireFx.sound(level, position(), SoundEvents.FIRE_AMBIENT, 3, .5f + .1f * tier);
            FireFx.sound(level, position(), SoundEvents.BLAZE_BURN, 2, .6f + .1f * tier);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
