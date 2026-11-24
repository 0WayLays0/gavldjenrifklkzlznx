package com.waylays.arcanes.piege.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.piege.Pieges;
import com.waylays.arcanes.piege.TrapBlock;
import com.waylays.arcanes.piege.TrapBlockEntity;
import com.waylays.arcanes.piege.TrapKind;
import com.waylays.arcanes.piege.TrapSetup;
import com.waylays.arcanes.rp.arms.Proc;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Matrix4f;

/** The traps on the client: the saw trap's blade turning, and what /piege set on a trap, under its name. */
public final class PiegeClient {
    private PiegeClient() {}

    @EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Pieges.TYPE.get(), Saw::new);
        }
    }

    @EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
    public static final class Tooltips {
        private Tooltips() {}

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            TrapKind kind = Pieges.kindOf(event.getItemStack());
            if (kind == null) {
                return;
            }
            TrapSetup s = event.getItemStack().getOrDefault(Pieges.SETUP.get(), TrapSetup.NONE);
            event.getToolTip().add(Component.translatable("gui.arcanes_oublies.piege.tooltip.damage", Pieges.number(s.damageFor(kind)))
                    .withStyle(ChatFormatting.GRAY));
            for (Proc p : s.effects()) {
                event.getToolTip().add(Pieges.line(p));
            }
        }
    }

    /** The saw trap's blade, turning in its slot, half sunk in its base. */
    static final class Saw implements BlockEntityRenderer<TrapBlockEntity> {
        private static final ResourceLocation BLADE = ArcanesOublies.id("textures/block/trap_rust_blade.png");

        Saw(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(TrapBlockEntity trap, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
            BlockState state = trap.getBlockState();
            if (!(state.getBlock() instanceof TrapBlock block) || block.kind() != TrapKind.SAW || trap.getLevel() == null) {
                return;
            }
            float turn = (trap.getLevel().getGameTime() + partialTick) * .55f;
            Direction facing = state.getValue(TrapBlock.FACING);
            poseStack.pushPose();
            poseStack.translate(.5, 4 / 16f, .5);
            // The blade turns in the plane along the slot: north to south as the block faces north
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            poseStack.mulPose(Axis.XP.rotation(turn));
            Matrix4f m = poseStack.last().pose();
            VertexConsumer c = buffer.getBuffer(RenderType.entityCutoutNoCull(BLADE));
            float h = 6 / 16f;
            vertex(c, m, poseStack, 0, -h, -h, 0, 1, light);
            vertex(c, m, poseStack, 0, -h, h, 1, 1, light);
            vertex(c, m, poseStack, 0, h, h, 1, 0, light);
            vertex(c, m, poseStack, 0, h, -h, 0, 0, light);
            poseStack.popPose();
        }

        private static void vertex(VertexConsumer c, Matrix4f m, PoseStack poseStack, float x, float y, float z, float u, float v, int light) {
            c.addVertex(m, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(poseStack.last(), 1, 0, 0);
        }
    }
}
