package com.waylays.arcanes.spells.acid;

import com.waylays.arcanes.entity.acid.AcidScene;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Acid Rain: a sickly cloud gathers over the spot and rains acid, burning, weakening and corroding all below. */
@AutoSpellConfig
public class AcidRainSpell extends ElementSpell {
    public AcidRainSpell() {
        super("acid_rain", ModSchools.ACID_RESOURCE, SpellRarity.RARE, 5, 25, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 15;
        this.baseManaCost = 70;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.acid.acid_rain", Utils.stringTruncation(power * .5f, 1), Utils.timeFromTicks(AcidScene.RAIN_TIME, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            AcidScene.spawn(server, entity, this, AcidScene.Kind.RAIN, SpellUtil.aimedSpot(level, entity, 24), entity.getYRot(), 0, null, AcidScene.RAIN_TIME,
                    getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
