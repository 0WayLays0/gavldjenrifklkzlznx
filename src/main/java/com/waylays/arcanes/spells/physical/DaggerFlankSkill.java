package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dagger 2, Flank: the fighter darts around the enemy they face (up to 5 blocks away) and ends up behind it, or at its
 * side if there is no room behind, then stabs. From behind the stab deals much more and leaves it bleeding.
 */
@AutoSpellConfig
public class DaggerFlankSkill extends PhysicalSkill {
    public static final double RANGE = 5;
    public static final int HIT_TICK = 3;
    public static final float BACKSTAB = 1.75f;

    public DaggerFlankSkill() {
        super("dagger_flank", SpellRarity.RARE, 5, 12);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 18;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                damageLine(getStrength(spellLevel, caster) * .7f),
                weaponLine(1f),
                Component.translatable("ui.arcanes_oublies.backstab", Utils.stringTruncation(BACKSTAB, 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1))
        );
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SLASH;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (findPrey(level, entity) != null) {
            return true;
        }
        refuse(entity, "ui.arcanes_oublies.no_target_near");
        return false;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        LivingEntity prey = findPrey(level, entity);
        if (prey != null) {
            Vec3 from = entity.position();
            Vec3 spot = flankSpot(level, entity, prey);
            if (spot != null) {
                Vec3 toPrey = prey.position().subtract(spot);
                float yaw = (float) (Mth.atan2(-toPrey.x, toPrey.z) * Mth.RAD_TO_DEG);
                if (entity instanceof ServerPlayer player) {
                    player.connection.teleport(spot.x, spot.y, spot.z, yaw, player.getXRot());
                } else {
                    entity.moveTo(spot.x, spot.y, spot.z, yaw, entity.getXRot());
                    entity.setYHeadRot(yaw);
                    entity.setYBodyRot(yaw);
                }
                // A blur along the path it darted
                for (int i = 0; i <= 6; i++) {
                    Vec3 p = from.lerp(spot, i / 6.0);
                    SpellUtil.particles(level, ParticleTypes.CLOUD, p.x, p.y + .9, p.z, 1, .1, .3, .1, .01);
                }
                Combat.groundDust(level, spot, 8, .3);
                level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .8f, 1.9f);
                level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1f, 1.5f);
            }
            SkillTimeline.after(entity, HIT_TICK, fighter -> stab(level, spellLevel, fighter, prey));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void stab(Level level, int spellLevel, LivingEntity fighter, LivingEntity prey) {
        StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 2f, 90, 50, false, StrikeTrail.STEEL);
        if (!prey.isAlive() || prey.distanceToSqr(fighter) > 3.5 * 3.5) {
            return;
        }
        Vec3 fromPrey = fighter.position().subtract(prey.position()).multiply(1, 0, 1);
        boolean behind = fromPrey.lengthSqr() > 1.0E-4 && fromPrey.normalize().dot(Combat.facing(prey)) < -.2;
        float damage = blow(spellLevel, fighter, prey, .7f, 1f) * (behind ? BACKSTAB : 1);
        if (Combat.hit(this, fighter, prey, damage, .2)) {
            if (behind) {
                Vec3 c = prey.getBoundingBox().getCenter();
                SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y, c.z, 16, .3, .4, .3, .5);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.3f, .9f);
                prey.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 80, 0, false, true, true));
            }
            level.playSound(null, prey.getX(), prey.getY(), prey.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1.3f);
        }
    }

    /** The enemy the fighter looks at within reach, else the nearest one in front. */
    @Nullable
    public static LivingEntity findPrey(Level level, LivingEntity fighter) {
        HitResult hit = Utils.raycastForEntity(level, fighter, (float) RANGE, true, .3f);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()
                && !(living instanceof ArmorStand) && !SpellUtil.isAlly(fighter, living)) {
            return living;
        }
        List<LivingEntity> ahead = Combat.inArc(fighter, RANGE, 30);
        return ahead.isEmpty() ? null : ahead.get(0);
    }

    /** Behind the prey if there is room, else at one of its sides; null if it is boxed in. */
    @Nullable
    private static Vec3 flankSpot(Level level, LivingEntity fighter, LivingEntity prey) {
        Vec3 facing = Combat.facing(prey);
        Vec3 side = new Vec3(-facing.z, 0, facing.x);
        double gap = prey.getBbWidth() * .5 + fighter.getBbWidth() * .5 + .4;
        for (Vec3 offset : new Vec3[]{facing.scale(-gap), side.scale(gap), side.scale(-gap)}) {
            Vec3 spot = SpellUtil.groundAt(level, prey.position().add(offset), 2, 2);
            if (level.noCollision(fighter, fighter.getBoundingBox().move(spot.subtract(fighter.position())))) {
                return spot;
            }
        }
        return null;
    }
}
