package com.waylays.arcanes.compat.epicnpc.client;

import com.waylays.arcanes.compat.epicnpc.Cnpc;
import com.waylays.arcanes.compat.epicnpc.EpicNpc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.registry.RegisterPatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.HumanoidMesh;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * How Custom NPCs' NPCs look with Epic Fight (client only, and only when both mods are there).
 * <ul>
 * <li>A person-shaped NPC is drawn by {@link NpcRenderer} with the Epic Fight body that fits its skin: wide arms,
 * slim arms ("Alex") or the old 64x32 skins.</li>
 * <li>An NPC disguised as a mob built like a person (zombie, husk, drowned, zombie villager, illagers, witch) is drawn
 * with that mob's Epic Fight body, so the mob's texture fits it, and keeps its animations and its weapon.</li>
 * <li>An NPC disguised as any other mob (animals, spiders, skeletons, piglins...), and the stand-in mob Custom NPCs draws
 * with "Basic Limited Renderer", keep Custom NPCs' own look, with the weapon and armour the NPC has now.</li>
 * </ul>
 * Custom NPCs' arm swing is also hidden on NPCs Epic Fight draws: the attack animation is their swing.
 */
public final class EpicNpcClient {
    /** The stand-in mobs Custom NPCs draws disguised NPCs with (held weakly: Custom NPCs owns them). */
    private static final Set<Entity> STAND_INS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private EpicNpcClient() {}

    public static void init() {
        EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(EpicNpcClient::addRenderers);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, EpicNpcClient::onTick);
    }

    private static void addRenderers(RegisterPatchedRenderersEvent.AddEntity event) {
        for (ResourceLocation id : EpicNpc.NPC_TYPES) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> event.addPatchedEntityRenderer(type, t -> new NpcRenderer(event.getContext(), t)));
        }
    }

    private static void onTick(EntityTickEvent.Pre event) {
        Entity e = event.getEntity();
        if (!e.level().isClientSide() || !EpicNpc.isNpc(e)) {
            return;
        }
        LivingEntity npc = (LivingEntity) e;
        LivingEntity standIn = Cnpc.disguise(npc);
        if (standIn != null) {
            STAND_INS.add(standIn);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack now = npc.getItemBySlot(slot);
                if (!ItemStack.matches(now, standIn.getItemBySlot(slot))) {
                    standIn.setItemSlot(slot, now.copy());
                }
            }
        }
        if (!vanillaLook(npc)) {
            npc.swinging = false;
            npc.swingTime = 0;
            npc.attackAnim = 0.0F;
            npc.oAttackAnim = 0.0F;
        }
    }

    /** Whether Epic Fight leaves this entity to its usual renderer (see the class comment). */
    public static boolean vanillaLook(Entity e) {
        if (STAND_INS.contains(e)) {
            return true;
        }
        if (!EpicNpc.isNpc(e)) {
            return false;
        }
        if (Cnpc.simpleRender(e)) {
            // Custom NPCs draws the stand-in mob itself and never asks for the NPC's own look
            return Cnpc.disguise(e) != null;
        }
        LivingEntity standIn = Cnpc.disguise(e);
        return standIn != null && disguiseMesh(standIn) == null;
    }

    /** The Epic Fight body the NPC is drawn with. */
    static AssetAccessor<HumanoidMesh> meshOf(LivingEntity npc) {
        LivingEntity standIn = Cnpc.disguise(npc);
        if (standIn != null) {
            AssetAccessor<HumanoidMesh> mesh = disguiseMesh(standIn);
            if (mesh != null) {
                return mesh;
            }
        }
        return skinMesh(npc.getType());
    }

    /** The body for the NPC's skin: slim arms for "Alex" NPCs, the old layout for 64x32 and classic skins. */
    static AssetAccessor<HumanoidMesh> skinMesh(EntityType<?> type) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        return switch (path) {
            case "customnpcalex" -> Meshes.ALEX;
            case "customnpc64x32", "customnpcclassic" -> Meshes.BIPED_OLD_TEX;
            default -> Meshes.BIPED;
        };
    }

    /**
     * The body of a mob an NPC is disguised as, when it is built like a person on Epic Fight's human skeleton (the
     * NPC's own), so the mob's texture fits it; null for any other mob.
     */
    @SuppressWarnings("unchecked")
    static AssetAccessor<HumanoidMesh> disguiseMesh(LivingEntity standIn) {
        EntityType<?> type = standIn.getType();
        if (EpicNpc.isNpc(standIn)) {
            return skinMesh(type);
        }
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED) {
            return Meshes.BIPED_OLD_TEX;
        }
        if (type == EntityType.ZOMBIE_VILLAGER) {
            return (AssetAccessor<HumanoidMesh>) (AssetAccessor<? extends HumanoidMesh>) Meshes.VILLAGER_ZOMBIE;
        }
        if (type == EntityType.VINDICATOR || type == EntityType.PILLAGER || type == EntityType.EVOKER || type == EntityType.ILLUSIONER) {
            return Meshes.ILLAGER;
        }
        if (type == EntityType.WITCH) {
            return (AssetAccessor<HumanoidMesh>) (AssetAccessor<? extends HumanoidMesh>) Meshes.WITCH;
        }
        return null;
    }
}
