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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Toppling Wall: an ancient wall of verdigrised copper, mossy and hung with vines, bursts out of the ground before the
 * caster, sways, and falls forward flat on the enemies in front of it (crushed and slowed); then it crumbles away.
 */
public class ToppleWall extends SpellEffectEntity {
    public static final float WIDTH = 5;
    public static final float HEIGHT = 3.5f;
    public static final int RISE = 8;
    public static final int SWAY = 10;
    public static final int FALL = 8;
    public static final int LIE = 30;
    public static final int SINK = 12;
    public static final int IMPACT = RISE + SWAY + FALL;

    @Nullable
    private String spellId;

    public ToppleWall(EntityType<? extends ToppleWall> entityType, Level level) {
        super(entityType, level);
    }

    public static ToppleWall raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float yaw, float damage) {
        ToppleWall wall = new ToppleWall(ModEntities.TOPPLE_WALL.get(), level);
        wall.setOwner(caster);
        wall.spellId = spell.getSpellId();
        wall.damage = damage;
        wall.moveTo(at.x, at.y, at.z, yaw, 0);
        level.addFreshEntity(wall);
        return wall;
    }

    /** How far out of the ground (0 to 1). */
    public float risen(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / RISE, 0, 1);
        float down = Mth.clamp((tickCount + partialTicks - IMPACT - LIE) / SINK, 0, 1);
        return (1 - (1 - t) * (1 - t)) * (1 - down);
    }

    /** How far it has fallen forward, in degrees (0 standing, 90 flat), with a sway before it goes. */
    public float fallen(float partialTicks) {
        float age = tickCount + partialTicks;
        if (age < RISE + SWAY) {
            float s = Mth.clamp((age - RISE) / SWAY, 0, 1);
            return 6 * Mth.sin(s * Mth.PI * 2) * s;
        }
        float t = Mth.clamp((age - RISE - SWAY) / FALL, 0, 1);
        return 90 * t * t;
    }

    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount == 1) {
            VerdigrisFx.rise(level, position(), 1.6f);
            VerdigrisFx.debris(level, position().add(0, .3, 0), 30, WIDTH * .4);
        }
        if (tickCount == RISE + SWAY) {
            VerdigrisFx.sound(level, position(), SoundEvents.COPPER_HIT, 1.5f, .5f);
        }
        if (tickCount == IMPACT) {
            impact(level);
        }
        if (tickCount > IMPACT + LIE + SINK) {
            discard();
        }
    }

    /** The wall lands flat: everything under it is crushed. */
    private void impact(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        Vec3 f = forward();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 c = position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(HEIGHT + WIDTH, 3, HEIGHT + WIDTH))) {
            if (!e.isAlive() || SpellUtil.isAlly(owner, e)) {
                continue;
            }
            Vec3 d = e.position().subtract(c);
            double along = d.dot(f);
            double across = Math.abs(d.dot(side));
            if (along < -.3 || along > HEIGHT + .6 || across > WIDTH * .5 + e.getBbWidth() * .5 || d.y < -1.5 || d.y > 2.5) {
                continue;
            }
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, owner));
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(0, -.3, 0));
            e.hurtMarked = true;
        }
        for (int i = 0; i <= 4; i++) {
            Vec3 p = c.add(f.scale(HEIGHT * i / 4.0));
            VerdigrisFx.debris(level, p.add(0, .3, 0), 10, WIDTH * .35);
            VerdigrisFx.chips(level, p.add(0, .3, 0), Blocks.MOSS_BLOCK.defaultBlockState(), 6, WIDTH * .35);
        }
        VerdigrisFx.sound(level, c, SoundEvents.ANVIL_LAND, 1.2f, .45f);
        VerdigrisFx.sound(level, c, SoundEvents.COPPER_BREAK, 1.6f, .5f);
        SpellUtil.cameraShake(level, c, 14, 14);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
