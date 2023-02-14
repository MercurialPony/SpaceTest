package melonslise.spacetest.mixin.core.finite_world;

import melonslise.spacetest.core.planet.PlanetProjection;
import melonslise.spacetest.core.planet.world.PlanetWorld;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.tick.SimpleTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * To make a world 'finite' all we need to do is disable certain chunks from being saved/read and generated
 *
 * During worldgen each (proto) chunk goes through a sequence of generation steps defined by ChunkStatuses
 * we can entirely bypass this by setting the chunk as FULL to skip over all the generation (if it is outside the planet cubemap)
 */
@Mixin(ProtoChunk.class)
public class ProtoChunkMixin
{
	@Shadow
	private volatile ChunkStatus status;

	@Shadow
	private volatile LightingProvider lightingProvider;

	@Inject(method = "<init>(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/tick/SimpleTickScheduler;Lnet/minecraft/world/tick/SimpleTickScheduler;Lnet/minecraft/world/HeightLimitView;Lnet/minecraft/registry/Registry;Lnet/minecraft/world/gen/chunk/BlendingData;)V", at = @At("RETURN"))
	private void setToFullIfOutOfPlanetRange(ChunkPos pos, UpgradeData upgradeData, ChunkSection[] sections, SimpleTickScheduler blockTickScheduler, SimpleTickScheduler fluidTickScheduler, HeightLimitView view, Registry biomeRegistry, BlendingData blendingData, CallbackInfo ci)
	{
		ServerWorld serverWorld = (ServerWorld) view;

		if(PlanetProjection.determineFaceInChunks(((PlanetWorld) view).getPlanetProperties(), pos.x, pos.z) == null)
		{
			this.status = ChunkStatus.FULL;
			this.lightingProvider = serverWorld.getLightingProvider();
		}
	}
}