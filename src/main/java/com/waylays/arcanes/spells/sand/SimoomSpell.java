package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.entity.sand.SandLance;
import com.waylays.arcanes.entity.sand.SandScene;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Simoom (ultimate): a towering storm of sand rises where the caster aims and crawls after their gaze for twelve
 * seconds, drawing enemies in, whirling and blinding them, lightning in it scarring the ground with glass.
 */
@AutoSpellConfig
public class SimoomSpell extends SandSpell {
    public static final double RANGE = 28;
    public static final float RADIUS = 5;
    public static final int LIFE = 240;

    public SimoomSpell() {
        super("simoom", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 16;
        this.spellPowerPerLevel = 4;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(whirl(spellLevel, caster), 1), Utils.timeFromTicks(10, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 16, 1)));
    }

    public float whirl(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .2f;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ELYTRA_FLYING);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 3 == 0) {
            SandFx.sand(level, entity.position().add(0, .3, 0), 8, 1.8);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            SandScene.spawn(server, entity, this, SandScene.Kind.SIMOOM, at, new Vector3f(0, 1, 0), RADIUS, LIFE, whirl(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.2f, .6f);
            SpellUtil.cameraShake(level, at, 20, 20);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
