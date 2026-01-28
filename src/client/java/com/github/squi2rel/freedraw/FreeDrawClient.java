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
import net.minecraft.item.Item;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreeDrawClient implements ClientModInitializer {
	public static Item brushItem, eraserItem;
	public static float brushIdStart, brushIdEnd, eraserId;
	public static Quaternionf brushQuat, eraserQuat;
	public static float brushLength,  eraserLength;
	public static int maxPoints;
	public static boolean connected;
	public static Map<UUID, ClientBrushPath> paths = new ConcurrentHashMap<>();
	public static ClientBrushPath currentPath = null;
	public static int uploadInterval;
	public static int color;
	public static float desktopRange;

	@Override
	public void onInitializeClient() {
		InputHandler.register();
		PathRenderer.register();

		ClientPlayNetworking.registerGlobalReceiver(DrawPayload.ID, (p, c) -> {
            ByteBuf buf = Unpooled.wrappedBuffer(p.data());
            try {
                ClientPacketHandler.handle(buf);
            } catch (Exception e) {
                FreeDraw.LOGGER.error("Error while decoding packet", e);
            } finally {
                buf.release();
            }
        });
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

		PathRenderer.reset();
	}
}