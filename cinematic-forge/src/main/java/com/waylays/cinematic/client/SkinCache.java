package com.waylays.cinematic.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.waylays.cinematic.CinematicMod;
import com.waylays.cinematic.net.ActionMsg;
import com.waylays.cinematic.net.DownloadMsg;
import com.waylays.cinematic.net.Net;
import com.waylays.cinematic.net.UploadMsg;
import com.waylays.cinematic.studio.Skins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Actor skins on the client: PNGs of any size (64 to 4096, square) turned into textures, kept in
 * .minecraft/cinematic/skin_cache by their SHA-1 and asked from the server when missing.
 */
public final class SkinCache {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, ResourceLocation> textures = new HashMap<>();
    private static final Set<String> requested = new HashSet<>();
    private static final Set<String> broken = new HashSet<>();
    private static final Map<String, Net.Assembler> parts = new HashMap<>();

    private SkinCache() {}

    public static Path skinsFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("cinematic").resolve("skins");
    }

    private static Path cacheFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("cinematic").resolve("skin_cache");
    }

    public static void clearRequests() {
        requested.clear();
        parts.clear();
    }

    /** The texture of a skin, or null while it is not here yet (the default skin is drawn meanwhile). */
    public static ResourceLocation get(String hash) {
        if (hash == null || hash.isEmpty() || !Skins.validHash(hash) || broken.contains(hash)) return null;
        ResourceLocation rl = textures.get(hash);
        if (rl != null) return rl;
        Path cached = cacheFolder().resolve(hash + ".png");
        if (Files.isRegularFile(cached)) {
            try {
                return load(hash, Files.readAllBytes(cached));
            } catch (IOException e) {
                LOG.warn("Cinematic: cached skin {} unreadable", hash, e);
            }
        }
        if (requested.add(hash) && ClientStudio.serverHasMod()) {
            Net.CHANNEL.sendToServer(new UploadMsg(UploadMsg.SKIN_REQUEST, hash, 0, 0, new byte[0]));
        }
        return null;
    }

    private static ResourceLocation load(String hash, byte[] bytes) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            ResourceLocation rl = new ResourceLocation(CinematicMod.MOD_ID, "actor_skins/" + hash);
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(img));
            textures.put(hash, rl);
            return rl;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Cinematic: skin {} could not be read", hash, e);
            broken.add(hash);
            return null;
        }
    }

    static void receive(DownloadMsg msg) {
        String hash = msg.key();
        if (!Skins.validHash(hash)) return;
        Net.Assembler as = parts.get(hash);
        if (msg.offset() == 0 || as == null || as.total() != msg.total()) {
            as = new Net.Assembler(msg.total());
            parts.put(hash, as);
        }
        byte[] all;
        try {
            all = as.add(msg.offset(), msg.data());
        } catch (IllegalArgumentException e) {
            parts.remove(hash);
            return;
        }
        if (all == null) return;
        parts.remove(hash);
        if (!Skins.sha1(all).equals(hash)) return;
        store(hash, all);
        load(hash, all);
    }

    private static void store(String hash, byte[] bytes) {
        try {
            Files.createDirectories(cacheFolder());
            Files.write(cacheFolder().resolve(hash + ".png"), bytes);
        } catch (IOException e) {
            LOG.warn("Cinematic: could not cache skin {}", hash, e);
        }
    }

    /** PNG files in .minecraft/cinematic/skins, sorted by name. */
    public static List<Path> folderSkins() {
        List<Path> out = new ArrayList<>();
        try {
            Files.createDirectories(skinsFolder());
            try (Stream<Path> files = Files.list(skinsFolder())) {
                files.filter(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).sorted().forEach(out::add);
            }
        } catch (IOException e) {
            LOG.warn("Cinematic: could not list {}", skinsFolder(), e);
        }
        return out;
    }

    /**
     * Gives a PNG file to an actor: checked, copied into the skins folder, cached, sent to the server, applied.
     * Returns a translation key of the problem, or null when it went.
     */
    public static String upload(UUID actor, Path file) {
        byte[] bytes;
        try {
            if (Files.size(file) > Skins.MAX_BYTES) return "cinematic.skin.too_heavy";
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            return "cinematic.skin.unreadable";
        }
        String problem = Skins.problem(bytes, 4096);
        if (problem != null) return problem;
        String hash = Skins.sha1(bytes);
        store(hash, bytes);
        if (load(hash, bytes) == null) return "cinematic.skin.unreadable";
        try {
            Path keep = skinsFolder().resolve(file.getFileName().toString());
            if (!Files.exists(keep)) {
                Files.createDirectories(skinsFolder());
                Files.write(keep, bytes);
            }
        } catch (IOException | RuntimeException ignored) {
            // Keeping a copy in the folder is only a convenience
        }
        Net.chunks(bytes, Net.UP_CHUNK, (off, piece) -> Net.CHANNEL.sendToServer(new UploadMsg(UploadMsg.SKIN, hash, bytes.length, off, piece)));
        ClientStudio.send(ActionMsg.Action.SET_SKIN, actor, hash, 0);
        return null;
    }
}
