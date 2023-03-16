package melonslise.spacetest.mixin.core.seamless_worldgen;

import melonslise.spacetest.core.seamless_worldgen.noise.FaceAwareNoisePos;
import melonslise.spacetest.core.seamless_worldgen.noise.Noise4dSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DensityFunctionTypes.ShiftedNoise.class)
public class ShiftedNoiseDensityFunctionTypeMixin
{
	@Shadow
	@Final
	private DensityFunction shiftX;
	@Shadow
	@Final
	private DensityFunction shiftY;
	@Shadow
	@Final
	private DensityFunction shiftZ;

	@Shadow
	@Final
	private double xzScale;
	@Shadow
	@Final
	private double yScale;

	@Shadow
	@Final
	private DensityFunction.Noise noise;

	/**
	 * @author Melonslise
	 * @reason add support for our 4d noise implementation
	 */
	@Overwrite
	public double sample(DensityFunction.NoisePos pos)
	{
		if(pos instanceof FaceAwareNoisePos faceAware)
		{
			return ((Noise4dSampler) (Object) this.noise).sample();
		}

		return this.noise.sample(
			pos.blockX() * this.xzScale + this.shiftX.sample(pos),
			pos.blockY() * this.yScale + this.shiftY.sample(pos),
			pos.blockZ() * this.xzScale + this.shiftZ.sample(pos));
	}
}