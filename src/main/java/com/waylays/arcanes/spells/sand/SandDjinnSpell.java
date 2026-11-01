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
 * Djinn of the Sands: a djinn of whirling sand rises at the caster's shoulder and stays there for fifteen seconds,
 * hurling bolts of sand at the nearest enemy.
 */
@AutoSpellConfig
public class SandDjinnSpell extends SandSpell {
    public static final int LIFE = 300;

    public SandDjinnSpell() {
        super("sand_djinn", SpellRarity.EPIC, 5, 45, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(getPower(spellLevel, caster), 1), Utils.timeFromTicks(SandScene.DJINN_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            SandScene.spawn(server, entity, this, SandScene.Kind.DJINN, entity.position(), new Vector3f(0, 1, 0), 1, LIFE, getPower(spellLevel, entity))
                    .follow(entity);
            SandFx.sand(level, entity.position().add(0, 1, 0), 40, .8);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.5f, .7f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 2f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
