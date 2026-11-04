package com.waylays.arcanes.spells.aurora;

import com.waylays.arcanes.entity.aurora.AuroraScene;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Celestial Spiral: a whirling column of aurora ribbons where the caster aims, drawing enemies in, lifting them and searing them for six seconds. */
@AutoSpellConfig
public class CelestialSpiralSpell extends AuroraSpell {
    public static final double RANGE = 24;
    public static final float RADIUS = 4;
    public static final int LIFE = 135;

    public CelestialSpiralSpell() {
        super("celestial_spiral", SpellRarity.RARE, 6, 22, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public float sear(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .3f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(sear(spellLevel, caster), 1), Utils.timeFromTicks(AuroraScene.SPIRAL_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.pull_radius", Utils.stringTruncation(RADIUS + 1.5f, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 15, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.SPIRAL, at, new Vector3f(0, 1, 0), RADIUS, LIFE, sear(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1.5f, 1.3f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
