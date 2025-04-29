package com.github.squi2rel.freedraw;

import net.fabricmc.api.ClientModInitializer;

public class FreeDrawClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		PathRenderer.register();
	}
}