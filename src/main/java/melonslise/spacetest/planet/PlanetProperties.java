package melonslise.spacetest.planet;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

public interface PlanetProperties
{
	Vec3d getPosition();

	ChunkSectionPos getOrigin();

	int getFaceSize();

	float getStartRadius();

	float getRadiusRatio();
}