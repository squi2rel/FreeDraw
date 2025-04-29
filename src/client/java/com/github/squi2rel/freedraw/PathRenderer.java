package com.github.squi2rel.freedraw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.github.squi2rel.freedraw.RingSampler.SEGMENTS;

public class PathRenderer {
    public static List<BrushPath> paths = new ArrayList<>();
    public static BrushPath path = null;
    private static Vec3d prevPos = null;

    public static void register() {
        int steps = 360;
        int[] argbColors = new int[steps];

        for (int i = 0; i < steps; i++) {
            float hue = i / 360f;
            float saturation = 1.0f;
            float brightness = 1.0f;
            Color color = Color.getHSBColor(hue, saturation, brightness);
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            int a = 255;
            int argb = ((a & 0xFF) << 24) |
                    ((r & 0xFF) << 16) |
                    ((g & 0xFF) << 8) |
                    (b & 0xFF);

            argbColors[i] = argb;
        }
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (client.options.useKey.isPressed()) {
                if (path == null) {
                    path = new BrushPath();
                    paths.add(path);
                }
                Vec3d drawPoint;
                if (VivecraftApi.vivecraftLoaded() && VivecraftApi.isVRActive()) {
                    drawPoint = VivecraftApi.getHandPosition();
                } else {
                    float delta = ctx.tickCounter().getTickDelta(true);
                    Vec3d eyePos = client.player.getCameraPosVec(delta);
                    Vec3d lookVec = client.player.getRotationVec(delta);
                    drawPoint = eyePos.add(lookVec.multiply(5));
                }
                if (prevPos != null && prevPos.subtract(drawPoint).length() < 0.05) return;
                if (prevPos != null) path.add(prevPos, drawPoint, 1, 0, 0);
                prevPos = drawPoint;
            } else if (path != null) {
                path = null;
                prevPos = null;
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            for (BrushPath path : paths) {
                if (path.points.size() < 2) continue;
                draw(path, ctx, argbColors);
            }
        });
    }

    private static void draw(BrushPath path, WorldRenderContext ctx, int[] argbColors) {
        if (path.dirty) {
            Matrix4f mat = ctx.projectionMatrix();
            List<BrushPath.BrushPoint> points = path.points;
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            drawFan(points.getFirst(), mat, argbColors[10]);
            drawFan(points.getLast(), mat, argbColors[points.size() % 360]);
            for (int i = 0, l = points.size() - 1; i < l; i++) {
                Vec3d[] A = points.get(i).vertices;
                Vec3d[] B = points.get(i + 1).vertices;
                for (int j = 0; j <= SEGMENTS; j++) {
                    int idx = j % SEGMENTS;
                    buf.vertex((float) B[idx].x, (float) B[idx].y, (float) B[idx].z).color(argbColors[((i + 1) * 10) % 360]);
                    buf.vertex((float) A[idx].x, (float) A[idx].y, (float) A[idx].z).color(argbColors[(i * 10) % 360]);
                }
            }
            path.buffer.bind();
            path.buffer.upload(buf.end());
            path.dirty = false;
        }
        path.buffer.bind();
        Vec3d camPos = ctx.camera().getPos();
        MatrixStack ms = ctx.matrixStack();
        ms.push();
        ms.multiplyPositionMatrix(ctx.positionMatrix());
        ms.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = ms.peek().getPositionMatrix();
        ms.pop();
        path.buffer.draw(mat, ctx.projectionMatrix(), RenderSystem.getShader());
    }

    private static void drawFan(BrushPath.BrushPoint point, Matrix4f mat, int color) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Vec3d pos = point.pos;
        buf.vertex(mat, (float) pos.x, (float) pos.y, (float) pos.z).color(color);
        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3d vertex = point.vertices[i % SEGMENTS];
            buf.vertex(mat, (float) vertex.x, (float) vertex.y, (float) vertex.z).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
