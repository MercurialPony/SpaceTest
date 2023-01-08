package melonslise.spacetest.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.managed.ManagedCoreShader;
import melonslise.spacetest.client.init.StShaders;
import melonslise.spacetest.util.CubemapDirection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.ClientWorldLoader;

import java.util.*;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class PlanetRenderer
{
	public static final ManagedCoreShader[] PLANET_SHADERS = new ManagedCoreShader[] { StShaders.PLANET_SOLID, StShaders.PLANET_CUTOUT, StShaders.PLANET_TRANSLUCENT };

	private World world;
	private WorldRenderer wr;

	private ChunkPos originChunkPos;
	private int xChunks;
	private int zChunks;

	private Set<WorldRenderer.ChunkInfo> chunkCache;

	public void init(RegistryKey<World> worldKey, ChunkPos centerPos, int xChunks, int zChunks)
	{
		this.wr = ClientWorldLoader.getWorldRenderer(worldKey);

		if(this.wr == null)
		{
			return;
		}

		this.world = ClientWorldLoader.getWorld(worldKey);

		this.originChunkPos = centerPos;
		this.xChunks = xChunks;
		this.zChunks = zChunks;

		// this.wr.chunks = new BuiltChunkStorage(this.wr.getChunkBuilder(), world, Math.max(xChunks, zChunks), this.wr); // FIXME set full planet view distance

		this.chunkCache = new HashSet<>(24 * (2 * xChunks + 1) * (2 * zChunks + 1) / 4); // approximate amount from a few quick tests
		Queue<WorldRenderer.ChunkInfo> chunkQueue = new ArrayDeque<>(this.wr.chunks.chunks.length);
		collectTopLevelChunks(this.world, this.wr.chunks::getRenderedChunk, this.originChunkPos, this.xChunks, this.zChunks, chunkQueue);
		collectVisibleChunks(this.world, this.wr.chunks::getRenderedChunk, chunkQueue, this.originChunkPos, this.xChunks, this.zChunks, this.chunkCache, new WorldRenderer.ChunkInfoList(this.wr.chunks.chunks.length));
		this.chunkCache.stream().filter(i -> i.chunk.getData().isEmpty()).forEach(i -> i.chunk.scheduleRebuild(false));
	}

	public void render(MatrixStack mtx, float frameDelta)
	{
		if(this.world == null || this.wr.chunks == null)
		{
			return;
		}

		updateChunks(this.chunkCache, this.wr.getChunkBuilder());

		MinecraftClient mc = MinecraftClient.getInstance();
		Camera cam = mc.gameRenderer.getCamera();
		Vec3d camPos = cam.getPos();

		mtx.push();

		mtx.translate(camPos.x, camPos.y, camPos.z);

		/*
		BlockPos pos = this.originChunkPos.getStartPos().withY(-64);
		List<ChunkBuilder.BuiltChunk> planes = Arrays.stream(CubemapDirection.values())
				.map(dir -> pos.add(dir.planeDirection.multiply(16)))
				.map(this.wr.chunks::getRenderedChunk)
				.toList();

		for(int i = 0; i < planes.size(); ++i)
		{
			ChunkBuilder.BuiltChunk builtChunk = planes.get(i);
			BlockPos cornerPos = builtChunk.getOrigin();

			for(ManagedCoreShader shader : PLANET_SHADERS)
			{
				shader.findUniform3f("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
				shader.findUniform3f("Corner").set((float) cornerPos.getX(), (float) cornerPos.getY(), (float) cornerPos.getZ()); // FIXME don't forget to not do origins here
				shader.findUniform1i("FaceSize").set(16);
				shader.findUniform1i("FaceIndex").set(i);
				shader.findUniform1i("SeaLevel").set(1);
			}

			List<WorldRenderer.ChunkInfo> list = List.of(new WorldRenderer.ChunkInfo(builtChunk, null, 0));
			renderLayer(list, RenderLayer.getSolid(), StShaders.PLANET_SOLID.getRenderLayer(RenderLayer.getSolid()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
			renderLayer(list, RenderLayer.getCutout(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutout()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
			renderLayer(list, RenderLayer.getCutoutMipped(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutoutMipped()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
			renderLayer(list, RenderLayer.getTranslucent(), StShaders.PLANET_TRANSLUCENT.getRenderLayer(RenderLayer.getTranslucent()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
		}
		 */

		final int faceSize = 10;

		for(ManagedCoreShader shader : PLANET_SHADERS)
		{
			shader.findUniform3f("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
			shader.findUniform1i("FaceSize").set(faceSize * 16);
			shader.findUniform1i("SeaLevel").set(1);
		}

		for(CubemapDirection direction : CubemapDirection.values())
		{
			renderCubeSphereSide(this.wr.chunks::getRenderedChunk, this.originChunkPos, camPos, mtx, faceSize, direction);
		}

		mtx.pop();
	}

	public static void renderCubeSphereSide(Function<BlockPos, ChunkBuilder.BuiltChunk> chunkGetter, ChunkPos originChunkPos, Vec3d cameraPos, MatrixStack mtx, int faceSize, CubemapDirection direction)
	{
		ChunkPos cornerChunkPos = new ChunkPos(originChunkPos.x + direction.planeDirection.getX(), originChunkPos.z + direction.planeDirection.getZ());

		List<WorldRenderer.ChunkInfo> sideChunks = new ArrayList<>();
		collectFaceChunks(chunkGetter, cornerChunkPos, faceSize, sideChunks);

		for(ManagedCoreShader shader : PLANET_SHADERS)
		{
			shader.findUniform3f("Corner").set(cornerChunkPos.getStartX(), -64, cornerChunkPos.getStartZ());
			shader.findUniform1i("FaceIndex").set(direction.ordinal());
		}

		renderLayer(sideChunks, RenderLayer.getSolid(), StShaders.PLANET_SOLID.getRenderLayer(RenderLayer.getSolid()), mtx, cameraPos.x, cameraPos.y, cameraPos.z, RenderSystem.getProjectionMatrix());
		renderLayer(sideChunks, RenderLayer.getCutout(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutout()), mtx, cameraPos.x, cameraPos.y, cameraPos.z, RenderSystem.getProjectionMatrix());
		renderLayer(sideChunks, RenderLayer.getCutoutMipped(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutoutMipped()), mtx, cameraPos.x, cameraPos.y, cameraPos.z, RenderSystem.getProjectionMatrix());
		renderLayer(sideChunks, RenderLayer.getTranslucent(), StShaders.PLANET_TRANSLUCENT.getRenderLayer(RenderLayer.getTranslucent()), mtx, cameraPos.x, cameraPos.y, cameraPos.z, RenderSystem.getProjectionMatrix());
	}

	/*
	public void renderLonLat(float x, float y, float z, MatrixStack mtx, float frameDelta)
	{
		if(this.world == null)
		{
			return;
		}

		updateChunks(this.chunkCache, this.wr.getChunkBuilder());

		MinecraftClient mc = MinecraftClient.getInstance();
		Camera cam = mc.gameRenderer.getCamera();
		Vec3d camPos = cam.getPos();

		BlockPos cornerPos = new ChunkPos(this.centerPos.x - this.xChunks, this.centerPos.z - this.zChunks).getStartPos();
		BlockPos tr = new BlockPos(x, y, z).subtract(cornerPos); // FIXME

		final int maxU = (xChunks * 2 + 1) * 16, maxV = (zChunks * 2 + 1) * 16; // FIXME precompute?

		mtx.push();

		float rot = (mc.world.getTime() + frameDelta) / 600f;
		mtx.multiply(Vec3f.POSITIVE_X.getRadialQuaternion(rot));
		mtx.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(rot));
		mtx.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(rot));

		mtx.translate(-x + camPos.x, -y + camPos.y, -z + camPos.z);
		mtx.translate(tr.getX(), tr.getY(), tr.getZ());

		for(ManagedCoreShader shader : PLANET_SHADERS)
		{
			shader.findUniform3f("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
			shader.findUniform3f("Corner").set((float) cornerPos.getX(), (float) cornerPos.getY(), (float) cornerPos.getZ());
			shader.findUniform2f("MaxUV").set((float) maxU, (float) maxV);
		}

		renderLayer(this.chunkCache, RenderLayer.getSolid(), StShaders.PLANET_SOLID.getRenderLayer(RenderLayer.getSolid()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
		renderLayer(this.chunkCache, RenderLayer.getCutout(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutout()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
		renderLayer(this.chunkCache, RenderLayer.getCutoutMipped(), StShaders.PLANET_CUTOUT.getRenderLayer(RenderLayer.getCutoutMipped()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
		renderLayer(this.chunkCache, RenderLayer.getTranslucent(), StShaders.PLANET_TRANSLUCENT.getRenderLayer(RenderLayer.getTranslucent()), mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());

		mtx.pop();
	}
	*/

	/*
	private static void collectChunks(WorldRenderer wr, ChunkPos chunkPos, int xChunks, int zChunks, Set<WorldRenderer.ChunkInfo> outChunks)
	{
		for (int x = -xChunks; x <= xChunks; ++x)
		{
			for (int z = -zChunks; z <= zChunks; ++z)
			{
				for (int y = 0; y < 16; ++y)
				{
					ChunkBuilder.BuiltChunk builtChunk = wr.chunks.getRenderedChunk(new ChunkPos(chunkPos.x + x, chunkPos.z + z).getStartPos().withY(y * 16));

					if(builtChunk == null)
					{
						continue;
					}

					if(builtChunk.getData() == ChunkBuilder.ChunkData.EMPTY) // difference between isEmpty??
					{
						//builtChunk.scheduleRebuild(false);
					}

					outChunks.add(new WorldRenderer.ChunkInfo(builtChunk, null, 0));
				}
			}
		}
	}
	 */

	private static void collectFaceChunks(Function<BlockPos, ChunkBuilder.BuiltChunk> chunkGetter, ChunkPos originChunkPos, int faceSize, Collection<WorldRenderer.ChunkInfo> outChunks)
	{
		for(int x = 0; x < faceSize; ++x)
		{
			for(int z = 0; z < faceSize; ++z)
			{
				ChunkBuilder.BuiltChunk builtChunk = chunkGetter.apply(new ChunkPos(originChunkPos.x + x, originChunkPos.z + z).getStartPos().withY(-64));

				if (builtChunk != null)
				{
					outChunks.add(new WorldRenderer.ChunkInfo(builtChunk, null, 0));
				}
			}
		}
	}

	private static void collectTopLevelChunks(World world, Function<BlockPos, ChunkBuilder.BuiltChunk> chunkGetter, ChunkPos centerChunkPos, int xChunks, int zChunks, Queue<WorldRenderer.ChunkInfo> outChunks)
	{
		//int topY = world.getTopY() - 8;
		for (int x = -xChunks; x <= xChunks; ++x)
		{
			for (int z = -zChunks; z <= zChunks; ++z)
			{
				ChunkBuilder.BuiltChunk builtChunk = chunkGetter.apply(new ChunkPos(centerChunkPos.x + x, centerChunkPos.z + z).getStartPos().withY(-64));

				if (builtChunk != null)
				{
					outChunks.add(new WorldRenderer.ChunkInfo(builtChunk, null, 0));
				}
			}
		}
	}

	private static boolean isChunkVisibleFrom(ChunkBuilder.BuiltChunk targetChunk, WorldRenderer.ChunkInfo originChunkInfo, Direction dirOfTargetChunk)
	{
		if(targetChunk == null || originChunkInfo.canCull(dirOfTargetChunk.getOpposite()))
		{
			return false;
		}

		if(!originChunkInfo.hasAnyDirection())
		{
			return true;
		}

		ChunkBuilder.ChunkData currentChunkData = originChunkInfo.chunk.getData();
		for (int j = 0; j < Direction.values().length; ++j)
		{
			Direction direction = Direction.values()[j].getOpposite();
			if (originChunkInfo.hasDirection(j) && currentChunkData.isVisibleThrough(direction, dirOfTargetChunk))
			{
				return true;
			}
		}

		return false;
	}

	private static void collectVisibleChunks(World world, Function<BlockPos, ChunkBuilder.BuiltChunk> chunkGetter, Queue<WorldRenderer.ChunkInfo> chunkQueue, ChunkPos centerChunkPos, int xChunks, int zChunks, Set<WorldRenderer.ChunkInfo> outChunks, WorldRenderer.ChunkInfoList chunkCache)
	{
		// BlockPos camChunkOrigin = new BlockPos(MathHelper.floor(cameraPos.x / 16.0) * 16, MathHelper.floor(cameraPos.y / 16.0) * 16, MathHelper.floor(cameraPos.z / 16.0) * 16);
		// BlockPos camChunkCenter = camChunkOrigin.add(8, 8, 8);

		// Entity.setRenderDistanceMultiplier(MathHelper.clamp((double)this.client.options.getClampedViewDistance() / 8.0, 1.0, 2.5) * this.client.options.getEntityDistanceScaling().getValue());

		while (!chunkQueue.isEmpty())
		{
			WorldRenderer.ChunkInfo currentInfo = chunkQueue.poll();
			ChunkBuilder.BuiltChunk currentChunk = currentInfo.chunk;
			outChunks.add(currentInfo);

			// get all neighbors of this chunk
			for (Direction direction : Direction.values())
			{
				BlockPos adjacentChunkOrigin = currentChunk.getNeighborPosition(direction);

				if(Math.abs(ChunkSectionPos.getSectionCoord(adjacentChunkOrigin.getX()) - centerChunkPos.x) > xChunks)
				{
					continue;
				}

				if(Math.abs(ChunkSectionPos.getSectionCoord(adjacentChunkOrigin.getZ()) - centerChunkPos.z) > zChunks)
				{
					continue;
				}

				if(adjacentChunkOrigin.getY() < world.getBottomY() || adjacentChunkOrigin.getY() >= world.getTopY())
				{
					continue;
				}

				ChunkBuilder.BuiltChunk adjacentChunk  = chunkGetter.apply(adjacentChunkOrigin);

				// if the neighbor chunk doesn't exist or can be culled easily then skip it
				// check if the neighboring chunk can be seen through at least one face of the current one. If it can't then skip it
				if(!isChunkVisibleFrom(adjacentChunk, currentInfo, direction))
				{
					continue;
				}

				/*
				BlockPos currentChunkOrigin = currentChunk.getOrigin();

				// if chebyshev distance between current chunk origin and cam chunk origin is bigger than 60 blocks
				// (chunk is far away presumably)
				if (Math.abs(currentChunkOrigin.getX() - camChunkOrigin.getX()) > 60 ||
					Math.abs(currentChunkOrigin.getY() - camChunkOrigin.getY()) > 60 ||
					Math.abs(currentChunkOrigin.getZ() - camChunkOrigin.getZ()) > 60)
				{
					BlockPos adjacentChunkOrigin = adjacentChunk.getOrigin();
					// to each axis add 16 either if it's the adjacent axis and camera chunk center is bigger than the adjacent chunk origin or if it's less
					BlockPos blockPos4 = adjacentChunkOrigin.add(
							(direction.getAxis() == Direction.Axis.X ? camChunkCenter.getX() > adjacentChunkOrigin.getX() : camChunkCenter.getX() < adjacentChunkOrigin.getX()) ? 16 : 0,
							(direction.getAxis() == Direction.Axis.Y ? camChunkCenter.getY() > adjacentChunkOrigin.getY() : camChunkCenter.getY() < adjacentChunkOrigin.getY()) ? 16 : 0,
							(direction.getAxis() == Direction.Axis.Z ? camChunkCenter.getZ() > adjacentChunkOrigin.getZ() : camChunkCenter.getZ() < adjacentChunkOrigin.getZ()) ? 16 : 0);

					Vec3d vec3d = new Vec3d(blockPos4.getX(), blockPos4.getY(), blockPos4.getZ());
					// direction from camera to this pos
					Vec3d vec3d2 = cameraPos.subtract(vec3d).normalize().multiply(Math.ceil(Math.sqrt(3d) * 16d));

					boolean bl3 = true;

					// ray march in  steps of sqrt(3) * 16 between camera and point
					while (cameraPos.subtract(vec3d).lengthSquared() > 3600.0)
					{
						vec3d = vec3d.add(vec3d2);

						// if ray is outside build area then stop
						if (vec3d.y > (double) this.world.getTopY() || vec3d.y < (double) this.world.getBottomY())
						{
							break;
						}

						// get chunk at current march step
						ChunkBuilder.BuiltChunk builtChunk3 = this.chunks.getRenderedChunk(new BlockPos(vec3d.x, vec3d.y, vec3d.z));
						// if it exists and has info stored already then skip it
						if (builtChunk3 != null && chunkInfoList.getInfo(builtChunk3) != null)
						{
							continue;
						}

						// if we found at least one chunk that either doesn't exist or exists but does not have info saved then we skip the entire neighboring chunk
						bl3 = false;
						break;
					}

					if (!bl3)
					{
						continue;
					}
				}
				 */

				// if we have info on this neighbor saved already then skip it
				WorldRenderer.ChunkInfo adjacentInfo = chunkCache.getInfo(adjacentChunk);
				if (adjacentInfo != null)
				{
					adjacentInfo.addDirection(direction);
					continue;
				}

				// seems to remove stray faraway outChunks that don't have any neighbors - we don't want that for a planet
				/*
				if (!adjacentChunk.shouldBuild())
				{
					if (this.isOutsideViewDistance(camChunkOrigin, currentChunk))
					{
						continue;
					}

					// this.nextUpdateTime.set(System.currentTimeMillis() + 500L);
					continue;
				}
				*/

				// if the neighbor is visible AND hasn't been saved yet then create an info for it, save it and add it to the chunkQueue
				adjacentInfo = new WorldRenderer.ChunkInfo(adjacentChunk, direction, currentInfo.propagationLevel + 1);
				adjacentInfo.updateCullingState(currentInfo.cullingState, direction);
				chunkQueue.add(adjacentInfo);
				chunkCache.setInfo(adjacentChunk, adjacentInfo);
			}
		}
	}

	private static void updateChunks(Collection<WorldRenderer.ChunkInfo> chunksToBuild, ChunkBuilder chunkBuilder) // FIXME check if this is called twice or if it works with the same amout of chunks as in og WR
	{
		ChunkRendererRegionBuilder chunkRendererRegionBuilder = new ChunkRendererRegionBuilder();

		chunkBuilder.upload();

		for (WorldRenderer.ChunkInfo chunkInfo : chunksToBuild)
		{
			ChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;

			if(!builtChunk.needsRebuild())
			{
				continue;
			}

			builtChunk.scheduleRebuild(chunkBuilder, chunkRendererRegionBuilder);
			builtChunk.cancelRebuild();
		}
	}

	private static void renderLayer(Collection<WorldRenderer.ChunkInfo> chunksToRender, RenderLayer chunkLayer, RenderLayer newLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix)
	{
		newLayer.startDrawing();

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

		for(WorldRenderer.ChunkInfo chunkInfo : chunksToRender)
		{
			ChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;

			if(builtChunk.getData().isEmpty(chunkLayer))
			{
				continue;
			}

			BlockPos pos = builtChunk.getOrigin();
			VertexBuffer buf = builtChunk.getBuffer(chunkLayer);

			if (chunkOffset != null)
			{
				chunkOffset.set((float) (pos.getX() - cameraX), (float) (pos.getY() - cameraY), (float) (pos.getZ() - cameraZ));
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