package com.github.squi2rel.freedraw.network;

import io.netty.buffer.ByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class IOUtil {
    public static String readString(ByteBuf buf, int maxLength) {
        int len = buf.readUnsignedShort();
        if (len > maxLength) throw new IllegalStateException(String.format("length(%d) exceeds max length(%d)", len, maxLength));
        byte[] data = new byte[len];
        buf.readBytes(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String str) {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(data.length);
        buf.writeBytes(data);
    }

    public static Quaternionf readQuaternion(ByteBuf buf) {
        return new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeQuaternion(ByteBuf buf, Quaternionf q) {
        buf.writeFloat(q.x);
        buf.writeFloat(q.y);
        buf.writeFloat(q.z);
        buf.writeFloat(q.w);
    }

    public static void writeVec3f(ByteBuf buf, Vector3f v) {
        buf.writeFloat(v.x);
        buf.writeFloat(v.y);
        buf.writeFloat(v.z);
    }

    public static void writeVec3f(DataOutput buf, Vector3f v) throws IOException {
        buf.writeFloat(v.x);
        buf.writeFloat(v.y);
        buf.writeFloat(v.z);
    }

    public static Vector3f readVec3f(ByteBuf buf) {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static Vector3f readVec3f(DataInput buf) throws IOException {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static Vector3d readVec3d(ByteBuf buf) {
        return new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static Vector3d readVec3d(DataInput buf) throws IOException {
        return new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void writeVec3d(ByteBuf buf, Vector3d v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    public static void writeVec3d(DataOutput buf, Vector3d v) throws IOException {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    public static UUID readUUID(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }
}
