package melonslise.spacetest.mixin.core.finite_world;

import com.mojang.datafixers.DataFixer;
import melonslise.spacetest.core.finite_world.WorldAware;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import net.minecraft.world.storage.StorageIoWorker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.function.Function;

@Mixin(SerializingRegionBasedStorage.class)
public class SerializingRegionBasedStorageMixin
{
	@Shadow
	@Final
	private StorageIoWorker worker;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void setWorkerWorld(Path path, Function codecFactory, Function factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, DynamicRegistryManager dynamicRegistryManager, HeightLimitView world, CallbackInfo ci)
	{
		((WorldAware) worker).setWorld((World) world);
	}
}