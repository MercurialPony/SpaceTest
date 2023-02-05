package melonslise.spacetest.compat.sodium;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkTracker;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import melonslise.spacetest.util.Vec3iFunction;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;

public class SodiumPlanetSectionStorage
{
	protected final ChunkTracker chunkTracker;
	public final Vec3iFunction<ChunkSection> chunkSectionGetter;

	public final RenderRegionManager regionManager;
	protected final Long2ReferenceMap<RenderSection> sectionStorage;

	public SodiumPlanetSectionStorage(ChunkTracker chunkTracker, Vec3iFunction<ChunkSection> chunkSectionGetter, CommandList commandList)
	{
		this.chunkTracker = chunkTracker;
		this.chunkSectionGetter = chunkSectionGetter;

		this.regionManager = new RenderRegionManager(commandList);
		this.sectionStorage = new Long2ReferenceOpenHashMap<>();
	}

	private RenderSection createRenderSection(int x, int y, int z)
	{
		RenderRegion region = this.regionManager.createRegionForChunk(x, y, z);
		RenderSection section = new RenderSection(null, x, y, z, region);
		region.addChunk(section);

		if (this.chunkSectionGetter.apply(x, y, z).isEmpty())
		{
			section.setData(ChunkRenderData.EMPTY);
		}
		else
		{
			section.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
		}

		return section;
	}

	private RenderSection obtainRenderSection(int x, int y, int z)
	{
		return this.sectionStorage.computeIfAbsent(ChunkSectionPos.asLong(x, y, z), key -> this.createRenderSection(x, y, z));
	}

	public RenderSection obtainLoadedRenderSection(int x, int y, int z)
	{
		return this.chunkTracker.hasMergedFlags(x, z, ChunkStatus.FLAG_HAS_BLOCK_DATA) ? this.obtainRenderSection(x, y, z) : null;
	}

	public RenderSection getRenderSectionRaw(int x, int y, int z)
	{
		return this.sectionStorage.get(ChunkSectionPos.asLong(x, y, z));
	}

	public void close(CommandList commandList)
	{
		this.regionManager.delete(commandList);
	}
}