package melonslise.immptl.client;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public interface ClientMultiDimensionRenderLoader {
    @Nullable
    public Iterable<ChunkRenderDispatcher.RenderChunk> getRenderChunks(ResourceKey<Level> dimension);
}
