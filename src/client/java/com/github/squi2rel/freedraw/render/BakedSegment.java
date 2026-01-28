package com.github.squi2rel.freedraw.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record BakedSegment(VertexBuffer buffer, Vector3f endTangent, Vector3f endNormal) {
    public void close() {
        buffer.close();
    }

    public void draw(Matrix4f view, Matrix4f projection) {
        buffer.bind();
        buffer.draw(view, projection, RenderSystem.getShader());
    }
}