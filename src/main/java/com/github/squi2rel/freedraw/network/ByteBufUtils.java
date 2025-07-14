package com.github.squi2rel.freedraw.network;

import io.netty.buffer.ByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ByteBufUtils {
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

    public static Vector3f readVec3(ByteBuf buf) {
        return readVec3(buf, new Vector3f());
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

    public static Vector3f readVec3(ByteBuf buf, Vector3f out) {
        return out.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeVec3(ByteBuf buf, Vector3f v) {
        buf.writeFloat(v.x);
        buf.writeFloat(v.y);
        buf.writeFloat(v.z);
    }

    public static Vector3f readVec3(DataInputStream buf, Vector3f out) throws IOException {
        return out.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeVec3(DataOutputStream buf, Vector3f v) throws IOException {
        buf.writeFloat(v.x);
        buf.writeFloat(v.y);
        buf.writeFloat(v.z);
    }

    public static Vector3d readVec3d(ByteBuf buf) {
        return readVec3d(buf, new Vector3d());
    }

    public static Vector3d readVec3d(ByteBuf buf, Vector3d out) {
        return out.set(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static Vector3d readVec3d(DataInputStream buf, Vector3d out) throws IOException {
        return out.set(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void writeVec3d(ByteBuf buf, Vector3d v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    public static void writeVec3d(DataOutputStream buf, Vector3d v) throws IOException {
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
