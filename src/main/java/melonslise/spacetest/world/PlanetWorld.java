package melonslise.spacetest.world;

import melonslise.spacetest.planet.PlanetProperties;

public interface PlanetWorld
{
	PlanetProperties getPlanetProperties();

	void setPlanetProperties(PlanetProperties props);
}