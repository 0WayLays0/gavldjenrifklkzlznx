package com.waylays.arcanes.debug;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SummoningSpell;
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
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("arcanes.autotest");
    private static final int SPELL_LEVEL = 5;
    private static final int SETTLE_TICKS = 100;
    private static final int TICKS_PER_SPELL = 80;
    private static final int RECAST_DELAY = 60;
    /** Let the last spells (the ultimates, about 8 s) finish before stopping. */
    private static final int FINAL_TICKS = 220;
    private static final long WATCHDOG_SECONDS = 1200;

    private enum Stage { WAIT_TITLE, CREATING, SETTLING, CASTING, DONE }

    private static volatile Stage stage = Stage.WAIT_TITLE;
    private static int ticks;
    private static int spellIndex;
    /** The summoning spell cast last: a while later it is cast again, which must send its summons away. */
    private static volatile AbstractSpell toRecast;
    /** The last target dummy, cleared before the next one so they do not pile up in front of the camera. */
    private static Husk lastDummy;
    private static final AtomicInteger failures = new AtomicInteger();

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
                    stage = Stage.SETTLING;
                    ticks = 0;
                }
            }
            case SETTLING -> {
                if (ticks >= SETTLE_TICKS) {
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
        List<DeferredHolder<AbstractSpell, ? extends AbstractSpell>> spells = List.copyOf(ModSpells.SPELLS.getEntries());
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL no integrated server or player");
            finish(mc);
            return;
        }
        if (spellIndex < spells.size() && ticks % TICKS_PER_SPELL == 1) {
            AbstractSpell spell = spells.get(spellIndex++).get();
            java.util.UUID id = mc.player.getUUID();
            server.execute(() -> castOnServer(server, id, spell));
            if (spell instanceof PhysicalSkill) {
                // Play the move's body animation on the real player, as Iron's does when it starts a cast
                try {
                    ClientSpellCastHelper.handleClientBoundOnCastStarted(id, spell.getSpellId(), SPELL_LEVEL);
                } catch (Throwable t) {
                    failures.incrementAndGet();
                    ArcanesOublies.LOGGER.error("[SmokeTest] FAIL animating {}", spell.getSpellId(), t);
                }
            }
        }
        if (ticks % TICKS_PER_SPELL == RECAST_DELAY + 1) {
            java.util.UUID id = mc.player.getUUID();
            server.execute(() -> recastOnServer(server, id));
        }
        if (spellIndex >= spells.size() && ticks > (spells.size() - 1) * TICKS_PER_SPELL + FINAL_TICKS) {
            finish(mc);
        }
    }

    private static void castOnServer(IntegratedServer server, java.util.UUID id, AbstractSpell spell) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL server player not found");
            return;
        }
        try {
            ServerLevel level = player.serverLevel();
            // Off the Flying Stack, so every spell is cast from the ground
            player.stopRiding();
            var school = spell.getSchoolType().getId();
            boolean fighting = school.equals(ModSchools.PHYSICAL_RESOURCE);
            // Fighting moves are done sword in hand (its damage counts), close to the dummy
            var book = fighting ? Items.IRON_SWORD : school.equals(ModSchools.EXPLOSION_RESOURCE) ? ModItems.DETONATION_CODEX.get()
                    : school.equals(ModSchools.BOOK_RESOURCE) ? ModItems.LIVING_GRIMOIRE.get() : ModItems.GOLDEN_GRIMOIRE.get();
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
            if (spell == ModSpells.MANA_SALMON.get()) {
                // The salmon lives only in water: dig a pool in front of the player
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
                // The dummy swings at the player: the guard must catch it, and the riposte plays its animation
                player.hurt(level.damageSources().mobAttack(dummy), 1);
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
        for (var animation : PhysicalAnimations.ALL) {
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
