package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Mana Crows: crows of mana that circle above their summoner and swoop down to peck at their enemies. */
@AutoSpellConfig
public class ManaCrowsSpell extends ManaSummonSpell {
    public ManaCrowsSpell() {
        super("mana_crows", SpellRarity.RARE, 5, 30);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 50;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_CROW.get();
    }

    @Override
    protected boolean airborne() {
        return true;
    }

    /** 2 crows, 3 from level 2, 4 from level 4. */
    @Override
    public int getCount(int spellLevel) {
        return 2 + spellLevel / 2;
    }

    /** 40 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 700 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 5 + spellLevel;
    }

    @Override
    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .8f;
    }
}
