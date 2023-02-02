package melonslise.spacetest.compat.sodium;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkTracker;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.common.util.collections.WorkStealingFutureDrain;
import net.minecraft.client.world.ClientWorld;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SodiumPlanetSectionUpdater
{
	private final ClientWorld world;
	private final ChunkTracker chunkTracker;

	private final CommandList commandList;

	private final RenderRegionManager regionManager;
	private final ClonedChunkSectionCache sectionCache;

	private final ChunkBuilder sectionBuilder;
	private final Map<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues;

	public boolean needsUpdate;

	public SodiumPlanetSectionUpdater(ClientWorld world, ChunkTracker chunkTracker, RenderRegionManager regionManager, BlockRenderPassManager passManager, CommandList commandList)
	{
		this.world = world;
		this.chunkTracker = chunkTracker;

		this.commandList = commandList;

		this.regionManager = regionManager;
		this.sectionCache = new ClonedChunkSectionCache(this.world);

		this.sectionBuilder = new ChunkBuilder(ChunkModelVertexFormats.DEFAULT);
		this.sectionBuilder.init(world, passManager);
		this.rebuildQueues  = new EnumMap<>(ChunkUpdateType.class);
		for (ChunkUpdateType type : ChunkUpdateType.values())
		{
			this.rebuildQueues.put(type, new ObjectArrayFIFOQueue<>());
		}

		this.needsUpdate = true;
	}

	public void schedulePendingUpdates(RenderSection section)
	{
		if (section.getPendingUpdate() == null || !this.chunkTracker.hasMergedFlags(section.getChunkX(), section.getChunkZ(), ChunkStatus.FLAG_ALL))
		{
			return;
		}

		PriorityQueue<RenderSection> queue = this.rebuildQueues.get(section.getPendingUpdate());

		if (queue.size() >= 32)
		{
			return;
		}

		queue.enqueue(section);
	}

	private boolean performPendingUploads()
	{
		Iterator<ChunkBuildResult> it = this.sectionBuilder.createDeferredBuildResultDrain();

		if (!it.hasNext())
		{
			return false;
		}

		this.regionManager.upload(this.commandList, it);
		return true;
	}

	private ChunkRenderBuildTask createRebuildTask(RenderSection render)
	{
		ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);

		if (context == null)
		{
			return new ChunkRenderEmptyBuildTask(render, 0);
		}

		return new ChunkRenderRebuildTask(render, context, 0);
	}

	private void submitRebuildTasks(ChunkUpdateType filterType, LinkedList<CompletableFuture<ChunkBuildResult>> immediateFutures)
	{
		int budget = immediateFutures != null ? Integer.MAX_VALUE : this.sectionBuilder.getSchedulingBudget();

		PriorityQueue<RenderSection> queue = this.rebuildQueues.get(filterType);

		while (budget > 0 && !queue.isEmpty())
		{
			RenderSection section = queue.dequeue();

			if (section.isDisposed())
			{
				continue;
			}

			// Sections can move between update queues, but they won't be removed from the queue they were
			// previously in to save CPU cycles. We just filter any changed entries here instead.
			if (section.getPendingUpdate() != filterType)
			{
				continue;
			}

			ChunkRenderBuildTask task = this.createRebuildTask(section);
			CompletableFuture<?> future;

			if (immediateFutures != null)
			{
				CompletableFuture<ChunkBuildResult> immediateFuture = this.sectionBuilder.schedule(task);
				immediateFutures.add(immediateFuture);

				future = immediateFuture;
			}
			else
			{
				future = this.sectionBuilder.scheduleDeferred(task);
			}

			section.onBuildSubmitted(future);

			budget--;
		}
	}

	public void updateChunks()
	{
		var blockingFutures = new LinkedList<CompletableFuture<ChunkBuildResult>>();

		this.submitRebuildTasks(ChunkUpdateType.IMPORTANT_REBUILD, blockingFutures);
		this.submitRebuildTasks(ChunkUpdateType.INITIAL_BUILD, null);
		this.submitRebuildTasks(ChunkUpdateType.REBUILD, null);

		// Try to complete some other work on the main thread while we wait for rebuilds to complete
		this.needsUpdate |= this.performPendingUploads();

		if (!blockingFutures.isEmpty())
		{
			this.needsUpdate = true;
			this.regionManager.upload(this.commandList, new WorkStealingFutureDrain<>(blockingFutures, this.sectionBuilder::stealTask));
		}

		this.regionManager.cleanup();
	}

	public void close()
	{
		this.rebuildQueues.values().forEach(PriorityQueue::clear);
		this.sectionBuilder.stopWorkers();
	}
}