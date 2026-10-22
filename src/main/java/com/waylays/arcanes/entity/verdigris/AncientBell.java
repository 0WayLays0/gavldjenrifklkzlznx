package com.waylays.arcanes.entity.verdigris;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
 * Ancient Bell: a great old bronze bell, green with verdigris and moss, drops out of the sky onto the target. Those
 * under it are crushed and dazed by its toll, those around thrown back; it rings on, swaying, then crumbles.
 */
public class AncientBell extends SpellEffectEntity {
    public static final int FALL = 12;
    public static final float DROP = 12;
    public static final int RING = 40;
    public static final int SINK = 12;
    public static final float UNDER = 2.2f;
    public static final float AROUND = 5;

    @Nullable
    private String spellId;

    public AncientBell(EntityType<? extends AncientBell> entityType, Level level) {
        super(entityType, level);
    }

    public static AncientBell drop(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float damage) {
        AncientBell bell = new AncientBell(ModEntities.ANCIENT_BELL.get(), level);
        bell.setOwner(caster);
        bell.spellId = spell.getSpellId();
        bell.damage = damage;
        bell.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        level.addFreshEntity(bell);
        return bell;
    }

    /** Height over the ground while it falls (0 once landed). */
    public float height(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / FALL, 0, 1);
        return DROP * (1 - t * t);
    }

    /** Its swing after the toll, in degrees, dying away. */
    public float sway(float partialTicks) {
        float since = tickCount + partialTicks - FALL;
        if (since < 0) {
            return 0;
        }
        return 14 * Mth.sin(since * .5f) * (float) Math.exp(-since / 14);
    }

    /** How far it has sunk back (0 to 1). */
    public float sunk(float partialTicks) {
        return Mth.clamp((tickCount + partialTicks - FALL - RING) / SINK, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount == FALL) {
            toll((ServerLevel) level());
        }
        if (tickCount == FALL + 20) {
            VerdigrisFx.sound(level(), position(), SoundEvents.BELL_RESONATE, 1.4f, .5f);
        }
        if (tickCount > FALL + RING + SINK) {
            discard();
        }
    }

    private void toll(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        Vec3 c = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), AROUND, owner, x -> x.getY() > c.y - 2 && x.getY() < c.y + 3)) {
            boolean under = e.distanceToSqr(c.x, e.getY(), c.z) <= UNDER * UNDER;
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, under ? damage : damage * .4f, SpellUtil.damageSource(spell, this, owner));
            }
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, under ? 100 : 60, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, under ? 60 : 30, under ? 2 : 0));
            if (!under) {
                Vec3 away = e.position().subtract(c).multiply(1, 0, 1).normalize();
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().add(away.scale(.8 * resist)).add(0, .3 * resist, 0));
                e.hurtMarked = true;
            }
        }
        VerdigrisFx.debris(level, c.add(0, .3, 0), 30, 1.4);
        VerdigrisFx.sound(level, c, SoundEvents.BELL_BLOCK, 3f, .5f);
        VerdigrisFx.sound(level, c, SoundEvents.ANVIL_LAND, 1f, .6f);
        SpellUtil.cameraShake(level, c, 12, 14);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
