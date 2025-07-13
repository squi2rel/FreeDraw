package com.github.squi2rel.freedraw;

import com.github.squi2rel.freedraw.brush.BrushPath;
import com.github.squi2rel.freedraw.network.ServerPacketHandler;
import com.google.gson.Gson;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import static com.github.squi2rel.freedraw.FreeDraw.server;

public class DataHolder {
    public static ServerConfig config;
    public static RegionManager paths;
    public static HashMap<UUID, String> players = new HashMap<>();

    public static void load() {
        config = loadConfig(ServerConfig.class, FreeDraw.configPath);
        paths = new RegionManager(config.broadcastRange);
        HashMap<UUID, BrushPath> map = config.paths;
        if (map != null) {
            for (BrushPath path : map.values()) {
                paths.insert(path);
            }
        }
    }

    public static void save() {
        saveConfig(config, FreeDraw.configPath);
    }

    public static <T> T loadConfig(Class<T> clazz, Path path) {
        try {
            return new Gson().fromJson(Files.readString(path), clazz);
        } catch (Exception e) {
            try {
                saveConfig(clazz.getDeclaredConstructor().newInstance(), path);
                return new Gson().fromJson(Files.readString(path), clazz);
            } catch (Exception ex) {
                RuntimeException th = new RuntimeException("Failed to load config file", ex);
                th.addSuppressed(e);
                throw th;
            }
        }
    }

    public static void update() {
        PlayerManager pm = server.getPlayerManager();
        for (UUID uuid : players.keySet()) {
            ServerPlayerEntity player = pm.getPlayer(uuid);
            Vec3d pos = Objects.requireNonNull(player).getPos();
            paths.update(player.getUuid(), player.getWorld().getRegistryKey().getValue().toString(), pos.x, pos.y, pos.z);
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        ServerPacketHandler.sendTo(player, ServerPacketHandler.config(FreeDraw.version, config));
        ServerPacketHandler.sendTo(player, ServerPacketHandler.maxPoints(config.maxPoints));
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        paths.removePlayer(player.getUuid());
        players.remove(player.getUuid());
    }

    public static void saveConfig(Object config, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, new Gson().toJson(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
