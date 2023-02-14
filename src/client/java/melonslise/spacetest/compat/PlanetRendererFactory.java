package melonslise.spacetest.compat;

import melonslise.spacetest.SpaceTestClient;
import melonslise.spacetest.compat.sodium.SodiumPlanetRenderer;
import melonslise.spacetest.core.planet.PlanetRenderer;
import melonslise.spacetest.core.planet.VanillaPlanetRenderer;

public class PlanetRendererFactory
{
	public static PlanetRenderer createPlanetRenderer()
	{
		if(SpaceTestClient.isSodiumLoaded)
		{
			return new SodiumPlanetRenderer();
		}

		return new VanillaPlanetRenderer();
	}
}