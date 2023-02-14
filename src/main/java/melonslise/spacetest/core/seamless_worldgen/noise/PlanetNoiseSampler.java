package melonslise.spacetest.core.seamless_worldgen.noise;

import melonslise.spacetest.core.planets.CubeFaceContext;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;

public class PlanetNoiseSampler extends ChunkNoiseSampler implements FaceAwareNoisePos
{
	protected CubeFaceContext faceCtx;

	// protected final Vector4d pos4d = new Vector4d();

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

	/*
	@Override
	public Vector4d pos4d()
	{
		return this.pos4d;
	}

	@Nullable
	@Override
	protected BlockState sampleBlockState()
	{
		Vector3f newPos = new Vector3f(this.blockX(), 0.0f, this.blockZ());
		newPos.sub(this.faceCtx.minX(), 0.0f, this.faceCtx.minZ());
		newPos.div(this.faceCtx.faceSize() * 16); // FIXME rename this
		PlanetProjection.uvToCube(this.faceCtx.face(), newPos);
		newPos.mul(this.faceCtx.faceSize() * 8);

		this.pos4d.set(newPos, this.blockY());

		return this.blockStateSampler.sample(this);
	}
	 */

	@Override
	public CubeFaceContext faceCtx()
	{
		return this.faceCtx;
	}
}