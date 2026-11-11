package com.waylays.eraphones.mixin;

import com.waylays.eraphones.compat.ItemData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The weapons' workshop's glint: any item made to shine shines (1.21 keeps it as the item's own data). */
@Mixin(ItemStack.class)
public abstract class ItemStackGlintMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void eraphones$glint(CallbackInfoReturnable<Boolean> cir) {
        if (ItemData.glint((ItemStack) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
