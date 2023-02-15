package melonslise.spacetest.core.planet.network;

import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.core.planet.PlanetProperties;
import melonslise.spacetest.core.planet.world.PlanetWorld;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class PlanetPropertiesRequestPacketHandler implements ServerPlayNetworking.PlayChannelHandler
{
	public static final Identifier ID = SpaceTestCore.id("request_planet_props");

	@Override
	public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
	{
		RegistryKey<World> worldKey = buf.readRegistryKey(RegistryKeys.WORLD);

		ServerWorld world = server.getWorld(worldKey);

		PlanetProperties props = ((PlanetWorld) world).getPlanetProperties();

		server.execute(() -> ServerPlayNetworking.send(player, PlanetPropertiesResponsePacket.ID, new PlanetPropertiesResponsePacket(worldKey, props)));
	}
}