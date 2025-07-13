package com.github.squi2rel.freedraw.vivecraft;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class Vivecraft {
    public static final boolean loaded = FabricLoader.getInstance().isModLoaded("vivecraft");

    public static boolean isVRActive() {
        return VivecraftImpl.isVRActive();
    }

    public static Vec3d getHandPosition(Hand hand) {
        return VivecraftImpl.getHandPosition(hand);
    }

    public static Vector3f getHandDirection(Hand hand) {
        return VivecraftImpl.getHandDirection(hand);
    }
}
