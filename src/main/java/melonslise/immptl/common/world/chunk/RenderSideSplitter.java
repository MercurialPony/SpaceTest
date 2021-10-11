package melonslise.immptl.common.world.chunk;

import melonslise.immptl.client.PlayerViewManager;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;

/**
 * Just a wrapper that handles requesting render handling from the appropriate side.
 */
public class RenderSideSplitter {
    public static boolean addImmutableRender(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth, boolean clientSide)
    {
        if (clientSide)
        {
            return PlayerViewManager.addImmutableRenderLoader(owner, startCorner, xWidth, zWidth);
        }
        else
        {
            return RenderLoaderManager.addImmutableRenderLoader(owner, startCorner, xWidth, zWidth);
        }
    }

    public static boolean removeImmutableRender(DimBlockPos owner, boolean clientSide)
    {
        if (clientSide)
        {
            return PlayerViewManager.removeImmutableRenderLoader(owner);
        }
        else
        {
            return RenderLoaderManager.removeImmutableRenderLoader(owner);
        }
    }
}
