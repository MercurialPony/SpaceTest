package melonslise.spacetest.common.worldgen;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import melonslise.spacetest.SpaceTest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.StructureSettings;

public class SpaceChunkGenerator extends ChunkGenerator
{
	public static final ResourceLocation ID = new ResourceLocation(SpaceTest.ID, "space");

	public static final Codec<SpaceChunkGenerator> CODEC = RecordCodecBuilder.create((bld) -> bld
		.group(
			Codec.LONG.fieldOf("seed").forGetter(inst -> inst.seed),
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(inst -> inst.biomeSource))
		.apply(bld, SpaceChunkGenerator::new));

	public final long seed;

	public SpaceChunkGenerator(long seed, BiomeSource biomeSource)
	{
		super(biomeSource, new StructureSettings(false));
		this.seed = seed;
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec()
	{
		return CODEC;
	}

	@Override
	public ChunkGenerator withSeed(long seed)
	{
		return new SpaceChunkGenerator(seed, this.biomeSource.withSeed(seed));
	}

	@Override
	public void buildSurfaceAndBedrock(WorldGenRegion region, ChunkAccess chunk)
	{
		
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, StructureFeatureManager structureManager, ChunkAccess chunk)
	{
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor heightAccessor)
	{
		return 0;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor)
	{
		BlockState[] states = new BlockState[256];
		Arrays.fill(states, Blocks.AIR.defaultBlockState());
		return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
	}
}