package com.waylays.arcanes.spells.plant;

import com.waylays.arcanes.entity.plant.PlantScene;
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

/** Pollen Cloud: golden pollen in which enemies sneeze and weaken while the caster and their allies heal. */
@AutoSpellConfig
public class PollenCloudSpell extends ElementSpell {
    public PollenCloudSpell() {
        super("pollen_cloud", ModSchools.PLANT_RESOURCE, SpellRarity.UNCOMMON, 6, 20, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.plant.pollen_cloud", Math.round(PlantScene.POLLEN_R), Utils.timeFromTicks(PlantScene.POLLEN_TIME, 1), Utils.stringTruncation(power * .15f, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            PlantScene.spawn(server, entity, this, PlantScene.Kind.POLLEN, SpellUtil.aimedSpot(level, entity, 16), entity.getYRot(), PlantScene.POLLEN_TIME, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
