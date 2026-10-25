package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaButterfly;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Mana Butterfly: a butterfly of mana that flutters around the caster and heals them (them only) by three hearts
 * every ten seconds. It never fights.
 */
@AutoSpellConfig
public class ManaButterflySpell extends ManaSummonSpell {
    public ManaButterflySpell() {
        super("mana_butterfly", SpellRarity.UNCOMMON, 5, 40);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 20;
        this.baseManaCost = 50;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_BUTTERFLY.get();
    }

    @Override
    protected boolean airborne() {
        return true;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.heals_summoner", (int) (ManaButterfly.HEAL / 2), Utils.timeFromTicks(ManaButterfly.HEAL_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.hp", (int) getHealth(spellLevel))
        );
    }

    /** 40 s at level 1, +20 s per level: four to ten healings. */
    @Override
    public int getLifetime(int spellLevel) {
        return 400 + spellLevel * 400;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 5;
    }
}
