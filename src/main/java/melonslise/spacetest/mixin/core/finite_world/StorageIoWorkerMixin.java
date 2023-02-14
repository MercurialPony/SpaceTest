package melonslise.spacetest.mixin.core.finite_world;

import melonslise.spacetest.core.planet.PlanetProjection;
import melonslise.spacetest.core.planet.world.PlanetWorld;
import melonslise.spacetest.core.finite_world.WorldAware;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.StorageIoWorker;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * To make a world 'finite' all we need to do is disable certain chunks from being saved/read and generated
 *
 * This class does all the reading from/writing to disk based on the ChunkPoses it is given through setResult
 * we can refuse chunk positions outside our planet cubemap to effectively disable reading and writing of those chunks
 *
 * for this we need to have access to the world that is being read/written (though we will most likely not have access to the world during reading
 * but that's okay because if the chunks weren't written in the first place, then there will be nothing to read anyway)
 * In order to get the world we mixin into all the spots where the IO worker is created, and pass it the corresponding world
 * those would be ThreadedAnvilChunkStorage, EntityChunkDataAccess and SerializingRegionBasedStorage
 * (worth noting that the world context is not available everywhere. Right now that is only in WorldUpdater which should be fine since it only does reading(?))
 */
@Mixin(StorageIoWorker.class)
public class StorageIoWorkerMixin implements WorldAware
{
	private World world;

	@Override
	public World getWorld()
	{
		return this.world;
	}

	@Override
	public void setWorld(World world)
	{
		this.world = world;
	}

	@Inject(method = "setResult", at = @At("HEAD"), cancellable = true)
	private void refuseChunkIfOutOfPlanetRange(ChunkPos pos, @Nullable NbtCompound nbt, CallbackInfoReturnable<CompletableFuture<Void>> cir)
	{
		if(world instanceof PlanetWorld pw && PlanetProjection.determineFaceInChunks(pw.getPlanetProperties(), pos.x, pos.z) == null)
		{
			cir.setReturnValue(CompletableFuture.completedFuture(null));
		}
	}
}