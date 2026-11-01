package com.waylays.arcanes.spells.echo;

import com.waylays.arcanes.entity.echo.EchoScene;
import com.waylays.arcanes.entity.echo.SoundWave;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Sonic Boom: the caster gathers sound for a second, as the warden does, then looses it in a beam that goes through
 * everything in its way, hurling back whatever it hits and leaving two levels of Resonance in it.
 */
@AutoSpellConfig
public class SonicBoomSpell extends EchoSpell {
    public static final double RANGE = 24;
    public static final double WIDTH = 1.3;

    public SonicBoomSpell() {
        super("sonic_boom", SpellRarity.RARE, 6, 16, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 20;
        this.baseManaCost = 55;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(RANGE, 1)),
                Component.translatable("ui.arcanes_oublies.resonance_added", 2));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.WARDEN_SONIC_CHARGE);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        Vec3 at = entity.getEyePosition().add(entity.getLookAngle().scale(.8));
        SpellUtil.particles(level, ParticleTypes.SCULK_CHARGE_POP, at.x, at.y, at.z, 3, .25, .25, .25, .02);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 eye = entity.getEyePosition();
            Vec3 look = entity.getLookAngle();
            HitResult wall = level.clip(new ClipContext(eye, eye.add(look.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            double reach = wall.getType() == HitResult.Type.MISS ? RANGE : wall.getLocation().distanceTo(eye);
            Vec3 end = eye.add(look.scale(reach));
            float damage = getPower(spellLevel, entity);
            AABB box = new AABB(eye, end).inflate(WIDTH + 1);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !SpellUtil.isAlly(entity, x))) {
                Vec3 mid = e.position().add(0, e.getBbHeight() * .5, 0);
                double along = mid.subtract(eye).dot(look);
                if (along < 0 || along > reach) {
                    continue;
                }
                double off = mid.subtract(eye.add(look.scale(along))).length();
                if (off <= WIDTH + e.getBbWidth() * .5) {
                    e.invulnerableTime = 0;
                    if (EchoFx.hit(this, entity, entity, e, damage, 2)) {
                        e.setDeltaMovement(e.getDeltaMovement().add(look.multiply(1.2, .4, 1.2)).add(0, .25, 0));
                        e.hurtMarked = true;
                    }
                }
            }
            EchoScene.visual(server, entity, this, EchoScene.Kind.BOOM, eye.add(look.scale(.6)).subtract(0, .2, 0), look.toVector3f(), (float) reach, 1.3f);
            for (double d = 1; d < reach; d += 2) {
                Vec3 p = eye.add(look.scale(d));
                SpellUtil.particles(level, ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5f, 1);
            SpellUtil.cameraShake(level, eye, 8, 8);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
