package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Geode: a geode closes round the enemy, layer on layer as geodes grow (smooth basalt outside, calcite, then
 * amethyst), open on the caster's side like a geode cut in half; amethyst crystals then grow inward and pierce the
 * prisoner, again and again; at the end the geode cracks open and bursts.
 */
public class AmethystGeode extends SpellEffectEntity {
    /** Ticks for the shell to close, then for the crystals to grow. */
    public static final int FORM = 16;
    public static final int GROW = 12;
    public static final int CRACK = 22;
    public static final int STAB_TICKS = 16;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(AmethystGeode.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(AmethystGeode.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AmethystGeode.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;

    public AmethystGeode(EntityType<? extends AmethystGeode> entityType, Level level) {
        super(entityType, level);
    }

    /** A geode round {@code target}, its open side toward the caster. */
    public static AmethystGeode close(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, int life, float damage) {
        AmethystGeode geode = new AmethystGeode(ModEntities.AMETHYST_GEODE.get(), level);
        geode.setOwner(caster);
        geode.spellId = spell.getSpellId();
        geode.damage = damage;
        Vec3 toCaster = caster.position().subtract(target.position());
        float yaw = (float) (Mth.atan2(toCaster.z, toCaster.x) * Mth.RAD_TO_DEG);
        geode.moveTo(target.getX(), target.getY(), target.getZ(), yaw, 0);
        geode.entityData.set(DATA_TARGET, target.getId());
        geode.entityData.set(DATA_LIFE, FORM + GROW + life);
        geode.entityData.set(DATA_RADIUS, Mth.clamp(Math.max(target.getBbWidth(), target.getBbHeight() * .6f) + .9f, 1.8f, 3.2f));
        level.addFreshEntity(geode);
        GemFx.sigil(level, target.position(), Gem.AMETHYST, 2.5f, FORM + 10);
        GemFx.sound(level, target.position(), SoundEvents.CALCITE_PLACE, 1.6f, .6f);
        return geode;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_LIFE, 80);
        builder.define(DATA_RADIUS, 2f);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** The geode's middle: about the prisoner's chest. */
    public Vec3 middle() {
        return position().add(0, getRadius() * .75, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int life = getLife();
        if (tickCount > life + CRACK) {
            discard();
            return;
        }
        if (tickCount > life) {
            return;
        }
        LivingEntity target = getTarget();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (target != null && target.isAlive() && tickCount < life) {
            // Held inside
            if (tickCount > FORM / 2) {
                if (target.position().distanceToSqr(position()) > .4 * .4) {
                    target.teleportTo(getX(), target.getY(), getZ());
                }
                target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, false));
            }
            if (tickCount == FORM) {
                GemFx.sound(level, middle(), SoundEvents.AMETHYST_CLUSTER_PLACE, 1.6f, .6f);
            }
            int since = tickCount - FORM - GROW;
            if (since >= 0 && since % STAB_TICKS == 0 && spell != null) {
                // The crystals pierce it
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, since == 0 ? damage : damage * .35f, SpellUtil.damageSource(spell, this, getOwner()));
                GemFx.glitter(level, target.getBoundingBox().getCenter(), Gem.AMETHYST, 12, .4);
                GemFx.sound(level, middle(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.4f, .8f + random.nextFloat() * .3f);
            }
        }
        if (tickCount == life) {
            // It cracks open and bursts
            if (target != null && target.isAlive() && spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage * .8f, SpellUtil.damageSource(spell, this, getOwner()));
            }
            GemFx.shatter(level, middle(), Gem.AMETHYST, getRadius(), 22);
            GemFx.sound(level, middle(), SoundEvents.CALCITE_BREAK, 2f, .6f);
            GemFx.sound(level, middle(), SoundEvents.DEEPSLATE_BREAK, 1.6f, .6f);
            SpellUtil.cameraShake(level, middle(), 8, 12);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
