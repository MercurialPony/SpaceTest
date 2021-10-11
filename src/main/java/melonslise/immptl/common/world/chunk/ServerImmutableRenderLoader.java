package melonslise.immptl.common.world.chunk;

import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;

public class ServerImmutableRenderLoader extends ImmutableRenderLoader {
    /**
     *
     * @param owner
     * @param startCorner
     * @param xWidth
     * @param zWidth
     */
    public ServerImmutableRenderLoader(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
    {
        super(owner, startCorner, xWidth, zWidth);
    }
    /**
     *
     * @param request
     */
    public ServerImmutableRenderLoader(RequestImmutableLoader request)
    {
        super(request);
    }
}
