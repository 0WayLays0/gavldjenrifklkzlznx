package com.mojang.blaze3d.vertex;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayDeque;
import java.util.Deque;

/** Render lab stand-in for Minecraft's PoseStack: a stack of position and normal matrices. */
public class PoseStack {
    private final Deque<Pose> poses = new ArrayDeque<>();

    public PoseStack() {
        poses.add(new Pose(new Matrix4f(), new Matrix3f()));
    }

    public void translate(double x, double y, double z) {
        translate((float) x, (float) y, (float) z);
    }

    public void translate(float x, float y, float z) {
        poses.getLast().pose.translate(x, y, z);
    }

    public void scale(float x, float y, float z) {
        Pose p = poses.getLast();
        p.pose.scale(x, y, z);
        if (x == y && y == z) {
            if (x < 0) {
                p.normal.scale(-1);
            }
            return;
        }
        float ix = 1 / x, iy = 1 / y, iz = 1 / z;
        float k = (float) Math.cbrt(ix * iy * iz);
        p.normal.scale(k * ix, k * iy, k * iz);
    }

    public void mulPose(Quaternionf q) {
        Pose p = poses.getLast();
        p.pose.rotate(q);
        p.normal.rotate(q);
    }

    public void rotateAround(Quaternionf q, float x, float y, float z) {
        Pose p = poses.getLast();
        p.pose.rotateAround(q, x, y, z);
        p.normal.rotate(q);
    }

    public void pushPose() {
        Pose p = poses.getLast();
        poses.addLast(new Pose(new Matrix4f(p.pose), new Matrix3f(p.normal)));
    }

    public void popPose() {
        poses.removeLast();
    }

    public Pose last() {
        return poses.getLast();
    }

    public void setIdentity() {
        Pose p = poses.getLast();
        p.pose.identity();
        p.normal.identity();
    }

    public void mulPose(Matrix4f m) {
        poses.getLast().pose.mul(m);
    }

    public static final class Pose {
        final Matrix4f pose;
        final Matrix3f normal;

        Pose(Matrix4f pose, Matrix3f normal) {
            this.pose = pose;
            this.normal = normal;
        }

        public Matrix4f pose() {
            return pose;
        }

        public Matrix3f normal() {
            return normal;
        }

        public Pose copy() {
            return new Pose(new Matrix4f(pose), new Matrix3f(normal));
        }
    }
}
