package com.waylays.arcanes.entity.rune;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.rune.RuneFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A shard of carved stone, its rune glowing: it goes through enemies (a set number of them) and shatters on stone. */
public class RuneShard extends SpellProjectile {
    private final IntOpenHashSet struck = new IntOpenHashSet();
    private int pierce;

    public RuneShard(EntityType<? extends RuneShard> entityType, Level level) {
        super(entityType, level);
    }

    public RuneShard(Level level, LivingEntity owner, float damage, int pierce) {
        this(ModEntities.RUNE_SHARD.get(), level);
        setOwner(owner);
        setDamage(damage);
        this.pierce = pierce;
    }

    @Override
    protected double gravity() {
        return .012;
    }

    @Override
    protected int lifetime() {
        return 40;
    }

    @Override
    protected boolean piercesEntities() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !struck.contains(target.getId()) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            struck.add(target.getId());
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
                DamageSources.applyDamage(living, damage, SpellUtil.damageSource(ModSpells.RUNE_SHARDS.get(), this, getOwner()));
                RuneFx.glyphs(level(), hit.getLocation(), 6, .2);
            }
            if (pierce-- <= 0) {
                shatter(hit.getLocation());
            }
        } else {
            shatter(hit.getLocation());
        }
    }

    private void shatter(Vec3 at) {
        RuneFx.stone(level(), at, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 12, .2);
        RuneFx.glyphs(level(), at, 4, .2);
        RuneFx.sound(level(), at, SoundEvents.DEEPSLATE_BRICKS_BREAK, .9f, 1.3f);
        RuneFx.sound(level(), at, SoundEvents.AMETHYST_CLUSTER_BREAK, .6f, 1.4f);
        discard();
    }

    @Override
    protected void onExpire() {
        RuneFx.stone(level(), position(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 6, .1);
    }

    @Override
    protected void clientTick() {
        level().addParticle(RuneFx.GLOW_DUST, getX(), getY() + getBbHeight() * .5, getZ(), 0, 0, 0);
    }
}
