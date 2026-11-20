package com.waylays.arcanes.spells.mixin;

import com.waylays.arcanes.spells.CastTimeFloor;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * An instant spell charged for a second (see {@link CastTimeFloor}): the caster holds Iron's charging pose while it
 * charges, and makes the spell's own gesture when it goes off.
 */
@Mixin(value = ClientSpellCastHelper.class, remap = false)
public abstract class ClientSpellCastHelperMixin {
    @Redirect(method = "handleClientBoundOnCastStarted", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastStartAnimation()Lio/redspace/ironsspellbooks/api/util/AnimationHolder;"))
    private static AnimationHolder arcanes$chargePose(AbstractSpell spell) {
        return CastTimeFloor.chargesInstant(spell) ? SpellAnimations.ANIMATION_LONG_CAST : spell.getCastStartAnimation();
    }

    @Redirect(method = "handleClientBoundOnCastFinished", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastFinishAnimation()Lio/redspace/ironsspellbooks/api/util/AnimationHolder;"))
    private static AnimationHolder arcanes$gestureAtTheEnd(AbstractSpell spell) {
        return CastTimeFloor.chargesInstant(spell) ? spell.getCastStartAnimation() : spell.getCastFinishAnimation();
    }
}
