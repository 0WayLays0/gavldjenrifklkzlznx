package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A log of Wood Magic, torn out of nothing and hurled end over end: it slams into the first enemy, knocking it far back
 * and staggering those beside it, and bursts into chips and splinters.
 */
public class FlyingLog extends SpellProjectile {
    public static final float SPEED = 1.5f;
    public static final float SPLASH = 1.8f;

    @Nullable
    private String spellId;

    public FlyingLog(EntityType<? extends FlyingLog> entityType, Level level) {
        super(entityType, level);
    }

    public static FlyingLog hurl(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        FlyingLog log = new FlyingLog(ModEntities.FLYING_LOG.get(), level);
        log.setOwner(caster);
        log.spellId = spell.getSpellId();
        log.setDamage(damage);
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(1.2)).subtract(0, .3, 0);
        log.setPos(from.x, from.y - log.getBbHeight() * .5, from.z);
        log.setDeltaMovement(look.scale(SPEED).add(0, .08, 0));
        level.addFreshEntity(log);
        WoodFx.sprout(level, from, 1);
        return log;
    }

    @Override
    protected double gravity() {
        return .03;
    }

    @Override
    protected int lifetime() {
        return 60;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (level() instanceof ServerLevel level) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            Vec3 at = hit.getLocation();
            LivingEntity struck = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
            if (spell != null) {
                Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize();
                for (LivingEntity e : SpellUtil.enemiesAround(level, at, SPLASH, getOwner(), x -> true)) {
                    boolean direct = e == struck;
                    e.invulnerableTime = 0;
                    DamageSources.applyDamage(e, direct ? damage : damage * .5f, SpellUtil.damageSource(spell, this, getOwner()));
                    double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                    e.setDeltaMovement(e.getDeltaMovement().add(push.scale((direct ? 1.4 : .6) * resist)).add(0, (direct ? .45 : .25) * resist, 0));
                    e.hurtMarked = true;
                }
                if (struck != null && struck.position().distanceTo(at) > SPLASH && !SpellUtil.isAlly(getOwner(), struck)) {
                    struck.invulnerableTime = 0;
                    DamageSources.applyDamage(struck, damage, SpellUtil.damageSource(spell, this, getOwner()));
                }
            }
            WoodFx.chips(level, at, 24, .4);
            WoodFx.crash(level, at, 1.2f);
        }
        discard();
    }
}
