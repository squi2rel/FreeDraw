package com.github.squi2rel.freedraw.brush;

import org.joml.Vector3d;

public class RingSampler {
    private static final Vector3d tmp1 = new Vector3d(), tmp2 = new Vector3d(),  tmp3 = new Vector3d(),  tmp4 = new Vector3d();
    public static final int SEGMENTS = 12;

    public static float[] sample(Vector3d prev, Vector3d now, float radius) {
        Vector3d tangent = now.sub(prev, tmp1).normalize();
        double proj = prev.dot(tangent);
        Vector3d u = prev.sub(tangent.mul(proj, tmp2), tmp3).normalize();
        Vector3d v = tangent.cross(u, tmp2).normalize();

        float[] vertices = new float[SEGMENTS * 3];
        for (int i = 0; i < SEGMENTS; i++) {
            double ang = 2 * Math.PI * i / SEGMENTS;
            Vector3d offset = u.mul(Math.cos(ang) * radius, tmp1)
                    .add(v.mul(Math.sin(ang) * radius, tmp4));
            Vector3d out = now.add(offset, tmp4);
            int idx = i * 3;
            vertices[idx] = (float) out.x;
            vertices[idx + 1] = (float) out.y;
            vertices[idx + 2] = (float) out.z;
        }
        return vertices;
    }
}
