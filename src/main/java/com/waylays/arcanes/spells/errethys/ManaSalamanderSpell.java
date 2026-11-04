package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSalamander;
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
 * Mana Salamander: a salamander of red mana whose bite sets its prey on fire, and which breathes a gout of flame on the
 * enemies close in front of it.
 */
@AutoSpellConfig
public class ManaSalamanderSpell extends ManaSummonSpell {
    public ManaSalamanderSpell() {
        super("mana_salamander", SpellRarity.UNCOMMON, 6, 25);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 40;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_SALAMANDER.get();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        List<MutableComponent> info = new ArrayList<>(super.getUniqueInfo(spellLevel, caster));
        info.add(Component.translatable("ui.arcanes_oublies.bite_burns", Utils.stringTruncation(ManaSalamander.BURN_SECONDS, 0)));
        info.add(Component.translatable("ui.arcanes_oublies.fire_breath", Utils.timeFromTicks(ManaSalamander.BREATH_EVERY, 1)));
        return info;
    }

    /** 30 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 500 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 12;
    }
}
