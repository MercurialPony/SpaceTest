package melonslise.spacetest.core.seamless_worldgen.noise;

import melonslise.spacetest.core.planets.CubeFaceContext;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public interface FaceAwareNoisePos extends DensityFunction.NoisePos
{
	CubeFaceContext faceCtx();
}