package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.clockwork.ClockMachineModels;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.clockwork.ClockModels;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.clockwork.ClockworkScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Clockwork Magic's machines, each drawn by its own model from where it stands. */
public class ClockworkSceneRenderer extends EntityRenderer<ClockworkScene> {
    public ClockworkSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ClockworkScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        float size = e.getSize();
        float since = e.sinceStruck(partialTicks);
        boolean struck = since < 1.0E6f && since < e.tickCount + 2;
        Vector3f target = struck ? e.getTarget() : null;
        Vector3f dir = e.getDir();
        int life = e.getLife();
        switch (e.getKind()) {
            case SAW -> ClockModels.saw(poseStack, buffer, view, dir, age, e.isDone(), since, seed, light);
            case RIVET -> ClockModels.rivet(poseStack, buffer, view, dir, age, e.isDone(), since, seed, light);
            case SPRING -> ClockModels.spring(poseStack, buffer, view, dir, age, life, since, e.isDone() ? target : null, ClockworkScene.SPRING_LAUNCH, seed, light);
            case TRAP -> ClockModels.trap(poseStack, buffer, view, age, life, struck, e.isDone(), since, ClockworkScene.TRAP_ARM, ClockworkScene.TRAP_SNAP,
                    ClockworkScene.TRAP_HOLD, seed, light);
            case TURRET -> ClockModels.turret(poseStack, buffer, view, dir, age, life, since, target, ClockworkScene.TURRET_RISE, seed, light);
            case GYRO -> ClockModels.gyro(poseStack, buffer, view, age, life, size, since, e.isDone(), ClockworkScene.GYRO_CLOSE, ClockworkScene.BURST, seed, light);
            case AUTOMATON -> {
                float speed = (float) Math.sqrt(Mth.square(e.getX() - e.xo) + Mth.square(e.getZ() - e.zo));
                float walking = Mth.clamp(speed / (float) ClockworkScene.AUTO_SPEED, 0, 1);
                ClockMachineModels.automaton(poseStack, buffer, view, dir, age, life, age * .45f, walking, struck ? since : -1, ClockworkScene.AUTO_RISE,
                        ClockworkScene.AUTO_WIND, seed, light);
            }
            case LOCOMOTIVE -> {
                float laid = ClockworkScene.LOCO_LENGTH * Mth.clamp(age / ClockworkScene.LOCO_RAILS, 0, 1) + 1;
                float front = ClockworkScene.trainAt(age);
                float visible = Mth.clamp((age - ClockworkScene.LOCO_RAILS + 6) / 6, 0, 1);
                float gone = age - ClockworkScene.LOCO_RAILS - ClockworkScene.LOCO_RUN;
                float speed = ClockworkScene.trainAt(age + 1) - front;
                ClockMachineModels.locomotive(poseStack, buffer, view, dir, age, life, laid, front, visible, gone, speed, seed, light);
            }
            case AIRSHIP -> ClockMachineModels.airship(poseStack, buffer, view, dir, age, life, ClockworkScene.shipAlong(age), ClockworkScene.SHIP_HEIGHT,
                    ClockworkScene.SHIP_BOMBS, ClockworkScene::bombDrop, i -> ClockworkScene.bombLands(i, dir), ClockworkScene.SHIP_FALL, ClockworkScene.SHIP_SPEED, seed,
                    light);
            case MECHANISM -> ClockMachineModels.mechanism(poseStack, buffer, view, age, life, size, since, target, ClockworkScene.MECH_RISE,
                    ClockworkScene.MECH_OVERLOAD, seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(ClockworkScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(ClockworkScene e) {
        return ClockMesh.BRASS;
    }
}
