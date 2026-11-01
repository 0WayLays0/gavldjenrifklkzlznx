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
 * Shriek: a shriek of the deep dark bursts from the caster over a cone ahead: it hurts, darkens the eyes of whoever it
 * catches for a moment, pushes them back and leaves Resonance in them.
 */
@AutoSpellConfig
public class ShriekSpell extends EchoSpell {
    public static final float LENGTH = 7;
    /** Degrees either side of ahead. */
    public static final float HALF_ANGLE = 36;

    public ShriekSpell() {
        super("shriek", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(LENGTH, 1)),
                Component.translatable("ui.arcanes_oublies.resonance_added", 1));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 eye = entity.getEyePosition();
            Vec3 look = entity.getLookAngle();
            float damage = getPower(spellLevel, entity);
            double cos = Math.cos(Math.toRadians(HALF_ANGLE));
            for (LivingEntity e : SpellUtil.enemiesAround(level, eye, LENGTH, entity, x -> true)) {
                Vec3 to = e.position().add(0, e.getBbHeight() * .5, 0).subtract(eye);
                if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) >= cos) {
                    if (EchoFx.hit(this, entity, entity, e, damage, 1)) {
                        e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                        Vec3 push = to.multiply(1, 0, 1);
                        if (push.lengthSqr() > 1.0E-4) {
                            e.setDeltaMovement(e.getDeltaMovement().add(push.normalize().scale(.45)).add(0, .15, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
            }
            EchoScene.visual(server, entity, this, EchoScene.Kind.SHRIEK, eye.add(look.scale(.4)).subtract(0, .15, 0), look.toVector3f(), LENGTH, 0);
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.6f, 1.2f);
            EchoFx.pulse(level, eye.add(look.scale(2)), 12, .6);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
