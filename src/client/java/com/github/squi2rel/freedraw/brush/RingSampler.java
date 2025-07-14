package com.github.squi2rel.freedraw.brush;

import org.joml.Vector3d;

public class RingSampler {
    private static final Vector3d tmp1 = new Vector3d(), tmp2 = new Vector3d(), tmp3 = new Vector3d(), tmp4 = new Vector3d();
    public static final int SEGMENTS = 10;
    private final float radius;
    private Vector3d lastU = null;
    private Vector3d lastTangent = null;

    public RingSampler(float radius) {
        this.radius = radius;
    }

    public float[] sample(Vector3d prev, Vector3d now) {
        Vector3d tangent = now.sub(prev, tmp1).normalize();

        if (lastU == null || lastTangent == null) {
            lastU = getU(tangent);
            lastTangent = new Vector3d(tangent);
        } else {
            Vector3d v = lastTangent.cross(lastU).normalize();
            lastU.set(v.cross(tangent).normalize());
            lastTangent.set(tangent);
        }

        Vector3d u = lastU;
        Vector3d v = tangent.cross(u).normalize();

        float[] vertices = new float[SEGMENTS * 3];
        for (int i = 0; i < SEGMENTS; i++) {
            double ang = 2 * Math.PI * i / SEGMENTS;
            Vector3d offset = u.mul(Math.cos(ang) * radius, tmp2)
                    .add(v.mul(Math.sin(ang) * radius, tmp3), tmp3);
            Vector3d out = now.add(offset, tmp4);
            int idx = i * 3;
            vertices[idx] = (float) out.x;
            vertices[idx + 1] = (float) out.y;
            vertices[idx + 2] = (float) out.z;
        }
        return vertices;
    }

    private static Vector3d getU(Vector3d dir) {
        Vector3d ref = Math.abs(dir.x) < 0.999 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        return dir.cross(ref, ref).normalize();
    }
}
