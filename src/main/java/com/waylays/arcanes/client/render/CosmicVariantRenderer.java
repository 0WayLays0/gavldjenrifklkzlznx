package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.CosmicVariant;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.Minecraft;
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
        float scanIn = Mth.clamp(age / 8, 0, 1);
        float out = Mth.clamp((CosmicVariant.LIFE - age) / 6, 0, 1);
        float alpha = scanIn * out;
        if (alpha <= .01f) {
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
        boolean glitch = RuneRender.hash(variant.getId() * 13 + (int) (age * .5f)) < .18f || out < 1;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - bodyYaw));
        poseStack.scale(-.9375f, -.9375f, .9375f);
        poseStack.translate(0, -1.501, 0);
        // The likeness, see-through and washed in orange
        int tint = RuneRender.alpha(GoldRender.lerp(0xFFFFFF, Cosmos.AMBER, .45f), .62f * alpha);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(skin.texture())), GlyphRender.FULL, OverlayTexture.NO_OVERLAY, tint);
        // Scanlines running down it
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.energySwirl(CosmosRender.SCANLINES, 0, age * .02f)), GlyphRender.FULL,
                OverlayTexture.NO_OVERLAY, RuneRender.additive(Cosmos.ORANGE, .55f * alpha));
        // Two ghosts slipping sideways when the signal breaks up
        if (glitch) {
            float slip = (RuneRender.hash((int) age * 7 + variant.getId()) - .5f) * .18f / .9375f;
            for (int k = 0; k < 2; k++) {
                poseStack.pushPose();
                poseStack.translate(k == 0 ? slip : -slip, 0, 0);
                model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.energySwirl(CosmosRender.SCANLINES, 0, age * .05f + k)), GlyphRender.FULL,
                        OverlayTexture.NO_OVERLAY, RuneRender.additive(k == 0 ? Cosmos.ORANGE : Cosmos.BLUE, .45f * alpha));
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        super.render(variant, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CosmicVariant variant) {
        return skin(variant).texture();
    }
}
