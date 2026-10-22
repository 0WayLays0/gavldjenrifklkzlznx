package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenBolt;
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

/** Ricochet: a soul token shot at an enemy bounces on to the next one, and the next, each a little further. */
@AutoSpellConfig
public class TokenRicochetSpell extends TokenSpell {
    public static final float SPEED = 1.7f;

    public TokenRicochetSpell() {
        super("token_ricochet", SpellRarity.UNCOMMON, 8, 5, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 22;
    }

    /** How many times it bounces on to another enemy. */
    public static int bounces(int spellLevel) {
        return 2 + spellLevel / 2;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .8f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.token_bounces", bounces(spellLevel)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 from = TokenFx.hand(entity);
        TokenBolt.shoot(level, entity, this, TokenBolt.Mode.RICOCHET, from, entity.getLookAngle(), SPEED, damage(spellLevel, entity))
                .withBounces(bounces(spellLevel));
        TokenFx.clink(level, from, .8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
