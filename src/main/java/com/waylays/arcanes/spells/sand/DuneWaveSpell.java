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

/** Dune Wave: a wall of sand rises in front of the caster and rolls on fifteen blocks, striking and carrying off whatever it meets. */
@AutoSpellConfig
public class DuneWaveSpell extends SandSpell {
    public static final float WIDTH = 6;
    public static final int LIFE = 34;

    public DuneWaveSpell() {
        super("dune_wave", SpellRarity.RARE, 6, 16, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(SandScene.WAVE_SPEED * LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle().multiply(1, 0, 1);
            look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(look.scale(1.5)), 3, 1);
            SandScene.spawn(server, entity, this, SandScene.Kind.WAVE, at, look.toVector3f(), WIDTH, LIFE, getPower(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 2f, .4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
