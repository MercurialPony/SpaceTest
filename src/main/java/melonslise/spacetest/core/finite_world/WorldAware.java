package melonslise.spacetest.core.finite_world;

import net.minecraft.world.World;

public interface WorldAware
{
	World getWorld();

	void setWorld(World world);
}