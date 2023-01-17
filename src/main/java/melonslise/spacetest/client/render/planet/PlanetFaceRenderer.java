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
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * This is the core of planet rendering which is responsible for rendering a single face of a planet
 *
 * It is majorly based on the vanilla WorldRenderer and the general process is very similar with some tweaks and improvements
 * 1. First it grabs the entire area of the planet and compiles EVERY chunk once
 * 2. After that is done, it continuously finds all the chunks visible from the top of the planet...
 *    ... and subsequently culls them via a very naive approach:
 *    the planet is split in half based on the direction to the camera, and every chunk in the back half is culled
 *    (the code for frustum culling is also there but currently unused, I'm looking for better approaches)
 * 3. TODO: all the remaining chunks are then rebuilt if they have been updated (e.g. block placed)
 * 4. The lightmap of the target dimension is applied and the chunks are rendered with a special shader
 */
@Environment(EnvType.CLIENT)
public class PlanetFaceRenderer
{
	public final ChunkBuilder chunkBuilder;

	public final PlanetProperties planetProps;

	public final CubemapFace face;
	public final ChunkSectionPos cornerChunkPos;
	public final int faceHeight;

	public PlanetFaceStorage chunkStorage;
	public Collection<ChunkBuilder.BuiltChunk> chunkCache;
	public CompletableFuture<Collection<ChunkBuilder.BuiltChunk>> collectFuture;

	public PlanetFaceRenderer(ChunkBuilder chunkBuilder, PlanetProperties planetProps, CubemapFace face, ChunkSectionPos cornerChunkPos, int faceHeight)
	{
		this.chunkBuilder = chunkBuilder;

		this.planetProps = planetProps;

		this.face = face;
		this.cornerChunkPos = cornerChunkPos;
		this.faceHeight = faceHeight;

		this.chunkStorage = new PlanetFaceStorage(chunkBuilder, cornerChunkPos, planetProps.getFaceSize(), faceHeight);
	}

	// FIXME remove force
	public static void rebuild(ChunkBuilder chunkBuilder, Stream<ChunkBuilder.BuiltChunk> stream, boolean force)
	{
		ChunkRendererRegionBuilder regionBuilder = new ChunkRendererRegionBuilder();

		stream.forEach(chunk ->
		{
			if(force)
			{
				chunkBuilder.rebuild(chunk, regionBuilder);
			}
			else
			{
				chunk.scheduleRebuild(chunkBuilder, regionBuilder);
			}

			chunk.cancelRebuild();
		});

		if(force)
		{
			chunkBuilder.upload();
		}
	}

	public void rebuildAll()
	{
		rebuild(this.chunkBuilder, Arrays.stream(this.chunkStorage.chunks), false);
	}

	public void rebuildCache()
	{
		if(this.chunkCache != null)
		{
			rebuild(this.chunkBuilder, this.chunkCache.stream().filter(ChunkBuilder.BuiltChunk::needsRebuild), false);
		}
	}

	public Collection<ChunkBuilder.BuiltChunk> discoverAndCull(Collection<ChunkBuilder.BuiltChunk> outChunks)
	{
		Queue<ChunkNode> chunkQueue = new ArrayDeque<>();
		boolean[] visited = new boolean[this.chunkStorage.chunks.length];

		Vec3f container = new Vec3f(this.planetProps.getPosition());
		Vec3f normal = new Vec3f(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
		normal.subtract(container);

		// this is essentially what WorldRenderer#enqueueChunksInViewDistance does, except instead of the camera chunk we choose an arbitrary one
		// ...that being the top corner chunk
		chunkQueue.add(new ChunkNode(this.chunkStorage.get(this.cornerChunkPos.getX(), this.cornerChunkPos.getY() + this.faceHeight - 1, this.cornerChunkPos.getZ()), null));

		while (!chunkQueue.isEmpty())
		{
			ChunkNode currentNode = chunkQueue.poll();

			if(!currentNode.cull(MinecraftClient.getInstance().worldRenderer.frustum, normal, container))
			{
				outChunks.add(currentNode.chunk);
			}

			// get all neighbors of this chunk
			for (Direction direction : Direction.values())
			{
				ChunkBuilder.BuiltChunk adjacentChunk = currentNode.getNeighbor(direction);

				// if we visited this neighbor already then skip it
				// check if the neighboring chunk can be seen through at least one face of the current one. If it can't then skip it
				if(adjacentChunk == null || visited[adjacentChunk.index] || !currentNode.isVisibleThrough(direction))
				{
					continue;
				}

				// if the neighbor is visible AND hasn't been saved yet then mark it as visited, create an info for it and add it to the chunkQueue
				visited[adjacentChunk.index] = true;
				chunkQueue.add(new ChunkNode(adjacentChunk, direction));
			}
		}

		System.out.println(outChunks.size());

		return outChunks;
	}

	public void discoverAndCullAsync()
	{
		if(this.collectFuture != null && !this.collectFuture.isDone())
		{
			return;
		}

		// TODO init queue and list with initial size (does that improve performance)?

		this.collectFuture = CompletableFuture.supplyAsync(() -> this.discoverAndCull(new ArrayList<>()), Util.getMainWorkerExecutor())
			.exceptionally(e ->
			{
				e.printStackTrace();
				return null;
			});

		this.collectFuture.thenAccept(chunks -> this.chunkCache = chunks); // TODO: can this cause concurrency issues?
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





	private class ChunkNode
	{
		public final ChunkBuilder.BuiltChunk chunk;

		public final Direction visitedFromDirection;

		public ChunkNode(ChunkBuilder.BuiltChunk chunk, Direction direction)
		{
			this.chunk = chunk;
			this.visitedFromDirection = direction;
		}

		public ChunkBuilder.BuiltChunk getNeighbor(Direction direction)
		{
			BlockPos pos = this.chunk.getOrigin();

			return PlanetFaceRenderer.this.chunkStorage.get(
				ChunkSectionPos.getSectionCoord(pos.getX()) + direction.getOffsetX(),
				ChunkSectionPos.getSectionCoord(pos.getY()) + direction.getOffsetY(),
				ChunkSectionPos.getSectionCoord(pos.getZ()) + direction.getOffsetZ());
		}

		public boolean isVisibleThrough(Direction direction)
		{
			return this.visitedFromDirection == null || this.chunk.getData().isVisibleThrough(this.visitedFromDirection.getOpposite(), direction);
		}

		// FIXME is this worth?
		// also move method to parent class?
		public boolean cull(Frustum frustum, Vec3f planeNormal, Vec3f delta)
		{
			if(this.chunk.getData().isEmpty())
			{
				return true;
			}

			PlanetFaceRenderer renderer = PlanetFaceRenderer.this;

			Vec3d planeCenter = renderer.planetProps.getPosition();
			Box bounds = this.chunk.getBoundingBox();

			for(int i = 0; i < 8; ++i)
			{
				// Generate cube vertices
				//https://stackoverflow.com/a/65306627/11734319
				delta.set(
					(float) (GeneralUtil.checkBit(i, 0) ? bounds.minX : bounds.maxX),
					(float) (GeneralUtil.checkBit(i, 0) ? bounds.minY : bounds.maxY),
					(float) (GeneralUtil.checkBit(i, 0) ? bounds.minZ : bounds.maxZ));
				// to face local coords
				delta.add(-renderer.cornerChunkPos.getMinX(), -renderer.cornerChunkPos.getMinY(), -renderer.cornerChunkPos.getMinZ());

				PlanetProjection.faceToSpace(renderer.planetProps, renderer.face, delta);

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
	}
}