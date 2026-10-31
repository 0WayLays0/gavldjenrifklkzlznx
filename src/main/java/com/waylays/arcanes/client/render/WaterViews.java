package com.waylays.arcanes.client.render;

import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** The camera as the Water Magic models need it: seen from where a renderer draws them. */
public final class WaterViews {
    private WaterViews() {}

    /**
     * The view from a model drawn at {@code entity}'s place (as it is drawn this frame) shifted by {@code offset}, in a
     * frame turned by {@code frame} (the rotation the renderer applied before drawing it), if any.
     */
    public static WaterMesh.View of(EntityRenderDispatcher dispatcher, Entity entity, float partialTicks, Vec3 offset, @Nullable Quaternionf frame) {
        Vec3 cam = dispatcher.camera.getPosition();
        Vec3 at = entity.getPosition(partialTicks).add(offset);
        Vector3f eye = new Vector3f((float) (cam.x - at.x), (float) (cam.y - at.y), (float) (cam.z - at.z));
        Quaternionf camera = new Quaternionf(dispatcher.cameraOrientation());
        if (frame != null) {
            Quaternionf undo = new Quaternionf(frame).conjugate();
            undo.transform(eye);
            camera = undo.mul(camera);
        }
        return new WaterMesh.View(camera, eye);
    }

    public static WaterMesh.View of(EntityRenderDispatcher dispatcher, Entity entity, float partialTicks) {
        return of(dispatcher, entity, partialTicks, Vec3.ZERO, null);
    }
}
