package melonslise.spacetest.render.planet;

import melonslise.spacetest.planet.PlanetState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

public interface PlanetRenderer
{
	void init(ClientWorld world, WorldRenderer worldRenderer);

	void render(PlanetState planetState, MatrixStack mtx, float tickDelta);

	void close();
}