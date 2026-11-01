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
 * The Deep Choir: three shriekers of sculk rise out of the ground round the caster; for five seconds they shriek in
 * turn, each loosing a sonic boom at the enemy nearest it, hurling it back and leaving two levels of Resonance in it.
 */
@AutoSpellConfig
public class DeepChoirSpell extends EchoSpell {
    public static final int SHRIEKERS = 3;
    public static final int LIFE = 112;

    public DeepChoirSpell() {
        super("deep_choir", SpellRarity.EPIC, 5, 35, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.shriek_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.shriekers", SHRIEKERS),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(EchoScene.SHRIEK_RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float damage = getPower(spellLevel, entity);
            float turn = entity.getYRot() * Mth.DEG_TO_RAD;
            for (int k = 0; k < SHRIEKERS; k++) {
                float a = turn + k * Mth.TWO_PI / SHRIEKERS + Mth.HALF_PI;
                Vec3 at = SpellUtil.groundAt(level, entity.position().add(Mth.cos(a) * 3, 0, Mth.sin(a) * 3), 4, 1);
                // They shriek in turn, a third of the way apart
                EchoScene.spawn(server, entity, this, EchoScene.Kind.SHRIEKER, at, new Vector3f(0, 1, 0), 1, LIFE, damage)
                        .arg(-k * EchoScene.SHRIEK_EVERY / (float) SHRIEKERS);
                EchoFx.pulse(level, at.add(0, .5, 0), 14, .5);
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SCULK_SHRIEKER_PLACE, SoundSource.PLAYERS, 2f, .7f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
