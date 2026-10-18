package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.world.entity.EntityType;

/** Mana Salmon: salmon of mana that swim through water and air alike and ram their summoner's enemies. */
@AutoSpellConfig
public class ManaSalmonSpell extends ManaSummonSpell {
    public ManaSalmonSpell() {
        super("mana_salmon", SpellRarity.UNCOMMON, 6, 25);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 40;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_SALMON.get();
    }

    @Override
    protected boolean airborne() {
        return true;
    }

    /** 2 salmon, +1 every 2 levels. */
    @Override
    public int getCount(int spellLevel) {
        return 2 + (spellLevel - 1) / 2;
    }

    /** 30 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 500 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 6 + spellLevel;
    }
}
