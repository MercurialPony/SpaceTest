package melonslise.spacetest.render.planet.sodium;

import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProperties;

public interface PlanetRenderOptions
{
	CubeFaceContext faceCtx();

	PlanetProperties planetProps();
}