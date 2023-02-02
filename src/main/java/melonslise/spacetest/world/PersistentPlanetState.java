package melonslise.spacetest.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

public class PersistentPlanetState extends PersistentState
{
	public static final String ID = "planet_properties";

	public World world;

	public PersistentPlanetState(World world)
	{
		// TODO
		//((PlanetWorld) world).setPlanetProperties(new BasicPlanetProperties(new Vector3d(), ChunkSectionPos.from(0, world.getBottomY(), 0), 5, world.getSeaLevel() - world.getBottomY()));
	}

	@Override
	public boolean isDirty()
	{
		return true;
	}

	public static PersistentPlanetState readNbt(ServerWorld world, NbtCompound nbt)
	{
		// TODO
		return new PersistentPlanetState(world);
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt)
	{
		// TODO
		return nbt;
	}
}