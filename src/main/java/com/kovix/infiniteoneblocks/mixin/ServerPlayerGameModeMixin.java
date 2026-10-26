package com.kovix.infiniteoneblocks.mixin;

import com.kovix.infiniteoneblocks.LineRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Shadow
	protected ServerLevel level;

	@Shadow
	@Final
	protected ServerPlayer player;

	/**
	 * Breaking a line block hands it to the player and leaves it in place. Returning false makes the server
	 * resend the block, so the client sees it come back right away.
	 */
	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$harvestLineBlock(final BlockPos pos, final CallbackInfoReturnable<Boolean> cir) {
		if (LineRules.isProtected(this.level, pos)) {
			LineRules.harvest(this.level, this.player, pos);
			cir.setReturnValue(false);
		}
	}
}
