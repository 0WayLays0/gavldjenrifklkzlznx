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

/**
 * Boreal Night (ultimate): chant for a moment; night falls round the caster under a dome of stars, the northern lights
 * rippling across it. Four times a great curtain of light sweeps the field, searing, slowing and marking the enemies it
 * crosses, while the caster's side regenerates; then all the lights gather into a pillar that strikes every enemy
 * under the dome, lifts them and heals the caster's side.
 */
@AutoSpellConfig
public class BorealNightSpell extends AuroraSpell {
    public static final float RADIUS = 16;
    public static final int LIFE = AuroraScene.NIGHT_GATHER + 50;

    public BorealNightSpell() {
        super("boreal_night", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 18;
        this.spellPowerPerLevel = 6;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.waves", 4, Utils.stringTruncation(power * .3f, 1)),
                Component.translatable("ui.arcanes_oublies.night_gather", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public java.util.Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return java.util.Optional.of(SoundEvents.BEACON_AMBIENT);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 4 == 0) {
            AuroraFx.lights(level, entity.position().add(0, 2.5, 0), 6, 1.2);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.NIGHT, at, new Vector3f(0, 1, 0), RADIUS, LIFE, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
