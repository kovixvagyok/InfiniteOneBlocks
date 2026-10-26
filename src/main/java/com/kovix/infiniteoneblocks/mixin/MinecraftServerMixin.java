package com.kovix.infiniteoneblocks.mixin;

import com.kovix.infiniteoneblocks.BlockLine;
import com.kovix.infiniteoneblocks.LineLevelSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	/**
	 * New line worlds spawn every player on the grass block at the start of the line, looking down the line.
	 */
	@Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
	private static void infiniteoneblocks$spawnAtLineStart(
		final ServerLevel level,
		final ServerLevelData levelData,
		final boolean spawnBonusChest,
		final boolean isDebug,
		final LevelLoadListener levelLoadListener,
		final CallbackInfo ci
	) {
		if (level.getChunkSource().getGenerator() instanceof LineLevelSource) {
			levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), BlockLine.spawnPos(), -90.0F, 0.0F));
			level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
			ci.cancel();
		}
	}
}
