package com.waylays.arcanes.compat.epicnpc.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.arcanes.compat.epicnpc.Cnpc;
import com.waylays.arcanes.compat.epicnpc.EpicNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How Custom NPCs' NPCs look with Epic Fight (client only, and only when both mods are there).
 * <ul>
 * <li>A person-shaped NPC is drawn by {@link NpcRenderer} with the Epic Fight body that fits its skin, read from the
 * skin itself: the old 64x32 layout, slim arms ("Alex" NPCs) or wide arms.</li>
 * <li>An NPC disguised as a mob built like a person (zombie, husk, drowned, zombie villager, illagers, witch) is drawn
 * with that mob's Epic Fight body, so the mob's texture fits it, and keeps its animations and its weapon.</li>
 * <li>Every other NPC (switched to Custom NPCs' combat in its edit screen, disguised as another mob, or drawn with
 * "Basic Limited Renderer"), and the stand-in mobs Custom NPCs draws disguised NPCs with, keep Custom NPCs' own look
 * and swing, with the weapon and armour the NPC has now (see {@link EpicNpc#fightsWithEpicFight}).</li>
 * </ul>
 * Custom NPCs' arm swing is hidden on NPCs Epic Fight draws: the attack animation is their swing.
 */
public final class EpicNpcClient {
    /** The stand-in mobs Custom NPCs draws disguised NPCs with (held weakly: Custom NPCs owns them). */
    private static final Set<Entity> STAND_INS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    /** Skins known to be in the old 64x32 layout (true) or not (false). */
    private static final Map<ResourceLocation, Boolean> OLD_LAYOUT = new ConcurrentHashMap<>();

    private EpicNpcClient() {}

    public static void init() {
        EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(EpicNpcClient::addRenderers);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, EpicNpcClient::onTick);
        NpcScreenSwitch.init();
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
        return EpicNpc.isNpc(e) && !EpicNpc.fightsWithEpicFight(e);
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
        return skinMesh(npc);
    }

    /**
     * The body for the NPC's skin. The skin's own size decides when it can be read (a 64x32 skin is in the old layout,
     * whatever kind of NPC wears it); otherwise the kind of NPC does: slim arms for "Alex" NPCs, the old layout for the
     * 64x32 and classic ones.
     */
    static AssetAccessor<HumanoidMesh> skinMesh(LivingEntity npc) {
        Boolean old = oldLayout(npc);
        if (Boolean.TRUE.equals(old)) {
            return Meshes.BIPED_OLD_TEX;
        }
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType()).getPath();
        if (path.equals("customnpcalex")) {
            return Meshes.ALEX;
        }
        if (old == null && (path.equals("customnpc64x32") || path.equals("customnpcclassic"))) {
            return Meshes.BIPED_OLD_TEX;
        }
        return Meshes.BIPED;
    }

    /** Whether the NPC's skin is in the old 64x32 layout (half as tall as wide), or null if it cannot be read yet. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Boolean oldLayout(LivingEntity npc) {
        ResourceLocation skin;
        try {
            EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(npc);
            if (!(renderer instanceof LivingEntityRenderer living)) {
                return null;
            }
            skin = living.getTextureLocation(npc);
        } catch (RuntimeException ex) {
            return null;
        }
        if (skin == null) {
            return null;
        }
        Boolean known = OLD_LAYOUT.get(skin);
        if (known != null) {
            return known;
        }
        // A player's skin: Minecraft always turns it into the 64x64 layout
        if (skin.getNamespace().equals("minecraft") && skin.getPath().startsWith("skins/")) {
            return false;
        }
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(skin, null);
        if (texture instanceof DynamicTexture dynamic) {
            NativeImage pixels = dynamic.getPixels();
            // A downloaded skin may still change: not remembered
            return pixels == null ? null : pixels.getHeight() * 2 == pixels.getWidth();
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(skin);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream in = resource.get().open()) {
            byte[] head = in.readNBytes(24);
            if (head.length < 24 || head[1] != 'P' || head[2] != 'N' || head[3] != 'G') {
                return null;
            }
            int width = ByteBuffer.wrap(head, 16, 4).getInt();
            int height = ByteBuffer.wrap(head, 20, 4).getInt();
            boolean old = height * 2 == width;
            OLD_LAYOUT.put(skin, old);
            return old;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * The body of a mob an NPC is disguised as, when it is built like a person on Epic Fight's human skeleton (the
     * NPC's own), so the mob's texture fits it; null for any other mob.
     */
    @SuppressWarnings("unchecked")
    static AssetAccessor<HumanoidMesh> disguiseMesh(LivingEntity standIn) {
        EntityType<?> type = standIn.getType();
        if (EpicNpc.isNpc(standIn)) {
            return skinMesh(standIn);
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
