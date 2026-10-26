package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.PocketMoon;
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
 * Pocket Moon: a little moon goes into orbit round the caster, waxing and waning as it goes round. It strikes what it
 * passes through, and its tide draws foes in toward the caster, into its path. Each time the spell is cast again the
 * caster throws it where they look like a boomerang: it strikes all along the way, there and back.
 */
@AutoSpellConfig
public class PocketMoonSpell extends CosmosSpell {
    public PocketMoonSpell() {
        super("pocket_moon", SpellRarity.UNCOMMON, 8, 25, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int life(int spellLevel) {
        return 400 + 80 * spellLevel;
    }

    public static int throwsOf(int spellLevel) {
        return 3 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.moon_hit", Utils.stringTruncation(power * .6f, 1)),
                Component.translatable("ui.arcanes_oublies.moon_throws", throwsOf(spellLevel), Utils.stringTruncation(power * 1.4f, 1)),
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
            PocketMoon moon = PocketMoon.of(level, entity);
            if (hasOpenRecast(entity) && moon != null) {
                moon.fling(entity);
            } else {
                PocketMoon.raise(level, entity, this, life(spellLevel), getPower(spellLevel, entity));
                if (entity instanceof ServerPlayer && castSource != CastSource.SCROLL) {
                    playerMagicData.getPlayerRecasts().addRecast(
                            new RecastInstance(getSpellId(), spellLevel, 1 + throwsOf(spellLevel), life(spellLevel), castSource, null), playerMagicData);
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
