package melonslise.spacetest.core.planet.network;

import io.netty.buffer.Unpooled;
import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.core.planet.PlanetProperties;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class PlanetPropertiesResponsePacket extends PacketByteBuf
{
	public static final Identifier ID = SpaceTestCore.id("planet_props_response");

	public PlanetPropertiesResponsePacket(RegistryKey<World> worldKey, PlanetProperties props)
	{
		super(Unpooled.buffer());
		this.writeRegistryKey(worldKey);

		this.writeBoolean(props == null);

		if(props == null)
		{
			return;
		}

		this.writeChunkSectionPos(props.getOrigin());
		this.writeByte(props.getFaceSize());
		this.writeFloat(props.getStartRadius());
		this.writeFloat(props.getRadiusRatio());
	}
}