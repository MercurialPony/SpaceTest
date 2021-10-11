package melonslise.immptl.common.world.chunk;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import melonslise.immptl.util.DimBlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;


public abstract class PlayerRenderLoader implements VariableRenderLoader, MultiDimensionalRenderLoader {
    protected Player player;
    protected int currViewDistance;
    protected int oldViewDistance;
    protected int currViewWidth;
    protected int oldViewWidth;
    protected ChunkPos newPos;
    protected ChunkPos oldPos;
    protected ResourceKey<Level> oldDimension;
    protected ResourceKey<Level> newDimension;

    protected PlayerRenderLoader(Player player, int viewDistance)
    {
        this.player = player;
        this.currViewDistance = viewDistance;
        this.oldViewDistance = this.currViewDistance;
        this.currViewWidth = Helpers.getViewWidth(this.currViewDistance);
        this.oldViewWidth = this.currViewWidth;
        this.newPos = player.chunkPosition();
        this.oldPos = this.newPos;
        this.newDimension = this.getCurrentDimension();
        this.oldDimension = this.newDimension;
    }

    public abstract Player getPlayer();

    /**
     * Wrapper method since local and server players have different ways to get their level...
     * @return
     */
    protected abstract ResourceKey<Level> getCurrentDimension();

    @Override
    public void update()
    {
        // Have to do this, since it seems that when this update method is called, the player's old positions are
        // already updated to the new position - that, or (what's more likely) if this is only called every 20 ticks,
        // and the player's position is updated every tick, it's very likely the tick where the player's chunk position
        // changes is missed; by the time this gets called upon, the player's old position will be their most recent
        // old position - not the position that was in another chunk.
        this.oldPos = this.newPos;
        this.newPos = player.chunkPosition();
        this.oldDimension = this.newDimension;
        this.newDimension = this.getCurrentDimension();
//        SpaceTest.LOGGER.info("Player's current chunk position: "+this.newPos
//                +"\nPlayer's old chunk position: "+this.oldPos);
    }

    /**
     * Finalizes updates run in this renderloader - meaning, the old state is overwritten - "forgets" which chunks were added/removed.
     */
    @Override
    public void finalizeUpdate()
    {
        this.oldViewDistance = this.currViewDistance;
        this.oldViewWidth = this.currViewWidth;
        this.oldPos = this.newPos;
        this.oldDimension = this.newDimension;
    }

    @Override
    public boolean isChunkPosInCurrent(ResourceKey<Level> dimension, long chunkPos)
    {
        return this.isChunkPosInCurrent(dimension, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    @Override
    public boolean isChunkPosInOld(ResourceKey<Level> dimension, long chunkPos)
    {
        return this.isChunkPosInOld(dimension, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    /**
     * Returns if the provided dimensional chunk position is in the current chunks
     * @param dimension
     * @param chunkX
     * @param chunkZ
     * @return
     */
    // FIXME Unused.
    @Override
    public boolean isChunkPosInCurrent(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        if (!dimension.equals(this.newDimension))
        {
            return false;
        }
        return Helpers.isInRectangle(chunkX, chunkZ, this.newPos.x-this.currViewDistance, this.newPos.z-this.currViewDistance,
                this.currViewWidth, this.currViewWidth);
    }

    /**
     * Returns if the provided dimensional chunk position is in the old chunks.
     * @param dimension
     * @param chunkX
     * @param chunkZ
     * @return
     */
    // FIXME Unused.
    @Override
    public boolean isChunkPosInOld(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        if (!dimension.equals(this.oldDimension))
        {
            return false;
        }
        return Helpers.isInRectangle(chunkX, chunkZ, this.oldPos.x-this.currViewDistance, this.oldPos.z-this.currViewDistance,
                this.currViewWidth, this.currViewWidth);
    }

    /**
     * Returns whether the provided dimensional block position is in the current chunks.
     * @param dimPos
     * @return
     */
    @Override
    public boolean isPositionInCurrent(DimBlockPos dimPos)
    {
        return this.isChunkPosInCurrent(dimPos.dimension, SectionPos.blockToSectionCoord(dimPos.pos.getX()), SectionPos.blockToSectionCoord(dimPos.pos.getZ()));
    }

    @Override
    public boolean hasAdded() {
        return !(this.newPos.equals(this.oldPos)
        && this.newDimension.equals(this.oldDimension));
    }

    @Override
    public boolean hasRemoved() {
        return !(this.newPos.equals(this.oldPos)
                && this.newDimension.equals(this.oldDimension));
    }

    // TODO Do I need to worry about the case where both view distance and position change?
    //      How do I discern between the two types of updates?
    @Override
    public Set<ResourceKey<Level>> getTargetDimensions()
    {
        return new HashSet<>();
    }

    public ResourceKey<Level> getTargetDimension() {
        return this.newDimension;
    }

    @Override
    public DimBlockPos getOwnerLocation()
    {
        return new DimBlockPos(this.newDimension, this.player.blockPosition());
    }

    private boolean changedDimension()
    {
        return !this.newDimension.equals(this.oldDimension);
    }

    private Map<ResourceKey<Level>, LongOpenHashSet> getAllGeneral(Consumer<Function<ResourceKey<Level>, LongConsumer>> consumer)
    {
        Map<ResourceKey<Level>, LongOpenHashSet> dimMap = new HashMap<>();
        consumer.accept((dim) -> dimMap.computeIfAbsent(dim, (d) -> new LongOpenHashSet())::add);
        return dimMap;
    }

    @Override
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllCurrent() {
        return this.getAllGeneral(this::forEachCurrent);
    }

    /**
     * Returns all chunk positions which were added since the player moved, for the current view distance only.
     * Computes based upon the current and old positions stored in it.
     * @return - set of chunk positions (long)
     */
    @Override
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllAdded() {
        return this.getAllGeneral(this::forEachAdded);
    }

    @Override
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllOld() {
        return this.getAllGeneral(this::forEachOld);
    }

    @Override
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllRemoved() {
        return this.getAllGeneral(this::forEachRemoved);
    }

    /**
     * Runs a method on every chunk position that was added since the player moved, for the current view distance only.
     * @param consumer - method to run.
     */
    @Override
    public void forEachAdded(ResourceKey<Level> dimension, LongConsumer consumer) {
//        SpaceTest.LOGGER.info("PlayerRenderLoader#forEachAdded called.");
        if (dimension.equals(this.newDimension)) // Can only ever have added chunks in the current dimension.
        {
            if (this.changedDimension())
            {
                // Changed dimensions, so this has added all chunks around it in the new dimension.
                Helpers.forEachInRectangularRange(this.newPos.x-this.currViewDistance, this.newPos.z-this.currViewDistance,
                        this.currViewWidth, this.currViewWidth, (x,z) -> consumer.accept(ChunkPos.asLong(x, z)));
            }
            else
            {
                Helpers.forEachInRectangularExcluded(this.newPos.x-this.currViewDistance, this.newPos.z-this.currViewDistance,
                        this.currViewWidth, this.currViewWidth,
                        this.oldPos.x-this.currViewDistance, this.oldPos.z-this.currViewDistance,
                        (x, z) -> consumer.accept(ChunkPos.asLong(x, z)));
            }
        }
    }

    /**
     * Runs a method on every chunk position that was added since the player moved, for the current view distance only.
     * @param consumer - method to run.
     */
    @Override
    public void forEachRemoved(ResourceKey<Level> dimension, LongConsumer consumer) {
//        SpaceTest.LOGGER.info("PlayerRenderLoader#forEachRemoved called.");
        // If we changed dimensions, we only have removed chunks in the old dimension; otherwise, we only have removed
        // chunks in the new dimension.
        if (dimension.equals(this.oldDimension))
        {
            if (this.changedDimension())
            {
                // If we changed dimensions, all of our chunks in the previous dimension were removed.
                Helpers.forEachInRectangularRange(this.oldPos.x-this.currViewDistance, this.oldPos.z-this.currViewDistance,
                        this.currViewWidth, this.currViewWidth,
                        (x, z) -> {consumer.accept(ChunkPos.asLong(x, z));});
            }
            else
            {
                Helpers.forEachInRectangularExcluded(this.oldPos.x-this.currViewDistance, this.oldPos.z-this.currViewDistance,
                        this.currViewWidth, this.currViewWidth,
                        this.newPos.x-this.currViewDistance, this.newPos.z-this.currViewDistance,
                        (x, z) -> {consumer.accept(ChunkPos.asLong(x, z));});
            }
        }
    }

    @Override
    public void forEachCurrent(ResourceKey<Level> dimension, LongConsumer consumer) {
        // Is wrapping the "add" method in a lambda expression a serious performance issue?
        if (dimension.equals(this.newDimension))
        {
            Helpers.forEachInRectangularRange(this.newPos.x-currViewDistance,
                    this.newPos.z-currViewDistance,
                    this.currViewWidth, this.currViewWidth,
                    (x, z) -> {consumer.accept(ChunkPos.asLong(x, z));});
        }
    }

    @Override
    public void forEachOld(ResourceKey<Level> dimension, LongConsumer consumer) {
        // If we changed dimensions, we only have old chunks in the old dimension; otherwise, we only
        // have old chunks in the current dimension.
        // Since in the case we didn't
        if (this.oldDimension.equals(dimension))
        {
            Helpers.forEachInRectangularRange(this.oldPos.x - currViewDistance,
                this.oldPos.z - currViewDistance,
                this.currViewWidth, this.currViewWidth,
                (x, z) -> {
                    consumer.accept(ChunkPos.asLong(x, z));
                });
        }
    }

    /**
     * Method that runs a consumer on each chunk position within the newly added chunks, for all dimensions.
     * @param consumer
     */
    public void forEachAdded(Function<ResourceKey<Level>, LongConsumer> consumer)
    {
        this.forEachAdded(this.newDimension, consumer.apply(this.newDimension));
    }

    /**
     * Method that runs a consumer on each chunk position within the recently removed chunks, for all dimensions.
     * @param consumer
     */
    public void forEachRemoved(Function<ResourceKey<Level>, LongConsumer> consumer)
    {
        this.forEachRemoved(this.oldDimension, consumer.apply(this.oldDimension));
    }

    /**
     * Method that runs a consumer on each chunk position within the current chunks, for all dimensions.
     * @param consumer
     */
    // FIXME Unused.
    public void forEachCurrent(Function<ResourceKey<Level>, LongConsumer> consumer)
    {
        this.forEachCurrent(this.newDimension, consumer.apply(this.newDimension));
    }

    /**
     * Method that runs a consumer on each chunk position within the old chunks, for all dimensions.
     * @param consumer
     */
    // FIXME Unused.
    public void forEachOld(Function<ResourceKey<Level>, LongConsumer> consumer)
    {
        this.forEachOld(this.oldDimension, consumer.apply(this.oldDimension));
    }

    // View distance update stuff
    // I think it's safe to not worry about dimensional stuff in this?

    /**
     * Updates the renderloader's view distance.
     * @param newViewDistance
     */
    @Override
    public void updateViewDistance(int newViewDistance)
    {
        this.oldViewDistance = this.currViewDistance;
        this.currViewDistance = newViewDistance;

        this.oldViewWidth = Helpers.getViewWidth(this.oldViewDistance);
        this.currViewWidth = Helpers.getViewWidth(this.currViewDistance);
    }

    @Override
    public int getBlockViewDistance()
    {
        return this.currViewDistance*16;
    }
//    /**
//     * Finalizes a view distance update for the renderloader, overwriting the copy of the old view distance with the
//     * new view distance.
//     * Should be called when all work involving the view distance update is complete.
//     * TODO Is this necessary?
//     */
//    @Override
//    public void finalizeViewDistanceUpdate()
//    {
//        this.currViewDistance = this.oldViewDistance;
//    }

    /**
     * Returns all chunk positions (long) that were added to watching by a view distance change.
     * @return - A set of chunk positions (long).
     */
    public LongOpenHashSet getViewAdded()
    {
        //return this.getAllGeneral();
        LongOpenHashSet added = new LongOpenHashSet();
        this.forEachViewAdded(added::add);
        return added;
    }

    /**
     * Returns all chunk positions (long) that were added to watching by a view distance change.
     * @return - A set of chunk positions (long).
     */
    public LongOpenHashSet getViewRemoved()
    {
        LongOpenHashSet removed = new LongOpenHashSet();
        this.forEachViewRemoved(removed::add);
        return removed;
    }

    /**
     * Returns whether or not chunks were added to the renderloader's chunks due to a view distance change.
     * @return
     */
    @Override
    public boolean hasViewAdded()
    {
        return this.currViewWidth > this.oldViewWidth;
    }

    /**
     * Returns whether or not chunks were removed from the renderloader's chunks due to a view distance change.
     * @return
     */
    @Override
    public boolean hasViewRemoved()
    {
        return this.currViewWidth < this.oldViewWidth;
    }

    /**
     * Executes a provided method for every chunk that was added to watching by a view distance change.
     * Note that it uses its latest copy of the player's CURRENT position, unlike vanilla.
     * This is because this assumes that any work done when calling the update/updateViewDistance will be processed
     * right away.
     * Because of that, it assumes that when it's called, any work involving the player moving (chunkmap being modified,
     * loaders added/removed, etc.) has been completed for the last player position - so, it wouldn't entirely make sense
     * to use the old position, if that's already done.
     * @param consumer - method to execute.
     */
    @Override
    public void forEachViewAdded(LongConsumer consumer)
    {
        // Won't have any added chunks if the view distance shrank.
        // Note that view distance updates use the player's OLD position, just like vanilla does.
        if (this.hasViewAdded())
        {
            // For all positions in the new view distance square.
            Helpers.forEachInRectangularRange(this.newPos.x-this.currViewDistance, this.newPos.z-this.currViewDistance,
                    this.currViewWidth, this.currViewWidth, (x, z) -> {
                // Exclude the old view distance square.
                if (!Helpers.isInRectangle(x, z, this.newPos.x-this.oldViewDistance, this.newPos.z-this.oldViewDistance,
                        this.oldViewWidth, this.oldViewWidth))
                {
                    consumer.accept(ChunkPos.asLong(x, z));
                }
            });
        }
    }

    /**
     * Executes a provided method for every chunk that was removed from watching by a view distance change.
     * Note that it uses its latest copy of the player's CURRENT position, unlike vanilla.
     * This is because this assumes that any work done when calling the update/updateViewDistance will be processed
     * right away.
     * Because of that, it assumes that when it's called, any work involving the player moving (chunkmap being modified,
     * loaders added/removed, etc.) has been completed for the last player position - so, it wouldn't entirely make sense
     * to use the old position, if that's already done.
     * @param consumer - method to execute.
     */
    @Override
    public void forEachViewRemoved(LongConsumer consumer)
    {
        // Won't have any removed chunks if the view distance increased.
        // Note that view distance updates use the player's OLD position, just like vanilla does.
        if (this.hasViewRemoved())
        {
            // For all positions in the old view distance square.
            Helpers.forEachInRectangularRange(this.newPos.x-this.oldViewDistance, this.newPos.z-this.oldViewDistance,
                    this.oldViewWidth, this.oldViewWidth, (x, z) -> {
                        // Exclude the new view distance square.
                        if (!Helpers.isInRectangle(x, z, this.newPos.x-this.currViewDistance, this.newPos.z-this.currViewDistance,
                                this.currViewWidth, this.currViewWidth))
                        {
                            consumer.accept(ChunkPos.asLong(x, z));
                        }
                    });
        }
    }

    @Override
    public double getSourceDistanceTo(DimBlockPos targetPos)
    {
        if (targetPos.dimension.equals(this.newDimension))
        {
            // Checkerboard distance to match Vanilla.
            return Math.max(
                    targetPos.pos.getX()-this.newPos.getMiddleBlockX(),
                    targetPos.pos.getZ()-this.newPos.getMiddleBlockZ());
        }
        else
        {
            return Double.POSITIVE_INFINITY;
        }
    }

    // Should only be one PlayerRenderLoader per player.
    @Override
    public boolean equals(Object obj)
    {
        throw new UnsupportedOperationException("PlayerRenderLoader subclass didn't implement equals()!");
    }

    @Override
    public int hashCode()
    {
        return this.player.hashCode();
    }

    @Override
    public String toString()
    {
        return "Render loader for player: " + this.player;
    }

    protected void playerRespawned(Player newPlayer) {
        if (newPlayer.hashCode() == this.player.hashCode())
        {
            this.player = newPlayer;
        }
        else
        {
            throw new RuntimeException("Player respawn error! New player instance didn't have the same ID as the old player instance!"
                    +" Old player: "+this.player+"; New Player: "+newPlayer);
        }
    }
}
