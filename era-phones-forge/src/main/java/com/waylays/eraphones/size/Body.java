package com.waylays.eraphones.size;

import com.mojang.brigadier.arguments.FloatArgumentType;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.Collection;

/**
 * What a game master sets on a player's body and that stays through death: their speed (/vitesse &lt;players&gt;
 * &lt;times&gt;, 1 the usual, or normale), their health (/hp &lt;players&gt; &lt;points&gt;, 20 the usual, or normal), and
 * the effects they carry (given by /effect or otherwise) when they die, given back when they come back with what
 * was left of them.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Body {
    private static final ResourceLocation SPEED = EraPhones.id("vitesse"), HEALTH = EraPhones.id("hp");
    private static final String SPEED_KEY = EraPhones.MODID + "_vitesse", HEALTH_KEY = EraPhones.MODID + "_hp", EFFECTS_KEY = EraPhones.MODID + "_effets";

    private Body() {}

    public static float speed(Player p) {
        float v = p.getPersistentData().getFloat(SPEED_KEY);
        return v <= 0 ? 1 : v;
    }

    public static int health(Player p) {
        int v = p.getPersistentData().getInt(HEALTH_KEY);
        return v <= 0 ? 20 : v;
    }

    public static void setSpeed(Player p, float times) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null) {
            a.removeModifier(SPEED);
            if (Math.abs(times - 1) > 1e-3) {
                a.addPermanentModifier(new AttributeModifier(SPEED, times - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (Math.abs(times - 1) > 1e-3) {
            p.getPersistentData().putFloat(SPEED_KEY, times);
        } else {
            p.getPersistentData().remove(SPEED_KEY);
        }
    }

    public static void setHealth(Player p, int points) {
        AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a != null) {
            a.removeModifier(HEALTH);
            if (points != 20) {
                a.addPermanentModifier(new AttributeModifier(HEALTH, points - 20, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        if (points != 20) {
            p.getPersistentData().putInt(HEALTH_KEY, points);
        } else {
            p.getPersistentData().remove(HEALTH_KEY);
        }
        p.setHealth(p.getMaxHealth());
    }

    /** The effects carried at the moment of death, written down to be given back. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ListTag list = new ListTag();
        for (MobEffectInstance e : p.getActiveEffects()) {
            list.add(e.save());
        }
        if (list.isEmpty()) {
            p.getPersistentData().remove(EFFECTS_KEY);
        } else {
            p.getPersistentData().put(EFFECTS_KEY, list);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player was = event.getOriginal(), now = event.getEntity();
        if (speed(was) != 1) {
            setSpeed(now, speed(was));
        }
        if (health(was) != 20) {
            float h = now.getHealth();
            setHealth(now, health(was));
            if (!event.isWasDeath()) {
                // Back from the End: as hurt as they were
                now.setHealth(Math.min(h, now.getMaxHealth()));
            }
        }
        if (event.isWasDeath() && was.getPersistentData().contains(EFFECTS_KEY)) {
            for (Tag t : was.getPersistentData().getList(EFFECTS_KEY, Tag.TAG_COMPOUND)) {
                MobEffectInstance e = MobEffectInstance.load((CompoundTag) t);
                if (e != null) {
                    now.addEffect(e);
                }
            }
        }
        now.getPersistentData().remove(EFFECTS_KEY);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player p = event.getEntity();
        if (speed(p) != 1) {
            setSpeed(p, speed(p));
        }
        if (health(p) != 20) {
            float h = p.getHealth();
            setHealth(p, health(p));
            p.setHealth(Math.min(h, p.getMaxHealth()));
        }
    }

    // ---------------------------------------------------------------- /vitesse, /hp

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        int level = RpConfig.get(RpConfig.HORROR_PERMISSION, 2);
        event.getDispatcher().register(Commands.literal("vitesse").requires(s -> s.hasPermission(level))
                .then(Commands.argument("joueurs", EntityArgument.players())
                        .then(Commands.literal("normale").executes(c -> speed(c, 1)))
                        .then(Commands.argument("fois", FloatArgumentType.floatArg(0, 10)).executes(c -> speed(c, FloatArgumentType.getFloat(c, "fois"))))));
        event.getDispatcher().register(Commands.literal("hp").requires(s -> s.hasPermission(level))
                .then(Commands.argument("joueurs", EntityArgument.players())
                        .then(Commands.literal("normal").executes(c -> health(c, 20)))
                        .then(Commands.argument("points", IntegerArgumentType.integer(1, 1024)).executes(c -> health(c, IntegerArgumentType.getInteger(c, "points"))))));
    }

    private static int speed(CommandContext<CommandSourceStack> c, float times) throws CommandSyntaxException {
        Collection<ServerPlayer> who = EntityArgument.getPlayers(c, "joueurs");
        for (ServerPlayer p : who) {
            setSpeed(p, times);
            c.getSource().sendSuccess(() -> Component.translatable("body.eraphones.speed", RpService.name(p), String.format("%.2f", times))
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return who.size();
    }

    private static int health(CommandContext<CommandSourceStack> c, int points) throws CommandSyntaxException {
        Collection<ServerPlayer> who = EntityArgument.getPlayers(c, "joueurs");
        for (ServerPlayer p : who) {
            setHealth(p, points);
            c.getSource().sendSuccess(() -> Component.translatable("body.eraphones.health", RpService.name(p), points).withStyle(ChatFormatting.GRAY), true);
        }
        return who.size();
    }
}
