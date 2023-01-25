package melonslise.spacetest.render.planet;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.managed.ManagedCoreShader;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import melonslise.spacetest.SpaceTestClient;
import melonslise.spacetest.init.StShaders;
import melonslise.spacetest.planet.CubeData;
import melonslise.spacetest.planet.PlanetProjection;
import melonslise.spacetest.planet.PlanetProperties;
import melonslise.spacetest.render.LightmapTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import qouteall.imm_ptl.core.ClientWorldLoader;

// FIXME: Bugs/improvements:
// placement doesn't get updated
// safe close all resources/threads/etc.
// refresh on resoruce reload/etc?
// add transparency sorting from WorldRenderer.renderLayer
// custom compile task that assumes camera is always above

/**
 * This is the central planet renderer class
 * It doesn't do much other than just act as a wrapper for each of the 6 face renderers with some extra logic
 */
@Environment(EnvType.CLIENT)
public class VanillaPlanetRenderer implements PlanetRenderer
{
	public PlanetProperties planetProps;

	public LightmapTexture lightmap;

	public CubeData<VanillaPlanetFaceRenderer> faceRenderers;

	// FIXME get properties from world
	public void init(RegistryKey<World> worldKey, PlanetProperties planetProps)
	{
		ClientWorld world = ClientWorldLoader.getWorld(worldKey);
		WorldRenderer wr = ClientWorldLoader.getWorldRenderer(worldKey);

		if(wr == null)
		{
			return;
		}

		this.planetProps = planetProps;

		this.lightmap = new LightmapTexture(world);

		this.faceRenderers = new CubeData<>(face -> new VanillaPlanetFaceRenderer(world, wr.getChunkBuilder(), planetProps, face));
	}

	public void render(MatrixStack mtx, float frameDelta)
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		Camera cam = mc.gameRenderer.getCamera();
		Vec3d camPos = cam.getPos();

		this.faceRenderers.forEach(VanillaPlanetFaceRenderer::processChunksAsync);

		mtx.push();

		mtx.multiply(this.planetProps.getLastRotation().nlerp(this.planetProps.getRotation(), frameDelta, new Quaternionf())); // FIXME ughh object creationnn

		mtx.translate(camPos.x, camPos.y, camPos.z);

		for(ManagedCoreShader shader : StShaders.PLANET_SHADERS)
		{
			shader.findUniform3f("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
			shader.findUniform1i("FaceSize").set(this.planetProps.getFaceSize() * 16);
			shader.findUniform1f("StartRadius").set(this.planetProps.getStartRadius());
			shader.findUniform1f("RadiusRatio").set(this.planetProps.getRadiusRatio());
		}

		this.lightmap.tick(); // TODO: tick this properly
		this.lightmap.update(frameDelta);

		// render
		this.faceRenderers.forEach(fr -> fr.render(mtx, camPos, this.lightmap));

		mtx.pop();

		Vector3d center = this.planetProps.getPosition();

		// FIXME
		ManagedShaderEffect shader = StShaders.ATMOSPHERE;
		shader.findUniform3f("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
		shader.findUniformMat4("ProjInverseMat").set(RenderSystem.getProjectionMatrix().invert(new Matrix4f()));
		shader.findUniformMat4("ViewInverseMat").set(SpaceTestClient.modelViewMat.invert(new Matrix4f()));
		shader.findUniform3f("Center").set((float) center.x, (float) center.y, (float) center.z);
		float radius = PlanetProjection.heightToRadius(this.planetProps, 126.0f);
		shader.findUniform1f("PlanetRadius").set(radius);
		shader.findUniform1f("AtmosphereFalloff").set(12.0f);

		shader.getShaderEffect().render(frameDelta);
		MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
	}
}