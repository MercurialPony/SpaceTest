package melonslise.spacetest.util;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public enum CubemapDirection // corresponds to the switch statement in the shader
{
	NORTH(Direction.NORTH, new Vec3i(0, 0, 0)),
	SOUTH(Direction.SOUTH, new Vec3i(-2, 0, 0)),
	EAST(Direction.EAST, new Vec3i(1, 0, 0)),
	WEST(Direction.WEST, new Vec3i(-1, 0, 0)),
	UP(Direction.UP, new Vec3i(0, 0, 1)),
	DOWN(Direction.DOWN, new Vec3i(0, 0, -1));

	public final Direction cubeDirection;
	public final Vec3i planeDirection;

	private CubemapDirection(Direction cubeDirection, Vec3i planeDirection)
	{
		this.cubeDirection = cubeDirection;
		this.planeDirection = planeDirection;
	}
}