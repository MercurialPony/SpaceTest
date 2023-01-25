package melonslise.spacetest.world.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.planet.CubemapFace;
import melonslise.spacetest.planet.PlanetProjection;
import melonslise.spacetest.world.PlanetWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PlanetChunkGenerator extends NoiseChunkGenerator
{
	public static final Identifier ID = new Identifier(SpaceTestCore.ID, "planet");

	public static final Codec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance
		.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
			ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(generator -> generator.settings))
		.apply(instance, instance.stable(PlanetChunkGenerator::new)));

	public PlanetChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings)
	{
		super(biomeSource, settings);
	}

	@Override
	protected Codec<? extends ChunkGenerator> getCodec()
	{
		return CODEC;
	}

	private static CubemapFace getFace(ServerWorld world, Chunk chunk)
	{
		ChunkPos pos = chunk.getPos();
		return PlanetProjection.determineFace(((PlanetWorld) world).getPlanetProperties(), ChunkSectionPos.getBlockCoord(pos.x), ChunkSectionPos.getBlockCoord(pos.z));
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk)
	{
		if(getFace(((ServerWorldAccess) structureAccessor.world).toServerWorld(), chunk) == null)
		{
			return CompletableFuture.completedFuture(chunk);
		}

		return super.populateNoise(executor, blender, noiseConfig, structureAccessor, chunk);
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk)
	{
		if(getFace(region.toServerWorld(), chunk) == null || true)
		{
			return;
		}

		super.buildSurface(region, structures, noiseConfig, chunk);
	}

	@Override
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep)
	{
		if(getFace(chunkRegion.toServerWorld(), chunk) == null || true)
		{
			return;
		}

		super.carve(chunkRegion, seed, noiseConfig, biomeAccess, structureAccessor, chunk, carverStep);
	}

	@Override
	public void populateEntities(ChunkRegion region)
	{
	}

	@Override
	public int getWorldHeight()
	{
		return super.getWorldHeight();
	}

	@Override
	public int getSeaLevel()
	{
		return super.getSeaLevel();
	}

	@Override
	public int getMinimumY()
	{
		return super.getMinimumY();
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig)
	{
		return super.getHeight(x, z, heightmap, world, noiseConfig);
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig)
	{
		return super.getColumnSample(x, z, world, noiseConfig);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos)
	{
		super.getDebugHudText(text, noiseConfig, pos);
	}
}