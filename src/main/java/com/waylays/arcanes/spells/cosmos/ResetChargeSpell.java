package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.ResetCharge;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Reset (ultimate): the caster sets a TVA reset charge on the ground where they aim. It counts down five seconds
 * (casting again sets it off at once), then resets the timeline all round: the caster's side come back as they were
 * at their best those five seconds (health, and rid of what ails them), foes are struck, sent back to where they
 * stood five seconds before and dazed, and the weakest are pruned from time altogether.
 */
@AutoSpellConfig
public class ResetChargeSpell extends CosmosSpell {
    public static final int RANGE = 16;

    public ResetChargeSpell() {
        super("reset_charge", SpellRarity.LEGENDARY, 3, 100, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 140;
    }

    public static float radius(int spellLevel) {
        return 9 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.reset_allies", Utils.timeFromTicks(ResetCharge.BACK, 1)),
                Component.translatable("ui.arcanes_oublies.reset_foes", Utils.stringTruncation(power * 3, 1), Utils.timeFromTicks(ResetCharge.BACK, 1)),
                Component.translatable("ui.arcanes_oublies.prune_line", Math.round(Math.min(.3f, .1f + .02f * (spellLevel + 4)) * 100)));
    }

    private boolean hasOpenRecast(LivingEntity entity) {
        return entity instanceof ServerPlayer player && MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(getSpellId());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            ResetCharge charge = ResetCharge.of(level, entity);
            if (hasOpenRecast(entity) && charge != null) {
                charge.detonate();
            } else {
                ResetCharge.set(level, entity, this, spellLevel, SpellUtil.aimedGround(level, entity, RANGE), radius(spellLevel), getPower(spellLevel, entity));
                if (entity instanceof ServerPlayer && castSource != CastSource.SCROLL) {
                    playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, 2, ResetCharge.COUNTDOWN, castSource, null), playerMagicData);
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (serverPlayer != null) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }
}
