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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Pruning Baton: the TVA's baton appears in the caster's hand for one sweeping blow. What it strikes is sent three
 * seconds back in time, to where it stood then; what it strikes when already worn down past the pruning line crumbles
 * into orange dust and is gone (bosses excepted: they only take the blow).
 */
@AutoSpellConfig
public class PruningBatonSpell extends CosmosSpell {
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
                Component.translatable("ui.arcanes_oublies.sent_back", Utils.timeFromTicks(PruningBaton.BACK, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            PruningBaton.swing(level, entity, this, spellLevel, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
