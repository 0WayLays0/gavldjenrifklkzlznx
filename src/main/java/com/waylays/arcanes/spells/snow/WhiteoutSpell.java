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

/** Whiteout: a wall of blizzard whirls round the caster, blinding and freezing enemies inside; at its end the frozen shatter. */
@AutoSpellConfig
public class WhiteoutSpell extends ElementSpell {
    public WhiteoutSpell() {
        super("whiteout", ModSchools.SNOW_RESOURCE, SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 150;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.snow.whiteout", Math.round(SnowScene.WHITEOUT_R), Utils.stringTruncation(power * 2.2f, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            SnowScene.spawn(server, entity, this, SnowScene.Kind.WHITEOUT, entity.position(), entity.getYRot(), SnowScene.WHITEOUT_TIME,
                    getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
