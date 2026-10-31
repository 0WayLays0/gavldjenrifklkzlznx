package renderlab;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.BubbleModel;
import com.waylays.arcanes.client.render.water.GeyserModel;
import com.waylays.arcanes.client.render.water.RainCloudModel;
import com.waylays.arcanes.client.render.water.SplashModel;
import com.waylays.arcanes.client.render.water.TideLanceModel;
import com.waylays.arcanes.client.render.water.MaelstromModel;
import com.waylays.arcanes.client.render.water.TidalWaveModel;
import com.waylays.arcanes.client.render.water.TyphoonModel;
import com.waylays.arcanes.client.render.water.WaterJetModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The scenes the render lab can draw: a spell's model, a caster and a target or two for scale. */
public final class Scenes {
    private Scenes() {}

    public record Ctx(Recorder rec, Quaternionf camera, Vector3f eye) {}

    private static final int LIGHT = 0xF000F0;

    static void caster(Ctx ctx, float x, float z) {
        ctx.rec().box(x - .3, 0, z - .3, x + .3, 1.8, z + .3, 0x6A4A8A);
    }

    static void husk(Ctx ctx, float x, float y, float z) {
        ctx.rec().box(x - .3, y, z - .3, x + .3, y + 1.95, z + .3, 0x8A7A52);
    }

    public static void draw(String scene, float age, Ctx ctx) {
        switch (scene) {
            case "jet" -> {
                caster(ctx, 0, 0);
                husk(ctx, 0, 0, 9);
                Vector3f o = new Vector3f();
                PoseStack ps = Lab.at(o);
                WaterJetModel.draw(ps, ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 1.32f, .9f), new Vector3f(0, 1.25f, 8.7f), true, age, 17, LIGHT);
            }
            case "geyser" -> {
                caster(ctx, 0, 0);
                husk(ctx, 0, 3.5f, 6);
                Vector3f o = new Vector3f(0, 0, 6);
                // WARN 14, RISE 4, SPOUT 36, FALL 10 (Geyser)
                float warn = Math.min(1, age / 14), rise = Math.min(1, Math.max(0, (age - 14) / 4));
                float end = Math.min(1, Math.max(0, (age - 54) / 10));
                float column = age < 14 ? 0 : rise * (1 - end);
                GeyserModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, warn, column, end, 9, 1.8f, 23, LIGHT);
            }
            case "wave" -> {
                caster(ctx, 0, 0);
                // RISE 10, BREAK 14, run 44, SPEED .5 (TidalWave)
                float z = 2.5f + .5f * Math.min(age, 44);
                husk(ctx, 1, 1.2f, z + .9f);
                float up = Math.min(1, age / 10), down = Math.min(1, Math.max(0, (age - 44) / 14));
                float stand = (1 - (1 - up) * (1 - up)) * (1 - down * down);
                Vector3f o = new Vector3f(0, 0, z);
                TidalWaveModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, stand, down, 13, 4.6f, 41, LIGHT);
            }
            case "typhoon" -> {
                caster(ctx, 0, 0);
                husk(ctx, .5f, 4, 7);
                // FORM 24, BREAK 20, life 200 (Typhoon)
                float formed = Math.min(1, age / 24), gone = Math.min(1, Math.max(0, (age - 200) / 20));
                int cycle = (int) (age / 37);
                float flash = age - cycle * 37 < 3 ? 1 : 0;
                Vector3f o = new Vector3f(0, 0, 7);
                TyphoonModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, formed, gone, flash, cycle, 16, 3.6f, 13, LIGHT);
            }
            case "maelstrom" -> {
                caster(ctx, 0, 0);
                husk(ctx, 2, .3f, 9);
                float in = Math.min(1, age / 16);
                float opened = 1 - (1 - in) * (1 - in);
                Vector3f o = new Vector3f(0, 0, 8);
                MaelstromModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, opened, 5.5f, 7, LIGHT);
            }
            case "lance" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f(0, 1.5f, 5);
                TideLanceModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 5, LIGHT);
            }
            case "splash" -> {
                Vector3f o = new Vector3f(0, 0, 4);
                SplashModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age / 16, 2, 9, LIGHT);
            }
            case "rain" -> {
                caster(ctx, 0, 4);
                Vector3f o = new Vector3f(0, 0, 4);
                RainCloudModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, Math.min(1, age / 16), 5, 4.6f, 3, LIGHT);
            }
            case "bubble" -> {
                husk(ctx, 0, 1.2f, 4);
                Vector3f o = new Vector3f(0, 2.2f, 4);
                BubbleModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 1.3f, 0, Math.max(0, (age - 60) / 8), 3, LIGHT);
            }
            default -> throw new IllegalArgumentException("no scene " + scene);
        }
    }
}
