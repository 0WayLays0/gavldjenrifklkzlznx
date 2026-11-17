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

/** Corroding Tide: a wave of acid rolls out in a ring round the caster, hurling back and corroding everything it passes over. */
@AutoSpellConfig
public class CorrodingTideSpell extends ElementSpell {
    public CorrodingTideSpell() {
        super("corroding_tide", ModSchools.ACID_RESOURCE, SpellRarity.LEGENDARY, 3, 100, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 2;
        this.castTime = 25;
        this.baseManaCost = 150;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * 2, 1)),
                Component.translatable("ui.arcanes_oublies.acid.corroding_tide", Math.round(AcidScene.TIDE_R)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            AcidScene.spawn(server, entity, this, AcidScene.Kind.TIDE, SpellUtil.groundAt(level, entity.position(), 3, 1), entity.getYRot(), 0, null,
                    AcidScene.TIDE_TIME, getPower(spellLevel, entity));
            SpellUtil.cameraShake(level, entity.position(), 25, 14);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
