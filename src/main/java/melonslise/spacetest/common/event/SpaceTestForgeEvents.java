package melonslise.spacetest.common.event;

import melonslise.immptl.client.PlayerViewManager;
import melonslise.immptl.common.world.chunk.RenderLoaderManager;
import melonslise.immptl.common.world.chunk.ServerHelper;
import melonslise.immptl.server.command.SpaceTestCommands;
import melonslise.spacetest.SpaceTest;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;

@Mod.EventBusSubscriber(modid = SpaceTest.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpaceTestForgeEvents
{
	private SpaceTestForgeEvents() {}

	// Server-side
	/**
	 * When the server starts.
	 * @param event - server start event
	 */
	@SubscribeEvent
	public static void onServerStart(FMLServerStartedEvent event)
	{
		RenderLoaderManager.onServerStart(event.getServer());
		ServerHelper.onServerStart(event);
	}

	@SubscribeEvent
	public static void onServerStop(FMLServerStoppedEvent event)
	{
		RenderLoaderManager.onServerClosing(event.getServer());
		ServerHelper.onServerStop(event);
	}

	@SubscribeEvent
	public static void serverTick(TickEvent.ServerTickEvent e)
	{
		if(e.phase != TickEvent.Phase.END)
			return;
		RenderLoaderManager.tick();
	}

	@SubscribeEvent
	public static void playerJoined(PlayerEvent.PlayerLoggedInEvent event)
	{
		RenderLoaderManager.playerJoined(event);
	}

	@SubscribeEvent
	public static void playerLeft(PlayerEvent.PlayerLoggedOutEvent event)
	{
		RenderLoaderManager.playerLeft(event);
	}

	@SubscribeEvent
	public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		RenderLoaderManager.playerChangedDimension(event);
	}

	@SubscribeEvent
	public static void playerRespawned(PlayerEvent.PlayerRespawnEvent event)
	{
		RenderLoaderManager.playerRespawned(event);
	}

	@SubscribeEvent
	public static void addCommands(RegisterCommandsEvent event)
	{
		new SpaceTestCommands(event.getDispatcher());
	}


	// Client-side
	// FIXME Get the client-side stuff working, so these can be resubscribed
//	@SubscribeEvent
	public static void renderTickStart(TickEvent.RenderTickEvent event)
	{
		PlayerViewManager.renderTick(event);
	}

//	@SubscribeEvent
	public static void clientTickStart(TickEvent.ClientTickEvent event)
	{
		PlayerViewManager.clientTick(event);
	}

//	@SubscribeEvent
	public static void clientLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event)
	{
		PlayerViewManager.playerLoggedIn(event);
	}

//	@SubscribeEvent
	public static void clientLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
	{
		PlayerViewManager.playerLoggedOut(event);
	}

//	@SubscribeEvent
	public static void clientRespawned(ClientPlayerNetworkEvent.RespawnEvent event)
	{
		PlayerViewManager.playerRespawned(event);
	}

	// TODO Add hook for client-side dimension change?
}