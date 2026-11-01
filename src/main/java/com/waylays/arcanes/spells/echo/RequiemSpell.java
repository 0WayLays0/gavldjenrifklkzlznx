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
 * Requiem (ultimate): the deep dark rises round the caster. Sculk floods the ground fourteen blocks around, a great heart
 * of it beats over the caster, and with each of five beats a wall of sound races out over the whole field, hurting
 * every enemy and leaving Resonance in it; with the last beat, every echo bursts and the field falls silent and dark.
 */
@AutoSpellConfig
public class RequiemSpell extends EchoSpell {
    public static final float RADIUS = 14;

    public RequiemSpell() {
        super("requiem", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 3;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.waves", EchoScene.REQUIEM_WAVES, Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.per_resonance", Utils.stringTruncation(power * .8f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.WARDEN_EMERGE);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .3f;
        for (int i = 0; i < 4; i++) {
            float b = a + i * Mth.HALF_PI;
            SpellUtil.particles(level, ParticleTypes.SCULK_SOUL, entity.getX() + Mth.cos(b) * 2, entity.getY() + .3, entity.getZ() + Mth.sin(b) * 2, 1, 0, .1, 0, .02);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float power = getPower(spellLevel, entity);
            Vec3 c = SpellUtil.groundAt(level, entity.position(), 3, 1);
            int life = EchoScene.REQUIEM_FIRST + (EchoScene.REQUIEM_WAVES - 1) * EchoScene.REQUIEM_STEP + 30;
            EchoScene.spawn(server, entity, this, EchoScene.Kind.REQUIEM, c, new Vector3f(0, 1, 0), RADIUS, life, power).burst(power * .8f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 3f, .5f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 2f, .7f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
