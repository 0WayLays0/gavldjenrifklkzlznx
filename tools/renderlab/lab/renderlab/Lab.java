package renderlab;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.WaterMesh;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Path;

/**
 * Render lab: draws one scene (a spell's model at a given age) into a quad list for raster.py.
 * {@code java renderlab.Lab <scene> <age> <eye x,y,z> <at x,y,z> <out.txt>}
 */
public final class Lab {
    private Lab() {}

    public static void main(String[] args) throws Exception {
        String scene = args[0];
        float age = Float.parseFloat(args[1]);
        Vector3f eye = vec(args[2]);
        Vector3f at = vec(args[3]);
        Recorder rec = new Recorder();
        Vector3f d = new Vector3f(at).sub(eye).normalize();
        float yaw = (float) Math.atan2(-d.x, d.z);
        float pitch = (float) Math.asin(-d.y);
        Quaternionf camera = new Quaternionf().rotationYXZ((float) Math.PI - yaw, -pitch, 0);
        Scenes.draw(scene, age, new Scenes.Ctx(rec, camera, eye));
        rec.save(Path.of(args[4]));
    }

    static Vector3f vec(String s) {
        String[] p = s.split(",");
        return new Vector3f(Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]));
    }

    /** A fresh pose stack translated to {@code origin} (where an entity stands), and the view from there. */
    static PoseStack at(Vector3f origin) {
        PoseStack ps = new PoseStack();
        ps.translate(origin.x, origin.y, origin.z);
        return ps;
    }

    static WaterMesh.View view(Scenes.Ctx ctx, Vector3f origin) {
        return new WaterMesh.View(ctx.camera(), new Vector3f(ctx.eye()).sub(origin));
    }
}
