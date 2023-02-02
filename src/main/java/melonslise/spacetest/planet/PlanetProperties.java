package melonslise.spacetest.planet;

import net.minecraft.util.math.ChunkSectionPos;

public interface PlanetProperties
{
	ChunkSectionPos getOrigin();

	int getFaceSize();

	float getStartRadius();

	float getRadiusRatio();
}