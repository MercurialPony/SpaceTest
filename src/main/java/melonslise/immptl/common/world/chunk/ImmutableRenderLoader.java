package melonslise.immptl.common.world.chunk;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.function.LongConsumer;

/**
 *
 */
public abstract class ImmutableRenderLoader implements SingleDimensionRenderLoader{

    // What to scale the distance returned by.
    protected static final double scaleFactor = 16f/16f;

    protected final DimBlockPos owner;
    protected final DimChunkPos startCorner;
    protected final int xWidth, zWidth;

    /**
     *
     * @param owner
     * @param startCorner
     * @param xWidth
     * @param zWidth
     */
    public ImmutableRenderLoader(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
    {
        this.owner = owner;
        this.startCorner = startCorner;
        this.xWidth = xWidth;
        this. zWidth = zWidth;
    }
    /**
     *
     * @param request
     */
    public ImmutableRenderLoader(RequestImmutableLoader request)
    {
        this.owner = request.owner;
        this.startCorner = request.startCorner;
        this.xWidth = request.xWidth;
        this. zWidth = request.zWidth;
    }

    /**
     * Returns where the actual render loader is located.
     * @return
     */
    @Override
    public DimBlockPos getOwnerLocation()
    {
        return this.owner;
    }

    /**
     * Returns whether or not this renderloader has chunks that were added in the previous update
     * @return
     */
    // FIXME Unused.
    @Override
    public boolean hasAdded() { return false; }

    /**
     * Returns whether or not this renderloader has chunks that were removed in the previous update
     * @return
     */
    // FIXME Unused.
    @Override
    public boolean hasRemoved() { return false; }

    /**
     * Used to run updates specific to the renderloader. Copies the current state to the old state, and updates the
     * current state.
     * Calls to the "Added", "Removed", "Current", and "Old" methods operate on whatever these states are, so this
     * method should be called before calling those methods, on each tick.
     * Maybe have a "setPendingUpdate" method(s), for updating the renderloader's state based on external information?
     * ???
     * Maybe this should take the server's current time (tick time)?
     */
    @Override
    public void update() {}

    @Override
    public void finalizeUpdate() {}

    @Override
    public double getSourceDistanceTo(DimBlockPos pos)
    {
        return scaleFactor*Math.max(this.xWidth, this.zWidth);
    }

    /**
     * Returns if the provided chunk position is in the current chunks
     * @param chunkX
     * @param chunkZ
     * @return
     */
    // FIXME Unused.
    public boolean isChunkPosInCurrent(int chunkX, int chunkZ)
    {
        return Helpers.isInRectangle(chunkX, chunkZ, this.startCorner.pos.x, this.startCorner.pos.z, this.xWidth, this.zWidth);
    }

    /**
     * Returns if the provided chunk position is in the old chunks.
     * @param chunkX
     * @param chunkZ
     * @return
     */
    // FIXME Unused.
    public boolean isChunkPosInOld(int chunkX, int chunkZ)
    {
        return Helpers.isInRectangle(chunkX, chunkZ, this.startCorner.pos.x, this.startCorner.pos.z, this.xWidth, this.zWidth);
    }

    /**
     * Returns whether the provided dimensional block position is in the current chunks.
     * @param dimPos
     * @return
     */
    public boolean isPositionInCurrent(DimBlockPos dimPos)
    {
        if (!this.startCorner.dimension.equals(dimPos.dimension))
        {
            return false;
        }
        return this.isChunkPosInCurrent(SectionPos.blockToSectionCoord(dimPos.pos.getX()), SectionPos.blockToSectionCoord(dimPos.pos.getZ()));
    }

    /**
     * Returns the dimension in which this is loading chunks
     * @return
     */
    // FIXME Unused.
    public ResourceKey<Level> getTargetDimension()
    {
        return this.startCorner.dimension;
    }

    /**
     * Returns all chunk positions (long) which were in the loader's previous state
     * @return
     */
    // FIXME Unused.
    public LongOpenHashSet getAllPrevious()
    {
        return this.getAllCurrent();
    }

    /**
     * Returns all chunk positions (long) which are in the loader's current state
     * @return
     */
    // FIXME Unused.
    public LongOpenHashSet getAllCurrent()
    {
        LongOpenHashSet current = new LongOpenHashSet();
        this.forEachCurrent(current::add);
        return current;
    }

    /**
     * Returns all chunk positions (long) which were removed in the most recent update to the loader
     * @return - Set of removed chunk positions
     */
    // FIXME Unused.
    public LongOpenHashSet getAllRemoved()
    {
        return new LongOpenHashSet();
    }

    /**
     * Returns all chunk positions (long) which were added in the most recent update to the loader
     * @return
     */
    // FIXME Unused.
    public LongOpenHashSet getAllAdded()
    {
        return new LongOpenHashSet();
    }

    /**
     * Method that runs a consumer on each chunk position within the newly added chunks
     * @param consumer
     */
    public void forEachAdded(LongConsumer consumer)
    {

    }

    /**
     * Method that runs a consumer on each chunk position within the recently removed chunks
     * @param consumer
     */
    public void forEachRemoved(LongConsumer consumer)
    {

    }

    /**
     * Method that runs a consumer on each chunk position within the current chunks
     * @param consumer
     */
    // FIXME Unused.
    public void forEachCurrent(LongConsumer consumer)
    {
        Helpers.forEachInRectangularRange(this.startCorner.pos.x, this.startCorner.pos.z, this.xWidth, this.zWidth, (x, z) -> consumer.accept(ChunkPos.asLong(x, z)));
    }

    /**
     * Method that runs a consumer on each chunk position within the old chunks
     * @param consumer
     */
    // FIXME Unused.
    public void forEachOld(LongConsumer consumer)
    {
        Helpers.forEachInRectangularRange(this.startCorner.pos.x, this.startCorner.pos.z, this.xWidth, this.zWidth, (x, z) -> consumer.accept(ChunkPos.asLong(x, z)));
    }

    public String toString()
    {
        return "Immutable loader. Owner: "+this.owner+"; Start corner: "+this.startCorner+"; xWidth, zWidth: "+this.xWidth+","+this.zWidth;
    }

    public static class RequestImmutableLoader
    {
        public final DimBlockPos owner;
        public final DimChunkPos startCorner;
        public final int xWidth, zWidth;

        public RequestImmutableLoader(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
        {
            this.owner = owner;
            this.startCorner = startCorner;
            this.xWidth = xWidth;
            this.zWidth = zWidth;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof RequestImmutableLoader))
            {
                return false;
            }
            return this.owner.equals(((RequestImmutableLoader) obj).owner);
        }

        @Override
        public int hashCode()
        {
            return this.owner.hashCode();
        }
    }
}
