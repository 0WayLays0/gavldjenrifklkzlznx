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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

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

    private record Part(java.util.function.Supplier<Attribute> attribute, AttributeModifier.Operation op, double base, boolean root) {}

    /**
     * What changes with the height besides the size itself (1.20.1 has no size attribute: the hitbox and the eyes follow
     * the entity's own size, the body is drawn bigger or smaller, the jump and the fall are taken care of below): the
     * step up, the hands' reach (added, from their usual values), the stride (as the square root).
     */
    private static final List<Part> PARTS = List.of(
            new Part(() -> net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(), AttributeModifier.Operation.ADDITION, .6, false),
            new Part(() -> net.minecraftforge.common.ForgeMod.BLOCK_REACH.get(), AttributeModifier.Operation.ADDITION, 4.5, false),
            new Part(() -> net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(), AttributeModifier.Operation.ADDITION, 3, false),
            new Part(() -> Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE, 1, true));
    private static final java.util.UUID UUID_ = java.util.UUID.nameUUIDFromBytes("eraphones:taille".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    /** The heights known on this side (by entity id): the server's players, and on a client those it was told of. */
    private static final java.util.Map<Integer, Integer> KNOWN = new java.util.concurrent.ConcurrentHashMap<>();

    public static int of(Player p) {
        Integer known = KNOWN.get(p.getId());
        if (known != null && p.level().isClientSide) {
            return known;
        }
        int cm = p.getPersistentData().getInt(KEY);
        return cm <= 0 ? NORMAL : cm;
    }

    /** The size, 1 for the usual height. */
    public static float scale(Player p) {
        return of(p) / (float) NORMAL;
    }

    /** Makes {@code p} {@code cm} tall. */
    public static void set(Player p, int cm) {
        cm = Math.max(MIN, Math.min(MAX, cm));
        double k = cm / (double) NORMAL;
        for (Part part : PARTS) {
            AttributeInstance a = p.getAttribute(part.attribute.get());
            if (a == null) {
                continue;
            }
            a.removeModifier(UUID_);
            if (cm != NORMAL) {
                double v = part.root ? Math.sqrt(k) - 1 : part.base * (k - 1);
                a.addPermanentModifier(new AttributeModifier(UUID_, "eraphones taille", v, part.op));
            }
        }
        if (cm == NORMAL) {
            p.getPersistentData().remove(KEY);
            KNOWN.remove(p.getId());
        } else {
            p.getPersistentData().putInt(KEY, cm);
            KNOWN.put(p.getId(), cm);
        }
        p.refreshDimensions();
        if (p instanceof ServerPlayer sp) {
            com.waylays.eraphones.compat.PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new Sync(sp.getId(), cm));
        }
    }

    /** The hitbox and the eyes, drawn from the height. */
    @SubscribeEvent
    public static void onSize(net.minecraftforge.event.entity.EntityEvent.Size event) {
        if (event.getEntity() instanceof Player p) {
            float k = scale(p);
            if (k != 1f) {
                event.setNewSize(event.getNewSize().scale(k), false);
                event.setNewEyeHeight(event.getNewEyeHeight() * k);
            }
        }
    }

    /** The jump, a little higher or lower (as the square root). */
    @SubscribeEvent
    public static void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player p && scale(p) != 1f) {
            net.minecraft.world.phys.Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, v.y * Math.sqrt(scale(p)), v.z);
        }
    }

    /** The fall taken without harm, in proportion. */
    @SubscribeEvent
    public static void onFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && scale(p) != 1f) {
            event.setDistance(event.getDistance() / scale(p));
        }
    }

    @SubscribeEvent
    public static void onTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer && of(target) != NORMAL) {
            com.waylays.eraphones.compat.PacketDistributor.sendToPlayer(viewer, new Sync(target.getId(), of(target)));
        }
    }

    public static void register(com.waylays.eraphones.compat.PayloadRegistrar r) {
        r.playToClient(Sync.TYPE, Sync.CODEC, (p, c) -> {
            if (p.cm() == NORMAL) {
                KNOWN.remove(p.id());
            } else {
                KNOWN.put(p.id(), p.cm());
            }
            net.minecraft.world.entity.Entity e = c.player() == null ? null : c.player().level().getEntity(p.id());
            if (e != null) {
                e.refreshDimensions();
            }
        });
    }

    /** A player's height, to the clients that see them. */
    public record Sync(int id, int cm) implements com.waylays.eraphones.compat.CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(EraPhones.id("size_sync"));
        public static final com.waylays.eraphones.compat.StreamCodec<net.minecraft.network.FriendlyByteBuf, Sync> CODEC =
                com.waylays.eraphones.compat.CustomPacketPayload.codec((p, b) -> {
                    b.writeVarInt(p.id);
                    b.writeVarInt(p.cm);
                }, b -> new Sync(b.readVarInt(), b.readVarInt()));

        @Override
        public Type<Sync> type() {
            return TYPE;
        }
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
