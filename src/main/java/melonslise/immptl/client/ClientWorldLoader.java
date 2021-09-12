package melonslise.immptl.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ClientWorldLoader
{
	public static final Map<ResourceKey<Level>, ClientLevel> clientWorldMap = new HashMap<>();
	public static final Map<ResourceKey<Level>, LevelRenderer> worldRendererMap = new HashMap<>();
	// public static final Map<ResourceKey<Level>, DimensionRenderHelper> renderHelperMap = new HashMap<>();

	public static boolean isFlatWorld = false;

	/*
	private static ClientLevel createSecondaryClientWorld(ResourceKey<Level> dimension)
	{
		Minecraft mc = Minecraft.getInstance();
		
		Validate.isTrue(mc.player.level.dimension() != dimension);

		// isCreatingClientWorld = true;

		// mc.getProfiler().push("create_world");

		int chunkLoadDistance = 3;// my own chunk manager doesn't need it

		LevelRenderer worldRenderer = new LevelRenderer(mc, mc.renderBuffers());

		ClientLevel newWorld;
		try
		{
			// multiple net handlers share the same playerListEntries object
			ClientPacketListener mainNetHandler = mc.player.connection;

			ResourceKey<DimensionType> dimensionTypeKey = DimensionTypeSync.getDimensionTypeKey(dimension);
			ClientLevel.ClientLevelData currentProperty = (ClientLevel.ClientLevelData) ((IEWorld) mc.world).myGetProperties();
			RegistryAccess dimensionTracker = mainNetHandler.registryAccess();

			DimensionType dimensionType = dimensionTracker.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(dimensionTypeKey);

			ClientLevel.ClientLevelData properties = new ClientLevel.ClientLevelData(currentProperty.getDifficulty(), currentProperty.isHardcore(), isFlatWorld);
			newWorld = new ClientLevel(mainNetHandler, properties, dimension, dimensionType, chunkLoadDistance, mc::getProfiler, worldRenderer, mc.level.isDebug(), mc.level.getBiomeManager().biomeZoomSeed);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Creating Client World " + dimension + " " + clientWorldMap.keySet(), e);
		}

		worldRenderer.setLevel(newWorld);

		worldRenderer.onResourceManagerReload(mc.getResourceManager());

		clientWorldMap.put(dimension, newWorld);
		worldRendererMap.put(dimension, worldRenderer);

		// Helper.log("Client World Created " + dimension.getValue());

		// isCreatingClientWorld = false;

		// clientWorldLoadSignal.emit(newWorld);

		// client.getProfiler().pop();

		return newWorld;
	}
	*/
}