package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Hurricane (Wind Magic's ultimate): a great storm wheels round the caster, moving with them; they and their allies
 * stand in its calm eye, where no shot reaches them. Every enemy caught in its wall is swept round it, lifted and
 * battered every second; at its end it flings them all away.
 */
public class Hurricane extends SpellEffectEntity {
    public static final float EYE = 4;
    public static final float RADIUS = 12;
    public static final float HEIGHT = 14;
    public static final int LIFE = 300;
    public static final int FORM = 30;
    public static final int FADE = 30;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(Hurricane.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public Hurricane(EntityType<? extends Hurricane> entityType, Level level) {
        super(entityType, level);
    }

    public static Hurricane raise(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        Hurricane h = new Hurricane(ModEntities.HURRICANE.get(), level);
        h.setOwner(caster);
        h.spellId = spell.getSpellId();
        h.damage = damage;
        h.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        h.entityData.set(DATA_OWNER, caster.getId());
        level.addFreshEntity(h);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 3, .4f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2, .5f);
        return h;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
    }

    @Nullable
    public Entity follows() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public float grow(float age) {
        return Mth.clamp(age / FORM, 0, 1);
    }

    public float fade(float age) {
        return Mth.clamp((age - LIFE) / FADE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = follows();
        if (owner != null && tickCount < LIFE) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > LIFE + FADE) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        Vec3 c = position();
        if (tickCount == LIFE) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, HEIGHT * .4, 0), RADIUS + 4, caster, x -> true)) {
                Vec3 out = e.position().subtract(c).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                WindFx.throwBy(e, out.scale(2).add(0, .7, 0));
            }
            WindFx.gust(level, c.add(0, 2, 0), 40, RADIUS * .5);
            return;
        }
        if (tickCount > LIFE) {
            return;
        }
        if (tickCount % 20 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 2.5f, .4f + .1f * level.random.nextFloat());
            level.playSound(null, c.x, c.y, c.z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.2f, .6f);
        }
        // The calm eye: allies there drift down softly and no shot reaches them
        AABB eye = new AABB(c, c).inflate(EYE, HEIGHT * .5, EYE).move(0, HEIGHT * .4, 0);
        WindFx.deflect(level, new AABB(c, c).inflate(EYE + 1.5, HEIGHT * .6, EYE + 1.5).move(0, HEIGHT * .4, 0), c.add(0, 1, 0), caster, 1);
        if (tickCount % 20 == 0) {
            for (Player p : level.getEntitiesOfClass(Player.class, eye, x -> x == caster || SpellUtil.isAlly(caster, x))) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
            }
            if (caster != null) {
                caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
            }
        }
        if (tickCount < FORM / 2) {
            return;
        }
        // Its wall: everything there swept round, held in it, lifted, battered
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        float reach = EYE + (RADIUS - EYE) * (.3f + .7f * grow(tickCount));
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, HEIGHT * .4, 0), reach + HEIGHT * .5, caster,
                x -> x.getY() > c.y - 2 && x.getY() < c.y + HEIGHT && Math.hypot(x.getX() - c.x, x.getZ() - c.z) < reach + 1.5)) {
            Vec3 to = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
            double d = Math.max(.5, to.length());
            Vec3 in = to.scale(1 / d);
            Vec3 round = new Vec3(-in.z, 0, in.x);
            double k = WindFx.give(e);
            // Held between the eye and the outer edge of the wall
            double mid = (EYE + 1.5 + reach) * .5;
            double hold = (d - mid) * .03;
            double up = e.getY() - c.y < HEIGHT * .55 ? .09 : -.04;
            Vec3 v = e.getDeltaMovement().scale(.6).add(round.scale(.42 * k)).add(in.scale(hold * k)).add(0, up * k + .05, 0);
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (spell != null && tickCount % 20 == 0) {
                Vec3 keep = e.getDeltaMovement();
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.setDeltaMovement(keep);
            }
        }
        if (tickCount % 4 == 0) {
            float a = tickCount * .37f;
            WindFx.dust(level, c.add(Mth.cos(a) * reach, 0, Mth.sin(a) * reach), 4, 1.5);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
