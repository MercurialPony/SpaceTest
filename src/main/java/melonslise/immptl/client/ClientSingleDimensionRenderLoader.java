package melonslise.immptl.client;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

import javax.annotation.Nullable;

public interface ClientSingleDimensionRenderLoader extends ClientRenderLoader {
    @Nullable
    public Iterable<ChunkRenderDispatcher.RenderChunk> getRenderChunks();
}
