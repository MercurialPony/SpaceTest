package melonslise.spacetest.planet;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

public interface PlanetProperties
{
	Vec3d getLastPosition();

	Vec3d getPosition();

	Quaternion getLastRotation();

	Quaternion getRotation();

	ChunkSectionPos getOrigin();

	int getFaceSize();

	float getStartRadius();

	float getRadiusRatio();
}