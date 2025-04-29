package com.github.squi2rel.freedraw;

import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class BrushPath {
    public ArrayList<BrushPoint> points = new ArrayList<>();
    public boolean dirty = false;
    public VertexBuffer buffer = new VertexBuffer(GlUsage.DYNAMIC_WRITE);

    public void add(Vec3d prev, Vec3d now, float r, float g, float b) {
        Vec3d tangent = now.subtract(prev);
        double dist = tangent.length();
        if (dist < 1e-6) return;
        Vec3d p = prev, tmp = prev;
        int steps = (int) (dist / 0.1);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / (steps + 1);
            p = prev.lerp(now, t);
            points.add(new BrushPoint(RingSampler.sample(tmp, p, 0.01f).vertices(), p, r, g, b, true));
            tmp = p;
        }
        points.add(new BrushPoint(RingSampler.sample(p, now, 0.01f).vertices(), now, r, g, b, false));
        dirty = true;
    }

    public void thickness(float th) {
        points.getLast().thickness = th;
    }

    public static class BrushPoint {
        public Vec3d[] vertices;
        public Vec3d pos;
        public float r, g, b;
        public float thickness = 1;
        public boolean isGenerated;

        public BrushPoint(Vec3d[] vertices, Vec3d pos, float r, float g, float b, boolean isGenerated) {
            this.vertices = vertices;
            this.pos = pos;
            this.r = r;
            this.g = g;
            this.b = b;
            this.isGenerated = isGenerated;
        }
    }
}
