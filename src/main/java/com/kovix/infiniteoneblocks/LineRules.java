package com.kovix.infiniteoneblocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared logic behind the mixins: which positions belong to the line and what breaking them does.
 */
public final class LineRules {
	/** Normally unbreakable line blocks (bedrock, end portal frame, command blocks...) mine like obsidian. */
	private static final float UNBREAKABLE_HARDNESS = 50.0F;

	/** Line length announced by the server we are connected to; 0 when it is not an Infinite One Blocks world. */
	public static volatile int clientLineLength = 0;

	private LineRules() {
	}

	/** True for every position occupied by a line block in a line world, on the server only. */
	public static boolean isProtected(final BlockGetter level, final BlockPos pos) {
		if (!couldBeLine(pos) || !(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		BlockLine line = LineLevelSource.lineOf(serverLevel);
		return line != null && line.ownerAt(pos) >= 0;
	}

	/** Like {@link #isProtected}, but also answers on the client, where only the main line positions are known. */
	public static boolean isLineBlock(final BlockGetter level, final BlockPos pos) {
		if (!couldBeLine(pos)) {
			return false;
		}

		if (level instanceof ServerLevel) {
			return isProtected(level, pos);
		}

		return level instanceof Level clientLevel
			&& clientLevel.isClientSide()
			&& clientLevel.dimension() == Level.OVERWORLD
			&& BlockLine.mainIndexAt(pos, clientLineLength) >= 0;
	}

	private static boolean couldBeLine(final BlockPos pos) {
		int y = pos.getY();
		int z = pos.getZ();
		return (z == BlockLine.Z || z == BlockLine.Z + 1) && (y == BlockLine.Y || y == BlockLine.Y + 1) && pos.getX() >= BlockLine.START_X;
	}

	public static float unbreakableDestroyProgress(final Player player) {
		BlockState proxy = Blocks.OBSIDIAN.defaultBlockState();
		int modifier = player.hasCorrectToolForDrops(proxy) ? 30 : 100;
		return player.getDestroySpeed(proxy) / UNBREAKABLE_HARDNESS / modifier;
	}

	/**
	 * Called instead of actually breaking a line block: the block stays (it "regenerates" instantly) and drops itself
	 * as an item, provided the player used a tool that would normally get drops from it. Since the block is back
	 * immediately, the item pops out of the side facing the player; in the void it can still fall away.
	 */
	public static void harvest(final ServerLevel level, final ServerPlayer player, final BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		ItemStack tool = player.getMainHandItem();
		if (state.isAir()
			|| !tool.canDestroyBlock(state, level, pos, player)
			|| player.blockActionRestricted(level, pos, player.gameMode())
			|| player.preventsBlockDrops()) {
			return;
		}

		boolean unbreakable = state.getDestroySpeed(level, pos) < 0.0F;
		boolean canHarvest = player.hasCorrectToolForDrops(unbreakable ? Blocks.OBSIDIAN.defaultBlockState() : state);
		// The breaking player already sees the particles client-side; show them to everyone else.
		level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state));
		tool.mineBlock(level, state, pos, player);
		if (!canHarvest) {
			return;
		}

		player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
		player.causeFoodExhaustion(0.005F);
		Direction towardsPlayer = Direction.getApproximateNearest(player.getEyePosition().subtract(Vec3.atCenterOf(pos)));
		Block.popResourceFromFace(level, pos, towardsPlayer, new ItemStack(state.getBlock().asItem()));
	}
}
