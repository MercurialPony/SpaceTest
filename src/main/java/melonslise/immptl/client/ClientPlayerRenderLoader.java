package melonslise.immptl.client;

import melonslise.immptl.common.world.chunk.PlayerRenderLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ClientPlayerRenderLoader extends PlayerRenderLoader implements ClientMultiDimensionRenderLoader {

    public ClientPlayerRenderLoader(AbstractClientPlayer player, int viewDistance)
    {
        super(player, viewDistance);
    }

    /**
     * Wrapper method since local and server players have different ways to get their level...
     * Maybe it would be better to store the current level, instead?
     * @return
     */
    @Override
    protected ResourceKey<Level> getCurrentDimension()
    {
        return ((AbstractClientPlayer) this.player).clientLevel.dimension();
    }

    @Override
    public AbstractClientPlayer getPlayer()
    {
        return (AbstractClientPlayer)this.player;
    }

    @Nullable
    @Override
    public Iterable<ChunkRenderDispatcher.RenderChunk> getRenderChunks(ResourceKey<Level> dimension)
    {
        if (this.newDimension.equals(dimension))
        {
            return PlayerViewManager.getRenderChunks(this.newDimension, this.currViewWidth*this.currViewWidth,
                    (consumer) -> this.forEachAdded(this.newDimension, consumer));
        }
        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        return ((obj instanceof ClientPlayerRenderLoader) && (((ClientPlayerRenderLoader) obj).player.equals(this.player)));
    }
}
