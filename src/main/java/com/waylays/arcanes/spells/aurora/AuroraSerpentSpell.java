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
 * Aurora Serpent: a serpent woven of the northern lights rises over the caster and circles the sky for ten seconds; every
 * two seconds it dives on the nearest enemy, striking everything where it lands and tossing it up.
 */
@AutoSpellConfig
public class AuroraSerpentSpell extends AuroraSpell {
    public static final int LIFE = 220;

    public AuroraSerpentSpell() {
        super("aurora_serpent", SpellRarity.EPIC, 5, 45, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    public float dive(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .7f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.dive_damage", Utils.stringTruncation(dive(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.dives", (LIFE - 20 - AuroraScene.SERPENT_FIRST) / AuroraScene.SERPENT_EVERY + 1),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(AuroraScene.SERPENT_RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.SERPENT, at, new Vector3f(0, 1, 0), 1, LIFE, dive(spellLevel, entity));
            AuroraFx.lights(level, entity.position().add(0, 2, 0), 30, 1.5);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2f, 1.5f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
