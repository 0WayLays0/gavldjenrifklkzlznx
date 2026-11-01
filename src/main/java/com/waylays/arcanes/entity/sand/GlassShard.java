package com.waylays.arcanes.entity.sand;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.sand.SandFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** A Glass Shard: flung glass that shatters where it strikes, its splinters cutting whatever is close (half damage). */
public class GlassShard extends SpellProjectile {
    public static final float SPEED = 1.9f;
    public static final double SPLINTERS = 1.8;

    @Nullable
    private String spellId;

    public GlassShard(EntityType<? extends GlassShard> entityType, Level level) {
        super(entityType, level);
    }

    public static GlassShard fling(Level level, LivingEntity caster, AbstractSpell spell, Vec3 dir, float damage) {
        GlassShard shard = new GlassShard(ModEntities.GLASS_SHARD.get(), level);
        shard.setOwner(caster);
        shard.spellId = spell.getSpellId();
        shard.setDamage(damage);
        Vec3 from = caster.getEyePosition().add(dir.scale(.8)).subtract(0, .25, 0);
        shard.setPos(from.x, from.y - shard.getBbHeight() * .5, from.z);
        shard.shoot(dir, SPEED);
        level.addFreshEntity(shard);
        return shard;
    }

    @Override
    protected int lifetime() {
        return 30;
    }

    @Override
    protected double gravity() {
        return .02;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        LivingEntity struck = hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity living ? living : null;
        if (spell != null) {
            if (struck != null) {
                struck.invulnerableTime = 0;
                DamageSources.applyDamage(struck, damage, SpellUtil.damageSource(spell, this, owner));
            }
            // Its splinters
            for (LivingEntity e : SpellUtil.enemiesAround(level, hit.getLocation(), SPLINTERS, owner instanceof LivingEntity l ? l : null, x -> x != struck)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * .5f, SpellUtil.damageSource(spell, this, owner));
            }
        }
        SandFx.glass(level, hit.getLocation(), 12, .25);
        level.playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1, 1.1f);
        discard();
    }
}
