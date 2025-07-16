package com.github.squi2rel.freedraw.render;

import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class DirtyBuffer {
    public static boolean drawing;
    public VertexBuffer buffer;
    public boolean dirty = true;

    public DirtyBuffer() {
        buffer = new VertexBuffer(GlUsage.DYNAMIC_WRITE);
    }

    public void make() {
        buffer.close();
        buffer = new VertexBuffer(GlUsage.STATIC_WRITE);
        dirty = true;
    }

    public void draw(Matrix4f viewMatrix, Matrix4f projectionMatrix, @Nullable ShaderProgram program) {
        drawing = true;
        buffer.draw(viewMatrix, projectionMatrix, program);
        drawing = false;
    }

    public void bind() {
        buffer.bind();
    }

    public void close() {
        buffer.close();
    }
}
