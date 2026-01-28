package com.github.squi2rel.freedraw.render;

import org.joml.Vector3d;
import org.joml.Vector3f;

public class RenderNode {
    public final Vector3d pos = new Vector3d();
    public final Vector3f tangent = new Vector3f();
    public final Vector3f normal = new Vector3f();
    public final Vector3f binormal = new Vector3f();
    public final int color;

    public RenderNode(Vector3d p, Vector3f t, Vector3f n, int c) {
        this.pos.set(p);
        this.tangent.set(t);
        this.normal.set(n);
        this.tangent.cross(n, this.binormal).normalize();
        this.color = c;
    }
}