package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenStorm;
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
 * Token Storm: a whirlwind of soul tokens rises around the caster and moves with them, slashing the enemies inside
 * and knocking enemy shots out of the air.
 */
@AutoSpellConfig
public class TokenStormSpell extends TokenSpell {
    public TokenStormSpell() {
        super("token_storm", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public static int duration(int spellLevel) {
        return 120 + 20 * spellLevel;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .35f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_per_half_second", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(TokenStorm.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        TokenStorm.raise(level, entity, this, duration(spellLevel), damage(spellLevel, entity));
        TokenFx.burst(level, entity.position().add(0, 1, 0), 24, 1.2);
        TokenFx.clink(level, entity.position(), 1.2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
