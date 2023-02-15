package melonslise.spacetest.mixin.core.planet;

import melonslise.spacetest.core.planet.BasicPlanetProperties;
import melonslise.spacetest.core.planet.world.PlanetWorld;
import melonslise.spacetest.core.planet.world.gen.PlanetChunkGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
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
	private void assignPlanetProperties(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List<Spawner> spawners, boolean shouldTickTime, CallbackInfo ci)
	{
		ServerWorld world = (ServerWorld) (Object) this;
		PlanetWorld planetWorld = (PlanetWorld) world;

		if (dimensionOptions.chunkGenerator() instanceof PlanetChunkGenerator)
		{
			int bottom = world.getBottomY();

			planetWorld.setPlanetProperties(new BasicPlanetProperties(
				ChunkSectionPos.from(0, ChunkSectionPos.getSectionCoord(bottom), 0),
				10,
				world.getSeaLevel() - bottom
			));
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