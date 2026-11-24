package com.waylays.arcanes.rp.mixin;

import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Water Walking: water is solid underfoot (as lava is to a strider), unless one crouches to go into it. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWaterWalkMixin {
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void arcanes$walkOnWater(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (fluid.is(FluidTags.WATER) && !self.isShiftKeyDown() && self.hasEffect(ModEffects.WATER_WALKING)) {
            cir.setReturnValue(true);
        }
    }
}
