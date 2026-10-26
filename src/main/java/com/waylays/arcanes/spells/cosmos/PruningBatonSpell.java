package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.PruningBaton;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Pruning Baton: the TVA's baton appears in the caster's hand for a combo of three blows (cast again within two
 * seconds for the next, free): a sweep from the right, a sweep back from the left, then an overhead slam into the
 * ground that throws everything round back in a wave of orange light. The sweeps send what they strike three seconds
 * back in time, to where it stood then. What any blow strikes when already worn down past the pruning line crumbles
 * into orange light and is gone (bosses excepted: they only take the blow); the slam prunes far deeper.
 */
@AutoSpellConfig
public class PruningBatonSpell extends CosmosSpell {
    /** Ticks the caster has to follow up with the next blow. */
    public static final int COMBO_WINDOW = 40;

    public PruningBatonSpell() {
        super("pruning_baton", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    /** At or under this health after the blow, a target is pruned: a share of its greatest health growing with the level. */
    public static float pruneLine(int spellLevel, float maxHealth) {
        return Math.max(3, maxHealth * Math.min(.3f, .1f + .02f * spellLevel));
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.prune_line", Math.round(Math.min(.3f, .1f + .02f * spellLevel) * 100)),
                Component.translatable("ui.arcanes_oublies.sent_back", Utils.timeFromTicks(PruningBaton.BACK, 1)),
                Component.translatable("ui.arcanes_oublies.baton_combo"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            int stage = 1;
            if (entity instanceof ServerPlayer player) {
                var recasts = playerMagicData.getPlayerRecasts();
                if (recasts.hasRecastForSpell(getSpellId())) {
                    // Remaining recasts: 2 before the second blow, 1 before the third
                    stage = Math.max(2, Math.min(3, 4 - recasts.getRemainingRecastsForSpell(getSpellId())));
                } else if (castSource != CastSource.SCROLL) {
                    recasts.addRecast(new RecastInstance(getSpellId(), spellLevel, 3, COMBO_WINDOW, castSource, null), playerMagicData);
                }
            }
            PruningBaton.swing(level, entity, this, spellLevel, getPower(spellLevel, entity), stage);
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
}
