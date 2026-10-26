package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.RiddenComet;
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
 * Comet Ride: a comet streaks down under the caster and carries them off: it goes where they look, never stopping,
 * and rams whatever is in its way. Cast again (or jump off) and it dives and crashes where it is headed: a blast all
 * round and a crater of burning ground, its rider thrown clear and floating down.
 */
@AutoSpellConfig
public class CometRideSpell extends CosmosSpell {
    public CometRideSpell() {
        super("comet_ride", SpellRarity.RARE, 5, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public static int life(int spellLevel) {
        return 120 + 30 * spellLevel;
    }

    public static float speed(int spellLevel) {
        return .75f + .08f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.comet_ram", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.comet_crash", Utils.stringTruncation(power * 2.5f, 1)),
                Component.translatable("ui.arcanes_oublies.flight_speed", Utils.stringTruncation(speed(spellLevel) * 20, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    private boolean hasOpenRecast(LivingEntity entity) {
        return entity instanceof ServerPlayer player && MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(getSpellId());
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        return hasOpenRecast(entity) ? 0 : super.getEffectiveCastTime(spellLevel, entity);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            if (hasOpenRecast(entity)) {
                RiddenComet comet = RiddenComet.of(level, entity);
                if (comet != null) {
                    comet.dive();
                }
            } else {
                RiddenComet old = RiddenComet.of(level, entity);
                if (old != null) {
                    old.dive();
                }
                RiddenComet.call(level, entity, life(spellLevel), speed(spellLevel), getPower(spellLevel, entity));
                if (entity instanceof ServerPlayer && castSource != CastSource.SCROLL) {
                    playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, 2, life(spellLevel), castSource, null), playerMagicData);
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
