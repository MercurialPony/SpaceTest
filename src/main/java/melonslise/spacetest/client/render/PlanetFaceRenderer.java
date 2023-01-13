package melonslise.spacetest.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.managed.ManagedCoreShader;
import melonslise.spacetest.client.init.StShaders;
import melonslise.spacetest.util.CubemapFace;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class PlanetFaceRenderer
{
	public final ChunkBuilder chunkBuilder;
	public final World world;

	public final CubemapFace face;
	public final ChunkSectionPos cornerChunkPos;
	public final int faceSize;

	public PlanetFaceStorage chunkStorage;
	public Collection<ChunkBuilder.BuiltChunk> chunkCache;
	public CompletableFuture<Collection<ChunkBuilder.BuiltChunk>> collectFuture;

	public PlanetFaceRenderer(ChunkBuilder chunkBuilder, World world, CubemapFace face, ChunkSectionPos cornerChunkPos, int faceSize)
	{
		this.chunkBuilder = chunkBuilder;
		this.world = world;

		this.face = face;
		this.cornerChunkPos = cornerChunkPos;
		this.faceSize = faceSize;

		this.chunkStorage = new PlanetFaceStorage(chunkBuilder, cornerChunkPos, faceSize, world.countVerticalSections());
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
		rebuild(this.chunkBuilder, Arrays.stream(this.chunkStorage.chunks).filter(Objects::nonNull), false);
	}

	public void rebuildCache()
	{
		if(this.chunkCache != null)
		{
			rebuild(this.chunkBuilder, this.chunkCache.stream().filter(Objects::nonNull).filter(ChunkBuilder.BuiltChunk::needsRebuild), false);
		}
	}

	private static boolean canSeeThrough(WorldRenderer.ChunkInfo chunkInfo, Direction dirOfTargetChunk)
	{
		//if(targetChunk == null || originChunkInfo.canCull(dirOfTargetChunk.getOpposite())) return false;

		if(!chunkInfo.hasAnyDirection())
		{
			return true;
		}

		ChunkBuilder.ChunkData chunkData = chunkInfo.chunk.getData();

		for (int j = 0; j < Direction.values().length; ++j) // FIXME just use the (only) saved direction
		{
			if (chunkInfo.hasDirection(j) && chunkData.isVisibleThrough(Direction.values()[j].getOpposite(), dirOfTargetChunk))
			{
				return true;
			}
		}

		return false;
	}

	// This is basically WorldRenderer#collectRenderableChunks minus the camera dependent stuff
	public Collection<ChunkBuilder.BuiltChunk> collect(Collection<ChunkBuilder.BuiltChunk> outChunks)
	{
		Queue<WorldRenderer.ChunkInfo> chunkQueue = new ArrayDeque<>();
		WorldRenderer.ChunkInfoList addedChunks = new WorldRenderer.ChunkInfoList(this.chunkStorage.chunks.length);

		// this is essentially what WorldRenderer#enqueueChunksInViewDistance does, except instead of the camera chunk we choose an arbitrary one
		// that being the top corner chunk
		int startChunkY = ChunkSectionPos.getSectionCoord(this.world.getTopY(Heightmap.Type.WORLD_SURFACE, this.cornerChunkPos.getMinX(), this.cornerChunkPos.getMinZ()));
		chunkQueue.add(new WorldRenderer.ChunkInfo(this.chunkStorage.get(this.cornerChunkPos.getX(), startChunkY, this.cornerChunkPos.getZ()), null, 0));

		while (!chunkQueue.isEmpty())
		{
			WorldRenderer.ChunkInfo currentInfo = chunkQueue.poll();
			outChunks.add(currentInfo.chunk);

			// get all neighbors of this chunk
			for (Direction direction : Direction.values())
			{
				// TODO
				BlockPos adjacentChunkOrigin = currentInfo.chunk.getNeighborPosition(direction);
				ChunkBuilder.BuiltChunk adjacentChunk = this.chunkStorage.get(adjacentChunkOrigin.getX() / 16, adjacentChunkOrigin.getY() / 16, adjacentChunkOrigin.getZ() / 16);

				// if the neighbor chunk doesn't exist or can be culled easily then skip it
				// check if the neighboring chunk can be seen through at least one face of the current one. If it can't then skip it
				if(adjacentChunk == null || !canSeeThrough(currentInfo, direction))
				{
					continue;
				}

				// if we have info on this neighbor saved already then skip it
				WorldRenderer.ChunkInfo adjacentInfo = addedChunks.getInfo(adjacentChunk);

				if (adjacentInfo != null)
				{
					//adjacentInfo.addDirection(direction);
					continue;
				}

				// if the neighbor is visible AND hasn't been saved yet then create an info for it, save it and add it to the chunkQueue
				adjacentInfo = new WorldRenderer.ChunkInfo(adjacentChunk, direction, currentInfo.propagationLevel + 1);
				//adjacentInfo.updateCullingState(currentInfo.cullingState, direction);
				addedChunks.setInfo(adjacentChunk, adjacentInfo);
				chunkQueue.add(adjacentInfo);
			}
		}

		return outChunks;
	}

	public void collectAsync()
	{
		if(this.collectFuture != null && !this.collectFuture.isDone())
		{
			return;
		}

		// TODO init queue and list with initial size (does that improve performance)?

		this.collectFuture = CompletableFuture.supplyAsync(() -> this.collect(new ArrayList<>()), Util.getMainWorkerExecutor())
			.exceptionally(e ->
			{
				e.printStackTrace();
				return null;
			});

		this.collectFuture.thenAccept(chunks -> this.chunkCache = chunks); // TODO: can this cause concurrency issues?
	}

	public void render(LightmapTexture lightmap, Vec3d cameraPos, MatrixStack mtx)
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