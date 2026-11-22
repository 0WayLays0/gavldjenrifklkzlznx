package com.waylays.cinematic.debug;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.waylays.cinematic.CinematicMod;
import com.waylays.cinematic.client.CameraTrack;
import com.waylays.cinematic.client.ClientStudio;
import com.waylays.cinematic.client.DirectorScreen;
import com.waylays.cinematic.client.EmoteScreen;
import com.waylays.cinematic.client.PoseEditorScreen;
import com.waylays.cinematic.client.SkinCache;
import com.waylays.cinematic.client.StudioCamera;
import com.waylays.cinematic.client.StudioScreen;
import com.waylays.cinematic.net.ActionMsg.Action;
import com.waylays.cinematic.net.SyncMsg;
import com.waylays.cinematic.studio.ActorEntity;
import com.waylays.cinematic.studio.BodyPart;
import com.waylays.cinematic.studio.Emote;
import com.waylays.cinematic.studio.PoseKey;
import com.waylays.cinematic.studio.Track;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automated client test (only with -Dcinematic.autotest=true, as CI runs it): a creative world, a scene, an actor
 * given a 512x512 skin, a take recorded by moving the player, the scene played back, the studio's screens opened,
 * camera shots previewed. Screens are photographed into the log ({@code [SmokeShot] name part/parts base64-jpeg}).
 */
@EventBusSubscriber(modid = CinematicMod.MOD_ID, value = Dist.CLIENT)
public final class SmokeTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("cinematic.autotest");
    private static final int SHOT_WIDTH = 640, SHOT_CHUNK = 8000;
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private static final List<Step> STEPS = new ArrayList<>();
    private static int stage = -1, ticks, step, wait;
    private static Runnable everyTick;
    private static Vec3 origin;

    private record Step(int delay, Runnable action, java.util.function.BooleanSupplier until) {}

    private SmokeTest() {}

    private static void log(String msg, Object... args) {
        LOG.info("[SmokeTest] " + msg, args);
    }

    private static void fail(String msg, Object... args) {
        FAILURES.incrementAndGet();
        LOG.error("[SmokeTest] FAIL " + msg, args);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ENABLED || stage != -1 || !(event.getNewScreen() instanceof TitleScreen)) return;
        stage = 0;
        log("title screen reached");
        Minecraft mc = Minecraft.getInstance();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(1200_000L);
            } catch (InterruptedException e) {
                return;
            }
            if (stage != 3) {
                LOG.error("[SmokeTest] FAIL watchdog: stuck at stage {} step {}", stage, step);
                Runtime.getRuntime().halt(3);
            }
        }, "Cinematic smoke watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        mc.tell(() -> {
            try {
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
                LevelSettings settings = new LevelSettings("cinematic_smoketest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("cinematic_smoketest_" + System.currentTimeMillis(), settings,
                        new WorldOptions(20261122L, false, false), WorldPresets::createNormalWorldDimensions);
            } catch (Throwable t) {
                fail("creating the world: {}", t.toString(), t);
                finish(mc);
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ENABLED || stage < 0 || stage == 3) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        try {
            if (everyTick != null) everyTick.run();
            if (stage == 0) {
                if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
                    log("joined the world");
                    plan(mc);
                    stage = 1;
                    ticks = 0;
                }
            } else if (stage == 1 && ticks > 100) {
                stage = 2;
                ticks = 0;
                step = 0;
                wait = 0;
            } else if (stage == 2) {
                if (--wait > 0) return;
                if (step < STEPS.size()) {
                    Step s = STEPS.get(step);
                    if (s.until() != null) {
                        // Waits (up to the step's delay) for a condition
                        if (!s.until().getAsBoolean() && ticks < s.delay()) return;
                        if (!s.until().getAsBoolean()) fail("timed out at step {}", step);
                        ticks = 0;
                        step++;
                        s.action().run();
                        wait = 1;
                        return;
                    }
                    step++;
                    ticks = 0;
                    s.action().run();
                    wait = s.delay();
                } else {
                    finish(mc);
                }
            }
        } catch (Throwable t) {
            fail("at step {}: {}", step, t.toString(), t);
            finish(mc);
        }
    }

    private static void then(int delay, Runnable r) {
        STEPS.add(new Step(delay, r, null));
    }

    /** Runs {@code r} once {@code cond} holds, failing after {@code timeout} ticks. */
    private static void when(java.util.function.BooleanSupplier cond, int timeout, Runnable r) {
        STEPS.add(new Step(timeout, r, cond));
    }

    private static void onServer(Minecraft mc, java.util.function.Consumer<ServerPlayer> r) {
        IntegratedServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) r.accept(p);
        });
    }

    private static SyncMsg.ActorInfo actorNamed(String name) {
        SyncMsg.SceneInfo s = ClientStudio.scene("Essai");
        if (s == null) return null;
        for (SyncMsg.ActorInfo a : s.actors()) if (a.name().equals(name)) return a;
        return null;
    }

    private static UUID bob() {
        SyncMsg.ActorInfo a = actorNamed("Bob");
        return a == null ? null : a.id();
    }

    private static void plan(Minecraft mc) {
        // A clear stage: a stone floor in front of the player, noon
        then(20, () -> onServer(mc, p -> {
            p.serverLevel().setDayTime(6000);
            p.serverLevel().setWeatherParameters(6000, 0, false, false);
            BlockPos base = new BlockPos(p.blockPosition().getX(), 140, p.blockPosition().getZ());
            for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) {
                boolean edge = Math.abs(dx) == 12 || Math.abs(dz) == 12;
                p.serverLevel().setBlockAndUpdate(base.offset(dx, 0, dz), (dx + dz) % 2 == 0 ? Blocks.SMOOTH_STONE.defaultBlockState()
                        : Blocks.POLISHED_ANDESITE.defaultBlockState());
                if (edge) p.serverLevel().setBlockAndUpdate(base.offset(dx, 1, dz), Blocks.OAK_FENCE.defaultBlockState());
            }
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
            p.setYRot(0);
            p.setXRot(0);
            p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
            p.getInventory().setItem(1, new ItemStack(Items.BOW));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        }));
        then(10, () -> {
            makeSkins(mc);
            ClientStudio.selectedScene = "Essai";
            ClientStudio.send(Action.CREATE_SCENE, null, "Essai", 0);
        });
        then(10, () -> {
            if (ClientStudio.scene("Essai") == null) fail("the scene was not created");
            else log("studio: scene created");
            if (!ClientStudio.canDirect()) fail("the world's owner cannot direct");
            ClientStudio.send(Action.CREATE_ACTOR, null, "Bob", 0);
        });
        then(10, () -> {
            UUID bob = bob();
            if (bob == null) {
                fail("the actor was not created");
                return;
            }
            log("studio: actor created");
            String problem = SkinCache.upload(bob, mc.gameDirectory.toPath().resolve("cinematic/skins/smoke_hd_512.png"));
            if (problem != null) fail("skin upload refused: {}", problem);
            ClientStudio.send(Action.SET_SLIM, bob, "", 1);
        });
        then(20, () -> {
            SyncMsg.ActorInfo a = actorNamed("Bob");
            if (a == null || a.skin().isEmpty()) fail("the skin was not applied (actor {})", a);
            else log("studio: 512x512 skin applied");
            if (a != null && !a.slim()) fail("slim arms not applied");
            mc.setScreen(new StudioScreen());
        });
        then(10, () -> shot(mc, "studio_screen"));
        then(5, () -> mc.setScreen(new com.waylays.cinematic.client.ActorScreen(bob())));
        then(15, () -> shot(mc, "actor_screen"));
        then(5, () -> {
            mc.setScreen(null);
            // Record: 3 s countdown, then a walk with a turn, a swing, a gesture
            ClientStudio.send(Action.RECORD, bob(), "", 0);
        });
        then(30, () -> shot(mc, "countdown"));
        when(() -> ClientStudio.recordedTicks() > 0, 600, () -> {
            log("recording started");
            origin = mc.player.position();
            int[] t = {0};
            everyTick = () -> {
                t[0]++;
                if (mc.player == null) return;
                double ang = t[0] * 0.03;
                mc.player.setPos(origin.x + Math.sin(ang) * 4, origin.y, origin.z + (1 - Math.cos(ang)) * 4);
                mc.player.setYRot((float) Math.toDegrees(-ang) - 90f);
                mc.player.setYHeadRot(mc.player.getYRot());
                if (t[0] % 25 == 0) mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                if (t[0] == 40) ClientStudio.emote = Emote.WAVE.ordinal();
                if (t[0] == 80) ClientStudio.emote = Emote.NONE.ordinal();
            };
        });
        then(30, () -> shot(mc, "recording"));
        when(() -> ClientStudio.recordedTicks() >= 140, 600, () -> {
            everyTick = null;
            ClientStudio.stopOnServer();
        });
        when(() -> ClientStudio.trackOf(bob()) != null && ClientStudio.trackOf(bob()).length() >= 100, 600, () -> {
            Track t = ClientStudio.trackOf(bob());
            if (t == null || t.length() < 100) fail("the take did not come back from the server ({} frames)", t == null ? -1 : t.length());
            else log("studio: take of {} frames recorded and synced", t.length());
            SyncMsg.ActorInfo a = actorNamed("Bob");
            if (a == null || a.length() < 100) fail("the server did not keep the take");
            // A second actor, duplicate of the first, moved aside
            ClientStudio.send(Action.DUPLICATE_ACTOR, bob(), "", 0);
            onServer(mc, p -> {
                p.teleportTo(origin.x - 7, origin.y, origin.z + 4);
                p.setYRot(-90f);
                p.setXRot(8f);
            });
        });
        then(20, () -> {
            mc.player.setYRot(-90f);
            mc.player.setXRot(8f);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            ClientStudio.cursor = 60;
            ClientStudio.previewing = false;
        });
        then(20, () -> {
            ActorEntity e = ClientStudio.findEntity(bob());
            if (e == null) fail("the actor's entity is not on the client");
            else if (!e.driven()) fail("the actor does not follow its take");
            else log("playback: the actor follows its take at {}", e.position());
            shot(mc, "actor_at_3s");
        });
        then(10, () -> {
            ClientStudio.cursor = 95;
        });
        then(20, () -> shot(mc, "actor_waving"));
        // Pose editor: right arm up at 2 s, then saved
        then(10, () -> mc.setScreen(new PoseEditorScreen(bob())));
        then(10, () -> {
            ClientStudio.cursor = 40;
            ClientStudio.editKeys().add(new PoseKey(40, BodyPart.RIGHT_ARM, -150f, 0f, 20f));
            ClientStudio.editKeys().add(new PoseKey(40, BodyPart.HEAD, 0f, 0f, 25f));
        });
        then(15, () -> shot(mc, "pose_editor"));
        then(10, () -> {
            ClientStudio.saveEdit();
            mc.setScreen(null);
        });
        then(15, () -> {
            Track t = ClientStudio.trackOf(bob());
            if (t == null || t.poseKeys.size() != 2) fail("pose keys not saved ({})", t == null ? -1 : t.poseKeys.size());
            else log("poses: 2 keys saved");
            mc.setScreen(new EmoteScreen());
        });
        then(10, () -> shot(mc, "emotes"));
        // Camera: a path shot then an orbit around Bob, previewed through the film camera
        then(5, () -> {
            mc.setScreen(null);
            CameraTrack t = CameraTrack.forScene("Essai");
            t.shots.clear();
            CameraTrack.Shot path = new CameraTrack.Shot();
            path.start = 0;
            path.keys.add(CameraTrack.key(0, origin.add(-6, 3, -4), -50f, 20f, 0f, 70f));
            path.keys.add(CameraTrack.key(60, origin.add(0, 1.6, -6), 0f, 5f, 0f, 60f));
            path.keys.add(CameraTrack.key(100, origin.add(6, 2.5, 0), 70f, 10f, -4f, 50f));
            CameraTrack.Shot orbit = new CameraTrack.Shot();
            orbit.start = 110;
            orbit.type = CameraTrack.Type.ORBIT;
            orbit.target = bob().toString();
            orbit.keys.add(CameraTrack.key(110, origin.add(3, 2, 3), 0f, 0f, 0f, 55f));
            t.shots.add(path);
            t.shots.add(orbit);
            t.selected = 0;
            t.save();
            ClientStudio.cursor = 0;
            ClientStudio.previewing = true;
        });
        then(40, () -> {
            if (!StudioCamera.filmActive()) fail("the film camera is not on during the preview");
            shot(mc, "film_path");
        });
        then(90, () -> shot(mc, "film_orbit"));
        then(10, () -> {
            ClientStudio.previewing = false;
            mc.setScreen(new DirectorScreen());
        });
        then(10, () -> shot(mc, "director"));
        then(5, () -> {
            mc.setScreen(null);
            StudioCamera.toggleFree(mc);
        });
        then(20, () -> {
            if (!StudioCamera.free()) fail("free camera did not start");
            shot(mc, "freecam");
            StudioCamera.toggleFree(mc);
        });
        // Played by the server, then stopped
        then(10, ClientStudio::playOnServer);
        then(40, () -> {
            if (!ClientStudio.serverPlaying()) fail("the server is not playing the scene");
            else log("playback: the server plays the scene");
            shot(mc, "server_playback");
        });
        then(10, ClientStudio::stopOnServer);
        then(10, () -> {
            if (ClientStudio.serverPlaying()) fail("the scene did not stop");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
    }

    /** A 512x512 test skin (8x the 64x64 layout): red head with a face, blue body, green arms, yellow legs. */
    private static void makeSkins(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("cinematic/skins");
        try {
            Files.createDirectories(dir);
            int s = 8;
            try (NativeImage img = new NativeImage(64 * s, 64 * s, true)) {
                fill(img, 0, 0, 32, 16, s, 0xFF3030D0);
                fill(img, 16, 16, 40, 32, s, 0xFFD06020);
                fill(img, 40, 16, 56, 32, s, 0xFF30B040);
                fill(img, 32, 48, 48, 64, s, 0xFF30B040);
                fill(img, 0, 16, 16, 32, s, 0xFF20C0E0);
                fill(img, 16, 48, 32, 64, s, 0xFF20C0E0);
                // Face: white eyes with dark pupils, a mouth, drawn at the HD resolution
                fill(img, 9, 11, 11, 12, s, 0xFFFFFFFF);
                fill(img, 13, 11, 15, 12, s, 0xFFFFFFFF);
                for (int y = 0; y < s; y++) for (int x = 0; x < s / 2; x++) {
                    img.setPixelRGBA(10 * s + x, 11 * s + y, 0xFF202020);
                    img.setPixelRGBA(13 * s + x, 11 * s + y, 0xFF202020);
                }
                fill(img, 10, 14, 14, 15, s, 0xFF101060);
                img.writeToFile(dir.resolve("smoke_hd_512.png"));
            }
        } catch (Exception e) {
            fail("making the test skin: {}", e.toString(), e);
        }
    }

    private static void fill(NativeImage img, int x0, int y0, int x1, int y1, int s, int abgr) {
        for (int y = y0 * s; y < y1 * s; y++) for (int x = x0 * s; x < x1 * s; x++) img.setPixelRGBA(x, y, abgr);
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
                LOG.info("[SmokeShot] {} {}/{} {}", name, i + 1, parts, data.substring(i * SHOT_CHUNK, Math.min(data.length(), (i + 1) * SHOT_CHUNK)));
            }
        } catch (Throwable t) {
            LOG.warn("[SmokeTest] could not photograph {}", name, t);
        }
    }

    private static void finish(Minecraft mc) {
        stage = 3;
        if (FAILURES.get() == 0) log("PASS: studio, actor, skin, take, playback, poses and cameras went through without a crash");
        else LOG.error("[SmokeTest] FAIL {} problem(s), see above", FAILURES.get());
        mc.stop();
    }
}
