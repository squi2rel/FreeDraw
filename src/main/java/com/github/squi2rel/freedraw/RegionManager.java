package com.github.squi2rel.freedraw;

import com.github.squi2rel.freedraw.brush.BrushPath;
import com.github.squi2rel.freedraw.network.ServerPacketHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

import static com.github.squi2rel.freedraw.DataHolder.config;
import static com.github.squi2rel.freedraw.FreeDraw.server;

public class RegionManager {
    private final int boxSize;
    private final Map<String, HashBox> worlds = new HashMap<>();
    private final Map<UUID, State> playerStates = new HashMap<>();

    public RegionManager(int boxSize) {
        this.boxSize = boxSize;
    }

    public void insert(BrushPath path) {
        worlds.computeIfAbsent(path.world, k -> new HashBox(boxSize)).insert(path);
    }

    public void remove(BrushPath path) {
        HashBox box = worlds.get(path.world);
        if (box == null) return;
        box.remove(path);
        for (Map.Entry<UUID, State> entry : playerStates.entrySet()) {
            if (entry.getValue().paths.contains(path)) {
                remove(entry.getKey(), path);
            }
        }
    }

    public void update(UUID player, String world, double px, double py, double pz) {
        HashBox box = worlds.get(world);
        if (box == null) return;

        State state = playerStates.get(player);
        if (state == null) {
            state = new State();
            state.world = world;
            playerStates.put(player, state);
        }

        List<BrushPath> current = box.get(px, py, pz, config.broadcastRange);
        if (Objects.equals(state.world, world)) {
            Set<BrushPath> entered = new HashSet<>(current);
            entered.removeAll(state.paths);
            for (BrushPath path : entered) {
                create(player, path);
            }

            List<BrushPath> exited = new ArrayList<>(state.paths);
            exited.removeAll(current);
            for (BrushPath path : exited) {
                remove(player, path);
            }
        } else {
            for (BrushPath path : current) {
                create(player, path);
            }
        }

        state.world = world;
        state.paths = new HashSet<>(current);
    }

    private void create(UUID player, BrushPath path) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        ServerPacketHandler.sendTo(p, ServerPacketHandler.createPath(path));
        ServerPacketHandler.sendTo(p, ServerPacketHandler.addPoints(path));
    }

    private void remove(UUID player, BrushPath path) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        ServerPacketHandler.sendTo(p, ServerPacketHandler.removePath(path.uuid));
    }

    private static class State {
        String world;
        Set<BrushPath> paths = new HashSet<>();
    }
}