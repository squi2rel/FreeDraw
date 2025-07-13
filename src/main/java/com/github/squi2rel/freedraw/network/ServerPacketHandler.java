package com.github.squi2rel.freedraw.network;

import com.github.squi2rel.freedraw.DataHolder;
import com.github.squi2rel.freedraw.FreeDraw;
import com.github.squi2rel.freedraw.ServerConfig;
import com.github.squi2rel.freedraw.brush.BrushPath;
import com.github.squi2rel.freedraw.brush.BrushPoint;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.joml.Vector3f;

import java.util.UUID;

import static com.github.squi2rel.freedraw.DataHolder.config;
import static com.github.squi2rel.freedraw.DataHolder.paths;
import static com.github.squi2rel.freedraw.network.ByteBufUtils.writeString;
import static com.github.squi2rel.freedraw.network.PacketID.*;

public class ServerPacketHandler {
    private static final Vector3f tmp1 = new Vector3f();

    public static void handle(ServerPlayerEntity player, ByteBuf buf) {
        short type = buf.readUnsignedByte();
        switch (type) {
            case CONFIG -> DataHolder.players.put(player.getUuid(), ByteBufUtils.readString(buf, 16));
            case NEW_PATH -> {
                UUID old = ByteBufUtils.readUUID(buf);
                UUID uuid = UUID.randomUUID();
                BrushPath path = new BrushPath(uuid, player.getWorld().getRegistryKey().getValue().toString(), ByteBufUtils.readVec3d(buf));
                config.paths.put(uuid, path);
                sendTo(player, newPath(old, uuid));
            }
            case REMOVE_PATH -> {
                UUID uuid = ByteBufUtils.readUUID(buf);
                BrushPath path = config.paths.remove(uuid);
                if (path == null) return;
                paths.remove(path);
                sendTo(player, removePath(uuid));
            }
            case ADD_POINTS -> {
                int index = buf.readerIndex();
                UUID uuid = ByteBufUtils.readUUID(buf);
                BrushPath path = config.paths.get(uuid);
                if (path == null || path.finalized) throw new RuntimeException("Invalid path!");
                int size = buf.readInt();
                BrushPoint prev;
                if (path.points.isEmpty()) {
                    prev = BrushPoint.read(buf);
                    path.points.add(prev);
                    size--;
                } else {
                    prev = path.points.getLast();
                }
                for (int i = 0; i < size; i++) {
                    BrushPoint now = BrushPoint.read(buf);
                    int len = (int) (now.pos.sub(prev.pos, tmp1).length() / 0.1);
                    path.size += len + 1;
                    path.points.add(now);
                    Vector3f pos = now.pos;
                    path.updateBounds(pos.x, pos.y, pos.z);
                    prev = now;
                }
                if (buf.readBoolean()) {
                    if (path.points.size() < 2) {
                        sendTo(player, removePath(uuid));
                        return;
                    }
                    path.stop();
                    paths.insert(path);
                    FreeDraw.LOGGER.info("Player {} created {} points with {}", player.getName(), path.size, path);
                }
                byte[] data = new byte[buf.readerIndex() - index];
                buf.readerIndex(index);
                buf.readBytes(data);
                broadcast(player, data);
            }
            default -> player.networkHandler.disconnect(Text.of("Unknown packet type: " + type));
        }
        if (buf.readableBytes() > 0) {
            player.networkHandler.disconnect(Text.of("Illegal packet! Remaining: " + buf.readableBytes()));
        }
    }

    private static void broadcast(ServerPlayerEntity pos, byte[] data) {
        for (ServerPlayerEntity player : PlayerLookup.around(pos.getServerWorld(), pos.getPos(), config.broadcastRange)) {
            if (player == pos) continue;
            sendTo(player, data);
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

    public static void sendTo(ServerPlayerEntity player, byte[] bytes) {
        ServerPlayNetworking.send(player, new DrawPayload(bytes));
    }

    public static byte[] config(String version, ServerConfig config) {
        ByteBuf buf = create(CONFIG);
        writeString(buf, version);
        writeString(buf, config.brushItem);
        buf.writeFloat(config.brushIdStart);
        buf.writeFloat(config.brushIdEnd);
        ByteBufUtils.writeQuaternion(buf, config.brushQuat);
        buf.writeFloat(config.brushLength);
        writeString(buf, config.eraserItem);
        buf.writeFloat(config.eraserId);
        ByteBufUtils.writeQuaternion(buf, config.eraserQuat);
        buf.writeFloat(config.eraserLength);
        buf.writeInt(config.maxPoints);
        buf.writeInt(config.uploadInterval);
        buf.writeInt(config.defaultColor);
        return toByteArray(buf);
    }

    public static byte[] newPath(UUID old, UUID uuid) {
        ByteBuf buf = create(NEW_PATH);
        ByteBufUtils.writeUUID(buf, old);
        ByteBufUtils.writeUUID(buf, uuid);
        return toByteArray(buf);
    }

    public static byte[] createPath(BrushPath path) {
        ByteBuf buf = create(CREATE_PATH);
        ByteBufUtils.writeUUID(buf, path.uuid);
        ByteBufUtils.writeVec3d(buf, path.offset);
        return toByteArray(buf);
    }

    public static byte[] addPoints(BrushPath path) {
        ByteBuf buf = create(ADD_POINTS);
        ByteBufUtils.writeUUID(buf, path.uuid);
        buf.writeInt(path.points.size());
        for (BrushPoint point : path.points) {
            BrushPoint.write(buf, point);
        }
        buf.writeBoolean(true);
        return toByteArray(buf);
    }

    public static byte[] removePath(UUID uuid) {
        ByteBuf buf = create(REMOVE_PATH);
        ByteBufUtils.writeUUID(buf, uuid);
        return toByteArray(buf);
    }

    public static byte[] maxPoints(int maxPoints) {
        ByteBuf buf = create(MAX_POINTS);
        buf.writeInt(maxPoints);
        return toByteArray(buf);
    }

    public static byte[] color(int color) {
        ByteBuf buf = create(COLOR);
        buf.writeInt(color);
        return toByteArray(buf);
    }
}
