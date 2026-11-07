package com.waylays.arcanes.client.phone;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.PhonePackets;
import com.waylays.arcanes.registry.ModItems;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The Arcanophone's place in the game: the key that takes it out (N, if the player carries one), its use in hand, what
 * the server last told it (conversations, contacts, feed, account, positions, the weather to come), its notifications
 * (on the rune island when it is out, sliding in at the top of the screen when it is not), the clock and the sky it
 * shows, and the camera (the phone steps aside, the world is photographed without the interface, and it comes back).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class PhoneClient {
    public static final KeyMapping OPEN = new KeyMapping("key.arcanes_oublies.phone", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N,
            "key.categories.arcanes_oublies");

    public record Note(String app, String title, String text, long time, long shownAt) {}

    public record Hour(String label, int glyph, int temperature) {}

    public record Sky(String place, int temperature, String condition, String range, boolean night, boolean rain, boolean thunder, List<Hour> hours) {}

    public static List<PhonePackets.Conv> inbox = List.of();
    @Nullable
    public static PhonePackets.Thread thread;
    public static List<PhonePackets.Person> contacts = List.of();
    public static List<PhonePackets.Person> online = List.of();
    public static boolean sharing;
    public static List<PhonePackets.PostView> feed = List.of();
    public static long balance;
    public static List<PhonePackets.TxnView> history = List.of();
    public static List<PhonePackets.Spot> spots = List.of();
    public static long spotsAt;
    @Nullable
    public static PhonePackets.Weather weather;
    public static long weatherAt;
    private static final List<Note> NOTES = new ArrayList<>();
    private static final Map<String, Integer> UNSEEN = new HashMap<>();
    private static int photoStep = -1;
    private static boolean hidGui;
    public static long flashAt;

    private PhoneClient() {}

    // ---------------------------------------------------------------- the phone

    public static boolean carries(Player p) {
        if (p.isCreative()) {
            return true;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(ModItems.ARCANOPHONE.get())) {
                return true;
            }
        }
        return false;
    }

    public static void takeOut() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!carries(mc.player)) {
            mc.player.displayClientMessage(Component.translatable("phone.arcanes_oublies.no_phone"), true);
            return;
        }
        mc.setScreen(new PhoneScreen());
    }

    public static void ask(int what, String arg) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new PhonePackets.Ask(what, arg));
        }
    }

    // ---------------------------------------------------------------- what the server says

    public static void inbox(PhonePackets.Inbox p) {
        inbox = p.convs();
    }

    public static void thread(PhonePackets.Thread p) {
        if (thread == null || thread.id().equals(p.id()) || Minecraft.getInstance().screen instanceof PhoneScreen ps && ps.current() != null
                && "missives".equals(ps.current().id)) {
            thread = p;
        }
    }

    public static void contacts(PhonePackets.Contacts p) {
        List<PhonePackets.Person> list = new ArrayList<>(p.contacts());
        // The first entry is the player themselves: whether they share their position
        if (!list.isEmpty()) {
            sharing = list.remove(0).sharing();
        }
        contacts = list;
        online = p.online();
    }

    public static void feed(PhonePackets.Feed p) {
        feed = p.posts();
    }

    public static void account(PhonePackets.Account p) {
        balance = p.balance();
        history = p.history();
    }

    public static void positions(PhonePackets.Positions p) {
        spots = p.spots();
        spotsAt = System.currentTimeMillis();
    }

    public static void weather(PhonePackets.Weather p) {
        weather = p;
        weatherAt = System.currentTimeMillis();
    }

    public static void notice(PhonePackets.Notice n) {
        Minecraft mc = Minecraft.getInstance();
        String app;
        String title;
        String text;
        switch (n.kind()) {
            case "message" -> {
                app = "missives";
                title = n.title();
                text = n.text();
            }
            case "echo" -> {
                app = "echos";
                title = Component.translatable("phone.arcanes_oublies.new_echo", n.title()).getString();
                text = n.text();
            }
            case "bank" -> {
                app = "banque";
                title = Component.translatable("phone.arcanes_oublies.app.banque").getString();
                text = Component.translatable("phone.arcanes_oublies.bank_received", n.text(), n.title()).getString();
            }
            default -> {
                app = "reglages";
                title = Component.translatable("phone.arcanes_oublies.name").getString();
                text = Component.translatable(n.text(), n.title()).getString();
            }
        }
        long now = System.currentTimeMillis();
        NOTES.add(0, new Note(app, title, text, now, now));
        while (NOTES.size() > 12) {
            NOTES.remove(NOTES.size() - 1);
        }
        boolean viewing = mc.screen instanceof PhoneScreen ps && ps.current() != null && ps.current().id.equals(app);
        if (!viewing) {
            UNSEEN.merge(app, 1, Integer::sum);
        }
        PhoneSettings s = PhoneSettings.get();
        if (s.sounds && !s.quiet) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.9f, .9f));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5f, .5f));
        }
    }

    public static List<Note> notes() {
        return NOTES;
    }

    /** The newest notification, while it is fresh (for the island). */
    @Nullable
    public static Note fresh() {
        if (NOTES.isEmpty()) {
            return null;
        }
        Note n = NOTES.get(0);
        return System.currentTimeMillis() - n.shownAt() < 4000 ? n : null;
    }

    public static void dismissFresh() {
        if (!NOTES.isEmpty()) {
            Note n = NOTES.get(0);
            NOTES.set(0, new Note(n.app(), n.title(), n.text(), n.time(), 0));
        }
    }

    public static void seen(String app) {
        UNSEEN.remove(app);
    }

    public static int badge(String app) {
        if ("missives".equals(app)) {
            int n = 0;
            for (PhonePackets.Conv c : inbox) {
                n += c.unread();
            }
            return n;
        }
        return UNSEEN.getOrDefault(app, 0);
    }

    // ---------------------------------------------------------------- the clock, the sky, the player

    private static long dayTime() {
        Level level = Minecraft.getInstance().level;
        return level == null ? 6000 : level.getDayTime();
    }

    /** The time of day in the world, as hh:mm (0:00 at midnight). */
    public static String clock() {
        long t = Math.floorMod(dayTime(), 24000);
        int h = (int) ((t / 1000 + 6) % 24), m = (int) (t % 1000 * 60 / 1000);
        return String.format("%d:%02d", h, m);
    }

    public static String dateLine() {
        long day = dayTime() / 24000;
        Level level = Minecraft.getInstance().level;
        int moon = level == null ? 0 : level.getMoonPhase();
        return Component.translatable("phone.arcanes_oublies.weekday." + Math.floorMod(day, 7)).getString() + " · "
                + Component.translatable("phone.arcanes_oublies.day", day + 1).getString() + " · "
                + Component.translatable("phone.arcanes_oublies.moon." + moon).getString();
    }

    public static boolean night(long t) {
        long d = Math.floorMod(t, 24000);
        return d > 13000 && d < 23000;
    }

    /** The temperature (°C) of a biome's base temperature at a time of day. */
    public static int celsius(float base, long t) {
        double day = Math.sin((Math.floorMod(t, 24000) - 0) / 24000.0 * Math.PI * 2);
        return (int) Math.round(base * 22 + 4 + 5 * day);
    }

    public static Sky sky() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) {
            return new Sky("—", 20, "", "", false, false, false, List.of(new Hour("", 0, 0), new Hour("", 0, 0), new Hour("", 0, 0), new Hour("", 0, 0),
                    new Hour("", 0, 0), new Hour("", 0, 0)));
        }
        BlockPos pos = mc.player.blockPosition();
        Holder<Biome> biome = level.getBiome(pos);
        String place = biome.unwrapKey().map(ResourceKey::location)
                .map(id -> Component.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString()).orElse("—");
        float base = biome.value().getBaseTemperature();
        long t = level.getDayTime();
        boolean rain = level.isRaining(), thunder = level.isThundering();
        boolean snow = rain && base < .15f;
        boolean night = night(t);
        int temp = celsius(base, t);
        String condition = Component.translatable("phone.arcanes_oublies.sky." + (thunder ? "thunder" : snow ? "snow" : rain ? "rain" : night ? "night" : "clear"))
                .getString();
        String range = Component.translatable("phone.arcanes_oublies.sky.range", celsius(base, 6000), celsius(base, 18000)).getString();
        List<Hour> hours = new ArrayList<>();
        PhonePackets.Weather w = weather;
        long since = w == null ? 0 : (System.currentTimeMillis() - weatherAt) / 50;
        for (int k = 1; k <= 6; k++) {
            long at = t + k * 1000L;
            boolean wet = rain;
            if (w != null) {
                long ahead = k * 1000L + since;
                if (!w.raining() && w.clear() > 0 && ahead > w.clear()) {
                    wet = true;
                } else if (w.raining() && w.rain() > 0 && ahead > w.rain()) {
                    wet = false;
                }
            }
            int glyph = night(at) ? (wet ? 4 : 1) : wet ? (thunder ? 3 : 2) : 0;
            int hh = (int) ((Math.floorMod(at, 24000) / 1000 + 6) % 24);
            hours.add(new Hour(String.valueOf(hh), glyph, celsius(base, at)));
        }
        return new Sky(place, temp, condition, range, night, rain, thunder, hours);
    }

    /** The player's mana, 0 to 1 (Iron's Spells keeps it on the client for its own bar). */
    public static float manaFraction() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        double max = mc.player.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA);
        return max <= 0 ? 0 : (float) Math.min(1, io.redspace.ironsspellbooks.player.ClientMagicData.getPlayerMana() / max);
    }

    public static float healthFraction() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 1 : Math.min(1, mc.player.getHealth() / mc.player.getMaxHealth());
    }

    // ---------------------------------------------------------------- photos

    private static final Map<Path, ResourceLocation> THUMBS = new HashMap<>();
    private static int loads;

    public static List<Path> photoFiles() {
        Path dir = PhoneSettings.photos();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".png")).sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed()).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** A small texture of a photo (128 x 96), loaded the first time it is asked for (a few a frame). */
    @Nullable
    public static ResourceLocation thumb(Path file) {
        ResourceLocation id = THUMBS.get(file);
        if (id != null || loads > 2) {
            return id;
        }
        loads++;
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage full = NativeImage.read(in);
            NativeImage small = new NativeImage(128, 96, false);
            full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), small);
            full.close();
            id = ArcanesOublies.id("phone_photo/" + Integer.toHexString(file.toString().hashCode()));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(small));
            THUMBS.put(file, id);
        } catch (Exception e) {
            ArcanesOublies.LOGGER.debug("Could not read the photo {}", file, e);
        }
        return id;
    }

    public static void forget(Path file) {
        ResourceLocation id = THUMBS.remove(file);
        if (id != null) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
    }

    public static List<ResourceLocation> recentPhotos(int n) {
        List<ResourceLocation> out = new ArrayList<>();
        for (Path p : photoFiles()) {
            if (out.size() >= n) {
                break;
            }
            ResourceLocation id = thumb(p);
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    /** Takes a photo: the phone steps aside and the interface hides for a moment. */
    public static void photo() {
        Minecraft mc = Minecraft.getInstance();
        hidGui = mc.options.hideGui;
        mc.setScreen(null);
        mc.options.hideGui = true;
        photoStep = 3;
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        loads = 0;
        if (photoStep < 0) {
            return;
        }
        if (photoStep-- > 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        mc.options.hideGui = hidGui;
        String name = "photo_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
        Path dir = PhoneSettings.photos();
        Util.ioPool().execute(() -> {
            try {
                Files.createDirectories(dir);
                img.writeToFile(dir.resolve(name));
            } catch (Exception e) {
                ArcanesOublies.LOGGER.warn("Could not save the photo", e);
            } finally {
                img.close();
            }
        });
        flashAt = System.currentTimeMillis();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.6f, .8f));
        mc.setScreen(new PhoneScreen("photo"));
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ArcanesOublies.id("phone_notice"), PhoneClient::toast);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                takeOut();
            }
        }
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide && event.getItemStack().is(ModItems.ARCANOPHONE.get())) {
            Minecraft.getInstance().setScreen(new PhoneScreen());
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        inbox = List.of();
        thread = null;
        contacts = List.of();
        online = List.of();
        feed = List.of();
        spots = List.of();
        weather = null;
        NOTES.clear();
        UNSEEN.clear();
    }

    /** A notification while the phone is put away: a black island sliding down at the top of the screen. */
    private static void toast(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Note n = fresh();
        if (n == null || mc.options.hideGui || mc.screen instanceof PhoneScreen) {
            return;
        }
        long age = System.currentTimeMillis() - n.shownAt();
        float in = PhoneUI.spring(age / 450f) * (1 - PhoneUI.ease((age - 3500) / 400f));
        if (in <= 0) {
            return;
        }
        int w = (int) (60 + 150 * Math.max(0, in)), h = (int) (14 + 20 * Math.max(0, in));
        int x = g.guiWidth() / 2 - w / 2, y = 4;
        PhoneUI.round(g, x - 1, y - 1, w + 2, h + 2, h / 2 + 1, 0x88B98CFF);
        PhoneUI.round(g, x, y, w, h, h / 2, 0xF0000000);
        if (in > .7f) {
            PhoneUI.icon(g, n.app(), x + 7, y + 7, 20);
            PhoneUI.text(g, PhoneUI.fit(n.title(), w - 70, .75f), x + 32, y + 8, 0xFFFFFFFF, .75f);
            PhoneUI.text(g, PhoneUI.fit(n.text(), w - 40, .65f), x + 32, y + 19, 0xFFB8B4C8, .65f);
            PhoneUI.right(g, OPEN.getTranslatedKeyMessage().getString(), x + w - 10, y + 8, 0xFF8A86A0, .6f);
        }
    }
}
