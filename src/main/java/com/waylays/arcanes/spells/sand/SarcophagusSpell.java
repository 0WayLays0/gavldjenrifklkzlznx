package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.entity.sand.SandScene;
import com.waylays.arcanes.entity.sand.GlassShard;
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
 * Sarcophagus: bands of old linen whip up out of the sand and bind the foe the caster looks at for four seconds: it
 * cannot move, it withers every second, and when the curse ends the bands tear loose for a last, harder blow.
 */
@AutoSpellConfig
public class SarcophagusSpell extends SandSpell {
    public static final double RANGE = 20;
    public static final int LIFE = 90;

    public SarcophagusSpell() {
        super("sarcophagus", SpellRarity.EPIC, 5, 28, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_per_second", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.burst_damage", Utils.stringTruncation(power * 2, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 10, 1)));
    }

    /** The foe looked at, or the one nearest the spot aimed at. */
    @Nullable
    public static LivingEntity victim(Level level, LivingEntity caster) {
        LivingEntity aimed = SpellUtil.aimedEnemy(level, caster, RANGE);
        if (aimed != null) {
            return aimed;
        }
        Vec3 spot = SpellUtil.aimedGround(level, caster, RANGE);
        LivingEntity best = null;
        double bestD = 16;
        for (LivingEntity e : SpellUtil.enemiesAround(level, spot, 4, caster, x -> true)) {
            double d = e.distanceToSqr(spot);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            LivingEntity victim = victim(level, entity);
            if (victim != null) {
                SandScene.spawn(server, entity, this, SandScene.Kind.SARCOPHAGUS, victim.position(), new Vector3f(0, 1, 0), 1, LIFE, getPower(spellLevel, entity))
                        .follow(victim);
                SandFx.sand(level, victim.position().add(0, .5, 0), 24, .5);
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.6f, .5f);
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.HUSK_AMBIENT, SoundSource.PLAYERS, 1.4f, .6f);
            } else {
                Vec3 spot = SpellUtil.aimedGround(level, entity, RANGE);
                SandFx.sand(level, spot.add(0, .5, 0), 16, .5);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
