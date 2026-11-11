package com.waylays.eraphones.gear.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * An instant photograph held up to look at: its white frame, the picture as instant film gives it (warm, soft, the
 * blacks lifted, darker at the corners) and, fresh out of the camera, still developing: a murky blue-grey first, the
 * shapes coming, then the colours, over a minute.
 */
public class PhotoScreen extends Screen {
    /** How long a photograph takes to develop (ticks). */
    public static final float DEVELOP = 60 * 20;
    private static final ResourceLocation FRAME = EraPhones.id("textures/gui/gear/polaroid.png");
    private static int made;
    private final ItemStack photo;
    private final int[] px;
    private final int side;
    private DynamicTexture texture;
    private ResourceLocation id;
    private float shownDev = -1;

    public PhotoScreen(ItemStack photo) {
        super(Component.translatable("item.eraphones.photo"));
        this.photo = photo;
        com.waylays.eraphones.gear.Picture pic = GearRegistry.PHOTO.get().get(photo);
        int[] p = PhotoClient.unpack(pic == null ? null : pic.data());
        this.side = p == null ? 1 : p[0];
        this.px = p;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (texture == null) {
            texture = new DynamicTexture(side, side, false);
            texture.setFilter(true, false);
            id = EraPhones.id("photo_view_" + made++);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
    }

    private float develop() {
        return Mth.clamp((PhotoClient.now() - PhotoClient.taken(photo)) / DEVELOP, 0, 1);
    }

    private void paint(float dev) {
        NativeImage img = texture.getPixels();
        if (img == null || px == null) {
            return;
        }
        paint(img, px, side, dev);
        texture.upload();
    }

    /** Paints {@code px} (a photograph's pixels, its side first) into {@code img} as instant film gives it, {@code dev} developed (0 to 1). */
    static void paint(NativeImage img, int[] px, int side, float dev) {
        float shapes = Mth.clamp(dev * 2.2f, 0, 1), colours = smooth(Mth.clamp((dev - .25f) / .75f, 0, 1));
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int c = px[1 + y * side + x];
                float r = (c >> 16 & 255), g = (c >> 8 & 255), b = (c & 255);
                // Instant film: blacks lifted, warm, a little faded, darker towards the corners
                float nx = (x + .5f) / side * 2 - 1, ny = (y + .5f) / side * 2 - 1;
                float vig = 1 - .28f * (nx * nx + ny * ny) / 2;
                r = (18 + r * .9f * 1.06f) * vig;
                g = (16 + g * .9f * 1.0f) * vig;
                b = (20 + b * .9f * .86f) * vig;
                float lum = r * .3f + g * .59f + b * .11f;
                // Developing: a murky blue-grey, the shapes in grey, then the colours
                float mr = 58 + (lum * .55f - 58) * shapes, mg = 66 + (lum * .58f - 66) * shapes, mb = 74 + (lum * .66f - 74) * shapes;
                int R = (int) Mth.clamp(mr + (r - mr) * colours, 0, 255);
                int G = (int) Mth.clamp(mg + (g - mg) * colours, 0, 255);
                int B = (int) Mth.clamp(mb + (b - mb) * colours, 0, 255);
                img.setPixelRGBA(x, y, 0xFF000000 | B << 16 | G << 8 | R);
            }
        }
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        float dev = develop();
        if (Math.abs(dev - shownDev) > .01f || shownDev < 0) {
            shownDev = dev;
            paint(dev);
        }
        // The frame: a print 150 x 180 units, its picture a 128 square near the top
        float scale = Math.min((height - 30) / 180f, (width - 40) / 150f);
        g.pose().pushPose();
        g.pose().translate((width - 150 * scale) / 2, (height - 180 * scale) / 2, 0);
        g.pose().scale(scale, scale, 1);
        g.blit(FRAME, 0, 0, 150, 180, 0, 0, 300, 360, 300, 360);
        g.blit(id, 11, 11, 128, 128, 0, 0, side, side, side, side);
        String by = GearRegistry.PHOTO_BY.get().get(photo);
        if (by != null) {
            g.pose().pushPose();
            g.pose().translate(75, 152, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-2));
            Component c = Component.literal(by).withStyle(net.minecraft.ChatFormatting.ITALIC);
            g.drawString(font, c, -font.width(c) / 2, 0, 0xFF2A3A78, false);
            g.pose().popPose();
        }
        g.pose().popPose();
    }

    @Override
    public void removed() {
        super.removed();
        if (id != null) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
    }
}
