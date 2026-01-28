package com.github.squi2rel.freedraw.network;

import com.github.squi2rel.freedraw.*;
import com.github.squi2rel.freedraw.brush.BrushPath;
import com.github.squi2rel.freedraw.brush.ClientBrushPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.github.squi2rel.freedraw.FreeDraw.LOGGER;
import static com.github.squi2rel.freedraw.FreeDrawClient.paths;
import static com.github.squi2rel.freedraw.network.IOUtil.writeString;
import static com.github.squi2rel.freedraw.network.PacketID.*;

public class ClientPacketHandler {

    public synchronized static void handle(ByteBuf buf) {
        short type = buf.readUnsignedByte();
        switch (type) {
            case CONFIG -> {
                String version = IOUtil.readString(buf, 16);
                if (!FreeDraw.checkVersion(version)) {
                    Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(Text.of("服务器FreeDraw版本和本地版本不匹配! 本地版本为" + FreeDraw.version + ", 服务器版本为" + version), false);
                    return;
                }
                FreeDrawClient.brushItem = Registries.ITEM.get(Identifier.of(IOUtil.readString(buf, 256)));
                FreeDrawClient.brushIdStart = buf.readFloat();
                FreeDrawClient.brushIdEnd = buf.readFloat();
                FreeDrawClient.brushQuat = IOUtil.readQuaternion(buf);
                FreeDrawClient.brushLength = buf.readFloat();
                FreeDrawClient.eraserItem = Registries.ITEM.get(Identifier.of(IOUtil.readString(buf, 256)));
                FreeDrawClient.eraserId = buf.readFloat();
                FreeDrawClient.eraserQuat = IOUtil.readQuaternion(buf);
                FreeDrawClient.eraserLength = buf.readFloat();
                FreeDrawClient.maxPoints = buf.readInt();
                FreeDrawClient.uploadInterval = buf.readInt();
                FreeDrawClient.color = buf.readInt();
                FreeDrawClient.desktopRange = buf.readFloat();
                FreeDrawClient.connected = true;
                config(FreeDraw.version);
            }
            case NEW_PATH -> {
                UUID old = IOUtil.readUUID(buf);
                UUID uuid = IOUtil.readUUID(buf);
                int color = buf.readInt();
                ClientBrushPath path = paths.remove(old);
                path.uuid = uuid;
                path.color = color;
                paths.put(uuid, path);
                addPoints(uuid, path.getNewPoints(), false);
                path.created();
            }
            case CREATE_PATH -> {
                UUID uuid = IOUtil.readUUID(buf);
                ClientBrushPath old = paths.get(uuid);
                if (old != null) MinecraftClient.getInstance().execute(old::remove);
                paths.put(uuid, new ClientBrushPath(uuid, IOUtil.readVec3d(buf), buf.readInt(), buf.readBoolean()));
            }
            case REMOVE_PATH -> {
                ClientBrushPath path = paths.remove(IOUtil.readUUID(buf));
                if (path == null) return;
                MinecraftClient.getInstance().execute(path::remove);
                if (FreeDrawClient.currentPath == path) FreeDrawClient.currentPath = null;
            }
            case ADD_POINTS -> {
                ClientBrushPath path = paths.get(IOUtil.readUUID(buf));
                int size = buf.readInt();
                for (int i = 0; i < size; i++) {
                    path.addRawPoint(buf.readFloat(), buf.readFloat(), buf.readFloat());
                }
                if (buf.readBoolean()) MinecraftClient.getInstance().execute(path::cache);
            }
            case MAX_POINTS -> FreeDrawClient.maxPoints = buf.readInt();
            case COLOR -> FreeDrawClient.color = buf.readInt();
            default -> LOGGER.warn("Unknown packet type: {}", type);
        }
        if (buf.readableBytes() > 0) {
            LOGGER.warn("Bytes remaining: {}, type {}", buf.readableBytes(), type);
        }
    }

    private static ByteBuf create(int id) {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
        buf.writeByte((byte) id);
        return buf;
    }

    private static byte[] toByteArray(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    private static void send(byte[] bytes) {
        ClientPlayNetworking.send(new DrawPayload(bytes));
    }

    public static void config(String version) {
        ByteBuf buf = create(CONFIG);
        writeString(buf, version);
        send(toByteArray(buf));
    }

    public static void newPath(BrushPath path) {
        ByteBuf buf = create(NEW_PATH);
        IOUtil.writeUUID(buf, path.uuid);
        IOUtil.writeVec3d(buf, path.offset);
        buf.writeInt(path.color);
        send(toByteArray(buf));
    }

    public static void removePath(UUID uuid) {
        send(ServerPacketHandler.removePath(uuid));
    }

    public static void addPoints(UUID uuid, List<Vector3f> points, boolean finalize) {
        ByteBuf buf = create(ADD_POINTS);
        IOUtil.writeUUID(buf, uuid);
        buf.writeInt(points.size());
        for (Vector3f point : points) {
            IOUtil.writeVec3f(buf, point);
        }
        buf.writeBoolean(finalize);
        send(toByteArray(buf));
    }
}
