package com.kovix.infiniteoneblocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The single row of blocks the world is built around. Every block that has an item form appears exactly once,
 * starting with a grass block under the spawn point, followed by every other block in a seed-dependent order.
 * Entry {@code i} sits at {@code (START_X + i * SPACING, Y, Z)}; two-block-tall blocks also use the position above,
 * beds also use the position to the south. After the last entry waits an End portal with 12 empty frames.
 */
public final class BlockLine {
	public static final int Y = 64;
	public static final int Z = 0;
	public static final int START_X = 0;
	public static final int SPACING = 2;

	/** No neighbour updates, no shape updates and no onPlace side effects (falling, tick scheduling, ...). */
	private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;

	/** Distance from the last line block to the centre of the End portal. */
	private static final int PORTAL_DISTANCE = 6;
	private static final int PLATFORM_RADIUS = 3;

	private static final byte SINGLE = 0;
	private static final byte DOUBLE_HIGH = 1;
	private static final byte BED = 2;

	private final Block[] blocks;
	private final byte[] kinds;

	private BlockLine(final List<Block> blocks) {
		this.blocks = blocks.toArray(Block[]::new);
		this.kinds = new byte[this.blocks.length];
		for (int i = 0; i < this.blocks.length; i++) {
			this.kinds[i] = kindOf(baseState(this.blocks[i]));
		}
	}

	public static BlockLine create(final long seed, final FeatureFlagSet enabledFeatures) {
		List<Block> blocks = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block == Blocks.GRASS_BLOCK || block.defaultBlockState().isAir() || !block.isEnabled(enabledFeatures)) {
				continue;
			}

			// Only blocks whose item places exactly this block, so wall torches, wall signs, etc. are not duplicates.
			if (block.asItem() instanceof BlockItem item && item.getBlock() == block && item.isEnabled(enabledFeatures)) {
				blocks.add(block);
			}
		}

		Collections.shuffle(blocks, new Random(seed));
		blocks.addFirst(Blocks.GRASS_BLOCK);
		return new BlockLine(blocks);
	}

	public int length() {
		return this.blocks.length;
	}

	public Block block(final int index) {
		return this.blocks[index];
	}

	public static BlockPos posOf(final int index) {
		return new BlockPos(START_X + index * SPACING, Y, Z);
	}

	public static BlockPos spawnPos() {
		return posOf(0).above();
	}

	/** Index of the entry whose main block is at {@code pos}, or -1. */
	public static int mainIndexAt(final BlockPos pos, final int length) {
		if (pos.getY() != Y || pos.getZ() != Z) {
			return -1;
		}

		return indexAtX(pos.getX(), length);
	}

	private static int indexAtX(final int x, final int length) {
		int dx = x - START_X;
		if (dx < 0 || dx % SPACING != 0) {
			return -1;
		}

		int index = dx / SPACING;
		return index < length ? index : -1;
	}

	/** Index of the entry occupying {@code pos} (main block, upper half or bed head), or -1. */
	public int ownerAt(final BlockPos pos) {
		int y = pos.getY();
		int z = pos.getZ();
		if (z == Z) {
			if (y == Y) {
				return indexAtX(pos.getX(), this.blocks.length);
			}

			if (y == Y + 1) {
				int index = indexAtX(pos.getX(), this.blocks.length);
				return index >= 0 && this.kinds[index] == DOUBLE_HIGH ? index : -1;
			}
		} else if (z == Z + 1 && y == Y) {
			int index = indexAtX(pos.getX(), this.blocks.length);
			return index >= 0 && this.kinds[index] == BED ? index : -1;
		}

		return -1;
	}

	/** Centre of the End portal (12 empty frames on a platform) waiting after the last block of the line. */
	public BlockPos portalCenter() {
		return posOf(this.blocks.length - 1).offset(PORTAL_DISTANCE, 0, 0);
	}

	public void placeInChunk(final LevelWriter level, final ChunkPos chunkPos) {
		if (Z >= chunkPos.getMinBlockZ() && Z <= chunkPos.getMaxBlockZ()) {
			int first = Math.max(0, Math.ceilDiv(chunkPos.getMinBlockX() - START_X, SPACING));
			for (int i = first; i < this.blocks.length && START_X + i * SPACING <= chunkPos.getMaxBlockX(); i++) {
				this.place(level, i);
			}
		}

		this.placePortal(level, chunkPos);
	}

	private void placePortal(final LevelWriter level, final ChunkPos chunkPos) {
		BlockPos center = this.portalCenter();
		BlockState floor = Blocks.STONE_BRICKS.defaultBlockState();
		for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
			for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
				placeInside(level, chunkPos, center.offset(dx, -1, dz), floor);
			}
		}

		// Every frame has to face the middle of the ring for eyes of ender to light the portal.
		for (int i = -1; i <= 1; i++) {
			placeInside(level, chunkPos, center.offset(i, 0, -2), emptyFrame(Direction.SOUTH));
			placeInside(level, chunkPos, center.offset(i, 0, 2), emptyFrame(Direction.NORTH));
			placeInside(level, chunkPos, center.offset(-2, 0, i), emptyFrame(Direction.EAST));
			placeInside(level, chunkPos, center.offset(2, 0, i), emptyFrame(Direction.WEST));
		}
	}

	private static BlockState emptyFrame(final Direction facing) {
		return Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, facing).setValue(EndPortalFrameBlock.HAS_EYE, false);
	}

	private static void placeInside(final LevelWriter level, final ChunkPos chunkPos, final BlockPos pos, final BlockState state) {
		if (ChunkPos.containing(pos).equals(chunkPos)) {
			level.setBlock(pos, state, PLACE_FLAGS);
		}
	}

	/** Puts back every entry in a loaded chunk that is no longer the block it should be (explosions, pistons, fire...). */
	public void restoreMissing(final ServerLevel level) {
		for (int i = 0; i < this.blocks.length; i++) {
			BlockPos pos = posOf(i);
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
			if (chunk != null && !this.isIntact(chunk, i, pos)) {
				this.place(level, i);
			}
		}
	}

	private boolean isIntact(final BlockGetter level, final int index, final BlockPos pos) {
		Block block = this.blocks[index];
		if (!level.getBlockState(pos).is(block)) {
			return false;
		}

		return switch (this.kinds[index]) {
			case DOUBLE_HIGH -> level.getBlockState(pos.above()).is(block);
			case BED -> level.getBlockState(pos.relative(Direction.SOUTH)).is(block);
			default -> true;
		};
	}

	private void place(final LevelWriter level, final int index) {
		BlockPos pos = posOf(index);
		BlockState state = baseState(this.blocks[index]);
		switch (this.kinds[index]) {
			case DOUBLE_HIGH -> {
				level.setBlock(pos, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), PLACE_FLAGS);
				level.setBlock(pos.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), PLACE_FLAGS);
			}
			case BED -> {
				BlockState foot = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
				level.setBlock(pos, foot.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), PLACE_FLAGS);
				level.setBlock(pos.relative(Direction.SOUTH), foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), PLACE_FLAGS);
			}
			default -> level.setBlock(pos, state, PLACE_FLAGS);
		}
	}

	private static BlockState baseState(final Block block) {
		BlockState state = block.defaultBlockState();
		// A waterlogged block floating in the void would pour water down forever.
		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			state = state.setValue(BlockStateProperties.WATERLOGGED, false);
		}

		return state;
	}

	private static byte kindOf(final BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && !(state.getBlock() instanceof PitcherCropBlock)) {
			return DOUBLE_HIGH;
		}

		if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return BED;
		}

		return SINGLE;
	}
}
