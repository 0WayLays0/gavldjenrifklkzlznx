package com.waylays.arcanes.spells;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.OwnedSummon;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A spell that summons allies ({@link OwnedSummon}s). For a player, casting it again while they are around sends them
 * away: it uses Iron's recast, open as long as the summons last, and that second cast is instant and costs no mana.
 * The cooldown starts once they are all gone, sent away, killed or expired. Mobs and scrolls just summon.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public abstract class SummoningSpell extends AbstractSpell {
    /** Brings the summons into the world (the cast that is not a recast). False if none could come. */
    protected abstract boolean summon(Level level, int spellLevel, LivingEntity caster);

    /** How long the summons last, and so how long the recast stays open. */
    public abstract int getLifetime(int spellLevel);

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (hasOpenRecast(entity)) {
            if (level instanceof ServerLevel serverLevel) {
                dismissSummons(serverLevel, entity);
            }
        } else {
            boolean summoned = summon(level, spellLevel, entity);
            if (summoned && entity instanceof ServerPlayer && playerMagicData != null && castSource != CastSource.SCROLL) {
                playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, 2, getLifetime(spellLevel), castSource, null), playerMagicData);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Sending them away takes no time. */
    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return hasOpenRecast(entity) ? 0 : super.getEffectiveCastTime(spellLevel, entity);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        // Only players get the recast, but the base would crash on a recast without one
        if (serverPlayer != null) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    /** Sends away the summoner's creatures from this spell, each in its own way. */
    public void dismissSummons(ServerLevel level, LivingEntity summoner) {
        for (OwnedSummon summon : summonsOf(level, summoner.getUUID(), this)) {
            summon.dismissSummon();
        }
    }

    /** True when this cast would send the summons away. */
    protected boolean hasOpenRecast(@Nullable LivingEntity entity) {
        return entity instanceof ServerPlayer player && MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(getSpellId());
    }

    /** The summoner's living creatures from that spell in this world. */
    public static List<OwnedSummon> summonsOf(ServerLevel level, UUID summoner, AbstractSpell spell) {
        List<OwnedSummon> found = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.isAlive() && entity instanceof OwnedSummon summon && summon.getSummoningSpell() == spell && summoner.equals(summon.getSummonerUUID())) {
                found.add(summon);
            }
        }
        return found;
    }

    /**
     * Once the last of a player's summons is gone, there is nothing left to send away: the recast closes, so the
     * cooldown starts and the next cast summons again.
     */
    @SubscribeEvent
    public static void onSummonGone(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (!(entity instanceof OwnedSummon summon) || !(event.getLevel() instanceof ServerLevel level) || reason == null || !reason.shouldDestroy()) {
            return;
        }
        UUID summoner = summon.getSummonerUUID();
        ServerPlayer player = summoner == null ? null : level.getServer().getPlayerList().getPlayer(summoner);
        if (player == null) {
            return;
        }
        AbstractSpell spell = summon.getSummoningSpell();
        PlayerRecasts recasts = MagicData.getPlayerMagicData(player).getPlayerRecasts();
        if (recasts.hasRecastForSpell(spell.getSpellId()) && summonsOf(level, summoner, spell).isEmpty()) {
            recasts.removeRecast(recasts.getRecastInstance(spell.getSpellId()), RecastResult.TIMEOUT);
        }
    }
}
