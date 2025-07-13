package com.github.squi2rel.freedraw;

import com.github.squi2rel.freedraw.brush.BrushPath;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HashBox {
    private final float cellSize;
    private final Map<GridIndex, Set<BrushPath>> gridMap = new ConcurrentHashMap<>();
    private int size;

    public HashBox(float size) {
        this.cellSize = size;
    }

    private Set<GridIndex> getIndices(BrushPath path) {
        int minX = (int) Math.floor((path.offset.x + path.minX) / cellSize);
        int minY = (int) Math.floor((path.offset.y + path.minY) / cellSize);
        int minZ = (int) Math.floor((path.offset.z + path.minZ) / cellSize);
        int maxX = (int) Math.floor((path.offset.x + path.maxX) / cellSize);
        int maxY = (int) Math.floor((path.offset.y + path.maxY) / cellSize);
        int maxZ = (int) Math.floor((path.offset.z + path.maxZ) / cellSize);

        Set<GridIndex> indices = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    indices.add(new GridIndex(x, y, z));
                }
            }
        }
        return indices;
    }

    public void insert(BrushPath path) {
        for (GridIndex idx : getIndices(path)) {
            gridMap.computeIfAbsent(idx, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(path);
            size++;
        }
    }

    public void remove(BrushPath path) {
        for (GridIndex idx : getIndices(path)) {
            Set<BrushPath> set = gridMap.get(idx);
            if (set != null) {
                if (set.remove(path)) size--;
                if (set.isEmpty()) gridMap.remove(idx);
            }
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public List<BrushPath> get(double px, double py, double pz, double range) {
        Set<BrushPath> all = getAll(px, py, pz, range);
        List<BrushPath> result = new ArrayList<>();
        for (BrushPath path : all) {
            double cx = (path.minX + path.maxX) / 2;
            double cy = (path.minY + path.maxY) / 2;
            double cz = (path.minZ + path.maxZ) / 2;
            Vector3d o = path.offset;
            double dist = Math.sqrt(Math.pow(o.x + cx - px, 2) + Math.pow(o.y + cy - py, 2) + Math.pow(o.z + cz - pz, 2));
            if (dist <= range) {
                result.add(path);
            }
        }
        return result;
    }

    private Set<BrushPath> getAll(double px, double py, double pz, double range) {
        int minX = (int) Math.floor((px - range) / cellSize);
        int minY = (int) Math.floor((py - range) / cellSize);
        int minZ = (int) Math.floor((pz - range) / cellSize);
        int maxX = (int) Math.floor((px + range) / cellSize);
        int maxY = (int) Math.floor((py + range) / cellSize);
        int maxZ = (int) Math.floor((pz + range) / cellSize);
        Set<BrushPath> all = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Set<BrushPath> set = gridMap.get(new GridIndex(x, y, z));
                    if (set != null) {
                        all.addAll(set);
                    }
                }
            }
        }
        return all;
    }

    public record GridIndex(int x, int y, int z) {}
}
