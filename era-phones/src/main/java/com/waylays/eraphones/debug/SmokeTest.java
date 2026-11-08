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
        // Two articles for the town's news (the smartphone's Journal, the flip phone's news)
        STEPS.add(() -> command(mc, "telephone journal Fête foraine : le cirque s'installe au parc | Les roulottes sont arrivées cette nuit. "
                + "Personne ne les a vues entrer en ville, mais le chapiteau rouge et blanc se dresse déjà près de l'ancienne usine."));
        STEPS.add(() -> command(mc, "telephone journal Un enfant porté disparu près des égouts | Un garçon de six ans n'est pas rentré hier soir. "
                + "Un voisin dit avoir vu un ballon rouge flotter au-dessus de la bouche d'égout de la rue des Saules. "
                + "La police demande à toute personne ayant des informations de se manifester. Les enfants sont priés de ne pas sortir après la tombée de la nuit."));
        STEPS.add(() -> {
            shot(mc, "rp_chat");
            if (!mc.player.getDisplayName().getString().equals("Bill Denbrough")) {
                fail("the character's name is not shown: {}", mc.player.getDisplayName().getString());
            } else {
                log("rp: the character's name is shown");
            }
        });
        // The game masters' tools of fear, on oneself: each shown, then stopped
        STEPS.add(() -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            command(mc, "voix masque");
            command(mc, "voix normale");
            command(mc, "horreur son @s rire");
            command(mc, "horreur murmure @s Tu flotteras aussi...");
        });
        STEPS.add(() -> {
        });
        STEPS.add(() -> shot(mc, "horror_whisper"));
        horror(mc, "horror_blood", "horreur sang @s 12", 4);
        horror(mc, "horror_blackout", "horreur coupure @s 8", 2);
        horror(mc, "horror_fog", "horreur brouillard @s 30", 4);
        horror(mc, "horror_balloon", "horreur ballon @s", 2);
        STEPS.add(() -> command(mc, "horreur stop @s"));
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
        keys(mc, "flip_camera_menu", "end", "ok", "down", "right");
        keys(mc, "flip_write", "end", "ok", "ok", "ok", "5", "5", "5", "0", "1", "4", "2", "ok", "4", "4", "3", "3", "5", "5", "5", "6", "6", "6");
        keys(mc, "flip_snake", "end", "ok", "down", "down", "ok");
        // The flip phone's news: the headlines, then the newest article
        keys(mc, "flip_news", "end", "ok", "4");
        keys(mc, "flip_article", "ok");
        keys(mc, "flip_settings", "end", "ok", "9");
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
        gear(mc);
    }

    /** The things: a lit torch in hand, the walkie-talkie's face, a radio and a video recorder set down and opened, an
     * identity card, the newspaper. */
    private static void gear(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        java.util.UUID id = mc.player.getUUID();
        STEPS.add(() -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                return;
            }
            ItemStack torch = new ItemStack(com.waylays.eraphones.registry.GearRegistry.FLASHLIGHT.get());
            com.waylays.eraphones.gear.Gear.setOn(torch, true);
            p.getInventory().setItem(5, torch);
            ItemStack walkie = new ItemStack(com.waylays.eraphones.registry.GearRegistry.WALKIE.get());
            com.waylays.eraphones.gear.Gear.setOn(walkie, true);
            p.getInventory().setItem(6, walkie);
            p.getInventory().setItem(7, new ItemStack(com.waylays.eraphones.registry.GearRegistry.ID_CARD.get()));
            ItemStack paper = new ItemStack(com.waylays.eraphones.registry.GearRegistry.NEWSPAPER.get());
            paper.set(com.waylays.eraphones.registry.GearRegistry.NEWS_TITLE.get(), "Le cirque s'installe au parc");
            paper.set(com.waylays.eraphones.registry.GearRegistry.NEWS_TEXT.get(), "Les roulottes sont arrivées cette nuit. Personne ne les a vues "
                    + "entrer en ville, mais le chapiteau rouge et blanc se dresse déjà près de l'ancienne usine. Les enfants sont priés de rentrer "
                    + "avant la tombée de la nuit.");
            p.getInventory().setItem(8, paper);
            // A table in front: the radio and the video recorder on it, facing the player
            net.minecraft.core.BlockPos base = p.blockPosition().relative(p.getDirection(), 2);
            net.minecraft.core.Direction face = p.getDirection().getOpposite();
            net.minecraft.core.BlockPos side = base.relative(p.getDirection().getClockWise());
            p.serverLevel().setBlockAndUpdate(base, com.waylays.eraphones.registry.GearRegistry.RADIO.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, face));
            p.serverLevel().setBlockAndUpdate(side, com.waylays.eraphones.registry.GearRegistry.VCR.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, face));
            if (p.serverLevel().getBlockEntity(base) instanceof com.waylays.eraphones.gear.block.RadioBlockEntity r) {
                r.set(true, 1003, 6);
            }
            p.serverLevel().setDayTime(18000);
            radioPos = base;
            vcrPos = side;
        }));
        STEPS.add(() -> {
            mc.setScreen(null);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            select(mc, 5);
        });
        STEPS.add(() -> {
        });
        STEPS.add(() -> shot(mc, "gear_torch_night"));
        STEPS.add(() -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        STEPS.add(() -> shot(mc, "gear_torch_front"));
        STEPS.add(() -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        STEPS.add(() -> shot(mc, "gear_torch_back"));
        STEPS.add(() -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            select(mc, 6);
        });
        STEPS.add(() -> shot(mc, "gear_walkie_held"));
        STEPS.add(() -> com.waylays.eraphones.gear.client.GearClient.openWalkie(net.minecraft.world.InteractionHand.MAIN_HAND));
        STEPS.add(() -> shot(mc, "gear_walkie"));
        STEPS.add(() -> {
            mc.setScreen(null);
            command(mc, "time set noon");
            mc.player.setXRot(35);
        });
        STEPS.add(() -> shot(mc, "gear_blocks"));
        STEPS.add(() -> {
            if (radioPos != null) {
                com.waylays.eraphones.gear.client.GearClient.openRadio(radioPos);
            }
        });
        STEPS.add(() -> shot(mc, "gear_radio"));
        STEPS.add(() -> {
            if (vcrPos != null) {
                com.waylays.eraphones.gear.client.GearClient.openVcr(vcrPos);
            }
        });
        STEPS.add(() -> shot(mc, "gear_vcr"));
        STEPS.add(() -> {
            mc.setScreen(null);
            select(mc, 7);
            com.waylays.eraphones.gear.client.GearClient.card(new com.waylays.eraphones.gear.GearNet.Card(mc.player.getUUID().toString(),
                    "Bill Denbrough", "412-63-1980", "Grand, mince, cheveux roux, bégaie un peu quand il a peur.", ""));
        });
        STEPS.add(() -> shot(mc, "gear_card"));
        STEPS.add(() -> {
            mc.setScreen(null);
            select(mc, 8);
            ItemStack paper = mc.player.getMainHandItem();
            com.waylays.eraphones.gear.client.GearClient.openNewspaper(
                    paper.getOrDefault(com.waylays.eraphones.registry.GearRegistry.NEWS_TITLE.get(), ""),
                    paper.getOrDefault(com.waylays.eraphones.registry.GearRegistry.NEWS_TEXT.get(), ""));
        });
        STEPS.add(() -> shot(mc, "gear_newspaper"));
        // A sheet of paper: written on, then signed and read
        STEPS.add(() -> {
            mc.setScreen(null);
            ItemStack blank = new ItemStack(com.waylays.eraphones.registry.GearRegistry.NOTE.get());
            mc.setScreen(new com.waylays.eraphones.gear.client.NoteEditScreen(net.minecraft.world.InteractionHand.MAIN_HAND, blank));
        });
        STEPS.add(() -> shot(mc, "gear_note_edit"));
        STEPS.add(() -> {
            mc.setScreen(null);
            mc.setScreen(new com.waylays.eraphones.gear.client.NoteReadScreen(new com.waylays.eraphones.gear.client.NotePage.Data("Pour Bill",
                    "Rendez-vous ce soir à la vieille maison au bout de la rue.\n\n"
                    + "Viens seul. Apporte la lampe torche et des piles neuves. Ne dis rien aux autres.\n\nOn flotte tous, en bas.",
                    0, 1, false, "Un ami")));
        });
        STEPS.add(() -> shot(mc, "gear_note_read"));
        // What the client has where the radio and the recorder were set down, and what it would draw there
        STEPS.add(() -> {
            for (net.minecraft.core.BlockPos at : new net.minecraft.core.BlockPos[] {radioPos, vcrPos}) {
                if (at == null) {
                    continue;
                }
                net.minecraft.world.level.block.state.BlockState st = mc.level.getBlockState(at);
                net.minecraft.client.resources.model.BakedModel model = mc.getBlockRenderer().getBlockModel(st);
                int quads = model.getQuads(st, null, net.minecraft.util.RandomSource.create()).size();
                log("block at {}: {} (below {}), light block {} sky {}, model {} with {} quads, shape {}", at.toShortString(), st,
                        mc.level.getBlockState(at.below()).getBlock(), mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at),
                        mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at), model.getClass().getSimpleName(), quads, st.getRenderShape());
            }
        });
        // A television on the recorder, a film made here and played into it; the doll and the music box beside them;
        // the game master's remote; a narration
        STEPS.add(() -> {
            try {
                java.nio.file.Files.createDirectories(com.waylays.eraphones.gear.Tape.folder());
                testFilm(com.waylays.eraphones.gear.Tape.folder().resolve("Film test.mp4").toFile());
            } catch (Exception e) {
                fail("cannot make a test film: {}", e.toString());
            }
        });
        STEPS.add(() -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || vcrPos == null || radioPos == null) {
                return;
            }
            net.minecraft.core.Direction face = p.getDirection().getOpposite();
            net.minecraft.core.Direction left = p.getDirection().getCounterClockWise();
            p.serverLevel().setBlockAndUpdate(vcrPos.above(), com.waylays.eraphones.registry.GearRegistry.TV.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, face)
                    .setValue(com.waylays.eraphones.gear.block.TvBlock.LIT, true));
            p.serverLevel().setBlockAndUpdate(radioPos.relative(left), com.waylays.eraphones.registry.GearRegistry.DOLL.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, face));
            p.serverLevel().setBlockAndUpdate(radioPos.relative(left, 2), com.waylays.eraphones.registry.GearRegistry.MUSIC_BOX_BLOCK.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, face));
        }));
        STEPS.add(() -> {
            mc.setScreen(null);
            if (vcrPos != null) {
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(vcrPos.above()));
            }
        });
        STEPS.add(() -> {
        });
        STEPS.add(() -> shot(mc, "tv_blue"));
        STEPS.add(() -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || vcrPos == null || !(p.serverLevel().getBlockEntity(vcrPos) instanceof com.waylays.eraphones.gear.block.VcrBlockEntity vcr)) {
                fail("no video recorder to play the film in");
                return;
            }
            ItemStack film = new ItemStack(com.waylays.eraphones.registry.GearRegistry.CASSETTE.get());
            film.set(com.waylays.eraphones.registry.GearRegistry.TAPE.get(), "Film test.mp4");
            film.set(com.waylays.eraphones.registry.GearRegistry.TAPE_LABEL.get(), "Film test");
            ItemStack keep = p.getOffhandItem().copy();
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, film);
            vcr.press(p, "insert");
            vcr.press(p, "play");
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, keep);
        }));
        for (int k = 0; k < 4; k++) {
            STEPS.add(() -> {
            });
        }
        STEPS.add(() -> shot(mc, "tv_film"));
        STEPS.add(() -> {
            if (radioPos != null) {
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(radioPos));
                mc.player.setXRot(28);
            }
        });
        STEPS.add(() -> shot(mc, "horror_things"));
        STEPS.add(() -> mc.setScreen(new com.waylays.eraphones.horror.client.RemoteScreen(
                java.util.List.of(new java.util.UUID(1, 2).toString(), new java.util.UUID(3, 4).toString()), java.util.List.of("Bill (4 m)", "Beverly (12 m)"))));
        STEPS.add(() -> shot(mc, "horror_remote"));
        STEPS.add(() -> {
            mc.setScreen(null);
            com.waylays.eraphones.horror.client.HorrorClient.effect(new com.waylays.eraphones.horror.HorrorNet.Effect("narration", 400,
                    "La pluie tombait sur la ville depuis trois jours, et personne ne voyait les ballons qui remontaient le courant.", 0, 0, 0));
        });
        STEPS.add(() -> {
        });
        STEPS.add(() -> {
        });
        STEPS.add(() -> shot(mc, "narration"));
        STEPS.add(() -> com.waylays.eraphones.horror.client.HorrorClient.effect(new com.waylays.eraphones.horror.HorrorNet.Effect("stop", 0, "", 0, 0, 0)));
        // Every thing in hand, one after the other; a few as others see them; the inventory's icons (the hotbar is
        // put back after, the phones in it are looked at again at the end)
        STEPS.add(() -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                for (int k = 0; k < 9; k++) {
                    HOTBAR[k] = p.getInventory().getItem(k).copy();
                }
            }
        }));
        String[] held = {"piece_25c", "billet_20", "carte_identite", "journal_papier", "feuille", "cassette", "piles", "talkie_walkie", "radio",
                "magnetoscope", "lampe_torche", "television", "ballon_rouge", "telecommande_mj", "poupee", "boite_a_musique"};
        for (String name : held) {
            STEPS.add(() -> server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.getInventory().setItem(0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.waylays.eraphones.EraPhones.id(name))));
                }
            }));
            STEPS.add(() -> {
                mc.setScreen(null);
                select(mc, 0);
                mc.player.setXRot(10);
            });
            STEPS.add(() -> shot(mc, "held_" + name));
        }
        for (String name : new String[] {"billet_20", "talkie_walkie", "journal_papier", "lampe_torche"}) {
            STEPS.add(() -> server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.getInventory().setItem(0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.waylays.eraphones.EraPhones.id(name))));
                }
            }));
            STEPS.add(() -> {
                select(mc, 0);
                mc.player.setXRot(0);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            });
            STEPS.add(() -> shot(mc, "held3_" + name));
            STEPS.add(() -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        }
        STEPS.add(() -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                return;
            }
            String[] icons = {"piece_10c", "piece_25c", "billet_1", "billet_20", "billet_100", "carte_identite", "journal_papier", "feuille", "cassette"};
            for (int k = 0; k < icons.length; k++) {
                p.getInventory().setItem(k, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.waylays.eraphones.EraPhones.id(icons[k]))));
            }
        }));
        STEPS.add(() -> select(mc, 8));
        STEPS.add(() -> shot(mc, "gear_icons"));
        // A blank cassette's recording screen, a file waiting in the folder
        STEPS.add(() -> {
            try {
                java.nio.file.Files.createDirectories(com.waylays.eraphones.gear.Tape.folder());
                java.nio.file.Files.write(com.waylays.eraphones.gear.Tape.folder().resolve("Chanson du cirque.mp3"), new byte[180000]);
            } catch (java.io.IOException e) {
                fail("cannot write a test recording: {}", e.toString());
            }
            mc.setScreen(new com.waylays.eraphones.gear.client.BurnScreen(false));
        });
        STEPS.add(() -> shot(mc, "gear_burn"));
        STEPS.add(() -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                for (int k = 0; k < 9; k++) {
                    p.getInventory().setItem(k, HOTBAR[k] == null ? ItemStack.EMPTY : HOTBAR[k]);
                }
            }
        }));
        STEPS.add(() -> {
            mc.setScreen(null);
            log("gear: torch, walkie-talkie, radio, video recorder, card, newspaper, things in hand and the cassette screen shown");
        });
    }

    /** A three-second film of colour bars with a black square crossing them, made with the same library that reads it. */
    private static void testFilm(java.io.File out) throws java.io.IOException {
        org.jcodec.api.SequenceEncoder enc = org.jcodec.api.SequenceEncoder.createSequenceEncoder(out, 10);
        int[][] cols = {{255, 255, 255}, {255, 255, 0}, {0, 255, 255}, {0, 255, 0}, {255, 0, 255}, {255, 0, 0}, {0, 0, 255}};
        for (int f = 0; f < 30; f++) {
            org.jcodec.common.model.Picture pic = org.jcodec.common.model.Picture.create(160, 120, org.jcodec.common.model.ColorSpace.RGB);
            byte[] d = pic.getPlaneData(0);
            for (int y = 0; y < 120; y++) {
                for (int x = 0; x < 160; x++) {
                    int i = (y * 160 + x) * 3;
                    int[] c = cols[x * 7 / 160];
                    boolean square = Math.abs(x - (20 + f * 4)) < 14 && Math.abs(y - 60) < 14;
                    for (int k = 0; k < 3; k++) {
                        d[i + k] = (byte) ((square ? 16 : c[k]) - 128);
                    }
                }
            }
            enc.encodeNativeFrame(pic);
        }
        enc.finish();
    }

    private static net.minecraft.core.BlockPos radioPos, vcrPos;
    private static final ItemStack[] HOTBAR = new ItemStack[9];

    private static void open(Minecraft mc, int slot) {
        mc.setScreen(null);
        select(mc, slot);
        PhoneClient.open(slot);
        if (!(mc.screen instanceof SmartScreen) && !(mc.screen instanceof RetroScreen)) {
            fail("the phone in slot {} did not open (screen {})", slot, mc.screen);
        }
    }

    /** Presses the keys of the retro phone open, then photographs it. */
    /** Ends the effects before, sends {@code cmd}, waits {@code steps}, takes the shot. */
    private static void horror(Minecraft mc, String shot, String cmd, int steps) {
        STEPS.add(() -> {
            command(mc, "horreur stop @s");
            command(mc, cmd);
        });
        for (int i = 1; i < steps; i++) {
            STEPS.add(() -> {
            });
        }
        STEPS.add(() -> shot(mc, shot));
    }

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
