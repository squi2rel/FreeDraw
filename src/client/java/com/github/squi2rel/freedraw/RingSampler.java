package com.github.squi2rel.freedraw;

import net.minecraft.util.math.Vec3d;

public class RingSampler {
    public static final int SEGMENTS = 12;

    public static Ring sample(Vec3d prev, Vec3d now, float radius) {
        Vec3d tangent = now.subtract(prev).normalize();
        double proj = prev.dotProduct(tangent);
        Vec3d u = prev.subtract(tangent.multiply(proj)).normalize();
        Vec3d v = tangent.crossProduct(u).normalize();

        Vec3d[] verts = new Vec3d[SEGMENTS];
        for (int i = 0; i < SEGMENTS; i++) {
            double ang = 2 * Math.PI * i / SEGMENTS;
            Vec3d offset = u.multiply(Math.cos(ang) * radius)
                    .add(v.multiply(Math.sin(ang) * radius));
            verts[i] = now.add(offset);
        }
        return new Ring(now, verts);
    }

    public record Ring(Vec3d center, Vec3d[] vertices) {}
}
