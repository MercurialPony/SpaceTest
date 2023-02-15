package melonslise.spacetest.core.planet.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class PlanetPropertiesRequestPacket extends PacketByteBuf
{
	public static Identifier ID = PlanetPropertiesRequestPacketHandler.ID;

	public PlanetPropertiesRequestPacket(RegistryKey<World> worldKey)
	{
		super(Unpooled.buffer());
		this.writeRegistryKey(worldKey);
	}
}