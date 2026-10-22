package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.AncientPillar;
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

/** Ancient Pillar: a mossy column of old copper bursts out of the ground under the enemy the caster looks at and throws it high. */
@AutoSpellConfig
public class AncientPillarSpell extends VerdigrisSpell {
    public static final float RANGE = 20;

    public AncientPillarSpell() {
        super("ancient_pillar", SpellRarity.UNCOMMON, 8, 8, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)),
                Component.translatable("ui.arcanes_oublies.launch"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = aimedSpot(level, entity, RANGE);
        AncientPillar.raise(level, entity, this, AncientPillar.Kind.PILLAR, at, 3, 80, 1f + spellLevel * .03f, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
