package com.waylays.arcanes.entity.verdigris;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Rust: a clot of rust flakes hurled at an enemy; it hurts it and rusts its armour (Rusted). */
public class RustBolt extends SpellProjectile {
    @Nullable
    private String spellId;
    private int rustTicks = 200;
    private int rustLevel;

    public RustBolt(EntityType<? extends RustBolt> entityType, Level level) {
        super(entityType, level);
    }

    public static RustBolt shoot(Level level, LivingEntity owner, AbstractSpell spell, Vec3 from, Vec3 direction, float speed, float damage, int rustTicks, int rustLevel) {
        RustBolt bolt = new RustBolt(ModEntities.RUST_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.spellId = spell.getSpellId();
        bolt.rustTicks = rustTicks;
        bolt.rustLevel = rustLevel;
        bolt.setDamage(damage);
        bolt.setPos(from.x, from.y - bolt.getBbHeight() * .5, from.z);
        bolt.shoot(direction, speed);
        level.addFreshEntity(bolt);
        return bolt;
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
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (level() instanceof ServerLevel level) {
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && spell != null) {
                DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, getOwner()));
                target.addEffect(new MobEffectInstance(ModEffects.RUSTED, rustTicks, rustLevel));
            }
            VerdigrisFx.rust(level, hit.getLocation(), 24, .4);
            VerdigrisFx.sound(level, hit.getLocation(), SoundEvents.AXE_SCRAPE, 1f, .7f);
            VerdigrisFx.sound(level, hit.getLocation(), SoundEvents.COPPER_HIT, 1f, .6f);
        }
        discard();
    }

    @Override
    protected void clientTick() {
        Vec3 p = position().add(0, getBbHeight() * .5, 0);
        level().addParticle(VerdigrisFx.RUST_DUST, p.x, p.y, p.z, 0, 0, 0);
        if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.WAX_ON, p.x, p.y, p.z, 0, 0, 0);
        }
    }
}
