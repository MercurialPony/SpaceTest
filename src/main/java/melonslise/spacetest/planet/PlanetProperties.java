package melonslise.spacetest.planet;

import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public interface PlanetProperties
{
	Vector3d getLastPosition();

	Vector3d getPosition();

	Quaternionf getLastRotation();

	Quaternionf getRotation();

	ChunkSectionPos getOrigin();

	int getFaceSize();

	float getStartRadius();

	float getRadiusRatio();
}