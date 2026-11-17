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

/** Verdant Maw: a huge carnivorous flower bursts from the ground under the enemy, snaps shut and chews, feeding its caster. */
@AutoSpellConfig
public class VerdantMawSpell extends ElementSpell {
    public VerdantMawSpell() {
        super("verdant_maw", ModSchools.PLANT_RESOURCE, SpellRarity.LEGENDARY, 3, 110, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 25;
        this.baseManaCost = 150;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * 2.6f, 1)), Component.translatable("ui.arcanes_oublies.plant.verdant_maw", Utils.timeFromTicks(PlantScene.MAW_TIME - PlantScene.MAW_SNAP, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            PlantScene.spawn(server, entity, this, PlantScene.Kind.MAW, SpellUtil.aimedSpot(level, entity, 24), entity.getYRot(), PlantScene.MAW_TIME, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
