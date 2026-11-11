package com.waylays.eraphones.pack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.eraphones.pack.Packs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The backpack a player wears, on their back: its straps at the shoulders, following the body as it bends and turns. */
public class BackpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** How big the bag is worn, against how it is drawn. */
    private static final float SIZE = .8f;

    public BackpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack bag = Packs.worn(player);
        if (bag.isEmpty() || player.isInvisible()) {
            return;
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // From the model's space (y down) to the item's (y up); the bag's middle at the back, below the shoulders
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        // A little smaller than drawn (80 %): its back against the player's, its top just under the shoulders
        pose.translate(0, -5.6 / 16, 4.2 / 16);
        pose.scale(SIZE, SIZE, SIZE);
        Minecraft.getInstance().getItemRenderer().renderStatic(bag, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                player.level(), player.getId());
        pose.popPose();
    }
}
