package melonslise.spacetest.init;

import melonslise.spacetest.core.planet.network.PlanetPropertiesResponsePacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class StClientPackets
{
	public static void register()
	{
		ClientPlayNetworking.registerGlobalReceiver(PlanetPropertiesResponsePacketHandler.ID, new PlanetPropertiesResponsePacketHandler());
	}
}