package com.waylays.eraphones.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.Picture;
import com.waylays.eraphones.gear.PinnedPhotoBlock;
import com.waylays.eraphones.gear.PinnedPhotoBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A pinned photograph: its white frame, its picture on instant film (developing still, if it is fresh), the name of
 * who took it in blue ink below, and on a wall a coloured pin through the top of the frame.
 */
public class PinnedPhotoRenderer implements BlockEntityRenderer<PinnedPhotoBlockEntity> {
    private static final ResourceLocation FRAME = EraPhones.id("textures/gui/gear/polaroid.png");
    private static final int[] PINS = {0xD83434, 0xE8C234, 0x3466D4, 0x44A456, 0xF0F0EC};
    /** The frame in units (150 x 180), and a unit in blocks. */
    private static final float U = .42f / 150, W = 150 * U, H = 180 * U;
    private static final Map<Picture, Tex> CACHE = new LinkedHashMap<>(16, .75f, true);
    private static int made;

    private static final class Tex {
        DynamicTexture texture;
        ResourceLocation id;
        int[] px;
        int side;
        float dev = -1;
    }

    private final Font font;

    public PinnedPhotoRenderer(BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    /** The picture's texture, painted as developed as it now is. */
    private static Tex texture(Picture pic, float dev) {
        Tex t = CACHE.get(pic);
        if (t == null) {
            int[] px = PhotoClient.unpack(pic.data());
            if (px == null) {
                return null;
            }
            t = new Tex();
            t.px = px;
            t.side = px[0];
            t.texture = new DynamicTexture(t.side, t.side, false);
            t.id = EraPhones.id("photo_pinned_" + made++);
            Minecraft.getInstance().getTextureManager().register(t.id, t.texture);
            CACHE.put(pic, t);
            // Only so many pictures kept at once: the longest unseen given back
            if (CACHE.size() > 64) {
                Iterator<Map.Entry<Picture, Tex>> it = CACHE.entrySet().iterator();
                Tex old = it.next().getValue();
                it.remove();
                Minecraft.getInstance().getTextureManager().release(old.id);
            }
        }
        if (t.dev < 0 || Math.abs(dev - t.dev) > .01f) {
            t.dev = dev;
            if (t.texture.getPixels() != null) {
                PhotoScreen.paint(t.texture.getPixels(), t.px, t.side, dev);
                t.texture.upload();
            }
        }
        return t;
    }

    @Override
    public void render(PinnedPhotoBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack photo = be.photo();
        Picture pic = GearRegistry.PHOTO.get().get(photo);
        if (pic == null) {
            return;
        }
        Direction facing = be.getBlockState().getValue(PinnedPhotoBlock.FACING);
        pose.pushPose();
        pose.translate(.5, .5, .5);
        switch (facing) {
            case UP -> {
                pose.mulPose(Axis.YP.rotationDegrees(be.angle()));
                pose.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case DOWN -> {
                pose.mulPose(Axis.YP.rotationDegrees(-be.angle()));
                pose.mulPose(Axis.XP.rotationDegrees(90));
            }
            default -> {
                pose.mulPose(Axis.YP.rotationDegrees(switch (facing) {
                    case NORTH -> 180;
                    case EAST -> 90;
                    case WEST -> -90;
                    default -> 0;
                }));
                pose.mulPose(Axis.ZP.rotationDegrees(be.angle()));
            }
        }
        pose.translate(0, 0, -.5 + .006);
        PoseStack.Pose p = pose.last();
        // The frame, then the picture just over it
        quad(buffers.getBuffer(RenderType.entityCutout(FRAME)), p, -W / 2, -H / 2, W / 2, H / 2, 0, 0, 1, 1, 0, 0xFFFFFFFF, light, overlay);
        float dev = Mth.clamp((PhotoClient.now() - PhotoClient.taken(photo)) / PhotoScreen.DEVELOP, 0, 1);
        Tex t = texture(pic, dev);
        if (t != null) {
            float top = H / 2 - 11 * U, left = -W / 2 + 11 * U, s = 128 * U;
            quad(buffers.getBuffer(RenderType.entitySolid(t.id)), p, left, top - s, left + s, top, 0, 0, 1, 1, .0012f, 0xFFFFFFFF, light, overlay);
        }
        // Who took it, written on the frame's foot
        String by = GearRegistry.PHOTO_BY.get().get(photo);
        if (by != null && !by.isBlank()) {
            pose.pushPose();
            pose.translate(0, H / 2 - 158 * U, .0016f);
            pose.mulPose(Axis.ZP.rotationDegrees(2));
            float k = U * .85f;
            pose.scale(k, -k, k);
            String text = font.plainSubstrByWidth(by, 150);
            font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFF2A3A78, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
        // The pin, on a wall: its round head and a glint
        if (facing.getAxis().isHorizontal()) {
            int c = PINS[Math.floorMod(be.pin(), PINS.length)];
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(FRAME));
            float y = H / 2 - 5 * U, r = 5 * U;
            quad(vc, p, -r, y - r, r, y + r, .3f, .9f, .32f, .92f, .006f, 0xFF000000 | shade(c, .6f), light, overlay);
            quad(vc, p, -r * .8f, y - r * .7f, r * .8f, y + r * .9f, .3f, .9f, .32f, .92f, .009f, 0xFF000000 | c, light, overlay);
            quad(vc, p, -r * .45f, y + r * .15f, -r * .05f, y + r * .55f, .3f, .9f, .32f, .92f, .011f, 0xFF000000 | shade(c, 1.6f), light, overlay);
        }
        pose.popPose();
    }

    private static int shade(int rgb, float k) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * k)), g = Math.min(255, (int) ((rgb >> 8 & 255) * k)), b = Math.min(255, (int) ((rgb & 255) * k));
        return r << 16 | g << 8 | b;
    }

    /** A flat face over (x0, y0)-(x1, y1) at depth z, facing out (+z); its texture's top at y1. */
    private static void quad(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
                             float z, int argb, int light, int overlay) {
        vc.addVertex(p, x0, y0, z).setColor(argb).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x1, y0, z).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x1, y1, z).setColor(argb).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x0, y1, z).setColor(argb).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
    }
}
