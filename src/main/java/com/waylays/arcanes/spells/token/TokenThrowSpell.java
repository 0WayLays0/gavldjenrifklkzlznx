package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenBolt;
import com.waylays.arcanes.entity.token.TokenMarker;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Token Throw: a soul token flung where the caster looks; it hurts what it hits and stays where it lands. Casting it
 * again while the token lies there (Iron's recast, instant and free) brings the caster to it.
 */
@AutoSpellConfig
public class TokenThrowSpell extends TokenSpell {
    public static final float SPEED = 1.6f;
    public static final double GRAVITY = .02;

    public TokenThrowSpell() {
        super("token_throw", SpellRarity.COMMON, 10, 8, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    /** How long the token stays where it landed, and so how long the teleport is open. */
    public static int markerLife(int spellLevel) {
        return 160 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.token_teleport", Utils.timeFromTicks(markerLife(spellLevel), 1)));
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return hasOpenRecast(entity) ? 0 : super.getEffectiveCastTime(spellLevel, entity);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (hasOpenRecast(entity)) {
            if (level instanceof ServerLevel serverLevel) {
                teleport(serverLevel, entity);
            }
        } else {
            Vec3 from = TokenFx.hand(entity);
            TokenBolt.shoot(level, entity, this, TokenBolt.Mode.THROW, from, entity.getLookAngle(), SPEED, getPower(spellLevel, entity))
                    .withGravity(GRAVITY).withMarkerLife(markerLife(spellLevel));
            TokenFx.clink(level, from, .8f);
            if (entity instanceof ServerPlayer && playerMagicData != null && castSource != CastSource.SCROLL) {
                playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, 2, markerLife(spellLevel) + 40, castSource, null), playerMagicData);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        // Only players get the recast, but the base would crash on a recast without one
        if (serverPlayer != null) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    /** Brings the caster to their token, standing on the ground next to it. */
    public static boolean teleport(ServerLevel level, LivingEntity caster) {
        TokenMarker marker = TokenMarker.find(level, caster.getUUID());
        if (marker == null) {
            return false;
        }
        Vec3 from = caster.position();
        Vec3 to = SpellUtil.groundAt(level, marker.position(), 3, 2);
        TokenFx.burst(level, from.add(0, 1, 0), 16, .5);
        caster.teleportTo(to.x, to.y, to.z);
        caster.resetFallDistance();
        TokenFx.burst(level, to.add(0, 1, 0), 20, .5);
        TokenFx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, .8f, 1.5f);
        TokenFx.sound(level, to, SoundEvents.ENDERMAN_TELEPORT, .8f, 1.7f);
        marker.discard();
        return true;
    }

    private boolean hasOpenRecast(@Nullable LivingEntity entity) {
        return entity instanceof ServerPlayer player && MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(getSpellId());
    }
}
