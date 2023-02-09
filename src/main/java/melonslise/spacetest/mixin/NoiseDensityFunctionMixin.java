package melonslise.spacetest.mixin;

import melonslise.spacetest.world.gen.noise.Noise4dSampler;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DensityFunction.Noise.class)
public class NoiseDensityFunctionMixin implements Noise4dSampler
{
	@Shadow
	@Final
	private DoublePerlinNoiseSampler noise;

	@Override
	public double sample(double x, double y, double z, double w)
	{
		return this.noise == null ? 0.0d : ((Noise4dSampler) this.noise).sample(x, y, z, w);
	}
}