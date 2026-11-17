package com.waylays.arcanes.spells.snow;

import com.waylays.arcanes.entity.snow.SnowScene;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.ElementSpell;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Frost Glide: the caster glides ahead on a ribbon of ice that stays behind, chilling and slowing whoever crosses it. */
@AutoSpellConfig
public class FrostGlideSpell extends ElementSpell {
    public FrostGlideSpell() {
        super("frost_glide", ModSchools.SNOW_RESOURCE, SpellRarity.UNCOMMON, 5, 10, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.snow.frost_glide", Math.round(SnowScene.TRAIL_LENGTH), Utils.timeFromTicks(SnowScene.TRAIL_TIME, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            SnowScene.spawn(server, entity, this, SnowScene.Kind.TRAIL, SpellUtil.groundAt(level, entity.position(), 3, 1), entity.getYRot(), SnowScene.TRAIL_TIME,
                    getPower(spellLevel, entity));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false, true));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
