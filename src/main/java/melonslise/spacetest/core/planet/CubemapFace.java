package melonslise.spacetest.core.planet;

import net.minecraft.util.Util;

public enum CubemapFace
{
	NORTH(0, 0),
	SOUTH(-2, 0),
	EAST(1, 0),
	WEST(-1, 0),
	UP(0, 1),
	DOWN(0, -1);

	private static final CubemapFace[] FACE_BY_OFFSET = Util.make(new CubemapFace[13], array ->
	{
		for(CubemapFace face : CubemapFace.values())
		{
			array[hash(face.offsetX, face.offsetZ)] = face;
		}
	});

	public final int offsetX;
	public final int offsetZ;

	CubemapFace(int planeOffsetX, int planeOffsetZ)
	{
		this.offsetX = planeOffsetX;
		this.offsetZ = planeOffsetZ;
	}

	// Thanks
	// https://stackoverflow.com/questions/919612/mapping-two-integers-to-one-in-a-unique-and-deterministic-way
	// FIXME this produces numbers up to 13, try to shrink that down so the array is smaller too
	public static int hash(int x, int z)
	{
		x += 2;
		z += 1;

		if(x < 0 || z < 0)
		{
			return -1;
		}

		return (z + x) * (z + x + 1) / 2 + z;
	}

	public static CubemapFace from(int offsetX, int offsetZ)
	{
		int hash = hash(offsetX, offsetZ);

		if(hash < 0 || hash >= FACE_BY_OFFSET.length)
		{
			return null;
		}

		return FACE_BY_OFFSET[hash];
	}
}