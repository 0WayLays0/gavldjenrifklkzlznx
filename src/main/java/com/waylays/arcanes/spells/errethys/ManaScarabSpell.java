package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaScarab;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Mana Scarab: a small, bright, armoured beetle of mana that wards its summoner while it stays close to them (they take
 * less damage), bites their enemies and flies short hops to reach them.
 */
@AutoSpellConfig
public class ManaScarabSpell extends ManaSummonSpell {
    public ManaScarabSpell() {
        super("mana_scarab", SpellRarity.UNCOMMON, 6, 30);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 15;
        this.baseManaCost = 35;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_SCARAB.get();
    }

    @Override
    protected void configure(ManaSummon summon, int spellLevel) {
        if (summon instanceof ManaScarab scarab) {
            scarab.setWard(wardAmplifier(spellLevel));
        }
    }

    /** Resistance I, Resistance II from level 5. */
    public static int wardAmplifier(int spellLevel) {
        return spellLevel >= ManaScarab.STRONG_WARD_LEVEL ? 1 : 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        List<MutableComponent> info = new ArrayList<>(super.getUniqueInfo(spellLevel, caster));
        info.add(Component.translatable("ui.arcanes_oublies.scarab_ward", Utils.stringTruncation((wardAmplifier(spellLevel) + 1) * 20, 0),
                Utils.stringTruncation(ManaScarab.WARD_RANGE, 0)));
        return info;
    }

    /** 40 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 700 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 14 + spellLevel;
    }
}
