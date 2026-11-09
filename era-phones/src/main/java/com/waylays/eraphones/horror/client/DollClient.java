package com.waylays.eraphones.horror.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.horror.block.DollBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The dolls on the players' side: their heads turning, and how they are drawn. */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class DollClient {
    private DollClient() {}

    /** Turns the head toward the nearest player, but only while the one playing here does not look at the doll. */
    public static void tick(DollBlockEntity doll) {
        doll.lastYaw = doll.yaw;
        doll.lastPitch = doll.pitch;
        Minecraft mc = Minecraft.getInstance();
        if (doll.getLevel() == null || mc.player == null) {
            return;
        }
        Vec3 head = Vec3.atBottomCenterOf(doll.getBlockPos()).add(0, .78, 0);
        Player nearest = doll.getLevel().getNearestPlayer(head.x, head.y, head.z, 16, false);
        if (nearest == null) {
            return;
        }
        Vec3 to = head.subtract(mc.player.getEyePosition());
        if (to.length() < 32 && mc.player.getViewVector(1).dot(to.normalize()) > .55) {
            return; // Seen: it keeps still
        }
        Vec3 d = nearest.getEyePosition().subtract(head);
        float facing = doll.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot();
        float world = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float target = Mth.clamp(Mth.wrapDegrees(world - facing), -120, 120);
        float targetPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))), -30, 30);
        doll.yaw += Mth.wrapDegrees(target - doll.yaw) * .35f;
        doll.pitch += (targetPitch - doll.pitch) * .35f;
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GearRegistry.DOLL_BE.get(), c -> new DollRenderer());
        event.registerEntityRenderer(GearRegistry.WRITING.get(), WritingRenderer::new);
    }
}
