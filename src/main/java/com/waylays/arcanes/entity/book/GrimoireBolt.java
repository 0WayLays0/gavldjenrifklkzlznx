package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The little spell a flying grimoire spits: a violet rune bolt. Credited to the grimoire's summoner.
 */
public class GrimoireBolt extends SpellProjectile {
    public GrimoireBolt(EntityType<? extends GrimoireBolt> entityType, Level level) {
        super(entityType, level);
    }

    public GrimoireBolt(Level level, LivingEntity owner, float damage) {
        this(ModEntities.GRIMOIRE_BOLT.get(), level);
        setOwner(owner);
        setDamage(damage);
    }

    @Override
    protected int lifetime() {
        return 40;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void clientTick() {
        Vec3 p = position();
        level().addParticle(BookFx.VIOLET_DUST, p.x, p.y, p.z, 0, 0, 0);
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.ENCHANT, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    @Override
    protected void onImpact(HitResult hit) {
        Vec3 at = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            DamageSources.applyDamage(target, damage, SpellUtil.damageSource(ModSpells.FORBIDDEN_LIBRARY.get(), this, getOwner()));
            at = target.getBoundingBox().getCenter();
        }
        SpellUtil.particles(level(), BookFx.VIOLET_DUST, at.x, at.y, at.z, 10, .2, .2, .2, 0);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, at.x, at.y, at.z, 14, .1, .1, .1, .8);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1f, 1.4f);
        discard();
    }
}
