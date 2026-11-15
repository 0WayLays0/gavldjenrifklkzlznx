package com.waylays.arcanes.debug;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.compat.epicnpc.EpicNpc;
import com.waylays.arcanes.compat.epicnpc.EpicNpcBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A check run by CI on a real dedicated server with Epic Fight and Custom NPCs (only with
 * {@code -Darcanes.epicnpccheck=true}; Custom NPCs does not start on Minecraft's test server): an NPC with a sword gets
 * an Epic Fight body; Custom NPCs' own blow does nothing and starts the attack animation instead, whose hit is the one
 * that lands (as Epic Fight's damage). It logs
 * {@code [EpicNpcCheck] PASS} or {@code [EpicNpcCheck] FAIL ...}, then stops the server.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class EpicNpcServerCheck {
    private static final boolean ENABLED = Boolean.getBoolean("arcanes.epicnpccheck");

    private static Mob npc;
    private static Husk target;
    private static float full;
    private static int tick = -1;

    private EpicNpcServerCheck() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!ENABLED) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (!EpicNpc.active()) {
            fail(server, "Epic Fight and Custom NPCs are not both loaded");
            return;
        }
        try {
            ServerLevel level = server.overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            level.getChunkAt(spawn);
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpc"));
            npc = (Mob) type.create(level);
            npc.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
            npc.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            level.addFreshEntity(npc);
            target = EntityType.HUSK.create(level);
            // In front of the NPC (it faces south), within its sword's reach
            target.moveTo(at.getX() + .5, at.getY(), at.getZ() + 1.8, 180, 0);
            target.setNoAi(true);
            target.setPersistenceRequired();
            level.addFreshEntity(target);
            npc.setTarget(target);
            full = target.getHealth();
            tick = 0;
            ArcanesOublies.LOGGER.info("[EpicNpcCheck] an NPC with a sword and a husk (it does not burn in the sun) stand at {}", at);
        } catch (Throwable t) {
            ArcanesOublies.LOGGER.error("[EpicNpcCheck] FAIL setting up", t);
            server.halt(false);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!ENABLED || tick < 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        tick++;
        try {
            if (tick == 20) {
                Object patch = EpicNpcBridge.patch(npc);
                if (patch == null) {
                    fail(server, "the NPC has no Epic Fight patch");
                    return;
                }
                ArcanesOublies.LOGGER.info("[EpicNpcCheck] the NPC's Epic Fight body: {}", patch.getClass().getName());
                npc.setTarget(target);
                npc.doHurtTarget(target);
                if (target.getHealth() != full) {
                    fail(server, "Custom NPCs' own blow landed (" + target.getHealth() + " / " + full + ")");
                }
            } else if (tick == 80) {
                if (target.getHealth() >= full || EpicNpcBridge.animationHits == 0) {
                    fail(server, "the attack animation's hit never landed (" + target.getHealth() + " / " + full + ", "
                            + EpicNpcBridge.animationHits + " animation hits)");
                    return;
                }
                ArcanesOublies.LOGGER.info("[EpicNpcCheck] PASS: the NPC has an Epic Fight body and only its attack animation hit ({} -> {}, {} hit(s))",
                        full, target.getHealth(), EpicNpcBridge.animationHits);
                tick = -1;
                server.halt(false);
            }
        } catch (Throwable t) {
            ArcanesOublies.LOGGER.error("[EpicNpcCheck] FAIL", t);
            tick = -1;
            server.halt(false);
        }
    }

    private static void fail(MinecraftServer server, String why) {
        ArcanesOublies.LOGGER.error("[EpicNpcCheck] FAIL {}", why);
        tick = -1;
        server.halt(false);
    }
}
