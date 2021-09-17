package melonslise.spacetest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import melonslise.immptl.common.world.chunk.ChunkLoaderValidator;
import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import melonslise.spacetest.common.init.SpaceTestBlocks;
import melonslise.spacetest.common.init.SpaceTestItems;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SpaceTest.ID)
public class SpaceTest
{
	public static final String ID = "spacetest";
	public static final Logger LOGGER = LogManager.getLogger();

	public SpaceTest()
	{
		SpaceTestBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
		SpaceTestItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
		SpaceTestBlockEntities.BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());

		ForgeChunkManager.setForcedChunkLoadingCallback(ID, new ChunkLoaderValidator()); // Register a chunk loader callback for the mod
	}
}