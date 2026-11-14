package com.waylays.arcanes.rp.horror.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.horror.WallWriting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writing on a wall: each letter a picture from its style's sheet (drawn by the asset generator from hand-written
 * fonts), laid flat on the surface. The letters come one after another; drips of blood and runs of paint lengthen
 * slowly below them; rubbed out, it fades.
 */
public class WritingRenderer extends EntityRenderer<WallWriting> {
    /** Ticks between two letters appearing, and over which one fades in. */
    private static final float PACE = 1.6f, APPEAR = 6;
    private static final Map<String, Sheet> SHEETS = new HashMap<>();

    /** A style's letters: which there are, how far each moves the pen, the size of a cell. */
    record Sheet(ResourceLocation texture, String chars, float[] advance, int cw, int ch, int body, int pad, int cols, int rows) {}

    public WritingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static Sheet sheet(String style) {
        return SHEETS.computeIfAbsent(style, s -> {
            try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(ArcanesOublies.id("writing/" + s + ".json"))) {
                JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
                JsonArray adv = j.getAsJsonArray("advance");
                float[] a = new float[adv.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = adv.get(i).getAsFloat();
                }
                String chars = j.get("chars").getAsString();
                int cols = j.get("cols").getAsInt();
                JsonArray cell = j.getAsJsonArray("cell");
                return new Sheet(ArcanesOublies.id("textures/writing/" + s + ".png"), chars, a, cell.get(0).getAsInt(), cell.get(1).getAsInt(),
                        j.get("body").getAsInt(), j.get("pad").getAsInt(), cols, (chars.length() + cols - 1) / cols);
            } catch (Exception e) {
                ArcanesOublies.LOGGER.warn("No letters for the writing style {}: {}", s, e.toString());
                return null;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(WallWriting w) {
        return ArcanesOublies.id("textures/writing/" + w.getEntityData().get(WallWriting.STYLE) + ".png");
    }

    @Override
    public boolean shouldRender(WallWriting w, Frustum frustum, double x, double y, double z) {
        return w.distanceToSqr(x, y, z) < 80 * 80;
    }

    @Override
    public void render(WallWriting w, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        String style = w.getEntityData().get(WallWriting.STYLE);
        Sheet sh = sheet(style);
        if (sh == null) {
            return;
        }
        long now = w.level().getGameTime();
        float age = now - w.getEntityData().get(WallWriting.BORN) + pt;
        long gone = w.getEntityData().get(WallWriting.GONE);
        float fade = gone < 0 ? 1 : 1 - Mth.clamp((now - gone + pt) / WallWriting.FADE, 0, 1);
        float k = w.getEntityData().get(WallWriting.SIZE) / sh.body;
        Direction face = Direction.from3DDataValue(w.getEntityData().get(WallWriting.FACE));

        pose.pushPose();
        if (face.getAxis().isHorizontal()) {
            pose.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(-w.getEntityData().get(WallWriting.TURN)));
            pose.mulPose(Axis.XP.rotationDegrees(face == Direction.UP ? -90 : 90));
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(sh.texture));
        PoseStack.Pose p = pose.last();
        String[] lines = w.getEntityData().get(WallWriting.TEXT).split("\\|");
        float lineH = sh.body * 1.08f * k;
        float top = lines.length * lineH / 2;
        boolean runs = style.equals("sang") || style.equals("peinture");
        float runTime = style.equals("sang") ? 400 : 160;
        int n = 0;
        for (String line : lines) {
            List<int[]> glyphs = new ArrayList<>();
            float width = 0;
            for (char c : line.toCharArray()) {
                int i = sh.chars.indexOf(c);
                if (i < 0) {
                    i = sh.chars.indexOf(' ');
                }
                glyphs.add(new int[] {i});
                width += sh.advance[i];
            }
            float x = -width * k / 2;
            for (int[] g : glyphs) {
                int i = g[0];
                float t = age - n * PACE;
                n++;
                float alpha = Mth.clamp(t / APPEAR, 0, 1) * fade;
                if (alpha > 0 && sh.chars.charAt(i) != ' ') {
                    float drip = runs ? Mth.clamp((t - APPEAR) / runTime, 0, 1) : 1;
                    float shown = sh.body + (sh.ch - sh.body) * (float) Math.sqrt(drip);
                    float u0 = (i % sh.cols) / (float) sh.cols, u1 = (i % sh.cols + 1) / (float) sh.cols;
                    float v0 = (i / sh.cols) / (float) sh.rows, v1 = (i / sh.cols + shown / sh.ch) / sh.rows;
                    float x0 = x - sh.pad * k, x1 = x0 + sh.cw * k, y0 = top, y1 = top - shown * k;
                    int a = (int) (alpha * 255);
                    quad(vc, p, light, a, x0, y0, x1, y1, u0, v0, u1, v1);
                }
                x += sh.advance[i] * k;
            }
            top -= lineH;
        }
        pose.popPose();
        super.render(w, yaw, pt, pose, buffers, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, int light, int a, float x0, float y0, float x1, float y1, float u0, float v0,
                             float u1, float v1) {
        vc.addVertex(p, x0, y1, 0).setColor(255, 255, 255, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x1, y1, 0).setColor(255, 255, 255, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x1, y0, 0).setColor(255, 255, 255, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x0, y0, 0).setColor(255, 255, 255, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
    }
}
