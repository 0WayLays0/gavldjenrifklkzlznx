package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenAegis;
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
 * Token Aegis: soul tokens turn around the caster; each one catches a blow or a shot meant for them and flies back at
 * whoever struck.
 */
@AutoSpellConfig
public class TokenAegisSpell extends TokenSpell {
    public static final int DURATION = 400;

    public TokenAegisSpell() {
        super("token_aegis", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public static int tokens(int spellLevel) {
        return 3 + spellLevel / 2;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.token_blocks", tokens(spellLevel)),
                Component.translatable("ui.arcanes_oublies.token_counter", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(DURATION, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        TokenAegis.raise(level, entity, this, tokens(spellLevel), DURATION, damage(spellLevel, entity));
        TokenFx.burst(level, TokenFx.chest(entity), 14, .7);
        TokenFx.clink(level, entity.position(), 1f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
