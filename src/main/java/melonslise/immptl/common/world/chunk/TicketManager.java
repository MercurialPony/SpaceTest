package melonslise.immptl.common.world.chunk;

import melonslise.spacetest.SpaceTest;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

/**
 * Class responsible for actually forcing/unforcing chunks
 * This is NOT thread-safe. No method in it should be called in parallel with another method in it.
 */
public class TicketManager {
    // A queue of chunks to force via the ticket manager. Used to limit how many tickets are created per tick.
    // TODO Should be able to set a priority for forcing chunks, so you can prioritize ones that are close to players.
    //  Not really a high priority, as I think that would normally only be an issue when the player has to load in their
    //  chunks from scratch. Plus, I could set it so that the forcing limit is higher if a player is newly loaded.
    private static Map<ServerLevel, HashSet<ChunkPos>> chunksToForce = new HashMap<>(); // TODO reset on server close.
    private static Map<ServerLevel, HashSet<ChunkPos>> chunksToUnforce = new HashMap<>();
    private static HashSet<ChunkPos> emptySet = new HashSet<>();
    // TODO add these to the config.
    private static final int maxForceLoadsPerTick = 4;
    private static final int bufferSize = 1000;
    private static final int loadRadius = 2; // How far out to load chunks; setting the radius to 2 means the passed chunk will be actively loaded (ticking?).
    // TODO actually implement this
    private static final int unforceDelay = 20; // How long this waits before actually unloading a chunk. NYI.

    public static final TicketType<ChunkPos> renderLoaderTicketType
            = TicketType.create(SpaceTest.ID+"_renderloader", Comparator.comparingLong(ChunkPos::toLong));



    /**
     * Attempts to queue a chunk to be forced. Returns true if the chunk was successfully queued, or if it cancelled
     *      a pending unforce.
     * Will prioritize cancelling pending unforces.
     * Note: The cancelling is only meant for handling chunk unforces that are found unnecessary on a later tick (e.g. the
     *      player moved away) - not as a working cache for determining which chunks ultimately need to be forced.
     *      I.e. only use this if you're sure a chunk needs to be forced.
     * @param target - the level to force a chunk in
     * @param chunkPos - the chunk position to force
     * @return - Whether the queueing was successful (queued or cancelled an unforce), or not (duplicated an already pending
     * force).
     */
    public static boolean queueForcedChunk(ServerLevel target, ChunkPos chunkPos)
    {
        // Attempt to remove it from the unforce queue, if it exists; otherwise, attempts to add it to the force queue.
        return chunksToUnforce.getOrDefault(target, emptySet).remove(chunkPos)
                || chunksToForce.computeIfAbsent(target, (l) -> {return new HashSet<>(bufferSize);}).add(chunkPos);
    }

    /**
     * Attempts to queue a chunk to be unforced. Returns true if the chunk was successfully queued, or if it cancelled
     *      a pending force.
     * Will prioritize cancelling pending forces.
     * Note: The cancelling is only meant for handling chunk forces that are found unnecessary on a later tick (e.g. the
     *      player moved away) - not as a working cache for determining which chunks ultimately need to be unforced.
     *      I.e. only use this if you're sure a chunk needs to be unforced.
     * @param target - the level to force a chunk in
     * @param chunkPos - the chunk position to force
     * @return - Whether the queueing was successful (queued or cancelled a force), or not (duplicated an already pending
     * unforce).
     */
    public static boolean queueUnforcedChunk(ServerLevel target, ChunkPos chunkPos)
    {
        // Attempt to remove it from the forced queue, if it exists; otherwise, attempts to add it to the unforce queue.
        return chunksToForce.getOrDefault(target, emptySet).remove(chunkPos)
                || chunksToUnforce.computeIfAbsent(target, (l) -> {return new HashSet<>(bufferSize);}).add(chunkPos);
    }

    /**
     * Processes the pending forced/unforced chunks. Will unforce all pending unforced chunks (eventually only old ones),
     * and forces pending forced chunks, up to a specified limit.
     */
    public static void processPendingChunks()
    {
        // We know the forced and unforced queues won't have any elements in common.
        // TODO There has to be a better way to implement this, and also allow ticket cancelling.
        //      Did I already resolve this?
        int i = 0;
        for (ServerLevel level : chunksToUnforce.keySet())
        {
            ServerChunkCache levelCache = level.getChunkSource();
            for (ChunkPos chunkPos : chunksToUnforce.get(level))
            {
                levelCache.removeRegionTicket(renderLoaderTicketType, chunkPos, loadRadius, chunkPos);
            }
            i += chunksToUnforce.get(level).size();
        }
        if (i > 0)
        {
            ;//SpaceTest.LOGGER.info("Unforced " + i + " chunks.");
        }
        chunksToUnforce.clear();

        i = 0;
        Iterator<ServerLevel> levelIter = chunksToForce.keySet().iterator();
        while (levelIter.hasNext() && i < maxForceLoadsPerTick)
        {
            ServerLevel level = levelIter.next();
            ServerChunkCache levelCache = level.getChunkSource();
            Iterator<ChunkPos> chunkIter = chunksToForce.get(level).iterator();
            while (chunkIter.hasNext() && i < maxForceLoadsPerTick)
            {
                ChunkPos chunkPos = chunkIter.next();
                levelCache.addRegionTicket(renderLoaderTicketType, chunkPos, loadRadius, chunkPos);
                chunkIter.remove();
                i++;
            }
            if (!chunkIter.hasNext())
            {
                levelIter.remove();
            }
        }
        if (i > 0)
        {
            ;//SpaceTest.LOGGER.info("Forced " + i + " chunks.");
        }
    }
}
