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
 * Sculk Sensor: the caster sets a sensor of sculk on the ground; for twenty seconds it listens, and whenever an enemy
 * moves near it, it looses a quivering bolt of sound at it, once every second and a half. Each caster keeps two at most.
 */
@AutoSpellConfig
public class SculkSensorSpell extends EchoSpell {
    public static final double RANGE = 12;
    public static final int LIFE = 400;
    public static final int MOST = 2;

    public SculkSensorSpell() {
        super("sculk_sensor", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(EchoScene.SENSOR_RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            // The oldest of the caster's sensors goes when one too many is set
            List<EchoScene> mine = server.getEntitiesOfClass(EchoScene.class, entity.getBoundingBox().inflate(128),
                    s -> s.getKind() == EchoScene.Kind.SENSOR && entity.getUUID().equals(s.getOwnerUUID()));
            mine.sort((a, b) -> Integer.compare(b.tickCount, a.tickCount));
            for (int i = 0; i + 1 < mine.size() + 1 && mine.size() - i >= MOST; i++) {
                mine.get(i).discard();
            }
            Vec3 at = SpellUtil.aimedGround(level, entity, RANGE);
            EchoScene.spawn(server, entity, this, EchoScene.Kind.SENSOR, at, new Vector3f(0, 1, 0), 1, LIFE, getPower(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SCULK_SENSOR_PLACE, SoundSource.PLAYERS, 1.4f, .8f);
            EchoFx.pulse(level, at.add(0, .4, 0), 12, .5);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
