package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.SporePatch;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Moss Spores: a carpet of flowering moss spreads where the caster points and breathes out choking spores on the enemies standing on it. */
@AutoSpellConfig
public class MossSporesSpell extends VerdigrisSpell {
    public static final float RANGE = 18;

    public MossSporesSpell() {
        super("moss_spores", SpellRarity.COMMON, 8, 14, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    public static int duration(int spellLevel) {
        return 100 + 10 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_per_half_second", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(SporePatch.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.spores_poison"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = aimedSpot(level, entity, RANGE);
        SporePatch.spread(level, entity, this, at, duration(spellLevel), getPower(spellLevel, entity));
        VerdigrisFx.sound(level, at, SoundEvents.MOSS_PLACE, 1.4f, .6f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
