package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/** The trades on the client: opens the right minigame, the stations' recipe lists, the editor; passes on what the server says. */
public final class MetierClient {
    private MetierClient() {}

    /** The minigame screen for {@code game} (a trial when {@code start} is null). */
    public static MinigameScreen create(MetierDefs.Game game, @Nullable MetierNet.Start start, double difficulty, long seed, ItemStack subject,
                                        ItemStack tool, List<ItemStack> inputs, Component heading, Component sub, float perfectAt, float criticalBelow) {
        return switch (game) {
            case MINING -> new MiningGame(start, difficulty, seed, subject, tool, heading, sub, perfectAt, criticalBelow);
            case HARVEST -> new HarvestGame(start, difficulty, seed, subject, tool, heading, sub, perfectAt, criticalBelow);
            case ASSEMBLY -> new AssemblyGame(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow);
            case FORGE -> new ForgeGame(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow);
            case DISTILL -> new DistillGame(start, difficulty, seed, subject, tool, inputs, heading, sub, perfectAt, criticalBelow);
        };
    }

    public static void start(MetierNet.Start s) {
        MetierDefs.Game[] games = MetierDefs.Game.values();
        MetierDefs.Game game = games[Mth.clamp(s.game(), 0, games.length - 1)];
        Minecraft.getInstance().setScreen(create(game, s, s.difficulty(), s.seed(), s.subject(), s.tool(), s.inputs(), Component.literal(s.title()),
                s.subtitle().isEmpty() ? Component.translatable("metier.arcanes_oublies.game." + game.name().toLowerCase(java.util.Locale.ROOT))
                        : Component.literal(s.subtitle()), s.perfectAt(), s.criticalBelow()));
    }

    @Nullable
    private static MinigameScreen current(int session) {
        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof MinigameScreen m && m.session() == session ? m : null;
    }

    public static void outcome(MetierNet.Outcome o) {
        MinigameScreen m = current(o.session());
        if (m != null) {
            m.outcome(o);
        }
    }

    public static void stop(MetierNet.Stop s) {
        MinigameScreen m = current(s.session());
        if (m != null) {
            m.stopped(Component.translatable(s.reason()));
        } else if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(s.reason()), true);
        }
    }

    public static void station(MetierNet.Station s) {
        Minecraft.getInstance().setScreen(new StationScreen(s));
    }

    public static void editor(MetierNet.Editor e) {
        MetierDefs.Config config;
        try {
            config = MetierDefs.fromJson(e.json());
        } catch (RuntimeException ex) {
            config = MetierDefs.defaults();
        }
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof MetierEditorScreen editor) {
            editor.reload(config);
        } else {
            Minecraft.getInstance().setScreen(new MetierEditorScreen(config));
        }
    }
}
