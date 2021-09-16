package melonslise.spacetest;

import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import melonslise.spacetest.common.init.SpaceTestBlocks;
import melonslise.spacetest.common.init.SpaceTestItems;
import melonslise.spacetest.server.util.ChunkLoaderManager;
import melonslise.spacetest.server.util.ChunkLoaderValidator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		MinecraftForge.EVENT_BUS.register(ChunkLoaderManager.class);
	}
}