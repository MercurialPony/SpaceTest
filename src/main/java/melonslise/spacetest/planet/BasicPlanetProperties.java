package melonslise.spacetest.planet;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BasicPlanetProperties implements PlanetProperties
{
	private final Vec3d position;
	private final ChunkSectionPos origin;
	private final int faceSize;
	private final float startRadius;
	private final float radiusRatio;

	// SeaLevel must be relative to the bottom Y!!!
	public BasicPlanetProperties(Vec3d pos, ChunkSectionPos origin, int faceSize, int seaLevel)
	{
		this.position = pos;
		this.origin = origin;
		this.faceSize = faceSize;
		this.radiusRatio = (faceSize * 64.0f) / (faceSize * 64.0f - 2.0f * MathHelper.PI);
		this.startRadius = 1f / (float) (Math.pow(this.radiusRatio, seaLevel) - Math.pow(this.radiusRatio, seaLevel - 1));
	}

	@Override
	public Vec3d getPosition()
	{
		return this.position;
	}

	@Override
	public ChunkSectionPos getOrigin()
	{
		return this.origin;
	}

	@Override
	public int getFaceSize()
	{
		return this.faceSize;
	}

	@Override
	public float getStartRadius()
	{
		return this.startRadius;
	}

	@Override
	public float getRadiusRatio()
	{
		return this.radiusRatio;
	}
}