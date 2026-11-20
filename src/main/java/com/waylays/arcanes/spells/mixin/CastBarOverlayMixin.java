package com.waylays.arcanes.spells.mixin;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.gui.overlays.CastBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Iron's cast bar hides for instant spells: an instant spell being charged (see CastTimeFloor) shows it like any other. */
@Mixin(value = CastBarOverlay.class, remap = false)
public abstract class CastBarOverlayMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;getCastType()Lio/redspace/ironsspellbooks/api/spells/CastType;"))
    private CastType arcanes$chargedInstant() {
        CastType type = ClientMagicData.getCastType();
        return type == CastType.INSTANT && ClientMagicData.getCastDuration() > 0 ? CastType.LONG : type;
    }
}
