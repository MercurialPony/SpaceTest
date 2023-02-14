package melonslise.spacetest.mixin.core.seamless_worldgen;

import melonslise.spacetest.core.seamless_worldgen.noise.Noise4dSampler;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DoublePerlinNoiseSampler.class)
public class DoublePerlinNoiseSamplerMixin implements Noise4dSampler
{
	@Shadow
	@Final
	private double amplitude;
	@Shadow
	@Final
	private OctavePerlinNoiseSampler firstSampler;
	@Shadow
	@Final
	private OctavePerlinNoiseSampler secondSampler;

	@Override
	public double sample(double x, double y, double z, double w)
	{
		double sx = x * 1.0181268882175227d;
		double sy = y * 1.0181268882175227d;
		double sz = z * 1.0181268882175227d;
		double sw = w * 1.0181268882175227d;

		return (((Noise4dSampler) this.firstSampler).sample(x, y, z, w) + ((Noise4dSampler) this.secondSampler).sample(sx, sy, sz, sw)) * this.amplitude;
	}
}