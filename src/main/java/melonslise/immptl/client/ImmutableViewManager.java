package melonslise.immptl.client;

import melonslise.immptl.common.world.chunk.ImmutableRenderLoader;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import melonslise.spacetest.SpaceTest;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class ImmutableViewManager {
    private final Map<DimBlockPos, ImmutableRenderView> immutableViewers = new HashMap<>();

    // Queued renderloaders
    private final Set<ImmutableViewManager.RequestImmutableView> immutableAddQueue = Collections.synchronizedSet(new HashSet<>());
    private final Set<DimBlockPos> immutableRemoveQueue = Collections.synchronizedSet(new HashSet<>());

    private Object mutex;

    public ImmutableViewManager()
    {
        this.mutex = this;
    }

    /**
     * Returns whether a loader for the given location is queued to be created, or already exists and isn't queued to be
     * removed.
     * @param request
     * @return
     */
    public boolean loaderWillExist(ImmutableViewManager.RequestImmutableView request)
    {
        return (immutableViewers.containsKey(request.owner) && !immutableRemoveQueue.contains(request.owner))
                || immutableAddQueue.contains(request);
    }

    /**
     * Returns whether a loader for the given location exists and isn't queued to be destroyed.
     * @param owner
     * @return
     */
    private boolean loaderExists(DimBlockPos owner)
    {
        return (immutableViewers.containsKey(owner) && !immutableRemoveQueue.contains(owner));
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
        ImmutableViewManager.RequestImmutableView request = new ImmutableViewManager.RequestImmutableView(owner, startCorner, xWidth, zWidth);
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

    public ImmutableViewManager.ExistentialLoaders processQueued()
    {
        ImmutableViewManager.ExistentialLoaders loaders = new ImmutableViewManager.ExistentialLoaders();
        synchronized (mutex)
        {
            immutableRemoveQueue.forEach((loaderPos) -> loaders.destroyed.add(this.destroyRenderLoader(loaderPos)));
            immutableAddQueue.forEach((loaderRequest) -> loaders.created.add(this.createRenderLoader(loaderRequest)));
            immutableRemoveQueue.clear();
            immutableAddQueue.clear();
        }
        return loaders;
    }

    /**
     *
     * @param request
     */
    private ImmutableRenderView createRenderLoader(ImmutableViewManager.RequestImmutableView request)
    {
        ChunkPos end = new ChunkPos(request.startCorner.pos.x + request.xWidth-1, request.startCorner.pos.z + request.zWidth-1);
        SpaceTest.LOGGER.info("Creating renderloader for block at " + request.owner
                + ".\n\tTarget dimension: " + request.startCorner.dimension.location()
                + "\n\tStart chunk: " + request.startCorner + "\n\tEnd chunk: " + end);

        // Actually create the chunkloader
        ImmutableRenderView loader = new ImmutableRenderView(request);
        immutableViewers.put(request.owner, loader);
        SpaceTest.LOGGER.debug("Renderloader created for owner at " + request.owner + ".");
        return loader;
    }

    /**
     *
     * @param owner
     */
    private ImmutableRenderView destroyRenderLoader(DimBlockPos owner)
    {
        SpaceTest.LOGGER.info("Destroying renderloader at "+owner);
        return immutableViewers.remove(owner);
    }

    public void clear() {
        synchronized (mutex)
        {
            this.immutableRemoveQueue.clear();
            this.immutableAddQueue.clear();
            this.immutableViewers.clear();
        }
    }

    public static class ExistentialLoaders
    {
        ArrayList<ImmutableRenderView> created = new ArrayList<>();
        ArrayList<ImmutableRenderView> destroyed = new ArrayList<>();
    }

    public static class RequestImmutableView
    {
        public final DimBlockPos owner;
        public final DimChunkPos startCorner;
        public final int xWidth, zWidth;

        public RequestImmutableView(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
        {
            this.owner = owner;
            this.startCorner = startCorner;
            this.xWidth = xWidth;
            this.zWidth = zWidth;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof ImmutableRenderLoader.RequestImmutableLoader))
            {
                return false;
            }
            return this.owner.equals(((ImmutableRenderLoader.RequestImmutableLoader) obj).owner);
        }

        @Override
        public int hashCode()
        {
            return this.owner.hashCode();
        }
    }
}
