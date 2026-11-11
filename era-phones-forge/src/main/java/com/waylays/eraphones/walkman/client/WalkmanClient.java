package com.waylays.eraphones.walkman.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.walkman.WalkmanItem;
import com.waylays.eraphones.walkman.Walkmans;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The walkman on the players' side: who is listening (their headphones drawn on their head, over the ears, the band
 * across the top), and the walkman's look with a cassette in it.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class WalkmanClient {
    public static final net.minecraft.resources.ResourceLocation HEADPHONES = EraPhones.id("item/casque_3d");
    private static final Set<UUID> LISTENING = new HashSet<>();

    private WalkmanClient() {}

    public static void listening(Walkmans.Listening l) {
        if (l.on()) {
            LISTENING.add(l.player());
        } else {
            LISTENING.remove(l.player());
        }
    }

    public static boolean listening(UUID player) {
        return LISTENING.contains(player);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        LISTENING.clear();
    }

    /** The headphones, on a listener's head. */
    public static class HeadphonesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public HeadphonesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!listening(player.getUUID()) || player.isInvisible()) {
                return;
            }
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(HEADPHONES);
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            // The model's space (y down) to the drawing's (y up); the head's middle at the model's middle
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.translate(-.5, 4 / 16f - .5, -.5);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                    buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), null, model, 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    @EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void onModels(ModelEvent.RegisterAdditional event) {
            event.register(HEADPHONES);
        }

        @SubscribeEvent
        public static void onLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer r) {
                    r.addLayer(new HeadphonesLayer(r));
                }
            }
        }

        @SubscribeEvent
        public static void onSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(com.waylays.eraphones.walkman.WalkmanRegistry.WALKMAN.get(), EraPhones.id("charge"),
                    (stack, level, entity, seed) -> WalkmanItem.cassette(stack).isEmpty() ? 0 : 1));
        }
    }
}
