package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.FirePillar;
import com.waylays.arcanes.entity.fire.SearingBrand;
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

import java.util.List;

/**
 * Searing Brand: a sigil of fire is burnt over the enemy the caster looks at; three seconds later a pillar of fire
 * bursts up under it. With no enemy in sight, the pillar bursts where the caster points (see SearingBrand).
 */
@AutoSpellConfig
public class SearingBrandSpell extends StarfireSpell {
    public static final double RANGE = 24;

    public SearingBrandSpell() {
        super("searing_brand", SpellRarity.RARE, 6, 14, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.arcanes_oublies.brand_fuse", Utils.timeFromTicks(SearingBrand.FUSE, 1), Utils.stringTruncation(hot(spellLevel, caster) * 2, 1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        LivingEntity target = aimed(level, entity, RANGE);
        if (target != null) {
            SearingBrand.mark(level, entity, this, target, hot(spellLevel, entity) * 2, heat(entity));
        } else {
            FirePillar.raise(level, entity, this, aimedSpot(level, entity, RANGE), 0, true, hot(spellLevel, entity) * 2, heat(entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
