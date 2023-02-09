package melonslise.spacetest.mixin;

import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkNoiseSampler.class)
public class TestMixin
{
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/densityfunction/DensityFunctionTypes;cacheAllInCell(Lnet/minecraft/world/gen/densityfunction/DensityFunction;)Lnet/minecraft/world/gen/densityfunction/DensityFunction;"))
	private DensityFunction sus(DensityFunction inputFunction)
	{
		return inputFunction;
	}

	/*
	private static CubemapFace getFace(ServerWorld world, Chunk chunk)
	{
		ChunkPos pos = chunk.getPos();
		return PlanetProjection.determineFace(((PlanetWorld) world).getPlanetProperties(), ChunkSectionPos.getBlockCoord(pos.x), ChunkSectionPos.getBlockCoord(pos.z));
	}

	@Inject(method = "blockX", at = @At("HEAD"))
	private void sus(CallbackInfoReturnable<Integer> cir)
	{
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();

		if(!stes[3].getClassName().equals("net.minecraft.world.gen.densityfunction.DensityFunctionTypes$Noise"))
		{

		}

		System.out.println("=============" + Arrays.stream(stes).map(StackTraceElement::toString).reduce("", (acc, val) -> acc + System.lineSeparator() + val));
	}

	@Inject(method = "populateNoise(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/chunk/Chunk;II)Lnet/minecraft/world/chunk/Chunk;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/chunk/ChunkNoiseSampler;sampleBlockState()Lnet/minecraft/block/BlockState;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
	private void sus(Blender blender, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk, int minimumCellY, int cellHeight, CallbackInfoReturnable<Chunk> cir, ChunkNoiseSampler sampler)
	{
		CubemapFace face = getFace(((ServerWorldAccess) structureAccessor.world).toServerWorld(), chunk);

		if(face == null)
		{
			return;
		}

		System.out.println(face + " + " + chunk.getPos() + " --- " + sampler.blockX() + ", " + sampler.blockY() + ", " + sampler.blockZ());
	}

	 */
}