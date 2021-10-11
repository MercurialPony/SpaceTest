package melonslise.immptl.client;

import melonslise.immptl.common.world.chunk.ImmutableRenderLoader;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

import javax.annotation.Nullable;

public class ClientImmutableRenderLoader extends ImmutableRenderLoader implements ClientSingleDimensionRenderLoader {

    /**
     *
     * @param owner
     * @param startCorner
     * @param xWidth
     * @param zWidth
     */
    public ClientImmutableRenderLoader(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
    {
        super(owner, startCorner, xWidth, zWidth);
    }
    /**
     *
     * @param request
     */
    public ClientImmutableRenderLoader(RequestImmutableLoader request)
    {
        super(request);
    }

    @Nullable
    @Override
    public Iterable<ChunkRenderDispatcher.RenderChunk> getRenderChunks()
    {
        return PlayerViewManager.getRenderChunks(this.startCorner.dimension, this.xWidth*this.zWidth, this::forEachCurrent);
    }
}
