package melonslise.spacetest.world.gen.noise;

import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProjection;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class PlanetNoiseSampler extends ChunkNoiseSampler
{
	protected CubeFaceContext faceCtx;

	public PlanetNoiseSampler(CubeFaceContext faceCtx, int horizontalCellCount, NoiseConfig noiseConfig, int startX, int startZ, GenerationShapeConfig generationShapeConfig, DensityFunctionTypes.Beardifying beardifying, ChunkGeneratorSettings chunkGeneratorSettings, AquiferSampler.FluidLevelSampler fluidLevelSampler, Blender blender)
	{
		super(horizontalCellCount, noiseConfig, startX, startZ, generationShapeConfig, beardifying, chunkGeneratorSettings, fluidLevelSampler, blender);
		this.faceCtx = faceCtx;
	}

	public static PlanetNoiseSampler create(
		CubeFaceContext faceCtx,
		Chunk chunk,
		NoiseConfig noiseConfig,
		DensityFunctionTypes.Beardifying beardifying,
		ChunkGeneratorSettings chunkGeneratorSettings,
		AquiferSampler.FluidLevelSampler fluidLevelSampler,
		Blender blender
	) {
		GenerationShapeConfig generationShapeConfig = chunkGeneratorSettings.generationShapeConfig().trimHeight(chunk);
		ChunkPos chunkPos = chunk.getPos();
		int i = 16 / generationShapeConfig.horizontalBlockSize();
		return new PlanetNoiseSampler(faceCtx, i, noiseConfig, chunkPos.getStartX(), chunkPos.getStartZ(), generationShapeConfig, beardifying, chunkGeneratorSettings, fluidLevelSampler, blender);
	}

	public static double test(int blockX, int blockY, int blockZ, int blockW, double xzScale, double yScale, Noise4dSampler sampler)
	{
		double res = sampler.sample(blockX * xzScale, blockY * yScale, blockZ * xzScale, blockW * xzScale);
		//double res = ((DensityFunction.Noise) (Object) sampler).sample(blockX * xzScale, blockY * xzScale, blockZ * yScale);
		return res;
	}

	public static DoublePerlinNoiseSampler octaveSamplur = DoublePerlinNoiseSampler.create(ChunkRandom.RandomProvider.XOROSHIRO.create(3228), -8, 1.0d, 1.0d, 1.0d, 1.0d);

	public static float remap(float v, float minOld, float maxOld, float minNew, float maxNew)
	{
		return minNew + (v - minOld) * (maxNew - minNew) / (maxOld - minOld);
	}

	@Nullable
	@Override
	protected BlockState sampleBlockState()
	{
		Vector3f pos = new Vector3f(this.blockX(), 0.0f, this.blockZ());
		pos.sub(this.faceCtx.minX(), 0.0f, this.faceCtx.minZ());
		pos.div(this.faceCtx.faceSize() * 16); // FIXME rename this
		PlanetProjection.uvToCube(this.faceCtx.face(), pos);
		pos.mul(this.faceCtx.faceSize() * 8);

		double xzScale = 2.0d;
		double yScale = 0.0d;

		/*
		double x = this.blockX() * xzScale;
		double y = this.blockY() * yScale;
		double z = this.blockZ() * xzScale;
		double w = 0.0d;

		double res = ((Noise4dSampler) octaveSamplur).sample(x, y, z, w);

		 */

		double res = ((Noise4dSampler) octaveSamplur).sample(pos.x * xzScale, pos.y * xzScale, pos.z * xzScale, this.blockY() * yScale) / 8.0d;

		res += remap(this.blockY(), -64, 320, 1f, -1f) + 0.3333333d;

		res -= 0.7d;

		return res <= 0.0d ? Blocks.AIR.getDefaultState() : Blocks.STONE.getDefaultState();

		/*
		return this.blockStateSampler.sample(new NoisePos4d()
		{
			@Override
			public int blockX()
			{
				//System.out.println(Arrays.stream(Thread.currentThread().getStackTrace()).map(e -> e.toString()).reduce("", (acc, val) -> acc + System.lineSeparator() + val));
				return x;
			}

			@Override
			public int blockY()
			{
				return y;
			}

			@Override
			public int blockZ()
			{
				return z;
			}

			@Override
			public int blockW()
			{
				return w;
			}
		});

		 */
	}
}