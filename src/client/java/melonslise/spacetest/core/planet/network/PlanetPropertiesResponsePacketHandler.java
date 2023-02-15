package melonslise.spacetest.core.planet.network;

import melonslise.spacetest.core.planet.BasicPlanetProperties;
import melonslise.spacetest.core.planet.PlanetProperties;
import melonslise.spacetest.core.planet.world.PlanetWorld;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.ClientWorldLoader;

public class PlanetPropertiesResponsePacketHandler implements ClientPlayNetworking.PlayChannelHandler
{
	public static final Identifier ID = PlanetPropertiesResponsePacket.ID;

	@Override
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
	{
		RegistryKey<World> worldKey = buf.readRegistryKey(RegistryKeys.WORLD);

		if(buf.readBoolean())
		{
			return;
		}

		PlanetProperties props = new BasicPlanetProperties(buf.readChunkSectionPos(), buf.readByte(), buf.readFloat(), buf.readFloat());

		client.execute(() ->
		{
			ClientWorld clientWorld = client.player == null ? client.world : ClientWorldLoader.getWorld(worldKey);
			PlanetWorld planetWorld = (PlanetWorld) clientWorld;

			planetWorld.setPlanetProperties(props);
		});
	}
}