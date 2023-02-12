package melonslise.spacetest.mixin;

import melonslise.spacetest.planet.PlanetProjection;
import melonslise.spacetest.world.PlanetWorld;
import melonslise.spacetest.world.WorldContext;
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

@Mixin(StorageIoWorker.class)
public class StorageIoWorkerMixin implements WorldContext
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
	private void setResultInjectHead(ChunkPos pos, @Nullable NbtCompound nbt, CallbackInfoReturnable<CompletableFuture<Void>> cir)
	{
		if(world instanceof PlanetWorld pw && PlanetProjection.determineFaceInChunks(pw.getPlanetProperties(), pos.x, pos.z) == null)
		{
			cir.setReturnValue(CompletableFuture.completedFuture(null));
		}
	}
}