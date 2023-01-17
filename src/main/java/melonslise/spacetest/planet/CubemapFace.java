package melonslise.spacetest.planet;

import net.minecraft.util.Util;

public enum CubemapFace
{
	NORTH(0, 0),
	SOUTH(-2, 0),
	EAST(1, 0),
	WEST(-1, 0),
	UP(0, 1),
	DOWN(0, -1);

	private static final CubemapFace[] FACE_BY_OFFSET = Util.make(new CubemapFace[6], array ->
	{
		for(int i = 0; i < array.length; ++i)
		{
			CubemapFace face = CubemapFace.values()[i];
			array[hash(face.planeOffsetX, face.planeOffsetZ)] = face;
		}
	});

	public final int planeOffsetX;
	public final int planeOffsetZ;

	CubemapFace(int planeOffsetX, int planeOffsetZ)
	{
		this.planeOffsetX = planeOffsetX;
		this.planeOffsetZ = planeOffsetZ;
	}

	public static int hash(int offsetX, int offsetZ)
	{
		return offsetZ == 0 ? offsetX + 2 : (offsetZ + 1) / 2 + 4;
	}

	public static CubemapFace from(int offsetX, int offsetZ)
	{
		return FACE_BY_OFFSET[hash(offsetX, offsetZ)];
	}
}