package melonslise.spacetest.client.render;

import ladysnake.satin.api.managed.ManagedCoreShader;
import melonslise.spacetest.client.init.StShaders;
import melonslise.spacetest.util.CubeData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.ClientWorldLoader;

// FIXME: Bugs:
// having to replace the block if not everything is loaded
// placement doesn't get updated
// experiment with parallel force initial compilation
// safe close all resources/threads/etc.
@Environment(EnvType.CLIENT)
public class PlanetRenderer
{
	public int faceSize;

	//public DimensionRenderHelper lightmapHelper;
	public LightmapTexture lightmap;

	public CubeData<PlanetFaceRenderer> faceRenderers;

	public void init(RegistryKey<World> worldKey, ChunkPos originChunkPos, int faceSize)
	{
		ClientWorld world = ClientWorldLoader.getWorld(worldKey);
		WorldRenderer wr = ClientWorldLoader.getWorldRenderer(worldKey);
		//this.lightmapHelper = ClientWorldLoader.getDimensionRenderHelper(worldKey);

		if(wr == null)
		{
			return;
		}

		this.lightmap = new LightmapTexture(world);

		this.faceSize = faceSize;

		this.faceRenderers = new CubeData<>(ChunkSectionPos.from(originChunkPos, world.getBottomSectionCoord()), this.faceSize, (face, cornerChunkPos) -> new PlanetFaceRenderer(wr.getChunkBuilder(), world, face, cornerChunkPos, faceSize));
		this.faceRenderers.forEach(PlanetFaceRenderer::rebuildAll);
	}

	public void render(MatrixStack mtx, float frameDelta)
	{
		this.faceRenderers.forEach(PlanetFaceRenderer::collectAsync);
		this.faceRenderers.forEach(PlanetFaceRenderer::rebuildCache);

		MinecraftClient mc = MinecraftClient.getInstance();
		Camera cam = mc.gameRenderer.getCamera();
		Vec3d camPos = cam.getPos();

		mtx.push();

		mtx.translate(camPos.x, camPos.y, camPos.z);

		for(ManagedCoreShader shader : StShaders.PLANET_SHADERS)
		{
			shader.findUniform3f("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
			shader.findUniform1i("FaceSize").set(this.faceSize * 16);
			shader.findUniform1i("SeaLevel").set(62);
		}

		/*
		this is how IP does it...

		// save state
		ClientWorld oldWorld = mc.world;
		LightmapTextureManager oldLightmap = mc.gameRenderer.getLightmapTextureManager();

		// switch
		mc.world = (ClientWorld) this.lightmapHelper.world;
		((IEGameRenderer) mc.gameRenderer).setLightmapTextureManager(this.lightmapHelper.lightmapTexture);
		this.lightmapHelper.lightmapTexture.update(frameDelta);
		 */

		// ...that's rather inconvenient so we're just gonna use our own lightmap texture
		this.lightmap.tick(); // TODO: tick this properly
		this.lightmap.update(frameDelta);

		// render
		this.faceRenderers.forEach(fr -> fr.render(this.lightmap, camPos, mtx));

		/*
		// recover
		mc.world = oldWorld;
		((IEGameRenderer) mc.gameRenderer).setLightmapTextureManager(oldLightmap);
		 */

		mtx.pop();
	}
}