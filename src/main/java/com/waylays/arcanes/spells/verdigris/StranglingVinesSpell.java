package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.VineSnare;
import com.waylays.arcanes.spells.SpellUtil;
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
 * Strangling Vines: moss spreads under the enemy the caster looks at and vines climb it, holding it in place and
 * squeezing it every second. From level 3, they also seize the enemies standing next to it.
 */
@AutoSpellConfig
public class StranglingVinesSpell extends VerdigrisSpell {
    public static final int RANGE = 20;
    public static final float SPREAD = 3;

    public StranglingVinesSpell() {
        super("strangling_vines", SpellRarity.UNCOMMON, 6, 12, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int duration(int spellLevel) {
        return 60 + 10 * spellLevel;
    }

    /** How many enemies next to the target are seized too. */
    public static int extra(int spellLevel) {
        return spellLevel / 3;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.squeeze_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.rooted", Utils.timeFromTicks(duration(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.vine_extra", extra(spellLevel)));
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
                float damage = getPower(spellLevel, entity);
                int life = duration(spellLevel);
                VineSnare.bind(level, entity, this, target, life, damage);
                int more = extra(spellLevel);
                for (LivingEntity near : SpellUtil.enemiesAround(level, target.position(), SPREAD, entity, e -> e != target)) {
                    if (more-- <= 0) {
                        break;
                    }
                    VineSnare.bind(level, entity, this, near, life, damage);
                }
                VerdigrisFx.chips(level, target.position().add(0, .2, 0), net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState(), 16, .6);
                VerdigrisFx.sound(level, target.position(), net.minecraft.sounds.SoundEvents.MOSS_PLACE, 1.2f, .7f);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
