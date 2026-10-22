package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenWave;
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

/** Token Wave: a wave of soul tokens surges out of the ground and rolls ahead, throwing up the enemies on its way. */
@AutoSpellConfig
public class TokenWaveSpell extends TokenSpell {
    public TokenWaveSpell() {
        super("token_wave", SpellRarity.UNCOMMON, 8, 8, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    public static float range(int spellLevel) {
        return 10 + spellLevel;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .9f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(range(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
        TokenWave.roll(level, entity, this, at, entity.getYRot(), range(spellLevel), damage(spellLevel, entity));
        TokenFx.burst(level, at.add(0, .3, 0), 12, .6);
        TokenFx.clink(level, at, 1f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
