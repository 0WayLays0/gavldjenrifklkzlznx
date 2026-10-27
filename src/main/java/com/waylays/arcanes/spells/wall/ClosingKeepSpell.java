package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.Keep;
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

/**
 * The Closing Keep: a ring of eight crenellated walls builds itself round the spot looked at, then closes in all at once,
 * herding everything inside to the middle, where it is crushed (see Keep).
 */
@AutoSpellConfig
public class ClosingKeepSpell extends WallSpell {
    public static final float RANGE = 26;

    public ClosingKeepSpell() {
        super("closing_keep", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    public static float radius(int spellLevel) {
        return 7.5f + .5f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.crush_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, aimedSpot(level, entity, RANGE), 4, 1);
        Keep.raise(level, entity, this, at, radius(spellLevel), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
