package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Howling Skull: a great skull wreathed in crimson, its jaws snapping, flies after the enemy it was loosed at,
 * howling; where it strikes it bursts into a spray of splinters, hurting all round, and those it spares are left
 * weak, sick and slow with fear.
 */
public class HowlingSkull extends SpellProjectile {
    public static final float SPEED = .7f;
    public static final float RADIUS = 3.5f;
    public static final int SPLINTERS = 10;

    @Nullable
    private String spellId;
    private int target = -1;

    public HowlingSkull(EntityType<? extends HowlingSkull> entityType, Level level) {
        super(entityType, level);
    }

    public static HowlingSkull loose(Level level, LivingEntity caster, AbstractSpell spell, @Nullable LivingEntity target, float damage) {
        HowlingSkull skull = new HowlingSkull(ModEntities.HOWLING_SKULL.get(), level);
        skull.setOwner(caster);
        skull.spellId = spell.getSpellId();
        skull.target = target == null ? -1 : target.getId();
        skull.setDamage(damage);
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(1.2));
        skull.setPos(from.x, from.y - skull.getBbHeight() * .5, from.z);
        skull.setDeltaMovement(look.scale(SPEED));
        level.addFreshEntity(skull);
        BoneFx.sound(level, from, SoundEvents.WITHER_SKELETON_AMBIENT, 1.4f, .6f);
        return skull;
    }

    @Override
    protected int lifetime() {
        return 100;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            Entity t = target >= 0 ? level().getEntity(target) : null;
            if (t instanceof LivingEntity living && living.isAlive()) {
                Vec3 want = living.getBoundingBox().getCenter().subtract(position()).normalize().scale(SPEED);
                setDeltaMovement(getDeltaMovement().scale(.88).add(want.scale(.12)).normalize().scale(SPEED));
                hasImpulse = true;
            }
            if (tickCount % 12 == 1) {
                BoneFx.sound(level(), position(), SoundEvents.GHAST_WARN, .6f, .5f);
            }
        }
        super.tick();
    }

    @Override
    protected void onImpact(HitResult hit) {
        burst(hit.getLocation());
        discard();
    }

    @Override
    protected void onExpire() {
        burst(position());
    }

    private void burst(Vec3 at) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getOwner() instanceof LivingEntity living ? living : null;
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, at, RADIUS, caster, x -> true)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.scale(.7 * resist)).add(0, .35 * resist, 0));
                e.hurtMarked = true;
            }
            if (caster != null) {
                // A spray of splinters out of the burst
                for (int i = 0; i < SPLINTERS; i++) {
                    float a = i * Mth.TWO_PI / SPLINTERS + random.nextFloat() * .4f;
                    Vec3 dir = new Vec3(Mth.cos(a), .25 + random.nextFloat() * .4, Mth.sin(a)).normalize();
                    BoneLance.throwBone(level, caster, spell, BoneLance.Kind.SHARD, at.add(dir.scale(.6)).add(0, .3, 0), dir.scale(.9), damage * .3f);
                }
            }
        }
        BoneFx.splinters(level, at, 40, .8);
        BoneFx.marrow(level, at, 30, .8);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3f, .7f);
        BoneFx.sound(level, at, SoundEvents.WITHER_SKELETON_DEATH, 1.2f, .7f);
        SpellUtil.cameraShake(level, at, 10, 16);
    }

    @Override
    protected void clientTick() {
        Vec3 p = position().add(0, getBbHeight() * .5, 0);
        for (int i = 0; i < 2; i++) {
            level().addParticle(BoneFx.MARROW_DUST, p.x + (random.nextDouble() - .5) * .8, p.y + (random.nextDouble() - .5) * .8, p.z + (random.nextDouble() - .5) * .8, 0, 0, 0);
        }
    }
}
