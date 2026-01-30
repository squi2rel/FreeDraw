package com.github.squi2rel.freedraw.render;

import com.github.squi2rel.freedraw.FreeDraw;
import com.github.squi2rel.freedraw.brush.ClientBrushPath;
import com.github.squi2rel.freedraw.FreeDrawClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.nio.file.Path;
import java.util.*;

import static com.github.squi2rel.freedraw.FreeDrawClient.*;

public class PathRenderer {
    private static final Identifier WHITE = Identifier.of(FreeDraw.MOD_ID, "white");
    private static final RenderLayer BRUSH_PATH = RenderLayer.of(
            "brush_path",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            32768,
            false,
            false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(new RenderPhase.ShaderProgram(ShaderProgramKeys.POSITION_TEX_COLOR))
                    .texture(new RenderPhase.Texture(WHITE, TriState.FALSE, false))
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .build(false)
    );
    private static boolean registered = false;

    private static final float[] COS_TABLE = {1.0f, 0.0f, -1.0f, 0.0f, 1.0f};
    private static final float[] SIN_TABLE = {0.0f, 1.0f, 0.0f, -1.0f, 0.0f};

    private static VertexBuffer buffer;

    private static final List<DirtyBuffer> buffers = new ArrayList<>();
    private static final int BUFFER_THRESHOLD = ClientBrushPath.BAKE_THRESHOLD * 2;
    private static final float COMPACT_THRESHOLD = 0.8f;
    private static int drawCalls = 0;

    public static boolean drawing;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (!FreeDrawClient.connected) return;

            if (!registered) {
                MinecraftClient.getInstance().getTextureManager().registerTexture(WHITE, new WhiteTexture());
                registered = true;
            }
            BRUSH_PATH.startDrawing();
            draw(paths.values(), ctx);
            BRUSH_PATH.endDrawing();
        });
    }

    public static BufferBuilder getBufferBuilder() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    private static void draw(Collection<ClientBrushPath> paths, WorldRenderContext ctx) {
        if (buffer == null) buffer = new VertexBuffer(GlUsage.DYNAMIC_WRITE);
        BufferBuilder builder = getBufferBuilder();

        int dynamicNodes = 0;
        for (ClientBrushPath path : paths) {
            if (path.points.size() < 2 || path.dynamicBuffer != null) continue;
            draw(path, ctx, getMatrix(ctx, path), builder);
            if (path.finalized && path.dynamicNodes != null) dynamicNodes += path.dynamicNodes.size();
        }
        BuiltBuffer dynamic = builder.endNullable();

        drawing = true;

        if (drawCalls++ >= 60) {
            compactBuffers();
            drawCalls = 0;
        }
        BufferQueue.processQueue();

        Matrix4f posMat = Objects.requireNonNull(ctx.matrixStack()).peek().getPositionMatrix();
        Matrix4f projMat = ctx.projectionMatrix();
        if (dynamic != null) {
            buffer.bind();
            buffer.upload(dynamic);
            buffer.draw(posMat, projMat, RenderSystem.getShader());
        }

        for (DirtyBuffer buf : buffers) {
            if (buf.getPaths().isEmpty()) continue;
            buf.draw(getMatrix(ctx, buf.getPaths().getFirst()), projMat);
        }

        drawing = false;

        if (dynamicNodes > BUFFER_THRESHOLD) {
            for (ClientBrushPath path : paths) {
                if (shouldBuffer(path)) getBuffer().add(path);
            }
        }
    }

    private static boolean shouldBuffer(ClientBrushPath path) {
        return path.finalized && path.dynamicNodes != null && path.dynamicBuffer == null;
    }

    private static Matrix4f getMatrix(WorldRenderContext ctx, ClientBrushPath path) {
        MatrixStack ms = Objects.requireNonNull(ctx.matrixStack());
        ms.push();
        ms.multiplyPositionMatrix(ctx.positionMatrix());
        Vec3d camPos = ctx.camera().getPos();
        ms.translate(path.offset.x - camPos.x, path.offset.y - camPos.y, path.offset.z - camPos.z);
        Matrix4f matrix = ms.peek().getPositionMatrix();
        ms.pop();
        return matrix;
    }

    private static DirtyBuffer getBuffer() {
        for (DirtyBuffer buf : buffers) {
            if (buf.getPoints() < BUFFER_THRESHOLD) {
                return buf;
            }
        }
        DirtyBuffer buf = new DirtyBuffer();
        buffers.add(buf);
        return buf;
    }

    public static void compactBuffers() {
        if (buffers.size() < 2) return;

        long totalNodes = 0;
        for (DirtyBuffer buf : buffers) {
            totalNodes += buf.getPoints();
        }

        float efficiency = (float) totalNodes / ((buffers.size() - 1) * BUFFER_THRESHOLD);

        if (efficiency > COMPACT_THRESHOLD) {
            return;
        }

        for (int i = 0; i < buffers.size(); i++) {
            DirtyBuffer current = buffers.get(i);

            while (i + 1 < buffers.size()) {
                DirtyBuffer next = buffers.get(i + 1);

                List<ClientBrushPath> nextPaths = next.getPaths();
                boolean movedAny = false;

                Iterator<ClientBrushPath> it = nextPaths.iterator();
                while (it.hasNext()) {
                    ClientBrushPath path = it.next();
                    int pathSize = path.dynamicNodes.size();

                    if (current.getPoints() + pathSize <= BUFFER_THRESHOLD) {
                        it.remove();
                        current.add(path);
                        movedAny = true;
                    } else {
                        break;
                    }
                }

                if (next.getPaths().isEmpty()) {
                    next.close();
                    buffers.remove(i + 1);
                } else if (!movedAny) {
                    break;
                }
            }

            if (buffers.size() == 1 && buffers.getFirst().getPaths().isEmpty()) {
                buffers.removeFirst().close();
            }
        }
    }

    private static void draw(ClientBrushPath path, WorldRenderContext ctx, Matrix4f matrix, BufferBuilder builder) {
        if (path == null) return;

        drawing = true;
        for (BakedSegment segment : path.bakedSegments) {
            segment.draw(matrix, ctx.projectionMatrix());
        }
        drawing = false;
        if (path.dynamicNodes != null) draw(builder, matrix, path.dynamicNodes, 0.01f, path.isFirstDraw(), true);
    }

    public static VertexBuffer bake(List<RenderNode> nodes, boolean startCap, boolean endCap) {
        BufferBuilder builder = getBufferBuilder();
        draw(builder, nodes, 0.01f, startCap, endCap);
        VertexBuffer buffer = new VertexBuffer(GlUsage.STATIC_WRITE);
        buffer.bind();
        buffer.upload(builder.end());
        return buffer;
    }

    public static void draw(BufferBuilder buf, List<RenderNode> nodes, float radius, boolean startCap, boolean endCap) {
        draw(buf, new Matrix4f(), nodes, radius, startCap, endCap);
    }

    public static void draw(VertexConsumer vc, Matrix4f mat, List<RenderNode> nodes, float radius, boolean startCap, boolean endCap) {
        if (nodes.size() < 2) return;

        RenderNode first = nodes.getFirst();
        RenderNode last = nodes.getLast();

        float s0 = SIN_TABLE[0] * radius;
        float c0 = COS_TABLE[0] * radius;

        if (startCap) {
            submitVertex(vc, mat, first, c0, s0, first.color);
            submitVertex(vc, mat, first, c0, s0, first.color);

            for (int j = 0; j < 5; j++) {
                submitVertex(vc, mat, first, COS_TABLE[j] * radius, SIN_TABLE[j] * radius, first.color);
                submitVertex(vc, mat, first, -radius, first.color);
            }
        } else {
            submitVertex(vc, mat, first, c0, s0, first.color);
            submitVertex(vc, mat, first, c0, s0, first.color);
        }

        for (int i = 0; i < nodes.size() - 1; i++) {
            RenderNode n1 = nodes.get(i);
            RenderNode n2 = nodes.get(i + 1);

            if (i > 0) {
                submitVertex(vc, mat, n1, c0, s0, n1.color);
                submitVertex(vc, mat, n1, c0, s0, n1.color);
            }

            for (int j = 0; j < 5; j++) {
                float c = COS_TABLE[j] * radius;
                float s = SIN_TABLE[j] * radius;

                submitVertex(vc, mat, n2, c, s, n2.color);
                submitVertex(vc, mat, n1, c, s, n1.color);
            }
        }

        if (endCap) {
            submitVertex(vc, mat, last, c0, s0, last.color);
            submitVertex(vc, mat, last, c0, s0, last.color);

            for (int j = 0; j < 5; j++) {
                submitVertex(vc, mat, last, radius, last.color);
                submitVertex(vc, mat, last, COS_TABLE[j] * radius, SIN_TABLE[j] * radius, last.color);
            }
        }

        float c4 = COS_TABLE[4] * radius;
        float s4 = SIN_TABLE[4] * radius;
        submitVertex(vc, mat, last, c4, s4, last.color);
        submitVertex(vc, mat, last, c4, s4, last.color);
    }

    private static void submitVertex(VertexConsumer vc, Matrix4f mat, RenderNode node, float tO, int color) {
        float x = Math.fma(node.tangent.x, tO, (float) node.pos.x);
        float y = Math.fma(node.tangent.y, tO, (float) node.pos.y);
        float z = Math.fma(node.tangent.z, tO, (float) node.pos.z);
        vc.vertex(mat, x, y, z).texture(0, 0).color(color);
    }

    private static void submitVertex(VertexConsumer vc, Matrix4f mat, RenderNode node, float nO, float bO, int color) {
        float x = Math.fma(node.normal.x, nO, (float) node.pos.x);
        x = Math.fma(node.binormal.x, bO, x);
        float y = Math.fma(node.normal.y, nO, (float) node.pos.y);
        y = Math.fma(node.binormal.y, bO, y);
        float z = Math.fma(node.normal.z, nO, (float) node.pos.z);
        z = Math.fma(node.binormal.z, bO, z);
        vc.vertex(mat, x, y, z).texture(0, 0).color(color);
    }

    public static void reset() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }

        for (DirtyBuffer buf : buffers) buf.close();
        buffers.clear();

        BufferQueue.clear();
    }

    private static class WhiteTexture extends NativeImageBackedTexture {
        public WhiteTexture() {
            super(createImage());
        }

        @Override
        public void upload() {
            if (getImage() == null) return;
            super.upload();
            setImage(null);
        }

        @Override
        public void close() {
            clearGlId();
        }

        @Override
        public void save(Identifier id, Path path) {
        }

        private static NativeImage createImage() {
            NativeImage image = new NativeImage(1, 1, false);
            image.setColorArgb(0, 0, 0xFFFFFFFF);
            return image;
        }
    }
}
