package melonslise.spacetest.mixin.core.finite_world;

import com.mojang.datafixers.DataFixer;
import melonslise.spacetest.core.finite_world.WorldAware;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.storage.EntityChunkDataAccess;
import net.minecraft.world.storage.StorageIoWorker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.concurrent.Executor;

@Mixin(EntityChunkDataAccess.class)
public class EntityChunkDataAccessMixin
{
	@Shadow
	@Final
	private StorageIoWorker dataLoadWorker;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void setWorkerWorld(ServerWorld world, Path path, DataFixer dataFixer, boolean dsync, Executor executor, CallbackInfo ci)
	{
		((WorldAware) dataLoadWorker).setWorld(world);
	}
}