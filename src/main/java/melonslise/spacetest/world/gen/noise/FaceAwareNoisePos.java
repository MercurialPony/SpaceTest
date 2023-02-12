package melonslise.spacetest.world.gen.noise;

import melonslise.spacetest.planet.CubeFaceContext;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public interface FaceAwareNoisePos extends DensityFunction.NoisePos
{
	CubeFaceContext faceCtx();
}