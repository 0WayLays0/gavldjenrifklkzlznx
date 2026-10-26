package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.AsteroidBelt;
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
 * Asteroid Belt: a ring of asteroids gathers round the spot aimed at and goes round it, a cage of rock: what crosses
 * it is struck and thrown back in, shots that meet it are smashed. Cast again (or wait for its end) and it falls in on
 * itself, dragging everything inside to the middle, where the rocks crash together.
 */
@AutoSpellConfig
public class AsteroidBeltSpell extends CosmosSpell {
    public static final int RANGE = 20;

    public AsteroidBeltSpell() {
        super("asteroid_belt", SpellRarity.RARE, 8, 24, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public static int rocks(int spellLevel) {
        return 14 + 2 * spellLevel;
    }

    public static int life(int spellLevel) {
        return 120 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.belt_rocks", rocks(spellLevel), Utils.stringTruncation(power * .5f, 1)),
                Component.translatable("ui.arcanes_oublies.belt_crush", Utils.stringTruncation(power * 2.4f, 1)),
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
            AsteroidBelt belt = AsteroidBelt.of(level, entity);
            if (hasOpenRecast(entity) && belt != null) {
                belt.crush();
            } else {
                AsteroidBelt.raise(level, entity, this, SpellUtil.aimedSpot(level, entity, RANGE), rocks(spellLevel), life(spellLevel), getPower(spellLevel, entity));
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
