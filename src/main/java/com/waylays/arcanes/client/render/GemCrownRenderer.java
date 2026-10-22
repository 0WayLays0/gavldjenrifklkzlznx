package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.GemCrown;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Crown of the Five Gems: a pentagram of light on the ground joining five great gems at its points, each in its
 * own cut (a ruby, a tablet of lapis with a golden rune, a diamond, a cluster of amethyst points, an emerald), a column
 * of light under each. Each gem shows what it does in its turn (the ruby's ray, the lapis's chain, the amethyst's
 * ring...). Then they spiral up and fuse into one prismatic gem that strikes the spot aimed at with a ray of every
 * colour.
 */
public class GemCrownRenderer extends EntityRenderer<GemCrown> {
    public GemCrownRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GemCrown crown, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = crown.tickCount + partialTicks;
        float fuse = Mth.clamp((age - GemCrown.RISE - GemCrown.ACT) / GemCrown.FUSE, 0, 1);
        float struck = age - GemCrown.STRIKE;
        float after = struck < 0 ? 1 : Mth.clamp(1 - struck / GemCrown.AFTER, 0, 1);
        Gem[] gems = Gem.values();
        Vec3[] at = new Vec3[gems.length];
        for (Gem g : gems) {
            at[g.ordinal()] = crown.gem(g, age);
        }
        float ground = Mth.clamp(age / 20, 0, 1) * (1 - fuse * .6f) * after;
        // The pentagram on the ground, and the ring round it
        int cycle = Gem.of((int) (age / 12) % 5).color;
        GemRender.sigil(poseStack, buffer, cycle, GemCrown.RADIUS + 2, .55f * ground, age * .3f);
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        for (int i = 0; i < 5; i++) {
            Vec3 a = new Vec3(at[i].x, .06, at[i].z);
            Vec3 b = new Vec3(at[(i + 2) % 5].x, .06, at[(i + 2) % 5].z);
            if (fuse <= 0) {
                GlyphRender.beam(stroke, poseStack.last(), a, b, .12f, RuneRender.additive(gems[i].color, .8f * ground));
                GlyphRender.beam(stroke, poseStack.last(), a, new Vec3(at[(i + 1) % 5].x, .06, at[(i + 1) % 5].z), .05f,
                        RuneRender.additive(gems[i].color, .4f * ground));
            }
        }
        // Columns of light under the gems, and threads joining them in the air
        for (int i = 0; i < 5; i++) {
            if (fuse <= 0) {
                GlyphRender.beam(stroke, poseStack.last(), new Vec3(at[i].x, .05, at[i].z), at[i], .4f, RuneRender.additive(gems[i].color, .25f * ground));
            }
            GlyphRender.beam(stroke, poseStack.last(), at[i], at[(i + 1) % 5], .04f, RuneRender.additive(gems[i].light, .5f * after * (struck < 0 ? 1 : 0)));
        }
        // The gems, while they are five
        if (struck < 0) {
            for (Gem g : gems) {
                Vec3 p = at[g.ordinal()];
                float size = 1.1f * (1 - fuse * .5f);
                poseStack.pushPose();
                poseStack.translate(p.x, p.y, p.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(age * 2 + g.ordinal() * 72));
                switch (g) {
                    case RUBY -> GemRender.jewel(poseStack, buffer, GemRender.Cut.BRILLIANT, g.color, size, .8f, age, .8f, false, g.ordinal());
                    case DIAMOND -> GemRender.jewel(poseStack, buffer, GemRender.Cut.BRILLIANT, g.color, size * 1.1f, .55f, age, .7f, true, g.ordinal());
                    case EMERALD -> {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                        GemRender.jewel(poseStack, buffer, GemRender.Cut.STEP, g.color, size, .75f, age, .8f, false, g.ordinal());
                    }
                    case AMETHYST -> {
                        for (int k = 0; k < 3; k++) {
                            poseStack.pushPose();
                            poseStack.mulPose(Axis.YP.rotationDegrees(k * 120));
                            poseStack.mulPose(Axis.XP.rotationDegrees(k == 0 ? 0 : 28));
                            poseStack.translate(0, size * .25f, 0);
                            poseStack.scale(.7f, 1.6f, .7f);
                            GemRender.gem(poseStack, buffer, GemRender.Cut.CRYSTAL, g.color, size * (k == 0 ? .75f : .55f), .78f, age + k * 9);
                            poseStack.popPose();
                        }
                        GemRender.heart(poseStack, buffer, g.color, size, .8f);
                    }
                    case LAPIS -> {
                        RuneRender.block(Blocks.LAPIS_BLOCK.defaultBlockState(), poseStack, buffer, GlyphRender.FULL, 0, -size * .7f, 0, size * .8f, size * 1.3f, size * .22f, 0, 0);
                        poseStack.translate(0, 0, size * .12f);
                        RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), crown.getId() + 3, size * .6f,
                                RuneRender.additive(g.light, .9f), GlyphRender.FULL);
                        poseStack.translate(0, 0, -size * .24f);
                        poseStack.mulPose(Axis.YP.rotationDegrees(180));
                        RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), crown.getId() + 3, size * .6f,
                                RuneRender.additive(g.light, .9f), GlyphRender.FULL);
                    }
                }
                poseStack.popPose();
                GemRender.glow(poseStack, buffer, p, g == Gem.LAPIS ? g.light : g.color, 1.8f, .35f);
            }
        }
        // What the gems do in their turn
        Vec3 base = crown.getPosition(partialTicks);
        if (age >= GemCrown.RISE && fuse <= 0) {
            int round = (int) ((age - GemCrown.RISE) / GemCrown.ROUND);
            for (Gem g : new Gem[]{Gem.RUBY, Gem.LAPIS}) {
                float since = age - GemCrown.actsAt(g, round);
                if (since < 0 || since > 8) {
                    continue;
                }
                Entity target = crown.level().getEntity(g == Gem.RUBY ? crown.getRubyTarget() : crown.getLapisTarget());
                if (target != null) {
                    Vec3 to = target.getPosition(partialTicks).add(0, target.getBbHeight() * .55, 0).subtract(base);
                    GemRender.ray(poseStack, buffer, at[g.ordinal()], to, g == Gem.LAPIS ? g.light : g.color, g == Gem.RUBY ? .2f : .07f, 1 - since / 8, age);
                    GemRender.glow(poseStack, buffer, to, g == Gem.LAPIS ? g.light : g.color, 1f, 1 - since / 8);
                }
            }
            float ring = age - GemCrown.actsAt(Gem.AMETHYST, round);
            if (ring >= 0 && ring < 12) {
                poseStack.pushPose();
                poseStack.translate(0, .15, 0);
                RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), (GemCrown.RADIUS + 3) * ring / 12,
                        RuneRender.additive(Gem.AMETHYST.light, (1 - ring / 12) * .8f), GlyphRender.FULL);
                poseStack.popPose();
            }
        }
        // The fused gem, and its strike
        if (fuse > .6f) {
            Vec3 top = new Vec3(0, GemCrown.FUSE_HEIGHT, 0);
            float form = Mth.clamp((fuse - .6f) / .4f, 0, 1);
            int prism = Mth.hsvToRgb((age * .01f) % 1, .35f, 1);
            if (struck < 0) {
                poseStack.pushPose();
                poseStack.translate(top.x, top.y, top.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(age * 6));
                GemRender.jewel(poseStack, buffer, GemRender.Cut.BRILLIANT, prism, 2.2f * form, .6f, age * 2, 1, true, 77);
                poseStack.popPose();
            }
            GemRender.glow(poseStack, buffer, top, prism, 5 * form * after, .6f * after);
            if (struck >= 0 && struck < 14) {
                Vec3 to = crown.getStrike().subtract(base);
                float s = 1 - struck / 14;
                for (Gem g : gems) {
                    Vec3 off = new Vec3(Mth.cos(g.ordinal() * 1.256f + age * .3f), 0, Mth.sin(g.ordinal() * 1.256f + age * .3f)).scale(.35 * s);
                    GemRender.ray(poseStack, buffer, top.add(off), to.add(off), g == Gem.LAPIS ? g.light : g.color, .35f, s, age + g.ordinal());
                }
                GemRender.ray(poseStack, buffer, top, to, 0xFFFFFF, .5f, s, age);
                GemRender.glow(poseStack, buffer, to.add(0, 1, 0), 0xFFFFFF, 7 * (1 - s * .5f), s);
            }
        }
        super.render(crown, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GemCrown crown, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GemCrown crown) {
        return GemRender.FACET;
    }
}
