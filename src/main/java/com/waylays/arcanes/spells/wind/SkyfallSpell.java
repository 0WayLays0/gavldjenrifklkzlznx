package com.waylays.arcanes.spells.wind;

import com.waylays.arcanes.entity.wind.Skyfall;
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

/** Skyfall: the enemy the caster looks at is carried up by a whirling updraft, then slammed down (see Skyfall). */
@AutoSpellConfig
public class SkyfallSpell extends WindSpell {
    public static final float RANGE = 20;

    public SkyfallSpell() {
        super("skyfall", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.lift_height", Utils.stringTruncation(Skyfall.HEIGHT, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            LivingEntity target = aimed(level, entity, RANGE);
            if (target != null) {
                Skyfall.seize(level, entity, this, target, getPower(spellLevel, entity));
            } else {
                WindFx.gust(level, aimedSpot(level, entity, RANGE), 12, 1);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
