package melonslise.immptl.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import melonslise.immptl.common.world.chunk.Helpers;
import melonslise.spacetest.SpaceTest;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayDeque;

/**
 * A container for all the renderchunks for a given dimension.
 * Unlike Vanilla, chunks in it have to have their presence requested by renderloaders before they can be used for
 * rendering. Additionally, chunks in this need to be explicitly evicted when they are no longer needed.
 */
public class RenderChunkContainer {
    private Long2ObjectOpenHashMap<ChunkRenderDispatcher.RenderChunk[]> renderColumnMap;
    private ArrayDeque<ChunkRenderDispatcher.RenderChunk[]> unusedRenderColumns;
    private ChunkRenderDispatcher.RenderChunk[][] renderColumns;
    private ChunkRenderDispatcher.RenderChunk[] emptyColumn;
    private int poolSize = 0;
    private final ChunkRenderDispatcher renderDispatcher;
    private final Level level;
    private int viewDistance;
    private int columnHeight;
    private int minBuildHeight;
    private static final double poolRatio = 20.0; // Ratio of the pool size to the number of chunks in the view distance square

    public RenderChunkContainer(Level level, ChunkRenderDispatcher dispatcher, int viewDistance)
    {
        SpaceTest.LOGGER.info("Render chunk container created with parameters: Level: "+level
                +"; Dispatcher: "+dispatcher+"; View Distance: "+viewDistance);
        this.level = level;
        this.renderDispatcher = dispatcher;
        this.setViewDistance(viewDistance);
    }

    public void setViewDistance(int newViewDistance)
    {
        SpaceTest.LOGGER.info("Render chunk container view distance set: "+newViewDistance);
        this.viewDistance = newViewDistance;
        this.columnHeight = this.level.getSectionsCount();
        this.minBuildHeight = this.level.getMinBuildHeight();
        // Generate an empty column for simplifying getting a chunk.
        this.emptyColumn = new ChunkRenderDispatcher.RenderChunk[this.columnHeight];
        for (int y = 0; y < this.columnHeight; y++)
        {
            this.emptyColumn[y] = null;
        }
        this.poolSize = (int)(Math.pow(Helpers.getViewWidth(newViewDistance), 2)*poolRatio);
        this.recreatePool(this.poolSize);
    }

    public void recreatePool(int newPoolSize)
    {
        SpaceTest.LOGGER.info("Render chunk container pool recreated: "+newPoolSize);
        this.renderColumns = new ChunkRenderDispatcher.RenderChunk[poolSize][this.columnHeight];
        this.unusedRenderColumns = new ArrayDeque<>(poolSize);
        this.renderColumnMap = new Long2ObjectOpenHashMap<>(poolSize);

        // Add the new render chunks.
        for (int i = 0; i < newPoolSize; i++)
        {
            ChunkRenderDispatcher.RenderChunk[] column = new ChunkRenderDispatcher.RenderChunk[this.columnHeight];
            for (int j = 0; j < this.columnHeight; j++)
            {
                column[j] = renderDispatcher.new RenderChunk(j);
            }
            this.renderColumns[i] = column;
            this.unusedRenderColumns.add(renderColumns[i]);
        }
        this.poolSize = newPoolSize;
    }

    public int getColumnHeight()
    {
        return this.columnHeight;
    }

    /**
     * Updates the y-origin for all chunks based on the current minimum build height.
     */
    public void updateYSource()
    {
        int newMinBuildHeight = this.level.getMinBuildHeight();
        if (newMinBuildHeight != this.minBuildHeight)
        {
            this.minBuildHeight = newMinBuildHeight;
            this.renderColumnMap.values().forEach((column) -> {
                for (ChunkRenderDispatcher.RenderChunk renderChunk : column) {
                    BlockPos oldOrigin = renderChunk.getOrigin();
                    renderChunk.setOrigin(oldOrigin.getX(), renderChunk.index*16 + this.minBuildHeight, oldOrigin.getZ());
                }
            });
        }
    }

    /**
     * Requests that a renderchunk column be added to the map, if it doesn't already exist.
     * @param pos
     */
    public void addRenderColumn(BlockPos pos)
    {
        this.addRenderColumn(ChunkPos.asLong(pos));
    }

    public void addRenderColumn(long chunkPos)
    {
        SpaceTest.LOGGER.info("Render chunk container had a column added: "+new ChunkPos(chunkPos));
        int chunkX = ChunkPos.getX(chunkPos);
        int chunkZ = ChunkPos.getZ(chunkPos);

        this.renderColumnMap.computeIfAbsent(chunkPos, (cPos) -> {
            ChunkRenderDispatcher.RenderChunk[] column = this.unusedRenderColumns.pollFirst();
            // TODO check if the unused pool is empty, and resize if so.
            for (ChunkRenderDispatcher.RenderChunk renderChunk : column) {
                // TODO Determine whether the origin should be calculated the way vanilla does in ViewArea::repositionCamera
                //  Nope, that seems to be a flawed calculation.
                // Chunk's source is supposed to be the (-x, -z) corner of it - so the chunk (-2, 0) would have a source of
                // (-32, 0), I believe
                renderChunk.setOrigin(chunkX*16, renderChunk.index*16 + this.minBuildHeight, chunkZ*16);
            }
            return column;
        });
    }

    /**
     * Attempts to remove a render column from the map, if it's in the map.
     * @param pos
     */
    public void removeRenderColumn(BlockPos pos)
    {
        this.removeRenderColumn(ChunkPos.asLong(pos));
    }

    public void removeRenderColumn(long chunkPos)
    {
        SpaceTest.LOGGER.info("Render chunk container had a column removed: "+new ChunkPos(chunkPos));
        ChunkRenderDispatcher.RenderChunk[] column = this.renderColumnMap.remove(chunkPos);
        if (column != null)
        {
            for (ChunkRenderDispatcher.RenderChunk renderChunk : column) {
                // Clear the renderchunk's position.
                renderChunk.setOrigin(-1, -1, -1);
            }
            this.unusedRenderColumns.addLast(column);
        }
    }

    /**
     * Gets the renderchunk for the provided block position
     * @param pos
     * @return - The corresponding RenderChunk, or null if there wasn't one for the position.
     */
    @Nullable
    public ChunkRenderDispatcher.RenderChunk getRenderChunkAt(BlockPos pos)
    {
        long chunkPos = ChunkPos.asLong(pos);
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        if ((sectionY >=  0) && (sectionY < this.columnHeight))
        {
            return this.renderColumnMap.getOrDefault(chunkPos, this.emptyColumn)[sectionY];
        }
        return null;
    }

    @Nullable
    public ChunkRenderDispatcher.RenderChunk[] getRenderColumnAt(long chunkPos)
    {
        SpaceTest.LOGGER.info("Render chunk container fetched a column: "+new ChunkPos(chunkPos));
        return this.renderColumnMap.get(chunkPos);
    }

    public void releaseAllBuffers()
    {
        for (ChunkRenderDispatcher.RenderChunk[] column : this.renderColumnMap.values()) {
            for (ChunkRenderDispatcher.RenderChunk renderChunk : column) {
                renderChunk.releaseBuffers();
            }
        }
    }

    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean dirty)
    {
        long chunkPos = ChunkPos.asLong(sectionX, sectionZ);
        int yIndex = Math.floorMod(sectionY-this.level.getMinSection(), this.columnHeight);
        ChunkRenderDispatcher.RenderChunk[] column = this.renderColumnMap.get(chunkPos);
        if (column != null)
        {
            column[yIndex].setDirty(dirty);
        }
    }

    // TODO Handle resizing the pool without recreating all the render chunks.
//    private void resizePool(int newPoolSize)
//    {
//        if (newPoolSize < this.poolSize)
//        {
//            // Shrinking the pool would be a bit more complicated.
//            throw new UnsupportedOperationException("Can't shrink the render chunk pool.");
//        }
//        else if (newPoolSize == this.poolSize)
//        {
//            SpaceTest.LOGGER.warn("Tried to set the render chunk pool size to the same size. Size: "+poolSize);
//        }
//
//        ChunkRenderDispatcher.RenderChunk[] newChunkArray = new ChunkRenderDispatcher.RenderChunk[poolSize];
//        ArrayDeque<ChunkRenderDispatcher.RenderChunk> newUnusedPool = new ArrayDeque<>(poolSize);
//        ArrayDeque<ChunkRenderDispatcher.RenderChunk> oldUnusedPool = unusedRenderColumns;
//
//        int i = 0;
//        // Transfer the preexisting RenderChunks
//        if (renderColumns != null)
//        {
//            for (; i < poolSize; i++)
//            {
//                newChunkArray[i] = renderColumns[i];
//            }
//        }
//        // Add the new render chunks.
//        for (; i < newPoolSize; i++)
//        {
//            newChunkArray[i] = renderDispatcher.new RenderChunk(i);
//            newUnusedPool.add(renderColumns[i]);
//        }
//        unusedRenderColumns = newUnusedPool;
//        if (oldUnusedPool != null)
//        {
//            unusedRenderColumns.addAll(oldUnusedPool);
//        }
//    }
}
