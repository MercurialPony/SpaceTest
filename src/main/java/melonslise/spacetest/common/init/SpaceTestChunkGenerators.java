package melonslise.spacetest.common.init;

import melonslise.spacetest.common.world.gen.SpaceChunkGenerator;
import net.minecraft.core.Registry;

public final class SpaceTestChunkGenerators
{
	private SpaceTestChunkGenerators() {}

	public static void register()
	{
		// FIXME this corrent way?
		Registry.register(Registry.CHUNK_GENERATOR, SpaceChunkGenerator.ID, SpaceChunkGenerator.CODEC);
	}
}