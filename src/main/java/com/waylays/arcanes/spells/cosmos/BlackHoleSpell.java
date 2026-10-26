package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.BlackHole;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Black Hole (ultimate): a black hole opens over the spot aimed at, its accretion disk whirling round it. Everything
 * not the caster's is drawn in, faster and faster, and torn at near the horizon; shots are swallowed. Cast again (or
 * wait for its end) and it collapses and bursts, throwing everything back out, with two jets of light shooting up and
 * down from it.
 */
@AutoSpellConfig
public class BlackHoleSpell extends CosmosSpell {
    public static final int RANGE = 28;

    public BlackHoleSpell() {
        super("black_hole", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 150;
    }

    public static float pullRange(int spellLevel) {
        return 12 + 2 * spellLevel;
    }

    public static int life(int spellLevel) {
        return 120 + 30 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.hole_pull", Utils.stringTruncation(pullRange(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.hole_horizon", Utils.stringTruncation(power * .6f, 1)),
                Component.translatable("ui.arcanes_oublies.hole_burst", Utils.stringTruncation(power * 3, 1)),
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
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (entity.tickCount % 3 == 0) {
            Vec3 at = SpellUtil.aimedSpot(level, entity, RANGE).add(0, 4, 0);
            CosmosFx.stars(level, at, 4, 1.5);
            SpellUtil.particles(level, CosmosFx.NEBULA, at.x, at.y, at.z, 6, 1.2, 1.2, 1.2, 0);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            BlackHole hole = BlackHole.of(level, entity);
            if (hasOpenRecast(entity) && hole != null) {
                hole.collapse();
            } else {
                Vec3 at = SpellUtil.aimedSpot(level, entity, RANGE).add(0, 4, 0);
                BlackHole.open(level, entity, this, at, pullRange(spellLevel), life(spellLevel), getPower(spellLevel, entity));
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
