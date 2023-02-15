package melonslise.spacetest.core.planet.render;

import melonslise.spacetest.core.planet.PlanetState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

public interface PlanetRenderer
{
	void init(ClientWorld world, WorldRenderer worldRenderer);

	void render(PlanetState planetState, MatrixStack mtx, float tickDelta);

	default void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important)
	{
		this.scheduleRebuildForChunks(minX >> 4, minY >> 4, minZ >> 4, maxX >> 4, maxY >> 4, maxZ >> 4, important);
	}

	default void scheduleRebuildForChunks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important)
	{
		for (int cx = minX; cx <= maxX; cx++)
		{
			for (int cy = minY; cy <= maxY; cy++)
			{
				for (int cz = minZ; cz <= maxZ; cz++)
				{
					this.scheduleRebuild(cx, cy, cz, important);
				}
			}
		}
	}

	void scheduleRebuild(int x, int y, int z, boolean important);

	void close();
}