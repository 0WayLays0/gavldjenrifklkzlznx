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
 * Boreal Blessing: curtains of the northern lights come down round the caster: the caster and their side close by are
 * healed, take a hurt off, regenerate and are shielded a while.
 */
@AutoSpellConfig
public class BorealBlessingSpell extends AuroraSpell {
    public static final float RADIUS = 8;

    public BorealBlessingSpell() {
        super("boreal_blessing", SpellRarity.RARE, 6, 35, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    public float heal(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .8f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.blessing_heal", Utils.stringTruncation(heal(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.blessing"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.BLESSING, SpellUtil.groundAt(level, entity.position(), 2, 1), new Vector3f(0, 1, 0), RADIUS, 60,
                    heal(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
