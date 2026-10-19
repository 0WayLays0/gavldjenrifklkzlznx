package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSerpent;
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

/** Mana Serpent: a serpent of mana whose bite carries a venom (poison, a short slowness). */
@AutoSpellConfig
public class ManaSerpentSpell extends ManaSummonSpell {
    public ManaSerpentSpell() {
        super("mana_serpent", SpellRarity.RARE, 5, 35);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 2;
        this.castTime = 25;
        this.baseManaCost = 60;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_SERPENT.get();
    }

    @Override
    protected void configure(ManaSummon summon, int spellLevel) {
        if (summon instanceof ManaSerpent serpent) {
            serpent.setVenom(getVenom(spellLevel));
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        List<MutableComponent> info = new ArrayList<>(super.getUniqueInfo(spellLevel, caster));
        info.add(Component.translatable("ui.arcanes_oublies.venom", getVenom(spellLevel) + 1));
        return info;
    }

    /** 40 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 700 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 16;
    }

    /** Poison amplifier: Poison I, Poison II from level 4. */
    public int getVenom(int spellLevel) {
        return spellLevel >= 4 ? 1 : 0;
    }
}
