package com.kovix.infiniteoneblocks.test;

import com.kovix.infiniteoneblocks.BlockLine;
import com.kovix.infiniteoneblocks.InfiniteOneBlocks;
import com.kovix.infiniteoneblocks.LineLevelSource;
import com.kovix.infiniteoneblocks.LineRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class LineClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().adjustSettings(settings -> {
			settings.setWorldType(new WorldCreationUiState.WorldTypeEntry(
				settings.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(InfiniteOneBlocks.WORLD_PRESET)
			));
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
		}).create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();

			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockLine line = LineLevelSource.lineOf(level);
				check(line != null, "the overworld does not use the line generator");
				check(level.getBlockState(BlockLine.posOf(0)).is(Blocks.GRASS_BLOCK), "the line does not start with grass");
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				check(player.blockPosition().equals(BlockLine.spawnPos()), "player spawned at " + player.blockPosition());
				check(line.length() > 900, "line has only " + line.length() + " blocks");
				InfiniteOneBlocks.LOGGER.info("[test] line length {}, first blocks: {} {} {} {}", line.length(), line.block(1), line.block(2), line.block(3), line.block(4));
			});
			context.getInput().lookAt(BlockLine.posOf(6));
			context.waitTicks(5);
			context.takeScreenshot("line-from-spawn");

			// A normal block: mining it gives the block and leaves it in place.
			int normal = findIndex(server, (level, state) -> state.isCollisionShapeFullBlock(level, BlockPos.ZERO) && state.getDestroySpeed(level, BlockPos.ZERO) >= 0.0F
				&& state.getDestroySpeed(level, BlockPos.ZERO) <= 3.0F && !state.requiresCorrectToolForDrops() && !(state.getBlock() instanceof FallingBlock));
			mineAndCheck(context, singleplayer, normal, "iron_pickaxe", 120);

			// Bedrock is normally unbreakable; on the line it mines like obsidian with a diamond pickaxe.
			int bedrock = findIndex(server, (level, state) -> state.is(Blocks.BEDROCK));
			mineAndCheck(context, singleplayer, bedrock, "diamond_pickaxe", 240);
			context.takeScreenshot("after-bedrock");

			// Destroyed by something else -> it comes back by itself.
			int restored = normal + 1;
			server.runCommand(String.format("setblock %d %d %d air", BlockLine.posOf(restored).getX(), BlockLine.Y, BlockLine.Z));
			context.waitTicks(25);
			server.runOnServer(s -> {
				BlockLine line = LineLevelSource.lineOf(s.overworld());
				check(s.overworld().getBlockState(BlockLine.posOf(restored)).is(line.block(restored)), "removed line block was not restored");
			});

			// Gravity blocks and blocks without support stay put even when their neighbours change.
			int falling = findIndex(server, (level, state) -> state.getBlock() instanceof FallingBlock);
			int unsupported = findIndex(server, (level, state) -> !state.canSurvive(level, BlockLine.posOf(0).above(10)));
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int index : new int[] {falling, unsupported}) {
					BlockPos pos = BlockLine.posOf(index);
					level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
					// Neighbour and shape updates that would normally make these fall or pop off.
					level.setBlockAndUpdate(pos.east(), Blocks.STONE.defaultBlockState());
					level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
				}
			});

			context.waitTicks(40);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockLine line = LineLevelSource.lineOf(level);
				for (int index : new int[] {falling, unsupported}) {
					BlockPos pos = BlockLine.posOf(index);
					check(level.getBlockState(pos).is(line.block(index)), line.block(index) + " did not stay in place");
					check(level.getEntities(EntityTypes.ITEM, new AABB(pos).inflate(3), e -> true).isEmpty(), line.block(index) + " dropped items");
					check(level.getEntities(EntityTypes.FALLING_BLOCK, new AABB(pos).inflate(3, 70, 3), e -> true).isEmpty(), line.block(index) + " started falling");
				}

				InfiniteOneBlocks.LOGGER.info("[test] falling block {} and unsupported block {} stayed in place", line.block(falling), line.block(unsupported));
			});

			// The End portal after the last block: 12 empty frames that light up with eyes of ender.
			BlockPos portal = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos center = LineLevelSource.lineOf(level).portalCenter();
				for (int dx = -3; dx <= 3; dx += 6) {
					for (int dz = -3; dz <= 3; dz += 6) {
						level.getChunk((center.getX() + dx) >> 4, (center.getZ() + dz) >> 4);
					}
				}

				for (BlockPos frame : framePositions(center)) {
					BlockState state = level.getBlockState(frame);
					check(state.is(Blocks.END_PORTAL_FRAME) && !state.getValue(EndPortalFrameBlock.HAS_EYE), "no empty end portal frame at " + frame + ": " + state);
				}

				check(level.getBlockState(center.below()).is(Blocks.STONE_BRICKS), "the portal has no platform");
				return center;
			});
			server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f", portal.getX() + 0.5, BlockLine.Y, portal.getZ() - 2.5));
			server.runCommand("clear @a");
			server.runCommand("give @a ender_eye");
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();
			context.getInput().lookAt(portal);
			context.waitTicks(5);
			context.takeScreenshot("end-portal-as-generated");

			// Fill 11 frames directly, the player puts in the 12th eye.
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				for (BlockPos frame : framePositions(portal)) {
					if (!frame.equals(portal.north(2))) {
						level.setBlock(frame, level.getBlockState(frame).setValue(EndPortalFrameBlock.HAS_EYE, true), 2);
					}
				}
			});
			context.getInput().lookAt(portal.north(2));
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						check(level.getBlockState(portal.offset(dx, 0, dz)).is(Blocks.END_PORTAL), "the last eye of ender did not open the portal");
					}
				}

				InfiniteOneBlocks.LOGGER.info("[test] End portal at {} opened with the 12th eye", portal);
			});
			context.getInput().lookAt(portal);
			context.waitTicks(5);
			context.takeScreenshot("end-portal");

			InfiniteOneBlocks.LOGGER.info("[test] all Infinite One Blocks checks passed");
		}
	}

	private interface StatePredicate {
		boolean test(ServerLevel level, BlockState state);
	}

	private static int findIndex(final TestServerContext server, final StatePredicate predicate) {
		return server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			BlockLine line = LineLevelSource.lineOf(level);
			for (int i = 1; i < line.length(); i++) {
				if (predicate.test(level, line.block(i).defaultBlockState())) {
					return i;
				}
			}

			throw new AssertionError("no line block matches the predicate");
		});
	}

	/** Stands the player next to line entry {@code index} and holds attack, then checks the reward. */
	private static void mineAndCheck(
		final ClientGameTestContext context, final TestSingleplayerContext singleplayer, final int index, final String tool, final int ticks
	) {
		TestServerContext server = singleplayer.getServer();
		BlockPos target = BlockLine.posOf(index);
		Block block = server.computeOnServer(s -> LineLevelSource.lineOf(s.overworld()).block(index));
		Item item = block.asItem();
		// A stand-alone platform north of the target (not on the line) to stand on; loads the chunk first.
		server.runOnServer(s -> {
			BlockPos platform = new BlockPos(target.getX(), BlockLine.Y, BlockLine.Z - 2);
			s.overworld().getChunk(platform.getX() >> 4, platform.getZ() >> 4);
			s.overworld().setBlockAndUpdate(platform, Blocks.STONE.defaultBlockState());
		});
		server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f", target.getX() + 0.5, BlockLine.Y + 1, BlockLine.Z - 1.5));
		server.runCommand("clear @a");
		server.runCommand("give @a " + tool);
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.getInput().lookAt(target);
		context.waitTicks(2);
		String clientView = context.computeOnClient(client -> "clientLineLength=" + LineRules.clientLineLength
			+ " clientProgress=" + client.level.getBlockState(target).getDestroyProgress(client.player, client.level, target)
			+ " hit=" + client.hitResult + " mainHand=" + client.player.getMainHandItem());
		float serverProgress = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			return s.overworld().getBlockState(target).getDestroyProgress(player, s.overworld(), target);
		});
		InfiniteOneBlocks.LOGGER.info("[test] before mining {}: {} serverProgress={}", BuiltInRegistries.BLOCK.getKey(block), clientView, serverProgress);
		context.getInput().holdKeyFor(options -> options.keyAttack, ticks);
		context.waitTicks(5);

		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			int picked = count(player.getInventory(), item);
			int dropped = 0;
			// Drops can roll off the block and fall into the void, so look far below as well.
			for (ItemEntity drop : level.getEntities(EntityTypes.ITEM, new AABB(target).inflate(4).expandTowards(0, -150, 0), e -> e.getItem().is(item))) {
				dropped += drop.getItem().getCount();
			}

			check(picked + dropped >= 1, "mining " + BuiltInRegistries.BLOCK.getKey(block) + " dropped nothing");
			check(level.getBlockState(target).is(block), BuiltInRegistries.BLOCK.getKey(block) + " did not regenerate");
			InfiniteOneBlocks.LOGGER.info(
				"[test] mined {} -> {}x {} dropped at the block, {}x picked up", BuiltInRegistries.BLOCK.getKey(block), dropped, BuiltInRegistries.ITEM.getKey(item), picked
			);
		});
		// Clean up so the next check starts from an empty area.
		server.runCommand("kill @e[type=item]");
	}

	private static List<BlockPos> framePositions(final BlockPos center) {
		List<BlockPos> frames = new ArrayList<>();
		for (int i = -1; i <= 1; i++) {
			frames.add(center.offset(i, 0, -2));
			frames.add(center.offset(i, 0, 2));
			frames.add(center.offset(-2, 0, i));
			frames.add(center.offset(2, 0, i));
		}

		return frames;
	}

	private static int count(final Inventory inventory, final Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot).is(item)) {
				count += inventory.getItem(slot).getCount();
			}
		}

		return count;
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
