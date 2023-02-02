package melonslise.spacetest.planet;

import org.joml.Quaternionf;
import org.joml.Vector3d;

public class BasicPlanetState implements PlanetState
{
	private final Vector3d lastPosition;
	private final Vector3d position;
	private final Quaternionf lastRotation = new Quaternionf();
	private final Quaternionf rotation = new Quaternionf();

	public BasicPlanetState(Vector3d pos)
	{
		this.lastPosition = pos;
		this.position = pos;
	}

	@Override
	public Vector3d getLastPosition()
	{
		return this.lastPosition;
	}

	@Override
	public Vector3d getPosition()
	{
		return this.position;
	}

	@Override
	public Quaternionf getLastRotation()
	{
		return this.lastRotation;
	}

	@Override
	public Quaternionf getRotation()
	{
		return this.rotation;
	}
}