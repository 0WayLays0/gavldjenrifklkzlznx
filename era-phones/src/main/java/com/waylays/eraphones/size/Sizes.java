package com.waylays.eraphones.size;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.rp.RpConfig;
import com.waylays.eraphones.rp.RpService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Collection;
import java.util.List;

/**
 * The players' heights, set by a game master in centimetres (180 is the usual): /taille &lt;players&gt; &lt;cm&gt; (50 to
 * 400), /taille &lt;players&gt; normale, /taille (one's own). The whole body follows: the size drawn, the hitbox, the
 * eyes' height (so what is seen), the camera behind, the reach of the hands, the step up, the stride and the jump (a
 * little), the fall one takes without harm. Kept with the player, through death.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Sizes {
    public static final int NORMAL = 180, MIN = 50, MAX = 400;
    private static final ResourceLocation ID = EraPhones.id("taille");
    private static final String KEY = EraPhones.MODID + "_taille";

    private Sizes() {}

    private record Part(Holder<Attribute> attribute, AttributeModifier.Operation op, boolean root) {}

    /** What changes with the height: the size itself (added to 1), the rest in proportion (or as its square root). */
    private static final List<Part> PARTS = List.of(
            new Part(Attributes.SCALE, AttributeModifier.Operation.ADD_VALUE, false),
            new Part(Attributes.STEP_HEIGHT, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false),
            new Part(Attributes.BLOCK_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false),
            new Part(Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false),
            new Part(Attributes.SAFE_FALL_DISTANCE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false),
            new Part(Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true),
            new Part(Attributes.JUMP_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true));

    public static int of(Player p) {
        int cm = p.getPersistentData().getInt(KEY);
        return cm <= 0 ? NORMAL : cm;
    }

    /** Makes {@code p} {@code cm} tall. */
    public static void set(Player p, int cm) {
        cm = Math.max(MIN, Math.min(MAX, cm));
        double k = cm / (double) NORMAL;
        for (Part part : PARTS) {
            AttributeInstance a = p.getAttribute(part.attribute);
            if (a == null) {
                continue;
            }
            a.removeModifier(ID);
            if (cm != NORMAL) {
                double v = part.root ? Math.sqrt(k) - 1 : k - 1;
                a.addPermanentModifier(new AttributeModifier(ID, v, part.op));
            }
        }
        if (cm == NORMAL) {
            p.getPersistentData().remove(KEY);
        } else {
            p.getPersistentData().putInt(KEY, cm);
        }
        p.refreshDimensions();
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        int cm = of(event.getOriginal());
        if (cm != NORMAL) {
            set(event.getEntity(), cm);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        int cm = of(event.getEntity());
        if (cm != NORMAL) {
            set(event.getEntity(), cm);
        }
    }

    // ---------------------------------------------------------------- /taille

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("taille")
                .executes(Sizes::own)
                .then(Commands.argument("joueurs", EntityArgument.players()).requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                        .then(Commands.literal("normale").executes(c -> apply(c, NORMAL)))
                        .then(Commands.argument("cm", IntegerArgumentType.integer(MIN, MAX)).executes(c -> apply(c, IntegerArgumentType.getInteger(c, "cm"))))));
    }

    private static int own(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int cm = of(p);
        c.getSource().sendSuccess(() -> Component.translatable("size.eraphones.own", cm).withStyle(ChatFormatting.GRAY), false);
        return cm;
    }

    private static int apply(CommandContext<CommandSourceStack> c, int cm) throws CommandSyntaxException {
        Collection<ServerPlayer> who = EntityArgument.getPlayers(c, "joueurs");
        for (ServerPlayer p : who) {
            set(p, cm);
            c.getSource().sendSuccess(() -> Component.translatable("size.eraphones.set", RpService.name(p), cm).withStyle(ChatFormatting.GRAY), true);
        }
        return who.size();
    }
}
