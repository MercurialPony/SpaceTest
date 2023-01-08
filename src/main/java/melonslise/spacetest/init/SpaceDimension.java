package melonslise.spacetest.init;

import melonslise.spacetest.world.dimension.VoidChunkGenerator;
import net.minecraft.util.registry.Registry;

public final class SpaceDimension
{
	private SpaceDimension() {}

	public static void registerParts()
	{
		Registry.register(Registry.CHUNK_GENERATOR, VoidChunkGenerator.ID, VoidChunkGenerator.CODEC);
	}
}