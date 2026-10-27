package com.waylays.arcanes.spells.bone;

import com.waylays.arcanes.entity.bone.SpikeField;
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

/** Ossuary: for a while, spikes of bone burst out of the ground all over a circle, two a tick (see SpikeField, ZONE). */
@AutoSpellConfig
public class OssuarySpell extends BoneSpell {
    public static final float RANGE = 24;

    public OssuarySpell() {
        super("ossuary", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 75;
    }

    public static float radius(int spellLevel) {
        return 4.5f + .5f * spellLevel;
    }

    public static int duration(int spellLevel) {
        return 100 + 10 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.spike_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, aimedSpot(level, entity, RANGE), 4, 1);
        SpikeField.erupt(level, entity, this, SpikeField.Mode.ZONE, at, entity.getYRot(), radius(spellLevel), duration(spellLevel), getPower(spellLevel, entity));
        BoneFx.erupt(level, at, 2);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
