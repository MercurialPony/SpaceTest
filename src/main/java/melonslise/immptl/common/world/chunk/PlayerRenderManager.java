package melonslise.immptl.common.world.chunk;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import melonslise.immptl.util.DimBlockPos;
import melonslise.spacetest.SpaceTest;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Marker;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.*;

/**
 * Class that manages the renderloading for a given player.
 * Current assumptions are that:
 *  1. Renderloaders don't move (no orbiting planets)
 *  2. There can't be any renderloader cycles (where loader A loads loader B's owner, and loader B loads loader A's owner)
 *  3. There aren't any "weird" renderloaders, which can load a chunk the player just unloaded (e.g. they walk into
 *      range of the renderloader, loading it, and causing a chunk they unloaded to be reloaded in the same update).
 *      I guess this would just be a different cycle than #2.
 * #1 will be easy to deal with, I think, but #2 and #3 are far more challenging, it seems.
 * I.e., this currently only handles renderloaders (aside from the player) which do not move, and do not change the chunks they load.
 */
public class PlayerRenderManager implements MultiDimensionalRenderLoader {

    private ServerPlayerRenderLoader playerRenderLoader;

    // Map of all the chunks the player is watching, along with how many renderloaders associated with them are watching that chunk.
    private Map<ResourceKey<Level>, Long2IntOpenHashMap> chunkMap = new HashMap<>();

    // Map that all updates are added to, before being applied to the main map.
    private Map<ResourceKey<Level>, Long2IntOpenHashMap> updateMap = new HashMap<>();

    // Map of all chunks that were added in the last update, for every dimension
    private Map<ResourceKey<Level>, LongOpenHashSet> addedMap = new HashMap<>();

    // Map of all chunks that were removed in the last update, for every dimension
    private Map<ResourceKey<Level>, LongOpenHashSet> removedMap = new HashMap<>();

    // Map of all loaders to their corresponding View
    private HashMap<RenderLoader, View> loadersMap = new HashMap<>();
    // Sort in reverse order to make pulling the closest loader off it faster.
    private SortedArraySet<RenderLoader> pendingLoaders = SortedArraySet.create(Collections.reverseOrder(Comparator.comparing(this.loadersMap::get)), 10);

    private static final Long2IntOpenHashMap emptyWatchCountMap = new Long2IntOpenHashMap();
    private static final LongOpenHashSet emptyChunkSet = new LongOpenHashSet();

    public PlayerRenderManager(@Nonnull Object masterManager, ServerPlayer player, int viewDistance) {
        this.playerRenderLoader = new ServerPlayerRenderLoader(player, viewDistance);
        this.loadersMap.put(this.playerRenderLoader, new EnterOnceView(this.playerRenderLoader,0));
        this.generateChunkmap();
    }

    private void generateChunkmap()
    {
        SpaceTest.LOGGER.info("Generating chunkmap for "+this+".");
        this.deletePending();
        this.pendingLoaders.addAll(this.loadersMap.keySet());

        double viewRange = this.playerRenderLoader.getBlockViewDistance();
        //SpaceTest.LOGGER.info("View range: "+viewRange);

        //SpaceTest.LOGGER.info("Iterating over list of loaders");
        while (!this.pendingLoaders.isEmpty())
        {
            RenderLoader loader = this.pendingLoaders.last();
            this.pendingLoaders.remove(loader);
            View loaderView = this.loadersMap.get(loader);
//            SpaceTest.LOGGER.info("Processing loader: "+loader);
            switch (loaderView.getThresholdStatus(viewRange))
            {
                case ENTERED:
                    //SpaceTest.LOGGER.info("Loader entered view range.");
                    // Add all chunks in this loader.
                    if (loader instanceof SingleDimensionRenderLoader)
                    {
                        ((SingleDimensionRenderLoader) loader).forEachCurrent(this.addChunks(
                                ((SingleDimensionRenderLoader) loader).getTargetDimension(),
                                loaderView.getCurrDistance(), loader::getSourceDistanceTo));
                    }
                    else if (loader instanceof MultiDimensionalRenderLoader)
                    {
                        ((MultiDimensionalRenderLoader) loader).forEachCurrent((dim) ->
                                this.addChunks(dim, loaderView.getCurrDistance(), loader::getSourceDistanceTo));
                    }
                    else
                    {
                        SpaceTest.LOGGER.warn("Unhandled loader passed during #generateChunkmaps! Loader: "+loader);
                    }
                    break;
                case STAYED_OUTSIDE:
                    // Loader is in the player's chunks, but it isn't within render range.
                    break;
                default:
                    // Shouldn't be possible.
                    dumpManagerAndCrash("Encountered illegal threshold state while creating render maps for "
                            +this+"! State: "+loaderView.getThresholdStatus(this.playerRenderLoader.getBlockViewDistance()));
                    break;
            }
        }
        this.applyChanges();
        this.loadersMap.values().forEach(View::finalizeUpdate);

        // If the distance stayed within loading range, add new chunks and remove old chunks
        // If the distance entered loading range, add all current chunks
        // If the distance exited loading range, remove all old chunks.
        // If the distance stayed outside loading range, something went wrong.
    }

    public void destroyChunkMap()
    {
        this.deletePending();
        this.chunkMap.forEach((dim, map) -> this.removedMap.put(dim, new LongOpenHashSet(map.keySet())));
        this.chunkMap.clear();
        this.loadersMap.clear();
    }

    private void deletePending()
    {
        this.addedMap.clear();
        this.removedMap.clear();
    }

    public ServerPlayer getPlayer() {
        return this.playerRenderLoader.getPlayer();
    }


    /**
     * Returns the dimension in which this is loading chunks
     *
     * @return
     */
    @Override
    public Set<ResourceKey<Level>> getTargetDimensions() {
        return this.chunkMap.keySet();
    }

    /**
     * Returns where the render manager's player is located.
     *
     * @return
     */
    @Override
    public DimBlockPos getOwnerLocation() {
        return this.playerRenderLoader.getOwnerLocation();
    }

    /**
     * Returns whether or not this renderloader has chunks that were added in the previous update
     *
     * @return
     */
    @Override
    public boolean hasAdded() {
        return !this.addedMap.isEmpty();
    }

    /**
     * Returns whether or not this renderloader has chunks that were removed in the previous update
     *
     * @return
     */
    @Override
    public boolean hasRemoved() {
        return !this.removedMap.isEmpty();
    }

    /**
     * Yields a LongConsumer which adds every chunk supplied to it to the manager's chunkmaps, for the given dimension.
     * @param dimension
     * @return
     */
    public LongConsumer addChunks(ResourceKey<Level> dimension, double sourceDistance, ToDoubleFunction<DimBlockPos> distanceFromSource)
    {
        // TODO handle view distance.
        Long2IntOpenHashMap dimMap = this.updateMap.computeIfAbsent(dimension, (d) -> new Long2IntOpenHashMap());
        return (chunkPos) -> {

            dimMap.addTo(chunkPos, 1);
            // Process all renderloaders at this chunk.
            RenderLoaderManager.forEachRenderLoader(dimension, chunkPos, (RenderLoader loader) -> {
                if (loader instanceof SingleDimensionRenderLoader)
                {
                    // If the distance changed, update the sorted set; if the loader is a new one, remove() won't do anything.
                    this.readdLoaderIfUpdated(loader, this.loadersMap.computeIfAbsent(loader, (l) -> new View(loader)), distanceFromSource.applyAsDouble(loader.getOwnerLocation())+sourceDistance);
                }
                else
                {
                    SpaceTest.LOGGER.warn("PlayerRenderManager#addChunks received a loader of an unhandled type! Loader: "+loader
                            +", at chunk position: "+new ChunkPos(chunkPos)+", in dimension: "+dimension.location()+".");
                }
            });
        };
    }

    /**
     * Yields a LongConsumer which removes every chunk supplied to it from the manager's chunkmaps, for the given dimension.
     * @param dimension
     */
    public LongConsumer removeChunks(ResourceKey<Level> dimension)
    {
        Long2IntOpenHashMap dimMap = this.updateMap.computeIfAbsent(dimension, (d) -> new Long2IntOpenHashMap());
        return (chunkPos) -> dimMap.addTo(chunkPos, -1);
    }

    public void applyChanges()
    {
        this.updateMap.forEach((dimension, changes) -> {
            Long2IntOpenHashMap mainMap = this.chunkMap.computeIfAbsent(dimension, (dim) -> new Long2IntOpenHashMap());
            changes.forEach((chunkPos, change) -> {
                // Only update main map if there was a change.
                if (change != 0)
                {
                    int oldWatchCount = mainMap.addTo(chunkPos, change);
                    // Detect added chunk
                    if (change > 0)
                    {
                        if (oldWatchCount == 0)
                        {
                            this.addedMap.computeIfAbsent(dimension, (d) -> new LongOpenHashSet()).add((long) chunkPos);
                        }
                    }
                    else
                    {
                        // Detect removed chunk
                        if ((-change) == oldWatchCount) {
                            mainMap.remove((long) chunkPos);
                            this.removedMap.computeIfAbsent(dimension, (d) -> new LongOpenHashSet()).add((long) chunkPos);
                        }
                        // Detect chunk removed when it wasn't being tracked
                        else if ((-change) > oldWatchCount) {
                            this.dumpManagerAndCrash("Player Manager Error! Attempted to remove a nonexistent chunk from"
                                    + " the chunkmap! Chunk: " + new ChunkPos(chunkPos) + " in dimension " + dimension.location()
                                    + ", for " + this);
                        }
                    }
                }
            });
        });
        this.updateMap.clear();
    }

    /**
     * Used to run updates specific to the manager, including updating the map of chunks to match changes to the player
     * and render loaders' positions.
     */
    @Override
    public void update() {
//        SpaceTest.LOGGER.info("Updating "+this+".");
        this.updateMaps(PlayerRenderLoader::update,
                this::handleEnteredLoader,
                this::handleExitedLoader,
                this::handleInsideLoader,
                this::handleOutsideLoader);
//        SpaceTest.LOGGER.info("Finished updating "+this+".");
    }

    // View distance stuff:
    /**
     * Updates the renderloader's view distance. Chunks added/removed due to this are stored/accessed the same way as
     * chunks added via a normal update.
     * Of note is that this will delete any chunks currently in the added/removed sets.
     * @param newViewDistance
     */
    public void updateViewDistance(int newViewDistance) {
        SpaceTest.LOGGER.info("View distance for " + this + " updated to: " + newViewDistance);
        this.updateMaps(
                // Update method
                (loader) -> loader.updateViewDistance(newViewDistance),
                // Entered consumer
                (loader, view) -> {
            if (this.playerRenderLoader.hasViewAdded())
            {
                this.handleEnteredLoader(loader, view);
            }
            else
            {
                dumpManagerAndCrash("Had a loader enter player chunks when view distance was reduced! Player Manager: "+this
                        +"; Loader: "+loader+"; View: "+view);
            }
                },
                // Exited consumer
                (loader, view) -> {
            if (this.playerRenderLoader.hasViewRemoved())
            {
                this.handleExitedLoader(loader, view);
            }
            else
            {
                dumpManagerAndCrash("Had a loader exit player chunks when view distance was increased! Player Manager: "+this
                        +"; Loader: "+loader+"; View: "+view);
            }
                },
                // Stayed inside consumer
                (loader, view) -> {
            // Add/remove chunks for this loader.
            // Only the player renderloader should have added/removed chunks
            if (loader == this.playerRenderLoader)
            {
                if (this.playerRenderLoader.hasViewAdded())
                {
                    this.playerRenderLoader.forEachViewAdded(this.addChunks(this.playerRenderLoader.getTargetDimension(), view.getCurrDistance(), loader::getSourceDistanceTo));
                }
                else if (this.playerRenderLoader.hasViewRemoved())
                {
                    this.playerRenderLoader.forEachViewRemoved(this.removeChunks(this.playerRenderLoader.getTargetDimension()));
                }
            }
            else if (loader.hasAdded() || loader.hasRemoved())
            {
                this.dumpManagerAndCrash("Loader other than this manager's playerRenderLoader had added/removed chunks while updating view distance!"
                        +"Loader: "+loader+"; View: "+view);
            }
                },
                // Stayed outside consumer
                this::handleOutsideLoader);
    }

    private void updateMaps(@Nonnull Consumer<PlayerRenderLoader> updateMethod,
                           BiConsumer<RenderLoader, View> enteredConsumer,
                           BiConsumer<RenderLoader, View> exitedConsumer,
                           BiConsumer<RenderLoader, View> insideConsumer,
                           BiConsumer<RenderLoader, View> outsideConsumer)
    {
//        SpaceTest.LOGGER.info("Updating player loader.");
        updateMethod.accept(this.playerRenderLoader);
        // Clear the previous updates.
        this.deletePending();
        this.pendingLoaders.addAll(this.loadersMap.keySet());

        ArrayList<RenderLoader> exitedLoaders = new ArrayList<>();

//        SpaceTest.LOGGER.info("Processing pending loaders.");
        while (!this.pendingLoaders.isEmpty())
        {
            // Recompute distances. Bad Dijkstra's implementation.
            // Current loader's distance shouldn't be able to change, if all edge weights are positive.
            RenderLoader closestLoader = this.pendingLoaders.last();
            View closestLoaderView = this.loadersMap.get(closestLoader);
            if (!this.pendingLoaders.remove(closestLoader))
            {
                this.dumpManagerAndCrash("Failed to remove the closest loader from the pending loaders list!");
            }
            double currDistance = closestLoaderView.getCurrDistance();
//            SpaceTest.LOGGER.info("Processing loader: "+closestLoader);
            this.pendingLoaders.forEach((loader) -> {
//                SpaceTest.LOGGER.info("Running Dijkstra's for other loader: "+loader);
                DimBlockPos owner = loader.getOwnerLocation();
                // Only compute distance to the loader if it's inside the closest loader's previous chunks
                if (closestLoader.isPositionInCurrent(owner))
                {
                    // If the distance changed, so did the renderloader's position in the array.
                    this.readdLoaderIfUpdated(loader, loadersMap.get(loader), currDistance+closestLoader.getSourceDistanceTo(owner));
                }
            });

            // Process this renderloader's chunks.
            switch (closestLoaderView.getThresholdStatus(this.playerRenderLoader.getBlockViewDistance())) {
                case ENTERED ->
                        {
                    // Add all chunks in this loader.
//                    SpaceTest.LOGGER.info("Loader entered player's view range. Loader: "+closestLoader+"; View: "+closestLoaderView);
                    enteredConsumer.accept(closestLoader, closestLoaderView);
                }
                case STAYED_INSIDE ->
                        {
//                    SpaceTest.LOGGER.info("Loader stayed inside player's view range. Loader: " + closestLoader + "; View: " + closestLoaderView);
                    insideConsumer.accept(closestLoader, closestLoaderView);
                }
                case EXITED ->
                        {
//                    SpaceTest.LOGGER.info("Loader exited player's view range. Loader: "+closestLoader+"; View: "+closestLoaderView);
                    exitedConsumer.accept(closestLoader, closestLoaderView);
                    exitedLoaders.add(closestLoader);
                }
                case STAYED_OUTSIDE ->
                        {
//                    SpaceTest.LOGGER.info("Loader stayed outside player's view range. Loader: "+closestLoader+"; View: "+closestLoaderView);
                    // Loader isn't within render range, and may be outside the player's chunks.
                    outsideConsumer.accept(closestLoader, closestLoaderView);
                    exitedLoaders.add(closestLoader);
                }
                default ->
                        // Shouldn't be possible.
                        dumpManagerAndCrash("Encountered illegal threshold state while creating render maps for "
                                + this + "! State: " + closestLoaderView.getThresholdStatus(this.playerRenderLoader.getBlockViewDistance()));
            }
        }

        // Update the main map.
        this.applyChanges();

        // For all renderloaders that are no longer in the player's render chunks, remove them from the loader map.
        exitedLoaders.forEach((loader) -> {
            if (!this.isPositionInCurrent(loader.getOwnerLocation())
            || (this.loadersMap.get(loader) instanceof RemovedView))
            {
                this.loadersMap.remove(loader);
            }
        });
        // Finalize the view updates.
        this.loadersMap.values().forEach(View::finalizeUpdate);
    }

    private void handleEnteredLoader(RenderLoader loader, View view)
    {
        // Add all chunks in this loader.
        if (loader instanceof SingleDimensionRenderLoader)
        {
            ((SingleDimensionRenderLoader) loader).forEachCurrent(this.addChunks(
                    ((SingleDimensionRenderLoader) loader).getTargetDimension(),
                    view.getCurrDistance(), loader::getSourceDistanceTo));
        }
        else if (loader instanceof MultiDimensionalRenderLoader)
        {
            ((MultiDimensionalRenderLoader) loader).forEachCurrent((dim) ->
                    this.addChunks(dim, view.getCurrDistance(), loader::getSourceDistanceTo));
        }
        else
        {
            SpaceTest.LOGGER.warn("Unhandled loader passed during #generateChunkmaps! Loader: "+loader);
        }
    }

    private void handleExitedLoader(RenderLoader loader, View view)
    {
        if (loader instanceof SingleDimensionRenderLoader)
        {
            ((SingleDimensionRenderLoader) loader).forEachOld(
                    this.removeChunks(((SingleDimensionRenderLoader) loader).getTargetDimension()));
        }
        else if (loader instanceof MultiDimensionalRenderLoader)
        {
            ((MultiDimensionalRenderLoader) loader).forEachOld(this::removeChunks);
        }
        else
        {
            SpaceTest.LOGGER.warn("Unhandled loader passed during #generateChunkmaps! Loader: "+loader);
        }
    }

    private void handleInsideLoader(RenderLoader loader, View view)
    {
        // Add/remove chunks for this loader.
        // Renderloader can't have added/removed chunks if it's immutable and invariant.
        if (!(loader instanceof ImmutableRenderLoader))
        {
            if (loader instanceof SingleDimensionRenderLoader)
            {
                ((SingleDimensionRenderLoader) loader).forEachAdded(this.addChunks(
                        ((SingleDimensionRenderLoader) loader).getTargetDimension(),
                        view.getCurrDistance(), loader::getSourceDistanceTo));
                ((SingleDimensionRenderLoader) loader).forEachRemoved((
                        this.removeChunks(((SingleDimensionRenderLoader) loader).getTargetDimension())));
            }
            else if (loader instanceof MultiDimensionalRenderLoader)
            {
                ((MultiDimensionalRenderLoader) loader).forEachAdded((dim) ->
                        this.addChunks(dim, view.getCurrDistance(), loader::getSourceDistanceTo));
                ((MultiDimensionalRenderLoader) loader).forEachRemoved(this::removeChunks);
            }
            else
            {
                SpaceTest.LOGGER.warn("Unhandled loader passed during #generateChunkmaps! Loader: "+loader);
            }
        }
    }

    private void handleOutsideLoader(RenderLoader loader, View view)
    {

    }

    /**
     * Finalizes updates run in this renderloader - meaning, the old state is overwritten - "forgets" which chunks were added/removed.
     */
    @Override
    public void finalizeUpdate()
    {
        this.playerRenderLoader.finalizeUpdate();
        this.deletePending();
    }

    public void readdLoaderIfUpdated(RenderLoader loader, View view, double newDistance)
    {
        if (view.wouldUpdate(newDistance))
        {
            this.pendingLoaders.remove(loader);
            view.updateDistanceIfLower(newDistance);
            this.pendingLoaders.add(loader);
        }
    }

    /**
     * Queues a newly created renderloader to be added to the map.
     * @param loader
     */
    public void addCreatedRenderLoader(SingleDimensionRenderLoader loader)
    {
        SpaceTest.LOGGER.info(this+" received loader to add. Loader: "+loader);
        if (this.loadersMap.put(loader, new View(loader)) != null)
        {
            dumpManagerAndCrash("Added a renderloader twice!\n\tManager: "+this+"\n\tLoader: "+loader);
        }
    }

    /**
     * Queues a destroyed renderloader to be removed from the loader map.
     * @param loader
     */
    public void removeDestroyedRenderLoader(SingleDimensionRenderLoader loader)
    {
        SpaceTest.LOGGER.info(this+" received loader to remove. Loader: "+loader);
        this.loadersMap.computeIfPresent(loader, (l, view) -> new RemovedView(view));
    }

    /**
     * Queues a renderloader that may have moved into this manager's chunks to be processed.
     * @param loader
     */
    public void addMovedRenderLoader(SingleDimensionRenderLoader loader)
    {
        this.loadersMap.computeIfAbsent(loader, (l) -> new View(loader));
    }

    /**
     * Returns if the provided chunk position is in the current chunkmap for the given dimension
     *
     * @param dimension
     * @param chunkPos
     * @return
     */
    @Override
    public boolean isChunkPosInCurrent(ResourceKey<Level> dimension, long chunkPos) {
        return this.chunkMap.getOrDefault(dimension, emptyWatchCountMap).containsKey(chunkPos);
    }

    /**
     * Returns if the provided chunk position is in the current chunkmap for the given dimension
     * TODO remove?
     * @param dimension
     * @param chunkPos
     * @return
     */
    @Override
    public boolean isChunkPosInOld(ResourceKey<Level> dimension, long chunkPos) {
        return false;
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
        return this.isChunkPosInCurrent(dimension, ChunkPos.asLong(chunkX, chunkZ));
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
        return this.isChunkPosInOld(dimension, ChunkPos.asLong(chunkX, chunkZ));
    }

    /**
     * Returns whether the provided dimensional block position is in the current chunks.
     * @param dimPos
     * @return
     */
    @Override
    public boolean isPositionInCurrent(DimBlockPos dimPos)
    {
        return this.isChunkPosInCurrent(dimPos.dimension, ChunkPos.asLong(dimPos.pos));
    }

    /**
     * Executes a provided consumer for every position in the render manager's current map, within a specific dimension.
     * @param dimension
     * @param consumer
     */
    @Override
    public void forEachCurrent(ResourceKey<Level> dimension, LongConsumer consumer) {
        Long2IntOpenHashMap dimMap = this.chunkMap.getOrDefault(dimension, emptyWatchCountMap);
        SpaceTest.LOGGER.info("Iterating over all "+dimMap.size()+" current chunks in dimension "+dimension.location()+", for: "+this);
        dimMap.keySet().forEach(consumer);
    }

    /**
     * Executes a provided consumer for every position added to the render manager's map, within a specific dimension.
     * @param consumer
     * @param dimension
     */
    @Override
    public void forEachOld(ResourceKey<Level> dimension, LongConsumer consumer) {

    }

    /**
     * Executes a provided consumer for every position added to the render manager's map, within a specific dimension.
     * @param consumer
     * @param dimension
     */
    @Override
    public void forEachAdded(ResourceKey<Level> dimension, LongConsumer consumer) {
        LongOpenHashSet dimMap = this.addedMap.getOrDefault(dimension, emptyChunkSet);
//        SpaceTest.LOGGER.info("Iterating over all "+dimMap.size()+" added chunks in dimension "+dimension.location()+", for: "+this);
        dimMap.forEach(consumer);
        Marker s;
    }

    /**
     * Executes a provided consumer for every position added to the render manager's map, within a specific dimension.
     * @param consumer
     * @param dimension
     */
    @Override
    public void forEachRemoved(ResourceKey<Level> dimension, LongConsumer consumer) {
        LongOpenHashSet dimMap = this.removedMap.getOrDefault(dimension, emptyChunkSet);
//        SpaceTest.LOGGER.info("Iterating over all "+dimMap.size()+" removed chunks in dimension "+dimension.location()+", for: "+this);
        dimMap.forEach(consumer);
    }

    /**
     * Executes a provided consumer for every position in the render manager's current map, for all dimensions
     *
     * @param consumer
     */
    public void forEachCurrent(Function<ResourceKey<Level>, LongConsumer> consumer) {
        this.chunkMap.keySet().forEach((d) -> this.forEachCurrent(d, consumer.apply(d)));
    }

    /**
     * Executes a provided consumer for every position added to the render manager's map, for all dimensions
     * @param consumer
     */
    @Override
    public void forEachOld(Function<ResourceKey<Level>, LongConsumer> consumer) {

    }

    /**
     * Executes a provided consumer for every position added to the render manager's map, for all dimensions
     * @param consumer
     */
    @Override
    public void forEachAdded(Function<ResourceKey<Level>, LongConsumer> consumer) {
//        SpaceTest.LOGGER.info("PlayerRenderManager: Iterating over all added chunks.");
        this.addedMap.keySet().forEach((d) -> this.forEachAdded(d, consumer.apply(d)));
    }

    /**
     * Executes a provided consumer for every position added to the render manager's map, for all dimensions
     * @param consumer
     */
    @Override
    public void forEachRemoved(Function<ResourceKey<Level>, LongConsumer> consumer) {
//        SpaceTest.LOGGER.info("PlayerRenderManager: Iterating over all removed chunks.");
        this.removedMap.keySet().forEach((d) -> this.forEachRemoved(d, consumer.apply(d)));
    }

    /**
     * Returns all chunk positions (long) which were in the loader's previous state
     *
     * @return
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllOld()
    {
        return new HashMap<>();
    }

    /**
     * Returns all chunk positions (long) which are in the loader's current state
     *
     * @return
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllCurrent()
    {
        // Return a copy of the main map.
        Map<ResourceKey<Level>, LongOpenHashSet> currentMap = new HashMap<>();
        this.chunkMap.forEach((dim, watchMap) -> currentMap.put(dim, new LongOpenHashSet(watchMap.keySet())));
        return currentMap;
    }

    /**
     * Returns all chunk positions (long) which were removed in the most recent update to the loader
     * @return - Set of removed chunk positions
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllRemoved()
    {
        return this.removedMap;
    }

    /**
     * Returns all chunk positions (long) which were added in the most recent update to the loader
     * @return
     */
    // FIXME Unused.
    public Map<ResourceKey<Level>, LongOpenHashSet> getAllAdded()
    {
        return this.addedMap;
    }

    @Override
    public double getSourceDistanceTo(DimBlockPos targetPos)
    {
        return this.playerRenderLoader.getSourceDistanceTo(targetPos);
    }

    public void playerRespawned(ServerPlayer newPlayer)
    {
        this.playerRenderLoader.playerRespawned(newPlayer);
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof PlayerRenderManager) && (((PlayerRenderManager) obj).playerRenderLoader.equals(this.playerRenderLoader)));
    }

    @Override
    public int hashCode()
    {
        return this.playerRenderLoader.hashCode();
    }

    @Override
    public String toString()
    {
        return "Render manager for: " + this.playerRenderLoader;
    }

    public void dumpManager()
    {
        SpaceTest.LOGGER.info("**********************************");
        SpaceTest.LOGGER.info("Dumping data to logs for: "+this);

        SpaceTest.LOGGER.info("Outputting chunkmap.");
        this.chunkMap.forEach((dimension, watchMap) -> {
            SpaceTest.LOGGER.info("Dimension: "+dimension.location());
            watchMap.forEach((chunkPos, watchCount) -> {
                SpaceTest.LOGGER.info("Chunk position: "+new ChunkPos(chunkPos)+"; Watch count: "+watchCount);
            });
        });

        SpaceTest.LOGGER.info("Outputting updateMap.");
        this.updateMap.forEach((dimension, watchMap) -> {
            SpaceTest.LOGGER.info("updateMap Dimension: "+dimension.location());
            watchMap.forEach((chunkPos, watchCount) -> {
                SpaceTest.LOGGER.info("updateMap Chunk position: "+new ChunkPos(chunkPos)+"; Watch count delta: "+watchCount);
            });
        });

        SpaceTest.LOGGER.info("Outputting addedMap.");
        this.addedMap.forEach((dimension, addMap) -> {
            SpaceTest.LOGGER.info("addedMap Dimension: "+dimension.location());
            addMap.forEach((long chunkPos) -> {
                SpaceTest.LOGGER.info("addedMap Chunk position: "+new ChunkPos(chunkPos));
            });
        });

        SpaceTest.LOGGER.info("Outputting removedMap.");
        this.removedMap.forEach((dimension, removeMap) -> {
            SpaceTest.LOGGER.info("removedMap Dimension: "+dimension.location());
            removeMap.forEach((long chunkPos) -> {
                SpaceTest.LOGGER.info("removedMap Chunk position: "+new ChunkPos(chunkPos));
            });
        });

        // Map of all loaders to their corresponding
        SpaceTest.LOGGER.info("Outputting loadersMap.");
        loadersMap.forEach((loader, view) -> {
            SpaceTest.LOGGER.info("loadersMap Loader: "+loader+"; ID "+System.identityHashCode(loader)+"; View: "+view);
        });

        SpaceTest.LOGGER.info("Outputting pendingLoaders.");
        pendingLoaders.forEach((loader) -> {
            SpaceTest.LOGGER.info("pendingLoaders Loader: "+loader);
        });
        SpaceTest.LOGGER.info("Manager dumped.");
        SpaceTest.LOGGER.info("**********************************");
    }

    public void dumpManagerAndCrash(String error)
    {
        SpaceTest.LOGGER.fatal(error);
        this.dumpManager();
        throw new RuntimeException(error);
    }
}
