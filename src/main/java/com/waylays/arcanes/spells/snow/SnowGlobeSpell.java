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

/** Snow Globe: a dome of glass closes over an enemy; snow whirls and freezes inside, no one gets out, then it shatters. */
@AutoSpellConfig
public class SnowGlobeSpell extends ElementSpell {
    public SnowGlobeSpell() {
        super("snow_globe", ModSchools.SNOW_RESOURCE, SpellRarity.UNCOMMON, 6, 18, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * 1.4f, 1)),
                Component.translatable("ui.arcanes_oublies.snow.snow_globe", Utils.timeFromTicks(SnowScene.GLOBE_TIME - 12, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.aimedSpot(level, entity, 24);
            SnowScene.spawn(server, entity, this, SnowScene.Kind.GLOBE, at, entity.getYRot(), SnowScene.GLOBE_TIME, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
