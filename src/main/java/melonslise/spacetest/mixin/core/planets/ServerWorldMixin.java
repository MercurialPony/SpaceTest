package melonslise.spacetest.mixin.core.planets;

import melonslise.spacetest.core.planets.world.gen.PlanetChunkGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.Spawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerWorld.class)
public class ServerWorldMixin
{
	@Inject(method = "<init>", at = @At("RETURN"))
	private void constructorInjectReturn(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List<Spawner> spawners, boolean shouldTickTime, CallbackInfo ci)
	{
		if (dimensionOptions.chunkGenerator() instanceof PlanetChunkGenerator)
		{
			// TODO: Set and sync planet props here
		}

		/*
		ServerWorld world = (ServerWorld) (Object) this;

		world.getPersistentStateManager().getOrCreate(
			tag -> PersistentPlanetState.readNbt(world, tag),
			() -> new PersistentPlanetState(world),
			PersistentPlanetState.ID
		);
		 */
	}
}