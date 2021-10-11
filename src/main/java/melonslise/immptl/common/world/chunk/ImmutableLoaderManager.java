package melonslise.immptl.common.world.chunk;

import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import melonslise.spacetest.SpaceTest;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongConsumer;

public class ImmutableLoaderManager<T extends ImmutableRenderLoader> {
    private final Map<DimBlockPos, T> immutableLoaders = new HashMap<>();

    // Queued renderloaders
    private final Set<ImmutableRenderLoader.RequestImmutableLoader> immutableAddQueue = Collections.synchronizedSet(new HashSet<>());
    private final Set<DimBlockPos> immutableRemoveQueue = Collections.synchronizedSet(new HashSet<>());

    private final Function<ImmutableRenderLoader.RequestImmutableLoader, T> loaderFactory;

    private Object mutex;

    public ImmutableLoaderManager(Function<ImmutableRenderLoader.RequestImmutableLoader, T> loaderFactory)
    {
        this.mutex = this;
        this.loaderFactory = loaderFactory;
    }

    /**
     * Returns whether a loader for the given location is queued to be created, or already exists and isn't queued to be
     * removed.
     * @param request
     * @return
     */
    public boolean loaderWillExist(ImmutableRenderLoader.RequestImmutableLoader request)
    {
        return (immutableLoaders.containsKey(request.owner) && !immutableRemoveQueue.contains(request.owner))
                || immutableAddQueue.contains(request);
    }

    /**
     * Returns whether a loader for the given location exists and isn't queued to be destroyed.
     * @param owner
     * @return
     */
    private boolean loaderExists(DimBlockPos owner)
    {
        return (immutableLoaders.containsKey(owner) && !immutableRemoveQueue.contains(owner));
    }

    /**
     *
     * @param owner
     * @param startCorner
     * @param xWidth
     * @param zWidth
     * @return
     */
    public boolean queueCreate(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
    {
        // TODO Make this create and return the renderloader, so the requester actually has it?
        // Or should that only be permitted after the loader's been created and added?
        ImmutableRenderLoader.RequestImmutableLoader request = new ImmutableRenderLoader.RequestImmutableLoader(owner, startCorner, xWidth, zWidth);
        synchronized (mutex)
        {
            // If there's already a renderloader for this location, don't overwrite it.
            if (!loaderWillExist(request))
            {
                if (immutableAddQueue.add(request))
                {
                    return true;
                }
            }
            SpaceTest.LOGGER.warn("Could not queue renderloader for block at '" + owner + "' to be created, as there will already be one there.");
            return false;
        }
    }

    /**
     *
     * @param owner
     * @return
     */
    public boolean queueDestroy(DimBlockPos owner)
    {
        synchronized (mutex)
        {
            // Can't destroy a non-existent loader.
            if (loaderExists(owner))
            {
                if (immutableRemoveQueue.add(owner))
                {
                    return true;
                }
            }
            SpaceTest.LOGGER.warn("Could not queue renderloader for block at '" + owner + "' to be destroyed.");
            return false;
        }
    }

    public ExistentialLoaders processQueued()
    {
        ExistentialLoaders loaders = new ExistentialLoaders();
        synchronized (mutex)
        {
            immutableRemoveQueue.forEach((loaderPos) -> loaders.destroyed.add(this.destroyRenderLoader(loaderPos)));
            immutableAddQueue.forEach((loaderRequest) -> loaders.created.add(this.createRenderLoader(loaderRequest)));
            immutableRemoveQueue.clear();
            immutableAddQueue.clear();
        }
        return loaders;
    }

    public void updateLoaders()
    {

    }

    public void finalizeUpdateLoaders()
    {

    }

    public void forEachAdded(BiFunction<ResourceKey<Level>, RenderLoader, LongConsumer> consumer)
    {
//        this.immutableLoaders.values().forEach((loader) -> {
//            loader.forEachAdded(consumer.apply(loader.getTargetDimension(), loader));
//        });
    }

    /**
     *
     * @param request
     */
    private T createRenderLoader(ImmutableRenderLoader.RequestImmutableLoader request)
    {
        ChunkPos end = new ChunkPos(request.startCorner.pos.x + request.xWidth-1, request.startCorner.pos.z + request.zWidth-1);
        SpaceTest.LOGGER.info("Creating renderloader for block at " + request.owner
                + ".\n\tTarget dimension: " + request.startCorner.dimension.location()
                + "\n\tStart chunk: " + request.startCorner + "\n\tEnd chunk: " + end);

        // Actually create the chunkloader
        T loader = this.loaderFactory.apply(request);
        immutableLoaders.put(request.owner, loader);
        SpaceTest.LOGGER.debug("Renderloader created for owner at " + request.owner + ".");
        return loader;
    }

    /**
     *
     * @param owner
     */
    private T destroyRenderLoader(DimBlockPos owner)
    {
        SpaceTest.LOGGER.info("Destroying renderloader at "+owner);
        return immutableLoaders.remove(owner);
    }

    public void dumpManager()
    {
        immutableLoaders.values().forEach(SpaceTest.LOGGER::info);
    }

    @Nullable
    public T getRenderLoader(DimBlockPos owner)
    {
        return this.immutableLoaders.get(owner);
    }

    public void clear() {
        synchronized (mutex)
        {
            this.immutableRemoveQueue.clear();
            this.immutableAddQueue.clear();
            this.immutableLoaders.clear();
        }
    }

    public static class ExistentialLoaders
    {
        public ArrayList<ImmutableRenderLoader> created = new ArrayList<>();
        public ArrayList<ImmutableRenderLoader> destroyed = new ArrayList<>();
    }
}
