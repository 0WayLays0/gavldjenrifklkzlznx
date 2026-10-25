package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaFox;
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

/** Mana Fox: a very fast fox of sky-blue mana whose bite leaves its prey bleeding. */
@AutoSpellConfig
public class ManaFoxSpell extends ManaSummonSpell {
    public ManaFoxSpell() {
        super("mana_fox", SpellRarity.UNCOMMON, 6, 25);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 40;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_FOX.get();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        List<MutableComponent> info = new ArrayList<>(super.getUniqueInfo(spellLevel, caster));
        info.add(Component.translatable("ui.arcanes_oublies.bite_bleeds", Utils.timeFromTicks(ManaFox.BLEED_TICKS, 1)));
        return info;
    }

    /** 30 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 500 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 8;
    }
}
