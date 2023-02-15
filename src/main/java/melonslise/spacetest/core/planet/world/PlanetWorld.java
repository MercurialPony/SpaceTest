package melonslise.spacetest.core.planet.world;

import melonslise.spacetest.core.planet.PlanetProperties;

public interface PlanetWorld
{
	PlanetProperties getPlanetProperties();

	void setPlanetProperties(PlanetProperties props);

	default boolean isPlanet()
	{
		return this.getPlanetProperties() != null;
	}
}