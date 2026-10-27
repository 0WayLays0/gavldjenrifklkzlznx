package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A seed pod of Wood Magic, swollen with life, lobbed in an arc: where it lands it bursts in a spray of thorns (hurting
 * and throwing back what is near) and a thicket of brambles springs up on the spot.
 */
public class SeedPod extends SpellProjectile {
    public static final float BURST = 2.6f;
    public static final float THICKET = 2.2f;
    public static final int THICKET_TICKS = 100;

    @Nullable
    private String spellId;

    public SeedPod(EntityType<? extends SeedPod> entityType, Level level) {
        super(entityType, level);
    }

    public static SeedPod lob(Level level, LivingEntity caster, AbstractSpell spell, Vec3 from, Vec3 velocity, float damage) {
        SeedPod pod = new SeedPod(ModEntities.SEED_POD.get(), level);
        pod.setOwner(caster);
        pod.spellId = spell.getSpellId();
        pod.setDamage(damage);
        pod.setPos(from.x, from.y - pod.getBbHeight() * .5, from.z);
        pod.setDeltaMovement(velocity);
        level.addFreshEntity(pod);
        return pod;
    }

    @Override
    protected double gravity() {
        return .055;
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
    protected void clientTick() {
        if (tickCount % 2 == 0) {
            level().addParticle(WoodFx.LIFE_DUST, getX(), getY() + getBbHeight() * .5, getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (level() instanceof ServerLevel level) {
            Vec3 at = hit.getLocation();
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            LivingEntity caster = getOwner() instanceof LivingEntity living ? living : null;
            if (spell != null) {
                for (LivingEntity e : SpellUtil.enemiesAround(level, at, BURST, caster, x -> true)) {
                    e.invulnerableTime = 0;
                    DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                    double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                    Vec3 out = e.position().subtract(at).multiply(1, 0, 1);
                    out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize();
                    e.setDeltaMovement(e.getDeltaMovement().add(out.scale(.5 * resist)).add(0, .3 * resist, 0));
                    e.hurtMarked = true;
                }
                Vec3 ground = SpellUtil.groundAt(level, at, 3, 1);
                BrambleHedge.grow(level, caster, spell, BrambleHedge.Mode.PATCH, ground, random.nextFloat() * 360, THICKET, THICKET_TICKS, damage * .35f);
            }
            WoodFx.chips(level, at, 14, .4);
            WoodFx.leaves(level, at.add(0, .3, 0), 18, .8);
            WoodFx.life(level, at.add(0, .3, 0), 12, .6);
            WoodFx.sound(level, at, SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.6f, .7f);
            WoodFx.sound(level, at, SoundEvents.AZALEA_LEAVES_BREAK, 1.4f, .8f);
        }
        discard();
    }
}
