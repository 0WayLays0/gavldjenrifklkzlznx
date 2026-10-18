package com.waylays.arcanes.debug;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSpells;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automated client test, only with {@code -Darcanes.autotest=true} (CI job {@code client-smoke}, headless under xvfb).
 * Creates a creative world, holds the Golden Grimoire, then casts every addon spell (level 5) at a target dummy in
 * front of the camera, one every few seconds, so every spell entity, particle and renderer runs on a real client.
 * A client crash ends the run with a crash report; a spell that throws is logged as {@code [SmokeTest] FAIL}.
 * The run ends with {@code [SmokeTest] PASS} or {@code [SmokeTest] FAIL ...} and stops the client.
 * A watchdog halts the JVM (exit code 3) if the whole run takes too long.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("arcanes.autotest");
    private static final int SPELL_LEVEL = 5;
    private static final int SETTLE_TICKS = 100;
    private static final int TICKS_PER_SPELL = 80;
    /** The last spell (the sun) lives about 8 s: let it finish before stopping. */
    private static final int FINAL_TICKS = 220;
    private static final long WATCHDOG_SECONDS = 1200;

    private enum Stage { WAIT_TITLE, CREATING, SETTLING, CASTING, DONE }

    private static volatile Stage stage = Stage.WAIT_TITLE;
    private static int ticks;
    private static int spellIndex;
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
                mc.createWorldOpenFlows().createFreshLevel("arcanes_smoketest_" + System.currentTimeMillis(), settings, options, WorldPresets::createNormalWorldDimensions);
            } catch (Throwable t) {
                ArcanesOublies.LOGGER.error("[SmokeTest] FAIL creating the world", t);
                finish(mc);
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) {
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
        List<RegistryObject<AbstractSpell>> spells = List.copyOf(ModSpells.SPELLS.getEntries());
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
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLDEN_GRIMOIRE.get()));
            // A sturdy target dummy 6 blocks in front of the player, at eye level of the raycast
            Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
            Husk dummy = EntityType.HUSK.create(level);
            if (dummy != null) {
                Vec3 at = player.position().add(forward.scale(6));
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
            MagicData magicData = MagicData.getPlayerMagicData(player);
            if (!spell.checkPreCastConditions(level, SPELL_LEVEL, player, magicData)) {
                log("{}: pre-cast conditions not met (no target in sight?), casting anyway", spell.getSpellId());
            }
            if (spell.getCastType() == CastType.LONG || spell.getCastType() == CastType.CONTINUOUS) {
                for (int i = 0; i < 10; i++) {
                    spell.onServerCastTick(level, SPELL_LEVEL, player, magicData);
                }
            }
            spell.onCast(level, SPELL_LEVEL, player, CastSource.COMMAND, magicData);
            log("cast {}", spell.getSpellId());
        } catch (Throwable t) {
            failures.incrementAndGet();
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL casting {}", spell.getSpellId(), t);
        }
    }

    private static void finish(Minecraft mc) {
        stage = Stage.DONE;
        if (failures.get() == 0) {
            log("PASS: every spell was cast and rendered without a crash");
        } else {
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL {} problem(s), see above", failures.get());
        }
        mc.stop();
    }
}
