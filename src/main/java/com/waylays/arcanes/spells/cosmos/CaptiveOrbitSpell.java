package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.CaptivePlanet;
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
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Captive Orbit: a small planet appears over the enemy the caster looks at, and the enemy is torn off the ground and
 * swept into orbit round it, faster and faster, struck at each turn. Cast again: the caster slings it out of orbit
 * where they look, and it crashes into whatever it meets, hurting what is round it. Left alone, it is flung off when
 * the planet fades.
 */
@AutoSpellConfig
public class CaptiveOrbitSpell extends CosmosSpell {
    public static final int RANGE = 24;

    public CaptiveOrbitSpell() {
        super("captive_orbit", SpellRarity.RARE, 6, 22, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static int life(int spellLevel) {
        return 100 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.orbit_turn", Utils.stringTruncation(power * .4f, 1)),
                Component.translatable("ui.arcanes_oublies.orbit_sling", Utils.stringTruncation(power * 2.5f, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    private boolean hasOpenRecast(LivingEntity entity) {
        return entity instanceof ServerPlayer player && MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(getSpellId());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return hasOpenRecast(entity) || Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, .35f);
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        return hasOpenRecast(entity) ? 0 : super.getEffectiveCastTime(spellLevel, entity);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            if (hasOpenRecast(entity)) {
                CaptivePlanet planet = CaptivePlanet.of(level, entity);
                if (planet != null) {
                    planet.release(entity.getLookAngle());
                }
            } else if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
                LivingEntity target = targetData.getTarget((ServerLevel) level);
                if (target != null) {
                    CaptivePlanet.capture(level, entity, target, this, life(spellLevel), getPower(spellLevel, entity));
                    if (entity instanceof ServerPlayer && castSource != CastSource.SCROLL) {
                        playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, 2, life(spellLevel), castSource, null), playerMagicData);
                    }
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
