package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.world.entity.EntityType;

/** Mana Salmon: a salmon of mana that swims through water and air alike and rams its summoner's enemies. */
@AutoSpellConfig
public class ManaSalmonSpell extends ManaSummonSpell {
    public ManaSalmonSpell() {
        super("mana_salmon", SpellRarity.UNCOMMON, 6, 25);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 2;
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

    /** 30 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 500 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 12 + spellLevel * 2;
    }
}
