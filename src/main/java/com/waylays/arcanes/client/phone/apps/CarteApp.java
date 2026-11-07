package com.waylays.arcanes.client.phone.apps;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.client.phone.PhoneSettings;
import com.waylays.arcanes.client.phone.PhoneUI;
import com.waylays.arcanes.network.PhonePackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Carte: a live map of the land round the player, drawn from the blocks the game has loaded (their map colours, shaded by
 * the lie of the land), north up: the player's arrow at its middle, the contacts who share their position, the places
 * marked; it zooms, and marks the place where the player stands.
 */
public class CarteApp extends PhoneApp {
    private static final int N = 128;
    private static final ResourceLocation TEX = ArcanesOublies.id("phone_map");
    @Nullable
    private DynamicTexture texture;
    private int zoom = 1;
    private int age;
    private double cx, cz;
    @Nullable
    private UUID focus;

    public CarteApp() {
        super("carte");
    }

    public void focus(UUID who) {
        focus = who;
    }

    @Override
    public void open() {
        age = 0;
        if (texture == null) {
            texture = new DynamicTexture(N, N, false);
            mc.getTextureManager().register(TEX, texture);
        }
        draw();
    }

    @Override
    public void tick() {
        if (++age % 20 == 0) {
            draw();
        }
    }

    /** Draws the map round the player: one pixel for {@code zoom} blocks. */
    private void draw() {
        Level level = mc.level;
        if (level == null || mc.player == null || texture == null || texture.getPixels() == null) {
            return;
        }
        NativeImage img = texture.getPixels();
        cx = mc.player.getX();
        cz = mc.player.getZ();
        int x0 = Mth.floor(cx) - N / 2 * zoom, z0 = Mth.floor(cz) - N / 2 * zoom;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int px = 0; px < N; px++) {
            int last = Integer.MIN_VALUE;
            for (int pz = -1; pz < N; pz++) {
                int x = x0 + px * zoom, z = z0 + pz * zoom;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    if (pz >= 0) {
                        img.setPixelRGBA(px, pz, 0xFF1A1418);
                    }
                    last = Integer.MIN_VALUE;
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                pos.set(x, y, z);
                BlockState st = level.getBlockState(pos);
                MapColor color = st.getMapColor(level, pos);
                if (pz >= 0) {
                    MapColor.Brightness b = last == Integer.MIN_VALUE || y == last ? MapColor.Brightness.NORMAL : y > last ? MapColor.Brightness.HIGH
                            : MapColor.Brightness.LOW;
                    img.setPixelRGBA(px, pz, color == MapColor.NONE ? 0xFF241C20 : color.calculateRGBColor(b));
                }
                last = y;
            }
        }
        texture.upload();
    }

    @Override
    public int[] background() {
        return new int[] {0xFF101820, 0xFF080C12};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, zoom == 1 ? "−" : "+");
        int size = X1 - X0 - 12, x = X0 + 6, y = Y0 + 2;
        PhoneUI.round(g, x - 1, y - 1, size + 2, size + 2, 10, 0x55FFFFFF);
        phone.clip(g, x, y, x + size, y + size);
        PhoneUI.blend();
        g.blit(TEX, x, y, size, size, 0, 0, N, N, N, N);
        unclip(g);
        float k = size / (float) (N * zoom);
        // Marked places
        int wi = 0;
        for (String w : PhoneSettings.get().waypoints) {
            double[] p = parse(w);
            if (p == null) {
                continue;
            }
            float ox = (float) (p[0] - cx) * k, oz = (float) (p[1] - cz) * k;
            pin(g, x + size / 2f + ox, y + size / 2f + oz, size, x, y, 0xFFFFD27A, String.valueOf(++wi));
        }
        // Contacts who share where they are
        if (mc.level != null) {
            String dim = mc.level.dimension().location().toString();
            for (PhonePackets.Spot s : PhoneClient.spots) {
                if (!s.dimension().equals(dim)) {
                    continue;
                }
                float ox = (float) (s.x() - cx) * k, oz = (float) (s.z() - cz) * k;
                float fx = Mth.clamp(x + size / 2f + ox, x + 6, x + size - 6), fy = Mth.clamp(y + size / 2f + oz, y + 6, y + size - 6);
                boolean focused = s.id().equals(focus);
                PhoneUI.glow(g, fx, fy, focused ? 22 : 14, focused ? 0xCCFF7AD9 : 0x88B98CFF);
                PhoneUI.face(g, s.id(), (int) fx - 5, (int) fy - 5, 10);
            }
        }
        // The player's arrow
        if (mc.player != null) {
            float px = x + size / 2f, py = y + size / 2f;
            PhoneUI.glow(g, px, py, 18, 0x885CB4FF);
            g.pose().pushPose();
            g.pose().translate(px, py, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(mc.player.getYRot() + 180));
            PhoneUI.round(g, -3, -3, 6, 6, 3, 0xFFFFFFFF);
            g.fill(-1, -7, 1, -2, 0xFF5CB4FF);
            g.fill(-2, -5, 2, -3, 0xFF5CB4FF);
            g.pose().popPose();
        }
        PhoneUI.text(g, "N", x + size / 2f - 2, y + 3, 0xFFFF5C6E, .7f);
        // Under the map: a card with where the player is (and the biome), the button to mark it; then the places marked
        int ty = y + size + 6;
        PhoneUI.glass(g, X0 + 6, ty, X1 - X0 - 12, 26, 9, 0x24FFFFFF);
        if (mc.player != null && mc.level != null) {
            BlockPos b = mc.player.blockPosition();
            PhoneUI.text(g, "⌖ " + b.getX() + ", " + b.getY() + ", " + b.getZ(), X0 + 12, ty + 5, PhoneUI.WHITE, .65f);
            String biome = mc.level.getBiome(b).unwrapKey().map(key -> Component.translatable("biome." + key.location().toLanguageKey()).getString())
                    .orElse("");
            PhoneUI.text(g, PhoneUI.fit(biome, 80, .55f), X0 + 12, ty + 15, PhoneUI.DIM, .55f);
        }
        button(g, X1 - 58, ty + 6, 48, 13, Component.translatable("phone.arcanes_oublies.mark").getString(), 0xFF2E7AD0, mx, my);
        ty += 32;
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.places").getString().toUpperCase(), X0 + 10, ty, PhoneUI.FAINT, .55f);
        ty += 9;
        int n = 0;
        for (String w : PhoneSettings.get().waypoints) {
            double[] p = parse(w);
            if (p == null || mc.player == null) {
                continue;
            }
            n++;
            if (ty > Y1 - 12) {
                break;
            }
            double d = Math.hypot(p[0] - mc.player.getX(), p[1] - mc.player.getZ());
            boolean hover = PhoneUI.in(mx, my, X0 + 6, ty - 1, X1 - X0 - 12, 12);
            PhoneUI.round(g, X0 + 6, ty - 1, X1 - X0 - 12, 12, 5, hover ? 0x22FFFFFF : 0x12FFFFFF);
            PhoneUI.round(g, X0 + 9, ty + 1, 8, 8, 4, 0xFFFFD27A);
            PhoneUI.centered(g, String.valueOf(n), X0 + 13, ty + 2.5f, 0xFF201010, .55f);
            PhoneUI.text(g, PhoneUI.fit(w.split(";")[0], 70, .6f), X0 + 21, ty + 2, PhoneUI.WHITE, .6f);
            PhoneUI.right(g, (int) d + " m", X1 - 24, ty + 2, PhoneUI.DIM, .6f);
            PhoneUI.text(g, "✕", X1 - 18, ty + 2, PhoneUI.alpha(PhoneUI.RED, .9f), .6f);
            ty += 13;
        }
        if (n == 0) {
            PhoneUI.paragraph(g, Component.translatable("phone.arcanes_oublies.no_places").getString(), (X0 + X1) / 2f, ty + 4, X1 - X0 - 30,
                    PhoneUI.FAINT, .6f);
        }
    }

    private void pin(GuiGraphics g, float fx, float fy, int size, int x, int y, int color, String label) {
        fx = Mth.clamp(fx, x + 5, x + size - 5);
        fy = Mth.clamp(fy, y + 5, y + size - 5);
        PhoneUI.round(g, (int) fx - 4, (int) fy - 4, 8, 8, 4, color);
        PhoneUI.centered(g, label, fx, fy - 2.5f, 0xFF201010, .55f);
    }

    /** A marked place: "name;x;z". */
    @Nullable
    private static double[] parse(String w) {
        String[] p = w.split(";");
        if (p.length < 3) {
            return null;
        }
        try {
            return new double[] {Double.parseDouble(p[1]), Double.parseDouble(p[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (headerAction(mx, my)) {
            click();
            zoom = zoom == 1 ? 2 : 1;
            draw();
            return true;
        }
        int size = X1 - X0 - 12, ty = Y0 + 2 + size + 6;
        if (PhoneUI.in(mx, my, X1 - 58, ty + 6, 48, 13) && mc.player != null) {
            click();
            BlockPos b = mc.player.blockPosition();
            PhoneSettings s = PhoneSettings.get();
            s.waypoints.add(Component.translatable("phone.arcanes_oublies.place", s.waypoints.size() + 1).getString() + ";" + b.getX() + ";" + b.getZ());
            s.save();
            return true;
        }
        ty += 32 + 9;
        PhoneSettings s = PhoneSettings.get();
        for (int i = 0; i < s.waypoints.size(); i++) {
            if (parse(s.waypoints.get(i)) == null) {
                continue;
            }
            if (PhoneUI.in(mx, my, X1 - 22, ty - 1, 16, 12)) {
                click();
                s.waypoints.remove(i);
                s.save();
                return true;
            }
            ty += 13;
        }
        return true;
    }
}
