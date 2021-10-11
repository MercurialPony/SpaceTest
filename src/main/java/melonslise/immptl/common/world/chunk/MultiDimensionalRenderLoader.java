package melonslise.immptl.common.world.chunk;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongConsumer;

/**
 * Interface for multidimensional renderloaders - objects which represent a set of chunks in one or more dimensions
 * that can be watched, and should be sent to players who can see the renderloader.
 * Ultimately, renderloaders can vary in three main ways:
 *      A given renderloader can move (their "owner", which is what is watched); examples would be planets
 *      The chunks they watch can change with time (e.g the radius they watch, or where the chunks
 *          they're watching are located); examples would be players
 *      The chunks that a given watcher "gets" from them can vary depending on the watcher's position relative to
 *          them (e.g. loads in fewer chunks for a watcher that's further away); examples would be Immersive
 *          Portal's portals (look at them from one side, you see something different than from the other).
 *      *Technically, I could also differentiate between renderloaders that can be watched (most of them) and can't
 *          (e.g. players, though I can imagine a mod where you can see through another player's eyes, "watching" them);
 *          as well as between renderloaders that can watch other renderloaders, and renderloaders that can't. But for
 *          this, I won't worry about that.
 * *For this, I will be ignoring the third option - all render loaders will load all their chunks to their watcher(s).
 *      This is because determining what chunks to load to a given player would require iterating over every possible
 *      watcher, since one watcher may load in different chunks than another. You would only be able to short-circuit if
 *      you load all the chunks for a given loader.
 *      Plus, for this mod, I don't think it's necessary.
 *      Maybe eventually, if we want to implement massive planets (60+ chunks across), but not outside of that...
 */
public interface MultiDimensionalRenderLoader extends RenderLoader {

    /**
     * Returns if the provided dimensional chunk position is in the current chunks
     * @param dimension
     * @param chunkPos
     * @return
     */
    // FIXME Unused.
    public boolean isChunkPosInCurrent(ResourceKey<Level> dimension, long chunkPos);

    /**
     * Returns if the provided dimensional chunk position is in the old chunks.
     * @param dimension
     * @param chunkPos
     * @return
     */
    // FIXME Unused.
    public boolean isChunkPosInOld(ResourceKey<Level> dimension, long chunkPos);



    /**
     * Returns if the provided dimensional chunk position is in the current chunks
     * @param dimension
     * @param chunkX
     * @param chunkZ
     * @return
     */
    // FIXME Unused.
    public boolean isChunkPosInCurrent(ResourceKey<Level> dimension, int chunkX, int chunkZ);

    /**
     * Returns if the provided dimensional chunk position is in the old chunks.
     * @param dimension
     * @param chunkX
     * @param chunkZ
     * @return
     */
    // FIXME Unused.
    public boolean isChunkPosInOld(ResourceKey<Level> dimension, int chunkX, int chunkZ);

    // TODO maybe add a version of isChunkPosIn___ that takes a blockpos/dimBlockpos?

    /**
     * Returns the dimension in which this is loading chunks
     * @return
     */
    // FIXME Unused.
    public Set<ResourceKey<Level>> getTargetDimensions();

    /**
     * Returns all chunk positions (long) which were in the loader's previous state
     * @return
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllOld();

    /**
     * Returns all chunk positions (long) which are in the loader's current state
     * @return
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllCurrent();

    /**
     * Returns all chunk positions (long) which were removed in the most recent update to the loader
     * @return - Set of removed chunk positions
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllRemoved();

    /**
     * Returns all chunk positions (long) which were added in the most recent update to the loader
     * @return
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllAdded();

    /**
     * Method that runs a consumer on each chunk position within the newly added chunks, for all dimensions.
     * @param consumer
     */
    public void forEachAdded(Function<ResourceKey<Level>, LongConsumer> consumer);

    /**
     * Method that runs a consumer on each chunk position within the recently removed chunks, for all dimensions.
     * @param consumer
     */
    public void forEachRemoved(Function<ResourceKey<Level>, LongConsumer> consumer);

    /**
     * Method that runs a consumer on each chunk position within the current chunks, for all dimensions.
     * @param consumer
     */
    // FIXME Unused.
    public void forEachCurrent(Function<ResourceKey<Level>, LongConsumer> consumer);

    /**
     * Method that runs a consumer on each chunk position within the old chunks, for all dimensions.
     * @param consumer
     */
    // FIXME Unused.
    public void forEachOld(Function<ResourceKey<Level>, LongConsumer> consumer);

    /**
     * Method that runs a consumer on each chunk position within the newly added chunks, for a specific dimension.
     * @param dimension
     * @param consumer
     */
    public void forEachAdded(ResourceKey<Level> dimension, LongConsumer consumer);

    /**
     * Method that runs a consumer on each chunk position within the recently removed chunks, for a specific dimension.
     * @param dimension
     * @param consumer
     */
    public void forEachRemoved(ResourceKey<Level> dimension, LongConsumer consumer);

    /**
     * Method that runs a consumer on each chunk position within the current chunks, for a specific dimension.
     * @param dimension
     * @param consumer
     */
    // FIXME Unused.
    public void forEachCurrent(ResourceKey<Level> dimension, LongConsumer consumer);

    /**
     * Method that runs a consumer on each chunk position within the old chunks, for a specific dimension.
     * @param dimension
     * @param consumer
     */
    // FIXME Unused.
    public void forEachOld(ResourceKey<Level> dimension, LongConsumer consumer);
}
