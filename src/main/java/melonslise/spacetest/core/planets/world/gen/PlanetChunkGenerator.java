package melonslise.spacetest.core.planets.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.core.planets.CubeFaceContext;
import melonslise.spacetest.core.planets.PlanetProjection;
import melonslise.spacetest.core.planets.PlanetProperties;
import melonslise.spacetest.core.planets.world.PlanetWorld;
import melonslise.spacetest.core.seamless_worldgen.noise.PlanetNoiseSampler;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;

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

	@Override
	protected ChunkNoiseSampler createChunkNoiseSampler(Chunk chunk, StructureAccessor structureAccessor, Blender blender, NoiseConfig noiseConfig)
	{
		ServerWorld world = ((ServerWorldAccess) structureAccessor.world).toServerWorld();
		PlanetProperties planetProps = ((PlanetWorld) world).getPlanetProperties();

		return PlanetNoiseSampler.create(
			new CubeFaceContext(PlanetProjection.determineFaceInChunks(planetProps, chunk.getPos().x, chunk.getPos().z), planetProps, world),
			chunk,
			noiseConfig,
			StructureWeightSampler.createStructureWeightSampler(structureAccessor, chunk.getPos()),
			this.settings.value(),
			this.fluidLevelSampler.get(),
			blender
		);
	}
}