package melonslise.spacetest.compat;

import melonslise.spacetest.compat.sodium.SodiumPlanetRenderer;
import melonslise.spacetest.render.planet.PlanetRenderer;
import melonslise.spacetest.render.planet.VanillaPlanetRenderer;
import net.fabricmc.loader.api.FabricLoader;

public class PlanetRendererFactory
{
	public static PlanetRenderer createPlanetRenderer()
	{
		if(FabricLoader.getInstance().isModLoaded("sodium"))
		{
			return new SodiumPlanetRenderer();
		}

		return new VanillaPlanetRenderer();
	}
}