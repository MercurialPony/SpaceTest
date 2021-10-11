package melonslise.immptl.common.world.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ServerPlayerRenderLoader extends PlayerRenderLoader {

    public ServerPlayerRenderLoader(ServerPlayer player, int viewDistance)
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
        return ((ServerPlayer) this.player).getLevel().dimension();
    }

    @Override
    public ServerPlayer getPlayer()
    {
        return (ServerPlayer)this.player;
    }

    @Override
    public boolean equals(Object obj)
    {
        return ((obj instanceof ServerPlayerRenderLoader) && (((ServerPlayerRenderLoader) obj).player.equals(this.player)));
    }
}
