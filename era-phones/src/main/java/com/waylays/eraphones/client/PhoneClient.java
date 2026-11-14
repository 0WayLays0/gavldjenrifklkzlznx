package com.waylays.eraphones.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.eraphones.Era;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.Signal;
import com.waylays.eraphones.client.retro.BrickScreen;
import com.waylays.eraphones.client.retro.FlipScreen;
import com.waylays.eraphones.client.retro.GsmScreen;
import com.waylays.eraphones.client.smart.SmartScreen;
import com.waylays.eraphones.item.PhoneItem;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * The phones on this side: which phone is in use (its slot, number, era), what the server last said about its line
 * (messages, directory, calls, social feed, news, positions, weather), the call in progress, the notifications, the
 * phone key (it takes out the phone in hand or in the hotbar; while a phone rings, the ringing one), the clock and
 * the sky the phones show, and the camera.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class PhoneClient {
    public static final KeyMapping OPEN = new KeyMapping("key.eraphones.phone", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N,
            "key.categories.eraphones");

    public record Note(String app, String title, String text, long time, long shownAt) {}

    public record Hour(String label, int glyph, int temperature) {}

    public record Sky(String place, int temperature, String condition, String range, boolean night, boolean rain, boolean thunder, List<Hour> hours) {}

    /** A line said during the call. */
    public record Said(boolean mine, String text, long at) {}

    // The phone in use
    public static int slot = -1;
    public static String number = "";
    public static Era era = Era.SMART;
    public static String handle = "";
    public static boolean sharing, silent;

    // Its line, as the server last told it
    public static List<PhoneNet.Conv> inbox = List.of();
    @Nullable
    public static PhoneNet.Thread thread;
    public static List<PhoneNet.ContactView> contacts = List.of();
    public static List<PhoneNet.CallView> calls = List.of();
    public static List<PhoneNet.PostView> feed = List.of();
    public static List<PhoneNet.NewsView> news = List.of();
    public static List<PhoneNet.Spot> spots = List.of();
    public static long spotsAt;
    @Nullable
    public static PhoneNet.Weather weather;
    public static long weatherAt;

    // The call
    public static int callState;
    public static String callMine = "", callNumber = "", callDisplay = "", callReason = "";
    public static long callSince, callEndedAt;
    public static final List<Said> SAID = new ArrayList<>();
    private static int ringBack;

    // A phone gone wrong
    public static long hauntUntil;
    public static String hauntText = "";

    private static final List<Note> NOTES = new ArrayList<>();
    private static final Map<String, Integer> UNSEEN = new HashMap<>();
    private static int photoStep = -1;
    private static boolean hidGui, vga;
    public static long flashAt;

    private PhoneClient() {}

    // ---------------------------------------------------------------- the phone in use

    public static ItemStack stack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack st = slot == 40 ? mc.player.getOffhandItem() : slot < mc.player.getInventory().items.size() ? mc.player.getInventory().items.get(slot)
                : ItemStack.EMPTY;
        if (!(st.getItem() instanceof PhoneItem)) {
            return ItemStack.EMPTY;
        }
        String n = PhoneItem.number(st);
        return number.isEmpty() || n == null || n.equals(number) ? st : ItemStack.EMPTY;
    }

    /** The battery of the phone in use, 0 to 1000. */
    public static int battery() {
        ItemStack st = stack();
        return st.isEmpty() ? 0 : PhoneItem.battery(st);
    }

    /** The bars of network where the player stands. */
    public static int bars() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || mc.player == null ? 0 : Signal.bars(mc.level, mc.player.blockPosition(), era);
    }

    /** How a number shows: the name it is saved under, "Unknown number" when hidden. */
    public static String name(String number, String display) {
        if (Numbers.hidden(number)) {
            return Component.translatable("phone.eraphones.hidden").getString();
        }
        return display == null || display.isEmpty() ? number : display;
    }

    public static String name(String number) {
        for (PhoneNet.ContactView c : contacts) {
            if (c.number().equals(number)) {
                return c.name();
            }
        }
        return name(number, number);
    }

    /** The slot of the phone to take out: in hand, in the off hand, then the hotbar's first; -1 when none. */
    public static int handySlot(Player p) {
        if (p.getMainHandItem().getItem() instanceof PhoneItem) {
            return p.getInventory().selected;
        }
        if (p.getOffhandItem().getItem() instanceof PhoneItem) {
            return 40;
        }
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().items.get(i).getItem() instanceof PhoneItem) {
                return i;
            }
        }
        return -1;
    }

    /** The slot of the phone of {@code n} anywhere on the player, or -1. */
    public static int slotOf(Player p, String n) {
        if (n.equals(PhoneItem.number(p.getOffhandItem()))) {
            return 40;
        }
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            if (n.equals(PhoneItem.number(p.getInventory().items.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    public static void open(int at) {
        open(at, null);
    }

    /** Takes out the phone in {@code at} (its own screen for its era), on {@code app} if given. */
    public static void open(int at, @Nullable String app) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack st = at == 40 ? mc.player.getOffhandItem() : mc.player.getInventory().items.get(at);
        if (!(st.getItem() instanceof PhoneItem phone)) {
            return;
        }
        String n = PhoneItem.number(st);
        if (n == null || !n.equals(number)) {
            forgetLine();
            number = n == null ? "" : n;
        }
        slot = at;
        era = phone.era;
        if (mc.getConnection() != null) {
            PacketDistributor.sendToServer(new PhoneNet.Open(at));
        }
        mc.setScreen(screenFor(era, app));
    }

    public static Screen screenFor(Era e, @Nullable String app) {
        return switch (e) {
            case SMART -> new SmartScreen(app);
            case FLIP -> new FlipScreen(app);
            case GSM -> new GsmScreen();
            case BRICK -> new BrickScreen();
        };
    }

    /** Whether one of the phones' own screens is open. */
    public static boolean phoneOpen() {
        Screen s = Minecraft.getInstance().screen;
        return s instanceof SmartScreen || s instanceof com.waylays.eraphones.client.retro.RetroScreen;
    }

    public static void closed() {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new PhoneNet.Close());
        }
    }

    public static void ask(int what, String arg) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new PhoneNet.Ask(what, arg));
        }
    }

    public static void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    private static void forgetLine() {
        inbox = List.of();
        thread = null;
        contacts = List.of();
        calls = List.of();
        feed = List.of();
        spots = List.of();
        handle = "";
        sharing = false;
        silent = false;
    }

    // ---------------------------------------------------------------- what the server says

    public static void hello(PhoneNet.Hello p) {
        if (!p.number().equals(number)) {
            forgetLine();
        }
        number = p.number();
        era = Era.byId(p.era());
        handle = p.handle();
        sharing = p.sharing();
        silent = p.silent();
    }

    public static void inbox(PhoneNet.Inbox p) {
        inbox = p.convs();
    }

    public static void thread(PhoneNet.Thread p) {
        thread = p;
    }

    public static void contacts(PhoneNet.Contacts p) {
        contacts = p.contacts();
    }

    public static void callLog(PhoneNet.CallLog p) {
        calls = p.calls();
    }

    public static void feed(PhoneNet.Feed p) {
        feed = p.posts();
    }

    public static void news(PhoneNet.NewsList p) {
        news = p.news();
    }

    public static void positions(PhoneNet.Positions p) {
        spots = p.spots();
        spotsAt = System.currentTimeMillis();
    }

    public static void weather(PhoneNet.Weather p) {
        weather = p;
        weatherAt = System.currentTimeMillis();
    }

    public static void notice(PhoneNet.Notice n) {
        Minecraft mc = Minecraft.getInstance();
        String app;
        String title;
        String text;
        switch (n.kind()) {
            case "sms" -> {
                app = "messages";
                title = name(n.number(), n.title());
                text = n.text();
            }
            case "post" -> {
                app = "murmures";
                title = Component.translatable("phone.eraphones.new_post", n.title()).getString();
                text = n.text();
            }
            case "news" -> {
                app = "journal";
                title = Component.translatable("phone.eraphones.app.journal").getString();
                text = n.title();
            }
            case "missed" -> {
                app = "telephone";
                title = Component.translatable("phone.eraphones.missed_call").getString();
                text = name(n.number(), n.title());
            }
            default -> {
                app = "reglages";
                title = Component.translatable("phone.eraphones.name").getString();
                text = Component.translatable(n.text(), n.title()).getString();
            }
        }
        long now = System.currentTimeMillis();
        NOTES.add(0, new Note(app, title, text, now, now));
        while (NOTES.size() > 12) {
            NOTES.remove(NOTES.size() - 1);
        }
        boolean viewing = mc.screen instanceof SmartScreen ps && ps.current() != null && ps.current().id.equals(app)
                || "journal".equals(app) && mc.screen instanceof com.waylays.eraphones.client.retro.FlipScreen f && f.readingNews();
        if (!viewing) {
            UNSEEN.merge(app, 1, Integer::sum);
        }
        if ("info".equals(n.kind()) && PhoneSettings.get().sounds) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), 1.2f, .4f));
        }
    }

    /** Whether Simple Voice Chat is here: calls are then spoken, the phone put to the ear once the other end answers. */
    public static boolean voiceChat() {
        return net.neoforged.fml.ModList.get().isLoaded("voicechat");
    }

    public static void callState(PhoneNet.CallState p) {
        int before = callState;
        if (p.state() == 3 && before != 3 && voiceChat()) {
            // Answered: the phone goes to the ear, the voice does the talking (the call goes on, shown at the top)
            Screen open = Minecraft.getInstance().screen;
            if (phoneOpen()) {
                Minecraft.getInstance().setScreen(null);
            }
        }
        callState = p.state();
        callMine = p.mine();
        callNumber = p.number();
        callDisplay = p.display();
        callReason = p.reason();
        if (p.state() == 3) {
            callSince = p.since();
            if (before != 3) {
                SAID.clear();
            }
        } else if (p.state() == 0) {
            callEndedAt = System.currentTimeMillis();
            callSince = p.since();
        } else {
            SAID.clear();
        }
        ringBack = 0;
        if (p.state() == 2 && !phoneOpen()) {
            NOTES.removeIf(n -> "telephone".equals(n.app()) && System.currentTimeMillis() - n.shownAt() < 4000);
        }
    }

    public static void callLine(PhoneNet.CallLine p) {
        SAID.add(new Said(p.mine(), p.text(), System.currentTimeMillis()));
        while (SAID.size() > 40) {
            SAID.remove(0);
        }
        if (!p.mine()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6f, .15f));
        }
    }

    public static void haunt(PhoneNet.Haunt p) {
        hauntUntil = System.currentTimeMillis() + p.ticks() * 50L;
        hauntText = p.text();
    }

    public static boolean haunted() {
        return System.currentTimeMillis() < hauntUntil;
    }

    public static void dial(String n) {
        send(new PhoneNet.Dial(n));
    }

    public static void answer(boolean accept) {
        send(new PhoneNet.Answer(accept));
    }

    public static void hangUp() {
        send(new PhoneNet.HangUp());
    }

    public static void speak(String text) {
        if (!text.isBlank()) {
            send(new PhoneNet.Speak(text));
        }
    }

    /** Whether the call screen should show: a call going on, or one just over (for a moment). */
    public static boolean inCall() {
        return callState != 0 || System.currentTimeMillis() - callEndedAt < 2500;
    }

    public static List<Note> notes() {
        return NOTES;
    }

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
        if ("messages".equals(app)) {
            int n = 0;
            for (PhoneNet.Conv c : inbox) {
                n += c.unread();
            }
            return n;
        }
        return UNSEEN.getOrDefault(app, 0);
    }

    // ---------------------------------------------------------------- the clock, the sky

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

    public static int hour() {
        return (int) ((Math.floorMod(dayTime(), 24000) / 1000 + 6) % 24);
    }

    public static int minute() {
        return (int) (Math.floorMod(dayTime(), 24000) % 1000 * 60 / 1000);
    }

    public static long day() {
        return dayTime() / 24000;
    }

    public static String dateLine() {
        long day = day();
        Level level = Minecraft.getInstance().level;
        int moon = level == null ? 0 : level.getMoonPhase();
        return Component.translatable("phone.eraphones.weekday." + Math.floorMod(day, 7)).getString() + " · "
                + Component.translatable("phone.eraphones.day", day + 1).getString() + " · "
                + Component.translatable("phone.eraphones.moon." + moon).getString();
    }

    public static boolean night(long t) {
        long d = Math.floorMod(t, 24000);
        return d > 13000 && d < 23000;
    }

    /** The temperature (°C) of a biome's base temperature at a time of day. */
    public static int celsius(float base, long t) {
        double day = Math.sin(Math.floorMod(t, 24000) / 24000.0 * Math.PI * 2);
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
        String condition = Component.translatable("phone.eraphones.sky." + (thunder ? "thunder" : snow ? "snow" : rain ? "rain" : night ? "night" : "clear"))
                .getString();
        String range = Component.translatable("phone.eraphones.sky.range", celsius(base, 6000), celsius(base, 18000)).getString();
        List<Hour> hours = new ArrayList<>();
        PhoneNet.Weather w = weather;
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
            id = EraPhones.id("photo/" + Integer.toHexString(file.toString().hashCode()));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(small));
            THUMBS.put(file, id);
        } catch (Exception e) {
            EraPhones.LOGGER.debug("Could not read the photo {}", file, e);
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

    /** Takes a photo: the phone steps aside and the interface hides for a moment. The 2004 camera takes them small. */
    public static void photo() {
        Minecraft mc = Minecraft.getInstance();
        hidGui = mc.options.hideGui;
        vga = era == Era.FLIP;
        mc.setScreen(null);
        mc.options.hideGui = true;
        photoStep = 3;
    }

    /** A 2004 phone's photo: 160 x 120, its colours a little washed and grainy, shown blown up. */
    private static NativeImage vga(NativeImage full) {
        NativeImage small = new NativeImage(160, 120, false);
        full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), small);
        java.util.Random r = new java.util.Random(full.getWidth() * 31L + full.getHeight());
        for (int y = 0; y < 120; y++) {
            for (int x = 0; x < 160; x++) {
                int c = small.getPixelRGBA(x, y);
                int rr = c & 255, gg = c >> 8 & 255, bb = c >> 16 & 255;
                int n = r.nextInt(17) - 8;
                rr = Math.min(255, Math.max(0, (rr * 7 + 140) / 8 + n) & 0xF8);
                gg = Math.min(255, Math.max(0, (gg * 7 + 130) / 8 + n) & 0xFC);
                bb = Math.min(255, Math.max(0, (bb * 7 + 120) / 8 + n) & 0xF8);
                small.setPixelRGBA(x, y, 0xFF000000 | bb << 16 | gg << 8 | rr);
            }
        }
        NativeImage big = new NativeImage(640, 480, false);
        for (int y = 0; y < 480; y++) {
            for (int x = 0; x < 640; x++) {
                big.setPixelRGBA(x, y, small.getPixelRGBA(x / 4, y / 4));
            }
        }
        small.close();
        full.close();
        return big;
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
        NativeImage taken = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        NativeImage img = vga ? vga(taken) : taken;
        mc.options.hideGui = hidGui;
        String name = "photo_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
        Path dir = PhoneSettings.photos();
        Util.ioPool().execute(() -> {
            try {
                Files.createDirectories(dir);
                img.writeToFile(dir.resolve(name));
            } catch (Exception e) {
                EraPhones.LOGGER.warn("Could not save the photo", e);
            } finally {
                img.close();
            }
        });
        flashAt = System.currentTimeMillis();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.6f, .8f));
        if (slot >= 0) {
            open(slot, "photo");
        }
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(EraPhones.id("phone_hud"), PhoneHud::render);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                takeOut(mc.player);
            }
        }
        // The ring-back tone while the other end rings
        if (callState == 1 && mc.player != null) {
            if (ringBack++ % 60 == 0) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), .84f, .25f));
            }
        }
    }

    /** The phone key: the ringing phone if one rings, else the phone in hand or in the hotbar. */
    public static void takeOut(Player p) {
        if (callState != 0 && !callMine.isEmpty()) {
            int ringing = slotOf(p, callMine);
            if (ringing >= 0) {
                open(ringing);
                return;
            }
        }
        int at = handySlot(p);
        if (at < 0) {
            p.displayClientMessage(Component.translatable("message.eraphones.no_phone"), true);
            return;
        }
        open(at);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide && event.getItemStack().getItem() instanceof PhoneItem && event.getEntity() == Minecraft.getInstance().player) {
            Player p = event.getEntity();
            open(event.getHand() == InteractionHand.OFF_HAND ? 40 : p.getInventory().selected);
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        forgetLine();
        number = "";
        slot = -1;
        callState = 0;
        SAID.clear();
        NOTES.clear();
        UNSEEN.clear();
        weather = null;
        news = List.of();
        hauntUntil = 0;
    }

    /** Plays a UI sound if the phone's sounds are on. */
    public static void beep(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        if (PhoneSettings.get().sounds) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    public static void toastNow(GuiGraphics g, DeltaTracker delta) {
        PhoneHud.render(g, delta);
    }
}
