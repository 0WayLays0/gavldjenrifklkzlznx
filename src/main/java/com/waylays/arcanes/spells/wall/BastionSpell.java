package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.Rampart;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bastion: a square keep of real stone bricks builds itself round the caster, its walls crenellated, a taller tower at
 * each corner, running round as it rises: nobody gets in, no shot gets through. The caster resists damage inside.
 */
@AutoSpellConfig
public class BastionSpell extends WallSpell {
    public static final int HALF = 3;

    public BastionSpell() {
        super("bastion", SpellRarity.RARE, 5, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int duration(int spellLevel) {
        return 160 + 30 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.solid_wall"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int life = duration(spellLevel);
        Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
        Rampart.raise(level, entity, Rampart.Shape.BASTION, at, 0, HALF, 3, life);
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, life, 0));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
