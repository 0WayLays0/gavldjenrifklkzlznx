package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Mana Crow: a crow of mana that circles above its summoner and swoops down to peck at its enemies. */
@AutoSpellConfig
public class ManaCrowsSpell extends ManaSummonSpell {
    public ManaCrowsSpell() {
        super("mana_crows", SpellRarity.RARE, 5, 30);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 2;
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

    /** 40 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 700 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 10 + spellLevel * 2;
    }

    @Override
    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .8f;
    }
}
