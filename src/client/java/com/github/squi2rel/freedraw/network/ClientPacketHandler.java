package com.github.squi2rel.freedraw.network;

import com.github.squi2rel.freedraw.*;
import com.github.squi2rel.freedraw.brush.BrushPath;
import com.github.squi2rel.freedraw.brush.BrushPoint;
import com.github.squi2rel.freedraw.brush.ClientBrushPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.github.squi2rel.freedraw.FreeDraw.LOGGER;
import static com.github.squi2rel.freedraw.FreeDrawClient.paths;
import static com.github.squi2rel.freedraw.network.ByteBufUtils.writeString;
import static com.github.squi2rel.freedraw.network.PacketID.*;

public class ClientPacketHandler {
    private static final Vector3f v3f1 = new Vector3f(), v3f2 = new Vector3f();
    private static final Vector3d v3d1 =  new Vector3d(), v3d2 = new Vector3d();

    public static void handle(ByteBuf buf) {
        short type = buf.readUnsignedByte();
        switch (type) {
            case CONFIG -> {
                String version = ByteBufUtils.readString(buf, 16);
                if (!FreeDrawClient.checkVersion(version)) {
                    Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(Text.of("服务器FreeDraw版本和本地版本不匹配! 本地版本为" + FreeDraw.version + ", 服务器版本为" + version), false);
                    return;
                }
                FreeDrawClient.brushItem = Registries.ITEM.get(Identifier.of(ByteBufUtils.readString(buf, 256)));
                FreeDrawClient.brushIdStart = buf.readFloat();
                FreeDrawClient.brushIdEnd = buf.readFloat();
                FreeDrawClient.brushQuat = ByteBufUtils.readQuaternion(buf);
                FreeDrawClient.brushLength = buf.readFloat();
                FreeDrawClient.eraserItem = Registries.ITEM.get(Identifier.of(ByteBufUtils.readString(buf, 256)));
                FreeDrawClient.eraserId = buf.readFloat();
                FreeDrawClient.eraserQuat = ByteBufUtils.readQuaternion(buf);
                FreeDrawClient.eraserLength = buf.readFloat();
                FreeDrawClient.maxPoints = buf.readInt();
                FreeDrawClient.uploadInterval = buf.readInt();
                FreeDrawClient.color = buf.readInt();
                FreeDrawClient.connected = true;
                config(FreeDraw.version);
            }
            case NEW_PATH -> {
                UUID old = ByteBufUtils.readUUID(buf);
                UUID uuid = ByteBufUtils.readUUID(buf);
                ClientBrushPath path = paths.remove(old);
                path.uuid = uuid;
                paths.put(uuid, path);
                for (BrushPoint point : path.points) {
                    point.color = FreeDrawClient.color;
                }
                addPoints(uuid, path.getNewPoints(), false);
                path.created();
            }
            case CREATE_PATH -> {
                UUID uuid = ByteBufUtils.readUUID(buf);
                ClientBrushPath old = paths.get(uuid);
                if (old != null) old.remove();
                paths.put(uuid, new ClientBrushPath(uuid, ByteBufUtils.readVec3d(buf)));
            }
            case REMOVE_PATH -> {
                ClientBrushPath path = paths.remove(ByteBufUtils.readUUID(buf));
                if (path == null) return;
                path.remove();
                if (FreeDrawClient.currentPath == path) FreeDrawClient.currentPath = null;
            }
            case ADD_POINTS -> {
                ClientBrushPath path = paths.get(ByteBufUtils.readUUID(buf));
                int size = buf.readInt();
                Vector3f prev;
                if (path.points.isEmpty()) {
                    prev = ByteBufUtils.readVec3(buf, v3f1);
                    buf.skipBytes(4);
                    Vector3f now = ByteBufUtils.readVec3(buf, v3f2);
                    buf.readerIndex(buf.readerIndex() - 16);
                    path.addFirst(v3d1.set(prev), v3d2.set(now), buf.readInt());
                    size--;
                } else {
                    prev = path.points.getLast().pos;
                }
                for (int i = 0; i < size; i++) {
                    Vector3f now = ByteBufUtils.readVec3(buf, v3f2);
                    path.addRaw(v3d1.set(prev), v3d2.set(now), buf.readInt(), Integer.MAX_VALUE);
                    prev.set(now);
                }
                if (buf.readBoolean()) path.cache();
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
        ByteBufUtils.writeUUID(buf, path.uuid);
        ByteBufUtils.writeVec3d(buf, path.offset);
        send(toByteArray(buf));
    }

    public static void removePath(UUID uuid) {
        send(ServerPacketHandler.removePath(uuid));
    }

    public static void addPoints(UUID uuid, List<BrushPoint> points, boolean finalize) {
        ByteBuf buf = create(ADD_POINTS);
        ByteBufUtils.writeUUID(buf, uuid);
        buf.writeInt(points.size());
        for (BrushPoint point : points) {
            BrushPoint.write(buf, point);
        }
        buf.writeBoolean(finalize);
        send(toByteArray(buf));
    }
}
