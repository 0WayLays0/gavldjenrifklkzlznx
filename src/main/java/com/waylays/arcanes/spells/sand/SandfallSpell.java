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
 * Sandfall: the sky splits over the spot the caster aims at and a torrent of sand pours out of it, burying whatever is
 * under it: struck, blinded and weighed down, again and again while it pours.
 */
@AutoSpellConfig
public class SandfallSpell extends SandSpell {
    public static final double RANGE = 24;
    public static final float RADIUS = 3.5f;
    public static final int LIFE = 80;

    public SandfallSpell() {
        super("sandfall", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(pour(spellLevel, caster), 1), Utils.timeFromTicks(SandScene.POUR_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    public float pour(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            SandScene.spawn(server, entity, this, SandScene.Kind.SANDFALL, at, new Vector3f(0, 1, 0), RADIUS, LIFE, pour(spellLevel, entity));
            level.playSound(null, at.x, at.y + SandScene.SANDFALL_HEIGHT, at.z, SoundEvents.SAND_FALL, SoundSource.PLAYERS, 3f, .4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
