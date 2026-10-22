package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.EmeraldBloom;
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

/** Emerald Bloom: a flower of emeralds that heals the caster's side and takes their ailments off (see {@link EmeraldBloom}). */
@AutoSpellConfig
public class EmeraldBloomSpell extends GemSpell {
    public EmeraldBloomSpell() {
        super("emerald_bloom", SpellRarity.UNCOMMON, 8, 25, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static int duration(int spellLevel) {
        return 140 + 20 * spellLevel;
    }

    public float heal(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bloom_heal", Utils.stringTruncation(heal(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.cleanses"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(EmeraldBloom.RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        EmeraldBloom.open(level, entity, SpellUtil.groundAt(level, entity.position(), 2, 1), duration(spellLevel), heal(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
