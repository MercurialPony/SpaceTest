package melonslise.spacetest.compat.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.*;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import melonslise.spacetest.SpaceTestClient;
import melonslise.spacetest.init.StShaders;
import melonslise.spacetest.planet.*;
import melonslise.spacetest.render.LightmapTexture;
import melonslise.spacetest.render.planet.PlanetRenderer;
import melonslise.spacetest.world.PlanetWorld;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

// FIXME
// IP clipping error in console
// batch all faces and do lazy collection (use needsUpdate from updater)
// refresh on resource reload/etc?
// add transparency sorting
// schedule chunks closest to the camera to compile first
// borrow already compiled chunks from worldrenderer
public class SodiumPlanetRenderer implements PlanetRenderer
{
	protected PlanetProperties planetProps;

	protected ChunkTracker chunkTracker;

	protected CommandList commandList;

	protected BlockRenderPassManager renderPassManager;
	protected ChunkRenderList renderableSections;

	protected SodiumPlanetSectionStorage sectionStorage;
	protected SodiumPlanetSectionUpdater sectionUpdater;
	protected CubeData<SodiumPlanetSectionCollector> sectionCollectors;
	protected SodiumPlanetRegionRenderer sectionRenderer;

	protected LightmapTexture lightmap;

	@Override
	public void init(ClientWorld world, WorldRenderer worldRenderer)
	{
		if(world == null)
		{
			return;
		}

		this.close();

		RenderDevice.enterManagedCode();

		this.planetProps = ((PlanetWorld) world).getPlanetProperties();

		this.chunkTracker = ((WorldRendererExtended) worldRenderer).getSodiumWorldRenderer().getChunkTracker();

		this.commandList = RenderDevice.INSTANCE.createCommandList();

		this.renderPassManager = BlockRenderPassManager.createDefaultMappings();
		this.renderableSections = new ChunkRenderList();

		this.sectionStorage = new SodiumPlanetSectionStorage(this.chunkTracker, (x, y, z) -> world.getChunk(x, z).getSection(world.sectionCoordToIndex(y)), this.commandList);
		this.sectionUpdater = new SodiumPlanetSectionUpdater(world, this.chunkTracker, this.sectionStorage.regionManager, this.renderPassManager, this.commandList);
		this.sectionCollectors = new CubeData<>(face -> new SodiumPlanetSectionCollector(
			this.sectionStorage.chunkSectionGetter,
			this.sectionStorage::obtainLoadedRenderSection,
			this.sectionUpdater::schedulePendingUpdates,
			new CubeFaceContext(face, this.planetProps, world)
		));
		this.sectionRenderer = new SodiumPlanetRegionRenderer(RenderDevice.INSTANCE, ChunkModelVertexFormats.DEFAULT);

		this.lightmap = new LightmapTexture(world);

		RenderDevice.exitManagedCode();
	}

	private void renderLayer(RenderLayer layer, ChunkRenderMatrices matrices, ChunkCameraContext cameraCtx)
	{
		layer.startDrawing();

		this.lightmap.enable();
		this.sectionRenderer.render(
			matrices,
			this.commandList,
			this.renderableSections,
			this.renderPassManager.getRenderPassForLayer(layer),
			cameraCtx
		);

		layer.endDrawing();
	}

	private void updateAndRender(PlanetState planetState, ChunkRenderMatrices matrices, ChunkCameraContext camera)
	{
		NativeBuffer.reclaim(false);

		this.chunkTracker.update();
		this.sectionUpdater.updateChunks();

		//if(this.sectionUpdater.needsUpdate) // FIXME
		this.sectionUpdater.needsUpdate = false;

		this.sectionRenderer.savePlanetProps(this.planetProps);

		for (SodiumPlanetSectionCollector collector : this.sectionCollectors)
		{
			this.renderableSections.clear();
			collector.discoverChunks(this.planetProps, planetState, this.renderableSections);

			this.sectionRenderer.saveFaceCtx(collector.faceCtx);
			this.renderLayer(RenderLayer.getSolid(), matrices, camera);
			this.renderLayer(RenderLayer.getCutout(), matrices, camera);
			this.renderLayer(RenderLayer.getCutoutMipped(), matrices, camera);
			this.renderLayer(RenderLayer.getTranslucent(), matrices, camera);
		}
	}

	private void renderAtmosphere(PlanetState planetState, ChunkCameraContext camera, float tickDelta)
	{
		Vector3d center = planetState.getPosition();

		// FIXME objectsss
		ManagedShaderEffect shader = StShaders.ATMOSPHERE;
		shader.findUniform3f("CameraPosition").set(camera.posX, camera.posY, camera.posZ);
		shader.findUniformMat4("ProjInverseMat").set(RenderSystem.getProjectionMatrix().invert(new Matrix4f()));
		shader.findUniformMat4("ViewInverseMat").set(SpaceTestClient.modelViewMat.invert(new Matrix4f()));
		shader.findUniform3f("Center").set((float) center.x, (float) center.y, (float) center.z);
		float radius = PlanetProjection.heightToRadius(this.planetProps, 126.0f);
		shader.findUniform1f("PlanetRadius").set(radius);
		shader.findUniform1f("AtmosphereFalloff").set(12.0f);

		shader.render(tickDelta);
	}

	// FIXME: add flawless frames mode
	@Override
	public void render(PlanetState planetState, MatrixStack mtx, float tickDelta)
	{
		if(this.closed())
		{
			return;
		}

		RenderDevice.enterManagedCode();

		Vec3d camPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		ChunkCameraContext camera = new ChunkCameraContext(camPos.x, camPos.y, camPos.z);

		this.lightmap.tick(); // TODO: tick this properly
		this.lightmap.update(tickDelta);

		mtx.push();
		mtx.multiply(planetState.getLastRotation().nlerp(planetState.getRotation(), tickDelta, new Quaternionf())); // FIXME ughh object creationnn
		mtx.translate(camPos.x, camPos.y, camPos.z);

		ChunkRenderMatrices matrices = ChunkRenderMatrices.from(mtx);

		this.updateAndRender(planetState, matrices, camera);
		this.renderAtmosphere(planetState, camera, tickDelta);

		mtx.pop();

		RenderDevice.exitManagedCode();
	}

	@Override
	public void scheduleRebuild(int x, int y, int z, boolean important)
	{
		this.sectionUpdater.sectionCache.invalidate(x, y, z);

		RenderSection section = this.sectionStorage.getRenderSectionRaw(x, y, z);

		if (section == null || !section.isBuilt())
		{
			return;
		}

		boolean alwaysDeferChunkUpdates = SodiumClientMod.options().performance.alwaysDeferChunkUpdates;

		// (important || this.isChunkPrioritized(section))
		section.markForUpdate(!alwaysDeferChunkUpdates && important ? ChunkUpdateType.IMPORTANT_REBUILD : ChunkUpdateType.REBUILD);
	}

	private boolean closed()
	{
		return this.planetProps == null;
	}

	@Override
	public void close()
	{
		if(this.closed())
		{
			return;
		}

		RenderDevice.enterManagedCode();

		this.planetProps = null;

		this.chunkTracker = null;

		this.renderPassManager = null;
		this.renderableSections.clear();
		this.renderableSections = null;

		this.sectionStorage.close(this.commandList);
		this.sectionStorage = null;
		this.sectionUpdater.close();
		this.sectionUpdater = null;
		this.sectionCollectors = null;
		this.sectionRenderer.delete();
		this.sectionRenderer = null;

		this.lightmap = null;

		this.commandList = null;

		RenderDevice.exitManagedCode();
	}
}