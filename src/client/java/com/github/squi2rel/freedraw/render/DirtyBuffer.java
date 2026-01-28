package com.github.squi2rel.freedraw.render;

import com.github.squi2rel.freedraw.brush.ClientBrushPath;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class DirtyBuffer {
    private final VertexBuffer buffer;
    private final List<ClientBrushPath> paths = new ArrayList<>();

    public boolean dirty = true, drawable = false;
    private int points = 0;

    public DirtyBuffer() {
        buffer = new VertexBuffer(GlUsage.STATIC_WRITE);
    }

    public void add(ClientBrushPath path) {
        paths.add(path);
        path.dynamicBuffer = this;
        dirty = true;
        points += path.dynamicNodes.size();
        BufferQueue.markDirty(this);
    }

    public void remove(ClientBrushPath path) {
        if (paths.remove(path)) {
            path.dynamicBuffer = null;
            points -= path.dynamicNodes.size();
            dirty = true;
            BufferQueue.markDirty(this);
        }
    }

    public void draw(Matrix4f posMatrix, Matrix4f projMatrix) {
        if (drawable) {
            buffer.bind();
            buffer.draw(posMatrix, projMatrix, RenderSystem.getShader());
        }
    }

    public int getPoints() {
        return points;
    }

    public void close() {
        buffer.close();
    }

    public void rebuild() {
        if (!dirty) return;

        if (paths.isEmpty()) {
            drawable = false;
            dirty = false;
            return;
        }

        BufferBuilder builder = PathRenderer.getBufferBuilder();
        Vector3d o = paths.getFirst().offset;
        for (ClientBrushPath path : paths) {
            List<RenderNode> nodes = path.dynamicNodes;
            if (nodes == null) continue;
            Vector3d d = path.offset;
            PathRenderer.draw(builder, getTranslate(d, o), nodes, 0.01f, path.isFirstDraw(), true);
        }

        BuiltBuffer built = builder.endNullable();
        if (built != null) {
            buffer.bind();
            buffer.upload(built);
            drawable = true;
        } else {
            drawable = false;
        }
        dirty = false;
    }

    public List<ClientBrushPath> getPaths() {
        return paths;
    }

    private static Matrix4f getTranslate(Vector3d d, Vector3d o) {
        return new Matrix4f().translate((float) (d.x - o.x), (float) (d.y - o.y), (float) (d.z - o.z));
    }
}
