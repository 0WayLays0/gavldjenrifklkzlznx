package com.waylays.arcanes.debug;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpRegistry;
import com.waylays.arcanes.rp.RpService;
import com.waylays.arcanes.rp.arms.Arms;
import com.waylays.arcanes.rp.arms.Proc;
import com.waylays.arcanes.rp.arms.client.WorkshopScreen;
import com.waylays.arcanes.rp.horror.Fear;
import com.waylays.arcanes.rp.horror.HorrorNet;
import com.waylays.arcanes.rp.horror.WallWriting;
import com.waylays.arcanes.rp.horror.client.HorrorClient;
import com.waylays.arcanes.rp.size.Body;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The client test's part for the role play and the game masters' tools (once part of Époque RP): a character's name,
 * an action and a roll in the chat; the tools of fear on oneself (a whisper, blood, fog, the red balloon, a narration);
 * words written on a wall; the fear gauge (the staff feel nothing, the gauge shows); a size of 100 cm (the hitbox with
 * it); the weapons' workshop on a sword with chance effects; health set by /hp. One step every few ticks.
 */
final class RoleplaySmoke {
    private static final int TICKS_PER_STEP = 12;
    private static final List<Consumer<Minecraft>> STEPS = new ArrayList<>();
    private static int step = -1;
    private static int lastTick;
    private static boolean failed;

    private RoleplaySmoke() {}

    /** Runs the next step when it is time; whether all are done. {@code fail} is told each failure. */
    static boolean run(Minecraft mc, int ticks, Consumer<String> fail) {
        if (step < 0) {
            plan(fail);
            step = 0;
            lastTick = ticks;
        }
        if (step >= STEPS.size()) {
            return true;
        }
        if (ticks - lastTick < TICKS_PER_STEP) {
            return false;
        }
        lastTick = ticks;
        try {
            STEPS.get(step++).accept(mc);
        } catch (Throwable t) {
            ArcanesOublies.LOGGER.error("[SmokeTest] FAIL the role play's step {}", step - 1, t);
            fail.accept("the role play's step " + (step - 1) + " threw " + t);
            step = STEPS.size();
            mc.setScreen(null);
        }
        if (step >= STEPS.size()) {
            ArcanesOublies.LOGGER.info("[SmokeTest] role play: {} steps done{}", STEPS.size(), failed ? " (with failures)" : "");
        }
        return step >= STEPS.size();
    }

    private static void command(Minecraft mc, String cmd) {
        mc.player.connection.sendCommand(cmd);
    }

    /** Runs {@code action} on the server, with the player's own server side. */
    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        IntegratedServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                action.accept(p);
            }
        });
    }

    private static void plan(Consumer<String> fail0) {
        Consumer<String> fail = msg -> {
            failed = true;
            fail0.accept(msg);
        };
        STEPS.add(mc -> {
            command(mc, "setname Thomas Hale");
            command(mc, "me ouvre lentement la porte de la cave");
            command(mc, "roll 1d20");
        });
        STEPS.add(mc -> {
            if (!mc.player.getDisplayName().getString().equals("Thomas Hale")) {
                fail.accept("the character's name is not shown: " + mc.player.getDisplayName().getString());
            }
            var server = mc.getSingleplayerServer();
            var team = server == null ? null : server.getScoreboard().getPlayersTeam(mc.player.getScoreboardName());
            if (team == null || team.getNameTagVisibility() != net.minecraft.world.scores.Team.Visibility.NEVER) {
                fail.accept("the names over the heads are not hidden by default");
            }
            command(mc, "voix masque");
            command(mc, "voix normale");
            command(mc, "horreur son @s rire");
            command(mc, "horreur murmure @s Tu flotteras aussi...");
        });
        STEPS.add(mc -> command(mc, "horreur sang @s 12"));
        STEPS.add(mc -> command(mc, "horreur brouillard @s 30"));
        STEPS.add(mc -> command(mc, "horreur ballon @s"));
        STEPS.add(mc -> {
            command(mc, "horreur stop @s");
            HorrorClient.effect(new HorrorNet.Effect("narration", 200, "La pluie tombait sur la ville depuis trois jours.", 0, 0, 0));
        });
        STEPS.add(mc -> {
            HorrorClient.effect(new HorrorNet.Effect("stop", 0, "", 0, 0, 0));
            server(mc, p -> {
                Direction dir = p.getDirection();
                Vec3 at = p.getEyePosition().add(Vec3.atLowerCornerOf(dir.getNormal()).scale(3));
                WallWriting w = new WallWriting(RpRegistry.WRITING.get(), p.level());
                w.setPos(at.x, at.y, at.z);
                w.setup("IL EST REVENU", "sang", dir.getOpposite().get3DDataValue(), .5f, 0);
                p.level().addFreshEntity(w);
                if (!Fear.exempt(p)) {
                    fail.accept("the game master is not exempt from the fear gauge");
                }
                Fear.scare(p, 50);
            });
        });
        STEPS.add(mc -> HorrorClient.gauge(85));
        STEPS.add(mc -> {
        });
        STEPS.add(mc -> {
            if (HorrorClient.gaugeShown() < 40) {
                fail.accept("the fear gauge is not shown (" + HorrorClient.gaugeShown() + ")");
            }
            HorrorClient.gauge(0);
            if (!RpService.closesEyes("/me ferme lentement... non: Il ferme les yeux") || RpService.closesEyes("ferme la porte")) {
                fail.accept("closed eyes are not told apart in /me");
            }
            command(mc, "taille @s 100");
        });
        STEPS.add(mc -> {
            if (Math.abs(mc.player.getBbHeight() - 1.0f) > .05f) {
                fail.accept("at 100 cm the player is " + mc.player.getBbHeight() + " blocks tall");
            }
            command(mc, "taille @s normale");
            server(mc, p -> {
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                sword.set(Arms.PROCS.get(), List.of(new Proc("*feu", 5, 1, 4, false, false), new Proc("minecraft:poison", 10, 2, 5, false, false)));
                p.setItemInHand(InteractionHand.MAIN_HAND, sword);
            });
        });
        STEPS.add(mc -> mc.setScreen(new WorkshopScreen(mc.player.getMainHandItem())));
        STEPS.add(mc -> {
            mc.setScreen(null);
            server(mc, p -> {
                Body.setHealth(p, 40);
                if (Math.abs(p.getMaxHealth() - 40) > .01f) {
                    fail.accept("/hp 40 gave " + p.getMaxHealth() + " health");
                }
                Body.setHealth(p, 20);
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
        });
    }
}
