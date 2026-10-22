package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.AncientBell;
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

/** Ancient Bell: a great verdigrised bell drops out of the sky on the enemy the caster looks at; its toll crushes and dazes. */
@AutoSpellConfig
public class AncientBellSpell extends VerdigrisSpell {
    public static final float RANGE = 24;

    public AncientBellSpell() {
        super("ancient_bell", SpellRarity.RARE, 5, 20, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.bell_under", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(power * .4f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(AncientBell.AROUND, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = aimedSpot(level, entity, RANGE);
        AncientBell.drop(level, entity, this, at, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
