package com.github.squi2rel.freedraw;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;

public class VivecraftApi {
    private static boolean vivecraftLoaded = false;
    
    public static boolean vivecraftLoaded() {
        if (vivecraftLoaded) return true;
        if (FabricLoader.getInstance().isModLoaded("vivecraft")) {
            vivecraftLoaded = true;
        }
        return vivecraftLoaded;
    }

    public static boolean isVRActive() {
        return VRState.VR_RUNNING;
    }

    public static Vec3d getHandPosition() {
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_world_pre.getController(0).getPosition();
    }
}
