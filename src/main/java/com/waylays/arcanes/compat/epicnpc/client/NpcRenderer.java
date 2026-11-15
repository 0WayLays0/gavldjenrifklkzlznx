package com.waylays.arcanes.compat.epicnpc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.compat.epicnpc.Cnpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Epic Fight's renderer for Custom NPCs' NPCs. It draws the NPC's Epic Fight body with Custom NPCs' texture, like
 * Epic Fight's player renderer, but picks the body that fits the skin or the mob the NPC is disguised as (see
 * {@link EpicNpcClient}), hides the skin layers hidden in Custom NPCs' model menu, keeps the size set in its display
 * menu, and draws the weapon and armour of disguised NPCs (Custom NPCs then draws them through a layer Epic Fight
 * does not know).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class NpcRenderer extends PHumanoidRenderer<LivingEntity, LivingEntityPatch<LivingEntity>, HumanoidModel<LivingEntity>,
        LivingEntityRenderer<LivingEntity, HumanoidModel<LivingEntity>>, HumanoidMesh> {
    private final PatchedLayer heldItems = new PatchedItemInHandLayer<>();
    private final PatchedLayer armour;
    private boolean warned;

    public NpcRenderer(EntityRendererProvider.Context context, EntityType<?> type) {
        super(Meshes.BIPED, context, type);
        this.armour = new WearableItemLayer<>(Meshes.BIPED, false, context.getModelManager());
        this.addCustomLayer(new DisguiseLayer());
    }

    @Override
    public AssetAccessor<HumanoidMesh> getMeshProvider(LivingEntityPatch<LivingEntity> patch) {
        return EpicNpcClient.meshOf(patch.getOriginal());
    }

    @Override
    protected void prepareModel(HumanoidMesh mesh, LivingEntity entity, LivingEntityPatch<LivingEntity> patch,
            LivingEntityRenderer<LivingEntity, HumanoidModel<LivingEntity>> renderer) {
        super.prepareModel(mesh, entity, patch, renderer);
        if (renderer.getModel() instanceof PlayerModel<?> model && Cnpc.disguise(entity) == null) {
            // The parts hidden in Custom NPCs' model menu, as it hid them on its own model before drawing
            if (mesh.head != null) mesh.head.setHidden(!model.head.visible);
            if (mesh.hat != null) mesh.hat.setHidden(!model.hat.visible);
            if (mesh.torso != null) mesh.torso.setHidden(!model.body.visible);
            if (mesh.jacket != null) mesh.jacket.setHidden(!model.jacket.visible);
            if (mesh.leftArm != null) mesh.leftArm.setHidden(!model.leftArm.visible);
            if (mesh.leftSleeve != null) mesh.leftSleeve.setHidden(!model.leftSleeve.visible);
            if (mesh.rightArm != null) mesh.rightArm.setHidden(!model.rightArm.visible);
            if (mesh.rightSleeve != null) mesh.rightSleeve.setHidden(!model.rightSleeve.visible);
            if (mesh.leftLeg != null) mesh.leftLeg.setHidden(!model.leftLeg.visible);
            if (mesh.leftPants != null) mesh.leftPants.setHidden(!model.leftPants.visible);
            if (mesh.rightLeg != null) mesh.rightLeg.setHidden(!model.rightLeg.visible);
            if (mesh.rightPants != null) mesh.rightPants.setHidden(!model.rightPants.visible);
        }
    }

    @Override
    public void mulPoseStack(PoseStack poseStack, Armature armature, LivingEntity entity, LivingEntityPatch<LivingEntity> patch, float partialTicks) {
        super.mulPoseStack(poseStack, armature, entity, patch, partialTicks);
        int size = Cnpc.size(entity);
        if (size > 0 && size != 5) {
            float scale = size / 5.0F;
            poseStack.scale(scale, scale, scale);
        }
    }

    /** The weapon and armour of a disguised NPC. */
    private final class DisguiseLayer extends PatchedLayer<LivingEntity, LivingEntityPatch<LivingEntity>, HumanoidModel<LivingEntity>,
            RenderLayer<LivingEntity, HumanoidModel<LivingEntity>>> {
        @Override
        protected void renderLayer(LivingEntityPatch<LivingEntity> patch, LivingEntity entity, RenderLayer<LivingEntity, HumanoidModel<LivingEntity>> vanillaLayer,
                PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
            if (Cnpc.disguise(entity) == null) {
                return;
            }
            try {
                NpcRenderer.this.heldItems.renderLayer(entity, patch, null, poseStack, buffer, packedLight, poses, bob, yRot, xRot, partialTicks);
                Object layer = Cnpc.publicField(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity), "armorLayer");
                if (layer instanceof HumanoidArmorLayer armorLayer) {
                    NpcRenderer.this.armour.renderLayer(entity, patch, armorLayer, poseStack, buffer, packedLight, poses, bob, yRot, xRot, partialTicks);
                }
            } catch (RuntimeException ex) {
                if (!NpcRenderer.this.warned) {
                    NpcRenderer.this.warned = true;
                    ArcanesOublies.LOGGER.warn("The weapon or armour of a disguised NPC could not be drawn with Epic Fight: {}", ex.toString());
                }
            }
        }
    }
}
