package com.github.squi2rel.freedraw;

import com.github.squi2rel.freedraw.brush.BrushPath;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.UUID;

public class ServerConfig {
    public String brushItem = "minecraft:brush", eraserItem = "minecraft:resin_brick";
    public float brushIdStart = -1, brushIdEnd = -1, eraserId = -1;
    public int maxPoints = 2048;
    public int broadcastRange = 128;
    public Quaternionf brushQuat = new Quaternionf(), eraserQuat = new Quaternionf();
    public float brushLength = 0.1f, eraserLength = 0.1f;
    public int uploadInterval = 100;
    public int defaultColor = 0xFFFF0000;
    public transient HashMap<UUID, BrushPath> paths = new HashMap<>();
}
