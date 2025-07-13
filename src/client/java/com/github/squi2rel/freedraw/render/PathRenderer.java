package com.github.squi2rel.freedraw.render;

import com.github.squi2rel.freedraw.brush.BrushPoint;
import com.github.squi2rel.freedraw.brush.ClientBrushPath;
import com.github.squi2rel.freedraw.FreeDrawClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Objects;

import static com.github.squi2rel.freedraw.FreeDrawClient.*;
import static com.github.squi2rel.freedraw.brush.RingSampler.SEGMENTS;

@SuppressWarnings("resource")
public class PathRenderer {
    private static final RenderLayer BRUSH_PATH = RenderLayer.of(
            "brush_path",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            32768,
            true,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(new RenderPhase.ShaderProgram(ShaderProgramKeys.POSITION_COLOR))
                    .build(true)
    );

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (!FreeDrawClient.connected) return;
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            for (ClientBrushPath path : paths.values()) {
                if (path.points.size() < 2) continue;
                draw(path, ctx);
            }
        });
    }

    private static void build(List<BrushPoint> points, VertexBuffer buffer) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0, l = points.size() - 1; i < l; i++) {
            BrushPoint p1 = points.get(i);
            BrushPoint p2 = points.get(i + 1);
            float[] A = p1.vertices;
            float[] B = p2.vertices;
            for (int j = 0; j <= SEGMENTS; j++) {
                int idx = j % SEGMENTS * 3;
                buf.vertex(B[idx], B[idx + 1], B[idx + 2]).color(p2.color);
                buf.vertex(A[idx], A[idx + 1], A[idx + 2]).color(p1.color);
            }
        }
        buffer.bind();
        buffer.upload(buf.end());
    }

    private static void draw(ClientBrushPath path, WorldRenderContext ctx) {
        for (int i = 0; i < path.buffers.size(); i++) {
            DirtyBuffer buffer = path.buffers.get(i);
            if (buffer.dirty) {
                List<BrushPoint> points = path.getRange(i);
                if (points.size() < 2) continue;
                build(points, buffer.buffer);
                buffer.dirty = false;
            }
        }
        Vec3d camPos = ctx.camera().getPos();
        MatrixStack ms = ctx.matrixStack();
        Objects.requireNonNull(ms).push();
        ms.multiplyPositionMatrix(ctx.positionMatrix());
        ms.translate(path.offset.x - camPos.x, path.offset.y - camPos.y, path.offset.z - camPos.z);
        Matrix4f mat = ms.peek().getPositionMatrix();
        ms.pop();
        for (DirtyBuffer buffer : path.buffers) {
            if (buffer.dirty) continue;
            BRUSH_PATH.startDrawing();
            buffer.bind();
            buffer.draw(mat, ctx.projectionMatrix(), RenderSystem.getShader());
            BRUSH_PATH.endDrawing();
        }
    }
}
