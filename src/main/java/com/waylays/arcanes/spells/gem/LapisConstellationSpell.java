package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.LapisConstellation;
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

/** Lapis Constellation: seven stars of lapis circle the caster and dart at the enemies that come near (see {@link LapisConstellation}). */
@AutoSpellConfig
public class LapisConstellationSpell extends GemSpell {
    public LapisConstellationSpell() {
        super("lapis_constellation", SpellRarity.UNCOMMON, 8, 20, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static int duration(int spellLevel) {
        return 300 + 40 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.lapis_stars", LapisConstellation.STARS, Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(LapisConstellation.RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        LapisConstellation.gather(level, entity, this, duration(spellLevel), getPower(spellLevel, entity));
        GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.LAPIS, 3, 30);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
