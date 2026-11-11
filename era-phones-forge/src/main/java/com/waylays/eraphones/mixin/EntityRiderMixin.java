package com.waylays.eraphones.mixin;

import com.waylays.eraphones.carry.Carry;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player carried in another's arms is held before them, not sat on their head. */
@Mixin(Entity.class)
public abstract class EntityRiderMixin {
    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("HEAD"),
            cancellable = true)
    private void eraphones$carry(Entity passenger, Entity.MoveFunction move, CallbackInfo ci) {
        if (Carry.position((Entity) (Object) this, passenger, move)) {
            ci.cancel();
        }
    }
}
