package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.BookmarkMarker;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Bookmark: the first cast plants a glowing bookmark where the caster stands; casting it again (Iron's recast, shown
 * on the HUD) brings the caster back to it in a shower of pages. Works within one dimension only. The cooldown
 * starts once the bookmark is used or runs out.
 */
@AutoSpellConfig
public class BookmarkSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("bookmark");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(25)
            .build();

    public BookmarkSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getBookmarkTicks(spellLevel), 1)));
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BOOK_PAGE_TURN);
    }

    /** Placing the bookmark, then going back to it (scrolls cannot hold a recast, like Iron's Portal). */
    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new BookmarkData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        PlayerRecasts recasts = playerMagicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(getSpellId()) && recasts.getRecastInstance(getSpellId()).getCastData() instanceof BookmarkData data) {
            goBack(level, entity, data);
            // A player's recast is used up by Iron's after this; anyone else's is closed here
            if (!(entity instanceof ServerPlayer)) {
                recasts.decrementRecastCount(getSpellId());
            }
        } else {
            BookmarkMarker marker = new BookmarkMarker(level, entity, getBookmarkTicks(spellLevel));
            marker.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0);
            level.addFreshEntity(marker);
            BookmarkData data = new BookmarkData(entity.position(), entity.getYRot(), entity.getXRot(), level.dimension().location().toString(), marker.getUUID());
            recasts.addRecast(new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), getBookmarkTicks(spellLevel), castSource, data), playerMagicData);
            pageShower(level, entity.position(), false);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void goBack(Level level, LivingEntity entity, BookmarkData data) {
        removeMarker(level, data);
        if (!level.dimension().location().toString().equals(data.dimension)) {
            if (entity instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.arcanes_oublies.bookmark_other_world").withStyle(ChatFormatting.RED)));
            }
            return;
        }
        Vec3 from = entity.position();
        pageShower(level, from, true);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.2f, 1.1f);
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(data.pos.x, data.pos.y, data.pos.z, player.getYRot(), player.getXRot());
        } else {
            entity.teleportTo(data.pos.x, data.pos.y, data.pos.z);
        }
        entity.resetFallDistance();
        entity.setDeltaMovement(Vec3.ZERO);
        pageShower(level, data.pos, true);
        level.playSound(null, data.pos.x, data.pos.y, data.pos.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.2f, .9f);
        level.playSound(null, data.pos.x, data.pos.y, data.pos.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 2f, .8f);
    }

    /** Pages raining down around a spot, a column of glyphs when {@code travel} (someone leaves or arrives). */
    private static void pageShower(Level level, Vec3 at, boolean travel) {
        SpellUtil.particles(level, BookFx.paper(), at.x, at.y + 2.6, at.z, travel ? 30 : 12, .6, .3, .6, .02);
        SpellUtil.particles(level, BookFx.GILT_DUST, at.x, at.y + 1, at.z, travel ? 20 : 8, .4, .8, .4, 0);
        if (travel) {
            SpellUtil.particles(level, ParticleTypes.ENCHANT, at.x, at.y + 1, at.z, 60, .3, .8, .3, 1.2);
            SpellUtil.particles(level, ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 20, .3, .7, .3, .05);
        }
    }

    private static void removeMarker(Level level, BookmarkData data) {
        if (data.marker != null && level instanceof ServerLevel serverLevel) {
            Entity marker = serverLevel.getEntity(data.marker);
            if (marker instanceof BookmarkMarker bookmark) {
                bookmark.fade();
            }
        }
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        // Timed out or cleared: the bookmark fades away (when used, it already did)
        if (castDataSerializable instanceof BookmarkData data && serverPlayer != null) {
            removeMarker(serverPlayer.level(), data);
        }
        if (serverPlayer != null) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    /** 20 s at level 1, +5 s per level. */
    public int getBookmarkTicks(int spellLevel) {
        return 300 + spellLevel * 100;
    }
}
