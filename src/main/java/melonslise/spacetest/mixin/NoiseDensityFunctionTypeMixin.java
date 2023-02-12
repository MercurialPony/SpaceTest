package melonslise.spacetest.mixin;

import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProjection;
import melonslise.spacetest.world.gen.noise.FaceAwareNoisePos;
import melonslise.spacetest.world.gen.noise.Noise4dSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DensityFunctionTypes.Noise.class)
public class NoiseDensityFunctionTypeMixin
{
	@Shadow
	@Final
	private DensityFunction.Noise noise;
	@Shadow
	@Final
	private double xzScale;
	@Shadow
	@Final
	private double yScale;

	/**
	 * @author Melonslise
	 * @reason add support for our 4d noise implementation
	 */
	@Overwrite
	public double sample(DensityFunction.NoisePos pos)
	{
		// FIXME memoize
		if(pos instanceof FaceAwareNoisePos fpos)
		{
			CubeFaceContext faceCtx = fpos.faceCtx();

			Vector3f newPos = new Vector3f(fpos.blockX(), 0.0f, fpos.blockZ());
			newPos.sub(faceCtx.minX(), 0.0f, faceCtx.minZ());
			newPos.div(faceCtx.faceSize() * 16); // FIXME rename this
			PlanetProjection.uvToCube(faceCtx.face(), newPos);
			newPos.mul(faceCtx.faceSize() * 8);

			return ((Noise4dSampler) (Object) this.noise).sample(newPos.x * this.xzScale, newPos.y * this.xzScale, newPos.z * this.xzScale, fpos.blockY() * yScale);
		}

		return this.noise.sample(pos.blockX() * this.xzScale, pos.blockY() * this.yScale, pos.blockZ() * this.xzScale);
	}
}