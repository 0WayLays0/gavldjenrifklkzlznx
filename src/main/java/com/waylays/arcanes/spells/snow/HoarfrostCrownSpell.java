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

/** Hoarfrost Crown: a crown of ice crystals turns over the caster's head, loosing an icicle at the nearest enemy now and then. */
@AutoSpellConfig
public class HoarfrostCrownSpell extends ElementSpell {
    public HoarfrostCrownSpell() {
        super("hoarfrost_crown", ModSchools.SNOW_RESOURCE, SpellRarity.RARE, 5, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * .8f, 1)),
                Component.translatable("ui.arcanes_oublies.snow.hoarfrost_crown", Utils.timeFromTicks(SnowScene.CROWN_TIME, 1),
                        Utils.timeFromTicks(SnowScene.CROWN_EVERY, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            SnowScene.spawn(server, entity, this, SnowScene.Kind.CROWN, entity.position().add(0, entity.getBbHeight() + .35, 0), entity.getYRot(),
                    SnowScene.CROWN_TIME, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
