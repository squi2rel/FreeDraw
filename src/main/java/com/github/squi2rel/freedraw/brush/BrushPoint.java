package com.github.squi2rel.freedraw.brush;

import com.github.squi2rel.freedraw.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import org.joml.Vector3f;

public class BrushPoint {
    public float[] vertices;
    public Vector3f pos;
    public int color;
    public boolean isGenerated;

    public BrushPoint(float[] vertices, Vector3f pos, int color, boolean isGenerated) {
        this.vertices = vertices;
        this.pos = pos;
        this.color = color;
        this.isGenerated = isGenerated;
    }

    public static void write(ByteBuf buf, BrushPoint point) {
        ByteBufUtils.writeVec3(buf, point.pos);
        buf.writeInt(point.color);
    }

    public static BrushPoint read(ByteBuf buf) {
        return new BrushPoint(null, ByteBufUtils.readVec3(buf), buf.readInt(), false);
    }
}
