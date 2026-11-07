package com.waylays.eraphones.debug;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.eraphones.Era;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.retro.RetroScreen;
import com.waylays.eraphones.client.smart.SmartScreen;
import com.waylays.eraphones.item.PhoneItem;
import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The automated client test (only with -Deraphones.autotest=true, as CI runs it): a creative world, the four phones
 * and the charger put in the hotbar, each phone held (seen in first person and from the front) and taken out, its
 * screens gone through by its own keys, and photographed into the log as base64 JPEG lines
 * ({@code [SmokeShot] name part/parts data}). Fails on a crash, an exception, or a phone that does not open.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class SmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("eraphones.autotest");
    private static final int SHOT_WIDTH = 640, SHOT_CHUNK = 8000;
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private static int stage = -1;
    private static int ticks;
    private static int step;
    private static final List<Runnable> STEPS = new ArrayList<>();

    private SmokeTest() {}

    private static void log(String msg, Object... args) {
        EraPhones.LOGGER.info("[SmokeTest] " + msg, args);
    }

    private static void fail(String msg, Object... args) {
        FAILURES.incrementAndGet();
        EraPhones.LOGGER.error("[SmokeTest] FAIL " + msg, args);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ENABLED || stage != -1 || !(event.getNewScreen() instanceof TitleScreen)) {
            return;
        }
        stage = 0;
        log("title screen reached");
        Minecraft mc = Minecraft.getInstance();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(1500_000L);
            } catch (InterruptedException e) {
                return;
            }
            if (stage != 3) {
                EraPhones.LOGGER.error("[SmokeTest] FAIL watchdog: stuck at stage {} step {}", stage, step);
                Runtime.getRuntime().halt(3);
            }
        }, "Era phones smoke watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        mc.tell(() -> {
            try {
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
                LevelSettings settings = new LevelSettings("eraphones_smoketest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("eraphones_smoketest_" + System.currentTimeMillis(), settings,
                        new WorldOptions(20261107L, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
            } catch (Throwable t) {
                fail("creating the world: {}", t.toString(), t);
                finish(mc);
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || stage < 0 || stage == 3) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        try {
            if (stage == 0) {
                if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
                    log("joined the world");
                    give(mc);
                    plan(mc);
                    stage = 1;
                    ticks = 0;
                }
            } else if (stage == 1 && ticks > 80) {
                stage = 2;
                ticks = 0;
                step = 0;
            } else if (stage == 2 && ticks % 25 == 0) {
                if (step < STEPS.size()) {
                    STEPS.get(step++).run();
                } else {
                    finish(mc);
                }
            }
        } catch (Throwable t) {
            fail("at step {}: {}", step, t.toString(), t);
            finish(mc);
        }
    }

    /** The four phones in the hotbar's first slots, the charger after them, the time at noon, clear weather. */
    private static void give(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                return;
            }
            int slot = 0;
            for (Era era : new Era[] {Era.SMART, Era.FLIP, Era.GSM, Era.BRICK}) {
                ItemStack st = new ItemStack(EraRegistry.PHONES.get(era).get());
                PhoneItem.ensure(st, p.serverLevel());
                p.getInventory().setItem(slot++, st);
            }
            p.getInventory().setItem(4, new ItemStack(EraRegistry.CHARGER.get()));
            p.serverLevel().setDayTime(5000);
            p.serverLevel().setWeatherParameters(6000, 0, false, false);
            p.setYRot(30);
            p.setXRot(10);
        });
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().selected = slot;
    }

    /** Runs a command as the player, on the server. */
    private static void command(Minecraft mc, String cmd) {
        IntegratedServer server = mc.getSingleplayerServer();
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), cmd);
            }
        });
    }

    private static void plan(Minecraft mc) {
        // The role play: a character's name, an action, a roll of the dice (in the chat)
        STEPS.add(() -> {
            select(mc, 5);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            command(mc, "setname Bill Denbrough");
        });
        STEPS.add(() -> command(mc, "me ouvre lentement la porte de la cave"));
        STEPS.add(() -> command(mc, "roll 1d20"));
        STEPS.add(() -> {
            shot(mc, "rp_chat");
            if (!mc.player.getDisplayName().getString().equals("Bill Denbrough")) {
                fail("the character's name is not shown: {}", mc.player.getDisplayName().getString());
            } else {
                log("rp: the character's name is shown");
            }
        });
        String[] names = {"smartphone", "clapet", "gsm", "radiotelephone"};
        // Held: first person, then from the front
        for (int k = 0; k < 4; k++) {
            int slot = k;
            STEPS.add(() -> {
                mc.setScreen(null);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                select(mc, slot);
            });
            STEPS.add(() -> shot(mc, "held_" + names[slot]));
            STEPS.add(() -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            STEPS.add(() -> shot(mc, "front_" + names[slot]));
        }
        STEPS.add(() -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            select(mc, 0);
        });
        // The smartphone: locked, home, a few apps
        STEPS.add(() -> open(mc, 0));
        STEPS.add(() -> shot(mc, "smart_lock"));
        STEPS.add(() -> {
            if (mc.screen instanceof SmartScreen s) {
                s.unlockNow();
            }
        });
        STEPS.add(() -> shot(mc, "smart_home"));
        for (String app : new String[] {"telephone", "messages", "contacts", "murmures", "journal", "serpent", "reglages"}) {
            STEPS.add(() -> {
                if (mc.screen instanceof SmartScreen s) {
                    s.open(app, 90, 200);
                } else {
                    fail("the smartphone closed before {}", app);
                }
            });
            STEPS.add(() -> shot(mc, "smart_" + app));
        }
        // The flip phone: standby, menu, directory, messages, a game
        STEPS.add(() -> open(mc, 1));
        STEPS.add(() -> shot(mc, "flip_idle"));
        keys(mc, "flip_menu", "ok");
        keys(mc, "flip_contacts", "right", "ok");
        keys(mc, "flip_camera_menu", "end", "ok", "down");
        keys(mc, "flip_write", "end", "ok", "ok", "ok", "5", "5", "5", "0", "1", "4", "2", "ok", "4", "4", "3", "3", "5", "5", "5", "6", "6", "6");
        keys(mc, "flip_snake", "end", "ok", "down", "right", "right", "ok");
        // The GSM: standby, the menu's first entries, typing a number
        STEPS.add(() -> open(mc, 2));
        STEPS.add(() -> shot(mc, "gsm_idle"));
        keys(mc, "gsm_menu", "ok");
        keys(mc, "gsm_menu2", "down");
        keys(mc, "gsm_dial", "end", "5", "5", "5", "0", "1", "9", "9");
        keys(mc, "gsm_snake", "end", "ok", "down", "down", "down", "ok");
        // The brick: a number typed, a memory stored
        STEPS.add(() -> open(mc, 3));
        STEPS.add(() -> shot(mc, "brick_idle"));
        keys(mc, "brick_typed", "5", "5", "5", "0", "1", "4", "2");
        keys(mc, "brick_stored", "sto", "3");
        STEPS.add(() -> {
            mc.setScreen(null);
            log("phones: four opened and gone through");
        });
    }

    private static void open(Minecraft mc, int slot) {
        mc.setScreen(null);
        select(mc, slot);
        PhoneClient.open(slot);
        if (!(mc.screen instanceof SmartScreen) && !(mc.screen instanceof RetroScreen)) {
            fail("the phone in slot {} did not open (screen {})", slot, mc.screen);
        }
    }

    /** Presses the keys of the retro phone open, then photographs it. */
    private static void keys(Minecraft mc, String shot, String... ids) {
        STEPS.add(() -> {
            if (!(mc.screen instanceof RetroScreen r)) {
                fail("no retro phone open for {}", shot);
                return;
            }
            try {
                Method click = RetroScreen.class.getDeclaredMethod("click", String.class);
                click.setAccessible(true);
                for (String id : ids) {
                    click.invoke(r, id);
                }
            } catch (ReflectiveOperationException e) {
                fail("pressing keys for {}: {}", shot, e.toString(), e);
            }
        });
        STEPS.add(() -> shot(mc, shot));
    }

    private static void shot(Minecraft mc, String name) {
        try (NativeImage shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int f = Math.max(1, shot.getWidth() / SHOT_WIDTH);
            int w = shot.getWidth() / f, h = shot.getHeight() / f;
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = 0, g = 0, b = 0;
                    for (int dy = 0; dy < f; dy++) {
                        for (int dx = 0; dx < f; dx++) {
                            int p = shot.getPixelRGBA(x * f + dx, y * f + dy);
                            r += p & 0xFF;
                            g += p >> 8 & 0xFF;
                            b += p >> 16 & 0xFF;
                        }
                    }
                    int n = f * f;
                    out.setRGB(x, y, r / n << 16 | g / n << 8 | b / n);
                }
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "jpg", bytes);
            String data = Base64.getEncoder().encodeToString(bytes.toByteArray());
            int parts = (data.length() + SHOT_CHUNK - 1) / SHOT_CHUNK;
            for (int i = 0; i < parts; i++) {
                EraPhones.LOGGER.info("[SmokeShot] {} {}/{} {}", name, i + 1, parts, data.substring(i * SHOT_CHUNK, Math.min(data.length(), (i + 1) * SHOT_CHUNK)));
            }
        } catch (Throwable t) {
            EraPhones.LOGGER.warn("[SmokeTest] could not photograph {}", name, t);
        }
    }

    private static void finish(Minecraft mc) {
        stage = 3;
        if (mc.player != null) {
            for (int i = 0; i < 4; i++) {
                ItemStack st = mc.player.getInventory().getItem(i);
                if (!(st.getItem() instanceof PhoneItem) || PhoneItem.number(st) == null) {
                    fail("the phone in slot {} has no number on the client", i);
                }
            }
        }
        if (FAILURES.get() == 0) {
            log("PASS: the four phones were held, opened and gone through without a crash");
        } else {
            EraPhones.LOGGER.error("[SmokeTest] FAIL {} problem(s), see above", FAILURES.get());
        }
        mc.stop();
    }
}
