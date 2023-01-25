package melonslise.spacetest.init;

import melonslise.spacetest.world.dimension.PlanetChunkGenerator;
import melonslise.spacetest.world.dimension.VoidChunkGenerator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class StDimensions
{
	private StDimensions() {}

	public static void registerParts()
	{
		Registry.register(Registries.CHUNK_GENERATOR, VoidChunkGenerator.ID, VoidChunkGenerator.CODEC);
		Registry.register(Registries.CHUNK_GENERATOR, PlanetChunkGenerator.ID, PlanetChunkGenerator.CODEC);
	}
}