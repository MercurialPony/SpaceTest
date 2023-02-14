package melonslise.spacetest.init;

import melonslise.spacetest.core.planet.world.gen.PlanetChunkGenerator;
import melonslise.spacetest.core.space.world.gen.VoidChunkGenerator;
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