package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaKangaroo;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Mana Kangaroo: a kangaroo of mana as tall as a player that boxes the caster's enemies: heavy punches that throw them
 * back, and every third blow a fury of punches.
 */
@AutoSpellConfig
public class ManaKangarooSpell extends ManaSummonSpell {
    public ManaKangarooSpell() {
        super("mana_kangaroo", SpellRarity.RARE, 5, 35);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 25;
        this.baseManaCost = 55;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_KANGAROO.get();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        List<MutableComponent> info = new ArrayList<>(super.getUniqueInfo(spellLevel, caster));
        info.add(Component.translatable("ui.arcanes_oublies.fury_punch", ManaKangaroo.FURY_CYCLE));
        return info;
    }

    /** 35 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 600 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 20;
    }
}
