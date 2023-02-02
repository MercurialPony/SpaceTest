package melonslise.spacetest.compat.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphIterationQueue;
import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProjection;
import melonslise.spacetest.planet.PlanetProperties;
import melonslise.spacetest.planet.PlanetState;
import melonslise.spacetest.util.Vec3iFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class SodiumPlanetSectionCollector
{
	public final Vec3iFunction<ChunkSection> chunkSectionGetter;
	public final Vec3iFunction<RenderSection> renderSectionGetter;
	public final Consumer<RenderSection> updateScheduler;

	public final CubeFaceContext faceCtx;

	public final ChunkGraphIterationQueue discoveryQueue;

	public SodiumPlanetSectionCollector(Vec3iFunction<ChunkSection> chunkSectionGetter, Vec3iFunction<RenderSection> renderSectionGetter, Consumer<RenderSection> updateScheduler, CubeFaceContext faceCtx)
	{
		this.chunkSectionGetter = chunkSectionGetter;
		this.renderSectionGetter = renderSectionGetter;
		this.updateScheduler = updateScheduler;

		this.faceCtx = faceCtx;

		this.discoveryQueue = new ChunkGraphIterationQueue();
	}

	private ChunkSection getChunkSection(RenderSection section)
	{
		return this.chunkSectionGetter.apply(section.getChunkX(), section.getChunkY(), section.getChunkZ());
	}

	private RenderSection getRenderSection(int x, int y, int z)
	{
		// local face coords
		int lx = x - this.faceCtx.x();
		int ly = y - this.faceCtx.y();
		int lz = z - this.faceCtx.z();

		if(lx < 0 || lx >= this.faceCtx.faceSize() || lz < 0 || lz >= this.faceCtx.faceSize() || ly < 0 || ly >= this.faceCtx.faceHeight())
		{
			return null;
		}

		return this.renderSectionGetter.apply(x, y, z);
	}

	private boolean cullSection(PlanetProperties planetProps, PlanetState planetState, RenderSection section, Vector3f planeNormal, Vector3f delta)
	{
		Vector3d planeCenter = planetState.getPosition();

		// center of chunk bounds (8 times fewer computations than checking all corners)
		delta.set(section.getOriginX(), section.getOriginY(), section.getOriginZ()).add(8.0f, 8.0f, 8.0f);
		// to face local coords
		delta.sub(this.faceCtx.minX(), this.faceCtx.minY(), this.faceCtx.minZ());
		// to space coords
		PlanetProjection.faceToSpace(planetProps, planetState, this.faceCtx.face(), delta);
		// find difference between this and the center
		delta.sub((float) planeCenter.x, (float) planeCenter.y, (float) planeCenter.z);

		// https://math.stackexchange.com/questions/1330210/how-to-check-if-a-point-is-in-the-direction-of-the-normal-of-a-plane
		return delta.dot(planeNormal) <= 0.0f;
	}

	/*
	private record RenderSectionWrapper(RenderSection section, double distSq) implements Comparable<RenderSectionWrapper>
	{
		public RenderSectionWrapper(RenderSection section)
		{
			this(section, MinecraftClient.getInstance().gameRenderer.getCamera().getPos().squaredDistanceTo(section.getOriginX() + 0.5d, section.getOriginY() + 0.5d, section.getOriginZ() + 0.5d));
		}

		@Override
		public int compareTo(@NotNull SodiumPlanetSectionCollector.RenderSectionWrapper o)
		{
			return (int) Math.signum(this.distSq - o.distSq);
		}
	}
	 */

	public void discoverChunks(PlanetProperties planetProps, PlanetState planetState, ChunkRenderList sectionsToRender)
	{
		// FIXME
		// setup for culling
		Vector3f container = new Vector3f();
		container.set(planetState.getPosition());
		Vector3f normal = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().toVector3f();
		normal.sub(container);

		// setup for discovery
		this.discoveryQueue.clear();

		for (int cx = 0; cx < this.faceCtx.faceSize(); ++cx)
		{
			for (int cz = 0; cz < this.faceCtx.faceSize(); ++cz)
			{
				RenderSection s = this.getRenderSection(this.faceCtx.x() + cx, this.faceCtx.y() + this.faceCtx.faceHeight() - 1, this.faceCtx.z() + cz);

				if(s != null)
				{
					this.discoveryQueue.add(s, null);
				}
			}
		}

		//List<RenderSectionWrapper> sectionsToUpdate = new ArrayList<>();

		for(int i = 0; i < this.discoveryQueue.size(); ++i)
		{
			RenderSection currentSection = this.discoveryQueue.getRender(i);

			if(!this.getChunkSection(currentSection).isEmpty() && !this.cullSection(planetProps, planetState, currentSection, normal, container))
			{
				this.updateScheduler.accept(currentSection);
				//sectionsToUpdate.add(new RenderSectionWrapper(currentSection));

				if(!currentSection.isEmpty())
				{
					sectionsToRender.add(currentSection);
				}
			}

			RenderSection nextSection = this.getRenderSection(currentSection.getChunkX(), currentSection.getChunkY() - 1, currentSection.getChunkZ());

			if(nextSection != null && (this.getChunkSection(nextSection).isEmpty() || currentSection.getGraphInfo().isVisibleThrough(Direction.UP, Direction.DOWN)))
			{
				this.discoveryQueue.add(nextSection, null);
			}
		}

		/*
		Collections.sort(sectionsToUpdate);
		sectionsToUpdate.forEach(w -> this.updateScheduler.accept(w.section));
		 */
	}
}