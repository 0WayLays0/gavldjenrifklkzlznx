package renderlab;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.BrandModel;
import com.waylays.arcanes.client.render.fire.BreathModel;
import com.waylays.arcanes.client.render.fire.CrescentModel;
import com.waylays.arcanes.client.render.fire.EmberBoltModel;
import com.waylays.arcanes.client.render.fire.FlameRingModel;
import com.waylays.arcanes.client.render.fire.NovaModel;
import com.waylays.arcanes.client.render.fire.PhoenixModel;
import com.waylays.arcanes.client.render.fire.PyreModel;
import com.waylays.arcanes.client.render.fire.WispModel;
import com.waylays.arcanes.client.render.fire.FireBurstModel;
import com.waylays.arcanes.client.render.fire.FirePillarModel;
import com.waylays.arcanes.client.render.ritual.ColossusModel;
import com.waylays.arcanes.client.render.wind.BladeStormModel;
import com.waylays.arcanes.client.render.wind.CycloneModel;
import com.waylays.arcanes.client.render.wind.GaleModel;
import com.waylays.arcanes.client.render.wind.GustBladeModel;
import com.waylays.arcanes.client.render.wind.HurricaneModel;
import com.waylays.arcanes.client.render.wind.SkyfallModel;
import com.waylays.arcanes.client.render.wind.UpdraftModel;
import com.waylays.arcanes.client.render.wind.VacuumModel;
import com.waylays.arcanes.client.render.wind.WindWallModel;
import com.waylays.arcanes.client.render.wind.WindWardModel;
import com.waylays.arcanes.client.render.ritual.PortalModel;
import com.waylays.arcanes.client.render.ritual.RitualFigureModel;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.BubbleModel;
import com.waylays.arcanes.client.render.water.GeyserModel;
import com.waylays.arcanes.client.render.water.RainCloudModel;
import com.waylays.arcanes.client.render.water.SplashModel;
import com.waylays.arcanes.client.render.water.TideLanceModel;
import com.waylays.arcanes.client.render.water.MaelstromModel;
import com.waylays.arcanes.client.render.water.TidalWaveModel;
import com.waylays.arcanes.client.render.water.TyphoonModel;
import com.waylays.arcanes.client.render.water.WaterJetModel;
import com.waylays.arcanes.client.render.boss.*;
import com.waylays.arcanes.client.render.echo.*;
import com.waylays.arcanes.client.render.sand.*;
import com.waylays.arcanes.client.render.lotus.*;
import com.waylays.arcanes.client.render.gem.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.util.Mth;

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

    /** Gem scenes that need more than one call. */
    static final class GemModelsLab {
        static void vault(Ctx ctx, Vector3f o, float age) {
            LapisModels.vault(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 112, 5, 8, (age - 12) % 8, new Vector3f(2, 1, 1), 3, LIGHT);
        }
    }

    public static void draw(String scene, float age, Ctx ctx) {
        int tier = 0;
        if (scene.contains(":")) {
            tier = Integer.parseInt(scene.substring(scene.indexOf(':') + 1));
            scene = scene.substring(0, scene.indexOf(':'));
        }
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
            case "fbolt" -> {
                // Six bolts side by side, one of each heat
                for (int t = 0; t < 6; t++) {
                    Vector3f o = new Vector3f(-5 + t * 2, 1.5f, 5);
                    EmberBoltModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, t, t * 7, LIGHT);
                }
            }
            case "fburst" -> {
                Vector3f o = new Vector3f(0, 0, 5);
                FireBurstModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age / 20, 1.6f, tier, 3, LIGHT);
            }
            case "fpillar" -> {
                caster(ctx, 0, 0);
                for (int t = 0; t < 6; t++) {
                    if (tier >= 0 && t != tier && scene.equals("fpillar") && tier < 6) {
                        continue;
                    }
                }
                Vector3f o = new Vector3f(0, 0, 5);
                float warn = Math.min(1, age / 12), rise = Math.min(1, Math.max(0, (age - 12) / 5)), end = Math.min(1, Math.max(0, (age - 47) / 10));
                FirePillarModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, warn, rise, end, 7, 1.1f, tier, 5, LIGHT);
            }
            case "fpillars" -> {
                for (int t = 0; t < 6; t++) {
                    Vector3f o = new Vector3f(-7.5f + t * 3, 0, 8);
                    FirePillarModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 1, 1, 0, 6, 1, t, t * 5, LIGHT);
                }
            }
            case "fbreath" -> {
                caster(ctx, 0, 0);
                husk(ctx, 0, 0, 6);
                Vector3f o = new Vector3f();
                BreathModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 1.3f, .7f), new Vector3f(0, -.05f, 1).normalize(), 8, age, tier, 3, LIGHT);
            }
            case "fring" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f();
                FlameRingModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 1 + age * .35f, 1.6f, Math.max(0, (age - 18) / 8), tier, 3, LIGHT);
            }
            case "fwisp" -> {
                caster(ctx, 0, 0);
                for (int t = 0; t < 6; t++) {
                    Vector3f o = new Vector3f(-3.75f + t * 1.5f, 2.2f, 2.5f);
                    WispModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(1, 0, 0), age + t * 5, 0, t, t * 3, LIGHT);
                }
            }
            case "fcrescent" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f(0, 1.3f, 4);
                CrescentModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), 3.2f, .15f, age, 0, tier, 3, LIGHT);
            }
            case "fbrand" -> {
                husk(ctx, 0, 0, 3);
                Vector3f o = new Vector3f(0, 2.6f, 3);
                BrandModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, Math.min(1, age / 60), 2.6f, tier, 3, LIGHT);
            }
            case "fnova" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f();
                NovaModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age / 24, 3 + 1.5f * tier, tier, 3, LIGHT);
            }
            case "fphoenix" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f(0, 3, 6);
                PhoenixModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 0, 0, 1, tier, 3, LIGHT);
            }
            case "fpyre" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f();
                PyreModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, tier, Math.min(5, (int) (age / 40)), .5f, 1, 0, 6, 14, 3, LIGHT);
            }
            case "wblade" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f(0, 1.4f, 1 + age * .9f);
                GustBladeModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 2.4f, 1, 3, LIGHT);
            }
            case "wgale" -> {
                caster(ctx, 0, 0);
                husk(ctx, .5f, 0, 5);
                Vector3f o = new Vector3f(0, 1.3f, .6f);
                GaleModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age / 14, 8, 3, LIGHT);
            }
            case "wupdraft" -> {
                ctx.rec().box(-.3, Math.min(6, age * .4), 3.7, .3, Math.min(6, age * .4) + 1.8, 4.3, 0x6A4A8A);
                Vector3f o = new Vector3f(0, 0, 4);
                UpdraftModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, Math.min(1, age / 30), 6, 1.2f, 1, 3, LIGHT);
            }
            case "wward" -> {
                ctx.rec().box(-.3, 0, 2.7, .3, 1.8, 3.3, 0x6A4A8A);
                Vector3f o = new Vector3f(0, 0, 3);
                WindWardModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 1.8f, 1, 3, LIGHT);
            }
            case "wcyclone" -> {
                caster(ctx, 0, 0);
                husk(ctx, .8f, 1.5f, 7);
                Vector3f o = new Vector3f(0, 0, 7);
                CycloneModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, Math.min(1, age / 15), 0, 6.5f, 2.6f, 3, LIGHT);
            }
            case "wvacuum" -> {
                husk(ctx, 1.5f, 0, 8);
                Vector3f o = new Vector3f(0, 1.4f, 7);
                float pull = Math.min(1, age / 30);
                VacuumModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, pull, age < 30 ? -1 : Math.min(1, (age - 30) / 12), 7, 3, LIGHT);
            }
            case "wwall" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f(0, 0, 3);
                WindWallModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 7, 4, Math.min(1, age / 10), 3, LIGHT);
            }
            case "wblades" -> {
                caster(ctx, 0, 4);
                Vector3f o = new Vector3f(0, 0, 4);
                BladeStormModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 7, 3, 1, 3, LIGHT);
            }
            case "wskyfall" -> {
                float lift = Math.min(1, age / 30), slam = age < 30 ? -1 : Math.min(1, (age - 30) / 14);
                float y = slam < 0 ? 9 * lift : 9 * Math.max(0, 1 - slam * 4);
                husk(ctx, 0, y, 6);
                Vector3f o = new Vector3f(0, 0, 6);
                SkyfallModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, lift, slam, 9, 3, LIGHT);
            }
            case "whurricane" -> {
                caster(ctx, 0, 0);
                husk(ctx, 8, 2, 3);
                Vector3f o = new Vector3f();
                HurricaneModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, Math.min(1, age / 30), 0, 4, 12, 14, 3, LIGHT);
            }
            case "colossus" -> {
                // tier: 0 standing/walking, 1 striking (age since the blow), 2 falling apart (age since)
                husk(ctx, 0, 0, 7);
                Vector3f o = new Vector3f(0, 0, 4);
                int[] tint = {0x8C8C8C, 0x6E8A5A, 0xA0A0A0, 0x9A9A96, 0x4A4A50, 0x7E7A6A};
                ColossusModel.Stones sink = (ps, kind) -> {
                    var c = ctx.rec().getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(RitualMesh.MENHIR));
                    RitualMesh.block(c, ps.last(), new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1), 0, 0, 0, 1, 1, 1, .5f,
                            0xFF000000 | tint[kind], LIGHT);
                };
                float assembled = tier == 0 ? age : 200;
                ColossusModel.draw(Lab.at(o), ctx.rec(), sink, Lab.view(ctx, o), assembled, tier == 0 ? age * .3f : 0, tier == 0 && age > 80 ? 1 : 0,
                        tier == 1 ? age : 1000, tier == 2 ? age : -1, 9, LIGHT);
            }
            case "portal" -> {
                caster(ctx, 1.5f, 1);
                Vector3f o = new Vector3f(0, 0, 4);
                float open = Math.min(1, age / 30), closing = Math.max(0, Math.min(1, (age - 200) / 30));
                PortalModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, open, closing, 0xC27BFF, 5, LIGHT);
            }
            case "rite" -> {
                // tier: the figure. Age: drawn over 60, waits 40, channels 600, then cast
                int state;
                float since, drawn = Math.min(1, age / 60), progress = 0;
                if (age < 60) {
                    state = RitualFigureModel.DRAWING;
                    since = age;
                } else if (age < 100) {
                    state = RitualFigureModel.WAITING;
                    since = age - 60;
                } else if (age < 700) {
                    state = RitualFigureModel.CHANNELING;
                    since = age - 100;
                    progress = since / 600;
                } else {
                    state = RitualFigureModel.DONE;
                    since = age - 700;
                }
                int[] colours = {0xFF7A2E, 0x7FD8FF, 0x9FA8FF, 0xC27BFF, 0xFFE27A, 0xB8B0A0, 0xB27AFF};
                Vector3f o = new Vector3f(0, 0, 4);
                RitualMesh.Plane plane;
                Vector3f[] stations;
                if (tier == 3) {
                    ctx.rec().box(-5, 0, 4, 5, 6, 5, 0x8A8A8A);
                    o = new Vector3f(0, 0, 3.98f);
                    plane = RitualMesh.Plane.wall(0, -1);
                    stations = new Vector3f[] {new Vector3f(-1.5f, 0, -2.4f), new Vector3f(1.5f, 0, -2.4f)};
                } else {
                    plane = RitualMesh.Plane.ground(0);
                    stations = new Vector3f[] {new Vector3f(3.5f, 0, 0), new Vector3f(-3.5f, 0, 0)};
                    if (tier >= 5) {
                        ctx.rec().box(-.5, 0, 3.5, .5, 1, 4.5, 0x3E3C48);
                    }
                }
                int occupied = age >= 80 ? 3 : age >= 70 ? 1 : 0;
                for (int i = 0; i < 2; i++) {
                    if ((occupied & (1 << i)) != 0) {
                        ctx.rec().box(o.x + stations[i].x - .3, 0, o.z + stations[i].z - .3, o.x + stations[i].x + .3, 1.5, o.z + stations[i].z + .3, 0x6A4A8A);
                    }
                }
                RitualFigureModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), tier, plane, stations, occupied, state, since, drawn, progress, age,
                        colours[tier], 7, LIGHT);
            }
            case "bsword" -> {
                // A volley: each sword strikes at its own tick; age is the volley's clock
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 9);
                for (int k = 0; k < 6; k++) {
                    float a = -1.2f + k * .48f;
                    Vector3f halo = new Vector3f(Mth.sin(a) * 4.5f, 7.5f + .8f * Mth.cos(a * 2), -1.5f + Mth.cos(a) * .8f);
                    Vector3f o = new Vector3f(-5 + k * 2.4f, 0, 8 + (k % 2) * 2);
                    int strike = 70 + k * 3;
                    SwordModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(halo).sub(o), age, strike - age, 3.4f, 0xFFD25A, 11 + k * 7, LIGHT);
                }
            }
            case "bwheel" -> {
                // Forms at the head of a 24-block strip; rolls from 56 to 76; bursts after
                caster(ctx, -2, -1);
                husk(ctx, 1, 0, 14);
                Vector3f o = new Vector3f(0, 0, 0);
                float rolled = Math.max(0, Math.min(1, (age - 56) / 20));
                WheelModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 24), 2.6f, age, rolled, age - 76, 9, LIGHT);
            }
            case "beclipse" -> {
                // Cast at 0; the first band strikes at 70, then every 14
                caster(ctx, 0, 0);
                husk(ctx, 4, 0, 6);
                husk(ctx, -9, 0, 7);
                Vector3f o = new Vector3f(0, 0, 0);
                EclipseModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 22, 15.5f, 3.6f, age, age - 70, 14, 5, LIGHT);
            }
            case "bmoon" -> {
                // Shows at 0, falls over 120 ticks onto a 16-block arena, then the aftermath
                caster(ctx, -18, -4);
                husk(ctx, 5, 0, 4);
                Vector3f o = new Vector3f(0, 0, 0);
                float fall = Math.min(1, age / 120);
                MoonModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(6, 46, -8), 6, 16, age, fall, age - 120, 3, LIGHT);
            }
            case "bmaw" -> {
                // Opens at 0, bites at 70
                caster(ctx, -9, -6);
                husk(ctx, 1, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                MawModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 6, age, 70 - age, 4, LIGHT);
            }
            case "bprism" -> {
                // Forms at 0; 12 beams 30 degrees apart, 4 ticks apart, the first at 70
                caster(ctx, 0, 0);
                husk(ctx, 6, 0, 9);
                Vector3f o = new Vector3f(0, 0, 0);
                PrismModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 8, age, age - 70, 12, 1.2f, Mth.PI / 6, 4, 18, 7, LIGHT);
            }
            case "bchains" -> {
                // Bursts out at 0, the sentence falls at 70
                caster(ctx, -8, -6);
                husk(ctx, 0, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                ChainsModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 2.6f, age, 70 - age, 3, LIGHT);
            }
            case "bstorm" -> {
                // Gathers from 0; bolts strike at 70, 72 and 76; its wrath is spent at 110
                caster(ctx, 0, -2);
                husk(ctx, 4, 0, 6);
                Vector3f o = new Vector3f(0, 0, 0);
                StormModel.storm(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 20, 14, age, age - 110, 3, LIGHT);
                float[][] bolts = {{4, 6, 70}, {-5, 8, 72}, {7, -3, 76}};
                for (float[] b : bolts) {
                    Vector3f at = new Vector3f(b[0], 0, b[1]);
                    StormModel.bolt(Lab.at(at), ctx.rec(), Lab.view(ctx, at), new Vector3f(-b[0] * .3f, 18, -b[1] * .3f), age - b[2], (int) b[2], LIGHT);
                }
            }
            case "bthorns" -> {
                // A star of radius 10: its strokes burst out 6 ticks apart from 70
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 5);
                Vector3f[] pts = new Vector3f[5];
                for (int j = 0; j < 5; j++) {
                    float a = -Mth.HALF_PI + j * Mth.TWO_PI / 5;
                    pts[j] = new Vector3f(Mth.cos(a) * 10, 0, Mth.sin(a) * 10);
                }
                for (int j = 0; j < 5; j++) {
                    Vector3f a = pts[(j * 2) % 5], b = pts[(j * 2 + 2) % 5];
                    ThornsModel.draw(Lab.at(a), ctx.rec(), Lab.view(ctx, a), new Vector3f(b).sub(a), 2.6f, age - 70 - j * 6, 5 + j * 37, LIGHT);
                }
            }
            case "bwings" -> {
                // Called at 0, beat at 70; a fan 16 long, 35 degrees either side
                caster(ctx, 0, 0);
                husk(ctx, 2, 0, 9);
                husk(ctx, -4, 0, 12);
                Vector3f o = new Vector3f(0, 0, 0);
                WingsModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 6, age, 70 - age, 16, 35 * Mth.DEG_TO_RAD, 5, LIGHT);
            }
            case "bbell" -> {
                caster(ctx, 0, 0);
                husk(ctx, 4, 0, 3);
                Vector3f o = new Vector3f(0, 0, 0);
                BellModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 9.5f, 7), age, age - 40, 16, 13.5f, 0xFFD25A, 3, LIGHT);
            }
            case "bfist" -> {
                husk(ctx, 1, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                FistModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 26, 0), age, age - 40, 40, 0, 4, 0xFF6A1A, 3, LIGHT);
            }
            case "bcolumn" -> {
                caster(ctx, 0, -2);
                husk(ctx, 0, 0, 10);
                for (int k = -2; k <= 2; k++) {
                    Vector3f o = new Vector3f(k * 3.4f, 0, 0);
                    ColumnModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 18), age, age - 40 - Math.abs(k) * 5, 2.4f, 0xFFD25A, 3 + k, LIGHT);
                }
            }
            case "bscythe" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 5);
                Vector3f o = new Vector3f(0, 0, 0);
                ScytheModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, age - 40, 0, 9, 12, 0x9A3CFF, 3, LIGHT);
            }
            case "bmaiden" -> {
                caster(ctx, 0, -8);
                husk(ctx, 0, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                MaidenModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, age - 40, 180, 2, 0xFF2438, 3, LIGHT);
            }
            case "bking" -> {
                husk(ctx, 0, 0, 7.5f);
                Vector3f o = new Vector3f(0, 0, 0);
                KingModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 15), age, age - 40, 0, 0x9A3CFF, 3, LIGHT);
            }
            case "bclock" -> {
                caster(ctx, 0, 0);
                husk(ctx, 4, 0, 6);
                Vector3f o = new Vector3f(0, 0, 0);
                ClockModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, age - 40, 0, 12, 3, 11, 0xFFD25A, 3, LIGHT);
            }
            case "blotus" -> {
                husk(ctx, 1, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                LotusModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, age - 40, 7.5f, 0, 0xFF2438, 3, LIGHT);
            }
            case "bboard" -> {
                caster(ctx, 0, -10);
                husk(ctx, 1, 0, 2);
                Vector3f o = new Vector3f(0, 0, 0);
                BoardModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, age - 40, 0, 3, 5, 22, new int[] {0xFF2438, 0x9A3CFF}, 3, LIGHT);
            }
            case "bcannon" -> {
                // Charges from 0 to 40, fires at 40 for 40 ticks
                caster(ctx, 0, -1);
                husk(ctx, 0, 0, 10);
                Vector3f o = new Vector3f(0, 0, 0);
                CannonModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), 1.3f, Math.min(1, age / 40), age - 40, 40, age, 20, 2.6f, LIGHT);
            }
            case "bprison" -> {
                husk(ctx, 0, 0, 0);
                caster(ctx, 0, -9);
                Vector3f o = new Vector3f(0, 0, 0);
                PrisonModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 6, 4, Math.min(1, age / 10), age, LIGHT);
            }
            case "bpendulum" -> {
                caster(ctx, 0, -10);
                husk(ctx, 2, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                PendulumModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 15, 0), age, age - 40, 16, 3, 90, 20, 0xFF2438, 3, LIGHT);
            }
            case "bcollapse" -> {
                husk(ctx, 5, 0, 4);
                Vector3f o = new Vector3f(0, 0, 0);
                CollapseModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 4.5f, 0), age, age - 40, 12, 0x9A3CFF, 3, LIGHT);
            }
            case "ewave" -> {
                caster(ctx, 0, 0);
                husk(ctx, 0, 0, 9);
                for (int k = 0; k < 3; k++) {
                    Vector3f o = new Vector3f(0, 1.4f, 2.5f + k * 2.5f);
                    WaveModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age + k * 3, 1.1f, 1, 3 + k, LIGHT);
                }
            }
            case "eshriek" -> {
                caster(ctx, 0, 0);
                husk(ctx, 1, 0, 5);
                Vector3f o = new Vector3f(0, 1.5f, .3f);
                ShriekModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), 7, .7f, age, 3, LIGHT);
            }
            case "estep" -> {
                caster(ctx, 0, 8);
                husk(ctx, 2, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                StepModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 30 - age, 3, (float) Math.atan2(0, -1), 3, LIGHT);
            }
            case "etendrils" -> {
                caster(ctx, -6, -4);
                husk(ctx, 0, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                TendrilsModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 3.5f, age, 80, 3, LIGHT);
            }
            case "esensor" -> {
                husk(ctx, 3, 0, 4);
                Vector3f o = new Vector3f(0, 0, 0);
                SensorModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 400, age % 30, new Vector3f(3, 1.2f, 4), 3, LIGHT);
            }
            case "eburst" -> {
                husk(ctx, 0, 0, 0);
                Vector3f o = new Vector3f(0, 1, 0);
                BurstModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 4, 3, LIGHT);
            }
            case "eboom" -> {
                caster(ctx, 0, 0);
                husk(ctx, 0, 0, 14);
                Vector3f o = new Vector3f(0, 1.4f, .5f);
                BoomModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), 20, 1.2f, age, LIGHT);
            }
            case "edome" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 2);
                Vector3f o = new Vector3f(0, 0, 0);
                DomeModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 6, age, 160, 3, LIGHT);
            }
            case "eshrieker" -> {
                husk(ctx, 4, 0, 6);
                Vector3f o = new Vector3f(0, 0, 0);
                ShriekerModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 200, age - 40, new Vector3f(4, 1, 6), 3, LIGHT);
            }
            case "erequiem" -> {
                caster(ctx, 0, 0);
                husk(ctx, 6, 0, 4);
                Vector3f o = new Vector3f(0, 0, 0);
                RequiemModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 14, age, 140, 30, 12, 5, 3, LIGHT);
            }
            case "slance" -> {
                caster(ctx, 0, 0);
                husk(ctx, 0, 0, 9);
                Vector3f o = new Vector3f(0, 1.4f, 3);
                LanceModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 3, LIGHT);
            }
            case "sswarm" -> {
                husk(ctx, 1.2f, 0, .5f);
                Vector3f o = new Vector3f(0, 1.2f, 0);
                SwarmModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 100, age % 10, new Vector3f(1.2f, .1f, .5f), 3, LIGHT);
            }
            case "sgrasp" -> {
                husk(ctx, 0, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                GraspModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 70, 1.6f, 10, 12, 3, LIGHT);
            }
            case "sveil" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                VeilModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 2.4f, age, 200, age % 20, new Vector3f(2, 1.3f, 1), 3, LIGHT);
            }
            case "shourglass" -> {
                husk(ctx, 2, 0, 1);
                husk(ctx, -1.5f, 0, -2);
                Vector3f o = new Vector3f(0, 0, 0);
                HourglassModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 5, 3.2f, age, 140, 12, 3, LIGHT);
            }
            case "sserpent" -> {
                husk(ctx, 0, 0, 2.5f);
                Vector3f o = new Vector3f(0, 0, 0);
                SerpentModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 200, age % 30, 3, LIGHT);
            }
            case "slens" -> {
                husk(ctx, .6f, 0, .3f);
                Vector3f o = new Vector3f(0, 0, 0);
                LensModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 7, age, 140, 3, LIGHT);
            }
            case "sfall" -> {
                husk(ctx, .8f, 0, .5f);
                Vector3f o = new Vector3f(0, 0, 0);
                SandfallModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 3.5f, 10, age, 80, 3, LIGHT);
            }
            case "sdjinn" -> {
                caster(ctx, 1.3f, 1.4f);
                husk(ctx, -3, 0, 6);
                Vector3f o = new Vector3f(0, 0, 0);
                DjinnModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 300, age % 30, new Vector3f(-3, 1, 6), 3, LIGHT);
            }
            case "ssimoom" -> {
                husk(ctx, 3, 2, 2);
                Vector3f o = new Vector3f(0, 0, 0);
                SimoomModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), 5, age, 240, age % 25, new Vector3f(2.5f, 0, -1.5f), 3, LIGHT);
            }
            case "lpetal" -> {
                caster(ctx, 0, -3);
                husk(ctx, 0, 0, 6);
                for (int k = 0; k < 5; k++) {
                    float a = (k - 2) * 11 * Mth.DEG_TO_RAD;
                    Vector3f d = new Vector3f(Mth.sin(a), 0, Mth.cos(a));
                    Vector3f o = new Vector3f(d).mul(Math.min(age, 18) * 1.1f).add(0, 1.4f, -2.4f);
                    boolean done = age > 7 && k == 2;
                    PetalModel.draw(Lab.at(done ? new Vector3f(0, 1.4f, 5.3f) : o), ctx.rec(), Lab.view(ctx, done ? new Vector3f(0, 1.4f, 5.3f) : o), d, age, done,
                            age - 7, k * 31, LIGHT);
                }
            }
            case "lbud" -> {
                caster(ctx, 0, -5);
                husk(ctx, 0, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                BudModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 42, 1.3f, 3, LIGHT);
            }
            case "lpond" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 2);
                Vector3f o = new Vector3f(0, 0, 0);
                PondModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 200, 5, 3, LIGHT);
            }
            case "lthrone" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 6);
                Vector3f o = new Vector3f(0, 0, 0);
                ThroneModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 215, age % 30, new Vector3f(3, 1, 6), 3, LIGHT);
            }
            case "lroots" -> {
                husk(ctx, 1.5f, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                RootsModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 110, 4, 3, LIGHT);
            }
            case "lpod" -> {
                caster(ctx, 0, 0);
                husk(ctx, -2, 0, 7);
                Vector3f o = new Vector3f(1, 0, -.6f);
                PodModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 252, age % 20, new Vector3f(-3, 1, 7.6f), 3, LIGHT);
            }
            case "lmandala" -> {
                husk(ctx, 3, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                MandalaModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 72, 7, 3, LIGHT);
            }
            case "llantern" -> {
                caster(ctx, 0, -3);
                for (int k = 0; k < 5; k++) {
                    float a = k * Mth.TWO_PI / 5;
                    Vector3f o = new Vector3f(Mth.sin(a) * 1.8f, 1.6f + .3f * k, Mth.cos(a) * 1.8f - 3);
                    LanternModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 200, k == 4 && age > 20, age - 20, k, k * 17, LIGHT);
                }
            }
            case "lstorm" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                PetalStormModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 175, 4.5f, 3, LIGHT);
            }
            case "lthousand" -> {
                husk(ctx, 5, 0, 3);
                husk(ctx, -4, 0, -5);
                Vector3f o = new Vector3f(0, 0, 0);
                ThousandModel.draw(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 170, 10, age % 5, new Vector3f(5, 1, 3), 3, LIGHT);
            }
            case "gshard" -> {
                caster(ctx, 0, -3);
                husk(ctx, 0, 0, 7);
                for (int k = 0; k < 3; k++) {
                    float a = (k - 1) * 6 * Mth.DEG_TO_RAD;
                    Vector3f d = new Vector3f(Mth.sin(a), 0, Mth.cos(a));
                    Vector3f o = new Vector3f(d).mul(Math.min(age, 18) * 1.3f).add(0, 1.4f, -2.4f);
                    RubyModels.shard(Lab.at(o), ctx.rec(), Lab.view(ctx, o), d, age, false, 0, k * 31, LIGHT);
                }
            }
            case "gnova" -> {
                caster(ctx, 0, 0);
                husk(ctx, 4, 0, 4);
                Vector3f o = new Vector3f(0, 0, 0);
                RubyModels.nova(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 40, 22, 9, 3, LIGHT);
            }
            case "gmeteor" -> {
                husk(ctx, 2, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                RubyModels.meteor(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 115, 30, 30, 4.5f, 3, LIGHT);
            }
            case "gstar" -> {
                caster(ctx, 0, -3);
                husk(ctx, 0, 0, 7);
                boolean done = age > 9;
                Vector3f o = done ? new Vector3f(0, 1.4f, 6.6f) : new Vector3f(0, 1.4f, -2.4f + age);
                LapisModels.star(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, done, age - 9, 3, LIGHT);
            }
            case "gvault" -> {
                husk(ctx, 2, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                GemModelsLab.vault(ctx, o, age);
            }
            case "gmirror" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 5);
                Vector3f o = new Vector3f(0, 0, 0);
                LapisModels.mirror(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 200, age % 40, new Vector3f(3, 1.1f, 5), 3, LIGHT);
            }
            case "glance" -> {
                caster(ctx, 0, 0);
                husk(ctx, 0, 0, 8);
                husk(ctx, 0, 0, 14);
                Vector3f o = new Vector3f(0, 1.5f, .3f);
                DiamondModels.lance(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 14, 20, 4, 3, LIGHT);
            }
            case "gdust" -> {
                husk(ctx, 1, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                DiamondModels.dust(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 92, 3.5f, 3, LIGHT);
            }
            case "gbastion" -> {
                caster(ctx, 2, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                DiamondModels.bastion(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 254, 5, 14, 3, LIGHT);
            }
            case "gspikes" -> {
                caster(ctx, 0, 0);
                husk(ctx, .3f, 0, 7);
                Vector3f o = new Vector3f(0, 0, 0);
                AmethystModels.spikes(Lab.at(o), ctx.rec(), Lab.view(ctx, o), new Vector3f(0, 0, 1), age, 32, 12, 3, LIGHT);
            }
            case "gbudding" -> {
                husk(ctx, 2, 0, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                AmethystModels.budding(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 95, 5, 20, 3, LIGHT);
            }
            case "gchimes" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 5);
                Vector3f o = new Vector3f(0, 0, 0);
                AmethystModels.chimes(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 240, 20, 20, (age - 20) % 20, new Vector3f(3, 1.1f, 5), 3, LIGHT);
            }
            case "glifeline" -> {
                caster(ctx, 0, 0);
                caster(ctx, 3, 4);
                Vector3f o = new Vector3f(0, 0, 0);
                EmeraldModels.lifeline(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 68, new Vector3f(3, 1.1f, 4), age % 10, 3, LIGHT);
            }
            case "gaegis" -> {
                caster(ctx, 0, 0);
                Vector3f o = new Vector3f(0, 0, 0);
                EmeraldModels.aegis(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 60, 3, LIGHT);
            }
            case "gtree" -> {
                caster(ctx, 2, 1);
                Vector3f o = new Vector3f(0, 0, 0);
                EmeraldModels.tree(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 260, 20, 5, (age - 20) % 40, 3, LIGHT);
            }
            case "gthorns" -> {
                caster(ctx, 0, 0);
                husk(ctx, 3, 0, 3);
                Vector3f o = new Vector3f(0, 0, 0);
                EmeraldModels.thorns(Lab.at(o), ctx.rec(), Lab.view(ctx, o), age, 30, 6, 7, 3, LIGHT);
            }
            default -> throw new IllegalArgumentException("no scene " + scene);
        }
    }
}
