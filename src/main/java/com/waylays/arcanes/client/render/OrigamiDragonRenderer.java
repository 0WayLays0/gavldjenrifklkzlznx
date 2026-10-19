package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.OrigamiDragon;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Origami Dragon, built every frame out of flat paper facets so it looks folded: a diamond-section body whose
 * points follow the path the dragon flew (the tail trails behind its turns), a pointed head with an opening jaw,
 * gilded horns and dorsal fins, two flapping fan wings and a fan tail. Each facet catches the light on its own.
 * <p>
 * When it appears every facet starts as a square of one creased sheet of paper, and they fold into place one after
 * the other from the head to the tail; at the end they unfold back into the sheet.
 */
public class OrigamiDragonRenderer extends EntityRenderer<OrigamiDragon> {
    private static final ResourceLocation PAPER = ArcanesOublies.id("textures/entity/origami_paper.png");
    private static final ResourceLocation EYE = ArcanesOublies.id("textures/entity/origami_eye.png");
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int BODY_POINTS = 11;
    private static final double SPACING = .55;
    private static final double SHEET = 6.5;

    private static final int BODY = 0xFFFBF6FF;
    private static final int BELLY = 0xFFE6DAFA;
    private static final int WING = 0xFFDCCBFF;
    private static final int WING_TIP = 0xFFB99BF6;
    private static final int GILT = 0xFFFFDC80;
    private static final int EYE_COLOR = 0xFFC9A0FF;

    /** One facet: its three corners (relative to the chest) and its colour. */
    private record Facet(Vec3 a, Vec3 b, Vec3 c, int color) {}

    private final List<Facet> facets = new ArrayList<>();
    private final List<Facet> eyes = new ArrayList<>();

    public OrigamiDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(OrigamiDragon entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        int duration = entity.getDuration();
        // 1 = a dragon, 0 = a flat sheet
        float unfold = Mth.clamp((age - 8) / (OrigamiDragon.UNFOLD_TICKS - 8), 0f, 1f);
        float refold = Mth.clamp((age - (duration - OrigamiDragon.FOLD_TICKS)) / (OrigamiDragon.FOLD_TICKS - 4), 0f, 1f);
        float shape = Math.min(unfold, 1 - refold);
        float sheetSize = Mth.clamp(age / 8f, 0f, 1f);

        Vec3 origin = entity.getPosition(partialTicks);
        Vec3 heading = entity.getHeading(partialTicks);
        facets.clear();
        eyes.clear();
        build(entity, origin, heading, age);

        // Paper gleams a little even at night
        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 9), LightTexture.sky(packedLight));
        VertexConsumer paper = buffer.getBuffer(RenderType.entityCutoutNoCull(PAPER));
        PoseStack.Pose pose = poseStack.last();
        Vec3 flatForward = new Vec3(heading.x, 0, heading.z);
        flatForward = flatForward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flatForward.normalize();
        Vec3 flatRight = flatForward.cross(UP);
        int count = facets.size();
        int grid = (int) Math.ceil(Math.sqrt(count / 2.0));
        double cell = SHEET * sheetSize / grid;
        double half = SHEET * sheetSize * .5;
        for (int k = 0; k < count; k++) {
            Facet f = facets.get(k);
            Vec3 a = f.a();
            Vec3 b = f.b();
            Vec3 c = f.c();
            if (shape < 1) {
                // Where this facet lies on the unfolded sheet: half of one square of the crease grid
                int square = k / 2;
                double x0 = (square % grid) * cell - half;
                double z0 = (square / grid) * cell - half;
                Vec3 p00 = sheetPoint(flatRight, flatForward, x0, z0, age);
                Vec3 p11 = sheetPoint(flatRight, flatForward, x0 + cell, z0 + cell, age);
                Vec3 sa = p00;
                Vec3 sb = k % 2 == 0 ? sheetPoint(flatRight, flatForward, x0 + cell, z0, age) : p11;
                Vec3 sc = k % 2 == 0 ? p11 : sheetPoint(flatRight, flatForward, x0, z0 + cell, age);
                // Head first, tail last
                float delay = k / (float) count * .45f;
                float t = Mth.clamp((shape - delay) / .55f, 0f, 1f);
                t = t * t * (3 - 2 * t);
                a = sa.lerp(a, t);
                b = sb.lerp(b, t);
                c = sc.lerp(c, t);
            }
            emit(paper, pose, a, b, c, f.color(), light, k % 2 == 0);
        }
        if (shape > .9f) {
            VertexConsumer glow = buffer.getBuffer(RenderType.eyes(EYE));
            // They light up once the head has folded
            float on = (shape - .9f) * 10;
            int color = 0xFF000000 | ((int) (on * 0xC9) << 16) | ((int) (on * 0xA0) << 8) | (int) (on * 0xFF);
            for (Facet f : eyes) {
                emit(glow, pose, f.a(), f.b(), f.c(), color, LightTexture.FULL_BRIGHT, true);
            }
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A point of the unfolded sheet, which ripples gently while it hangs in the air. */
    private static Vec3 sheetPoint(Vec3 right, Vec3 forward, double x, double z, float age) {
        double ripple = Mth.sin(age * .3f + (float) x * 1.3f) * .12 + Mth.cos(age * .23f + (float) z * 1.1f) * .08;
        return right.scale(x).add(forward.scale(z)).add(0, ripple, 0);
    }

    // ---------------------------------------------------------------- the dragon's shape

    private void build(OrigamiDragon entity, Vec3 origin, Vec3 heading, float age) {
        // The spine, head end first: neck in front of the chest, body and tail along the flown path behind it
        List<Vec3> spine = new ArrayList<>();
        spine.add(heading.scale(1.25));
        spine.add(heading.scale(.65));
        spine.add(Vec3.ZERO);
        List<Vec3> path = followPath(entity, origin, heading);
        Vec3 side = safeRight(heading, null);
        for (int i = 0; i < path.size(); i++) {
            // A slow sway, growing toward the tail
            double sway = Mth.sin(age * .22f - i * .7f) * .14 * (i + 1) / path.size();
            spine.add(path.get(i).subtract(origin).add(side.scale(sway)));
        }
        int n = spine.size();
        Vec3[] tangent = new Vec3[n];
        Vec3[] right = new Vec3[n];
        Vec3[] up = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Vec3 ahead = spine.get(Math.max(0, i - 1));
            Vec3 behind = spine.get(Math.min(n - 1, i + 1));
            Vec3 t = ahead.subtract(behind);
            tangent[i] = t.lengthSqr() < 1.0E-6 ? heading : t.normalize();
            right[i] = safeRight(tangent[i], i > 0 ? right[i - 1] : null);
            up[i] = right[i].cross(tangent[i]).normalize();
        }
        // Diamond cross-sections: top, right, bottom, left
        Vec3[][] ring = new Vec3[n][4];
        for (int i = 0; i < n; i++) {
            double w;
            double h;
            if (i == 0) {
                w = .26;
                h = .3;
            } else if (i == 1) {
                w = .34;
                h = .36;
            } else {
                double t = (i - 2) / (double) (n - 2);
                w = .58 * Math.pow(1 - t, .9) + .05;
                h = .5 * (1 - t) + .06;
            }
            Vec3 p = spine.get(i);
            ring[i][0] = p.add(up[i].scale(h));
            ring[i][1] = p.add(right[i].scale(w));
            ring[i][2] = p.subtract(up[i].scale(h * .8));
            ring[i][3] = p.subtract(right[i].scale(w));
        }
        head(ring[0], spine.get(0), heading, right[0], up[0], entity.isBreathing(), age);
        for (int i = 0; i < n - 1; i++) {
            for (int s = 0; s < 4; s++) {
                int s2 = (s + 1) % 4;
                int color = s == 0 || s == 3 ? BODY : BELLY;
                facet(ring[i][s], ring[i][s2], ring[i + 1][s2], color);
                facet(ring[i][s], ring[i + 1][s2], ring[i + 1][s], color);
            }
        }
        // Gilded fins down the back
        for (int i = 2; i < n - 3; i += 2) {
            double size = .5 * (1 - (i - 2) / (double) n);
            facet(ring[i][0], ring[i + 1][0], ring[i][0].add(up[i].scale(size)).subtract(tangent[i].scale(size * .6)), GILT);
        }
        wings(spine.get(2), tangent[2], right[2], up[2], age, entity);
        // Fan tail
        Vec3 tip = spine.get(n - 1);
        Vec3 before = spine.get(n - 2);
        Vec3 t = tangent[n - 1];
        Vec3 r = right[n - 1];
        facet(before, tip.add(r.scale(.6)).subtract(t.scale(.55)), tip, GILT);
        facet(before, tip, tip.subtract(r.scale(.6)).subtract(t.scale(.55)), GILT);
        facet(tip, tip.add(r.scale(.3)).subtract(t.scale(.9)), tip.subtract(r.scale(.3)).subtract(t.scale(.9)), WING_TIP);
    }

    private void head(Vec3[] base, Vec3 neck, Vec3 f, Vec3 r, Vec3 u, boolean breathing, float age) {
        Vec3 top = base[0];
        Vec3 rightBase = base[1];
        Vec3 bottom = base[2];
        Vec3 leftBase = base[3];
        Vec3 crown = neck.add(f.scale(.45)).add(u.scale(.3));
        Vec3 snout = neck.add(f.scale(1.1)).subtract(u.scale(.02));
        Vec3 cheekR = neck.add(f.scale(.35)).add(r.scale(.3));
        Vec3 cheekL = neck.add(f.scale(.35)).subtract(r.scale(.3));
        Vec3 mouthR = neck.add(f.scale(.25)).add(r.scale(.2)).subtract(u.scale(.14));
        Vec3 mouthL = neck.add(f.scale(.25)).subtract(r.scale(.2)).subtract(u.scale(.14));
        double open = breathing ? .75 + .2 * Mth.sin(age * 1.4f) : .06;
        Vec3 chin = neck.add(f.scale(.85 - open * .2)).subtract(u.scale(.2 + open * .45));
        // Upper head
        facet(top, cheekR, crown, BODY);
        facet(top, crown, cheekL, BODY);
        facet(crown, cheekR, snout, BODY);
        facet(crown, snout, cheekL, BODY);
        facet(top, rightBase, cheekR, BODY);
        facet(top, cheekL, leftBase, BODY);
        facet(rightBase, mouthR, cheekR, BELLY);
        facet(leftBase, cheekL, mouthL, BELLY);
        facet(cheekR, mouthR, snout, BELLY);
        facet(cheekL, snout, mouthL, BELLY);
        facet(mouthR, mouthL, snout, WING_TIP);
        // Lower jaw
        facet(mouthR, chin, mouthL, WING_TIP);
        facet(bottom, chin, mouthR, BELLY);
        facet(bottom, mouthL, chin, BELLY);
        // Gilded horns sweeping back
        facet(top, top.subtract(f.scale(.75)).add(u.scale(.6)).add(r.scale(.2)), top.add(f.scale(.2)).add(r.scale(.1)), GILT);
        facet(top, top.add(f.scale(.2)).subtract(r.scale(.1)), top.subtract(f.scale(.75)).add(u.scale(.6)).subtract(r.scale(.2)), GILT);
        // Glowing eyes on both sides of the head
        for (int s = -1; s <= 1; s += 2) {
            Vec3 c = neck.add(f.scale(.5)).add(u.scale(.13)).add(r.scale(.22 * s));
            Vec3 out = r.scale(.03 * s);
            eyes.add(new Facet(c.add(f.scale(.13)).add(out), c.subtract(f.scale(.1)).add(u.scale(.07)).add(out), c.subtract(f.scale(.1)).subtract(u.scale(.05)).add(out), EYE_COLOR));
        }
    }

    private void wings(Vec3 chest, Vec3 t, Vec3 r, Vec3 u, float age, OrigamiDragon entity) {
        Vec3 last = entity.getTrailPoint(0);
        Vec3 before = entity.getTrailPoint(1);
        double speed = last != null && before != null ? last.distanceTo(before) : 0;
        // Flaps hard when slow, glides with wings spread when fast
        float glide = (float) Mth.clamp((speed - .6) / .5, 0, 1);
        float beat = Mth.sin(age * (.32f - glide * .12f));
        float lift = Mth.lerp(glide, beat * .65f, -.08f + beat * .08f);
        float lift2 = lift + Mth.lerp(glide, Mth.sin(age * .32f - .9f) * .4f, 0f);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 side = r.scale(s);
            Vec3 shoulder = chest.add(t.scale(.35)).add(u.scale(.25)).add(side.scale(.35));
            Vec3 root = chest.subtract(t.scale(.9)).add(u.scale(.2)).add(side.scale(.3));
            Vec3 arm = side.scale(Mth.cos(lift)).add(u.scale(Mth.sin(lift)));
            Vec3 hand = side.scale(Mth.cos(lift2)).add(u.scale(Mth.sin(lift2)));
            Vec3 elbow = shoulder.add(arm.scale(2.2)).add(t.scale(.2));
            Vec3 tip = elbow.add(hand.scale(2.5)).subtract(t.scale(1.3));
            Vec3 trailing = root.add(arm.scale(1.5)).subtract(t.scale(.9));
            Vec3 scallop = trailing.add(hand.scale(.9)).subtract(t.scale(.55));
            facet(shoulder, elbow, root, WING);
            facet(root, elbow, trailing, WING);
            facet(elbow, tip, trailing, WING_TIP);
            facet(trailing, tip, scallop, WING_TIP);
        }
    }

    /** Points every {@link #SPACING} blocks back along the path it flew; straight on past the recorded part. */
    private static List<Vec3> followPath(OrigamiDragon entity, Vec3 origin, Vec3 heading) {
        List<Vec3> out = new ArrayList<>();
        Vec3 cursor = origin;
        double need = SPACING;
        int k = 0;
        while (out.size() < BODY_POINTS) {
            Vec3 next = entity.getTrailPoint(k);
            if (next == null) {
                Vec3 dir = heading.scale(-1);
                if (out.size() >= 2) {
                    Vec3 d = out.get(out.size() - 1).subtract(out.get(out.size() - 2));
                    if (d.lengthSqr() > 1.0E-4) {
                        dir = d.normalize();
                    }
                }
                cursor = cursor.add(dir.scale(need));
                out.add(cursor);
                need = SPACING;
                continue;
            }
            double d = next.distanceTo(cursor);
            if (d >= need) {
                cursor = cursor.add(next.subtract(cursor).scale(need / d));
                out.add(cursor);
                need = SPACING;
            } else {
                need -= d;
                cursor = next;
                k++;
            }
        }
        return out;
    }

    /** Right of a direction, kept steady when the direction points straight up or down. */
    private static Vec3 safeRight(Vec3 dir, Vec3 previous) {
        Vec3 r = dir.cross(UP);
        if (r.lengthSqr() < 1.0E-4) {
            return previous != null ? previous : new Vec3(1, 0, 0);
        }
        return r.normalize();
    }

    private void facet(Vec3 a, Vec3 b, Vec3 c, int color) {
        facets.add(new Facet(a, b, c, color));
    }

    /**
     * A triangle as a quad with its last corner doubled, lit by its own facet normal. Two triangles in a row make one
     * square of the paper texture ({@code first}: the lower-right half, else the upper-left one).
     */
    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, int color, int light, boolean first) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a));
        normal = normal.lengthSqr() < 1.0E-8 ? UP : normal.normalize();
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;
        RenderHelper.vertex(consumer, pose, (float) a.x, (float) a.y, (float) a.z, 0, 0, color, light, nx, ny, nz);
        RenderHelper.vertex(consumer, pose, (float) b.x, (float) b.y, (float) b.z, 1, first ? 0 : 1, color, light, nx, ny, nz);
        RenderHelper.vertex(consumer, pose, (float) c.x, (float) c.y, (float) c.z, first ? 1 : 0, 1, color, light, nx, ny, nz);
        RenderHelper.vertex(consumer, pose, (float) c.x, (float) c.y, (float) c.z, first ? 1 : 0, 1, color, light, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(OrigamiDragon entity) {
        return PAPER;
    }
}
