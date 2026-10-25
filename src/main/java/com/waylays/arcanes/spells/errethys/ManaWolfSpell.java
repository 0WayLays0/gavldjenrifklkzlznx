package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Mana Wolf: a heavy, maned wolf of mana that bites hard. */
@AutoSpellConfig
public class ManaWolfSpell extends ManaSummonSpell {
    public ManaWolfSpell() {
        super("mana_wolf", SpellRarity.RARE, 5, 30);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 2;
        this.castTime = 25;
        this.baseManaCost = 55;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_WOLF.get();
    }

    /** It bites half again as hard as the spell's power. */
    @Override
    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.5f;
    }

    /** 35 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 600 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 16;
    }
}
