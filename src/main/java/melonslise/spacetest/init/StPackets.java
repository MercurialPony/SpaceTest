package melonslise.spacetest.init;

import melonslise.spacetest.core.planet.network.PlanetPropertiesRequestPacketHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class StPackets
{
	public static void register()
	{
		ServerPlayNetworking.registerGlobalReceiver(PlanetPropertiesRequestPacketHandler.ID, new PlanetPropertiesRequestPacketHandler());
	}
}