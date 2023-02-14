package melonslise.spacetest.core.seamless_worldgen.noise;

import melonslise.spacetest.core.planet.CubeFaceContext;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * In order to achieve seamless noise we need to:
 * - for 2d noise, sample a (2d) slice of 3d noise on the surface of a cube for each face
 * - for 3d noise, sample a (3d) slice of 4d noise on the surface of a cube for each face, and then scroll along the 4th dimension using the height of the block on the face
 * - the 2d noise case can be generalized to the 3d case by taking w = 0 (or any other constant, really)
 *
 * The way minecraft generates its terrain is by sampling and combining various density functions (which are defined in the noise settings json)
 * at the very core though, there are only a couple of actual noise-sampling density functions, and what we can do is
 * convert the position we want to sample at to a higher dimension as described above and pass it to the noise density functions, but then
 * give all the other non-noise-sampling density functions the normal, unaltered world pos so things remain nice and compatible
 *
 * This class acts as a mutable position which is passed to all the density functions for evaluation. We make it aware of the face of the planet (cube)
 * it is on, and then mixin to all the noise-sampling density functions to get the face and convert the position to a higher dimension
 * based on the face before sampling noise, without actually mutating the NoisePos so that other density functions don't mess up
 * (at first I mutated the pos directly and it messed up things like y_clampled_gradient and even the CellCache density function)
 *
 * Note that since we only modify the vanilla noise-sampling density functions, seamless worldgen will NOT work with mods that add their own
 * noise sampling density functions or use their own noise generators
 */
public class PlanetNoiseSampler extends ChunkNoiseSampler implements FaceAware
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

	@Override
	public CubeFaceContext faceCtx()
	{
		return this.faceCtx;
	}
}