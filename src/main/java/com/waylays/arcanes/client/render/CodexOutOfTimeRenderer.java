package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Codex Out of Time as it is held, worn on the hip or lies on the ground: the 3D book (its model,
 * codex_out_of_time_3d), the TVA clock on its cover ticking (hands of light going round, its jewel pulsing), a small
 * ringed moon going round the book on a tilted orbit, and stars twinkling about it.
 */
public class CodexOutOfTimeRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation BOOK = ModelResourceLocation.standalone(ArcanesOublies.id("item/codex_out_of_time_3d"));
    private static CodexOutOfTimeRenderer instance;

    private CodexOutOfTimeRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static CodexOutOfTimeRenderer get() {
        if (instance == null) {
            instance = new CodexOutOfTimeRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel book = mc.getModelManager().getModel(BOOK);
        mc.getItemRenderer().renderModelLists(book, stack, light, overlay, poseStack, buffer.getBuffer(Sheets.cutoutBlockSheet()));
        float time = (Util.getMillis() % 3_600_000L) / 50f;
        // The clock on the front cover (its east face, at x 10.35/16), ticking: seconds, minutes, a pulsing jewel
        poseStack.pushPose();
        poseStack.translate(10.56f / 16, 3.8f / 16, 8f / 16);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        float tick = (float) Math.floor(time / 20);
        float second = (tick + Math.min(1, (time % 20) / 3)) / 60f;
        hand(stroke, poseStack.last(), second * Mth.TWO_PI, 2.4f / 16, .22f / 16, RuneRender.additive(Cosmos.ORANGE, 1));
        hand(stroke, poseStack.last(), time / 1200 * Mth.TWO_PI, 1.8f / 16, .32f / 16, RuneRender.additive(Cosmos.CREAM, .9f));
        float pulse = .6f + .4f * Mth.sin(time * .15f);
        GlyphRender.dot(stroke, poseStack.last(), 0, 0, .55f / 16, RuneRender.additive(Cosmos.AMBER, pulse));
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, 2.6f / 16, RuneRender.additive(Cosmos.ORANGE, .25f * pulse));
        poseStack.popPose();
        // A small ringed moon on a tilted orbit round the book
        Vec3 centre = new Vec3(8f / 16, 4.2f / 16, 8f / 16);
        float a = time * .06f;
        poseStack.pushPose();
        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(22));
        poseStack.translate(Mth.cos(a) * 9.5f / 16, 0, Mth.sin(a) * 9.5f / 16);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-22));
        CosmosRender.planet(poseStack, buffer, CosmosRender.PLANET_ICE, 1.15f / 16, time * .004f, new Vector3f(-1, .4f, .3f), .25f, 0xFFFFFF);
        CosmosRender.atmosphere(poseStack, buffer, 1.15f / 16, Cosmos.VIOLET, .5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(28));
        CosmosRender.ring(poseStack, buffer, 1.5f / 16, 2.5f / 16, 0xFFFFFF, .8f);
        poseStack.popPose();
        // Stars twinkling about it
        for (int k = 0; k < 6; k++) {
            float h = RuneRender.hash(k * 53 + 7);
            float b = h * Mth.TWO_PI + time * .01f * (k % 2 == 0 ? 1 : -1);
            poseStack.pushPose();
            poseStack.translate(centre.x + Mth.cos(b) * (6 + 4 * h) / 16, centre.y + (RuneRender.hash(k * 31) - .3f) * 9 / 16, centre.z + Mth.sin(b) * (6 + 4 * h) / 16);
            CosmosRender.twinkle(poseStack, buffer, .9f / 16, k % 3 == 0 ? Cosmos.GOLD : Cosmos.STAR, .9f, time, k * 17);
            poseStack.popPose();
        }
    }

    private static void hand(VertexConsumer stroke, PoseStack.Pose pose, float angle, float length, float width, int argb) {
        float x = Mth.sin(angle) * length, y = Mth.cos(angle) * length;
        GlyphRender.ribbon(stroke, pose, -x * .15f, -y * .15f, x, y, width, argb);
    }
}
