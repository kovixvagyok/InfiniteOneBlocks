package com.kovix.infiniteoneblocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.jspecify.annotations.Nullable;

/**
 * A flat generator (used with no layers, so the world is void) that also generates the {@link BlockLine}.
 */
public class LineLevelSource extends FlatLevelSource {
	public static final MapCodec<LineLevelSource> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings)).apply(i, i.stable(LineLevelSource::new))
	);

	private volatile @Nullable BlockLine line;

	public LineLevelSource(final FlatLevelGeneratorSettings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	public BlockLine line(final long seed, final FeatureFlagSet enabledFeatures) {
		BlockLine line = this.line;
		if (line == null) {
			synchronized (this) {
				line = this.line;
				if (line == null) {
					line = BlockLine.create(seed, enabledFeatures);
					this.line = line;
				}
			}
		}

		return line;
	}

	@Override
	public void applyBiomeDecoration(final WorldGenLevel level, final ChunkAccess chunk, final StructureManager structureManager) {
		super.applyBiomeDecoration(level, chunk, structureManager);
		this.line(level.getSeed(), level.enabledFeatures()).placeInChunk(level, chunk.getPos());
	}

	/** The line of this level, or null when the level does not use this generator. */
	public static @Nullable BlockLine lineOf(final ServerLevel level) {
		return level.getChunkSource().getGenerator() instanceof LineLevelSource generator ? generator.line(level.getSeed(), level.enabledFeatures()) : null;
	}
}
