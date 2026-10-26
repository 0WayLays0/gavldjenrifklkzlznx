package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.ZeroGravityField;
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
 * Zero Gravity: a dome of zero gravity opens round the caster. Foes inside drift helplessly up off the ground and
 * their shots hang in the air; the caster and their allies bound about under a moon's gravity. Cast again (or wait
 * for it to fail) and gravity comes back at once: everything that floated is slammed into the ground, the higher the
 * harder.
 */
@AutoSpellConfig
public class ZeroGravitySpell extends CosmosSpell {
    public ZeroGravitySpell() {
        super("zero_gravity", SpellRarity.EPIC, 6, 35, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    public static float radius(int spellLevel) {
        return 7 + .5f * spellLevel;
    }

    public static int life(int spellLevel) {
        return 100 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.gravity_slam", Utils.stringTruncation(power, 1), Utils.stringTruncation(power * 3.8f, 1)),
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
            ZeroGravityField field = ZeroGravityField.of(level, entity);
            if (hasOpenRecast(entity) && field != null) {
                field.slam();
            } else {
                ZeroGravityField.open(level, entity, this, radius(spellLevel), life(spellLevel), getPower(spellLevel, entity));
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
