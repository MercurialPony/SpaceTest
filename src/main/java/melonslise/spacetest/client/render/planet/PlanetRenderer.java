package melonslise.spacetest.client.render.planet;

import ladysnake.satin.api.managed.ManagedCoreShader;
import melonslise.spacetest.client.init.StShaders;
import melonslise.spacetest.client.render.LightmapTexture;
import melonslise.spacetest.planet.CubeData;
import melonslise.spacetest.planet.CubemapFace;
import melonslise.spacetest.planet.PlanetProjection;
import melonslise.spacetest.planet.PlanetProperties;
import melonslise.spacetest.util.QuatMath;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.ClientWorldLoader;

// FIXME: Bugs:
// placement doesn't get updated
// safe close all resources/threads/etc.
// wrong scale

/**
 * This is the central planet renderer class
 * It doesn't do much other than just act as a wrapper for each of the 6 face renderers with some extra logic
 */
@Environment(EnvType.CLIENT)
public class PlanetRenderer
{
	public PlanetProperties planetProps;

	public LightmapTexture lightmap;

	public CubeData<PlanetFaceRenderer> faceRenderers;

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

		this.faceRenderers = new CubeData<>(planetProps.getOrigin(), planetProps.getFaceSize(), (face, cornerChunkPos) -> new PlanetFaceRenderer(world, wr.getChunkBuilder(), planetProps, face, cornerChunkPos));
	}


	/*
	============= DEBUG METHODS - DELETE =============
	 */
	public static void line(MatrixStack mtx, VertexConsumer vc, Vec3f pos1, Vec3f pos2)
	{
		vc.vertex(mtx.peek().getPositionMatrix(), pos1.getX(), pos1.getY(), pos1.getZ()).color(1f,0f,0f,1f).normal(mtx.peek().getNormalMatrix(), 1f, 0f, 0f).next();
		vc.vertex(mtx.peek().getPositionMatrix(), pos2.getX(), pos2.getY(), pos2.getZ()).color(1f,0f,0f,1f).normal(mtx.peek().getNormalMatrix(), 1f, 0f, 0f).next();
	}

	public Vec3f transform(ChunkSectionPos corner, CubemapFace face, Vec3f pos)
	{
		pos.add(-corner.getMinX(), -corner.getMinY(), -corner.getMinZ());
		PlanetProjection.faceToSpace(this.planetProps, face, pos);
		return pos;
	}

		/*
	============= --------------------- =============
	 */

	public void render(MatrixStack mtx, float frameDelta)
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		Camera cam = mc.gameRenderer.getCamera();
		Vec3d camPos = cam.getPos();

		/*
		mtx.push();
		mtx.translate(-center.getX(), -center.getY(), -center.getZ());

		VertexConsumerProvider.Immediate buf = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer vc = buf.getBuffer(RenderLayer.getLines());

		for(CubemapFace face : CubemapFace.values())
		{
			PlanetFaceRenderer renderer = this.faceRenderers.get(face);
			ChunkSectionPos corner = renderer.cornerChunkPos;

			for (ChunkBuilder.BuiltChunk chunk : renderer.chunkCache)
			{
				if (chunk.getData().isEmpty())
				{
					continue;
				}

				Box bb = chunk.getBoundingBox();

				Vec3f v1 = this.transform(corner, face, new Vec3f((float) bb.minX, (float) bb.minY, (float) bb.minZ));
				Vec3f v2 = this.transform(corner, face, new Vec3f((float) bb.maxX, (float) bb.minY, (float) bb.minZ));
				Vec3f v3 = this.transform(corner, face, new Vec3f((float) bb.maxX, (float) bb.minY, (float) bb.maxZ));
				Vec3f v4 = this.transform(corner, face, new Vec3f((float) bb.minX, (float) bb.minY, (float) bb.maxZ));

				Vec3f v5 = this.transform(corner, face, new Vec3f((float) bb.minX, (float) bb.maxY, (float) bb.minZ));
				Vec3f v6 = this.transform(corner, face, new Vec3f((float) bb.maxX, (float) bb.maxY, (float) bb.minZ));
				Vec3f v7 = this.transform(corner, face, new Vec3f((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ));
				Vec3f v8 = this.transform(corner, face, new Vec3f((float) bb.minX, (float) bb.maxY, (float) bb.maxZ));

				line(mtx, vc, v1, v2);
				line(mtx, vc, v2, v3);
				line(mtx, vc, v3, v4);
				line(mtx, vc, v4, v1);

				line(mtx, vc, v1, v5);
				line(mtx, vc, v2, v6);
				line(mtx, vc, v3, v7);
				line(mtx, vc, v4, v8);

				line(mtx, vc, v5, v6);
				line(mtx, vc, v6, v7);
				line(mtx, vc, v7, v8);
				line(mtx, vc, v8, v5);
			}
		}

		buf.draw();

		mtx.pop();
		*/

		this.faceRenderers.forEach(PlanetFaceRenderer::processChunksAsync);

		mtx.push();

		Quaternion q = Quaternion.IDENTITY.copy();
		QuatMath.nlerp(q, this.planetProps.getLastRotation(), this.planetProps.getRotation(), frameDelta);

		mtx.multiply(q);

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
	}
}