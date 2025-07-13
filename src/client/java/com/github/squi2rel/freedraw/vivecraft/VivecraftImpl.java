package com.github.squi2rel.freedraw.vivecraft;

import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.VRState;

class VivecraftImpl {
    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();

    static boolean isVRActive() {
        return VRState.VR_RUNNING;
    }

    static Vec3d getHandPosition(Hand hand) {
        VRData data = DATA_HOLDER.vrPlayer.getVRDataWorld();
        VRData.VRDevicePose pose = data.getHand(hand.ordinal());
        return pose == null ? data.getController(hand.ordinal()).getPosition() : pose.getPosition();
    }

    static Vector3f getHandDirection(Hand hand) {
        VRData data = DATA_HOLDER.vrPlayer.getVRDataWorld();
        VRData.VRDevicePose pose = data.getHand(hand.ordinal());
        return pose == null ? data.getController(hand.ordinal()).getDirection() : pose.getDirection();
    }
}
