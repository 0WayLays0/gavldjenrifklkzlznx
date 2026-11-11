package com.waylays.eraphones.horror;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The fear gauge, 0 to 100, for each player who is not staff. It rises in the dark, alone, at night outside, hurt,
 * with something hostile near, and at once with whatever a game master sends them (a sound, a vision, the lights going
 * out); it falls in the light and in company. The player feels it (see HorrorClient): from a third of the way up the
 * heart beats, then faster, the edges of the sight darken, the breath comes short, the sight shakes and blurs.
 * The staff (who may use /horreur), and anyone in creative or spectator, feel nothing. A game master turns it on or
 * off for the whole server (/peur jauge on|off, kept with the world).
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Fear extends SavedData {
    private static final String NAME = EraPhones.MODID + "_peur";
    /** Each player's fear, 0 to 100, and what their screen was last told. */
    private static final Map<UUID, Float> LEVEL = new HashMap<>();
    private static final Map<UUID, Integer> SENT = new HashMap<>();
    private boolean enabled = true;

    private Fear() {}

    private static Fear data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Fear::new, Fear::load, null), NAME);
    }

    private static Fear load(CompoundTag tag, HolderLookup.Provider registries) {
        Fear f = new Fear();
        f.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        return f;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    public static boolean enabled(MinecraftServer server) {
        return data(server).enabled;
    }

    public static void setEnabled(MinecraftServer server, boolean on) {
        Fear f = data(server);
        f.enabled = on;
        f.setDirty();
        if (!on) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                set(p, 0);
            }
        }
    }

    /** Staff, creative and spectators never feel it. */
    public static boolean exempt(Player p) {
        return p.isCreative() || p.isSpectator() || p.hasPermissions(RpConfig.get(RpConfig.HORROR_PERMISSION, 2));
    }

    public static float get(Player p) {
        return LEVEL.getOrDefault(p.getUUID(), 0f);
    }

    public static void set(ServerPlayer p, float value) {
        LEVEL.put(p.getUUID(), Math.max(0, Math.min(100, value)));
        sync(p);
    }

    /** A sudden fright: {@code amount} more at once (nothing if the gauge is off or the player is staff). */
    public static void scare(ServerPlayer p, float amount) {
        if (!enabled(p.server) || exempt(p)) {
            return;
        }
        set(p, get(p) + amount);
    }

    /** How much a game master's effect frightens. */
    static float fright(String kind) {
        return switch (kind) {
            case "coupure", "sang" -> 25;
            case "peur", "tremblement" -> 20;
            case "murmure", "ballon", "yeux" -> 15;
            case "brouillard", "narration" -> 8;
            default -> 0;
        };
    }

    private static void sync(ServerPlayer p) {
        int v = Math.round(get(p));
        Integer last = SENT.get(p.getUUID());
        if (last == null || Math.abs(last - v) >= 1) {
            SENT.put(p.getUUID(), v);
            PacketDistributor.sendToPlayer(p, new HorrorNet.Gauge(v));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        boolean on = enabled(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!on || exempt(p) || !p.isAlive()) {
                if (get(p) > 0 || !SENT.containsKey(p.getUUID())) {
                    set(p, 0);
                }
                continue;
            }
            set(p, get(p) + rate(p) * .5f);
        }
    }

    /** How fast fear grows (or, below 0, fades) for {@code p} now, per second. */
    static float rate(ServerPlayer p) {
        float r = -.6f;
        BlockPos eyes = BlockPos.containing(p.getEyePosition());
        int light = p.level().getMaxLocalRawBrightness(eyes);
        if (holdsLight(p)) {
            light = Math.max(light, 10);
        }
        if (light <= 3) {
            r += 2.2f;
        } else if (light <= 7) {
            r += .9f;
        } else {
            r -= .8f;
        }
        if (p.level().isNight() && p.level().canSeeSky(eyes)) {
            r += .4f;
        }
        // Company: other players near (not staff watching), each calms a little
        int near = 0;
        for (Player o : p.level().players()) {
            if (o != p && !o.isSpectator() && o.distanceToSqr(p) < 10 * 10) {
                near++;
            }
        }
        r += near == 0 ? .5f : -.7f * Math.min(near, 3);
        if (p.getHealth() < p.getMaxHealth() * .35f) {
            r += 1f;
        }
        int hostile = p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(8), m -> m instanceof Enemy && m.isAlive()).size();
        r += Math.min(3, hostile) * 1.2f;
        return r;
    }

    private static boolean holdsLight(ServerPlayer p) {
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof com.waylays.eraphones.gear.FlashlightItem && s.getOrDefault(GearRegistry.POWER.get(), false)) {
                return true;
            }
            if (s.is(net.minecraft.world.item.Items.TORCH) || s.is(net.minecraft.world.item.Items.LANTERN) || s.is(net.minecraft.world.item.Items.SOUL_TORCH)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getAmount() > 0) {
            scare(p, 6);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LEVEL.remove(event.getEntity().getUUID());
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            set(p, 0);
        }
    }
}
