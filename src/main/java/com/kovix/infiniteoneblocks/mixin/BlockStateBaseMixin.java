package com.kovix.infiniteoneblocks.mixin;

import com.kovix.infiniteoneblocks.LineRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freezes line blocks: they never fall, decay, grow, pop off for lack of support or react to redstone,
 * and normally unbreakable ones can be mined.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$mineUnbreakable(final Player player, final BlockGetter level, final BlockPos pos, final CallbackInfoReturnable<Float> cir) {
		BlockState self = (BlockState) (Object) this;
		if (self.getDestroySpeed(level, pos) < 0.0F && LineRules.isLineBlock(level, pos)) {
			cir.setReturnValue(LineRules.unbreakableDestroyProgress(player));
		}
	}

	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$keepShape(
		final LevelReader level,
		final ScheduledTickAccess ticks,
		final BlockPos pos,
		final Direction directionToNeighbour,
		final BlockPos neighbourPos,
		final BlockState neighbourState,
		final RandomSource random,
		final CallbackInfoReturnable<BlockState> cir
	) {
		if (LineRules.isProtected(level, pos)) {
			cir.setReturnValue((BlockState) (Object) this);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$skipTick(final ServerLevel level, final BlockPos pos, final RandomSource random, final CallbackInfo ci) {
		if (LineRules.isProtected(level, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$skipRandomTick(final ServerLevel level, final BlockPos pos, final RandomSource random, final CallbackInfo ci) {
		if (LineRules.isProtected(level, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "handleNeighborChanged", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$ignoreNeighbors(
		final Level level, final BlockPos pos, final Block changedBlock, final Orientation orientation, final boolean movedByPiston, final CallbackInfo ci
	) {
		if (LineRules.isProtected(level, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$skipOnPlace(final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston, final CallbackInfo ci) {
		if (LineRules.isProtected(level, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z", at = @At("HEAD"), cancellable = true)
	private void infiniteoneblocks$notReplaceable(final BlockPlaceContext context, final CallbackInfoReturnable<Boolean> cir) {
		if (LineRules.isProtected(context.getLevel(), context.getClickedPos())) {
			cir.setReturnValue(false);
		}
	}
}
