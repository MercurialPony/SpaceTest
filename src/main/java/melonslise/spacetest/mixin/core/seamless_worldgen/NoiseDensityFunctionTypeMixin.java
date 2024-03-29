package melonslise.spacetest.mixin.core.seamless_worldgen;

import melonslise.spacetest.core.seamless_worldgen.noise.FaceAwareNoisePos;
import melonslise.spacetest.core.seamless_worldgen.noise.Noise4dSampler;
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
	 *
	 * Reason behind using y as w is described in PlanetNoiseSampler
	 */
	@Overwrite
	public double sample(DensityFunction.NoisePos pos)
	{
		// FIXME memoize
		if(pos instanceof FaceAwareNoisePos faceAware)
		{
			Vector3f cubePos = faceAware.getPosOnCube();
			return ((Noise4dSampler) (Object) this.noise).sample(cubePos.x * this.xzScale, cubePos.y * this.xzScale, cubePos.z * this.xzScale, pos.blockY() * yScale);
		}

		return this.noise.sample(pos.blockX() * this.xzScale, pos.blockY() * this.yScale, pos.blockZ() * this.xzScale);
	}
}