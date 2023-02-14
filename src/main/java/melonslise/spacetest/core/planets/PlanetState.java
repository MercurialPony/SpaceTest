package melonslise.spacetest.core.planets;

import org.joml.Quaternionf;
import org.joml.Vector3d;

public interface PlanetState
{
	Vector3d getLastPosition();

	Vector3d getPosition();

	Quaternionf getLastRotation();

	Quaternionf getRotation();
}