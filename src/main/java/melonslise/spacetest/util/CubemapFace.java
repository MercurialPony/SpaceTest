package melonslise.spacetest.util;

import net.minecraft.util.math.Direction;

public enum CubemapFace // corresponds to the switch statement in the shader
{
	NORTH(Direction.NORTH, 0, 0),
	SOUTH(Direction.SOUTH, -2, 0),
	EAST(Direction.EAST, 1, 0),
	WEST(Direction.WEST, -1, 0),
	UP(Direction.UP, 0, 1),
	DOWN(Direction.DOWN,  0, -1);

	public final Direction cubeDirection;
	public final int planeOffsetX;
	public final int planeOffsetZ;

	private CubemapFace(Direction cubeDirection, int planeOffsetX, int planeOffsetZ)
	{
		this.cubeDirection = cubeDirection;
		this.planeOffsetX = planeOffsetX;
		this.planeOffsetZ = planeOffsetZ;
	}
}