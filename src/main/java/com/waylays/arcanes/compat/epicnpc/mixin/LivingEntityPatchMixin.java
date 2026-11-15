package com.waylays.arcanes.compat.epicnpc.mixin;

import com.waylays.arcanes.compat.epicnpc.EpicNpc;
import com.waylays.arcanes.compat.epicnpc.client.EpicNpcClient;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Lets Custom NPCs draw, in its own way, the NPCs Epic Fight's human body cannot show: those disguised as an animal or
 * another mob not built like a person, and the stand-in mobs it draws disguised NPCs with (see {@link EpicNpcClient}).
 * Only applied when Epic Fight is installed (its target is optional).
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch", remap = false)
public abstract class LivingEntityPatchMixin {
    @Inject(method = "overrideRender", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void arcanes$customNpcsLook(CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (self instanceof LivingEntityPatch<?> patch && EpicNpc.active()) {
            LivingEntity entity = patch.getOriginal();
            if (entity != null && entity.level() != null && entity.level().isClientSide() && EpicNpcClient.vanillaLook(entity)) {
                cir.setReturnValue(false);
            }
        }
    }
}
