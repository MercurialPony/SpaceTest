package melonslise.spacetest.common.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class DimChunkPos {
    public final ResourceKey<Level> dimension;
    public final ChunkPos pos;

    public DimChunkPos(ResourceKey<Level> dim, ChunkPos pos) {
        this.dimension = dim;
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "dimensional chunk position " + pos.toString() + " in dimension " + dimension.location();
    }
}
