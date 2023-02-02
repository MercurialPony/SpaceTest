package melonslise.spacetest.planet;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;

public record CubeFaceContext(CubemapFace face, int x, int y, int z, int faceSize, int faceHeight)
{
	public CubeFaceContext(CubemapFace face, PlanetProperties props, World world)
	{
		this(face,
			props.getOrigin().getX() + face.offsetX * props.getFaceSize(),
			props.getOrigin().getY(),
			props.getOrigin().getZ() + face.offsetZ * props.getFaceSize(),
			props.getFaceSize(),
			world.countVerticalSections());
	}

	public int minX()
	{
		return ChunkSectionPos.getBlockCoord(this.x);
	}

	public int minY()
	{
		return ChunkSectionPos.getBlockCoord(this.y);
	}

	public int minZ()
	{
		return ChunkSectionPos.getBlockCoord(this.z);
	}
}