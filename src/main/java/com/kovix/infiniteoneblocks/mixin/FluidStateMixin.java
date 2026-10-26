package com.kovix.infiniteoneblocks.mixin;

import com.kovix.infiniteoneblocks.LineRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Water inside line blocks such as seagrass or kelp must not spill into the void.
 */
@Mixin(FluidState.class)
public abstract class FluidStateMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$skipFluidTick(final ServerLevel level, final BlockPos pos, final BlockState blockState, final CallbackInfo ci) {
		if (LineRules.isProtected(level, pos)) {
			ci.cancel();
		}
	}
}
