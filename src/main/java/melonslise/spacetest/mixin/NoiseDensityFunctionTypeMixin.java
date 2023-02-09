package melonslise.spacetest.mixin;

import melonslise.spacetest.world.gen.noise.Noise4dSampler;
import melonslise.spacetest.world.gen.noise.NoisePos4d;
import melonslise.spacetest.world.gen.noise.PlanetNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
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
		if(pos instanceof NoisePos4d pos4)
		{
			//return ((Noise4dSampler) (Object) this.noise).sample(pos4.blockX() * this.xzScale, pos4.blockY() * this.xzScale, pos4.blockZ() * this.yScale, pos4.blockW() * xzScale);
			return PlanetNoiseSampler.test(pos4.blockX(), pos4.blockY(), pos4.blockZ(), pos4.blockW(), this.xzScale, this.yScale, (Noise4dSampler) (Object) this.noise);
		}

		return this.noise.sample(pos.blockX() * this.xzScale, pos.blockY() * this.yScale, pos.blockZ() * this.xzScale);
	}
}