package com.waylays.arcanes.debug;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.CreativeTabFilter;
import com.waylays.arcanes.registry.ModCreativeTabs;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SummoningSpell;
import com.waylays.arcanes.spells.boss.BossAnimations;
import com.waylays.arcanes.spells.boss.BossSpell;
import com.waylays.arcanes.spells.physical.LongswordParrySkill;
import com.waylays.arcanes.spells.physical.PhysicalAnimations;
import com.waylays.arcanes.spells.physical.PhysicalSkill;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

/**
 * Automated client test, only with {@code -Darcanes.autotest=true} (CI job {@code client-smoke}, headless under xvfb).
 * Creates a creative world, holds the school's spellbook, then casts every addon spell (level 5) at a target dummy in
 * front of the camera, one every few seconds, so every spell entity, particle and renderer runs on a real client.
 * Summoning spells are then cast again, which must send their summons away. Fighting moves are done sword in hand
 * close to the dummy, with their body animation played on the player (the parry is put to the test by the dummy, so
 * the riposte plays too); every animation they use must have loaded.
 * The camera is in third person so the caster, its spellbook and the shells around it are drawn too.
 * A client crash ends the run with a crash report; a spell that throws is logged as {@code [SmokeTest] FAIL}.
 * The run ends with {@code [SmokeTest] PASS} or {@code [SmokeTest] FAIL ...} and stops the client.
 * A watchdog halts the JVM (exit code 3) if the whole run takes too long.
 * With {@code -Darcanes.shots=a,b,prefix_*,school:x} the casts of those spells are also photographed a few times, each picture
 * logged as base64 JPEG lines {@code [SmokeShot] name part/parts data}, so CI shows what they look like
 * ({@code world_tree_end} also photographs a World Tree's end, after the last spell).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("arcanes.autotest");
    private static final int SPELL_LEVEL = 5;
    private static final int SETTLE_TICKS = 100;
    private static final int TICKS_PER_SPELL = 80;
    private static final int RECAST_DELAY = 60;
    /** Let the last spells (the ultimates, about 8 s) finish before stopping. */
    private static final int FINAL_TICKS = 220;
    private static final long WATCHDOG_SECONDS = 1950;
    /** Spells (ids without the namespace, or prefixes ending in *) whose casts are photographed into the log. */
    private static final List<String> SHOTS = Arrays.stream(System.getProperty("arcanes.shots", "").split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();
    /** Ticks after the cast each picture is taken. */
    private static final int[] SHOT_DELAYS = {10, 25, 45};
    /** A Boss attack lands when its warning runs out (3.5 s): its warning, then the moments it lands. */
    private static final int[] BOSS_SHOT_DELAYS = {30, 72, 78};
    /** The delays of the spell being photographed. */
    private static int[] shotDelays = SHOT_DELAYS;
    /**
     * With {@code world_tree_end} in {@code arcanes.shots}: after the last spell a World Tree that lives only a moment
     * is raised, and its end photographed this many ticks into it.
     */
    private static final int[] TREE_END_SHOTS = {6, 28, 50, 72};
    private static final int TREE_END_AFTER = 50;
    private static final int TREE_END_LIFE = 40;
    private static final int SHOT_WIDTH = 640;
    private static final int SHOT_CHUNK = 8000;

    private enum Stage { WAIT_TITLE, CREATING, SETTLING, CASTING, DONE }

    private static volatile Stage stage = Stage.WAIT_TITLE;
    private static int ticks;
    private static int spellIndex;
    /** The summoning spell cast last: a while later it is cast again, which must send its summons away. */
    private static volatile AbstractSpell toRecast;
    /** The last target dummy, cleared before the next one so they do not pile up in front of the camera. */
    private static Husk lastDummy;
    private static final AtomicInteger failures = new AtomicInteger();
    /** A held (continuous) spell being photographed: it is held on until the last picture. */
    private static volatile AbstractSpell held;
    /** The spell being photographed and the tick it was cast. */
    private static String shotName;
    /** Where the photographed spells are cast, once it is built. */
    private static BlockPos photoFloor;
    private static int shotCastTick;
    private static float shotYaw;
    /** Frames drawn so far. */
    private static int frames;
    /** The creative tab's next step, the tick and frame its last step was taken at, and the name of its next picture. */
    private static int creativeStep;
    private static int creativeTick;
    private static int creativeFrame;
    private static String creativeShot = "all";

    static {
        if (ENABLED) {
            Thread watchdog = new Thread(() -> {
                try {
                    Thread.sleep(WATCHDOG_SECONDS * 1000);
                } catch (InterruptedException e) {
                    return;
                }
                if (stage != Stage.DONE) {
                    ArcanesOublies.LOGGER.error("[SmokeTest] FAIL watchdog: stuck in stage {}", stage);
                    Runtime.getRuntime().halt(3);
                }
            }, "Arcanes smoke watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
        }
    }

    private ClientSmokeTest() {}

    private static void log(String msg, Object... args) {
        ArcanesOublies.LOGGER.info("[SmokeTest] " + msg, args);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ENABLED || stage != Stage.WAIT_TITLE || !(event.getNewScreen() instanceof TitleScreen)) {
            return;
        }
        stage = Stage.CREATING;
        log("title screen reached");
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            try {
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                LevelSettings settings = new LevelSettings("arcanes_smoketest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                        rules, WorldDataConfiguration.DEFAULT);
                WorldOptions options = new WorldOptions(20261018L, false, false);
                mc.createWorldOpenFlows().createFreshLevel("arcanes_smoketest_" + System.currentTimeMillis(), settings, options, WorldPresets::createNormalWorldDimensions, mc.screen);
            } catch (Throwable t) {
                ArcanesOublies.LOGGER.error("[SmokeTest] FAIL creating the world", t);
                finish(mc);
            }
        });
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        frames++;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        switch (stage) {
            case CREATING -> {
                if (mc.level != null && mc.player != null) {
                    log("joined the world");
                    checkScrolls(mc);
                    stage = Stage.SETTLING;
                    ticks = 0;
                }
            }
            case SETTLING -> {
                if (creativeTab(mc) && musicPlayer(mc) && ticks >= SETTLE_TICKS) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    stage = Stage.CASTING;
                    ticks = 0;
                    spellIndex = 0;
                }
            }
            case CASTING -> cast(mc);
            default -> {
            }
        }
    }

    private static void cast(Minecraft mc) {
        // Stellar Fire's spells come in seven colours sharing their code: its first (red) and its last (green) are cast
        List<DeferredHolder<AbstractSpell, ? extends AbstractSpell>> spells = ModSpells.SPELLS.getEntries().stream()
                .filter(e -> !(e.get() instanceof com.waylays.arcanes.spells.fire.StarfireSpell fire) || fire.tier() == 0
                        || fire.tier() == com.waylays.arcanes.spells.fire.HeatColors.TIERS - 1)
                .toList();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL no integrated server or player");
            finish(mc);
            return;
        }
        if (spellIndex < spells.size() && ticks % TICKS_PER_SPELL == 1) {
            AbstractSpell spell = spells.get(spellIndex++).get();
            String name = spell.getSpellId().substring(spell.getSpellId().indexOf(':') + 1);
            String school = "school:" + spell.getSchoolType().getId().getPath();
            shotName = SHOTS.stream().anyMatch(s -> s.endsWith("*") ? name.startsWith(s.substring(0, s.length() - 1)) : s.equals(name) || s.equals(school))
                    ? name : null;
            shotCastTick = ticks;
            shotDelays = spell instanceof BossSpell ? BOSS_SHOT_DELAYS : SHOT_DELAYS;
            java.util.UUID id = mc.player.getUUID();
            // The piranha needs its pool dug in solid ground: it is always cast on the photo floor
            boolean alone = shotName != null || spell == ModSpells.MANA_PIRANHA.get();
            server.execute(() -> castOnServer(server, id, spell, alone));
            // A held spell photographed is held on until the last picture, as a player would
            held = alone && spell.getCastType() == CastType.CONTINUOUS ? spell : null;
            if (spell instanceof PhysicalSkill || spell instanceof BossSpell) {
                // Play the move's body animation on the real player, as Iron's does when it starts a cast
                try {
                    ClientSpellCastHelper.handleClientBoundOnCastStarted(id, spell.getSpellId(), SPELL_LEVEL);
                } catch (Throwable t) {
                    failures.incrementAndGet();
                    ArcanesOublies.LOGGER.error("[SmokeTest] FAIL animating {}", spell.getSpellId(), t);
                }
            }
        }
        AbstractSpell holding = held;
        if (holding != null && ticks - shotCastTick <= shotDelays[shotDelays.length - 1]) {
            java.util.UUID id = mc.player.getUUID();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    holding.onServerCastTick(player.serverLevel(), SPELL_LEVEL, player, MagicData.getPlayerMagicData(player));
                }
            });
        }
        if (shotName != null && mc.player != null) {
            // Seen from a little to the right and looking down, then from the left, then looking up over the caster
            int since = ticks - shotCastTick;
            if (since == shotDelays[0] - 2) {
                shotYaw = mc.player.getYRot();
                aim(mc, shotYaw + 32, 24);
            } else if (since == shotDelays[1] - 2) {
                aim(mc, shotYaw - 32, 12);
            } else if (since == shotDelays[2] - 2) {
                // A Boss attack is mostly overhead (its eclipse, its moon, its storm): look higher
                aim(mc, shotYaw, shotDelays == BOSS_SHOT_DELAYS ? -55 : -25);
            }
            for (int k = 0; k < shotDelays.length; k++) {
                if (ticks - shotCastTick == shotDelays[k]) {
                    screenshot(mc, shotName + "_" + (k + 1));
                }
            }
        }
        if (ticks % TICKS_PER_SPELL == RECAST_DELAY + 1) {
            java.util.UUID id = mc.player.getUUID();
            server.execute(() -> recastOnServer(server, id));
        }
        if (SHOTS.contains("world_tree_end") && spellIndex >= spells.size()) {
            int since = ticks - ((spells.size() - 1) * TICKS_PER_SPELL + 1 + TREE_END_AFTER);
            if (since == 0) {
                java.util.UUID id = mc.player.getUUID();
                server.execute(() -> raiseEndingTree(server, id));
            }
            for (int k = 0; k < TREE_END_SHOTS.length; k++) {
                if (since == TREE_END_LIFE + TREE_END_SHOTS[k] - 2) {
                    aim(mc, k % 2 == 0 ? 16 : -16, -22);
                } else if (since == TREE_END_LIFE + TREE_END_SHOTS[k]) {
                    screenshot(mc, "world_tree_end_" + (k + 1));
                }
            }
        }
        if (spellIndex >= spells.size() && ticks > (spells.size() - 1) * TICKS_PER_SPELL + FINAL_TICKS) {
            finish(mc);
        }
    }

    /** A World Tree that lives only a moment, raised in front of the player on the photo floor, to photograph its end. */
    private static void raiseEndingTree(IntegratedServer server, java.util.UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            return;
        }
        try {
            ServerLevel level = player.serverLevel();
            player.stopRiding();
            toPhotoFloor(level, player);
            for (Entity e : level.getEntities((Entity) null, player.getBoundingBox().inflate(64), x -> !(x instanceof Player))) {
                e.discard();
            }
            lastDummy = null;
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), 0, -22);
            Vec3 spot = new Vec3(player.getX(), player.getY(), player.getZ() + 12);
            com.waylays.arcanes.entity.wood.WorldTree.raise(level, player, ModSpells.WORLD_TREE.get(), spot, TREE_END_LIFE, 0, 0, 0);
            log("raised a World Tree to photograph its end");
        } catch (Throwable t) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL raising the World Tree to photograph its end", t);
        }
    }

    private static void aim(Minecraft mc, float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.setXRot(pitch);
        mc.player.xRotO = pitch;
    }

    /**
     * Photographed spells are cast on a floor of stone high in the open air, at noon, the caster back in its middle each
     * time: no tree, pool or hill of the test world hides them, and they are seen against the sky.
     */
    private static void toPhotoFloor(ServerLevel level, ServerPlayer player) {
        if (photoFloor == null) {
            BlockPos center = BlockPos.containing(player.getX(), Math.min(level.getMaxBuildHeight() - 40, player.getY() + 60), player.getZ());
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-14, -1, -14), center.offset(14, -1, 14))) {
                level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            photoFloor = center;
            log("photographs are taken on a stone floor at {}", center);
        }
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.connection.teleport(photoFloor.getX() + .5, photoFloor.getY(), photoFloor.getZ() + .5, player.getYRot(), player.getXRot());
        player.resetFallDistance();
    }

    /** {@code alone}: everything earlier spells left around is cleared first, so its pictures show that spell only. */
    private static void castOnServer(IntegratedServer server, java.util.UUID id, AbstractSpell spell, boolean alone) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL server player not found");
            return;
        }
        try {
            ServerLevel level = player.serverLevel();
            // Off whatever it rides (the Flying Stack, a comet) first, or it would be put down beside it
            player.stopRiding();
            if (alone) {
                toPhotoFloor(level, player);
                for (Entity e : level.getEntities((Entity) null, player.getBoundingBox().inflate(64), x -> !(x instanceof Player))) {
                    e.discard();
                }
                lastDummy = null;
            }
            // Off the Flying Stack, so every spell is cast from the ground
            player.stopRiding();
            var school = spell.getSchoolType().getId();
            boolean fighting = school.equals(ModSchools.PHYSICAL_RESOURCE);
            // Fighting moves are done sword in hand (its damage counts), close to the dummy
            var book = fighting ? Items.IRON_SWORD : school.equals(ModSchools.EXPLOSION_RESOURCE) ? ModItems.DETONATION_CODEX.get()
                    : school.equals(ModSchools.BOOK_RESOURCE) ? ModItems.LIVING_GRIMOIRE.get()
                    : school.equals(ModSchools.BOSS_RESOURCE) ? ModItems.TYRANTS_CODEX.get()
                    : school.equals(ModSchools.RUNE_RESOURCE) ? ModItems.ELDER_RUNE_CODEX.get()
                    : school.equals(ModSchools.GLYPH_RESOURCE) ? ModItems.SIX_INKS_CODEX.get()
                    : school.equals(ModSchools.TOKEN_RESOURCE) ? ModItems.SOUL_LEDGER.get()
                    : school.equals(ModSchools.VERDIGRIS_RESOURCE) ? ModItems.RUSTED_CODEX.get()
                    : school.equals(ModSchools.WALL_RESOURCE) ? ModItems.RAMPART_CODEX.get()
                    : school.equals(ModSchools.GEM_RESOURCE) ? ModItems.JEWELLERS_CODEX.get()
                    : school.equals(ModSchools.COSMOS_RESOURCE) ? ModItems.CODEX_OUT_OF_TIME.get()
                    : school.equals(ModSchools.BONE_RESOURCE) ? ModItems.OSSUARY_CODEX.get()
                    : school.equals(ModSchools.WOOD_RESOURCE) ? ModItems.SYLVAN_CODEX.get()
                    : school.equals(ModSchools.WATER_RESOURCE) ? ModItems.TIDE_CODEX.get()
                    : school.equals(ModSchools.STARFIRE_RESOURCE) ? ModItems.STARFIRE_CODEX.get()
                    : school.equals(ModSchools.WIND_RESOURCE) ? ModItems.WINDS_CODEX.get()
                    : school.equals(ModSchools.ECHO_RESOURCE) ? ModItems.ECHOES_CODEX.get()
                    : school.equals(ModSchools.SAND_RESOURCE) ? ModItems.DUNES_CODEX.get()
                    : school.equals(ModSchools.LOTUS_RESOURCE) ? ModItems.LOTUS_CODEX.get()
                    : school.equals(ModSchools.AURORA_RESOURCE) ? ModItems.BOREAL_CODEX.get()
                    : school.equals(ModSchools.CLOCKWORK_RESOURCE) ? ModItems.CLOCKMAKERS_CODEX.get() : ModItems.GOLDEN_GRIMOIRE.get();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(book));
            // A sturdy target dummy 6 blocks in front of the player, at eye level of the raycast
            Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
            if (lastDummy != null) {
                lastDummy.discard();
            }
            Husk dummy = EntityType.HUSK.create(level);
            lastDummy = dummy;
            if (dummy != null) {
                Vec3 at = player.position().add(forward.scale(fighting ? 3 : 6));
                dummy.moveTo(at.x, player.getY(), at.z, player.getYRot() + 180, 0);
                dummy.setNoAi(true);
                dummy.setNoGravity(true);
                var health = dummy.getAttribute(Attributes.MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(500);
                }
                dummy.setHealth(500);
                level.addFreshEntity(dummy);
                // Look straight at the dummy's chest so targeted spells find it
                Vec3 eye = player.getEyePosition();
                Vec3 chest = dummy.position().add(0, dummy.getBbHeight() * .6, 0);
                Vec3 d = chest.subtract(eye);
                float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
                float pitch = (float) -(Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
                player.setYRot(yaw);
                player.setXRot(pitch);
                player.setYHeadRot(yaw);
            }
            if (spell == ModSpells.MANA_PIRANHA.get()) {
                // The piranha lives only in water: dig a pool in front of the player
                Vec3 pool = player.position().add(Vec3.directionFromRotation(0, player.getYRot()).scale(3));
                BlockPos center = BlockPos.containing(pool.x, player.getY() - 1, pool.z);
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
                    level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                }
            }
            MagicData magicData = MagicData.getPlayerMagicData(player);
            if (!spell.checkPreCastConditions(level, SPELL_LEVEL, player, magicData)) {
                log("{}: pre-cast conditions not met (no target in sight?), casting anyway", spell.getSpellId());
            }
            if (spell.getCastType() == CastType.LONG || spell.getCastType() == CastType.CONTINUOUS) {
                for (int i = 0; i < 10; i++) {
                    spell.onServerCastTick(level, SPELL_LEVEL, player, magicData);
                }
            }
            Vec3 before = player.position();
            spell.onCast(level, SPELL_LEVEL, player, CastSource.COMMAND, magicData);
            log("cast {}", spell.getSpellId());
            if (spell == ModSpells.LONGSWORD_PARRY.get() && dummy != null) {
                // The dummy swings at the player: the guard must catch it, and the riposte plays its animation. In
                // survival for the blow: a creative player shrugs damage off before anything can parry it
                player.setGameMode(GameType.SURVIVAL);
                player.hurt(level.damageSources().mobAttack(dummy), 1);
                player.setGameMode(GameType.CREATIVE);
                if (LongswordParrySkill.isGuarding(player)) {
                    failures.incrementAndGet();
                    ArcanesOublies.LOGGER.error("[SmokeTest] FAIL longsword_parry: the guard did not catch the dummy's blow");
                } else {
                    log("longsword_parry caught the dummy's blow and riposted");
                }
            }
            if (spell == ModSpells.DAGGER_FLANK.get()) {
                log("dagger_flank moved the player {} blocks (behind the dummy: {})", String.format("%.2f", before.distanceTo(player.position())),
                        dummy != null && player.position().subtract(dummy.position()).dot(Vec3.directionFromRotation(0, dummy.getYRot())) < 0);
            }
            if (spell instanceof SummoningSpell) {
                toRecast = spell;
            }
        } catch (Throwable t) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL casting {}", spell.getSpellId(), t);
        }
    }

    /**
     * Opens the creative inventory on the addon's tab and goes through its school entries: a school's entry must
     * leave that school's items only. Photographed with {@code creative} in {@code arcanes.shots}. Each step waits for
     * frames drawn since the one before (under xvfb a frame can take several ticks, and a picture is the last frame
     * drawn). Returns whether it is done.
     */
    private static boolean creativeTab(Minecraft mc) {
        if (creativeStep > 7 || ticks < 20 || ticks - creativeTick < 5 || frames - creativeFrame < 2) {
            return creativeStep > 7;
        }
        boolean shots = SHOTS.contains("creative");
        try {
            switch (creativeStep) {
                case 0 -> {
                    // Chosen before the screen opens, so that it opens on the page of tabs that holds the addon's
                    CreativeModeInventoryScreen.selectedTab = ModCreativeTabs.MAIN.get();
                    mc.setScreen(new InventoryScreen(mc.player));
                }
                case 1 -> {
                    if (mc.screen instanceof CreativeModeInventoryScreen screen) {
                        screen.selectTab(ModCreativeTabs.MAIN.get());
                        if (!screen.getCurrentPage().getVisibleTabs().contains(ModCreativeTabs.MAIN.get())) {
                            failures.incrementAndGet();
                            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the creative inventory is not on the page of the addon's tab");
                        }
                    } else {
                        failures.incrementAndGet();
                        ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the creative inventory did not open ({})", mc.screen);
                    }
                }
                case 2, 4, 6 -> {
                    if (shots) {
                        screenshot(mc, "creative_" + creativeShot);
                    }
                }
                case 3, 5 -> {
                    int index = creativeStep == 3 ? 1 : ModCreativeTabs.SECTIONS.size();
                    CreativeTabFilter.press(index);
                    ModCreativeTabs.Section section = ModCreativeTabs.SECTIONS.get(index - 1);
                    creativeShot = section.school().get().getId().getPath();
                    if (mc.screen instanceof CreativeModeInventoryScreen screen) {
                        var items = screen.getMenu().items;
                        if (items.isEmpty() || items.stream().anyMatch(stack -> !ModCreativeTabs.shows(section, stack))) {
                            failures.incrementAndGet();
                            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the {} button of the creative tab shows other items ({} items)",
                                    section.school().get().getId(), items.size());
                        } else {
                            log("creative tab: the {} button shows its {} items", section.school().get().getId(), items.size());
                        }
                    }
                }
                case 7 -> {
                    CreativeTabFilter.press(0);
                    mc.setScreen(null);
                }
                default -> {
                }
            }
            creativeStep++;
        } catch (Throwable t) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL going through the creative tab", t);
            mc.setScreen(null);
            creativeStep = 8;
        }
        creativeTick = ticks;
        creativeFrame = frames;
        return creativeStep > 7;
    }

    private static int musicStep;
    private static int musicTick;

    /**
     * The music player: the tune that comes with the addon (an MP3) must decode, and play on the player (where the game
     * has a sound device); its window is opened over it (photographed with {@code music} in {@code arcanes.shots}).
     * Returns whether it is done.
     */
    private static boolean musicPlayer(Minecraft mc) {
        if (musicStep > 2) {
            return true;
        }
        if (musicStep == 0 && ticks < 30) {
            return false;
        }
        try {
            switch (musicStep) {
                case 0 -> {
                    var lib = com.waylays.arcanes.client.music.MusicLibrary.get();
                    var track = lib.track("builtin:valse_des_arcanes");
                    if (track == null) {
                        failures.incrementAndGet();
                        ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the music player has not got its built-in tune");
                        musicStep = 3;
                        return true;
                    }
                    // Decoded here and now: the MP3 decoder must be there and work
                    byte[] data = track.read();
                    var stream = new com.waylays.arcanes.client.music.MusicStream(data, track.ext, 2000, false, null);
                    double sum = 0;
                    int n = 0;
                    for (int k = 0; k < 4; k++) {
                        java.nio.ByteBuffer buf = stream.read(44100);
                        while (buf != null && buf.remaining() >= 2) {
                            double v = buf.getShort() / 32768.0;
                            sum += v * v;
                            n++;
                        }
                    }
                    stream.close();
                    double rms = n > 0 ? Math.sqrt(sum / n) : 0;
                    int length = com.waylays.arcanes.client.music.MusicDecoders.lengthMs(data, track.ext);
                    if (n < 40000 || rms < .01 || length < 20000) {
                        failures.incrementAndGet();
                        ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the built-in tune did not decode ({} samples, loudness {}, {} ms long)", n, rms, length);
                    } else {
                        log("music player: the built-in tune decodes ({} samples, loudness {}, {} ms long)", n, String.format(java.util.Locale.ROOT, "%.3f", rms),
                                length);
                    }
                    mc.setScreen(new com.waylays.arcanes.client.music.MusicScreen());
                    com.waylays.arcanes.client.music.MusicPlayer.play(track, lib.all());
                    musicTick = ticks;
                    musicStep = 1;
                }
                case 1 -> {
                    if (com.waylays.arcanes.client.music.MusicPlayer.loading() && ticks - musicTick < 200 || ticks - musicTick < 70) {
                        return false;
                    }
                    long at = com.waylays.arcanes.client.music.MusicPlayer.positionMs();
                    boolean active = com.waylays.arcanes.client.music.MusicPlayer.soundActive();
                    if (com.waylays.arcanes.client.music.MusicPlayer.current() == null || at < 1500) {
                        failures.incrementAndGet();
                        ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the music player did not play its tune (at {} ms)", at);
                    } else {
                        log("music player: playing, {} ms in, sound {}", at, active ? "playing on the player" : "not heard (no sound device?)");
                    }
                    if (SHOTS.contains("music")) {
                        screenshot(mc, "music_player");
                    }
                    musicTick = ticks;
                    musicStep = 2;
                }
                case 2 -> {
                    com.waylays.arcanes.client.music.MusicPlayer.stop();
                    mc.setScreen(null);
                    musicStep = 3;
                }
                default -> {
                }
            }
        } catch (Throwable t) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the music player", t);
            mc.setScreen(null);
            musicStep = 3;
        }
        return musicStep > 2;
    }

    /**
     * The addon's scrolls are Iron's scroll recoloured by the game (tools/scroll_assets.py): every colour of Iron's
     * scroll must be in the palette key, or some of it keeps its old colour, and every school must get its scroll.
     */
    private static void checkScrolls(Minecraft mc) {
        try {
            Set<Integer> uncovered = colours(mc, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/item/scroll.png"));
            uncovered.removeAll(colours(mc, ArcanesOublies.id("textures/item/scroll_palettes/key.png")));
            if (!uncovered.isEmpty()) {
                failures.incrementAndGet();
                ArcanesOublies.LOGGER.error("[SmokeTest] FAIL Iron's scroll has colours the scroll palettes do not cover: {}",
                        uncovered.stream().map(c -> String.format("%06x", c)).toList());
            }
            TextureAtlas atlas = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
            for (var school : ModSchools.SCHOOLS.getEntries()) {
                ResourceLocation sprite = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "item/scroll_arcanes_" + school.getId().getPath());
                if (atlas.getSprite(sprite).contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                    failures.incrementAndGet();
                    ArcanesOublies.LOGGER.error("[SmokeTest] FAIL no recoloured scroll {} for the {} school", sprite, school.getId());
                }
            }
            log("scrolls checked for {} schools", ModSchools.SCHOOLS.getEntries().size());
        } catch (Throwable t) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL checking the scrolls", t);
        }
    }

    /** The opaque colours of a picture, as RGB. */
    private static Set<Integer> colours(Minecraft mc, ResourceLocation file) throws IOException {
        try (InputStream in = mc.getResourceManager().open(file); NativeImage image = NativeImage.read(in)) {
            Set<Integer> out = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    // ABGR
                    int p = image.getPixelRGBA(x, y);
                    if (p >>> 24 != 0) {
                        out.add((p & 0xFF) << 16 | (p >> 8 & 0xFF) << 8 | (p >> 16 & 0xFF));
                    }
                }
            }
            return out;
        }
    }

    /** The last frame drawn, halved and logged as base64 JPEG lines (a picture never fails the run). */
    private static void screenshot(Minecraft mc, String name) {
        try (NativeImage shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int f = Math.max(1, shot.getWidth() / SHOT_WIDTH);
            int w = shot.getWidth() / f;
            int h = shot.getHeight() / f;
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = 0, g = 0, b = 0;
                    for (int dy = 0; dy < f; dy++) {
                        for (int dx = 0; dx < f; dx++) {
                            // ABGR
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
                ArcanesOublies.LOGGER.info("[SmokeShot] {} {}/{} {}", name, i + 1, parts,
                        data.substring(i * SHOT_CHUNK, Math.min(data.length(), (i + 1) * SHOT_CHUNK)));
            }
        } catch (Throwable t) {
            ArcanesOublies.LOGGER.warn("[SmokeTest] could not photograph {}", name, t);
        }
    }

    /** Casts the last summoning spell again through Iron's real cast path, as a second click would. */
    private static void recastOnServer(IntegratedServer server, java.util.UUID id) {
        AbstractSpell spell = toRecast;
        toRecast = null;
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (spell == null || player == null) {
            return;
        }
        try {
            ServerLevel level = player.serverLevel();
            MagicData magicData = MagicData.getPlayerMagicData(player);
            if (!magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId()) || SummoningSpell.summonsOf(level, id, spell).isEmpty()) {
                failures.incrementAndGet();
                ArcanesOublies.LOGGER.error("[SmokeTest] FAIL {}: no recast open or no summons to send away", spell.getSpellId());
                return;
            }
            spell.castSpell(level, SPELL_LEVEL, player, CastSource.COMMAND, true);
            if (!SummoningSpell.summonsOf(level, id, spell).isEmpty() || magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId())) {
                failures.incrementAndGet();
                ArcanesOublies.LOGGER.error("[SmokeTest] FAIL {}: casting again did not send the summons away", spell.getSpellId());
            } else {
                log("recast {}: summons sent away", spell.getSpellId());
            }
        } catch (Throwable t) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL recasting {}", spell.getSpellId(), t);
        }
    }

    private static void finish(Minecraft mc) {
        stage = Stage.DONE;
        // Every body animation the fighting moves use must have loaded (a missing one would leave the move unanimated)
        for (var animation : java.util.stream.Stream.concat(PhysicalAnimations.ALL.stream(), BossAnimations.ALL.stream()).toList()) {
            animation.getForPlayer().ifPresent(id -> {
                if (PlayerAnimationRegistry.getAnimation(id) == null) {
                    failures.incrementAndGet();
                    ArcanesOublies.LOGGER.error("[SmokeTest] FAIL animation {} did not load", id);
                }
            });
        }
        if (failures.get() == 0) {
            log("PASS: every spell was cast and rendered without a crash");
        } else {
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL {} problem(s), see above", failures.get());
        }
        mc.stop();
    }
}
