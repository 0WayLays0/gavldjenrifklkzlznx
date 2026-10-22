package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenWave;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Token Wave: a breaking wave made of soul tokens, laid over each other like scales so no gap shows. Its profile is a
 * long back rising to a crest and a lip that curls forward and down; the tokens flow along it, coming out of the ground
 * at the back, riding up over the crest and tumbling down the lip, while more spray off the front. The crest sways
 * along its width, the middle runs ahead of the sides, and the sides taper down. It rises out of the ground as it sets
 * off and, at the end of its run, crashes and scatters.
 */
public class TokenWaveRenderer extends EntityRenderer<TokenWave> {
    /** Tokens along the wave's profile and across its width. */
    private static final int ALONG = 18;
    private static final int ACROSS = 12;
    private static final float SIZE = .42f;
    /** Height of the crest, and how fast the tokens flow over it (profile lengths per tick). */
    private static final float HEIGHT = 1.9f;
    private static final float FLOW = .045f;
    private static final int SPRAY = 24;
    private static final int SPRAY_TICKS = 14;

    /** The profile as two cubic curves (along, height), the height in crest heights: the back, then the curling lip. */
    private static final float[][] BACK = {{-2.8f, 0}, {-1.7f, .1f}, {-1.0f, .95f}, {-.25f, 1}};
    private static final float[][] LIP = {{-.25f, 1}, {.4f, 1.05f}, {.8f, .6f}, {.45f, .08f}};
    private static final float SPLIT = .6f;

    public TokenWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** The profile at {@code s} (0 at the foot of its back, 1 at the foot of its lip): along, height, tangent along, tangent up. */
    private static float[] profile(float s) {
        float[][] c = s < SPLIT ? BACK : LIP;
        float t = s < SPLIT ? s / SPLIT : (s - SPLIT) / (1 - SPLIT);
        float m = 1 - t;
        float b0 = m * m * m, b1 = 3 * m * m * t, b2 = 3 * m * t * t, b3 = t * t * t;
        float d0 = -3 * m * m, d1 = 3 * m * m - 6 * m * t, d2 = 6 * m * t - 3 * t * t, d3 = 3 * t * t;
        return new float[]{
                b0 * c[0][0] + b1 * c[1][0] + b2 * c[2][0] + b3 * c[3][0],
                b0 * c[0][1] + b1 * c[1][1] + b2 * c[2][1] + b3 * c[3][1],
                d0 * c[0][0] + d1 * c[1][0] + d2 * c[2][0] + d3 * c[3][0],
                d0 * c[0][1] + d1 * c[1][1] + d2 * c[2][1] + d3 * c[3][1]};
    }

    private static float smooth(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override
    public void render(TokenWave wave, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = wave.strength(partialTicks);
        if (strength <= 0) {
            return;
        }
        float age = wave.tickCount + partialTicks;
        float front = wave.front(partialTicks);
        float grown = wave.grown(partialTicks);
        float crash = 1 - strength;
        int seed = wave.getId() * 7919;

        poseStack.pushPose();
        // From here +Z is the way the wave rolls
        poseStack.mulPose(Axis.YP.rotationDegrees(-wave.getYRot()));
        for (int i = 0; i < ACROSS; i++) {
            float l = (i + .5f) / ACROSS * 2 - 1;
            float x = l * TokenWave.WIDTH * .5f;
            // The sides lower and lag, the crest sways along its width
            float taper = 1 - l * l * l * l * .75f;
            float bow = .45f * (1 - l * l);
            float height = HEIGHT * grown * taper * (1 + .12f * Mth.sin(l * 3.1f + age * .32f)) * (1 - .65f * crash);
            float stretch = .35f + .65f * grown;
            float phase = RuneRender.hash(seed + i) * .5f;
            for (int k = 0; k < ALONG; k++) {
                float s = (k + (i & 1) * .5f) / ALONG + phase + age * FLOW;
                s -= Mth.floor(s);
                float[] p = profile(s);
                float along = front + bow + p[0] * stretch;
                if (along < .2f) {
                    continue;
                }
                // Each token comes out of the ground at the back and sinks back at the foot of the lip
                float fade = smooth(0, .08f, s) * (1 - smooth(.9f, 1, s));
                float size = SIZE * fade * (.85f + .3f * RuneRender.hash(seed + i * 31 + k)) * (1 - .4f * crash);
                if (size < .02f) {
                    continue;
                }
                int h = seed + i * 131 + k * 17;
                // As it crashes the tokens break loose: thrown forward, falling
                float loose = crash * (.6f + RuneRender.hash(h));
                float y = p[1] * height + .12f - loose * .6f;
                float z = along + loose * (1 + RuneRender.hash(h + 1));
                float sideways = (RuneRender.hash(h + 2) - .5f) * .12f + loose * (RuneRender.hash(h + 3) - .5f) * 1.2f;
                // Laid on the wave's face: the token's flat side turned out of it, a little flutter, tumbling at the lip
                float facing = (float) Mth.atan2(p[3] * height, p[2] * stretch) * Mth.RAD_TO_DEG;
                float tumble = Math.max(0, s - .86f) / .14f * 110 + loose * 400;
                float flutter = 14 * Mth.sin(age * .45f + h);
                poseStack.pushPose();
                poseStack.translate(x + sideways, Math.max(.05f, y), z);
                poseStack.mulPose(Axis.XP.rotationDegrees(-facing + tumble));
                poseStack.mulPose(Axis.ZP.rotationDegrees(flutter));
                TokenRender.token(poseStack, buffer, size, GlyphRender.FULL);
                poseStack.popPose();
            }
        }
        spray(wave, poseStack, buffer, age, front, grown, crash, seed);
        // A faint cyan sheen along the crest
        if (grown > .3f) {
            for (int g = 0; g < 5; g++) {
                float l = (g / 4f) * 2 - 1;
                poseStack.pushPose();
                poseStack.translate(l * TokenWave.WIDTH * .45f, HEIGHT * grown * (1 - .65f * crash) + .1f, front + .45f * (1 - l * l) - .2f);
                TokenRender.glow(poseStack, buffer, this.entityRenderDispatcher, .9f, .22f * strength);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        super.render(wave, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Tokens thrown off the lip in short arcs, landing ahead of the wave; a burst of them as it crashes. */
    private static void spray(TokenWave wave, PoseStack poseStack, MultiBufferSource buffer, float age, float front, float grown,
                              float crash, int seed) {
        if (grown < .5f) {
            return;
        }
        int count = SPRAY + (int) (crash * 16);
        for (int n = 0; n < count; n++) {
            int h = seed + 977 * n;
            float t = (age / SPRAY_TICKS + RuneRender.hash(h)) % 1f;
            float ticks = t * SPRAY_TICKS;
            float l = RuneRender.hash(h + 1) * 2 - 1;
            float up = .12f + .1f * RuneRender.hash(h + 2) + crash * .1f;
            float ahead = .1f + .12f * RuneRender.hash(h + 3);
            float y = HEIGHT * grown * .95f * (1 - .5f * crash) + up * ticks - .03f * ticks * ticks;
            if (y < .05f) {
                continue;
            }
            float size = SIZE * .75f * (1 - t * t);
            poseStack.pushPose();
            poseStack.translate(l * TokenWave.WIDTH * .5f, y, front + .5f * (1 - l * l) + .5f + ahead * ticks);
            poseStack.mulPose(Axis.XP.rotationDegrees(ticks * (30 + 40 * RuneRender.hash(h + 4))));
            poseStack.mulPose(Axis.ZP.rotationDegrees(ticks * 25 * (RuneRender.hash(h + 5) - .5f)));
            TokenRender.token(poseStack, buffer, size, GlyphRender.FULL);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRender(TokenWave entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TokenWave entity) {
        return GlyphRender.GLOW;
    }
}
