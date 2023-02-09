package melonslise.spacetest.mixin;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import melonslise.spacetest.world.gen.noise.Noise4dSampler;
import melonslise.spacetest.world.gen.noise.PerlinNoise4d;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OctavePerlinNoiseSampler.class)
public class OctavePerlinNoiseSamplerMixin implements Noise4dSampler
{
	@Shadow
	@Final
	private PerlinNoiseSampler[] octaveSamplers;
	@Shadow
	@Final
	private DoubleList amplitudes;
	@Shadow
	@Final
	private double persistence;
	@Shadow
	@Final
	private double lacunarity;

	@Override
	public double sample(double x, double y, double z, double w)
	{
		double total = 0.0;
		double lacunarity = this.lacunarity;
		double persistence = this.persistence;

		for (int i = 0; i < this.octaveSamplers.length; ++i)
		{
			PerlinNoiseSampler octaveSampler = this.octaveSamplers[i];

			if (octaveSampler != null)
			{
				double noise = PerlinNoise4d.sample(
					OctavePerlinNoiseSampler.maintainPrecision(x * lacunarity) + octaveSampler.originX,
					OctavePerlinNoiseSampler.maintainPrecision(y * lacunarity) + octaveSampler.originY,
					OctavePerlinNoiseSampler.maintainPrecision(z * lacunarity) + octaveSampler.originZ,
					OctavePerlinNoiseSampler.maintainPrecision(w * lacunarity) // FIXME need offset w?
				);

				total += this.amplitudes.getDouble(i) * noise * persistence;
			}

			lacunarity *= 2.0;
			persistence /= 2.0;
		}

		return total;
	}
}