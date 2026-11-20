package com.waylays.arcanes.spells.mixin;

import com.waylays.arcanes.spells.CastTimeFloor;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The minimum cast time (see {@link CastTimeFloor}) where Iron's starts a player's cast: whatever the spell's own
 * cast time (and however a spell computes it), the cast it starts lasts at least that long. A recast the spell holds
 * open (sending summons away, a second throw) stays as quick as the spell makes it.
 */
@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {
    @Redirect(method = "attemptInitiateCast", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getEffectiveCastTime(ILnet/minecraft/world/entity/LivingEntity;)I"))
    private int arcanes$minCastTime(AbstractSpell spell, int spellLevel, LivingEntity caster) {
        int ticks = spell.getEffectiveCastTime(spellLevel, caster);
        if (caster != null && MagicData.getPlayerMagicData(caster).getPlayerRecasts().hasRecastForSpell(spell.getSpellId())) {
            return ticks;
        }
        return CastTimeFloor.apply(spell, caster, ticks);
    }
}
