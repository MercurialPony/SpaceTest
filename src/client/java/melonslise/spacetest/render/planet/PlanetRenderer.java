package melonslise.spacetest.render.planet;

import melonslise.spacetest.planet.PlanetProperties;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public interface PlanetRenderer
{
	void init(RegistryKey<World> dimensionKey, PlanetProperties planetProps);

	void render(MatrixStack mtx, float tickDelta);
}