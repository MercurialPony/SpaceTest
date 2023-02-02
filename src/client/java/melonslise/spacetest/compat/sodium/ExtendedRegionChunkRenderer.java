package melonslise.spacetest.compat.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;

public interface ExtendedRegionChunkRenderer
{
	default boolean enableBlockFaceCulling(boolean original)
	{
		return original;
	}

	default void beginRender(ChunkCameraContext cameraCtx)
	{
	}
}