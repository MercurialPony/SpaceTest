package melonslise.immptl.client;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import melonslise.immptl.common.world.chunk.ImmutableLoaderManager;
import melonslise.immptl.common.world.chunk.RenderLoader;
import melonslise.immptl.common.world.chunk.SingleDimensionRenderLoader;
import melonslise.immptl.mixinInterfaces.ILevelRenderer_CustomViewArea;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import melonslise.spacetest.SpaceTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class PlayerViewManager {
    private static AbstractClientPlayer player;
    private static ClientPlayerRenderLoader playerLoader = null;

    private static final Map<ResourceKey<Level>, Long2IntOpenHashMap> renderChunkMap = new HashMap<>();
    private static final HashSet<RenderLoader> loaders = new HashSet<>();
    private static final ImmutableLoaderManager<ClientImmutableRenderLoader> immutableManager
            = new ImmutableLoaderManager<>(ClientImmutableRenderLoader::new);

    private static final Map<ResourceKey<Level>, ClientLevelInfo> clientLevels = new HashMap<>();

    private static final Long2IntOpenHashMap emptyChunkMap = new Long2IntOpenHashMap();

    private static long ticks = -1;
    private static final int updateFrequency = 10; // How often to update the player.

    /**
     * Attempts to queue an immutable renderloader to be created for the specified dimensional block position.
     * @param owner
     * @return
     */
    public static boolean addImmutableRenderLoader(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
    {
        SpaceTest.LOGGER.info("Client-side renderloader queued to be created.");
        return immutableManager.queueCreate(owner, startCorner, xWidth, zWidth);
    }

    /**
     * Attempts to queue the immutable renderloader for the specified dimensional block position to be destroyed.
     * @param owner
     * @return
     */
    public static boolean removeImmutableRenderLoader(DimBlockPos owner)
    {
        SpaceTest.LOGGER.info("Client-side renderloader queued to be destroyed.");
        return immutableManager.queueDestroy(owner);
    }

    /**
     * Gets the immutable renderloader for the specified dimensional block position.
     * @param owner
     * @return
     */
    @Nullable
    public static ClientImmutableRenderLoader getImmutableRenderLoader(DimBlockPos owner)
    {
        SpaceTest.LOGGER.info("Client-side renderloader requested for position: "+owner);
        return immutableManager.getRenderLoader(owner);
    }

    /**
     * Adds a provided level, and related objects, to the list of levels.
     * @param level
     * @param renderer
     */
    public static void addLevel(ClientLevel level, LevelRenderer renderer)
    {
        ClientLevelInfo info = new ClientLevelInfo(renderer, level, ((ILevelRenderer_CustomViewArea) renderer).getRenderChunkContainer());
        if (clientLevels.put(level.dimension(), info) != null)
        {
            dumpMapsAndCrash("Created duplicate client level! Dimension: "+level.dimension(), null, null);
        }
        SpaceTest.LOGGER.info("Added level to the level list. Level: "+info);
        // Make sure we add the render chunks for that dimension. That way we can queue renderchunks to be created, even if
        //  the level has yet to be created (idk, network lag or something.)
        renderChunkMap.getOrDefault(level.dimension(), emptyChunkMap).keySet().forEach((LongConsumer) info.container::addRenderColumn);
    }

    @Nullable
    private static RenderChunkContainer getRenderContainer(ResourceKey<Level> dimension)
    {
        return clientLevels.getOrDefault(dimension, ClientLevelInfo.emptyInfo).container;
    }

    /**
     * Yields a LongConsumer that adds all provided chunk to the specified dimension's maps.
     * @param dimension
     * @param addedMap
     * @param loader
     * @return
     */
    private static LongConsumer addRenderChunks(ResourceKey<Level> dimension, Map<ResourceKey<Level>, LongOpenHashSet> addedMap, RenderLoader loader)
    {
        LongOpenHashSet addedSet = addedMap.computeIfAbsent(dimension, (dim) -> new LongOpenHashSet());
        Long2IntOpenHashMap chunkMap = renderChunkMap.computeIfAbsent(dimension, (dim) -> new Long2IntOpenHashMap());
        return (chunkPos) -> {
            if (chunkMap.addTo(chunkPos, 1) == 0)
            {
                addedSet.add(chunkPos);
            }
        };
    }

    /**
     * Yields a LongConsumer that removes all provided chunk from the specified dimension's maps.
     * @param dimension
     * @param removedMap
     * @param loader
     * @return
     */
    private static LongConsumer removeRenderChunks(ResourceKey<Level> dimension, Map<ResourceKey<Level>, LongOpenHashSet> removedMap, RenderLoader loader)
    {
        LongOpenHashSet removedSet = removedMap.computeIfAbsent(dimension, (dim) -> new LongOpenHashSet());
        Long2IntOpenHashMap chunkMap = renderChunkMap.computeIfAbsent(dimension, (dim) -> {
            dumpMapsAndCrash("Attempted to remove a renderchunk in a dimension that doesn't have any!"
            +" Loader: "+loader+"; Dimension: "+dimension.location(), null, removedMap);
            return new Long2IntOpenHashMap();
        });
        return (chunkPos) -> {
            int oldCount = chunkMap.addTo(chunkPos, -1);
            if (oldCount == 1)
            {
                removedSet.add(chunkPos);
                chunkMap.remove(chunkPos);
            }
            else if (oldCount <= 0)
            {
                dumpMapsAndCrash("Attempted to remove a chunk that wasn't in the render map!"
                        +" Loader: "+loader+"; Dimension: "+dimension.location()+"; Chunk position: "+new ChunkPos(chunkPos), null, removedMap);
            }
        };
    }

    /**
     * Adds a set of single-dimensional renderloaders to the set of loaders, and to the main render map.
     * @param newLoaders
     * @param addedMap
     */
    private static void addSDRenderLoaders(ArrayList<? extends SingleDimensionRenderLoader> newLoaders,
                                           Map<ResourceKey<Level>, LongOpenHashSet> addedMap)
    {
        newLoaders.forEach((loader) -> {
            loader.forEachCurrent(addRenderChunks(loader.getTargetDimension(), addedMap, loader));
            loaders.add(loader);
        });
    }

    /**
     * Removes a set of single-dimensional renderloaders from the set of loaders, and from the main render map.
     * @param removedLoaders
     * @param removedMap
     */
    private static void removeSDRenderLoaders(ArrayList<? extends SingleDimensionRenderLoader> removedLoaders,
                                              Map<ResourceKey<Level>, LongOpenHashSet> removedMap)
    {
        removedLoaders.forEach((loader) -> {
            loader.forEachCurrent(removeRenderChunks(loader.getTargetDimension(), removedMap, loader));
            loaders.remove(loader);
        });
    }

    /**
     * Applies all added/removed chunks to the corresponding RenderChunkContainers.
     * @param added
     * @param removed
     */
    private static void applyChanges(Map<ResourceKey<Level>, LongOpenHashSet> added, Map<ResourceKey<Level>, LongOpenHashSet> removed)
    {
        added.forEach((dim, set) -> {
            RenderChunkContainer container = getRenderContainer(dim);
            if (container != null)
            {
                set.forEach((LongConsumer) container::addRenderColumn);
            }
        });
        removed.forEach((dim, set) -> {
            RenderChunkContainer container = getRenderContainer(dim);
            if (container != null)
            {
                set.forEach((LongConsumer) container::removeRenderColumn);
            }
        });
    }

    /**
     * Attempts to get the specified renderchunks
     * @param dimension - Dimension in which to fetch RenderChunks
     * @param nChunks - Number of chunks to fetch
     * @param arrayBuilder - A consumer which iterates over all the desired chunk positions.
     * @return - A list of RenderChunks, or null if the RenderChunkContainer for that dimension doesn't exist.
     */
    @Nullable
    public static Iterable<ChunkRenderDispatcher.RenderChunk> getRenderChunks(ResourceKey<Level> dimension, int nChunks, Consumer<LongConsumer> arrayBuilder)
    {
        RenderChunkContainer container = getRenderContainer(dimension);
        if (container != null)
        {
            ArrayList<ChunkRenderDispatcher.RenderChunk> renderChunks = new ArrayList<>(nChunks*container.getColumnHeight());
            try
            {
                arrayBuilder.accept((long chunkPos) -> {
                    Collections.addAll(renderChunks, container.getRenderColumnAt(chunkPos));
                });
            }
            catch (NullPointerException e)
            {
                dumpMapsAndCrash("Attempted to get a render column that didn't exist!", null, null);
            }
            return renderChunks;
        }
        return null;
    }

    // Setup/teardown stuff
    private static void initialize(AbstractClientPlayer newPlayer)
    {
        ticks = 0;
        addLevel(Minecraft.getInstance().level, Minecraft.getInstance().levelRenderer);
        playerLoader = new ClientPlayerRenderLoader(newPlayer, Minecraft.getInstance().options.renderDistance);
        Map<ResourceKey<Level>, LongOpenHashSet> addedChunks = new HashMap<>();
        Map<ResourceKey<Level>, LongOpenHashSet> removedChunks = new HashMap<>();
        playerLoader.forEachCurrent((dim) -> addRenderChunks(dim, addedChunks, playerLoader));
        applyChanges(addedChunks, removedChunks);
        playerLoader.finalizeUpdate();
        player = newPlayer;
    }

    private static void clear()
    {
        player = null;
        playerLoader = null;
        loaders.clear();
        renderChunkMap.clear();
        immutableManager.clear();
        ticks = 0;
    }

    // Event stuff
    public static void renderTick(TickEvent.RenderTickEvent event)
    {
        if (player != null)
        {
            if (event.phase == TickEvent.Phase.START)
            {
                //SpaceTest.LOGGER.info("Render tick start event received.");
            }
        }
    }

    public static void clientTick(TickEvent.ClientTickEvent event)
    {
        if (player != null)
        {
            if (event.phase == TickEvent.Phase.START)
            {
                Map<ResourceKey<Level>, LongOpenHashSet> addedChunks = new HashMap<>();
                Map<ResourceKey<Level>, LongOpenHashSet> removedChunks = new HashMap<>();
                ImmutableLoaderManager.ExistentialLoaders addedRemovedLoaders = immutableManager.processQueued();
                if ((ticks % updateFrequency) == 0)
                {
                    // TODO Implement system equivalent to PlayerRenderManager, where if the client has a lower render
                    //  distance than the server, they only generate render chunks for loaders in their range?
                    immutableManager.updateLoaders();
                    playerLoader.update();
                    playerLoader.forEachAdded((dim) -> addRenderChunks(dim, addedChunks, playerLoader));

                    // TODO deduplicate code, figure out the final view system.
                    addSDRenderLoaders(addedRemovedLoaders.created, addedChunks);
                    removeSDRenderLoaders(addedRemovedLoaders.destroyed, removedChunks);
                    playerLoader.forEachRemoved((dim) -> removeRenderChunks(dim, addedChunks, playerLoader));
                    applyChanges(addedChunks, removedChunks);
                    // Handle other loaders updating here.
                    playerLoader.finalizeUpdate();
                    immutableManager.finalizeUpdateLoaders();
                }
                else
                {
                    addSDRenderLoaders(addedRemovedLoaders.created, addedChunks);
                    removeSDRenderLoaders(addedRemovedLoaders.destroyed, removedChunks);
                    applyChanges(addedChunks, removedChunks);
                }
                ticks++;
                // TODO Is there any easy way to get client-side tick time?
                //SpaceTest.LOGGER.info("Client tick start event received.");
            }
        }
    }

    /**
     * Player joined the game.
     * @param event
     */
    public static void playerLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event)
    {
        if (event.getPlayer() != null)
        {
            // TODO Handle setup for player.
            // Add the initial level.
            initialize(event.getPlayer());
        }
        SpaceTest.LOGGER.info("Player logged in. Player: "+event.getPlayer());
    }

    public static void playerLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
    {
        clear();
        // TODO Handle cleanup for player
        SpaceTest.LOGGER.info("Player logged out. Player: "+event.getPlayer());
    }

    public static void playerRespawned(ClientPlayerNetworkEvent.RespawnEvent event)
    {
        // TODO Handle player respawn.
        SpaceTest.LOGGER.info("Player respawned. Player: "+event.getPlayer());
    }

    public static void dumpMaps(@Nullable Map<ResourceKey<Level>, LongOpenHashSet> added, @Nullable Map<ResourceKey<Level>, LongOpenHashSet> removed)
    {
        SpaceTest.LOGGER.info("\nDumping PlayerViewManager maps to log.");
        SpaceTest.LOGGER.info("Current player loader: "+playerLoader);

        SpaceTest.LOGGER.info("Render chunk map: ");
        renderChunkMap.forEach((dim, map) -> {
            SpaceTest.LOGGER.info("**Dimension: "+dim.location());
            map.keySet().stream().sorted().forEach((chunkPos) -> {
                SpaceTest.LOGGER.info("*Chunk: "+new ChunkPos(chunkPos)+"; Watcher count: "+map.get((long)chunkPos));
            });
        });

        SpaceTest.LOGGER.info("Loaders: ");
        loaders.forEach(SpaceTest.LOGGER::info);

        SpaceTest.LOGGER.info("Loader managers: ");
        immutableManager.dumpManager();

        SpaceTest.LOGGER.info("Levels: ");
        clientLevels.forEach((dim, levelInfo) -> {
            SpaceTest.LOGGER.info("**Dimension: "+dim.location()+"; Level: "+levelInfo);
        });

        if (added != null)
        {
            SpaceTest.LOGGER.info("Added chunks map: ");
            added.forEach((dim, set) -> {
                SpaceTest.LOGGER.info("**Added Dimension: "+dim.location());
                set.stream().sorted().forEach((chunkPos) -> {
                    SpaceTest.LOGGER.info("*Added Chunk: "+new ChunkPos(chunkPos));
                });
            });
        }
        if (removed != null)
        {
            SpaceTest.LOGGER.info("Removed chunks map: ");
            removed.forEach((dim, set) -> {
                SpaceTest.LOGGER.info("**Removed Dimension: "+dim.location());
                set.stream().sorted().forEach((chunkPos) -> {
                    SpaceTest.LOGGER.info("*Removed Chunk: "+new ChunkPos(chunkPos));
                });
            });
        }
    }

    public static void dumpMapsAndCrash(String error, @Nullable Map<ResourceKey<Level>, LongOpenHashSet> added, @Nullable Map<ResourceKey<Level>, LongOpenHashSet> removed)
    {
        SpaceTest.LOGGER.fatal(error);
        dumpMaps(added, removed);
        throw new RuntimeException(error);
    }
}
