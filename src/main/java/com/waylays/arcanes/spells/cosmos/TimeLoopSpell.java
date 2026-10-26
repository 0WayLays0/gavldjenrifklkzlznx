package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.TimeLoop;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Time Loop: the enemy the caster looks at is caught in a loop three seconds long. Each time round it is snapped back
 * to where it stood when the loop closed, and the blows it took meanwhile strike it again.
 */
@AutoSpellConfig
public class TimeLoopSpell extends CosmosSpell {
    public static final int RANGE = 24;

    public TimeLoopSpell() {
        super("time_loop", SpellRarity.RARE, 5, 30, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static int loops(int spellLevel) {
        return 2 + (spellLevel + 1) / 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.loops", loops(spellLevel), Utils.timeFromTicks(TimeLoop.PERIOD, 1)),
                Component.translatable("ui.arcanes_oublies.loop_sting", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.loop_echo", Math.round(TimeLoop.ECHO * 100)));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                TimeLoop.close(level, entity, target, this, loops(spellLevel), getPower(spellLevel, entity));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
