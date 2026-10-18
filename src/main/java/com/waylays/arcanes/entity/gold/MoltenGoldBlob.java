package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A gob of molten gold from Molten Torrent: burns what it hits, and leaves a glowing pool where it lands.
 */
public class MoltenGoldBlob extends GoldProjectile {
    public MoltenGoldBlob(EntityType<? extends MoltenGoldBlob> entityType, Level level) {
        super(entityType, level);
    }

    public MoltenGoldBlob(Level level, LivingEntity caster) {
        this(ModEntities.MOLTEN_GOLD_BLOB.get(), level);
        setOwner(caster);
    }

    @Override
    protected double gravity() {
        return .045;
    }

    @Override
    protected int lifetime() {
        return 40;
    }

    @Override
    protected void onImpact(HitResult hit) {
        Entity owner = getOwner();
        Vec3 at = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (!GoldFx.isAlly(owner, target) && DamageSources.applyDamage(target, getDamage(), GoldFx.damageSource(ModSpells.MOLTEN_TORRENT.get(), this, owner))) {
                target.setSecondsOnFire(3);
            }
            at = target.position();
        } else if (hit instanceof BlockHitResult blockHit && blockHit.getDirection() == Direction.UP) {
            MoltenPool.spreadAt(level(), owner instanceof LivingEntity living ? living : null, at, getDamage() * .5f);
        }
        GoldFx.particles(level(), GoldFx.MOLTEN_DUST, at.x, at.y + .1, at.z, 5, .2, .05, .2, .02);
        GoldFx.particles(level(), ParticleTypes.LAVA, at.x, at.y + .1, at.z, 1, .1, .05, .1, 0);
        if (random.nextInt(3) == 0) {
            level().playSound(null, at.x, at.y, at.z, SoundEvents.LAVA_POP, SoundSource.NEUTRAL, .8f, .8f + random.nextFloat() * .4f);
        }
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Molten gold flows through the caster's allies
        return super.canHitEntity(target) && !GoldFx.isAlly(getOwner(), target);
    }

    @Override
    protected void clientTick() {
        Level level = level();
        level.addParticle(GoldFx.MOLTEN_DUST, getX(), getY() + .1, getZ(), 0, 0, 0);
        if (random.nextInt(4) == 0) {
            level.addParticle(GoldFx.GOLD_DUST, getX(), getY() + .1, getZ(), 0, -.02, 0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SMOKE, getX(), getY() + .2, getZ(), 0, .03, 0);
        }
    }
}
