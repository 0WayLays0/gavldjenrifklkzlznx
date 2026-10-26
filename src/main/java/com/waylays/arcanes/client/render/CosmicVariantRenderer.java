package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.CosmicVariant;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * A variant: its original's own skin and build (wide or slim arms), see-through and washed in orange, scanlines
 * running down it and two ghost copies of it (orange and blue) slipping sideways now and then, like a signal from
 * another branch of time. It is scanned in from its feet up and breaks up as it goes; it raises its arm to cast.
 */
public class CosmicVariantRenderer extends EntityRenderer<CosmicVariant> {
    private final PlayerModel<CosmicVariant> wide;
    private final PlayerModel<CosmicVariant> slim;

    public CosmicVariantRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.shadowRadius = .3f;
    }

    private static PlayerSkin skin(CosmicVariant variant) {
        UUID owner = variant.getSummonerUUID();
        if (owner != null && Minecraft.getInstance().getConnection() != null) {
            PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(owner);
            if (info != null) {
                return info.getSkin();
            }
        }
        return DefaultPlayerSkin.get(owner != null ? owner : variant.getUUID());
    }

    @Override
    public void render(CosmicVariant variant, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = variant.tickCount + partialTicks;
        // Scanned in from the feet up; broken up from the head down as it goes
        float scan = Mth.clamp(age / CosmicVariant.SCAN_TICKS, 0, 1);
        float out = Mth.clamp((variant.getLife() - age) / CosmicVariant.SCAN_TICKS, 0, 1);
        float top = Math.min(scan, out) * 1.12f;
        if (top <= .01f) {
            return;
        }
        PlayerSkin skin = skin(variant);
        PlayerModel<CosmicVariant> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        float bodyYaw = Mth.rotLerp(partialTicks, variant.yBodyRotO, variant.yBodyRot);
        float headYaw = Mth.rotLerp(partialTicks, variant.yHeadRotO, variant.yHeadRot) - bodyYaw;
        float pitch = Mth.lerp(partialTicks, variant.xRotO, variant.getXRot());
        model.attackTime = 0;
        model.riding = false;
        model.young = false;
        model.setupAnim(variant, 0, 0, age, headYaw, pitch);
        int cast = variant.getCastTick();
        if (cast >= 0 && age - cast < 14) {
            // The casting arm thrust out toward the aim
            float raise = Mth.sin(Mth.clamp((age - cast) / 14, 0, 1) * Mth.PI);
            model.rightArm.xRot = Mth.lerp(raise, model.rightArm.xRot, -Mth.HALF_PI + pitch * Mth.DEG_TO_RAD);
            model.rightSleeve.copyFrom(model.rightArm);
        }
        boolean glitch = RuneRender.hash(variant.getId() * 13 + (int) (age * .5f)) < .12f || out < 1;
        float height = variant.getBbHeight();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - bodyYaw));
        poseStack.scale(-.9375f, -.9375f, .9375f);
        poseStack.translate(0, -1.501, 0);
        // The likeness itself, a little see-through and warm
        int tint = RuneRender.alpha(GoldRender.lerp(0xFFFFFF, Cosmos.AMBER, .2f), .8f);
        model.renderToBuffer(poseStack, mask(buffer.getBuffer(RenderType.entityTranslucent(skin.texture())), poseStack, height, top, false, 0), GlyphRender.FULL,
                OverlayTexture.NO_OVERLAY, tint);
        // A faint wash of scanlines
        model.renderToBuffer(poseStack, mask(buffer.getBuffer(RenderType.energySwirl(CosmosRender.SCANLINES, 0, age * .02f)), poseStack, height, top, true,
                RuneRender.additive(Cosmos.ORANGE, .22f)), GlyphRender.FULL, OverlayTexture.NO_OVERLAY, -1);
        // Two ghosts slipping sideways when the signal breaks up
        if (glitch) {
            float slip = (RuneRender.hash((int) age * 7 + variant.getId()) - .5f) * .18f / .9375f;
            for (int k = 0; k < 2; k++) {
                poseStack.pushPose();
                poseStack.translate(k == 0 ? slip : -slip, 0, 0);
                model.renderToBuffer(poseStack, mask(buffer.getBuffer(RenderType.energySwirl(CosmosRender.SCANLINES, 0, age * .05f + k)), poseStack, height, top, true,
                        RuneRender.additive(k == 0 ? Cosmos.ORANGE : Cosmos.BLUE, .4f)), GlyphRender.FULL, OverlayTexture.NO_OVERLAY, -1);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        // The TVA's designation over its head
        if (scan >= 1) {
            poseStack.pushPose();
            poseStack.translate(0, height + .45f, 0);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.scale(.022f, -.022f, .022f);
            Component tag = Component.translatable("entity.arcanes_oublies.cosmic_variant.tag", variant.getDesignation());
            float x = -getFont().width(tag) / 2f;
            float flicker = glitch ? .5f : 1;
            getFont().drawInBatch(tag, x, 0, RuneRender.alpha(Cosmos.AMBER, out * flicker), false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    0x50000000, GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(variant, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * The model shown up to {@code top} (a share of its height from the feet), with a seam of light along the cut. In
     * the model's frame y runs down from the top of the head, so the cut is reckoned from the pose's own height.
     */
    private static VertexConsumer mask(VertexConsumer out, PoseStack poseStack, float height, float top, boolean additive, int light) {
        // The model's feet (model y 24/16 = 1.5) are 1.501 below its origin, after the flip and scale
        float originY = poseStack.last().pose().m31() - 1.5f * .9375f;
        return new CosmosRender.Masked(out, light, originY, height, 0, top, RuneRender.additive(Cosmos.CREAM, 1), additive);
    }

    @Override
    public ResourceLocation getTextureLocation(CosmicVariant variant) {
        return skin(variant).texture();
    }
}
