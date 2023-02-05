package melonslise.spacetest.compat;

import melonslise.spacetest.SpaceTestClient;
import melonslise.spacetest.compat.sodium.SodiumPlanetRenderer;
import melonslise.spacetest.render.planet.PlanetRenderer;
import melonslise.spacetest.render.planet.VanillaPlanetRenderer;

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