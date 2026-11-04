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
 * Borealis Curtain: a curtain of the northern lights falls across the ground where the caster aims, twelve blocks long,
 * across their gaze: for eight seconds enemies in it are struck, slowed and marked, and shots flying into it are turned.
 */
@AutoSpellConfig
public class BorealisCurtainSpell extends AuroraSpell {
    public static final double RANGE = 18;
    public static final int LIFE = 175;

    public BorealisCurtainSpell() {
        super("borealis_curtain", SpellRarity.UNCOMMON, 8, 18, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public float burn(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(burn(spellLevel, caster), 1),
                        Utils.timeFromTicks(AuroraScene.CURTAIN_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.mirror_turns"),
                Component.translatable("ui.arcanes_oublies.wall_size", Utils.stringTruncation(AuroraScene.CURTAIN_LENGTH, 1),
                        Utils.stringTruncation(AuroraScene.CURTAIN_HEIGHT, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 15, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.aimedGround(level, entity, RANGE);
            Vector3f f = facing(entity);
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.CURTAIN, at, new Vector3f(-f.z, 0, f.x), AuroraScene.CURTAIN_LENGTH, LIFE, burn(spellLevel, entity));
            AuroraFx.lights(level, at.add(0, 1, 0), 24, 3);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
