package com.github.squi2rel.freedraw;

import com.github.squi2rel.freedraw.brush.ClientBrushPath;
import com.github.squi2rel.freedraw.brush.InputHandler;
import com.github.squi2rel.freedraw.network.ClientPacketHandler;
import com.github.squi2rel.freedraw.network.DrawPayload;
import com.github.squi2rel.freedraw.render.PathRenderer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import org.apache.commons.lang3.StringUtils;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.UUID;

public class FreeDrawClient implements ClientModInitializer {
	public static Item brushItem, eraserItem;
	public static float brushIdStart, brushIdEnd, eraserId;
	public static Quaternionf brushQuat, eraserQuat;
	public static float brushLength,  eraserLength;
	public static int maxPoints;
	public static boolean connected;
	public static HashMap<UUID, ClientBrushPath> paths = new HashMap<>();
	public static ClientBrushPath currentPath = null;
	public static int uploadInterval;
	public static int color;

	@Override
	public void onInitializeClient() {
		InputHandler.register();
		PathRenderer.register();

		ClientPlayNetworking.registerGlobalReceiver(DrawPayload.ID, (p, c) -> MinecraftClient.getInstance().execute(() -> {
            ByteBuf buf = Unpooled.wrappedBuffer(p.data());
            try {
                ClientPacketHandler.handle(buf);
            } catch (Exception e) {
                FreeDraw.LOGGER.error("Error while decoding packet", e);
            } finally {
                buf.release();
            }
        }));
	}

	public static void onDisconnect() {
		clear();
		connected = false;
	}

	public static void clear() {
		for (ClientBrushPath path : paths.values()) {
			path.remove();
		}
		paths.clear();
	}

	public static boolean checkVersion(String v) {
		String[] p1 = StringUtils.split(v, '.');
		String[] p2 = StringUtils.split(FreeDraw.version, '.');
		if (p1.length < 2 || p2.length < 2) return false;
		return p1[0].equals(p2[0]) && p1[1].equals(p2[1]);
	}
}