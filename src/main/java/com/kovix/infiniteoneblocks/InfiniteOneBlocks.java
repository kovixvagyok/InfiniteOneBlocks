package com.kovix.infiniteoneblocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfiniteOneBlocks implements ModInitializer {
	public static final String MOD_ID = "infiniteoneblocks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceKey<WorldPreset> WORLD_PRESET = ResourceKey.create(Registries.WORLD_PRESET, id("infinite_one_blocks"));

	/** How often (in ticks) line blocks destroyed by something other than a player are put back. */
	private static final int RESTORE_INTERVAL = 10;

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("line"), LineLevelSource.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LineInfoPayload.TYPE, LineInfoPayload.CODEC);

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % RESTORE_INTERVAL == 0) {
				BlockLine line = LineLevelSource.lineOf(level);
				if (line != null) {
					line.restoreMissing(level);
				}
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, LineInfoPayload.TYPE)) {
				BlockLine line = LineLevelSource.lineOf(server.overworld());
				sender.sendPacket(new LineInfoPayload(line != null ? line.length() : 0));
			}
		});
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
