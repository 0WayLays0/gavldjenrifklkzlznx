package com.waylays.arcanes.rp.carry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpConfig;
import com.waylays.arcanes.rp.RpService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Carrying someone in one's arms: crouching with an empty hand, use another player: they are lifted at once, lying
 * across the carrier's arms. The carrier walks more slowly; crouching with an empty hand again (or /porter lacher), they set them down; the one carried gets
 * down by crouching. A game master can lift anyone onto anyone: /porter &lt;carrier&gt; &lt;carried&gt;.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Carry {
    private static final ResourceLocation SLOW = ArcanesOublies.id("porter");

    private Carry() {}

    /** Whether {@code carrier} holds {@code carried} in their arms. */
    public static boolean carrying(Entity carrier, Entity carried) {
        return carrier instanceof Player && carried instanceof Player && carried.getVehicle() == carrier;
    }

    /** Where the one carried lies: before the carrier, at the height of their arms. Returns false if not carried. */
    public static boolean position(Entity carrier, Entity carried, Entity.MoveFunction move) {
        if (!carrying(carrier, carried)) {
            return false;
        }
        float yaw = carrier instanceof Player p ? p.yBodyRot : carrier.getYRot();
        float r = yaw * Mth.DEG_TO_RAD;
        double k = carrier instanceof Player p ? p.getScale() : 1;
        Vec3 at = carrier.position().add(-Mth.sin(r) * .5 * k, .14 * k, Mth.cos(r) * .5 * k);
        move.accept(carried, at.x, at.y, at.z);
        if (carried instanceof Player c) {
            c.setYBodyRot(yaw);
            c.yBodyRotO = c.yBodyRot;
        }
        return true;
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p)
                || !p.isSecondaryUseActive() || !p.getMainHandItem().isEmpty()) {
            return;
        }
        if (p.isVehicle()) {
            setDown(p);
            event.setCanceled(true);
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target) || target.isPassenger() || target.isVehicle()) {
            return;
        }
        event.setCanceled(true);
        // No asking: the player is picked up at once
        lift(p, target);
    }

    public static void lift(ServerPlayer carrier, ServerPlayer carried) {
        if (carried.isPassenger()) {
            carried.stopRiding();
        }
        carried.ejectPassengers();
        if (carried.startRiding(carrier, true)) {
            carrier.displayClientMessage(Component.translatable("carry.arcanes_oublies.lifted", RpService.name(carried)).withStyle(ChatFormatting.GRAY), true);
            carried.displayClientMessage(Component.translatable("carry.arcanes_oublies.carried", RpService.name(carrier)).withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void setDown(ServerPlayer carrier) {
        for (Entity e : carrier.getPassengers()) {
            if (e instanceof Player) {
                e.stopRiding();
                Vec3 front = carrier.position().add(carrier.getLookAngle().multiply(1, 0, 1).normalize().scale(.8));
                e.teleportTo(front.x, carrier.getY(), front.z);
            }
        }
    }

    /** The carrier walks more slowly. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || event.getEntity().tickCount % 5 != 0) {
            return;
        }
        Player p = event.getEntity();
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) {
            return;
        }
        boolean carrying = p.getFirstPassenger() instanceof Player;
        if (carrying && !a.hasModifier(SLOW)) {
            a.addTransientModifier(new AttributeModifier(SLOW, -.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!carrying && a.hasModifier(SLOW)) {
            a.removeModifier(SLOW);
        }
    }

    // ---------------------------------------------------------------- /porter

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        int level = RpConfig.get(RpConfig.HORROR_PERMISSION, 2);
        event.getDispatcher().register(Commands.literal("porter")
                .then(Commands.literal("lacher").executes(c -> {
                    setDown(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.argument("porteur", EntityArgument.player()).requires(s -> s.hasPermission(level))
                        .then(Commands.argument("porte", EntityArgument.player()).executes(c -> {
                            lift(EntityArgument.getPlayer(c, "porteur"), EntityArgument.getPlayer(c, "porte"));
                            return 1;
                        }))));
    }
}
