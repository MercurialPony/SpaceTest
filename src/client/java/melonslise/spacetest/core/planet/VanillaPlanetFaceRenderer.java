package melonslise.spacetest.core.planet;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.managed.ManagedCoreShader;
import melonslise.spacetest.core.planets.*;
import melonslise.spacetest.init.StShaders;
import melonslise.spacetest.planet.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * This is the core of planet rendering which is responsible for rendering a single face of a planet
 *
 * It is majorly based on the vanilla WorldRenderer and the general process is very similar (albeit with major refactors)
 * Chunks that are visible from the top of the planet are continuously discovered, rebuilt and culled, then sent to render with the lightmap of the target dimension and a special (planet) shader applied
 * More details below
 */
@Environment(EnvType.CLIENT)
public class VanillaPlanetFaceRenderer
{
	public final World world;
	public final ChunkBuilder chunkBuilder;

	public final CubeFaceContext ctx;

	public final VanillaPlanetFaceStorage chunkStorage;
	public Collection<ChunkBuilder.BuiltChunk> chunkCache;
	public CompletableFuture<Collection<ChunkBuilder.BuiltChunk>> processTask;

	public VanillaPlanetFaceRenderer(World world, ChunkBuilder chunkBuilder, PlanetProperties planetProps, CubemapFace face)
	{
		this.world = world;
		this.chunkBuilder = chunkBuilder;

		this.ctx = new CubeFaceContext(face, planetProps, world);

		this.chunkStorage = new VanillaPlanetFaceStorage(chunkBuilder, ctx);
	}

	// TODO: what is the difference between shouldRenderOnUpdate and checking world.isChunkLoaded (considering the immptl mixin)
	public void rebuildChunk(ChunkRendererRegionBuilder builder, ChunkBuilder.BuiltChunk chunk, int x, int z)
	{
		if(chunk.needsRebuild() && this.world.getChunk(x, z).shouldRenderOnUpdate())
		{
			chunk.scheduleRebuild(this.chunkBuilder, builder);
			chunk.cancelRebuild();
		}
	}

	// FIXME is this worth?
	public boolean cullChunk(PlanetProperties planetProps, PlanetState planetState, ChunkBuilder.BuiltChunk chunk, Vector3f planeNormal, Vector3f delta)
	{
		Vector3d planeCenter = planetState.getPosition();
		Box bounds = chunk.getBoundingBox();

		// center of chunk bounds (8 times fewer computations than checking corners)
		delta.set(MathHelper.lerp(0.5d, bounds.minX, bounds.maxX), MathHelper.lerp(0.5d, bounds.minY, bounds.maxY), MathHelper.lerp(0.5d, bounds.minZ, bounds.maxZ));
		// to face local coords
		delta.sub(this.ctx.minX(), this.ctx.minY(), this.ctx.minZ());

		PlanetProjection.faceToSpace(planetProps, planetState, this.ctx.face(), delta);

		delta.sub((float) planeCenter.x, (float) planeCenter.y, (float) planeCenter.z);

		// https://math.stackexchange.com/questions/1330210/how-to-check-if-a-point-is-in-the-direction-of-the-normal-of-a-plane
		return delta.dot(planeNormal) <= 0.0f;
	}

	/**
	 * This single method is essentially a mash of the vanilla WorldRenderer's enqueueChunksInViewDistance, collectRenderableChunks, applyFrustum and updateChunks all in one (for simplicity, performance and other reasons)
	 *
	 * The algorithm works as follows:
	 * 1. Starting with the top corner chunk, we recursively check all neighbors if:
	 *    The neighbor exists, hasn't been visited and can be seen through the parent chunk from the direction the parent chunk was visited previously
	 * 2. All of these discovered chunks are then rebuilt only if needed, i.e. if the planet is being rendered for the first time and the chunk has just been created, or if a block update occurred
	 *    Makes sure to also check if the chunk has been loaded first (that is what shouldRenderOnUpdate does I assume)
	 * 3. After that we check if the chunk can be culled and add it to the output if not
	 *    The current culling approach is very naive: the planet is split in half based on the direction to the camera, and every chunk in the back half is culled
	 *    (the code for frustum culling is also there but currently unused, I'm looking for better approaches)
	 *
	 * Worth noting that on the first couple of calls, not many chunks will be discovered, because they haven't been compiled yet and therefor their visibility graphs don't exist yet
	 * But with every call, more and more chunks are compiled and thus discovered
	 */
	/*
	public Collection<ChunkBuilder.BuiltChunk> processChunks(Collection<ChunkBuilder.BuiltChunk> outChunks)
	{
		// setup for culling
		Vector3f container = new Vector3f();
		container.set(this.planetProps.getPosition());
		Vector3f normal = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().toVector3f();
		normal.sub(container);

		// setup for rebuilding
		ChunkRendererRegionBuilder regionBuilder = new ChunkRendererRegionBuilder();

		// setup for discovery
		Queue<ChunkBuilder.BuiltChunk> chunkQueue = new ArrayDeque<>();
		Direction[] visited = new Direction[this.chunkStorage.chunks.length];

		for(int x = 0; x < this.planetProps.getFaceSize(); ++x)
		{
			for(int z = 0; z < this.planetProps.getFaceSize(); ++z)
			{
				chunkQueue.add(this.chunkStorage.get(this.ctx.x() + x, this.ctx.y() + this.ctx.faceHeight() - 1, this.ctx.z() + z));
			}
		}

		while (!chunkQueue.isEmpty())
		{
			ChunkBuilder.BuiltChunk currentChunk = chunkQueue.poll();

			BlockPos currentOrigin = currentChunk.getOrigin();
			int cx = ChunkSectionPos.getSectionCoord(currentOrigin.getX());
			int cy = ChunkSectionPos.getSectionCoord(currentOrigin.getY());
			int cz = ChunkSectionPos.getSectionCoord(currentOrigin.getZ());

			if(!this.cullChunk(currentChunk, normal, container))
			{
				this.rebuildChunk(regionBuilder, currentChunk, cx, cz);

				if(!currentChunk.getData().isEmpty())
				{
					outChunks.add(currentChunk);
				}
			}

			for (Direction direction : Direction.values())
			{
				ChunkBuilder.BuiltChunk adjacentChunk =  this.getNeighborChunk(cx, cy, cz, direction);

				// if the neighbor doesn't exist or has been visited already, skip it
				// also check if it can be seen through the current one via the face to which the current one was visited. If not, skip it
				if(adjacentChunk == null || visited[adjacentChunk.index] != null || !this.isSideVisibleThroughChunk(currentChunk, visited[currentChunk.index], direction))
				{
					continue;
				}

				// save the direction from which we came to this chunk for later
				visited[adjacentChunk.index] = direction;
				chunkQueue.add(adjacentChunk);
			}
		}

		return outChunks;
	}
	 */

	public Collection<ChunkBuilder.BuiltChunk> processChunks(PlanetProperties planetProps, PlanetState planetState, Collection<ChunkBuilder.BuiltChunk> outChunks)
	{
		// setup for culling
		Vector3f container = new Vector3f();
		container.set(planetState.getPosition());
		Vector3f normal = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().toVector3f();
		normal.sub(container);

		// setup for rebuilding
		ChunkRendererRegionBuilder regionBuilder = new ChunkRendererRegionBuilder();

		// setup for discovery
		Queue<ChunkBuilder.BuiltChunk> chunkQueue = new ArrayDeque<>();

		for (int x = 0; x < this.ctx.faceSize(); ++x)
		{
			for (int z = 0; z < this.ctx.faceSize(); ++z)
			{
				chunkQueue.add(this.chunkStorage.get(this.ctx.x() + x, this.ctx.y() + this.ctx.faceHeight() - 1, this.ctx.z() + z));
			}
		}

		while (!chunkQueue.isEmpty())
		{
			ChunkBuilder.BuiltChunk currentChunk = chunkQueue.poll();

			BlockPos currentOrigin = currentChunk.getOrigin();
			int cx = ChunkSectionPos.getSectionCoord(currentOrigin.getX());
			int cy = ChunkSectionPos.getSectionCoord(currentOrigin.getY());
			int cz = ChunkSectionPos.getSectionCoord(currentOrigin.getZ());

			if(!this.world.getChunk(cx, cz).getSection(this.world.sectionCoordToIndex(cy)).isEmpty())
			{
				if(!this.cullChunk(planetProps, planetState,  currentChunk, normal, container))
				{
					this.rebuildChunk(regionBuilder, currentChunk, cx, cz);
					outChunks.add(currentChunk);
				}
			}

			ChunkBuilder.BuiltChunk nextChunk = this.chunkStorage.get(cx, cy - 1, cz);

			if(nextChunk == null)
			{
				continue;
			}

			if(this.world.getChunk(cx, cz).getSection(this.world.sectionCoordToIndex(cy)).isEmpty() || currentChunk.getData().isVisibleThrough(Direction.UP, Direction.DOWN))
			{
				chunkQueue.add(nextChunk);
			}
			else
			{
				/*
				// hmm this adds too many unnecessary chunks.. maybe add a maximum depth (or travel distance from surface) parameter to cut off ones that are too deep?

				for(Direction direction : Direction.Type.HORIZONTAL)
				{
					int ax = cx + direction.getOffsetX();
					int ay = cy - 1 + direction.getOffsetY();
					int az = cz + direction.getOffsetZ();

					ChunkBuilder.BuiltChunk adjacentChunk = this.chunkStorage.get(ax, ay, az);

					if(adjacentChunk != null && (this.world.getChunk(ax, az).getSection(this.world.sectionCoordToIndex(ay)).isEmpty() || adjacentChunk.getData().isVisibleThrough(direction.getOpposite(), Direction.UP)))
					{
						chunkQueue.add(nextChunk);
						break;
					}
				}
				 */
			}
		}

		return outChunks;
	}

	public void processChunksAsync(PlanetProperties planetProps, PlanetState planetState)
	{
		if(this.processTask != null && !this.processTask.isDone())
		{
			return;
		}

		// TODO init queue and list with initial size (does that improve performance)?
		this.processTask = CompletableFuture.supplyAsync(() -> this.processChunks(planetProps, planetState, new ArrayList<>()), Util.getMainWorkerExecutor())
			.exceptionally(e ->
			{
				e.printStackTrace();
				return null;
			});

		// TODO: can this cause concurrency issues?
		this.processTask.thenAccept(chunks -> this.chunkCache = chunks);
	}

	public void render(MatrixStack mtx, Vec3d cameraPos, LightmapTexture lightmap)
	{
		if(this.chunkCache == null)
		{
			return;
		}

		for(ManagedCoreShader shader : StShaders.PLANET_SHADERS)
		{
			shader.findUniform3f("Corner").set(this.ctx.minX(), this.ctx.minY(), this.ctx.minZ());
			shader.findUniform1i("FaceIndex").set(this.ctx.face().ordinal());
		}

		// TODO loop this
		renderLayer(this.chunkCache, RenderLayer.getSolid(), StShaders.PLANET_SOLID.getRenderLayer(RenderLayer.getSolid()), lightmap, cameraPos, mtx, RenderSystem.getProjectionMatrix());
		renderLayer(this.chunkCache, RenderLayer.getCutout(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutout()), lightmap, cameraPos, mtx, RenderSystem.getProjectionMatrix());
		renderLayer(this.chunkCache, RenderLayer.getCutoutMipped(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutoutMipped()), lightmap, cameraPos, mtx, RenderSystem.getProjectionMatrix());
		renderLayer(this.chunkCache, RenderLayer.getTranslucent(), StShaders.PLANET_TRANSLUCENT.getRenderLayer(RenderLayer.getTranslucent()), lightmap, cameraPos, mtx, RenderSystem.getProjectionMatrix());
	}

	private static void renderLayer(Collection<ChunkBuilder.BuiltChunk> chunksToRender, RenderLayer chunkLayer, RenderLayer newLayer, LightmapTexture lightmap, Vec3d cameraPos, MatrixStack matrices, Matrix4f positionMatrix)
	{
		newLayer.startDrawing();
		lightmap.enable();

		ShaderProgram shader = RenderSystem.getShader();

		for(int j = 0; j < 12; ++j)
		{
			int k = RenderSystem.getShaderTexture(j);
			shader.addSampler("Sampler" + j, k);
		}

		if (shader.modelViewMat != null)
		{
			shader.modelViewMat.set(matrices.peek().getPositionMatrix());
		}

		if (shader.projectionMat != null)
		{
			shader.projectionMat.set(positionMatrix);
		}

		if (shader.colorModulator != null)
		{
			shader.colorModulator.set(RenderSystem.getShaderColor());
		}

		if (shader.fogStart != null)
		{
			shader.fogStart.set(RenderSystem.getShaderFogStart());
		}

		if (shader.fogEnd != null)
		{
			shader.fogEnd.set(RenderSystem.getShaderFogEnd());
		}

		if (shader.fogColor != null)
		{
			shader.fogColor.set(RenderSystem.getShaderFogColor());
		}

		if (shader.fogShape != null)
		{
			shader.fogShape.set(RenderSystem.getShaderFogShape().getId());
		}

		if (shader.textureMat != null)
		{
			shader.textureMat.set(RenderSystem.getTextureMatrix());
		}

		if (shader.gameTime != null)
		{
			shader.gameTime.set(RenderSystem.getShaderGameTime());
		}

		RenderSystem.setupShaderLights(shader);
		shader.bind();
		GlUniform chunkOffset = shader.chunkOffset;

		for(ChunkBuilder.BuiltChunk chunk : chunksToRender)
		{
			if(chunk.getData().isEmpty(chunkLayer))
			{
				continue;
			}

			BlockPos pos = chunk.getOrigin();
			VertexBuffer buf = chunk.getBuffer(chunkLayer);

			if (chunkOffset != null)
			{
				chunkOffset.set((float) (pos.getX() - cameraPos.getX()), (float) (pos.getY() - cameraPos.getY()), (float) (pos.getZ() - cameraPos.getZ()));
				chunkOffset.upload();
			}

			buf.bind();
			buf.draw();
		}

		if (chunkOffset != null)
		{
			chunkOffset.set(0.0f, 0.0f, 0.0f);
		}

		shader.unbind();
		VertexBuffer.unbind();
		newLayer.endDrawing();
	}
}