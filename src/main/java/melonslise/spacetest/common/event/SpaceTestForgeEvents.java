package melonslise.spacetest.common.event;

import com.google.common.eventbus.Subscribe;
import melonslise.immptl.common.world.chunk.ChunkLoaderManager;
import melonslise.spacetest.SpaceTest;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;

@Mod.EventBusSubscriber(modid = SpaceTest.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpaceTestForgeEvents
{
	private SpaceTestForgeEvents() {}

	/**
	 * When the server starts,
	 * 
	 * @param event - server start event
	 */
	@SubscribeEvent
	public static void onServerStart(FMLServerStartedEvent event)
	{
		ChunkLoaderManager.onServerStart(event.getServer());
	}

	@SubscribeEvent
	public static void onServerStop(FMLServerStoppedEvent event)
	{
		ChunkLoaderManager.onServerClosing(event.getServer());
	}

	@SubscribeEvent
	public static void serverTick(TickEvent.ServerTickEvent e)
	{
		if(e.phase != TickEvent.Phase.START)
			return;
		ChunkLoaderManager.tick();
	}
}