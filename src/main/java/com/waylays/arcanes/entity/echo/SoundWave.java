package com.waylays.arcanes.entity.echo;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.echo.EchoFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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

/** A Sound Wave: a ring of sound flung straight ahead, passing through {@link #PIERCE} enemies, leaving Resonance in each. */
public class SoundWave extends SpellProjectile {
    public static final int PIERCE = 2;
    public static final float SPEED = 1.8f;

    private final IntOpenHashSet struck = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public SoundWave(EntityType<? extends SoundWave> entityType, Level level) {
        super(entityType, level);
    }

    public static SoundWave loose(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        SoundWave wave = new SoundWave(ModEntities.SOUND_WAVE.get(), level);
        wave.setOwner(caster);
        wave.spellId = spell.getSpellId();
        wave.setDamage(damage);
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(.8)).subtract(0, .2, 0);
        wave.setPos(from.x, from.y - wave.getBbHeight() * .5, from.z);
        wave.shoot(look, SPEED);
        level.addFreshEntity(wave);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.2f, 1.6f);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.4f);
        return wave;
    }

    @Override
    protected int lifetime() {
        return 24;
    }

    @Override
    protected double gravity() {
        return 0;
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
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity e) {
            struck.add(e.getId());
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null) {
                e.invulnerableTime = 0;
                EchoFx.hit(spell, this, getOwner(), e, damage, 1);
            }
            EchoFx.pulse(level, e.position().add(0, e.getBbHeight() * .5, 0), 8, .3);
            if (struck.size() < PIERCE) {
                return;
            }
        }
        EchoFx.pulse(level, hit.getLocation(), 10, .4);
        level.playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, SoundEvents.SCULK_CLICKING_STOP, SoundSource.PLAYERS, 1, 1.2f);
        discard();
    }

    @Override
    protected void onExpire() {
        if (level() instanceof ServerLevel level) {
            EchoFx.pulse(level, position(), 6, .3);
        }
    }
}
