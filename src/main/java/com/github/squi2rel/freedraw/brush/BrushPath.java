package com.github.squi2rel.freedraw.brush;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.UUID;

public class BrushPath {
    public @NotNull UUID uuid;
    public String world;
    public Vector3d offset;
    public ArrayList<BrushPoint> points = new ArrayList<>();
    public int size = 0;
    public float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
    public float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
    public boolean finalized = false;

    public BrushPath(@NotNull UUID uuid, String world, Vector3d offset) {
        this.uuid = uuid;
        this.world = world;
        this.offset = offset;
    }

    public void updateBounds(float x, float y, float z) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (z < minZ) minZ = z;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
        if (z > maxZ) maxZ = z;
    }

    public void stop() {
        finalized = true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BrushPath p && p.uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "BrushPath[uuid=" + uuid + ", offset=" + offset + "]";
    }
}
