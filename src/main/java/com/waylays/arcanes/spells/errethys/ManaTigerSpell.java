package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.entity.errethys.ManaTiger;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mana Tiger: a great cat of mana with glowing stripes that pounces on its prey from afar (a heavy blow that slows it)
 * and rakes it with its claws.
 */
@AutoSpellConfig
public class ManaTigerSpell extends ManaSummonSpell {
    public ManaTigerSpell() {
        super("mana_tiger", SpellRarity.EPIC, 5, 45);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 75;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_TIGER.get();
    }

    /** Its claws strike a third again as hard as the spell's power; its pounce half again as hard as that. */
    @Override
    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.35f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        List<MutableComponent> info = new ArrayList<>(super.getUniqueInfo(spellLevel, caster));
        info.add(Component.translatable("ui.arcanes_oublies.pounce",
                Utils.stringTruncation(getDamage(spellLevel, caster) * ManaTiger.POUNCE_DAMAGE, 1), Utils.timeFromTicks(ManaTiger.POUNCE_EVERY, 1)));
        return info;
    }

    /** 30 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 500 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 26 + spellLevel * 2;
    }
}
