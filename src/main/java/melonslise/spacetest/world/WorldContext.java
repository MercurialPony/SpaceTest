package melonslise.spacetest.world;

import net.minecraft.world.World;

public interface WorldContext
{
	World getWorld();

	void setWorld(World world);
}