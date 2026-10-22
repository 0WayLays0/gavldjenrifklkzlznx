package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.gem.AmethystGeode;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Geode: a shell of blocks closing round the prisoner from the ground up, in the three layers of a real geode
 * (smooth basalt, calcite, amethyst), left open on the caster's side like a geode cut in half so the inside shows;
 * amethyst crystals growing in from the shell and stabbing, flaring at each stab; when it cracks, the shell is
 * thrown out in pieces.
 */
public class AmethystGeodeRenderer extends EntityRenderer<AmethystGeode> {
    private static final BlockState[] LAYERS = {Blocks.SMOOTH_BASALT.defaultBlockState(), Blocks.CALCITE.defaultBlockState(),
            Blocks.AMETHYST_BLOCK.defaultBlockState()};
    private static final int[] COUNTS = {70, 56, 44};
    private static final float[] DEPTH = {0, .42f, .8f};
    private static final float[] CUBE = {.62f, .55f, .48f};
    private static final Vec3[][] POINTS = new Vec3[3][];

    static {
        // Points spread evenly over a sphere (a Fibonacci spiral), one set per layer
        for (int l = 0; l < 3; l++) {
            int n = COUNTS[l];
            POINTS[l] = new Vec3[n];
            double golden = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < n; i++) {
                double y = 1 - 2 * (i + .5) / n;
                double r = Math.sqrt(1 - y * y);
                double a = golden * i + l;
                POINTS[l][i] = new Vec3(Math.cos(a) * r, y, Math.sin(a) * r);
            }
        }
    }

    public AmethystGeodeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AmethystGeode geode, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = geode.tickCount + partialTicks;
        int life = geode.getLife();
        float radius = geode.getRadius();
        float midY = radius * .75f;
        float yaw = geode.getYRot() * Mth.DEG_TO_RAD;
        Vec3 open = new Vec3(Mth.cos(yaw), .15, Mth.sin(yaw)).normalize();
        boolean cracked = age >= life;
        float since = age - life;
        int seed = geode.getId() * 53;
        for (int l = 0; l < 3; l++) {
            float r = radius - DEPTH[l];
            for (int i = 0; i < POINTS[l].length; i++) {
                Vec3 d = POINTS[l][i];
                // The cut side, and nothing under the ground
                if (d.dot(open) > .42 - .08 * l || midY + d.y * r < -.2) {
                    continue;
                }
                Vec3 p = d.scale(r).add(0, midY, 0);
                int s = seed + l * 1000 + i;
                if (cracked) {
                    float h = RuneRender.hash(s);
                    WallRender.fragment(LAYERS[l], poseStack, buffer, packedLight, (float) p.x, (float) p.y, (float) p.z,
                            (float) d.x * (.25f + .2f * h), (float) d.y * .2f + .2f, (float) d.z * (.25f + .2f * h), since, AmethystGeode.CRACK, CUBE[l], s);
                    continue;
                }
                // From the ground up, each block flying into place
                float delay = (float) (1 - (d.y + 1) * .5) * AmethystGeode.FORM * .75f + l * 2;
                float t = Mth.clamp((age - delay) / 5, 0, 1);
                if (t <= 0) {
                    continue;
                }
                float lift = (1 - t) * (1 - t) * 1.5f;
                poseStack.pushPose();
                poseStack.translate(p.x * (.7f + .3f * t), p.y - lift, p.z * (.7f + .3f * t));
                poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d.toVector3f()));
                float c = CUBE[l];
                RuneRender.block(LAYERS[l], poseStack, buffer, packedLight, 0, -c * .5f, 0, c, c, c, 0, 0);
                poseStack.popPose();
            }
        }
        if (cracked) {
            super.render(geode, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        // The crystals growing in, flaring at each stab
        float grow = Mth.clamp((age - AmethystGeode.FORM) / AmethystGeode.GROW, 0, 1);
        float stab = 0;
        if (age >= AmethystGeode.FORM + AmethystGeode.GROW) {
            stab = Math.max(0, 1 - ((age - AmethystGeode.FORM - AmethystGeode.GROW) % AmethystGeode.STAB_TICKS) / 6);
        }
        if (grow > 0) {
            float r = radius - DEPTH[2];
            for (int i = 0; i < POINTS[2].length; i += 3) {
                Vec3 d = POINTS[2][i];
                if (d.dot(open) > .3 || midY + d.y * r < 0) {
                    continue;
                }
                float h = RuneRender.hash(seed + i * 7);
                float len = (r - .35f) * (.55f + .4f * h) * grow * (1 + .12f * stab);
                Vec3 p = d.scale(r - .1).add(0, midY, 0);
                poseStack.pushPose();
                poseStack.translate(p.x, p.y, p.z);
                // Pointing in at the heart
                poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d.scale(-1).toVector3f()));
                poseStack.translate(0, len * .45f, 0);
                poseStack.scale(.7f, 1.7f, .7f);
                GemRender.gem(poseStack, buffer, GemRender.Cut.CRYSTAL, GlyphRender.whiten(Gem.AMETHYST.color, stab * .35f), len * .6f, .8f, age + i * 7);
                poseStack.popPose();
            }
        }
        GemRender.glow(poseStack, buffer, new Vec3(0, midY, 0), Gem.AMETHYST.color, radius * 1.1f, (.25f + .5f * stab) * grow);
        super.render(geode, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(AmethystGeode geode, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(geode.getBoundingBox().inflate(5));
    }

    @Override
    public ResourceLocation getTextureLocation(AmethystGeode geode) {
        return GemRender.FACET;
    }
}
