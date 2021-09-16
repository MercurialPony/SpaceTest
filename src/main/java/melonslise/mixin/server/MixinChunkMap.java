package melonslise.mixin.server;

import melonslise.mixinInterfaces.server.IChunkMapExtended;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.class)
public class MixinChunkMap implements IChunkMapExtended {
    @Shadow
    private int viewDistance;

    @Override
    public int getViewDistance() {
        return viewDistance;
    }
}
