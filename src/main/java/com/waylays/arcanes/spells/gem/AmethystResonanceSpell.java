package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.AmethystResonance;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Amethyst Resonance: clusters of amethyst ring round the caster in three waves of sound (see {@link AmethystResonance}). */
@AutoSpellConfig
public class AmethystResonanceSpell extends GemSpell {
    public AmethystResonanceSpell() {
        super("amethyst_resonance", SpellRarity.UNCOMMON, 8, 14, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.waves", AmethystResonance.PULSES, Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.shatters_shots"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(AmethystResonance.RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        AmethystResonance.ring(level, entity, this, SpellUtil.groundAt(level, entity.position(), 2, 1), AmethystResonance.RANGE, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
