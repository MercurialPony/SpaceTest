package melonslise.spacetest.client.render.planet;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.managed.ManagedCoreShader;
import melonslise.spacetest.client.init.StShaders;
import melonslise.spacetest.client.render.LightmapTexture;
import melonslise.spacetest.planet.CubemapFace;
import melonslise.spacetest.planet.PlanetProjection;
import melonslise.spacetest.planet.PlanetProperties;
import melonslise.spacetest.util.GeneralUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

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
public class PlanetFaceRenderer
{
	public final World world;
	public final ChunkBuilder chunkBuilder;

	public final PlanetProperties planetProps;

	public final CubemapFace face;
	public final ChunkSectionPos cornerChunkPos;
	public final int faceHeight;

	public final PlanetFaceStorage chunkStorage;
	public Collection<ChunkBuilder.BuiltChunk> chunkCache;
	public CompletableFuture<Collection<ChunkBuilder.BuiltChunk>> processTask;

	public PlanetFaceRenderer(World world, ChunkBuilder chunkBuilder, PlanetProperties planetProps, CubemapFace face, ChunkSectionPos cornerChunkPos)
	{
		this.world = world;
		this.chunkBuilder = chunkBuilder;

		this.planetProps = planetProps;

		this.face = face;
		this.cornerChunkPos = cornerChunkPos;
		this.faceHeight = world.countVerticalSections();

		this.chunkStorage = new PlanetFaceStorage(chunkBuilder, cornerChunkPos, planetProps.getFaceSize(), this.faceHeight);
	}

	public ChunkBuilder.BuiltChunk getNeighborChunk(int x, int y, int z, Direction direction)
	{
		return this.chunkStorage.get(x + direction.getOffsetX(), y + direction.getOffsetY(), z + direction.getOffsetZ());
	}

	public boolean isSideVisibleThroughChunk(ChunkBuilder.BuiltChunk chunk, Direction visitedFromDirection, Direction targetDirection)
	{
		return visitedFromDirection == null || chunk.getData().isVisibleThrough(visitedFromDirection.getOpposite(), targetDirection);
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
	public boolean cullChunk(ChunkBuilder.BuiltChunk chunk, Vec3f planeNormal, Vec3f delta)
	{
		if(chunk.getData().isEmpty())
		{
			return true;
		}

		Vec3d planeCenter = this.planetProps.getPosition();
		Box bounds = chunk.getBoundingBox();

		for(int i = 0; i < 8; ++i)
		{
			// Generate cube vertices
			//https://stackoverflow.com/a/65306627/11734319
			delta.set(
					(float) (GeneralUtil.checkBit(i, 0) ? bounds.minX : bounds.maxX),
					(float) (GeneralUtil.checkBit(i, 0) ? bounds.minY : bounds.maxY),
					(float) (GeneralUtil.checkBit(i, 0) ? bounds.minZ : bounds.maxZ));
			// to face local coords
			delta.add(-this.cornerChunkPos.getMinX(), -this.cornerChunkPos.getMinY(), -this.cornerChunkPos.getMinZ());

			PlanetProjection.faceToSpace(this.planetProps, this.face, delta);

			delta.add((float) -planeCenter.x, (float) -planeCenter.y, (float) -planeCenter.z);

			// https://math.stackexchange.com/questions/1330210/how-to-check-if-a-point-is-in-the-direction-of-the-normal-of-a-plane
			if(delta.dot(planeNormal) > 0.0f)
			{
				return false;
			}

				/*
				delta.add((float) (planeCenter.x - frustum.x), (float) (planeCenter.y - frustum.y), (float) (planeCenter.z - frustum.z));

				boolean inFrustum = true;

				for(Vector4f frustumNormal : frustum.homogeneousCoordinates)
				{
					if(delta.getX() * frustumNormal.getX() + delta.getY() * frustumNormal.getY() + delta.getZ() * frustumNormal.getZ() < 0.0f)
					{
						inFrustum = false;
						break;
					}
				}

				if(inFrustum)
				{
					return true;
				}

				 */
		}

		return true;
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
	public Collection<ChunkBuilder.BuiltChunk> processChunks(Collection<ChunkBuilder.BuiltChunk> outChunks)
	{
		// setup for culling
		Vec3f container = new Vec3f(this.planetProps.getPosition());
		Vec3f normal = new Vec3f(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
		normal.subtract(container);

		// setup for rebuilding
		ChunkRendererRegionBuilder regionBuilder = new ChunkRendererRegionBuilder();

		// setup for discovery
		Queue<ChunkBuilder.BuiltChunk> chunkQueue = new ArrayDeque<>();
		Direction[] visited = new Direction[this.chunkStorage.chunks.length];

		// FIXME: add all top level chunks in case this one is obstructed?
		chunkQueue.add(this.chunkStorage.get(this.cornerChunkPos.getX(), this.cornerChunkPos.getY() + this.faceHeight - 1, this.cornerChunkPos.getZ()));

		while (!chunkQueue.isEmpty())
		{
			ChunkBuilder.BuiltChunk currentChunk = chunkQueue.poll();

			BlockPos currentOrigin = currentChunk.getOrigin();
			int cx = ChunkSectionPos.getSectionCoord(currentOrigin.getX());
			int cy = ChunkSectionPos.getSectionCoord(currentOrigin.getY());
			int cz = ChunkSectionPos.getSectionCoord(currentOrigin.getZ());

			// TODO: only rebuild chunks that can be seen (i.e. not culled)?
			// TODO: experiment with parallel force compilation
			this.rebuildChunk(regionBuilder, currentChunk, cx, cz);

			if(!this.cullChunk(currentChunk, normal, container))
			{
				outChunks.add(currentChunk);
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

	public void processChunksAsync()
	{
		if(this.processTask != null && !this.processTask.isDone())
		{
			return;
		}

		// TODO init queue and list with initial size (does that improve performance)?
		this.processTask = CompletableFuture.supplyAsync(() -> this.processChunks(new ArrayList<>()), Util.getMainWorkerExecutor())
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
			shader.findUniform3f("Corner").set(this.cornerChunkPos.getMinX(), this.cornerChunkPos.getMinY(), this.cornerChunkPos.getMinZ());
			shader.findUniform1i("FaceIndex").set(this.face.ordinal());
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

		Shader shader = RenderSystem.getShader();

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
			buf.drawElements();
		}

		if (chunkOffset != null)
		{
			chunkOffset.set(Vec3f.ZERO);
		}

		shader.unbind();
		VertexBuffer.unbind();
		newLayer.endDrawing();
	}
}