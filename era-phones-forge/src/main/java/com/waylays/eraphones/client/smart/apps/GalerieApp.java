package com.waylays.eraphones.client.smart.apps;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Galerie: the photos taken with the phone, newest first, in a grid; one opens large, to delete or keep. */
public class GalerieApp extends PhoneApp {
    private static final ResourceLocation FULL = EraPhones.id("phone_photo_full");
    @Nullable
    private Path viewing;
    private boolean fullLoaded;

    public GalerieApp() {
        super("galerie");
    }

    @Override
    public void open() {
        viewing = null;
        scroll = 0;
    }

    @Override
    public int[] background() {
        return new int[] {0xFF0C0C10, 0xFF050507};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (viewing != null) {
            view(g, mx, my);
            return;
        }
        header(g, title(), mx, my, "⧉");
        List<Path> photos = PhoneClient.photoFiles();
        PhoneUI.text(g, Component.translatable("phone.eraphones.photo_count", photos.size()).getString(), X0 + 10, Y0 + 2, PhoneUI.DIM, .6f);
        if (photos.isEmpty()) {
            PhoneUI.icon(g, "galerie", (X0 + X1) / 2f - 16, Y0 + 60, 32);
            PhoneUI.paragraph(g, Component.translatable("phone.eraphones.no_photos").getString(), (X0 + X1) / 2f, Y0 + 100, X1 - X0 - 28, PhoneUI.DIM, .65f);
            return;
        }
        clip(g, Y0 + 12, Y1);
        int cell = (X1 - X0 - 16) / 3;
        for (int i = 0; i < photos.size(); i++) {
            int x = X0 + 6 + (i % 3) * (cell + 2), y = Y0 + 14 + (i / 3) * (cell + 2) - (int) scroll;
            if (y > Y1 || y + cell < Y0) {
                continue;
            }
            ResourceLocation t = PhoneClient.thumb(photos.get(i));
            if (t != null) {
                PhoneUI.blend();
                g.blit(t, x, y, cell, cell, 16, 0, 96, 96, 128, 96);
            } else {
                g.fill(x, y, x + cell, y + cell, 0xFF201C28);
            }
        }
        unclip(g);
    }

    private void view(GuiGraphics g, int mx, int my) {
        if (!fullLoaded) {
            load(viewing);
        }
        header(g, Component.literal(viewing.getFileName().toString().replace("photo_", "").replace(".png", "")), mx, my, "✕");
        int w = X1 - X0 - 8, h = w * 9 / 16;
        int y = (Y0 + Y1) / 2 - h / 2 - 10;
        PhoneUI.blend();
        g.blit(FULL, X0 + 4, y, w, h, 0, 0, 320, 180, 320, 180);
        PhoneUI.centered(g, Component.translatable("phone.eraphones.delete_hint").getString(), (X0 + X1) / 2f, y + h + 10, PhoneUI.FAINT, .55f);
    }

    private void load(Path file) {
        fullLoaded = true;
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage full = NativeImage.read(in);
            NativeImage fit = new NativeImage(320, 180, false);
            full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), fit);
            full.close();
            mc.getTextureManager().register(FULL, new DynamicTexture(fit));
        } catch (Exception e) {
            EraPhones.LOGGER.debug("Could not open the photo {}", file, e);
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (viewing != null) {
            if (headerBack(mx, my)) {
                return back();
            }
            if (headerAction(mx, my)) {
                click();
                try {
                    PhoneClient.forget(viewing);
                    Files.deleteIfExists(viewing);
                } catch (Exception ignored) {
                }
                return back();
            }
            return true;
        }
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (headerAction(mx, my)) {
            try {
                Files.createDirectories(PhoneSettings.photos());
            } catch (Exception ignored) {
            }
            Util.getPlatform().openFile(PhoneSettings.photos().toFile());
            return true;
        }
        List<Path> photos = PhoneClient.photoFiles();
        int cell = (X1 - X0 - 16) / 3;
        for (int i = 0; i < photos.size(); i++) {
            int x = X0 + 6 + (i % 3) * (cell + 2), y = Y0 + 14 + (i / 3) * (cell + 2) - (int) scroll;
            if (PhoneUI.in(mx, my, x, y, cell, cell) && my > Y0 + 12) {
                click();
                viewing = photos.get(i);
                fullLoaded = false;
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean back() {
        if (viewing != null) {
            viewing = null;
            return true;
        }
        return false;
    }
}
