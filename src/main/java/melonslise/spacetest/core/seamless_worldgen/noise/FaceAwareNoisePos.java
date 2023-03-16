package melonslise.spacetest.core.seamless_worldgen.noise;

import melonslise.spacetest.core.planet.CubeFaceContext;
import melonslise.spacetest.core.planet.PlanetProjection;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.joml.Vector3f;

public interface FaceAwareNoisePos extends DensityFunction.NoisePos
{
	CubeFaceContext faceCtx();

	default Vector3f getPosOnCube() // FIXME cache?
	{
		Vector3f newPos = new Vector3f(this.blockX(), 0.0f, this.blockZ());
		newPos.sub(this.faceCtx().minX(), 0.0f, this.faceCtx().minZ());
		newPos.div(this.faceCtx().faceSize() * 16); // FIXME rename this
		PlanetProjection.uvToCube(this.faceCtx().face(), newPos);
		newPos.mul(this.faceCtx().faceSize() * 8);
		return newPos;
	}
}