package melonslise.spacetest.world;

import melonslise.spacetest.planet.BasicPlanetProperties;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.joml.Vector3d;

public class PlanetState extends PersistentState
{
	public static final String ID = "planet_properties";

	public World world;

	public PlanetState(World world)
	{
		// TODO
		((PlanetWorld) world).setPlanetProperties(new BasicPlanetProperties(new Vector3d(), ChunkSectionPos.from(0, world.getBottomY(), 0), 5, world.getSeaLevel() - world.getBottomY()));
	}

	@Override
	public boolean isDirty()
	{
		return true;
	}

	public static PlanetState readNbt(ServerWorld world, NbtCompound nbt)
	{
		// TODO
		return new PlanetState(world);
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt)
	{
		// TODO
		return nbt;
	}
}