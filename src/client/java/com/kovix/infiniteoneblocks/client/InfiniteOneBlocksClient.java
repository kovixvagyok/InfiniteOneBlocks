package com.kovix.infiniteoneblocks.client;

import com.kovix.infiniteoneblocks.LineInfoPayload;
import com.kovix.infiniteoneblocks.LineRules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class InfiniteOneBlocksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(LineInfoPayload.TYPE, (payload, context) -> LineRules.clientLineLength = payload.length());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LineRules.clientLineLength = 0);
	}
}
